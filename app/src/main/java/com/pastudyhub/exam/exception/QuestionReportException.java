package com.pastudyhub.exam.exception;

import org.springframework.http.HttpStatus;

/** A report that can't be accepted, with the reason in the message. */
public class QuestionReportException extends StudyHubException {
    public QuestionReportException(String message, HttpStatus status) {
        super(message, status);
    }
}
