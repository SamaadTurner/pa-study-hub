package com.pastudyhub.user.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a deactivated user attempts to authenticate. */
public class AccountDeactivatedException extends StudyHubException {
    public AccountDeactivatedException() {
        super("This account has been deactivated. Contact support to reactivate.", HttpStatus.FORBIDDEN);
    }
}
