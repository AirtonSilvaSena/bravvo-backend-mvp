package br.com.bravvo.api.service;

import br.com.bravvo.api.dto.funcionario.*;
import br.com.bravvo.api.entity.*;
import br.com.bravvo.api.enums.PerfilUser;
import br.com.bravvo.api.exception.BusinessException;
import br.com.bravvo.api.exception.ForbiddenException;
import br.com.bravvo.api.exception.NotFoundException;
import br.com.bravvo.api.repository.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service self-service do funcionário.
 *
 * Regras: - Apenas FUNCIONARIO ativo pode acessar - Agenda é atualizada como
 * "update completo" (7 dias) - Bloqueios podem ser criados/removidos pelo
 * próprio funcionário
 */
@Service
public class FuncionarioAgendaService {

	private final UserRepository userRepository;
	private final FuncionarioAgendaRepository agendaRepository;
	private final FuncionarioBloqueioRepository bloqueioRepository;

	public FuncionarioAgendaService(UserRepository userRepository, FuncionarioAgendaRepository agendaRepository,
			FuncionarioBloqueioRepository bloqueioRepository) {
		this.userRepository = userRepository;
		this.agendaRepository = agendaRepository;
		this.bloqueioRepository = bloqueioRepository;
	}

	// =========================
	// Agenda
	// =========================

	/**
	 * Retorna a agenda semanal do funcionário logado. Sempre retorna os 7 dias
	 * (1..7).
	 *
	 * Se não existir configuração no banco para um dia, retornamos ativo=false.
	 */
	public List<FuncionarioAgendaDayResponseDTO> getMyAgenda() {
		Long funcionarioId = getFuncionarioLogadoIdObrigatorio();

		List<FuncionarioAgenda> rows = agendaRepository.findByIdFuncionarioId(funcionarioId);

		Map<Integer, FuncionarioAgenda> map = rows.stream()
				.collect(Collectors.toMap(r -> r.getId().getDiaSemana(), r -> r));

		List<FuncionarioAgendaDayResponseDTO> resp = new ArrayList<>();

		for (int dia = 1; dia <= 7; dia++) {
			FuncionarioAgenda row = map.get(dia);

			FuncionarioAgendaDayResponseDTO dto = new FuncionarioAgendaDayResponseDTO();
			dto.setDiaSemana(dia);

			if (row == null) {
				dto.setAtivo(false);
			} else {
				dto.setAtivo(Boolean.TRUE.equals(row.getAtivo()));
				dto.setInicio1(row.getInicio1());
				dto.setFim1(row.getFim1());
				dto.setInicio2(row.getInicio2());
				dto.setFim2(row.getFim2());
			}

			resp.add(dto);
		}

		return resp;
	}

	/**
	 * Atualização completa da agenda.
	 *
	 * MVP: - exige envio dos 7 dias (1..7) - valida janelas conforme regra (2
	 * janelas + almoço)
	 */
	@Transactional
	public List<FuncionarioAgendaDayResponseDTO> updateMyAgenda(FuncionarioAgendaUpdateRequestDTO request) {
		Long funcionarioId = getFuncionarioLogadoIdObrigatorio();

		if (request.getAgenda() == null || request.getAgenda().size() != 7) {
			throw new BusinessException("Envie a agenda completa com 7 dias (1..7).");
		}

		// valida duplicidade e se todos os dias existem
		Set<Integer> dias = new HashSet<>();
		for (FuncionarioAgendaDayRequestDTO d : request.getAgenda()) {
			if (!dias.add(d.getDiaSemana())) {
				throw new BusinessException("diaSemana duplicado na agenda.");
			}
		}
		for (int i = 1; i <= 7; i++) {
			if (!dias.contains(i)) {
				throw new BusinessException("Agenda incompleta: falta diaSemana " + i + ".");
			}
		}

		// upsert por dia
		for (FuncionarioAgendaDayRequestDTO d : request.getAgenda()) {

			validateDayWindows(d);

			FuncionarioAgendaId id = new FuncionarioAgendaId(funcionarioId, d.getDiaSemana());
			FuncionarioAgenda row = agendaRepository.findById(id).orElseGet(FuncionarioAgenda::new);

			row.setId(id);
			row.setAtivo(Boolean.TRUE.equals(d.getAtivo()));

			// dia off -> limpa horários
			if (!Boolean.TRUE.equals(d.getAtivo())) {
				row.setInicio1(null);
				row.setFim1(null);
				row.setInicio2(null);
				row.setFim2(null);
			} else {
				row.setInicio1(d.getInicio1());
				row.setFim1(d.getFim1());
				row.setInicio2(d.getInicio2());
				row.setFim2(d.getFim2());
			}

			agendaRepository.save(row);
		}

		return getMyAgenda();
	}

