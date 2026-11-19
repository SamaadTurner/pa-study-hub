package com.pastudyhub.user.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when someone signs in with Google and an account with the same email
 * already exists but is not linked yet. Linking needs that account's password once.
 */
public class GoogleLinkRequiredException extends StudyHubException {
    public GoogleLinkRequiredException() {
        super("An account with this email already exists. Enter its password once to add Google sign-in.",
                HttpStatus.CONFLICT);
    }
}
