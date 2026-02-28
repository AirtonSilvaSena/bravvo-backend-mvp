package br.com.bravvo.api.security;

import br.com.bravvo.api.exception.ForbiddenException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * TenantContext
 *
 * Fonte única de verdade do contexto autenticado (usuário + tenant).
 *
 * Objetivo:
 * - Em ambiente multi-tenant, endpoints autenticados NÃO devem "adivinhar" o estabelecimento.
 * - O estabelecimento atual vem do JWT (claim) no momento do login com slug.
 *
 * Observação:
 * - Essa classe NÃO acessa banco. Apenas lê SecurityContext.
 */
public class TenantContext {

    private TenantContext() { }

    public static Authentication getAuthenticationOrThrow() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ForbiddenException("Usuário não autenticado.");
        }
        return auth;
    }

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
     * Esse valor deve vir do JWT (login com slug).
     */
    public static Long getEstabelecimentoIdOrThrow() {
        Authentication auth = getAuthenticationOrThrow();

        Long fromPrincipal = tryGetLongViaGetter(auth.getPrincipal(), "getEstabelecimentoId");
        if (fromPrincipal != null) return fromPrincipal;

        Object details = auth.getDetails();
        Long fromDetails = tryGetLongFromDetails(details, "estabelecimentoId");
        if (fromDetails != null) return fromDetails;

        throw new ForbiddenException("Contexto de estabelecimento não informado no token.");
    }

    /**
     * Opcional: retorna userId do token se disponível nos details.
     * Útil para serviços que prefiram userId em vez de e-mail.
     */
    public static Long getUserIdOrThrow() {
        Authentication auth = getAuthenticationOrThrow();

        Long fromPrincipal = tryGetLongViaGetter(auth.getPrincipal(), "getUserId");
        if (fromPrincipal != null) return fromPrincipal;

        Object details = auth.getDetails();
        Long fromDetails = tryGetLongFromDetails(details, "userId");
        if (fromDetails != null) return fromDetails;

        throw new ForbiddenException("Identificador do usuário não informado no token.");
    }

    private static Long tryGetLongViaGetter(Object target, String getterName) {
        if (target == null) return null;
        try {
            Method m = target.getClass().getMethod(getterName);
            Object value = m.invoke(target);
            return coerceToLong(value);
        } catch (NoSuchMethodException e) {
            return null;
        } catch (Exception e) {
            throw new ForbiddenException("Falha ao ler contexto do token: " + getterName);
        }
    }

    @SuppressWarnings("unchecked")
    private static Long tryGetLongFromDetails(Object details, String key) {
        if (details == null) return null;

        if (details instanceof Map<?, ?> map) {
            Object value = map.get(key);
            return coerceToLong(value);
        }

        return null;
    }

    private static Long coerceToLong(Object value) {
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