package br.com.bravvo.api.controller;

import br.com.bravvo.api.dto.publico.PublicFuncionarioServicoResponseDTO;
import br.com.bravvo.api.dto.publico.PublicServicoResponseDTO;
import br.com.bravvo.api.service.PublicCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller público para suportar o fluxo de agendamento SEM LOGIN.
 *
 * Base path: /api/public
 *
 * Regras:
 * - Não exige JWT
 * - Retorna dados mínimos e seguros
 */
@RestController
@RequestMapping("/api/public")
@Tag(
    name = "Catálogo Público",
    description = "Endpoints públicos (sem login) para seleção de serviços e profissionais antes do agendamento."
)
public class PublicCatalogController {

    private final PublicCatalogService publicCatalogService;

    public PublicCatalogController(PublicCatalogService publicCatalogService) {
        this.publicCatalogService = publicCatalogService;
    }

    @GetMapping("/servicos")
    @Operation(
        summary = "Listar serviços públicos",
        description = "Retorna somente serviços ATIVOS com dados mínimos (id, nome, valor)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Serviços retornados com sucesso")
    })
    public ResponseEntity<List<PublicServicoResponseDTO>> listServicosPublicos() {
        return ResponseEntity.ok(publicCatalogService.listServicosPublicos());
    }

    @GetMapping("/servicos/{servicoId}/funcionarios")
    @Operation(
        summary = "Listar funcionários por serviço (público)",
        description = """
            Retorna funcionários ATIVOS que executam o serviço, com:
            - nome do funcionário (sem email/telefone)
            - valor do serviço
            - duracaoMin resolvida (prefs_json -> fallback duração padrão do serviço)
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Funcionários retornados com sucesso"),
        @ApiResponse(responseCode = "404", description = "Serviço não encontrado ou indisponível")
    })
    public ResponseEntity<List<PublicFuncionarioServicoResponseDTO>> listFuncionariosPorServico(
            @PathVariable Long servicoId
    ) {
        return ResponseEntity.ok(publicCatalogService.listFuncionariosPorServico(servicoId));
    }
}
