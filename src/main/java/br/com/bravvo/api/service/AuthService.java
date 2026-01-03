package br.com.bravvo.api.service;

import br.com.bravvo.api.dto.auth.AuthResponseDTO;
import br.com.bravvo.api.dto.auth.MeResponseDTO;
import br.com.bravvo.api.dto.auth.RegisterRequestDTO;
import br.com.bravvo.api.dto.user.UserMeUpdateRequestDTO;
import br.com.bravvo.api.entity.RefreshToken;
import br.com.bravvo.api.entity.User;
import br.com.bravvo.api.enums.PerfilUser;
import br.com.bravvo.api.exception.BusinessException;
import br.com.bravvo.api.exception.ForbiddenException;
import br.com.bravvo.api.exception.NotFoundException;
import br.com.bravvo.api.repository.RefreshTokenRepository;
import br.com.bravvo.api.repository.UserRepository;
import br.com.bravvo.api.security.JwtService;
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

	public AuthService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
			PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.userRepository = userRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	/**
	 * LOGIN - valida credenciais - gera access token (JWT) - gera refresh token -
	 * persiste refresh token (hash)
	 */
	public AuthResponseDTO login(String email, String senha) {

		User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Credenciais inválidas."));

		if (Boolean.FALSE.equals(user.getAtivo())) {
			throw new RuntimeException("Usuário inativo.");
		}

		if (!passwordEncoder.matches(senha, user.getSenhaHash())) {
			throw new RuntimeException("Credenciais inválidas.");
		}

		// Access token (JWT)
		String accessToken = jwtService.generateAccessToken(user);

		// Refresh token (RAW + HASH)
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
	 * REFRESH - valida refresh token (existe, não revogado, não expirado) - revoga
	 * o atual - cria um novo refresh (rotação) - gera novo access token
	 */
	public AuthResponseDTO refresh(String refreshTokenRaw) {
		String hash = TokenHashUtils.sha256(refreshTokenRaw);

		RefreshToken rt = refreshTokenRepository.findByTokenHash(hash)
				.orElseThrow(() -> new RuntimeException("Refresh token inválido."));

		if (Boolean.TRUE.equals(rt.getRevoked())) {
			throw new RuntimeException("Refresh token revogado.");
		}

		if (rt.getExpiresAt().isBefore(LocalDateTime.now())) {
			throw new RuntimeException("Refresh token expirado.");
		}

		User user = rt.getUser();

		if (Boolean.FALSE.equals(user.getAtivo())) {
			throw new RuntimeException("Usuário inativo.");
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

		// Novo access token
		String newAccessToken = jwtService.generateAccessToken(user);

		return new AuthResponseDTO(newAccessToken, newRefreshRaw, jwtService.getAccessTokenExpiresInSeconds());
	}

	/**
	 * LOGOUT - revoga o refresh token informado
	 */
	public void logout(String refreshTokenRaw) {
		String hash = TokenHashUtils.sha256(refreshTokenRaw);

		RefreshToken rt = refreshTokenRepository.findByTokenHash(hash)
				.orElseThrow(() -> new RuntimeException("Refresh token inválido."));

		rt.setRevoked(true);
		refreshTokenRepository.save(rt);
	}

	/**
	 * ME - retorna dados do usuário autenticado (via email do JWT)
	 */
	public MeResponseDTO me(String email) {

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

		if (Boolean.FALSE.equals(user.getAtivo())) {
			throw new RuntimeException("Usuário inativo.");
		}

		return new MeResponseDTO(user.getId(), user.getNome(), user.getEmail(), user.getTelefone(), user.getPerfil());
	}

	/**
	 * Gera token forte e aleatório (URL-safe)
	 */
	private String generateSecureToken() {
		byte[] bytes = new byte[48];
		new SecureRandom().nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	/**
	 * REGISTER (público) - cria um usuário do tipo CLIENTE (self-register) - valida
	 * e-mail único - gera senha_hash com BCrypt - retorna dados do usuário criado
	 * (sem tokens)
	 */
	public MeResponseDTO register(RegisterRequestDTO dto) {

		// valida e-mail único
		if (userRepository.existsByEmail(dto.getEmail())) {
			throw new BusinessException("Já existe um usuário com este e-mail.");
		}

		User user = new User();
		user.setNome(dto.getNome());
		user.setEmail(dto.getEmail());
		user.setTelefone(dto.getTelefone());

		// REGRA: cadastro público sempre cria CLIENTE
		user.setPerfil(PerfilUser.CLIENTE);

		user.setAtivo(true);
		user.setSenhaHash(passwordEncoder.encode(dto.getSenha()));

		userRepository.save(user);

		// Retorna o "me" do usuário recém criado
		return new MeResponseDTO(user.getId(), user.getNome(), user.getEmail(), user.getTelefone(), user.getPerfil());
	}
	
	@Transactional
	public MeResponseDTO updateMe(UserMeUpdateRequestDTO dto) {

	    /*
	     * Recupera a autenticação atual a partir do SecurityContext.
	     * O JwtAuthenticationFilter já validou o token e populou o contexto.
	     */
	    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

	    if (auth == null || !auth.isAuthenticated()) {
	        throw new ForbiddenException("Usuário não autenticado.");
	    }

	    /*
	     * O subject do JWT é o email do usuário.
	     * Esse padrão já foi definido no login.
	     */
	    String email = auth.getName();

	    /*
	     * Busca o usuário no banco.
	     * Caso não exista, algo está inconsistente com o token.
	     */
	    User user = userRepository.findByEmail(email)
	            .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));

	    /*
	     * Defesa extra: usuário inativo não pode atualizar seus dados.
	     */
	    if (Boolean.FALSE.equals(user.getAtivo())) {
	        throw new ForbiddenException("Usuário inativo.");
	    }

	    /*
	     * Atualiza SOMENTE os campos permitidos.
	     */
	    user.setNome(dto.getNome().trim());
	    user.setTelefone(
	        dto.getTelefone() == null ? null : dto.getTelefone().trim()
	    );

	    /*
	     * Se a senha foi enviada, gera novo hash.
	     * Caso contrário, mantém o hash atual.
	     */
	    if (dto.getSenha() != null && !dto.getSenha().isBlank()) {
	        user.setSenhaHash(passwordEncoder.encode(dto.getSenha()));
	    }

	    /*
	     * Persiste as alterações.
	     * updatedAt será atualizado automaticamente pelo @PreUpdate.
	     */
	    userRepository.save(user);

	    /*
	     * Retorna os dados atualizados do usuário autenticado.
	     */
	    return new MeResponseDTO(
	        user.getId(),
	        user.getNome(),
	        user.getEmail(),
	        user.getTelefone(),
	        user.getPerfil()
	    );
	}


}
