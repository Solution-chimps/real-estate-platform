package com.br.real_estate_platform.exception;

import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@Slf4j
public class ApiExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	public ProblemDetail handleNotFound(ResourceNotFoundException exception) {
		return problem(HttpStatus.NOT_FOUND, exception.getMessage());
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ProblemDetail handleNoResource(NoResourceFoundException exception) {
		return problem(HttpStatus.NOT_FOUND, "Recurso não encontrado");
	}

	@ExceptionHandler(InvalidStatusTransitionException.class)
	public ProblemDetail handleInvalidTransition(InvalidStatusTransitionException exception) {
		return problem(HttpStatus.CONFLICT, exception.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
		Map<String, String> errors = new LinkedHashMap<>();
		for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
			errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
		}
		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Revise os campos informados");
		problem.setProperty("errors", errors);
		return problem;
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ProblemDetail handleConstraintViolation(ConstraintViolationException exception) {
		Map<String, String> errors = new LinkedHashMap<>();
		exception.getConstraintViolations()
				.forEach(violation -> errors.putIfAbsent(violation.getPropertyPath().toString(), violation.getMessage()));
		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Revise os campos informados");
		problem.setProperty("errors", errors);
		return problem;
	}

	@ExceptionHandler({
			HttpMessageNotReadableException.class,
			MethodArgumentTypeMismatchException.class,
			MissingServletRequestParameterException.class,
			MissingServletRequestPartException.class,
			HandlerMethodValidationException.class,
			PropertyReferenceException.class })
	public ProblemDetail handleBadRequest(Exception exception) {
		return problem(HttpStatus.BAD_REQUEST, "Requisição inválida");
	}

	@ExceptionHandler(UnsupportedPhotoException.class)
	public ProblemDetail handleUnsupportedPhoto(UnsupportedPhotoException exception) {
		return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, exception.getMessage());
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ProblemDetail handleUploadTooLarge(MaxUploadSizeExceededException exception) {
		return problem(HttpStatus.PAYLOAD_TOO_LARGE, "Cada foto deve ter no máximo 10 MB");
	}

	@ExceptionHandler(TooManyLoginAttemptsException.class)
	public ProblemDetail handleTooManyAttempts(TooManyLoginAttemptsException exception) {
		return problem(HttpStatus.TOO_MANY_REQUESTS, exception.getMessage());
	}

	@ExceptionHandler({ BadCredentialsException.class, DisabledException.class, LockedException.class })
	public ProblemDetail handleBadCredentials(AuthenticationException exception) {
		return problem(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos");
	}

	@ExceptionHandler({ InvalidMfaCodeException.class, MfaChallengeExpiredException.class })
	public ProblemDetail handleMfaRejected(RuntimeException exception) {
		return problem(HttpStatus.UNAUTHORIZED, exception.getMessage());
	}

	@ExceptionHandler({ MfaNotConfiguredException.class, MfaAlreadyConfiguredException.class })
	public ProblemDetail handleMfaState(RuntimeException exception) {
		return problem(HttpStatus.CONFLICT, exception.getMessage());
	}

	@ExceptionHandler(AuthenticationException.class)
	public ProblemDetail handleAuthentication(AuthenticationException exception) {
		return problem(HttpStatus.UNAUTHORIZED, "Autenticação necessária");
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ProblemDetail handleAccessDenied(AccessDeniedException exception) {
		return problem(HttpStatus.FORBIDDEN, "Acesso negado");
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleUnexpected(Exception exception) {
		String errorId = UUID.randomUUID().toString();
		log.error("Unhandled error {}", errorId, exception);
		ProblemDetail problem = problem(HttpStatus.INTERNAL_SERVER_ERROR,
				"Erro interno. Tente novamente mais tarde");
		problem.setProperty("errorId", errorId);
		return problem;
	}

	private static ProblemDetail problem(HttpStatus status, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(status.getReasonPhrase());
		return problem;
	}
}
