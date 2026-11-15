package com.pastudyhub.ai.usage;

import com.pastudyhub.user.exception.StudyHubException;
import org.springframework.http.HttpStatus;

/** Today's AI allowance is used up, for this user or for the whole site. Returns 429. */
public class AiLimitReachedException extends StudyHubException {

    public AiLimitReachedException(String message) {
        super(message, HttpStatus.TOO_MANY_REQUESTS);
    }
}
