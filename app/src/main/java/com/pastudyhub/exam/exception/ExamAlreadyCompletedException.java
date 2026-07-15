package com.pastudyhub.exam.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class ExamAlreadyCompletedException extends StudyHubException {
    public ExamAlreadyCompletedException(UUID sessionId) {
        super("Exam session is already completed: " + sessionId, HttpStatus.CONFLICT);
    }
}
