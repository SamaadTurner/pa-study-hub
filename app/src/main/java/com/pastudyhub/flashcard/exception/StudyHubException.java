package com.pastudyhub.flashcard.exception;

import org.springframework.http.HttpStatus;

/** Base exception for all PA Study Hub flashcard service exceptions. */
public abstract class StudyHubException extends RuntimeException {
    private final HttpStatus httpStatus;

    protected StudyHubException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
