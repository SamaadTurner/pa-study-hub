package com.pastudyhub.user.exception;

import org.springframework.http.HttpStatus;

/**
 * Base exception for all PA Study Hub application exceptions.
 *
 * <p>All custom exceptions in every microservice extend this class.
 * It carries the HTTP status code that should be returned to the client,
 * enabling the global exception handler to map exceptions to responses
 * without using service-layer-specific catch blocks.
 *
 * <p>Example usage:
 * <pre>{@code
 * throw new UserNotFoundException("No user found with email: " + email);
 * // GlobalExceptionHandler catches StudyHubException → returns 404 Problem Details JSON
 * }</pre>
 */
public abstract class StudyHubException extends RuntimeException {

    private final HttpStatus httpStatus;

    protected StudyHubException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    protected StudyHubException(String message, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
