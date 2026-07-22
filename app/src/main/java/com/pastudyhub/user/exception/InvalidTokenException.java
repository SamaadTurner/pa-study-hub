package com.pastudyhub.user.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a JWT or refresh token is expired, malformed, or has an invalid signature. */
public class InvalidTokenException extends StudyHubException {
    public InvalidTokenException(String reason) {
        super("Token is invalid: " + reason, HttpStatus.UNAUTHORIZED);
    }
}
