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
 * Ele:
 * 1) Lê Authorization: Bearer <token>
 * 2) Valida JWT (assinatura + expiração)
 * 3) Extrai subject (email) e estabelecimento_id
 * 4) Autentica no SecurityContext com ROLE_<perfil do vínculo>
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
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (!jwtService.isValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        String email = jwtService.getSubject(token);
        Long estabelecimentoId = jwtService.getEstabelecimentoId(token);
        Long userId = jwtService.getUserId(token);
        var perfil = jwtService.getPerfil(token);

        if (SecurityContextHolder.getContext().getAuthentication() == null) {

            // Multi-tenant é obrigatório para endpoints autenticados do Bravvo
            UserDetails userDetails;
            if (estabelecimentoId != null) {
                userDetails = userDetailsService.loadUserByEmailAndEstabelecimentoId(email, estabelecimentoId);
            } else {
                // fallback (não ideal) — mantém compatibilidade
                userDetails = userDetailsService.loadUserByUsername(email);
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            var webDetails = new WebAuthenticationDetailsSource().buildDetails(request);

            authentication.setDetails(java.util.Map.of(
                    "estabelecimentoId", estabelecimentoId,
                    "userId", userId,
                    "perfil", perfil != null ? perfil.name() : null,
                    "web", webDetails
            ));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}