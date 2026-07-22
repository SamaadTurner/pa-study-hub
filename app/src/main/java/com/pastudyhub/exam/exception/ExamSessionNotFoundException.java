package com.pastudyhub.exam.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class ExamSessionNotFoundException extends StudyHubException {
    public ExamSessionNotFoundException(UUID id) {
        super("Exam session not found: " + id, HttpStatus.NOT_FOUND);
    }
}
