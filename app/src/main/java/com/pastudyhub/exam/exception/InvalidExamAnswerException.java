package com.pastudyhub.exam.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a submitted answer does not fit the exam: the question is not one of
 * the exam's questions, or the option is not one of the question's options.
 */
public class InvalidExamAnswerException extends StudyHubException {
    public InvalidExamAnswerException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
