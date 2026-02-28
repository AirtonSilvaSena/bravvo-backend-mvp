package br.com.bravvo.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Filtro responsável por:
 *
 * - Interceptar requisições
 * - Extrair o JWT do header Authorization
 * - Validar o token
 * - Carregar o usuário no contexto do tenant
 * - Popular o SecurityContext
 *
 * Multi-tenant:
 * - estabelecimento_id vem do claim do JWT
 * - perfil vem do vínculo estabelecimento_users
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            CustomUserDetailsService userDetailsService
    ) {
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

        // Se não tiver Authorization ou não for Bearer, segue fluxo
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);

        // Se token inválido, segue (Security vai barrar depois se necessário)
        if (!jwtService.isValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        String email = jwtService.getSubject(token);
        Long estabelecimentoId = jwtService.getEstabelecimentoId(token);
        Long userId = jwtService.getUserId(token);

        // Já autenticado? então não reprocessa
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails userDetails;

            if (estabelecimentoId != null) {
                // Multi-tenant: carrega usuário dentro do contexto do estabelecimento
                userDetails = userDetailsService
                        .loadUserByEmailAndEstabelecimentoId(email, estabelecimentoId);
            } else {
                // fallback (não recomendado para multi-tenant real)
                userDetails = userDetailsService.loadUserByUsername(email);
            }

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            // Adiciona informações extras no contexto (usado pelo TenantContext)
            Map<String, Object> details = new HashMap<>();
            details.put("estabelecimentoId", estabelecimentoId);
            details.put("userId", userId);

            authToken.setDetails(details);

            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }
}