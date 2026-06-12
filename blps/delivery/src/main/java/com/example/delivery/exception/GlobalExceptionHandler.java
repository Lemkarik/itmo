package com.example.delivery.exception;

import java.time.OffsetDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(NotFoundException.class)
	public ResponseEntity<ApiError> handleNotFound(NotFoundException ex) {
		return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<ApiError> handleBadRequest(BadRequestException ex) {
		return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<ApiError> handleConflict(ConflictException ex) {
		return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		String message = "Invalid value for parameter: " + ex.getName();
		if (isNumericOverflow(ex)) {
			message = "Numeric value is too large for parameter: " + ex.getName();
		}
		return buildResponse(HttpStatus.BAD_REQUEST, message);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex) {
		String message = isNumericOverflow(ex)
				? "Numeric value is too large in request body"
				: "Request body is invalid or malformed";
		return buildResponse(HttpStatus.BAD_REQUEST, message);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiError> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
		String message = "Request data violates database constraints";
		if (containsIgnoreCase(ex, "too long") || containsIgnoreCase(ex, "value too long")) {
			message = "Text value is too long for the database column";
		}
		return buildResponse(HttpStatus.BAD_REQUEST, message);
	}

	@ExceptionHandler(NoHandlerFoundException.class)
	public ResponseEntity<ApiError> handleNoHandler(NoHandlerFoundException ex) {
		String message = "No endpoint found for " + ex.getHttpMethod() + " " + ex.getRequestURL();
		return buildResponse(HttpStatus.NOT_FOUND, message);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
		String message = "Method " + ex.getMethod() + " is not supported for this endpoint";
		return buildResponse(HttpStatus.METHOD_NOT_ALLOWED, message);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
		return buildResponse(HttpStatus.FORBIDDEN, "Access denied: insufficient privileges");
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex) {
		return buildResponse(HttpStatus.UNAUTHORIZED, "Authentication failed: " + ex.getMessage());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleGeneric(Exception ex) {
		return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
	}

	private boolean isNumericOverflow(Throwable ex) {
		Throwable current = ex;
		while (current != null) {
			String message = current.getMessage();
			if (message != null) {
				String normalized = message.toLowerCase();
				if (normalized.contains("out of range")
						|| normalized.contains("numeric value")
						|| normalized.contains("overflow")
						|| normalized.contains("cannot deserialize value of type `java.lang.long`")
						|| normalized.contains("cannot deserialize value of type `java.lang.integer`")) {
					return true;
				}
			}
			current = current.getCause();
		}
		return false;
	}

	private boolean containsIgnoreCase(Throwable ex, String expected) {
		Throwable current = ex;
		String expectedLower = expected.toLowerCase();
		while (current != null) {
			String message = current.getMessage();
			if (message != null && message.toLowerCase().contains(expectedLower)) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}

	private ResponseEntity<ApiError> buildResponse(HttpStatus status, String message) {
		ApiError error = new ApiError(OffsetDateTime.now(), status.value(), status.getReasonPhrase(), message);
		return ResponseEntity.status(status).body(error);
	}
}

