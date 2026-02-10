package com.pastudyhub.user.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when login fails due to wrong email or password.
 *
 * <p>The message intentionally doesn't specify whether the email or password was wrong
 * to prevent user enumeration attacks (attacker can't tell if the email exists).
 */
public class InvalidCredentialsException extends StudyHubException {
    public InvalidCredentialsException() {
        super("Invalid email or password", HttpStatus.UNAUTHORIZED);
    }
}
