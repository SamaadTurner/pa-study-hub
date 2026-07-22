package com.pastudyhub.exam.exception;

import org.springframework.http.HttpStatus;

public class InsufficientQuestionsException extends StudyHubException {
    public InsufficientQuestionsException(int requested, int available) {
        super(String.format(
                "Not enough questions available: requested %d but only %d match the filters",
                requested, available), HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
