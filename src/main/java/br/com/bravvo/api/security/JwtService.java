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
 * Responsabilidades desta classe:
 * - Gerar Access Tokens JWT
 * - Validar tokens JWT
 * - Extrair informações do token (subject/email)
 * - Centralizar regras de expiração de token
 *
 * Essa classe NÃO acessa banco de dados e NÃO valida permissões.
 * Ela apenas cria e interpreta tokens.
 */
@Service
public class JwtService {

    /**
     * Propriedades do JWT carregadas do application.yml / application.properties.
     *
     * Exemplo de propriedades:
     * - secret
     * - issuer
     * - accessTokenMinutes
     * - refreshTokenDays
     */
    private final JwtProperties props;

    /**
     * Chave criptográfica usada para assinar e validar os tokens JWT.
     *
     * É criada uma única vez no construtor a partir do secret configurado.
     */
    private final SecretKey key;

    /**
     * Construtor do JwtService.
     *
     * @param props propriedades de configuração do JWT
     */
    public JwtService(JwtProperties props) {
        this.props = props;

        // Gera a chave HMAC a partir do secret definido na configuração
        // O secret precisa ter tamanho adequado para o algoritmo HS256
        this.key = Keys.hmacShaKeyFor(
                props.getSecret().getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Gera um Access Token JWT para o usuário autenticado.
     *
     * Conteúdo do token:
     * - issuer: emissor do token (ex: bravvo-api)
     * - subject: email do usuário (identificador principal)
     * - issuedAt: data/hora de emissão
     * - expiration: data/hora de expiração
     * - uid: ID do usuário
     * - perfil: perfil do usuário (ADMIN, FUNCIONARIO, CLIENTE, etc)
     *
     * @param user usuário autenticado
     * @return token JWT assinado
     */
    public String generateAccessToken(User user) {

        // Momento atual
        Instant now = Instant.now();

        // Calcula a expiração com base nos minutos configurados
        Instant exp = now.plus(
                props.getAccessTokenMinutes(),
                ChronoUnit.MINUTES
        );

        // Monta e assina o token JWT
        return Jwts.builder()
                // Emissor do token (boa prática para validação futura)
                .issuer(props.getIssuer())

                // Subject padrão do JWT (usamos o email)
                .subject(user.getEmail())

                // Data de emissão
                .issuedAt(Date.from(now))

                // Data de expiração
                .expiration(Date.from(exp))

                // Claims customizadas
                .claim("uid", user.getId())
                .claim("perfil", user.getPerfil().name())

                // Assina o token com a chave secreta
                .signWith(key)

                // Gera o token final (String)
                .compact();
    }

    /**
     * Valida se um token JWT é válido.
     *
     * Regras de validação:
     * - Assinatura correta
     * - Token não expirado
     * - Estrutura válida
     *
     * NÃO valida permissões nem se o usuário existe.
     *
     * @param token token JWT
     * @return true se o token for válido, false caso contrário
     */
    public boolean isValid(String token) {
        try {
            // Tenta fazer o parse e validar assinatura + expiração
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);

            return true;
        } catch (Exception e) {
            // Qualquer exceção indica token inválido
            return false;
        }
    }

    /**
     * Extrai o subject (email) do token JWT.
     *
     * Esse método assume que o token já é válido.
     *
     * @param token token JWT
     * @return subject (email do usuário)
     */
    public String getSubject(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * Retorna o tempo de expiração do Access Token em segundos.
     *
     * Esse valor é útil para:
     * - Frontend saber quando renovar token
     * - Payload de resposta do login
     *
     * @return tempo de expiração em segundos
     */
    public long getAccessTokenExpiresInSeconds() {
        return props.getAccessTokenMinutes() * 60L;
    }

    /**
     * Retorna a quantidade de dias de validade do Refresh Token.
     *
     * Normalmente usado no AuthService.
     *
     * @return dias de validade do refresh token
     */
    public int getRefreshTokenDays() {
        return props.getRefreshTokenDays();
    }
}
