package com.pastudyhub.exam.exception;

import org.springframework.http.HttpStatus;

public abstract class StudyHubException extends RuntimeException {
    private final HttpStatus status;

    protected StudyHubException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }
}
