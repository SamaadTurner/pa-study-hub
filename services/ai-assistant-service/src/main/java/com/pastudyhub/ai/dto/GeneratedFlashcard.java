package com.pastudyhub.ai.dto;

import java.util.List;

/**
 * A single flashcard pair produced by the AI.
 */
public record GeneratedFlashcard(
        String front,
        String back,
        String hint,
        List<String> tags
) {}
