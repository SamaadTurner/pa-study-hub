package com.pastudyhub.user.exception;

import org.springframework.http.HttpStatus;

/** The sign-up form's check that a person is signing up was missing or did not pass. */
public class BotCheckFailedException extends StudyHubException {
    public BotCheckFailedException() {
        this("Please complete the check that you're not a robot, then try again.");
    }

    public BotCheckFailedException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
