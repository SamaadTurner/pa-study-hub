package com.pastudyhub.user.exception;

import org.springframework.http.HttpStatus;

public class TooManyEmailsException extends StudyHubException {
    public TooManyEmailsException(String message) {
        super(message, HttpStatus.TOO_MANY_REQUESTS);
    }
}
