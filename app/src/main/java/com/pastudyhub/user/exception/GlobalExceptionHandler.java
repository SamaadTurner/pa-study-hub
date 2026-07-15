package com.pastudyhub.user.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for the User Service.
 *
 * <p>Returns RFC 7807 Problem Details JSON for all errors. This ensures:
 * <ul>
 *   <li>Consistent error format across all endpoints</li>
 *   <li>No stack traces or internal details exposed to clients</li>
 *   <li>Structured error data that the frontend can reliably parse</li>
 * </ul>
 *
 * <p>Example response for a validation error:
 * <pre>{@code
 * {
 *   "type": "https://pastudyhub.com/errors/validation",
 *   "title": "Validation Failed",
 *   "status": 400,
 *   "detail": "Request body has validation errors",
 *   "instance": "/api/v1/auth/register",
 *   "timestamp": "2026-02-09T10:00:00Z",
 *   "errors": { "email": "must be a valid email address", "password": "must not be blank" }
 * }
 * }</pre>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR_TYPE_BASE = "https://pastudyhub.com/errors/";

    /**
     * Handles all custom StudyHubException subclasses.
     * Maps the exception's HTTP status to the response status.
     */
    @ExceptionHandler(StudyHubException.class)
    public ProblemDetail handleStudyHubException(StudyHubException ex) {
        // Log at WARN for 4xx, ERROR for 5xx — avoid leaking details in logs for security
        if (ex.getHttpStatus().is5xxServerError()) {
            log.error("Server error: {}", ex.getMessage(), ex);
        } else {
            log.warn("Client error [{}]: {}", ex.getHttpStatus(), ex.getMessage());
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getHttpStatus(), ex.getMessage());
        problem.setTitle(ex.getHttpStatus().getReasonPhrase());
        problem.setType(URI.create(ERROR_TYPE_BASE + ex.getHttpStatus().value()));
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }

    /**
     * Handles Jakarta Bean Validation failures (@Valid on request DTOs).
     * Returns field-level error messages to help the client show targeted form errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value",
                        (existing, replacement) -> existing  // keep first error per field
                ));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Request body has validation errors");
        problem.setTitle("Validation Failed");
        problem.setType(URI.create(ERROR_TYPE_BASE + "validation"));
        problem.setProperty("timestamp", Instant.now().toString());
        problem.setProperty("errors", fieldErrors);
        return problem;
    }

    /**
     * Catch-all for unexpected exceptions.
     * Returns a generic 500 — NEVER exposing stack traces or internal details.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedException(Exception ex) {
        log.error("Unexpected error", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.");
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create(ERROR_TYPE_BASE + "internal"));
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }
}
