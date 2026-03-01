package br.com.bravvo.api.service;

import br.com.bravvo.api.dto.agendamento.*;
import br.com.bravvo.api.entity.Agendamento;
import br.com.bravvo.api.entity.EstabelecimentoUser;
import br.com.bravvo.api.entity.FuncionarioPrefs;
import br.com.bravvo.api.entity.Protocolo;
import br.com.bravvo.api.entity.User;
import br.com.bravvo.api.exception.BusinessException;
import br.com.bravvo.api.exception.ForbiddenException;
import br.com.bravvo.api.exception.NotFoundException;
import br.com.bravvo.api.mapper.AgendamentoMapper;
import br.com.bravvo.api.repository.*;
import br.com.bravvo.api.security.TenantContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core único de criação e consulta de agendamentos (MVP).
 *
 * MULTI-TENANT (real): - TODO acesso a agendamento deve filtrar por
 * estabelecimentoId. - protocolo NÃO é mais único global -> é único por
 * (estabelecimento_id, protocolo). - endpoints autenticados pegam
 * estabelecimentoId via TenantContext. - endpoint público deve resolver
 * estabelecimentoId (via slug) e passar aqui.
 */
@Service
public class AgendamentoService {

	private final AgendamentoRepository agendamentoRepository;
	private final ServicoRepository servicoRepository;
	private final UserRepository userRepository;
	private final EstabelecimentoUserRepository estabelecimentoUserRepository;
	private final FuncionarioServicoRepository funcionarioServicoRepository;
	private final FuncionarioPrefsRepository funcionarioPrefsRepository;
	private final ProtocoloRepository protocoloRepository;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

	public AgendamentoService(AgendamentoRepository agendamentoRepository, ServicoRepository servicoRepository,
			UserRepository userRepository, EstabelecimentoUserRepository estabelecimentoUserRepository,
			FuncionarioServicoRepository funcionarioServicoRepository,
			FuncionarioPrefsRepository funcionarioPrefsRepository, ProtocoloRepository protocoloRepository) {
		this.agendamentoRepository = agendamentoRepository;
		this.servicoRepository = servicoRepository;
		this.userRepository = userRepository;
		this.estabelecimentoUserRepository = estabelecimentoUserRepository;
		this.funcionarioServicoRepository = funcionarioServicoRepository;
		this.funcionarioPrefsRepository = funcionarioPrefsRepository;
		this.protocoloRepository = protocoloRepository;
	}

	// ============================
	// Entradas (Controllers)
	// ============================

	/**
	 * PÚBLICO (visitante, sem JWT)
	 *
	 * Obrigatório receber estabelecimentoId (resolvido por slug antes).
	 */
	@Transactional
	public AgendamentoCreateResponseDTO createPublic(Long estabelecimentoId, PublicAgendamentoCreateRequestDTO req) {
		LocalDateTime inicio = parseInicio(req.getData(), req.getHora());

		return createCore(estabelecimentoId, req.getServicoId(), req.getFuncionarioId(), null, req.getClienteNome(),
				req.getClienteTelefone(), req.getClienteEmail(), req.getObservacoes(), inicio);
	}

	/**
	 * CLIENTE LOGADO (JWT)
	 *
	 * Segurança: - valida vínculo no tenant - força clienteId ser o do token
	 */
	@Transactional
	public AgendamentoCreateResponseDTO createClienteLogado(Long clienteIdFromJwt,
			ClienteAgendamentoCreateRequestDTO req) {
		Long estabelecimentoId = TenantContext.getEstabelecimentoIdOrThrow();

		// reforço: controller deve passar o mesmo userId do token
		Long tokenUserId = TenantContext.getUserIdOrThrow();
		if (!Objects.equals(tokenUserId, clienteIdFromJwt)) {
			throw new ForbiddenException("Usuário inválido no contexto do token.");
		}

		LocalDateTime inicio = parseInicio(req.getData(), req.getHora());

		User cliente = userRepository.findById(clienteIdFromJwt)
				.orElseThrow(() -> new NotFoundException("Cliente não encontrado."));

		if (!Boolean.TRUE.equals(cliente.getAtivo())) {
			throw new ForbiddenException("Usuário inativo.");
		}

		EstabelecimentoUser linkCliente = getLinkOrThrow(estabelecimentoId, cliente.getId());

		if (Boolean.FALSE.equals(linkCliente.getAtivo())) {
			throw new ForbiddenException("Usuário sem permissão (vínculo inativo).");
		}

		if (linkCliente.getPerfil() != br.com.bravvo.api.enums.PerfilUser.CLIENTE) {
			throw new ForbiddenException("Somente CLIENTE pode criar agendamento neste endpoint.");
		}

		return createCore(estabelecimentoId, req.getServicoId(), req.getFuncionarioId(), cliente.getId(),
				cliente.getNome(), cliente.getTelefone(), cliente.getEmail(), req.getObservacoes(), inicio);
	}

