package com.pastudyhub.ai.dto;

import java.util.UUID;

public record ChatResponse(
        UUID sessionId,
        String userMessage,
        String assistantReply,
        int turnNumber
) {}
