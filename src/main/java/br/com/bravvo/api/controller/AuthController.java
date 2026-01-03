package br.com.bravvo.api.controller;

import br.com.bravvo.api.dto.auth.AuthResponseDTO;
import br.com.bravvo.api.dto.auth.LoginRequestDTO;
import br.com.bravvo.api.dto.auth.MeResponseDTO;
import br.com.bravvo.api.dto.auth.RefreshRequestDTO;
import br.com.bravvo.api.dto.auth.RegisterRequestDTO;
import br.com.bravvo.api.dto.user.UserMeUpdateRequestDTO;
import br.com.bravvo.api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Endpoints de autenticação e gerenciamento de tokens")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	/**
	 * LOGIN Recebe email e senha e retorna access + refresh token
	 */
	@Operation(summary = "Login do usuário", description = "Realiza a autenticação do usuário e retorna access token (JWT) e refresh token.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Login realizado com sucesso"),
			@ApiResponse(responseCode = "400", description = "Dados inválidos"),
			@ApiResponse(responseCode = "401", description = "Credenciais inválidas"),
			@ApiResponse(responseCode = "403", description = "Usuário inativo") })
	@PostMapping("/login")
	public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
		return ResponseEntity.ok(authService.login(dto.getEmail(), dto.getSenha()));
	}

	/**
	 * REFRESH Recebe refresh token e retorna novo access + novo refresh token
	 */
	@Operation(summary = "Renovar tokens", description = "Gera um novo access token e um novo refresh token a partir de um refresh token válido.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Tokens renovados com sucesso"),
			@ApiResponse(responseCode = "400", description = "Refresh token inválido"),
			@ApiResponse(responseCode = "401", description = "Refresh token expirado ou revogado") })
	@PostMapping("/refresh")
	public ResponseEntity<AuthResponseDTO> refresh(@Valid @RequestBody RefreshRequestDTO dto) {
		return ResponseEntity.ok(authService.refresh(dto.getRefreshToken()));
	}

	/**
	 * LOGOUT Revoga o refresh token informado
	 */
	@Operation(summary = "Logout do usuário", description = "Revoga o refresh token informado, encerrando a sessão do usuário.")
	@ApiResponses({ @ApiResponse(responseCode = "204", description = "Logout realizado com sucesso"),
			@ApiResponse(responseCode = "400", description = "Refresh token inválido") })
	@PostMapping("/logout")
	public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequestDTO dto) {
		authService.logout(dto.getRefreshToken());
		return ResponseEntity.noContent().build();
	}

	/**
	 * ME Retorna dados do usuário autenticado
	 */
	@Operation(summary = "Usuário autenticado", description = "Retorna os dados do usuário atualmente autenticado com base no JWT (access token).")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Usuário autenticado"),
			@ApiResponse(responseCode = "401", description = "Token inválido ou ausente") })
	@GetMapping("/me")
	public ResponseEntity<MeResponseDTO> me() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String email = auth.getName(); // subject do JWT
		return ResponseEntity.ok(authService.me(email));
	}

	/**
	 * REGISTER Cadastro público (self-register) - sempre cria CLIENTE
	 */
	@Operation(summary = "Cadastro público de cliente", description = "Cria um novo usuário do tipo CLIENTE (self-register). Não gera tokens automaticamente.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Cadastro realizado com sucesso"),
			@ApiResponse(responseCode = "400", description = "Dados inválidos / e-mail já cadastrado") })
	@PostMapping("/register")
	public ResponseEntity<MeResponseDTO> register(@Valid @RequestBody RegisterRequestDTO dto) {
		return ResponseEntity.ok(authService.register(dto));
	}
	
	@Operation(
		    summary = "Atualizar dados do usuário autenticado",
		    description = """
		        Atualiza os dados do PRÓPRIO usuário autenticado.

		        Campos permitidos:
		        - nome
		        - telefone
		        - senha (opcional)

		        Campos NÃO permitidos:
		        - email
		        - perfil
		        - status (ativo)

		        Requer Authorization: Bearer <token>
		        """
		)
		@ApiResponses({
		    @ApiResponse(responseCode = "200", description = "Dados atualizados com sucesso"),
		    @ApiResponse(responseCode = "400", description = "Dados inválidos"),
		    @ApiResponse(responseCode = "401", description = "Token inválido ou ausente"),
		    @ApiResponse(responseCode = "403", description = "Usuário inativo ou não autenticado")
		})
		@PutMapping("/me")
		public ResponseEntity<MeResponseDTO> updateMe(
		        @Valid @RequestBody UserMeUpdateRequestDTO dto) {

		    return ResponseEntity.ok(authService.updateMe(dto));
		}

}
