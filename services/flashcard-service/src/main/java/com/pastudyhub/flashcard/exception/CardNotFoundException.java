package com.pastudyhub.flashcard.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class CardNotFoundException extends StudyHubException {
    public CardNotFoundException(UUID id) {
        super("Card not found: " + id, HttpStatus.NOT_FOUND);
    }
}
