package br.com.bravvo.api.controller;

import br.com.bravvo.api.dto.agendamento.AgendamentoItemResponseDTO;
import br.com.bravvo.api.dto.agendamento.ClienteAgendamentoCreateRequestDTO;
import br.com.bravvo.api.exception.NotFoundException;
import br.com.bravvo.api.repository.UserRepository;
import br.com.bravvo.api.service.AgendamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Endpoints autenticados para CLIENTE criar agendamento para si.
 */
@RestController
@RequestMapping("/api/agendamentos")
public class AgendamentoController {

	private final AgendamentoService agendamentoService;
	private final UserRepository userRepository;

	public AgendamentoController(AgendamentoService agendamentoService, UserRepository userRepository) {
		this.agendamentoService = agendamentoService;
		this.userRepository = userRepository;
	}

	@Operation(summary = "Cria agendamento (cliente logado)", description = """
			Cliente cria agendamento para si.

			Regras:
			- clienteId é derivado do JWT (subject=email -> busca no banco)
			- validações finais + gera protocolo
			""", security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Agendamento criado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Object.class))),
			@ApiResponse(responseCode = "400", description = "Dados inválidos / regras violadas"),
			@ApiResponse(responseCode = "401", description = "Não autenticado"),
			@ApiResponse(responseCode = "403", description = "Acesso negado (somente CLIENTE)"),
			@ApiResponse(responseCode = "409", description = "Conflito de horário") })
	@PreAuthorize("hasRole('CLIENTE')")
	@PostMapping
	public ResponseEntity<?> create(@Valid @RequestBody ClienteAgendamentoCreateRequestDTO request) {

		// No seu projeto: auth.getName() == email (subject do JWT)
		String email = SecurityContextHolder.getContext().getAuthentication().getName();

		Long clienteId = userRepository.findByEmail(email)
				.orElseThrow(() -> new NotFoundException("Usuário não encontrado.")).getId();

		var data = agendamentoService.createClienteLogado(clienteId, request);

		// Padrão do projeto: { success, data }
		return ResponseEntity.ok(Map.of("success", true, "data", data));
	}

	@Operation(summary = "Lista meus agendamentos (cliente logado)", description = """
			Retorna os agendamentos do CLIENTE autenticado.

			Query params (opcionais):
			- from=yyyy-MM-dd
			- to=yyyy-MM-dd
			- status=pendente,confirmado,em_atendimento,concluido,cancelado
			""", security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
			@ApiResponse(responseCode = "401", description = "Não autenticado"),
			@ApiResponse(responseCode = "403", description = "Acesso negado (somente CLIENTE)") })
	@PreAuthorize("hasRole('CLIENTE')")
	@GetMapping("/me")
	public ResponseEntity<?> listMe(@RequestParam(required = false) String from,
			@RequestParam(required = false) String to, @RequestParam(required = false) String status) {
		String email = SecurityContextHolder.getContext().getAuthentication().getName();

		Long clienteId = userRepository.findByEmail(email)
				.orElseThrow(() -> new NotFoundException("Usuário não encontrado.")).getId();

		List<AgendamentoItemResponseDTO> data = agendamentoService.listCliente(clienteId, from, to, status);
		return ResponseEntity.ok(Map.of("success", true, "data", data));
	}
}
