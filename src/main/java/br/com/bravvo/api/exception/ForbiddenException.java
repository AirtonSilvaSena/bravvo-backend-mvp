package br.com.bravvo.api.exception;

/**
 * Exceção lançada quando o usuário autenticado
 * não possui permissão para executar a ação solicitada.
 *
 * Deve resultar em HTTP 403 (FORBIDDEN).
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
