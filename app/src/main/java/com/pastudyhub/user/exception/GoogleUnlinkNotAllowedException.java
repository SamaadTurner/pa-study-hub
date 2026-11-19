package com.pastudyhub.user.exception;

import org.springframework.http.HttpStatus;

public class GoogleUnlinkNotAllowedException extends StudyHubException {
    public GoogleUnlinkNotAllowedException() {
        super("Set a password before removing Google, or you would have no way to sign in.", HttpStatus.CONFLICT);
    }
}
