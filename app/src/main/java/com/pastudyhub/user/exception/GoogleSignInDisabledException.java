package com.pastudyhub.user.exception;

import org.springframework.http.HttpStatus;

public class GoogleSignInDisabledException extends StudyHubException {
    public GoogleSignInDisabledException() {
        super("Google sign-in is not set up on this server.", HttpStatus.NOT_FOUND);
    }
}
