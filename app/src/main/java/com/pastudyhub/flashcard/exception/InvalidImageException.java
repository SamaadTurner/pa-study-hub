package com.pastudyhub.flashcard.exception;

import org.springframework.http.HttpStatus;

/** An upload that is not a PNG or JPEG picture, or is too big. */
public class InvalidImageException extends StudyHubException {
    public InvalidImageException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
