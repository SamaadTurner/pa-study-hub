package com.pastudyhub.flashcard.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class DeckNotFoundException extends StudyHubException {
    public DeckNotFoundException(UUID id) {
        super("Deck not found: " + id, HttpStatus.NOT_FOUND);
    }
}
