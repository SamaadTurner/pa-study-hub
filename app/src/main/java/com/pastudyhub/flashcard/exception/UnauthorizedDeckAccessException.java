package com.pastudyhub.flashcard.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedDeckAccessException extends StudyHubException {
    public UnauthorizedDeckAccessException() {
        super("You do not have permission to modify this deck", HttpStatus.FORBIDDEN);
    }
}
