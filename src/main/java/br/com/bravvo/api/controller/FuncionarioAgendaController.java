package br.com.bravvo.api.controller;

import br.com.bravvo.api.dto.funcionario.*;
import br.com.bravvo.api.service.FuncionarioAgendaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Self-service do FUNCIONARIO: - agenda semanal (2 janelas / almoço) -
 * bloqueios pontuais
 *
 * Admin/Cliente não acessam.
 */
@RestController
@RequestMapping("/api/funcionarios/me")
@Tag(name = "Funcionário - Agenda", description = "Configuração de agenda e bloqueios do próprio funcionário.")
public class FuncionarioAgendaController {

	private final FuncionarioAgendaService service;

	public FuncionarioAgendaController(FuncionarioAgendaService service) {
		this.service = service;
	}

	// =========================
	// Agenda
	// =========================

	@GetMapping("/agenda")
	@Operation(summary = "Obter minha agenda semanal", description = "Retorna os 7 dias (1..7). Dias não configurados voltam como ativo=false.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Agenda retornada com sucesso"),
			@ApiResponse(responseCode = "403", description = "Acesso negado (não é FUNCIONARIO)"), })
	public ResponseEntity<List<FuncionarioAgendaDayResponseDTO>> getMyAgenda() {
		return ResponseEntity.ok(service.getMyAgenda());
	}

	@PutMapping("/agenda")
	@Operation(summary = "Atualizar minha agenda semanal", description = "Atualiza a agenda completa (7 dias). Suporta 2 janelas por dia (antes e depois do almoço).")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Agenda atualizada com sucesso"),
			@ApiResponse(responseCode = "400", description = "Dados inválidos"),
			@ApiResponse(responseCode = "403", description = "Acesso negado (não é FUNCIONARIO)") })
	public ResponseEntity<List<FuncionarioAgendaDayResponseDTO>> updateMyAgenda(
			@Valid @RequestBody FuncionarioAgendaUpdateRequestDTO request) {
		return ResponseEntity.ok(service.updateMyAgenda(request));
	}

	// =========================
	// Bloqueios
	// =========================

	@GetMapping("/bloqueios")
	@Operation(summary = "Listar meus bloqueios", description = "Lista bloqueios do funcionário (dia inteiro ou intervalo).")
	public ResponseEntity<List<FuncionarioBloqueioResponseDTO>> listMyBloqueios() {
		return ResponseEntity.ok(service.listMyBloqueios());
	}

	@PostMapping("/bloqueios")
	@Operation(summary = "Criar bloqueio", description = "Cria bloqueio (dia inteiro ou intervalo) usando startDt e endDt.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Bloqueio criado com sucesso"),
			@ApiResponse(responseCode = "400", description = "Dados inválidos"),
			@ApiResponse(responseCode = "403", description = "Acesso negado (não é FUNCIONARIO)") })
	public ResponseEntity<FuncionarioBloqueioResponseDTO> createMyBloqueio(
			@Valid @RequestBody FuncionarioBloqueioCreateRequestDTO dto) {
		return ResponseEntity.ok(service.createMyBloqueio(dto));
	}

	@DeleteMapping("/bloqueios/{id}")
	@Operation(summary = "Remover bloqueio", description = "Remove um bloqueio pertencente ao funcionário logado.")
	@ApiResponses({ @ApiResponse(responseCode = "204", description = "Bloqueio removido com sucesso"),
			@ApiResponse(responseCode = "403", description = "Acesso negado ou bloqueio não pertence ao funcionário"),
			@ApiResponse(responseCode = "404", description = "Bloqueio não encontrado") })
	public ResponseEntity<Void> deleteMyBloqueio(@PathVariable Long id) {
		service.deleteMyBloqueio(id);
		return ResponseEntity.noContent().build();
	}
}
