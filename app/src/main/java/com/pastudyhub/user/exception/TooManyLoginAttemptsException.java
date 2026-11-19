package com.pastudyhub.user.exception;

import org.springframework.http.HttpStatus;

/** Thrown when an email has had too many failed logins in a short time. */
public class TooManyLoginAttemptsException extends StudyHubException {

    private final long retryAfterSeconds;

    public TooManyLoginAttemptsException(long retryAfterSeconds) {
        super(String.format("Too many failed logins. Try again in %d minute%s.",
                minutes(retryAfterSeconds), minutes(retryAfterSeconds) == 1 ? "" : "s"),
                HttpStatus.TOO_MANY_REQUESTS);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    // Rounded up, so "1 minute" is never shown when 70 seconds are left
    private static long minutes(long seconds) {
        return (seconds + 59) / 60;
    }
}
