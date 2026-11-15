package com.pastudyhub.ai;

import com.pastudyhub.user.exception.StudyHubException;
import org.springframework.http.HttpStatus;

/** The AI features are off, or the provider can't be reached or is over its limit. Returns 503. */
public class AiUnavailableException extends StudyHubException {

    public AiUnavailableException(String message) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE);
    }

    public AiUnavailableException(String message, Throwable cause) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE, cause);
    }
}
