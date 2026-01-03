package br.com.bravvo.api.controller;

import br.com.bravvo.api.dto.agendamento.AgendamentoItemResponseDTO;
import br.com.bravvo.api.dto.agendamento.PublicAgendamentoCreateRequestDTO;
import br.com.bravvo.api.service.AgendamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.media.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoints públicos (sem login) para criação de agendamento por visitante.
 */
@RestController
@RequestMapping("/api/public/agendamentos")
public class PublicAgendamentoController {

	private final AgendamentoService agendamentoService;

	public PublicAgendamentoController(AgendamentoService agendamentoService) {
		this.agendamentoService = agendamentoService;
	}

	@Operation(summary = "Cria agendamento público (visitante)", description = """
			Cria um agendamento sem login.

			Regras:
			- valida serviço ativo
			- valida funcionário ativo e habilitado
			- resolve duração (prefs_json -> fallback)
			- valida conflito final
			- gera protocolo
			""")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Agendamento criado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Object.class))),
			@ApiResponse(responseCode = "400", description = "Dados inválidos / regras violadas"),
			@ApiResponse(responseCode = "404", description = "Serviço/Funcionário não encontrado"),
			@ApiResponse(responseCode = "409", description = "Conflito de horário") })
	@PostMapping
	public ResponseEntity<?> create(@Valid @RequestBody PublicAgendamentoCreateRequestDTO request) {
		var data = agendamentoService.createPublic(request);
		return ResponseEntity.ok(Map.of("success", true, "data", data));
	}
	
	@Operation(
		    summary = "Consulta agendamento por protocolo (público)",
		    description = """
		        Endpoint público para consultar um agendamento pelo protocolo.
		        Útil para visitante confirmar/acompanhar.
		        """
		)
		@ApiResponses({
		    @ApiResponse(responseCode = "200", description = "Agendamento encontrado"),
		    @ApiResponse(responseCode = "404", description = "Protocolo não encontrado")
		})
		@GetMapping("/{protocolo}")
		public ResponseEntity<?> getByProtocolo(@PathVariable String protocolo) {
		    AgendamentoItemResponseDTO data = agendamentoService.getPublicByProtocolo(protocolo);
		    return ResponseEntity.ok(Map.of("success", true, "data", data));
		}
}
