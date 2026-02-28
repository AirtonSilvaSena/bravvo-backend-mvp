package br.com.bravvo.api.controller;

import br.com.bravvo.api.dto.auth.AuthResponseDTO;
import br.com.bravvo.api.dto.auth.LoginRequestDTO;
import br.com.bravvo.api.dto.auth.MeResponseDTO;
import br.com.bravvo.api.dto.auth.RefreshRequestDTO;
import br.com.bravvo.api.dto.user.UserMeUpdateRequestDTO;
import br.com.bravvo.api.service.AuthService;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        return ResponseEntity.ok(authService.login(dto.getSlug(), dto.getEmail(), dto.getSenha()));
    }

    /**
     * REFRESH agora exige slug junto (multi-tenant real).
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refresh(@Valid @RequestBody RefreshRequestDTO dto) {
        return ResponseEntity.ok(authService.refresh(dto.getRefreshToken(), dto.getSlug()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequestDTO dto) {
        authService.logout(dto.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponseDTO> me() {
        return ResponseEntity.ok(authService.me());
    }

    @PutMapping("/me")
    public ResponseEntity<MeResponseDTO> updateMe(@Valid @RequestBody UserMeUpdateRequestDTO dto) {
        return ResponseEntity.ok(authService.updateMe(dto));
    }
}