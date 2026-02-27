package br.com.bravvo.api.security;

import br.com.bravvo.api.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Serviço responsável por todas as operações relacionadas a JWT (JSON Web Token).
 *
 * Responsabilidades:
 * - Gerar Access Tokens JWT
 * - Validar tokens JWT
 * - Extrair informações do token (subject/email + claims)
 *
 * NÃO acessa banco e NÃO valida permissões.
 */
@Service
public class JwtService {

    private final JwtProperties props;
    private final SecretKey key;

    public JwtService(JwtProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Gera um Access Token JWT para o usuário autenticado.
     *
     * Claims:
     * - uid
     * - perfil
     * - salao_id (multi-tenant)
     * - slug (opcional)
     */
    public String generateAccessToken(User user, Long salaoId, String slug) {

        Instant now = Instant.now();
        Instant exp = now.plus(props.getAccessTokenMinutes(), ChronoUnit.MINUTES);

        var builder = Jwts.builder()
                .issuer(props.getIssuer())
                .subject(user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .claim("uid", user.getId())
                .claim("perfil", user.getPerfil().name());

        if (salaoId != null) {
            builder.claim("salao_id", salaoId);
        }
        if (slug != null && !slug.isBlank()) {
            builder.claim("slug", slug);
        }

        return builder.signWith(key).compact();
    }

    public boolean isValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getSubject(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * Extrai salao_id (Long) do token.
     * Retorna null se não existir.
     */
    public Long getEstabelecimentoId(String token) {
        Object val = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("salao_id");

        if (val == null) return null;

        if (val instanceof Integer i) return i.longValue();
        if (val instanceof Long l) return l;
        if (val instanceof String s) return Long.parseLong(s);

        return null;
    }

    public long getAccessTokenExpiresInSeconds() {
        return props.getAccessTokenMinutes() * 60L;
    }

    public int getRefreshTokenDays() {
        return props.getRefreshTokenDays();
    }
}