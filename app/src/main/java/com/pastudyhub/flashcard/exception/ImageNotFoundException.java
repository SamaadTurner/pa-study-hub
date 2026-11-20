package com.pastudyhub.flashcard.exception;

import org.springframework.http.HttpStatus;

/** No such picture, or one this user may not see (the answer is the same, on purpose). */
public class ImageNotFoundException extends StudyHubException {
    public ImageNotFoundException() {
        super("Picture not found", HttpStatus.NOT_FOUND);
    }
}
