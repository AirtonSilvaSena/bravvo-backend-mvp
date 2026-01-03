package br.com.bravvo.api.controller;

import br.com.bravvo.api.dto.common.PagedResponseDTO;
import br.com.bravvo.api.dto.servico.ServicoCreateRequestDTO;
import br.com.bravvo.api.dto.servico.ServicoResponseDTO;
import br.com.bravvo.api.dto.servico.ServicoStatusUpdateRequestDTO;
import br.com.bravvo.api.dto.servico.ServicoUpdateRequestDTO;
import br.com.bravvo.api.enums.StatusServico;
import br.com.bravvo.api.service.ServicoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller responsável pelos endpoints de Serviços.
 *
 * IMPORTANTE:
 * - Todos os endpoints são restritos ao perfil ADMIN
 * - O formato das respostas segue EXATAMENTE o contrato esperado pelo frontend
 */
@RestController
@RequestMapping("/api/servicos")
@Tag(name = "Serviços", description = "Gerenciamento do catálogo de serviços")
@PreAuthorize("hasRole('ADMIN')")
public class ServicoController {

    private final ServicoService servicoService;

    public ServicoController(ServicoService servicoService) {
        this.servicoService = servicoService;
    }

    // =========================================================
    // 6.1 GET /api/servicos (LISTAGEM PAGINADA)
    // =========================================================

    @Operation(
        summary = "Listar serviços",
        description = """
            Retorna uma lista paginada de serviços.

            Query params:
            - page (1-based)
            - limit
            - search (nome)
            - status (ATIVO | INATIVO)
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
        @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) StatusServico status
    ) {

        PagedResponseDTO<ServicoResponseDTO> paged =
                servicoService.listPaged(page, limit, search, status);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", paged.getItems());
        response.put("pagination", Map.of(
                "page", paged.getPage(),
                "limit", paged.getLimit(),
                "total", paged.getTotal(),
                "pages", paged.getPages()
        ));

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // 6.2 GET /api/servicos/{id}
    // response.data.data
    // =========================================================

    @Operation(
        summary = "Buscar serviço por ID",
        description = "Retorna os dados de um serviço específico."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Serviço encontrado"),
        @ApiResponse(responseCode = "404", description = "Serviço não encontrado"),
        @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {

        ServicoResponseDTO servico = servicoService.getById(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", Map.of("data", servico));

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // 6.3 POST /api/servicos
    // =========================================================

    @Operation(
        summary = "Criar serviço",
        description = "Cria um novo serviço no catálogo."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Serviço criado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @Valid @RequestBody ServicoCreateRequestDTO dto) {

        ServicoResponseDTO servico = servicoService.create(dto);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", Map.of("data", servico));

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // 6.4 PUT /api/servicos/{id}
    // =========================================================

    @Operation(
        summary = "Atualizar serviço",
        description = "Atualiza todos os dados de um serviço existente."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Serviço atualizado com sucesso"),
        @ApiResponse(responseCode = "404", description = "Serviço não encontrado"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable Long id,
            @Valid @RequestBody ServicoUpdateRequestDTO dto) {

        ServicoResponseDTO servico = servicoService.update(id, dto);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", Map.of("data", servico));

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // 6.5 PUT /api/servicos/{id}/status
    // =========================================================

    @Operation(
        summary = "Atualizar status do serviço",
        description = "Atualiza somente o status (ATIVO / INATIVO) do serviço."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status atualizado com sucesso"),
        @ApiResponse(responseCode = "404", description = "Serviço não encontrado"),
        @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ServicoStatusUpdateRequestDTO dto) {

        ServicoResponseDTO servico = servicoService.updateStatus(id, dto);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", Map.of("data", servico));

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // 6.6 DELETE /api/servicos/{id}
    // =========================================================

    @Operation(
        summary = "Remover serviço",
        description = "Remove um serviço do catálogo."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Serviço removido com sucesso"),
        @ApiResponse(responseCode = "404", description = "Serviço não encontrado"),
        @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {

        servicoService.delete(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);

        return ResponseEntity.ok(response);
    }
}
