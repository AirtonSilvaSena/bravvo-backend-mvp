package br.com.bravvo.api.service;

import br.com.bravvo.api.dto.auth.AuthResponseDTO;
import br.com.bravvo.api.dto.auth.MeResponseDTO;
import br.com.bravvo.api.dto.auth.RegisterRequestDTO;
import br.com.bravvo.api.dto.user.UserMeUpdateRequestDTO;
import br.com.bravvo.api.entity.Estabelecimentos;
import br.com.bravvo.api.entity.RefreshToken;
import br.com.bravvo.api.entity.User;
import br.com.bravvo.api.enums.PerfilUser;
import br.com.bravvo.api.enums.StatusAssinatura;
import br.com.bravvo.api.exception.BusinessException;
import br.com.bravvo.api.exception.ForbiddenException;
import br.com.bravvo.api.exception.NotFoundException;
import br.com.bravvo.api.repository.EstabelecimentoRepository;
import br.com.bravvo.api.repository.RefreshTokenRepository;
import br.com.bravvo.api.repository.UserRepository;
import br.com.bravvo.api.security.JwtService;
import br.com.bravvo.api.security.TenantContext;
import br.com.bravvo.api.util.TokenHashUtils;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EstabelecimentoRepository estabelecimentoRepository;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            EstabelecimentoRepository estabelecimentoRepository
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.estabelecimentoRepository = estabelecimentoRepository;
    }

    /**
     * LOGIN (slug + email + senha)
     *
     * Regras:
     * - login somente por email (qualquer perfil)
     * - slug obrigatório
     * - usuário deve pertencer ao estabelecimento (users.estabelecimento_id)
     * - estabelecimento INADIMPLENTE/CANCELADO bloqueia
     */
    public AuthResponseDTO login(String slug, String email, String senha) {

        if (slug == null || slug.isBlank()) {
            throw new BusinessException("Slug é obrigatório.");
        }
        if (email == null || email.isBlank() || senha == null || senha.isBlank()) {
            throw new BusinessException("Credenciais inválidas.");
        }

        String slugTrim = slug.trim().toLowerCase();
        String emailTrim = email.trim().toLowerCase();

        Estabelecimentos estab = estabelecimentoRepository.findBySlug(slugTrim)
                .orElseThrow(() -> new NotFoundException("Estabelecimento não encontrado"));

        // Bloqueio por assinatura
        if (estab.getStatusAssinatura() == StatusAssinatura.INADIMPLENTE
                || estab.getStatusAssinatura() == StatusAssinatura.CANCELADO) {
            throw new ForbiddenException("Acesso indisponível para este estabelecimento.");
        }

        // User precisa existir dentro do estabelecimento
        User user = userRepository.findByEmailAndEstabelecimentoId(emailTrim, estab.getId())
                .orElseThrow(() -> new BusinessException("Credenciais inválidas."));

        if (Boolean.FALSE.equals(user.getAtivo())) {
            throw new ForbiddenException("Usuário inativo.");
        }

        if (!passwordEncoder.matches(senha, user.getSenhaHash())) {
            throw new BusinessException("Credenciais inválidas.");
        }

        // JWT com salao_id + slug
        String accessToken = jwtService.generateAccessToken(user, estab.getId(), estab.getSlug());

        // Refresh token (rotacionável)
        String refreshRaw = generateSecureToken();
        String refreshHash = TokenHashUtils.sha256(refreshRaw);

        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setTokenHash(refreshHash);
        rt.setRevoked(false);
        rt.setExpiresAt(LocalDateTime.now().plusDays(jwtService.getRefreshTokenDays()));
        refreshTokenRepository.save(rt);

        return new AuthResponseDTO(accessToken, refreshRaw, jwtService.getAccessTokenExpiresInSeconds());
    }

    /**
     * REFRESH - valida refresh token (existe, não revogado, não expirado)
     * - revoga o atual
     * - cria um novo refresh (rotação)
     * - gera novo access token
     *
     * Observação multi-tenant:
     * - gera o novo JWT mantendo salao_id do usuário (user.estabelecimentoId)
     * - se o salão estiver INADIMPLENTE/CANCELADO, bloqueia refresh
     */
    public AuthResponseDTO refresh(String refreshTokenRaw) {

        String hash = TokenHashUtils.sha256(refreshTokenRaw);

        RefreshToken rt = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BusinessException("Refresh token inválido."));

        if (Boolean.TRUE.equals(rt.getRevoked())) {
            throw new BusinessException("Refresh token revogado.");
        }

        if (rt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Refresh token expirado.");
        }

        User user = rt.getUser();

        if (Boolean.FALSE.equals(user.getAtivo())) {
            throw new ForbiddenException("Usuário inativo.");
        }

        // Rotação: revoga o token atual
        rt.setRevoked(true);
        refreshTokenRepository.save(rt);

        // Cria novo refresh token
        String newRefreshRaw = generateSecureToken();
        String newRefreshHash = TokenHashUtils.sha256(newRefreshRaw);

        RefreshToken newRt = new RefreshToken();
        newRt.setUser(user);
        newRt.setTokenHash(newRefreshHash);
        newRt.setRevoked(false);
        newRt.setExpiresAt(LocalDateTime.now().plusDays(jwtService.getRefreshTokenDays()));
        refreshTokenRepository.save(newRt);

        // Novo access token mantendo contexto do salão
        Long salaoId = user.getEstabelecimentoId();
        String slug = null;

        if (salaoId != null) {
            Estabelecimentos estab = estabelecimentoRepository.findById(salaoId)
                    .orElseThrow(() -> new ForbiddenException("Estabelecimento inválido."));

            if (estab.getStatusAssinatura() == StatusAssinatura.INADIMPLENTE
                    || estab.getStatusAssinatura() == StatusAssinatura.CANCELADO) {
                throw new ForbiddenException("Acesso indisponível para este estabelecimento.");
            }

            slug = estab.getSlug();
        }

        String newAccessToken = jwtService.generateAccessToken(user, salaoId, slug);

        return new AuthResponseDTO(newAccessToken, newRefreshRaw, jwtService.getAccessTokenExpiresInSeconds());
    }

    /**
     * LOGOUT - revoga o refresh token informado
     */
    public void logout(String refreshTokenRaw) {
        String hash = TokenHashUtils.sha256(refreshTokenRaw);

        RefreshToken rt = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BusinessException("Refresh token inválido."));

        rt.setRevoked(true);
        refreshTokenRepository.save(rt);
    }

    /**
     * ME - retorna dados do usuário autenticado (via email do JWT)
     */
    public MeResponseDTO me() {
        Long estabelecimentoId = TenantContext.getEstabelecimentoIdOrThrow();
        String email = TenantContext.getEmailOrThrow();

        User user = userRepository
                .findByEstabelecimentoIdAndEmail(estabelecimentoId, email)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado neste estabelecimento."));

        if (Boolean.FALSE.equals(user.getAtivo())) {
            throw new ForbiddenException("Usuário inativo.");
        }

        return new MeResponseDTO(
                user.getId(),
                user.getNome(),
                user.getEmail(),
                user.getTelefone(),
                user.getPerfil()
        );
    }

    @Transactional
    public MeResponseDTO updateMe(UserMeUpdateRequestDTO dto) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new ForbiddenException("Usuário não autenticado.");
        }

        Long estabelecimentoId = TenantContext.getEstabelecimentoIdOrThrow();
        String email = TenantContext.getEmailOrThrow();

        User user = userRepository
                .findByEstabelecimentoIdAndEmail(estabelecimentoId, email)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado neste estabelecimento."));

        if (Boolean.FALSE.equals(user.getAtivo())) {
            throw new ForbiddenException("Usuário inativo.");
        }

        user.setNome(dto.getNome().trim());
        user.setTelefone(dto.getTelefone() == null ? null : dto.getTelefone().trim());

        if (dto.getSenha() != null && !dto.getSenha().isBlank()) {
            user.setSenhaHash(passwordEncoder.encode(dto.getSenha()));
        }

        userRepository.save(user);

        return new MeResponseDTO(
                user.getId(),
                user.getNome(),
                user.getEmail(),
                user.getTelefone(),
                user.getPerfil()
        );
    }

    /**
     * Gera token forte e aleatório (URL-safe)
     */
    private String generateSecureToken() {
        byte[] bytes = new byte[48];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}