package com.pastudyhub.exam.exception;

import org.springframework.http.HttpStatus;

/** Thrown when an answer arrives after the exam's time limit (and grace period) ran out. */
public class ExamTimeUpException extends StudyHubException {
    public ExamTimeUpException() {
        super("Time is up. The exam was handed in with the answers saved before the time ran out.",
                HttpStatus.CONFLICT);
    }
}
