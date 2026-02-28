package br.com.bravvo.api.service;

import br.com.bravvo.api.dto.auth.AuthResponseDTO;
import br.com.bravvo.api.dto.auth.MeResponseDTO;
import br.com.bravvo.api.dto.user.UserMeUpdateRequestDTO;
import br.com.bravvo.api.entity.EstabelecimentoUser;
import br.com.bravvo.api.entity.Estabelecimentos;
import br.com.bravvo.api.entity.RefreshToken;
import br.com.bravvo.api.entity.User;
import br.com.bravvo.api.enums.PerfilUser;
import br.com.bravvo.api.enums.StatusAssinatura;
import br.com.bravvo.api.exception.BusinessException;
import br.com.bravvo.api.exception.ForbiddenException;
import br.com.bravvo.api.exception.NotFoundException;
import br.com.bravvo.api.repository.EstabelecimentoRepository;
import br.com.bravvo.api.repository.EstabelecimentoUserRepository;
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
    private final EstabelecimentoUserRepository estabelecimentoUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EstabelecimentoRepository estabelecimentoRepository;

    public AuthService(
            UserRepository userRepository,
            EstabelecimentoUserRepository estabelecimentoUserRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            EstabelecimentoRepository estabelecimentoRepository
    ) {
        this.userRepository = userRepository;
        this.estabelecimentoUserRepository = estabelecimentoUserRepository;
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
     * - vínculo estabelecimento_users obrigatório e ativo (fonte de verdade)
     * - estabelecimento INADIMPLENTE/CANCELADO bloqueia
     *
     * Obs (fase atual):
     * - enquanto users ainda possuir estabelecimento_id, buscamos user por (estabelecimentoId + email)
     *   para evitar ambiguidades de e-mail repetido em tenants diferentes.
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

        // User precisa existir dentro do estabelecimento (fase atual)
        User user = userRepository.findByEstabelecimentoIdAndEmail(estab.getId(), emailTrim)
                .orElseThrow(() -> new BusinessException("Credenciais inválidas."));

        if (Boolean.FALSE.equals(user.getAtivo())) {
            throw new ForbiddenException("Usuário inativo.");
        }

        if (!passwordEncoder.matches(senha, user.getSenhaHash())) {
            throw new BusinessException("Credenciais inválidas.");
        }

        // Vínculo é a fonte de verdade do perfil
        EstabelecimentoUser link = estabelecimentoUserRepository
                .findByEstabelecimentoIdAndUserId(estab.getId(), user.getId())
                .orElseThrow(() -> new ForbiddenException("Usuário não possui vínculo com este estabelecimento."));

        if (Boolean.FALSE.equals(link.getAtivo())) {
            throw new ForbiddenException("Usuário sem permissão (vínculo inativo).");
        }

        PerfilUser perfilVinculo = link.getPerfil();

        // JWT com estabelecimento_id + slug + perfil do vínculo
        String accessToken = jwtService.generateAccessToken(user, estab.getId(), estab.getSlug(), perfilVinculo);

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
     * Observação multi-tenant (fase atual):
     * - Como o refresh token está atrelado ao "user row" (ainda por estabelecimento),
     *   ainda usamos user.estabelecimentoId nesta fase.
     * - O perfil no JWT é recalculado pelo vínculo estabelecimento_users.
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

        // Mantém contexto do estabelecimento (fase atual)
        Long estabelecimentoId = user.getEstabelecimentoId();
        String slug = null;

        if (estabelecimentoId != null) {
            Estabelecimentos estab = estabelecimentoRepository.findById(estabelecimentoId)
                    .orElseThrow(() -> new ForbiddenException("Estabelecimento inválido."));

            if (estab.getStatusAssinatura() == StatusAssinatura.INADIMPLENTE
                    || estab.getStatusAssinatura() == StatusAssinatura.CANCELADO) {
                throw new ForbiddenException("Acesso indisponível para este estabelecimento.");
            }

            slug = estab.getSlug();
        }

        // Perfil deve vir do vínculo
        PerfilUser perfilVinculo = null;
        if (estabelecimentoId != null) {
            EstabelecimentoUser link = estabelecimentoUserRepository
                    .findByEstabelecimentoIdAndUserId(estabelecimentoId, user.getId())
                    .orElseThrow(() -> new ForbiddenException("Vínculo do usuário com o estabelecimento não encontrado."));

            if (Boolean.FALSE.equals(link.getAtivo())) {
                throw new ForbiddenException("Usuário sem permissão (vínculo inativo).");
            }

            perfilVinculo = link.getPerfil();
        }

        String newAccessToken = jwtService.generateAccessToken(user, estabelecimentoId, slug, perfilVinculo);

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
     *
     * Multi-tenant:
     * - user é resolvido no contexto do estabelecimento do token
     * - perfil vem do vínculo estabelecimento_users
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

        EstabelecimentoUser link = estabelecimentoUserRepository
                .findByEstabelecimentoIdAndUserId(estabelecimentoId, user.getId())
                .orElseThrow(() -> new ForbiddenException("Vínculo do usuário com o estabelecimento não encontrado."));

        if (Boolean.FALSE.equals(link.getAtivo())) {
            throw new ForbiddenException("Usuário sem permissão (vínculo inativo).");
        }

        return new MeResponseDTO(
                user.getId(),
                user.getNome(),
                user.getEmail(),
                user.getTelefone(),
                link.getPerfil()
        );
    }

    /**
     * PUT /api/auth/me - atualiza dados do próprio usuário.
     *
     * Regras:
     * - Sem validação por perfil (ADMIN/FUNCIONARIO/CLIENTE), pois altera apenas o próprio usuário.
     * - Nome obrigatório (DTO @NotBlank).
     * - Senha é opcional.
     */
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

        // vínculo precisa existir e estar ativo
        EstabelecimentoUser link = estabelecimentoUserRepository
                .findByEstabelecimentoIdAndUserId(estabelecimentoId, user.getId())
                .orElseThrow(() -> new ForbiddenException("Vínculo do usuário com o estabelecimento não encontrado."));

        if (Boolean.FALSE.equals(link.getAtivo())) {
            throw new ForbiddenException("Usuário sem permissão (vínculo inativo).");
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
                link.getPerfil()
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