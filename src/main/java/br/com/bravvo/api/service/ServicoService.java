package br.com.bravvo.api.service;

import br.com.bravvo.api.dto.common.PagedResponseDTO;
import br.com.bravvo.api.dto.servico.ServicoCreateRequestDTO;
import br.com.bravvo.api.dto.servico.ServicoResponseDTO;
import br.com.bravvo.api.dto.servico.ServicoStatusUpdateRequestDTO;
import br.com.bravvo.api.dto.servico.ServicoUpdateRequestDTO;
import br.com.bravvo.api.entity.FuncionarioServico;
import br.com.bravvo.api.entity.FuncionarioServicoId;
import br.com.bravvo.api.entity.Servico;
import br.com.bravvo.api.enums.StatusServico;
import br.com.bravvo.api.exception.BusinessException;
import br.com.bravvo.api.exception.NotFoundException;
import br.com.bravvo.api.mapper.ServicoMapper;
import br.com.bravvo.api.repository.EstabelecimentoUserRepository;
import br.com.bravvo.api.repository.ServicoRepository;
import br.com.bravvo.api.repository.FuncionarioServicoRepository;
import br.com.bravvo.api.repository.UserRepository;
import br.com.bravvo.api.security.TenantContext;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * ========================================================= SERVICE - DOMÍNIO
 * SERVIÇO =========================================================
 *
 * Multi-tenant: - Todas operações utilizam estabelecimentoId via TenantContext.
 *
 * Status no BD: - Campo varchar(20) - Valores persistidos: "ativo" | "inativo"
 *
 * DTO usa enum StatusServico (ATIVO / INATIVO) O service converte enum → String
 * do banco.
 */
@Service
public class ServicoService {

	private static final String STATUS_ATIVO = "ativo";
	private static final String STATUS_INATIVO = "inativo";

	private final ServicoRepository servicoRepository;
	private final FuncionarioServicoRepository funcionarioServicoRepository;
	private final EstabelecimentoUserRepository estabelecimentoUserRepository;

	public ServicoService(ServicoRepository servicoRepository, EstabelecimentoUserRepository estabelecimentoUserRepository, FuncionarioServicoRepository funcionarioServicoRepository) {
		this.servicoRepository = servicoRepository;
	    this.estabelecimentoUserRepository = estabelecimentoUserRepository;
	    this.funcionarioServicoRepository = funcionarioServicoRepository;
	}

	// =========================================================
	// LISTAGEM PAGINADA
	// =========================================================

	/**
	 * Lista serviços do tenant com paginação e filtros opcionais.
	 *
	 * GET /api/servicos
	 *
	 * @param page   Página 1-based
	 * @param limit  Quantidade por página
	 * @param search Filtro por nome
	 * @param status Filtro por status ("ativo" | "inativo")
	 */
	@Transactional(readOnly = true)
	public PagedResponseDTO<ServicoResponseDTO> listPaged(int page, int limit, String search, String status) {
		Long estabelecimentoId = TenantContext.getEstabelecimentoIdOrThrow();

		Pageable pageable = PageRequest.of(Math.max(page - 1, 0), limit, Sort.by("nome").ascending());

		String statusNorm = normalizeStatusOrNull(status);
		String searchNorm = (search == null || search.isBlank()) ? null : search.trim();

		Page<Servico> result = servicoRepository.search(estabelecimentoId, statusNorm, searchNorm, pageable);

		List<ServicoResponseDTO> items = result.getContent().stream().map(ServicoMapper::toResponse).toList();

		return new PagedResponseDTO<>(page, limit, result.getTotalElements(), result.getTotalPages(), items);
	}

	// =========================================================
	// BUSCAR POR ID
	// =========================================================

	@Transactional(readOnly = true)
	public ServicoResponseDTO getById(Long id) {
		Long estabelecimentoId = TenantContext.getEstabelecimentoIdOrThrow();

		Servico servico = servicoRepository.findByIdAndEstabelecimentoId(id, estabelecimentoId)
				.orElseThrow(() -> new NotFoundException("Serviço não encontrado."));

		return ServicoMapper.toResponse(servico);
	}

	// =========================================================
	// CRIAR
	// =========================================================

	@Transactional
	public ServicoResponseDTO create(ServicoCreateRequestDTO dto) {
	    Long estabelecimentoId = TenantContext.getEstabelecimentoIdOrThrow();

	    if (dto == null || dto.getNome() == null || dto.getNome().isBlank()) {
	        throw new BusinessException("Informe o nome do serviço.");
	    }

	    String nomeNormalizado = dto.getNome().trim();

	    if (servicoRepository.existsByEstabelecimentoIdAndNomeIgnoreCase(estabelecimentoId, nomeNormalizado)) {
	        throw new BusinessException("Já existe um serviço cadastrado com esse nome.");
	    }

	    Servico servico = ServicoMapper.toEntity(dto);
	    servico.setEstabelecimentoId(estabelecimentoId);
	    servico.setNome(nomeNormalizado);

	    // Status default
	    if (servico.getStatus() == null || servico.getStatus().isBlank()) {
	        servico.setStatus(STATUS_ATIVO);
	    } else {
	        servico.setStatus(normalizeStatusOrThrow(servico.getStatus()));
	    }

	    Servico saved = servicoRepository.save(servico);

	    // ✅ NOVO: habilita automaticamente este serviço para todos FUNCIONARIOS do estabelecimento
	    habilitarServicoParaTodosUsuarios(estabelecimentoId, saved.getId());

	    return ServicoMapper.toResponse(saved);
	}

