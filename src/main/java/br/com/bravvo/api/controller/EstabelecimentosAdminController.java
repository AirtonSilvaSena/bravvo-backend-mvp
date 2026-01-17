package br.com.bravvo.api.controller;

import br.com.bravvo.api.dto.estabelecimento.EstabelecimentoMeResponseDTO;
import br.com.bravvo.api.dto.estabelecimento.EstabelecimentoMeUpdateRequestDTO;
import br.com.bravvo.api.service.EstabelecimentosAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/estabelecimento")
@Tag(name = "Admin - Estabelecimento")
@SecurityRequirement(name = "bearerAuth")
public class EstabelecimentosAdminController {

    private final EstabelecimentosAdminService service;

    public EstabelecimentosAdminController(EstabelecimentosAdminService service) {
        this.service = service;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Retorna os dados do estabelecimento do ADMIN logado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Sem permissão (não é ADMIN)"),
            @ApiResponse(responseCode = "404", description = "Estabelecimento não encontrado para este admin")
    })
    public ResponseEntity<?> me() {
        EstabelecimentoMeResponseDTO dto = service.getMe();
        return ResponseEntity.ok(Map.of("success", true, "data", dto));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualiza os dados do estabelecimento do ADMIN logado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Validação inválida"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Sem permissão (não é ADMIN)"),
            @ApiResponse(responseCode = "404", description = "Estabelecimento não encontrado para este admin")
    })
    public ResponseEntity<?> updateMe(@Valid @RequestBody EstabelecimentoMeUpdateRequestDTO body) {
        EstabelecimentoMeResponseDTO dto = service.updateMe(body);
        return ResponseEntity.ok(Map.of("success", true, "data", dto));
    }
}
