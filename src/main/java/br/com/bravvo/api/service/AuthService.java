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
     * - slug obrigatório
     * - estabelecimento deve existir e não pode estar INADIMPLENTE/CANCELADO
     * - usuário deve existir e estar ativo
     * - vínculo estabelecimento_users deve existir e estar ativo (fonte de verdade do perfil)
     */
    public AuthResponseDTO login(String slug, String email, String senha) {

        if (slug == null || slug.isBlank()) throw new BusinessException("Slug é obrigatório.");
        if (email == null || email.isBlank() || senha == null || senha.isBlank()) {
            throw new BusinessException("Credenciais inválidas.");
        }

        String slugTrim = slug.trim().toLowerCase();
        String emailTrim = email.trim().toLowerCase();

        Estabelecimentos estab = estabelecimentoRepository.findBySlug(slugTrim)
                .orElseThrow(() -> new NotFoundException("Estabelecimento não encontrado"));

        // bloqueio por assinatura
        if (estab.getStatusAssinatura() == StatusAssinatura.INADIMPLENTE
                || estab.getStatusAssinatura() == StatusAssinatura.CANCELADO) {
            throw new ForbiddenException("Acesso indisponível para este estabelecimento.");
        }

        // vínculo é a fonte de verdade: acha vínculo pelo email e estabelecimento
        EstabelecimentoUser link = estabelecimentoUserRepository
                .findAtivoByEstabelecimentoIdAndUserEmail(estab.getId(), emailTrim)
                .orElseThrow(() -> new BusinessException("Credenciais inválidas."));

        // pega o user global do vínculo
        User user = userRepository.findById(link.getUserId())
                .orElseThrow(() -> new BusinessException("Credenciais inválidas."));

        if (Boolean.FALSE.equals(user.getAtivo())) {
            throw new ForbiddenException("Usuário inativo.");
        }

        if (!passwordEncoder.matches(senha, user.getSenhaHash())) {
            throw new BusinessException("Credenciais inválidas.");
        }

        PerfilUser perfilVinculo = link.getPerfil();

        String accessToken = jwtService.generateAccessToken(user, estab.getId(), estab.getSlug(), perfilVinculo);

        // refresh token (rotação)
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
     * REFRESH (refreshToken + slug)
     *
     * Necessário pois refresh token está ligado ao user, e o user pode ter vínculo com vários estabelecimentos.
     */
    public AuthResponseDTO refresh(String refreshTokenRaw, String slug) {

        if (refreshTokenRaw == null || refreshTokenRaw.isBlank()) {
            throw new BusinessException("Refresh token inválido.");
        }
        if (slug == null || slug.isBlank()) {
            throw new BusinessException("Slug é obrigatório.");
        }

        String slugTrim = slug.trim().toLowerCase();

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

        Estabelecimentos estab = estabelecimentoRepository.findBySlug(slugTrim)
                .orElseThrow(() -> new NotFoundException("Estabelecimento não encontrado"));

        if (estab.getStatusAssinatura() == StatusAssinatura.INADIMPLENTE
                || estab.getStatusAssinatura() == StatusAssinatura.CANCELADO) {
            throw new ForbiddenException("Acesso indisponível para este estabelecimento.");
        }

        // valida vínculo no tenant
        EstabelecimentoUser link = estabelecimentoUserRepository
                .findByEstabelecimentoIdAndUserId(estab.getId(), user.getId())
                .orElseThrow(() -> new ForbiddenException("Usuário não possui vínculo com este estabelecimento."));

        if (Boolean.FALSE.equals(link.getAtivo())) {
            throw new ForbiddenException("Usuário sem permissão (vínculo inativo).");
        }

        // rotação: revoga atual e cria novo
        rt.setRevoked(true);
        refreshTokenRepository.save(rt);

        String newRefreshRaw = generateSecureToken();
        String newRefreshHash = TokenHashUtils.sha256(newRefreshRaw);

        RefreshToken newRt = new RefreshToken();
        newRt.setUser(user);
        newRt.setTokenHash(newRefreshHash);
        newRt.setRevoked(false);
        newRt.setExpiresAt(LocalDateTime.now().plusDays(jwtService.getRefreshTokenDays()));
        refreshTokenRepository.save(newRt);

        String newAccessToken = jwtService.generateAccessToken(
                user,
                estab.getId(),
                estab.getSlug(),
                link.getPerfil()
        );

        return new AuthResponseDTO(newAccessToken, newRefreshRaw, jwtService.getAccessTokenExpiresInSeconds());
    }

    public void logout(String refreshTokenRaw) {
        String hash = TokenHashUtils.sha256(refreshTokenRaw);

        RefreshToken rt = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BusinessException("Refresh token inválido."));

        rt.setRevoked(true);
        refreshTokenRepository.save(rt);
    }

    /**
     * ME - resolve user no contexto do tenant pelo vínculo.
     */
    public MeResponseDTO me() {
        Long estabelecimentoId = TenantContext.getEstabelecimentoIdOrThrow();
        String email = TenantContext.getEmailOrThrow();

        EstabelecimentoUser link = estabelecimentoUserRepository
                .findAtivoByEstabelecimentoIdAndUserEmail(estabelecimentoId, email)
                .orElseThrow(() -> new ForbiddenException("Usuário sem permissão (vínculo inativo ou inexistente)."));

        User user = userRepository.findById(link.getUserId())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));

        if (Boolean.FALSE.equals(user.getAtivo())) {
            throw new ForbiddenException("Usuário inativo.");
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
     * UPDATE ME - atualiza somente o próprio user (global), mas exige vínculo ativo no tenant do token.
     */
    @Transactional
    public MeResponseDTO updateMe(UserMeUpdateRequestDTO dto) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ForbiddenException("Usuário não autenticado.");
        }

        Long estabelecimentoId = TenantContext.getEstabelecimentoIdOrThrow();
        String email = TenantContext.getEmailOrThrow();

        EstabelecimentoUser link = estabelecimentoUserRepository
                .findAtivoByEstabelecimentoIdAndUserEmail(estabelecimentoId, email)
                .orElseThrow(() -> new ForbiddenException("Usuário sem permissão (vínculo inativo ou inexistente)."));

        User user = userRepository.findById(link.getUserId())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));

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
                link.getPerfil()
        );
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[48];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}