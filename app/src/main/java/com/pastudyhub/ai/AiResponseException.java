package com.pastudyhub.ai;

import com.pastudyhub.user.exception.StudyHubException;
import org.springframework.http.HttpStatus;

/** The provider answered, but with an error or with something we can't use. Returns 502. */
public class AiResponseException extends StudyHubException {

    public AiResponseException(String message) {
        super(message, HttpStatus.BAD_GATEWAY);
    }

    public AiResponseException(String message, Throwable cause) {
        super(message, HttpStatus.BAD_GATEWAY, cause);
    }
}
