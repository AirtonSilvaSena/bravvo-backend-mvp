package br.com.bravvo.api.service;

import br.com.bravvo.api.entity.Protocolo;
import br.com.bravvo.api.dto.agendamento.*;
import br.com.bravvo.api.entity.Agendamento;
import br.com.bravvo.api.entity.FuncionarioPrefs;
import br.com.bravvo.api.enums.PerfilUser;
import br.com.bravvo.api.enums.StatusServico;
import br.com.bravvo.api.exception.BusinessException;
import br.com.bravvo.api.exception.ForbiddenException;
import br.com.bravvo.api.exception.NotFoundException;
import br.com.bravvo.api.mapper.AgendamentoMapper;
import br.com.bravvo.api.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.bravvo.api.dto.agendamento.AgendamentoItemResponseDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import java.time.*;

import java.util.Random;

/**
 * Core único de criação de agendamento (MVP).
 *
 * Regras aplicadas aqui (para todos os perfis): - serviço deve existir e estar
 * ativo - funcionário deve existir, estar ativo, e ser perfil FUNCIONARIO -
 * funcionário deve ter o serviço habilitado (funcionario_servicos) - resolver
 * duração: prefs_json -> fallback servico.duracaoMin - calcular fim = inicio +
 * duração - validar conflito final (overlap) com agendamentos bloqueantes -
 * gerar protocolo único - persistir
 *
 * Importante: - Controllers diferentes só adaptam "quem é o cliente" e "quem é
 * o funcionário"
 */
@Service
public class AgendamentoService {

	private final AgendamentoRepository agendamentoRepository;
	private final ServicoRepository servicoRepository;
	private final UserRepository userRepository;
	private final FuncionarioServicoRepository funcionarioServicoRepository;
	private final FuncionarioPrefsRepository funcionarioPrefsRepository;
	private final ProtocoloRepository protocoloRepository;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

	public AgendamentoService(AgendamentoRepository agendamentoRepository, ServicoRepository servicoRepository,
			UserRepository userRepository, FuncionarioServicoRepository funcionarioServicoRepository,
			FuncionarioPrefsRepository funcionarioPrefsRepository, ProtocoloRepository protocoloRepository) {
		this.agendamentoRepository = agendamentoRepository;
		this.servicoRepository = servicoRepository;
		this.userRepository = userRepository;
		this.funcionarioServicoRepository = funcionarioServicoRepository;
		this.funcionarioPrefsRepository = funcionarioPrefsRepository;
		this.protocoloRepository = protocoloRepository;
	}

	// ============================
	// Entradas (use pelos controllers)
	// ============================

	@Transactional
	public AgendamentoCreateResponseDTO createPublic(PublicAgendamentoCreateRequestDTO req) {
		var inicio = parseInicio(req.getData(), req.getHora());

		return createCore(req.getServicoId(), req.getFuncionarioId(), null, // clienteId = null (visitante)
				req.getClienteNome(), req.getClienteTelefone(), req.getClienteEmail(), req.getObservacoes(), inicio);
	}

	@Transactional
	public AgendamentoCreateResponseDTO createClienteLogado(Long clienteIdFromJwt,
			ClienteAgendamentoCreateRequestDTO req) {
		var inicio = parseInicio(req.getData(), req.getHora());

		// cliente logado: força clienteId, e (opcionalmente) podemos trazer
		// nome/telefone/email do cadastro
		var cliente = userRepository.findById(clienteIdFromJwt)
				.orElseThrow(() -> new NotFoundException("Cliente não encontrado."));

		if (!Boolean.TRUE.equals(cliente.getAtivo())) {
			throw new ForbiddenException("Usuário inativo.");
		}
		if (cliente.getPerfil() != PerfilUser.CLIENTE) {
			throw new ForbiddenException("Somente CLIENTE pode criar agendamento neste endpoint.");
		}

		return createCore(req.getServicoId(), req.getFuncionarioId(), cliente.getId(), cliente.getNome(),
				cliente.getTelefone(), cliente.getEmail(), req.getObservacoes(), inicio);
	}

	@Transactional
	public AgendamentoCreateResponseDTO createFuncionario(Long funcionarioIdFromJwt,
			FuncionarioAgendamentoCreateRequestDTO req) {

		var inicio = parseInicio(req.getData(), req.getHora());

		Long clienteId = req.getClienteId();
		String nome = req.getClienteNome();
		String tel = req.getClienteTelefone();
		String email = req.getClienteEmail();

		// Cenário A: cliente cadastrado
		if (clienteId != null) {
			var cliente = userRepository.findById(clienteId)
					.orElseThrow(() -> new NotFoundException("Cliente não encontrado."));

			if (!Boolean.TRUE.equals(cliente.getAtivo())) {
				throw new BusinessException("Cliente está inativo.");
			}
			if (cliente.getPerfil() != PerfilUser.CLIENTE) {
				throw new BusinessException("Usuário informado não é um cliente.");
			}

			nome = cliente.getNome();
			tel = cliente.getTelefone();
			email = cliente.getEmail();
		} else {
			// Cenário B: visitante
			if (nome == null || nome.isBlank() || tel == null || tel.isBlank()) {
				throw new BusinessException(
						"Informe clienteNome e clienteTelefone (ou selecione um cliente cadastrado).");
			}
		}

		return createCore(req.getServicoId(), funcionarioIdFromJwt, clienteId, nome, tel, email, req.getObservacoes(),
				inicio);
	}

