package com.pastudyhub.exam.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a written question breaks a rule the request validation cannot check. */
public class InvalidQuestionException extends StudyHubException {
    public InvalidQuestionException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
