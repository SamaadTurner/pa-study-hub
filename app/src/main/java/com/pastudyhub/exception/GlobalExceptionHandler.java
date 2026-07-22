package com.pastudyhub.exception;

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
 * Global exception handler for PA Study Hub monolith.
 * Returns RFC 7807 Problem Details JSON — never exposes stack traces.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR_TYPE_BASE = "https://pastudyhub.com/errors/";

    /**
     * Handles all StudyHubException subclasses from any module.
     * Uses reflection to get httpStatus since StudyHubException classes are in different packages.
     */
    @ExceptionHandler({
        com.pastudyhub.flashcard.exception.StudyHubException.class,
        com.pastudyhub.user.exception.StudyHubException.class,
        com.pastudyhub.exam.exception.StudyHubException.class
    })
    public ProblemDetail handleStudyHubException(RuntimeException ex) {
        HttpStatus status = extractHttpStatus(ex);
        if (status.is5xxServerError()) {
            log.error("Server error: {}", ex.getMessage(), ex);
        } else {
            log.warn("Client error [{}]: {}", status, ex.getMessage());
        }
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problem.setTitle(status.getReasonPhrase());
        problem.setType(URI.create(ERROR_TYPE_BASE + status.value()));
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value",
                        (existing, replacement) -> existing));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Request body has validation errors");
        problem.setTitle("Validation Failed");
        problem.setType(URI.create(ERROR_TYPE_BASE + "validation"));
        problem.setProperty("timestamp", Instant.now().toString());
        problem.setProperty("fieldErrors", fieldErrors);
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedException(Exception ex) {
        log.error("Unexpected error", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred. Please try again later.");
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create(ERROR_TYPE_BASE + "internal"));
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }

    /**
     * Extracts HTTP status from StudyHubException using reflection
     * since we have multiple StudyHubException classes in different packages.
     */
    private HttpStatus extractHttpStatus(RuntimeException ex) {
        try {
            var method = ex.getClass().getMethod("getHttpStatus");
            return (HttpStatus) method.invoke(ex);
        } catch (Exception e) {
            log.warn("Could not extract HTTP status from exception, defaulting to 500", e);
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
}
