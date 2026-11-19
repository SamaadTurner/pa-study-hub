package com.pastudyhub.user.exception;

import org.springframework.http.HttpStatus;

public class WrongCurrentPasswordException extends StudyHubException {
    public WrongCurrentPasswordException() {
        super("Your current password is not right.", HttpStatus.BAD_REQUEST);
    }
}
