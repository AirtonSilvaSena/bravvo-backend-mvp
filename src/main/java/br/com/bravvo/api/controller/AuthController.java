package br.com.bravvo.api.controller;

import br.com.bravvo.api.dto.auth.AuthResponseDTO;
import br.com.bravvo.api.dto.auth.LoginRequestDTO;
import br.com.bravvo.api.dto.auth.MeResponseDTO;
import br.com.bravvo.api.dto.auth.RefreshRequestDTO;
import br.com.bravvo.api.dto.user.UserMeUpdateRequestDTO;
import br.com.bravvo.api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Endpoints de autenticação e gerenciamento de tokens")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "Login do usuário (por estabelecimento)",
            description = """
                Realiza autenticação do usuário dentro do contexto do estabelecimento (slug).

                Regras:
                - Login é somente por e-mail (qualquer perfil).
                - Slug é obrigatório.
                - Usuário deve possuir vínculo com o estabelecimento (estabelecimento_users).
                - Estabelecimento INADIMPLENTE/CANCELADO bloqueia login.
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login realizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas"),
            @ApiResponse(responseCode = "403", description = "Usuário inativo / vínculo inativo / estabelecimento bloqueado"),
            @ApiResponse(responseCode = "404", description = "Estabelecimento não encontrado")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        return ResponseEntity.ok(authService.login(dto.getSlug(), dto.getEmail(), dto.getSenha()));
    }

    @Operation(
            summary = "Renovar tokens",
            description = """
                Gera um novo access token e um novo refresh token a partir de um refresh token válido.

                Regras:
                - Refresh token deve estar válido e não revogado.
                - O perfil do JWT é recalculado a partir do vínculo (estabelecimento_users).
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tokens renovados com sucesso"),
            @ApiResponse(responseCode = "400", description = "Refresh token inválido"),
            @ApiResponse(responseCode = "401", description = "Refresh token expirado ou revogado")
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refresh(@Valid @RequestBody RefreshRequestDTO dto) {
        return ResponseEntity.ok(authService.refresh(dto.getRefreshToken()));
    }

    @Operation(
            summary = "Logout do usuário",
            description = """
                Revoga o refresh token informado, encerrando a sessão do usuário.

                Regras:
                - O refresh token informado é invalidado/revogado.
                - Novas renovações a partir deste token deixam de ser aceitas.
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Logout realizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Refresh token inválido")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequestDTO dto) {
        authService.logout(dto.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Usuário autenticado",
            description = """
                Retorna os dados do usuário atualmente autenticado com base no JWT (access token).

                Multi-tenant:
                - A identificação ocorre no contexto do estabelecimento presente no token.
                - O perfil retornado vem do vínculo (estabelecimento_users).
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário autenticado"),
            @ApiResponse(responseCode = "401", description = "Token inválido ou ausente"),
            @ApiResponse(responseCode = "403", description = "Usuário inativo ou vínculo inativo")
    })
    @GetMapping("/me")
    public ResponseEntity<MeResponseDTO> me() {
        return ResponseEntity.ok(authService.me());
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

                Multi-tenant:
                - A atualização ocorre no contexto do estabelecimento presente no token.
                - O perfil é mantido pelo vínculo (estabelecimento_users).

                Requer Authorization: Bearer <token>
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados atualizados com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Token inválido ou ausente"),
            @ApiResponse(responseCode = "403", description = "Usuário inativo, vínculo inativo ou não autenticado")
    })
    @PutMapping("/me")
    public ResponseEntity<MeResponseDTO> updateMe(@Valid @RequestBody UserMeUpdateRequestDTO dto) {
        return ResponseEntity.ok(authService.updateMe(dto));
    }
}