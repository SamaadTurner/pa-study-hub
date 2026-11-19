package com.pastudyhub.user.exception;

import org.springframework.http.HttpStatus;

public class GoogleAccountMismatchException extends StudyHubException {
    public GoogleAccountMismatchException() {
        super("This account is already linked to a different Google account.", HttpStatus.CONFLICT);
    }
}
