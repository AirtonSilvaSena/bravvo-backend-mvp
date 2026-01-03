package br.com.bravvo.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro JWT executado UMA VEZ por request.
 *
 * Ele: 1) Lê o header Authorization 2) Valida o JWT 3) Extrai o email 4)
 * Autentica o usuário no SecurityContext
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final CustomUserDetailsService userDetailsService;

	public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService userDetailsService) {
		this.jwtService = jwtService;
		this.userDetailsService = userDetailsService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		// Lê o header Authorization
		String authHeader = request.getHeader("Authorization");

		// Se não tiver Bearer token, segue a request normalmente
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}

		// Remove o prefixo "Bearer "
		String token = authHeader.substring(7);

		// Valida o token (assinatura + expiração)
		if (!jwtService.isValid(token)) {
			filterChain.doFilter(request, response);
			return;
		}

		// Extrai o email (subject) do JWT
		String email = jwtService.getSubject(token);

		/*
		 * Se ainda não houver autenticação no contexto, autenticamos agora.
		 */
		if (SecurityContextHolder.getContext().getAuthentication() == null) {

			// Carrega o usuário do banco
			UserDetails userDetails = userDetailsService.loadUserByUsername(email);

			/*
			 * Cria um objeto de autenticação do Spring contendo: - usuário - roles
			 */
			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails,
					null, userDetails.getAuthorities());

			// Adiciona detalhes da request (IP, etc.)
			authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

			// Registra o usuário como autenticado
			SecurityContextHolder.getContext().setAuthentication(authentication);
		}

		// Continua a cadeia de filtros
		filterChain.doFilter(request, response);
	}
}
