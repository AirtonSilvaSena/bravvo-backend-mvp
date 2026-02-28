package br.com.bravvo.api.security;

import br.com.bravvo.api.entity.User;
import br.com.bravvo.api.enums.PerfilUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Serviço responsável por operações JWT (JSON Web Token).
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
     * - perfil (do vínculo estabelecimento_users)
     * - estabelecimento_id (multi-tenant)
     * - slug (opcional)
     */
    public String generateAccessToken(User user, Long estabelecimentoId, String slug, PerfilUser perfil) {

        Instant now = Instant.now();
        Instant exp = now.plus(props.getAccessTokenMinutes(), ChronoUnit.MINUTES);

        var builder = Jwts.builder()
                .issuer(props.getIssuer())
                .subject(user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .claim("uid", user.getId());

        if (perfil != null) {
            builder.claim("perfil", perfil.name());
        }

        if (estabelecimentoId != null) {
            builder.claim("estabelecimento_id", estabelecimentoId);
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
        return getClaims(token).getSubject();
    }

    public Long getUserId(String token) {
        Object val = getClaims(token).get("uid");
        return coerceToLong(val);
    }

    public PerfilUser getPerfil(String token) {
        Object val = getClaims(token).get("perfil");
        if (val == null) return null;
        try {
            return PerfilUser.valueOf(val.toString());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extrai estabelecimento_id (Long) do token.
     * Retorna null se não existir.
     */
    public Long getEstabelecimentoId(String token) {
        Object val = getClaims(token).get("estabelecimento_id");
        return coerceToLong(val);
    }

    public long getAccessTokenExpiresInSeconds() {
        return props.getAccessTokenMinutes() * 60L;
    }

    public int getRefreshTokenDays() {
        return props.getRefreshTokenDays();
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Long coerceToLong(Object value) {
        if (value == null) return null;
        if (value instanceof Long l) return l;
        if (value instanceof Integer i) return i.longValue();
        if (value instanceof String s) {
            if (s.isBlank()) return null;
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}