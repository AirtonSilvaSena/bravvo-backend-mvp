package br.com.bravvo.api.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import br.com.bravvo.api.dto.error.ApiErrorResponse;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest request) {
		return build(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI());
	}

	@ExceptionHandler(NotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleNotFound(NotFoundException ex, HttpServletRequest request) {
		return build(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		String message = ex.getBindingResult().getFieldErrors().stream().map(this::formatFieldError)
				.collect(Collectors.joining("; "));

		return build(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
			HttpServletRequest request) {
		return build(HttpStatus.CONFLICT, "Violação de integridade: registro já existente.", request.getRequestURI());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
		ex.printStackTrace(); // <<< OBRIGATÓRIO AGORA
		return build(HttpStatus.INTERNAL_SERVER_ERROR, ex.getClass().getName() + ": " + ex.getMessage(),
				request.getRequestURI());
	}

	private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String message, String path) {
		ApiErrorResponse body = new ApiErrorResponse(OffsetDateTime.now(), status.value(), status.getReasonPhrase(),
				message, path);
		return ResponseEntity.status(status).body(body);
	}

	private String formatFieldError(FieldError error) {
		return error.getField() + ": " + error.getDefaultMessage();
	}

	@ExceptionHandler(ForbiddenException.class)
	public ResponseEntity<ApiErrorResponse> handleForbidden(ForbiddenException ex, HttpServletRequest request) {
		return build(HttpStatus.FORBIDDEN, ex.getMessage(), request.getRequestURI());
	}
}
