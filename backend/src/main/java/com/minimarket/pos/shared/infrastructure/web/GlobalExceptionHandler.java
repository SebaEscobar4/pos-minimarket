package com.minimarket.pos.shared.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import com.minimarket.pos.shared.application.error.ApplicationException;
import com.minimarket.pos.shared.application.error.ProblemType;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String PROBLEM_PREFIX = "urn:pos-minimarket:problem:";

    @ExceptionHandler(ApplicationException.class)
    ResponseEntity<ProblemDetail> handleApplicationException(
            ApplicationException exception, HttpServletRequest request) {
        HttpStatus status = statusFor(exception.problemType());
        return response(
                status,
                exception.problemType().code(),
                titleFor(status),
                exception.getMessage(),
                request,
                List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<FieldViolation> violations = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toViolation)
                .toList();
        return response(
                HttpStatus.BAD_REQUEST,
                ProblemType.VALIDATION.code(),
                "Solicitud inválida",
                ProblemType.VALIDATION.defaultMessage(),
                request,
                violations);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ProblemDetail> handleConstraintViolation(
            ConstraintViolationException exception, HttpServletRequest request) {
        List<FieldViolation> violations = exception.getConstraintViolations().stream()
                .map(violation -> new FieldViolation(
                        violation.getPropertyPath().toString(), violation.getMessage()))
                .toList();
        return response(
                HttpStatus.BAD_REQUEST,
                ProblemType.VALIDATION.code(),
                "Solicitud inválida",
                ProblemType.VALIDATION.defaultMessage(),
                request,
                violations);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> handleUnreadableBody(
            HttpMessageNotReadableException exception, HttpServletRequest request) {
        return response(
                HttpStatus.BAD_REQUEST,
                "malformed-body",
                "Cuerpo inválido",
                "El cuerpo JSON no tiene un formato válido.",
                request,
                List.of());
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    ResponseEntity<ProblemDetail> handleNotFound(
            NoHandlerFoundException exception, HttpServletRequest request) {
        return response(
                HttpStatus.NOT_FOUND,
                ProblemType.NOT_FOUND.code(),
                "Recurso no encontrado",
                ProblemType.NOT_FOUND.defaultMessage(),
                request,
                List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(
            Exception exception, HttpServletRequest request) {
        String correlationId = correlationId(request);
        LOGGER.error("Unhandled request failure. correlationId={}", correlationId, exception);
        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "internal-error",
                "Error interno",
                "Ocurrió un error inesperado.",
                request,
                List.of());
    }

    private ResponseEntity<ProblemDetail> response(
            HttpStatus status,
            String code,
            String title,
            String detail,
            HttpServletRequest request,
            List<FieldViolation> violations) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(PROBLEM_PREFIX + code));
        problem.setTitle(title);
        problem.setProperty("code", code);
        problem.setProperty("correlationId", correlationId(request));
        if (!violations.isEmpty()) {
            problem.setProperty("fieldErrors", violations);
        }
        return ResponseEntity.status(status).body(problem);
    }

    private String correlationId(HttpServletRequest request) {
        Object value = request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);
        return value instanceof String id ? id : UUID.randomUUID().toString();
    }

    private FieldViolation toViolation(FieldError error) {
        String message = error.getDefaultMessage() == null ? "Valor inválido" : error.getDefaultMessage();
        return new FieldViolation(error.getField(), message);
    }

    private HttpStatus statusFor(ProblemType problemType) {
        return switch (problemType) {
            case VALIDATION -> HttpStatus.BAD_REQUEST;
            case AUTHENTICATION -> HttpStatus.UNAUTHORIZED;
            case RATE_LIMIT -> HttpStatus.TOO_MANY_REQUESTS;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
        };
    }

    private String titleFor(HttpStatus status) {
        String reason = status.getReasonPhrase().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(reason.charAt(0)) + reason.substring(1);
    }
}
