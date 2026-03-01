package br.com.bravvo.api.security;

import br.com.bravvo.api.exception.ForbiddenException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

/**
 * TenantContext
 *
 * Fonte única de verdade do contexto autenticado (usuário + tenant).
 *
 * Regras:
 * - Em ambiente multi-tenant, endpoints autenticados NÃO devem "adivinhar" o estabelecimento.
 * - O estabelecimento atual vem do JWT e é injetado no SecurityContext pelo JwtAuthenticationFilter.
 *
 * Observações:
 * - Esta classe NÃO acessa banco.
 * - Apenas lê SecurityContext.
 * - Para evitar dependência de "email global", preferimos userId vindo do token.
 */
public final class TenantContext {

    private TenantContext() {}

    /**
     * Obtém o Authentication do SecurityContext.
     *
     * @return authentication autenticado
     * @throws ForbiddenException se não estiver autenticado
     */
    public static Authentication getAuthenticationOrThrow() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ForbiddenException("Usuário não autenticado.");
        }
        return auth;
    }

    /**
     * Retorna o e-mail do usuário autenticado (subject do JWT).
     *
     * @return email
     * @throws ForbiddenException se não houver e-mail
     */
    public static String getEmailOrThrow() {
        Authentication auth = getAuthenticationOrThrow();
        String email = auth.getName();
        if (email == null || email.isBlank()) {
            throw new ForbiddenException("E-mail do usuário autenticado não encontrado.");
        }
        return email;
    }

    /**
     * Retorna o estabelecimentoId (tenant) do contexto autenticado.
     *
     * Fonte:
     * - auth.getDetails(): Map com "estabelecimentoId" (populado pelo JwtAuthenticationFilter)
     *
     * @return estabelecimentoId
     * @throws ForbiddenException se não existir no token/contexto
     */
    public static Long getEstabelecimentoIdOrThrow() {
        Authentication auth = getAuthenticationOrThrow();
        Long estabelecimentoId = readLongFromDetails(auth.getDetails(), "estabelecimentoId");

        if (estabelecimentoId == null) {
            throw new ForbiddenException("Contexto de estabelecimento não informado no token.");
        }
        return estabelecimentoId;
    }

    /**
     * Retorna o userId do token/contexto.
     *
     * Fonte:
     * - auth.getDetails(): Map com "userId" (populado pelo JwtAuthenticationFilter)
     *
     * @return userId
     * @throws ForbiddenException se não existir no token/contexto
     */
    public static Long getUserIdOrThrow() {
        Authentication auth = getAuthenticationOrThrow();
        Long userId = readLongFromDetails(auth.getDetails(), "userId");

        if (userId == null) {
            throw new ForbiddenException("Identificador do usuário não informado no token.");
        }
        return userId;
    }

    @SuppressWarnings("unchecked")
    private static Long readLongFromDetails(Object details, String key) {
        if (!(details instanceof Map<?, ?> map)) return null;

        Object v = map.get(key);
        if (v == null) return null;

        if (v instanceof Long l) return l;
        if (v instanceof Integer i) return i.longValue();

        if (v instanceof String s) {
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