	/**
	 * Regras das janelas: - Se ativo=false: ok - Se ativo=true: - inicio1 e fim1
	 * obrigatórios e inicio1 < fim1 - janela2 opcional, mas se vier deve ter
	 * inicio2+fim2 e inicio2 < fim2 - janela2 não pode sobrepor janela1: fim1 <=
	 * inicio2
	 */
	private void validateDayWindows(FuncionarioAgendaDayRequestDTO d) {
		if (!Boolean.TRUE.equals(d.getAtivo())) {
			return;
		}

		if (d.getInicio1() == null || d.getFim1() == null) {
			throw new BusinessException("Dia " + d.getDiaSemana() + ": informe inicio1 e fim1.");
		}
		if (!d.getInicio1().isBefore(d.getFim1())) {
			throw new BusinessException("Dia " + d.getDiaSemana() + ": inicio1 deve ser menor que fim1.");
		}

		boolean hasInicio2 = d.getInicio2() != null;
		boolean hasFim2 = d.getFim2() != null;

		if (hasInicio2 != hasFim2) {
			throw new BusinessException("Dia " + d.getDiaSemana() + ": janela2 deve ter inicio2 e fim2.");
		}

		if (hasInicio2) {
			if (!d.getInicio2().isBefore(d.getFim2())) {
				throw new BusinessException("Dia " + d.getDiaSemana() + ": inicio2 deve ser menor que fim2.");
			}
			if (d.getFim1().isAfter(d.getInicio2())) {
				throw new BusinessException(
						"Dia " + d.getDiaSemana() + ": janela2 não pode sobrepor a janela1 (fim1 <= inicio2).");
			}
		}
	}

	// =========================
	// Bloqueios
	// =========================

	public List<FuncionarioBloqueioResponseDTO> listMyBloqueios() {
		Long funcionarioId = getFuncionarioLogadoIdObrigatorio();

		return bloqueioRepository.findByFuncionarioIdOrderByStartDtAsc(funcionarioId).stream().map(this::toResponse)
				.toList();
	}

	@Transactional
	public FuncionarioBloqueioResponseDTO createMyBloqueio(FuncionarioBloqueioCreateRequestDTO dto) {
		Long funcionarioId = getFuncionarioLogadoIdObrigatorio();

		if (dto.getStartDt() == null || dto.getEndDt() == null) {
			throw new BusinessException("Informe startDt e endDt.");
		}
		if (!dto.getStartDt().isBefore(dto.getEndDt())) {
			throw new BusinessException("startDt deve ser menor que endDt.");
		}

		FuncionarioBloqueio b = new FuncionarioBloqueio();
		b.setFuncionarioId(funcionarioId);
		b.setStartDt(dto.getStartDt());
		b.setEndDt(dto.getEndDt());
		b.setMotivo(dto.getMotivo());

		return toResponse(bloqueioRepository.save(b));
	}

	@Transactional
	public void deleteMyBloqueio(Long id) {
		Long funcionarioId = getFuncionarioLogadoIdObrigatorio();

		FuncionarioBloqueio b = bloqueioRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Bloqueio não encontrado."));

		if (!Objects.equals(b.getFuncionarioId(), funcionarioId)) {
			throw new ForbiddenException("Você não tem permissão para remover este bloqueio.");
		}

		bloqueioRepository.delete(b);
	}

	private FuncionarioBloqueioResponseDTO toResponse(FuncionarioBloqueio b) {
		FuncionarioBloqueioResponseDTO r = new FuncionarioBloqueioResponseDTO();
		r.setId(b.getId());
		r.setStartDt(b.getStartDt());
		r.setEndDt(b.getEndDt());
		r.setMotivo(b.getMotivo());
		return r;
	}

	// =========================
	// Auth helper
	// =========================

	/**
	 * Retorna o ID do funcionário logado (perfil FUNCIONARIO e ativo).
	 *
	 * No seu projeto, JwtAuthenticationFilter coloca UserDetails como principal,
	 * então auth.getName() retorna o email.
	 */
	private Long getFuncionarioLogadoIdObrigatorio() {
		var auth = SecurityContextHolder.getContext().getAuthentication();

		if (auth == null || !auth.isAuthenticated()) {
			throw new ForbiddenException("Usuário não autenticado.");
		}

		String email = auth.getName();

		var user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ForbiddenException("Usuário não encontrado."));

		if (!Boolean.TRUE.equals(user.getAtivo())) {
			throw new ForbiddenException("Usuário inativo.");
		}

		if (user.getPerfil() != PerfilUser.FUNCIONARIO) {
			throw new ForbiddenException("Acesso permitido apenas para FUNCIONARIO.");
		}

		return user.getId();
	}
}
