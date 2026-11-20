package com.pastudyhub.user.exception;

import org.springframework.http.HttpStatus;

public class EmailNotSentException extends StudyHubException {
    public EmailNotSentException(String message) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
