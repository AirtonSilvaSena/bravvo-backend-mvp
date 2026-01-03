package br.com.bravvo.api.controller;

import br.com.bravvo.api.dto.publico.PublicDisponibilidadeResponseDTO;
import br.com.bravvo.api.service.PublicDisponibilidadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.media.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * Catálogo público (sem JWT): disponibilidade de horários.
 *
 * Fluxo no front:
 * - usuário escolhe serviço e funcionário
 * - escolhe a data
 * - chama este endpoint para receber os horários disponíveis
 *
 * Observação:
 * - Se não houver horários disponíveis, retorna "horarios": [] (não é erro).
 */
@RestController
@RequestMapping("/api/public")
public class PublicDisponibilidadeController {

    private final PublicDisponibilidadeService service;

    public PublicDisponibilidadeController(PublicDisponibilidadeService service) {
        this.service = service;
    }

    @Operation(
            summary = "Disponibilidade pública (horários por data)",
            description = """
                Retorna horários disponíveis para um funcionário em uma data,
                considerando:
                - agenda semanal (funcionario_agenda)
                - bloqueios (funcionario_bloqueios)
                - agendamentos existentes (status pendente/confirmado/em_atendimento)
                
                Regras:
                - serviço deve estar ATIVO
                - funcionário deve estar ATIVO e perfil FUNCIONARIO
                - serviço deve estar habilitado para o funcionário
                
                Retorno:
                - { success: true, data: { data, duracaoMin, horarios[] } }
                - horários vazios não representam erro.
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Disponibilidade retornada (pode vir vazia)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Object.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Serviço ou funcionário não encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Object.class))
            )
    })
    @GetMapping("/disponibilidade")
    public ResponseEntity<?> getDisponibilidade(
            @RequestParam Long servicoId,
            @RequestParam Long funcionarioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data
    ) {
        PublicDisponibilidadeResponseDTO dto = service.getDisponibilidade(servicoId, funcionarioId, data);

        // Mantendo padrão simples: { success, data }
        return ResponseEntity.ok(Map.of("success", true, "data", dto));
    }
}
