package br.com.bravvo.api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuração de segurança da aplicação (JWT + Stateless).
 *
 * Nesta fase (Aula 4):
 * - Endpoints públicos:
 *   - /api/auth/** (login, refresh, logout, me)
 *   - Swagger: /swagger-ui/** e /v3/api-docs/**
 *
 * - Todo o restante exige JWT válido via:
 *   Authorization: Bearer <token>
 *
 * - O filtro JwtAuthenticationFilter é responsável por:
 *   - ler o header Authorization
 *   - validar o JWT
 *   - autenticar o usuário no SecurityContext
 */
@Configuration
@EnableMethodSecurity // Habilita @PreAuthorize nos controllers (Aula 5)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    /**
     * PasswordEncoder usado para:
     * - comparar senha digitada com o hash no login
     * - gerar hash no cadastro de usuários (quando aplicarmos)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * AuthenticationManager padrão do Spring.
     * (Não é obrigatório para o JWT puro, mas é útil para evoluções e padrões.)
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Cadeia de filtros do Spring Security.
     *
     * Aqui configuramos:
     * - CSRF desabilitado (API stateless)
     * - CORS habilitado (integra com WebConfig)
     * - sessionCreationPolicy = STATELESS
     * - regras de autorização por endpoint
     * - filtro JWT antes do filtro padrão do Spring
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            // API REST stateless -> não usa cookie/sessão, então CSRF não faz sentido
            .csrf(csrf -> csrf.disable())

            // Permite que o CORS configurado no WebConfig seja respeitado pelo Security
            .cors(cors -> {})

            // Garante que o Spring não vai criar/manter sessão HTTP
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Define quais rotas são públicas e quais são protegidas
            .authorizeHttpRequests(auth -> auth
                // rotas públicas
            		.requestMatchers(
            		        "/api/auth/login",
            		        "/api/auth/refresh",
            		        "/api/auth/logout",
            		        "/api/auth/register",
            		        "/api/public/**",
            		        "/swagger-ui/**",
            		        "/v3/api-docs/**"
            		    ).permitAll()

            		    // =========================
            		    // Endpoints protegidos
            		    // =========================
            		    .requestMatchers("/api/auth/me").authenticated()

            		    // =========================
            		    // Qualquer outro endpoint exige JWT
            		    // =========================
            		    .anyRequest().authenticated()
          )

            // Registra o filtro JWT ANTES do filtro padrão de autenticação do Spring
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
