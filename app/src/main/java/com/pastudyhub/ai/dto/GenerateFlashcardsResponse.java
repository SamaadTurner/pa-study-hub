package com.pastudyhub.ai.dto;

import java.util.List;

public record GenerateFlashcardsResponse(
        String topic,
        String category,
        int requestedCount,
        int generatedCount,
        List<GeneratedFlashcard> flashcards
) {}
