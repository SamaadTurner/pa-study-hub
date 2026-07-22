package com.pastudyhub.user.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a registration attempt uses an email that already has an account. */
public class EmailAlreadyExistsException extends StudyHubException {
    public EmailAlreadyExistsException(String email) {
        super("An account with email '" + email + "' already exists", HttpStatus.CONFLICT);
    }
}
