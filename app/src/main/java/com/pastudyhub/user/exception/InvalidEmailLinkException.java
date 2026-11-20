package com.pastudyhub.user.exception;

import org.springframework.http.HttpStatus;

public class InvalidEmailLinkException extends StudyHubException {
    public InvalidEmailLinkException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
