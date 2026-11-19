package com.pastudyhub.user.exception;

import org.springframework.http.HttpStatus;

public class AccountDeletionNotConfirmedException extends StudyHubException {
    public AccountDeletionNotConfirmedException() {
        super("To close your account, enter your password (or your email if you only use Google).", HttpStatus.BAD_REQUEST);
    }
}
