package com.pastudyhub.exam.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class QuestionNotFoundException extends StudyHubException {
    public QuestionNotFoundException(UUID questionId) {
        super("Question not found: " + questionId, HttpStatus.NOT_FOUND);
    }
}
