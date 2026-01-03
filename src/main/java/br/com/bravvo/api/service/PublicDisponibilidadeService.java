package br.com.bravvo.api.service;

import br.com.bravvo.api.dto.publico.PublicDisponibilidadeResponseDTO;
import br.com.bravvo.api.entity.FuncionarioAgenda;
import br.com.bravvo.api.entity.FuncionarioPrefs;
import br.com.bravvo.api.enums.PerfilUser;
import br.com.bravvo.api.enums.StatusServico;
import br.com.bravvo.api.exception.NotFoundException;
import br.com.bravvo.api.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service responsável por calcular a disponibilidade pública:
 *
 * Considera: - agenda semanal (funcionario_agenda) - bloqueios pontuais
 * (funcionario_bloqueios) - agendamentos existentes (agendamentos) - duração
 * resolvida (funcionario_prefs -> fallback servico.duracaoMin)
 *
 * Endpoint consumidor: GET /api/public/disponibilidade
 */
@Service
public class PublicDisponibilidadeService {

	private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

	private final ServicoRepository servicoRepository;
	private final UserRepository userRepository;
	private final FuncionarioServicoRepository funcionarioServicoRepository;
	private final FuncionarioPrefsRepository funcionarioPrefsRepository;
	private final FuncionarioAgendaRepository funcionarioAgendaRepository;
	private final FuncionarioBloqueioRepository funcionarioBloqueioRepository;
	private final AgendamentoRepository agendamentoRepository;

	private final ObjectMapper objectMapper;

	public PublicDisponibilidadeService(ServicoRepository servicoRepository, UserRepository userRepository,
			FuncionarioServicoRepository funcionarioServicoRepository,
			FuncionarioPrefsRepository funcionarioPrefsRepository,
			FuncionarioAgendaRepository funcionarioAgendaRepository,
			FuncionarioBloqueioRepository funcionarioBloqueioRepository, AgendamentoRepository agendamentoRepository,
			ObjectMapper objectMapper) {
		this.servicoRepository = servicoRepository;
		this.userRepository = userRepository;
		this.funcionarioServicoRepository = funcionarioServicoRepository;
		this.funcionarioPrefsRepository = funcionarioPrefsRepository;
		this.funcionarioAgendaRepository = funcionarioAgendaRepository;
		this.funcionarioBloqueioRepository = funcionarioBloqueioRepository;
		this.agendamentoRepository = agendamentoRepository;
		this.objectMapper = objectMapper;
	}

