package com.pastudyhub.user.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

/** Thrown when a user lookup fails. */
public class UserNotFoundException extends StudyHubException {
    public UserNotFoundException(UUID id) {
        super("No user found with id: " + id, HttpStatus.NOT_FOUND);
    }
}
