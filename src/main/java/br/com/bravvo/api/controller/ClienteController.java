package br.com.bravvo.api.controller;

import br.com.bravvo.api.dto.common.PagedResponseDTO;
import br.com.bravvo.api.dto.cliente.ClientePickerResponseDTO;
import br.com.bravvo.api.service.ClientePickerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoints internos de apoio ao agendamento: - funcionário/admin listam
 * clientes para selecionar antes de criar agendamento.
 */
@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

	private final ClientePickerService service;

	public ClienteController(ClientePickerService service) {
		this.service = service;
	}

	@Operation(summary = "Lista clientes cadastrados (para seleção no agendamento)", description = """
			Endpoint interno para ADMIN/FUNCIONARIO selecionar cliente.

			Regras:
			- Retorna apenas perfil CLIENTE e ativo=true
			- Busca opcional (q) em nome/email/telefone
			- Paginado: page (1-based), limit

			Resposta:
			{ "success": true, "data": { page, limit, total, pages, items } }
			""", security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Lista paginada retornada com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Object.class))),
			@ApiResponse(responseCode = "401", description = "Não autenticado"),
			@ApiResponse(responseCode = "403", description = "Acesso negado (somente ADMIN/FUNCIONARIO)") })
	@PreAuthorize("hasAnyRole('ADMIN','FUNCIONARIO')")
	@GetMapping
	public ResponseEntity<?> list(@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "10") int limit, @RequestParam(required = false) String q) {
		PagedResponseDTO<ClientePickerResponseDTO> data = service.listClientes(page, limit, q);
		return ResponseEntity.ok(Map.of("success", true, "data", data));
	}
}
