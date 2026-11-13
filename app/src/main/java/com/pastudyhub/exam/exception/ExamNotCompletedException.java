package com.pastudyhub.exam.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

/**
 * Thrown when results are asked for an exam that was never finished
 * (still in progress, or abandoned).
 */
public class ExamNotCompletedException extends StudyHubException {
    public ExamNotCompletedException(UUID sessionId) {
        super("Exam session has no results because it was not completed: " + sessionId, HttpStatus.CONFLICT);
    }
}
