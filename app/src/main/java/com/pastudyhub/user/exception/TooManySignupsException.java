package com.pastudyhub.user.exception;

import org.springframework.http.HttpStatus;

/** The site has made as many new accounts as it allows for now. */
public class TooManySignupsException extends StudyHubException {
    public TooManySignupsException() {
        super("A lot of accounts were made recently, so sign-up is paused for a little while. Please try again later.",
                HttpStatus.TOO_MANY_REQUESTS);
    }
}