	public PublicDisponibilidadeResponseDTO getDisponibilidade(Long servicoId, Long funcionarioId, LocalDate data) {

		// =========================
		// 1) Valida serviço (existe e ATIVO)
		// =========================
		var servico = servicoRepository.findById(servicoId)
				.orElseThrow(() -> new NotFoundException("Serviço não encontrado."));

		if (servico.getStatus() != StatusServico.ATIVO) {
			// catálogo público: "indisponível" -> lista vazia
			return new PublicDisponibilidadeResponseDTO(data, servico.getDuracaoMin(), List.of());
		}

		// =========================
		// 2) Valida funcionário (existe, ativo e perfil FUNCIONARIO)
		// =========================
		var funcionario = userRepository.findById(funcionarioId)
				.orElseThrow(() -> new NotFoundException("Funcionário não encontrado."));

		if (!Boolean.TRUE.equals(funcionario.getAtivo())) {
			return new PublicDisponibilidadeResponseDTO(data, servico.getDuracaoMin(), List.of());
		}
		if (funcionario.getPerfil() != PerfilUser.FUNCIONARIO) {
			return new PublicDisponibilidadeResponseDTO(data, servico.getDuracaoMin(), List.of());
		}

		// =========================
		// 3) Valida se serviço está habilitado para o funcionário
		// =========================
		boolean habilitado = funcionarioServicoRepository.existsByIdFuncionarioIdAndIdServicoId(funcionarioId,
				servicoId);
		if (!habilitado) {
			return new PublicDisponibilidadeResponseDTO(data, servico.getDuracaoMin(), List.of());
		}

		// =========================
		// 4) Resolve duração do serviço (prefs_json -> fallback duração padrão)
		// =========================
		int duracaoMin = resolveDuracaoMin(funcionarioId, servicoId, servico.getDuracaoMin());

		// =========================
		// 5) Busca agenda do dia (dia_semana 1..7)
		// =========================
		int diaSemana = data.getDayOfWeek().getValue(); // 1=Seg ... 7=Dom (igual sua doc)

		FuncionarioAgenda agenda = funcionarioAgendaRepository
				.findById(new br.com.bravvo.api.entity.FuncionarioAgendaId(funcionarioId, diaSemana)).orElse(null);

		if (agenda == null || !Boolean.TRUE.equals(agenda.getAtivo())) {
			return new PublicDisponibilidadeResponseDTO(data, duracaoMin, List.of());
		}

		// monta janelas do dia (até 2)
		List<Intervalo> janelas = new ArrayList<>();
		addJanelaIfValid(janelas, data, agenda.getInicio1(), agenda.getFim1());
		addJanelaIfValid(janelas, data, agenda.getInicio2(), agenda.getFim2());

		if (janelas.isEmpty()) {
			return new PublicDisponibilidadeResponseDTO(data, duracaoMin, List.of());
		}

		// =========================
		// 6) Carrega indisponibilidades do dia: bloqueios + agendamentos
		// =========================
		LocalDateTime from = data.atStartOfDay();
		LocalDateTime to = data.plusDays(1).atStartOfDay();

		var bloqueios = funcionarioBloqueioRepository.findOverlapping(funcionarioId, from, to);
		var agendamentos = agendamentoRepository.findBlockingOverlapping(funcionarioId, from, to);

		List<Intervalo> indisponiveis = new ArrayList<>();

		bloqueios.forEach(b -> indisponiveis.add(new Intervalo(b.getStartDt(), b.getEndDt())));
		agendamentos.forEach(a -> indisponiveis.add(new Intervalo(a.getInicio(), a.getFim())));

		// =========================
		// 7) Gera slots (step 15m) e filtra conflito
		// =========================
		Duration duracao = Duration.ofMinutes(duracaoMin);
		List<String> horarios = new ArrayList<>();

		for (Intervalo janela : janelas) {

			// primeiro slot começa no início da janela
			LocalDateTime slotStart = janela.start;

			// último início permitido = fimJanela - duracao
			LocalDateTime lastStart = janela.end.minus(duracao);

			while (!slotStart.isAfter(lastStart)) {

				LocalDateTime slotEnd = slotStart.plus(duracao);

				if (!intersectsAny(slotStart, slotEnd, indisponiveis)) {
					horarios.add(slotStart.toLocalTime().format(HHMM));
				}

				slotStart = slotStart.plusMinutes(duracaoMin);
			}
		}

		return new PublicDisponibilidadeResponseDTO(data, duracaoMin, horarios);
	}

	// ==========================================================
	// Auxiliares
	// ==========================================================

	private void addJanelaIfValid(List<Intervalo> janelas, LocalDate data, LocalTime inicio, LocalTime fim) {
		if (inicio == null || fim == null)
			return;
		if (!fim.isAfter(inicio))
			return;
		janelas.add(new Intervalo(LocalDateTime.of(data, inicio), LocalDateTime.of(data, fim)));
	}

	/**
	 * Resolve duração do serviço para o funcionário (mesma ideia que você já usa).
	 *
	 * Formato: { "servicos": { "16": { "duracaoMin": 60 } } }
	 */
	private int resolveDuracaoMin(Long funcionarioId, Long servicoId, Integer fallback) {

		Optional<FuncionarioPrefs> prefsOpt = funcionarioPrefsRepository.findById(funcionarioId);

		if (prefsOpt.isEmpty() || prefsOpt.get().getPrefsJson() == null || prefsOpt.get().getPrefsJson().isBlank()) {
			return fallback;
		}

		try {
			JsonNode root = objectMapper.readTree(prefsOpt.get().getPrefsJson());

			JsonNode duracaoNode = root.path("servicos").path(String.valueOf(servicoId)).path("duracaoMin");

			if (duracaoNode != null && duracaoNode.isInt()) {
				int v = duracaoNode.asInt();
				return v >= 1 ? v : fallback;
			}

			return fallback;
		} catch (Exception e) {
			// IMPORTANTE: disponibilidade pública não pode quebrar por prefs_json inválido
			return fallback;
		}
	}

	private boolean intersectsAny(LocalDateTime start, LocalDateTime end, List<Intervalo> intervals) {
		for (Intervalo i : intervals) {
			if (i.intersects(start, end))
				return true;
		}
		return false;
	}

	/**
	 * Intervalo [start, end) para checar interseção.
	 */
	private static class Intervalo {
		private final LocalDateTime start;
		private final LocalDateTime end;

		private Intervalo(LocalDateTime start, LocalDateTime end) {
			this.start = start;
			this.end = end;
		}

		private boolean intersects(LocalDateTime otherStart, LocalDateTime otherEnd) {
			return start.isBefore(otherEnd) && end.isAfter(otherStart);
		}
	}
}
