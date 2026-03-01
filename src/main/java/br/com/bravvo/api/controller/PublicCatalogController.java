package br.com.bravvo.api.controller;

import br.com.bravvo.api.dto.publico.PublicFuncionarioServicoResponseDTO;
import br.com.bravvo.api.dto.publico.PublicServicoResponseDTO;
import br.com.bravvo.api.exception.NotFoundException;
import br.com.bravvo.api.repository.EstabelecimentoRepository;
import br.com.bravvo.api.service.PublicCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller público para suportar o fluxo de agendamento SEM LOGIN.
 *
 * Base path: /api/public
 *
 * Multi-tenant:
 * - "slug" é obrigatório para resolver o estabelecimento.
 * - Sem slug, não existe contexto para o catálogo público.
 */
@RestController
@RequestMapping("/api/public")
@Tag(
        name = "Catálogo Público",
        description = "Endpoints públicos (sem login) para seleção de serviços e profissionais antes do agendamento."
)
public class PublicCatalogController {

    private final PublicCatalogService publicCatalogService;
    private final EstabelecimentoRepository estabelecimentoRepository;

    public PublicCatalogController(
            PublicCatalogService publicCatalogService,
            EstabelecimentoRepository estabelecimentoRepository
    ) {
        this.publicCatalogService = publicCatalogService;
        this.estabelecimentoRepository = estabelecimentoRepository;
    }

    @GetMapping("/servicos")
    @Operation(
            summary = "Listar serviços públicos por estabelecimento (slug)",
            description = """
                Retorna somente serviços ATIVOS do estabelecimento informado (slug),
                com dados mínimos (id, nome, valor).
                
                Query params:
                - slug (obrigatório)
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Serviços retornados com sucesso"),
            @ApiResponse(responseCode = "404", description = "Estabelecimento não encontrado")
    })
    public ResponseEntity<List<PublicServicoResponseDTO>> listServicosPublicos(
            @RequestParam String slug
    ) {
        Long estabelecimentoId = resolveEstabelecimentoIdOrThrow(slug);
        return ResponseEntity.ok(publicCatalogService.listServicosPublicos(estabelecimentoId));
    }

    @GetMapping("/servicos/{servicoId}/funcionarios")
    @Operation(
            summary = "Listar funcionários por serviço (público) por estabelecimento (slug)",
            description = """
                Retorna funcionários ATIVOS (perfil FUNCIONARIO e vínculo ativo no tenant) que executam o serviço,
                com:
                - nome do funcionário (sem email/telefone)
                - valor do serviço
                - duracaoMin resolvida (prefs_json -> fallback duração padrão do serviço)
                
                Query params:
                - slug (obrigatório)
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Funcionários retornados com sucesso"),
            @ApiResponse(responseCode = "404", description = "Estabelecimento/Serviço não encontrado ou indisponível")
    })
    public ResponseEntity<List<PublicFuncionarioServicoResponseDTO>> listFuncionariosPorServico(
            @PathVariable Long servicoId,
            @RequestParam String slug
    ) {
        Long estabelecimentoId = resolveEstabelecimentoIdOrThrow(slug);
        return ResponseEntity.ok(publicCatalogService.listFuncionariosPorServico(estabelecimentoId, servicoId));
    }

    private Long resolveEstabelecimentoIdOrThrow(String slug) {
        String slugNorm = slug == null ? "" : slug.trim().toLowerCase();
        return estabelecimentoRepository.findIdBySlug(slugNorm)
                .orElseThrow(() -> new NotFoundException("Estabelecimento não encontrado."));
    }
}