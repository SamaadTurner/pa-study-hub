package com.pastudyhub.flashcard.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedDeckAccessException extends StudyHubException {
    public UnauthorizedDeckAccessException() {
        super("You do not have permission to modify this deck", HttpStatus.FORBIDDEN);
    }

    // Used when the user is only trying to look at the deck, so the message fits
    public UnauthorizedDeckAccessException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
