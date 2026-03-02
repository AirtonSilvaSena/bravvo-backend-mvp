package br.com.bravvo.api.controller.publico;

import br.com.bravvo.api.dto.agendamento.AgendamentoItemResponseDTO;
import br.com.bravvo.api.dto.agendamento.PublicAgendamentoCreateRequestDTO;
import br.com.bravvo.api.exception.NotFoundException;
import br.com.bravvo.api.repository.EstabelecimentoRepository;
import br.com.bravvo.api.service.AgendamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.media.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoints públicos (sem login) para criação/consulta de agendamento por visitante.
 *
 * Multi-tenant:
 * - Público NÃO tem JWT.
 * - Portanto o estabelecimento é resolvido pelo slug na rota.
 */
@RestController
@RequestMapping("/api/public/estabelecimentos/{slug}/agendamentos")
public class PublicAgendamentoController {

    private final AgendamentoService agendamentoService;
    private final EstabelecimentoRepository estabelecimentoRepository;

    public PublicAgendamentoController(
            AgendamentoService agendamentoService,
            EstabelecimentoRepository estabelecimentoRepository
    ) {
        this.agendamentoService = agendamentoService;
        this.estabelecimentoRepository = estabelecimentoRepository;
    }

    @Operation(summary = "Cria agendamento público (visitante)", description = """
            Cria um agendamento sem login (visitante), no estabelecimento do slug informado.

            Regras:
            - valida serviço ativo
            - valida funcionário ativo e habilitado
            - resolve duração (prefs_json -> fallback)
            - valida conflito final
            - gera protocolo
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agendamento criado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Object.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos / regras violadas"),
            @ApiResponse(responseCode = "404", description = "Estabelecimento/Serviço/Funcionário não encontrado"),
            @ApiResponse(responseCode = "409", description = "Conflito de horário")
    })
    @PostMapping
    public ResponseEntity<?> create(
            @PathVariable String slug,
            @Valid @RequestBody PublicAgendamentoCreateRequestDTO request
    ) {
        Long estabelecimentoId = estabelecimentoRepository.findIdBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Estabelecimento não encontrado para este slug."));

        var data = agendamentoService.createPublic(estabelecimentoId, request);
        return ResponseEntity.ok(Map.of("success", true, "data", data));
    }

    
}