	/**
	 * FUNCIONÁRIO LOGADO (JWT)
	 *
	 * - funcionárioId vem do token (ou repassado pelo controller). - pode agendar
	 * para cliente cadastrado (clienteId) OU visitante (nome/telefone).
	 */
	@Transactional
	public AgendamentoCreateResponseDTO createFuncionario(Long funcionarioIdFromJwt,
			FuncionarioAgendamentoCreateRequestDTO req) {
		Long estabelecimentoId = TenantContext.getEstabelecimentoIdOrThrow();

		// reforço de segurança
		Long tokenUserId = TenantContext.getUserIdOrThrow();
		if (!Objects.equals(tokenUserId, funcionarioIdFromJwt)) {
			throw new ForbiddenException("Usuário inválido no contexto do token.");
		}

		LocalDateTime inicio = parseInicio(req.getData(), req.getHora());

		// valida caller FUNCIONARIO no tenant
		User funcionarioLogado = userRepository.findById(funcionarioIdFromJwt)
				.orElseThrow(() -> new NotFoundException("Funcionário não encontrado."));

		if (!Boolean.TRUE.equals(funcionarioLogado.getAtivo())) {
			throw new ForbiddenException("Usuário inativo.");
		}

		EstabelecimentoUser linkCaller = getLinkOrThrow(estabelecimentoId, funcionarioLogado.getId());

		if (Boolean.FALSE.equals(linkCaller.getAtivo())) {
			throw new ForbiddenException("Usuário sem permissão (vínculo inativo).");
		}

		if (linkCaller.getPerfil() != br.com.bravvo.api.enums.PerfilUser.FUNCIONARIO) {
			throw new ForbiddenException("Somente FUNCIONARIO pode criar agendamento neste endpoint.");
		}

		Long clienteId = req.getClienteId();
		String nome = req.getClienteNome();
		String tel = req.getClienteTelefone();
		String email = req.getClienteEmail();

		// Cenário A: cliente cadastrado
		if (clienteId != null) {
			User cliente = userRepository.findById(clienteId)
					.orElseThrow(() -> new NotFoundException("Cliente não encontrado."));

			if (!Boolean.TRUE.equals(cliente.getAtivo())) {
				throw new BusinessException("Cliente está inativo.");
			}

			EstabelecimentoUser linkCliente = getLinkOrThrow(estabelecimentoId, cliente.getId());

			if (Boolean.FALSE.equals(linkCliente.getAtivo())) {
				throw new BusinessException("Cliente sem permissão (vínculo inativo).");
			}
			if (linkCliente.getPerfil() != br.com.bravvo.api.enums.PerfilUser.CLIENTE) {
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

		return createCore(estabelecimentoId, req.getServicoId(), funcionarioIdFromJwt, clienteId, nome, tel, email,
				req.getObservacoes(), inicio);
	}

	// ============================
	// Core único (tenant-safe)
	// ============================

	private AgendamentoCreateResponseDTO createCore(Long estabelecimentoId, Long servicoId, Long funcionarioId,
			Long clienteId, String clienteNome, String clienteTelefone, String clienteEmail, String observacoes,
			LocalDateTime inicio) {
		if (estabelecimentoId == null) {
			throw new BusinessException("Estabelecimento inválido.");
		}

		// 1) valida serviço
		var servico = servicoRepository.findById(servicoId)
				.orElseThrow(() -> new NotFoundException("Serviço não encontrado."));

		// ✅ CORREÇÃO: status do serviço agora é String (schema do BD:
		// "ativo"/"inativo")
		if (!isServicoAtivo(servico.getStatus())) {
			throw new BusinessException("Serviço está inativo.");
		}

		// 2) valida funcionário (user ativo + vínculo FUNCIONARIO no tenant)
		var funcionario = userRepository.findById(funcionarioId)
				.orElseThrow(() -> new NotFoundException("Funcionário não encontrado."));

		if (!Boolean.TRUE.equals(funcionario.getAtivo())) {
			throw new BusinessException("Funcionário está inativo.");
		}

		EstabelecimentoUser linkFunc = getLinkOrThrow(estabelecimentoId, funcionarioId);

		if (Boolean.FALSE.equals(linkFunc.getAtivo())) {
			throw new BusinessException("Funcionário sem permissão (vínculo inativo).");
		}
		if (linkFunc.getPerfil() != br.com.bravvo.api.enums.PerfilUser.FUNCIONARIO) {
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

		// 5) conflito final (TENANT-SAFE)
		var conflitos = agendamentoRepository.findBlockingOverlapping(estabelecimentoId, funcionarioId, inicio, fim);
		if (!conflitos.isEmpty()) {
			throw new BusinessException("Horário indisponível. Escolha outro horário.");
		}

		// 6) gera protocolo único POR TENANT
		String protocolo = generateUniqueProtocolo(estabelecimentoId);

		// 6.1) registra protocolo (mantido como está)
		Protocolo p = new Protocolo();
		p.setCodigo(protocolo);
		p.setTipo("agendamento");
		p.setDadosJson(buildProtocoloDadosJson(servicoId, funcionarioId, clienteId, clienteNome, clienteTelefone,
				clienteEmail, inicio, fim));
		protocoloRepository.save(p);

		// 7) persiste agendamento (TENANT-SAFE)
		Agendamento ag = new Agendamento();
		ag.setEstabelecimentoId(estabelecimentoId);
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
	// Helpers (serviço)
	// ============================

	/**
	 * Status do serviço no schema atual: - "ativo" / "inativo" (varchar(20))
	 */
	private boolean isServicoAtivo(String statusDb) {
		if (statusDb == null)
			return false;
		return "ativo".equalsIgnoreCase(statusDb.trim());
	}

	// ============================
	// Helpers (tenant/vínculo)
	// ============================

	private EstabelecimentoUser getLinkOrThrow(Long estabelecimentoId, Long userId) {
		return estabelecimentoUserRepository.findByEstabelecimentoIdAndUserId(estabelecimentoId, userId)
				.orElseThrow(() -> new ForbiddenException("Vínculo do usuário com o estabelecimento não encontrado."));
	}

	// ============================
	// Helpers (parsing)
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

			if (duracaoNode.isInt())
				return duracaoNode.asInt();
			return duracaoPadrao;
		} catch (Exception e) {
			return duracaoPadrao;
		}
	}

	private String generateUniqueProtocolo(Long estabelecimentoId) {
		String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
		Random r = new Random();

		for (int i = 0; i < 10; i++) {
			String suffix = String.format("%06d", r.nextInt(1_000_000));
			String proto = "BRV-" + date + "-" + suffix;

			boolean existsAg = agendamentoRepository.existsByEstabelecimentoIdAndProtocolo(estabelecimentoId, proto);
			boolean existsProto = protocoloRepository.existsByCodigo(proto);

			if (!existsAg && !existsProto)
				return proto;
		}

		throw new BusinessException("Não foi possível gerar protocolo. Tente novamente.");
	}

	private String buildProtocoloDadosJson(Long servicoId, Long funcionarioId, Long clienteId, String clienteNome,
			String clienteTelefone, String clienteEmail, LocalDateTime inicio, LocalDateTime fim) {
		return "{" + "\"servicoId\":" + servicoId + "," + "\"funcionarioId\":" + funcionarioId + "," + "\"clienteId\":"
				+ (clienteId == null ? "null" : clienteId) + "," + "\"clienteNome\":\"" + safeJson(clienteNome) + "\","
				+ "\"clienteTelefone\":\"" + safeJson(clienteTelefone) + "\"," + "\"clienteEmail\":\""
				+ safeJson(clienteEmail) + "\"," + "\"inicio\":\"" + inicio + "\"," + "\"fim\":\"" + fim + "\"" + "}";
	}

	private String safeJson(String s) {
		if (s == null)
			return "";
		return s.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	// ============================
	// Listagens / Consultas (tenant-safe)
	// ============================

	public List<AgendamentoItemResponseDTO> listCliente(String from, String to, String status) {
		Long estabelecimentoId = TenantContext.getEstabelecimentoIdOrThrow();
		Long clienteId = TenantContext.getUserIdOrThrow();

		EstabelecimentoUser link = getLinkOrThrow(estabelecimentoId, clienteId);
		if (Boolean.FALSE.equals(link.getAtivo()) || link.getPerfil() != br.com.bravvo.api.enums.PerfilUser.CLIENTE) {
			throw new ForbiddenException("Acesso negado.");
		}

		LocalDateTime fromDt = parseDateStart(from);
		LocalDateTime toDt = parseDateEndExclusive(to);
		List<String> statusList = parseStatusList(status);

		var list = agendamentoRepository.findByClienteFiltro(estabelecimentoId, clienteId, fromDt, toDt, statusList);
		return list.stream().map(AgendamentoMapper::toItemDTO).collect(Collectors.toList());
	}

	public List<AgendamentoItemResponseDTO> listFuncionario(String from, String to, String status) {
		Long estabelecimentoId = TenantContext.getEstabelecimentoIdOrThrow();
		Long funcionarioId = TenantContext.getUserIdOrThrow();

		EstabelecimentoUser link = getLinkOrThrow(estabelecimentoId, funcionarioId);
		if (Boolean.FALSE.equals(link.getAtivo())
				|| link.getPerfil() != br.com.bravvo.api.enums.PerfilUser.FUNCIONARIO) {
			throw new ForbiddenException("Acesso negado.");
		}

		LocalDateTime fromDt = parseDateStart(from);
		LocalDateTime toDt = parseDateEndExclusive(to);
		List<String> statusList = parseStatusList(status);

		var list = agendamentoRepository.findByFuncionarioFiltro(estabelecimentoId, funcionarioId, fromDt, toDt,
				statusList);
		return list.stream().map(AgendamentoMapper::toItemDTO).collect(Collectors.toList());
	}

	public AgendamentoItemResponseDTO getPublicByProtocolo(Long estabelecimentoId, String protocolo) {
		if (estabelecimentoId == null)
			throw new BusinessException("Estabelecimento inválido.");
		if (protocolo == null || protocolo.isBlank())
			throw new BusinessException("Protocolo inválido.");

		var ag = agendamentoRepository.findByEstabelecimentoIdAndProtocolo(estabelecimentoId, protocolo.trim())
				.orElseThrow(() -> new NotFoundException("Agendamento não encontrado para este protocolo."));

		return AgendamentoMapper.toItemDTO(ag);
	}

	private LocalDateTime parseDateStart(String date) {
		if (date == null || date.isBlank())
			return null;
		LocalDate d = LocalDate.parse(date.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
		return d.atStartOfDay();
	}

	private LocalDateTime parseDateEndExclusive(String date) {
		if (date == null || date.isBlank())
			return null;
		LocalDate d = LocalDate.parse(date.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
		return d.plusDays(1).atStartOfDay();
	}

	private List<String> parseStatusList(String status) {
		if (status == null || status.isBlank())
			return null;

		List<String> list = Arrays.stream(status.split(",")).map(String::trim).filter(s -> !s.isBlank())
				.collect(Collectors.toList());

		return list.isEmpty() ? null : list;
	}
}