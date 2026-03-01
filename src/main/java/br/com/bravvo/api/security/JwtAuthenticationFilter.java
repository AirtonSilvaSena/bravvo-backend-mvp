package br.com.bravvo.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * JwtAuthenticationFilter
 *
 * Responsável por:
 * - Interceptar requisições
 * - Extrair o JWT do header Authorization
 * - Validar o token
 * - Carregar o usuário (UserDetails)
 * - Popular o SecurityContext
 *
 * Multi-tenant:
 * - estabelecimentoId vem de claim no JWT
 * - userId vem de claim no JWT
 * - perfil/authorities devem ser derivados do vínculo (estabelecimento_users) no CustomUserDetailsService
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
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // Sem Bearer -> segue
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7).trim();

        // Token inválido -> segue (Security barra se rota exigir)
        if (token.isBlank() || !jwtService.isValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Já autenticado -> não reprocessa
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String email = jwtService.getSubject(token);
        Long estabelecimentoId = jwtService.getEstabelecimentoId(token);
        Long userId = jwtService.getUserId(token);

        // Segurança: token precisa carregar tenant e userId no modelo multi-tenant real
        if (email == null || email.isBlank() || estabelecimentoId == null || userId == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Carrega o usuário dentro do tenant (vínculo)
        UserDetails userDetails = userDetailsService.loadUserByEmailAndEstabelecimentoId(email, estabelecimentoId);

        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

        // Details padronizado (TenantContext lê daqui)
        Map<String, Object> details = new HashMap<>();
        details.put("estabelecimentoId", estabelecimentoId);
        details.put("userId", userId);

        authToken.setDetails(details);
        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
    }
}