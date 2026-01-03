package br.com.bravvo.api.controller;

import br.com.bravvo.api.dto.funcionario.FuncionarioServicoConfigResponseDTO;
import br.com.bravvo.api.dto.funcionario.FuncionarioServicosUpdateRequestDTO;
import br.com.bravvo.api.service.FuncionarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Módulo exclusivo do FUNCIONÁRIO: auto-configuração de serviços e
 * preferências.
 *
 * IMPORTANTE: Admin NÃO acessa esses endpoints por regra de produto.
 */
@RestController
@RequestMapping("/api/funcionarios")
public class FuncionarioController {

	private final FuncionarioService funcionarioService;

	public FuncionarioController(FuncionarioService funcionarioService) {
		this.funcionarioService = funcionarioService;
	}

	@Operation(summary = "Lista serviços configuráveis do funcionário logado", description = """
			Endpoint exclusivo do FUNCIONÁRIO.
			Retorna todos os serviços ativos do sistema,
			marcando quais o funcionário habilitou e a duração personalizada.
			""", security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Lista retornada com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Object.class))),
			@ApiResponse(responseCode = "401", description = "Não autenticado (token ausente ou inválido)"),
			@ApiResponse(responseCode = "403", description = "Acesso negado (somente FUNCIONARIO)") })
	@GetMapping("/me/servicos")
	public ResponseEntity<?> getMeServicos() {

		List<FuncionarioServicoConfigResponseDTO> data = funcionarioService.getMeServicos();

		// Mantendo padrão simples: { success, data }
		// (se você tiver ApiResponse helper, trocamos depois)
		return ResponseEntity.ok(Map.of("success", true, "data", data));
	}

	@Operation(summary = "Atualiza serviços habilitados e preferências do funcionário logado", description = """
			Endpoint exclusivo do FUNCIONÁRIO.
			Sincroniza os serviços habilitados (funcionario_servicos) e salva preferências (funcionario_prefs.prefs_json)
			em uma única transação.
			""", security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Configuração salva com sucesso"),
			@ApiResponse(responseCode = "400", description = "Dados inválidos (validações)"),
			@ApiResponse(responseCode = "401", description = "Não autenticado"),
			@ApiResponse(responseCode = "403", description = "Acesso negado (somente FUNCIONARIO)"),
			@ApiResponse(responseCode = "409", description = "Regras de negócio violadas") })
	@PutMapping("/me/servicos")
	public ResponseEntity<?> updateMeServicos(
			@Valid @org.springframework.web.bind.annotation.RequestBody FuncionarioServicosUpdateRequestDTO request) {

		var data = funcionarioService.updateMeServicos(request);

		return ResponseEntity.ok(Map.of("success", true, "data", data));
	}

}
