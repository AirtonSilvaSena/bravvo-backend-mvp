package br.com.bravvo.api.controller.publico;

import br.com.bravvo.api.dto.publico.PublicDisponibilidadeResponseDTO;
import br.com.bravvo.api.exception.NotFoundException;
import br.com.bravvo.api.repository.EstabelecimentoRepository;
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
    private final EstabelecimentoRepository estabelecimentoRepository;

    public PublicDisponibilidadeController(
            PublicDisponibilidadeService service,
            EstabelecimentoRepository estabelecimentoRepository
    ) {
        this.service = service;
        this.estabelecimentoRepository = estabelecimentoRepository;
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
                - funcionário deve estar ATIVO e perfil FUNCIONARIO (no tenant do slug)
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
                    description = "Serviço/Funcionário/Estabelecimento não encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Object.class))
            )
    })
    @GetMapping("/disponibilidade")
    public ResponseEntity<?> getDisponibilidade(
            @RequestParam String slug,
            @RequestParam Long servicoId,
            @RequestParam Long funcionarioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data
    ) {
        String slugNorm = slug == null ? "" : slug.trim().toLowerCase();

        Long estabelecimentoId = estabelecimentoRepository.findIdBySlug(slugNorm)
                .orElseThrow(() -> new NotFoundException("Estabelecimento não encontrado."));

        PublicDisponibilidadeResponseDTO dto = service.getDisponibilidade(estabelecimentoId, servicoId, funcionarioId, data);

        return ResponseEntity.ok(Map.of("success", true, "data", dto));
    }
}