	// ============================
	// Core único
	// ============================

	private AgendamentoCreateResponseDTO createCore(Long servicoId, Long funcionarioId, Long clienteId,
			String clienteNome, String clienteTelefone, String clienteEmail, String observacoes, LocalDateTime inicio) {
		// 1) valida serviço
		var servico = servicoRepository.findById(servicoId)
				.orElseThrow(() -> new NotFoundException("Serviço não encontrado."));

		// Ajuste se seu campo status for enum/string:
		// regra: precisa estar ATIVO
		// serviço precisa estar ATIVO
		if (servico.getStatus() != StatusServico.ATIVO) {
			throw new BusinessException("Serviço está inativo.");
		}

		// 2) valida funcionário
		var funcionario = userRepository.findById(funcionarioId)
				.orElseThrow(() -> new NotFoundException("Funcionário não encontrado."));

		if (!Boolean.TRUE.equals(funcionario.getAtivo())) {
			throw new BusinessException("Funcionário está inativo.");
		}
		if (funcionario.getPerfil() != PerfilUser.FUNCIONARIO) {
			throw new BusinessException("Usuário informado não é um funcionário.");
		}

		// 3) valida vínculo funcionário-serviço (habilitado)
		boolean habilitado = funcionarioServicoRepository.existsByIdFuncionarioIdAndIdServicoId(funcionarioId,
				servicoId);
		if (!habilitado) {
			throw new BusinessException("Este serviço não está habilitado para o funcionário.");
		}

		// 4) resolve duração
		int duracaoMin = resolveDuracaoMin(funcionarioId, servicoId, servico.getDuracaoMin());

		LocalDateTime fim = inicio.plusMinutes(duracaoMin);

		// 5) conflito final (não confiar só no GET)
		var conflitos = agendamentoRepository.findBlockingOverlapping(funcionarioId, inicio, fim);
		if (!conflitos.isEmpty()) {
			throw new BusinessException("Horário indisponível. Escolha outro horário.");
		}

		// 6) gera protocolo único
		String protocolo = generateUniqueProtocolo();

		// 6.1) registra protocolo na tabela protocolos (auditoria/rastreabilidade)
		Protocolo p = new Protocolo();
		p.setCodigo(protocolo);
		p.setTipo("agendamento");

		// Snapshot mínimo (MVP). Se quiser enriquecer depois, sem stress.
		p.setDadosJson(buildProtocoloDadosJson(servicoId, funcionarioId, clienteId, clienteNome, clienteTelefone,
				clienteEmail, inicio, fim));

		protocoloRepository.save(p);

		// 7) persiste agendamento referenciando o código do protocolo
		Agendamento ag = new Agendamento();
		ag.setProtocolo(protocolo);
		ag.setTipo("hora_marcada");
		ag.setServicoId(servicoId);
		ag.setFuncionarioId(funcionarioId);
		ag.setClienteId(clienteId);
		ag.setClienteNome(clienteNome);
		ag.setClienteTelefone(clienteTelefone);
		ag.setClienteEmail(clienteEmail);
		ag.setInicio(inicio);
		ag.setFim(fim);
		ag.setStatus("pendente");
		ag.setObservacoes(observacoes);

		ag = agendamentoRepository.save(ag);

		return new AgendamentoCreateResponseDTO(ag.getId(), ag.getProtocolo(), ag.getInicio(), ag.getFim(),
				ag.getStatus());
	}

	// ============================
	// Helpers
	// ============================

	private LocalDateTime parseInicio(String data, String hora) {
		try {
			LocalDate d = LocalDate.parse(data, DATE_FMT);
			LocalTime t = LocalTime.parse(hora, TIME_FMT);
			return LocalDateTime.of(d, t);
		} catch (Exception e) {
			throw new BusinessException("Data/hora inválidas. Use yyyy-MM-dd e HH:mm.");
		}
	}

	/**
	 * Resolve a duração do serviço para o funcionário: - prefs_json: { "servicos":
	 * { "<servicoId>": { "duracaoMin": X } } } - fallback: servico.duracaoMin
	 */
	private int resolveDuracaoMin(Long funcionarioId, Long servicoId, int duracaoPadrao) {
		try {
			FuncionarioPrefs prefs = funcionarioPrefsRepository.findById(funcionarioId).orElse(null);
			if (prefs == null || prefs.getPrefsJson() == null || prefs.getPrefsJson().isBlank()) {
				return duracaoPadrao;
			}

			JsonNode root = objectMapper.readTree(prefs.getPrefsJson());
			JsonNode servicosNode = root.path("servicos");
			JsonNode servicoNode = servicosNode.path(String.valueOf(servicoId));
			JsonNode duracaoNode = servicoNode.path("duracaoMin");

			if (duracaoNode.isInt()) {
				return duracaoNode.asInt();
			}
			return duracaoPadrao;
		} catch (Exception e) {
			// se prefs quebrar, não travar o agendamento (fallback no padrão)
			return duracaoPadrao;
		}
	}

