package br.com.bravvo.api.service;

import br.com.bravvo.api.dto.common.PagedResponseDTO;
import br.com.bravvo.api.dto.servico.ServicoCreateRequestDTO;
import br.com.bravvo.api.dto.servico.ServicoResponseDTO;
import br.com.bravvo.api.dto.servico.ServicoStatusUpdateRequestDTO;
import br.com.bravvo.api.dto.servico.ServicoUpdateRequestDTO;
import br.com.bravvo.api.entity.Servico;
import br.com.bravvo.api.enums.StatusServico;
import br.com.bravvo.api.exception.BusinessException;
import br.com.bravvo.api.exception.NotFoundException;
import br.com.bravvo.api.mapper.ServicoMapper;
import br.com.bravvo.api.repository.ServicoRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service responsável pelas regras de negócio do domínio Serviço.
 *
 * Observações importantes:
 * - Não controla permissão por perfil (isso fica no Controller via @PreAuthorize)
 * - Trabalha sempre com DTOs e Entities via Mapper
 * - Centraliza validações e exceções
 */
@Service
public class ServicoService {

    private final ServicoRepository servicoRepository;

    public ServicoService(ServicoRepository servicoRepository) {
        this.servicoRepository = servicoRepository;
    }

    // =========================================================
    // LISTAGEM PAGINADA
    // =========================================================

    /**
     * Retorna lista paginada de serviços.
     *
     * Endpoint:
     *   GET /api/servicos
     *
     * Query params:
     * - page (1-based)
     * - limit
     * - search (nome)
     * - status (ATIVO | INATIVO)
     */
    @Transactional(readOnly = true)
    public PagedResponseDTO<ServicoResponseDTO> listPaged(
            int page,
            int limit,
            String search,
            StatusServico status
    ) {

        // PageRequest é 0-based, mas o front usa 1-based
        Pageable pageable = PageRequest.of(
                Math.max(page - 1, 0),
                limit,
                Sort.by("nome").ascending()
        );

        Page<Servico> result = servicoRepository.search(status, search, pageable);

        List<ServicoResponseDTO> items = result
                .getContent()
                .stream()
                .map(ServicoMapper::toResponse)
                .toList();

        return new PagedResponseDTO<>(
                page,
                limit,
                result.getTotalElements(),
                result.getTotalPages(),
                items
        );
    }

    // =========================================================
    // BUSCAR POR ID
    // =========================================================

    /**
     * Retorna um serviço pelo ID.
     *
     * Endpoint:
     *   GET /api/servicos/{id}
     */
    @Transactional(readOnly = true)
    public ServicoResponseDTO getById(Long id) {

        Servico servico = servicoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Serviço não encontrado."));

        return ServicoMapper.toResponse(servico);
    }

    // =========================================================
    // CRIAR
    // =========================================================

    /**
     * Cria um novo serviço.
     *
     * Endpoint:
     *   POST /api/servicos
     */
    @Transactional
    public ServicoResponseDTO create(ServicoCreateRequestDTO dto) {

        // =========================
        // Regra: não permitir nome duplicado
        // =========================
        String nomeNormalizado = dto.getNome().trim();

        if (servicoRepository.existsByNomeIgnoreCase(nomeNormalizado)) {
            throw new BusinessException("Já existe um serviço cadastrado com esse nome.");
        }

        Servico servico = ServicoMapper.toEntity(dto);

        // Defesa extra: garante status padrão
        if (servico.getStatus() == null) {
            servico.setStatus(StatusServico.ATIVO);
        }

        // Garante que o nome salvo seja o normalizado (sem espaços)
        servico.setNome(nomeNormalizado);

        Servico saved = servicoRepository.save(servico);
        return ServicoMapper.toResponse(saved);
    }

    // =========================================================
    // ATUALIZAR (COMPLETO)
    // =========================================================

    /**
     * Atualiza um serviço existente.
     *
     * Endpoint:
     *   PUT /api/servicos/{id}
     */
    @Transactional
    public ServicoResponseDTO update(Long id, ServicoUpdateRequestDTO dto) {

        Servico servico = servicoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Serviço não encontrado."));

        // =========================
        // Regra: não permitir atualizar para um nome já existente (outro ID)
        // =========================
        String nomeNormalizado = dto.getNome().trim();

        if (servicoRepository.existsByNomeIgnoreCaseAndIdNot(nomeNormalizado, id)) {
            throw new BusinessException("Já existe um serviço cadastrado com esse nome.");
        }

        ServicoMapper.updateEntity(servico, dto);

        // Garante o nome normalizado após o mapper
        servico.setNome(nomeNormalizado);

        Servico updated = servicoRepository.save(servico);
        return ServicoMapper.toResponse(updated);
    }
    // =========================================================
    // ATUALIZAR STATUS
    // =========================================================

    /**
     * Atualiza somente o status do serviço.
     *
     * Endpoint:
     *   PUT /api/servicos/{id}/status
     */
    @Transactional
    public ServicoResponseDTO updateStatus(Long id, ServicoStatusUpdateRequestDTO dto) {

        Servico servico = servicoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Serviço não encontrado."));

        servico.setStatus(dto.getStatus());

        Servico updated = servicoRepository.save(servico);
        return ServicoMapper.toResponse(updated);
    }

    // =========================================================
    // DELETE
    // =========================================================

    /**
     * Remove um serviço.
     *
     * Endpoint:
     *   DELETE /api/servicos/{id}
     */
    @Transactional
    public void delete(Long id) {

        if (!servicoRepository.existsById(id)) {
            throw new NotFoundException("Serviço não encontrado.");
        }

        servicoRepository.deleteById(id);
    }
    
 
}
