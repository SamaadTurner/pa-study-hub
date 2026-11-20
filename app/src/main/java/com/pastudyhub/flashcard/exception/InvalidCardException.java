package com.pastudyhub.flashcard.exception;

import org.springframework.http.HttpStatus;

/** A card that breaks a rule the request annotations can't check (depends on the card's type). */
public class InvalidCardException extends StudyHubException {
    public InvalidCardException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
