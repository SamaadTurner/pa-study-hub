package com.pastudyhub.exam.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a user opens or changes a question someone else wrote, or a starter question. */
public class QuestionAccessDeniedException extends StudyHubException {
    public QuestionAccessDeniedException() {
        super("You can only open and change questions you wrote", HttpStatus.FORBIDDEN);
    }
}
