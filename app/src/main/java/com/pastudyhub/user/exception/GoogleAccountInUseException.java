package com.pastudyhub.user.exception;

import org.springframework.http.HttpStatus;

public class GoogleAccountInUseException extends StudyHubException {
    public GoogleAccountInUseException() {
        super("This Google account is already used by another PA Study Hub account.", HttpStatus.CONFLICT);
    }
}