	/**
	 * Protocolo simples (MVP): BRV-YYYYMMDD-XXXXXX - Checa unicidade em protocolos
	 * e em agendamentos (defesa dupla).
	 */
	private String generateUniqueProtocolo() {
		String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
		Random r = new Random();

		for (int i = 0; i < 10; i++) {
			String suffix = String.format("%06d", r.nextInt(1_000_000));
			String proto = "BRV-" + date + "-" + suffix;

			boolean exists = protocoloRepository.existsByCodigo(proto)
					|| agendamentoRepository.existsByProtocolo(proto);

			if (!exists) {
				return proto;
			}
		}

		throw new BusinessException("Não foi possível gerar protocolo. Tente novamente.");
	}

	/**
	 * Monta um JSON simples para auditoria do protocolo (MVP). Mantemos manual para
	 * evitar criar DTO extra só pra isso agora.
	 */
	private String buildProtocoloDadosJson(Long servicoId, Long funcionarioId, Long clienteId, String clienteNome,
			String clienteTelefone, String clienteEmail, LocalDateTime inicio, LocalDateTime fim) {
		// JSON “manual” simples. (Se preferir, podemos usar ObjectMapper depois.)
		return "{" + "\"servicoId\":" + servicoId + "," + "\"funcionarioId\":" + funcionarioId + "," + "\"clienteId\":"
				+ (clienteId == null ? "null" : clienteId) + "," + "\"clienteNome\":\"" + safeJson(clienteNome) + "\","
				+ "\"clienteTelefone\":\"" + safeJson(clienteTelefone) + "\"," + "\"clienteEmail\":\""
				+ safeJson(clienteEmail) + "\"," + "\"inicio\":\"" + inicio + "\"," + "\"fim\":\"" + fim + "\"" + "}";
	}

	/**
	 * Escape mínimo para não quebrar JSON com aspas.
	 */
	private String safeJson(String s) {
		if (s == null)
			return "";
		return s.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	/**
	 * Lista agendamentos do cliente logado (MVP).
	 *
	 * Params: - from/to: yyyy-MM-dd (janela por data, opcional) - status:
	 * "pendente,confirmado" (opcional)
	 */
	public List<AgendamentoItemResponseDTO> listCliente(Long clienteId, String from, String to, String status) {

		LocalDateTime fromDt = parseDateStart(from);
		LocalDateTime toDt = parseDateEndExclusive(to);
		List<String> statusList = parseStatusList(status);

		var list = agendamentoRepository.findByClienteFiltro(clienteId, fromDt, toDt, statusList);
		return list.stream().map(AgendamentoMapper::toItemDTO).collect(Collectors.toList());
	}

	/**
	 * Lista agendamentos do funcionário logado (MVP).
	 *
	 * Params: - from/to: yyyy-MM-dd (opcional) - status:
	 * "pendente,confirmado,em_atendimento" (opcional)
	 */
	public List<AgendamentoItemResponseDTO> listFuncionario(Long funcionarioId, String from, String to, String status) {

		LocalDateTime fromDt = parseDateStart(from);
		LocalDateTime toDt = parseDateEndExclusive(to);
		List<String> statusList = parseStatusList(status);

		var list = agendamentoRepository.findByFuncionarioFiltro(funcionarioId, fromDt, toDt, statusList);
		return list.stream().map(AgendamentoMapper::toItemDTO).collect(Collectors.toList());
	}

	/**
	 * Consulta pública por protocolo (MVP). Útil para o visitante
	 * confirmar/consultar um agendamento sem login.
	 */
	public AgendamentoItemResponseDTO getPublicByProtocolo(String protocolo) {
		var ag = agendamentoRepository.findByProtocolo(protocolo)
				.orElseThrow(() -> new NotFoundException("Agendamento não encontrado para este protocolo."));
		return AgendamentoMapper.toItemDTO(ag);
	}

	/*
	 * ================================ Helpers de parsing (MVP)
	 * ================================
	 */

	/**
	 * Parse yyyy-MM-dd para início do dia.
	 */
	private LocalDateTime parseDateStart(String date) {
		if (date == null || date.isBlank())
			return null;
		LocalDate d = LocalDate.parse(date.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
		return d.atStartOfDay();
	}

	/**
	 * Parse yyyy-MM-dd para fim exclusivo (dia seguinte 00:00). Isso facilita
	 * queries do tipo [from, to).
	 */
	private LocalDateTime parseDateEndExclusive(String date) {
		if (date == null || date.isBlank())
			return null;
		LocalDate d = LocalDate.parse(date.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
		return d.plusDays(1).atStartOfDay();
	}

	/**
	 * Parse status "pendente,confirmado" em lista.
	 */
	private List<String> parseStatusList(String status) {
		if (status == null || status.isBlank())
			return null;

		List<String> list = Arrays.stream(status.split(",")).map(String::trim).filter(s -> !s.isBlank())
				.collect(Collectors.toList());

		return list.isEmpty() ? null : list;
	}
}
