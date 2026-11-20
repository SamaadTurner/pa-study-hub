package com.pastudyhub.flashcard.exception;

import org.springframework.http.HttpStatus;

/** The user, or the whole site, has used up the room for pictures. */
public class ImageLimitException extends StudyHubException {
    public ImageLimitException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