	// =========================================================
	// ATUALIZAR
	// =========================================================

	@Transactional
	public ServicoResponseDTO update(Long id, ServicoUpdateRequestDTO dto) {
		Long estabelecimentoId = TenantContext.getEstabelecimentoIdOrThrow();

		Servico servico = servicoRepository.findByIdAndEstabelecimentoId(id, estabelecimentoId)
				.orElseThrow(() -> new NotFoundException("Serviço não encontrado."));

		if (dto == null || dto.getNome() == null || dto.getNome().isBlank()) {
			throw new BusinessException("Informe o nome do serviço.");
		}

		String nomeNormalizado = dto.getNome().trim();

		if (servicoRepository.existsByEstabelecimentoIdAndNomeIgnoreCaseAndIdNot(estabelecimentoId, nomeNormalizado,
				id)) {
			throw new BusinessException("Já existe um serviço cadastrado com esse nome.");
		}

		ServicoMapper.updateEntity(servico, dto);
		servico.setNome(nomeNormalizado);

		// Defende status
		if (servico.getStatus() == null || servico.getStatus().isBlank()) {
			servico.setStatus(STATUS_ATIVO);
		} else {
			servico.setStatus(normalizeStatusOrThrow(servico.getStatus()));
		}

		Servico updated = servicoRepository.save(servico);
		return ServicoMapper.toResponse(updated);
	}

	// =========================================================
	// ATUALIZAR STATUS (ENUM → STRING)
	// =========================================================

	/**
	 * Atualiza apenas o status do serviço.
	 *
	 * PUT /api/servicos/{id}/status
	 *
	 * DTO usa enum StatusServico.
	 */
	@Transactional
	public ServicoResponseDTO updateStatus(Long id, ServicoStatusUpdateRequestDTO dto) {
		Long estabelecimentoId = TenantContext.getEstabelecimentoIdOrThrow();

		Servico servico = servicoRepository.findByIdAndEstabelecimentoId(id, estabelecimentoId)
				.orElseThrow(() -> new NotFoundException("Serviço não encontrado."));

		if (dto == null || dto.getStatus() == null) {
			throw new BusinessException("Informe o status.");
		}

		// Converte enum → String do BD
		String statusDb = convertEnumToDbValue(dto.getStatus());
		servico.setStatus(statusDb);

		Servico updated = servicoRepository.save(servico);
		return ServicoMapper.toResponse(updated);
	}

	// =========================================================
	// DELETE
	// =========================================================

	@Transactional
	public void delete(Long id) {
		Long estabelecimentoId = TenantContext.getEstabelecimentoIdOrThrow();

		if (!servicoRepository.existsByIdAndEstabelecimentoId(id, estabelecimentoId)) {
			throw new NotFoundException("Serviço não encontrado.");
		}

		servicoRepository.deleteById(id);
	}

	// =========================================================
	// HELPERS
	// =========================================================

	/**
	 * Normaliza status vindo como String.
	 */
	private String normalizeStatusOrNull(String raw) {
		if (raw == null || raw.isBlank())
			return null;

		String s = raw.trim().toLowerCase();

		if (Objects.equals(s, STATUS_ATIVO))
			return STATUS_ATIVO;
		if (Objects.equals(s, STATUS_INATIVO))
			return STATUS_INATIVO;

		return null;
	}

	private String normalizeStatusOrThrow(String raw) {
		if (raw == null || raw.isBlank()) {
			throw new BusinessException("Status inválido.");
		}

		String s = raw.trim().toLowerCase();

		if (Objects.equals(s, STATUS_ATIVO))
			return STATUS_ATIVO;
		if (Objects.equals(s, STATUS_INATIVO))
			return STATUS_INATIVO;

		throw new BusinessException("Status inválido. Use 'ativo' ou 'inativo'.");
	}

	/**
	 * Converte enum StatusServico para valor persistido no BD.
	 */
	private String convertEnumToDbValue(StatusServico status) {
		return switch (status) {
		case ATIVO -> STATUS_ATIVO;
		case INATIVO -> STATUS_INATIVO;
		};
	}
	
	/**
	 * Habilita um serviço recém-criado para todos os usuários ativos do estabelecimento (tenant-safe).
	 *
	 * ATENÇÃO: Isso cria vínculo em funcionario_servicos para QUALQUER perfil do tenant.
	 * Se no futuro isso causar efeitos colaterais, troque para filtrar apenas FUNCIONARIO.
	 */
	private void habilitarServicoParaTodosUsuarios(Long estabelecimentoId, Long servicoId) {

	    List<Long> userIds = estabelecimentoUserRepository
	            .findActiveUserIdsByEstabelecimentoId(estabelecimentoId);

	    if (userIds == null || userIds.isEmpty()) {
	        return;
	    }

	    List<FuncionarioServico> vinculos = new ArrayList<>();

	    for (Long userId : userIds) {
	        if (userId == null) continue;

	        FuncionarioServico fs = new FuncionarioServico();
	        fs.setId(new FuncionarioServicoId(userId, servicoId));
	        vinculos.add(fs);
	    }

	    funcionarioServicoRepository.saveAll(vinculos);
	}
}