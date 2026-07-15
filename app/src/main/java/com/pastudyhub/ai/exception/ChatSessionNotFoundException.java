package com.pastudyhub.ai.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ChatSessionNotFoundException extends RuntimeException {
    public ChatSessionNotFoundException(UUID sessionId) {
        super("Chat session not found: " + sessionId);
    }
}
