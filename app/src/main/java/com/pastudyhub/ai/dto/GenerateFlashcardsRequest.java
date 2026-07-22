package com.pastudyhub.ai.dto;

import jakarta.validation.constraints.*;

public record GenerateFlashcardsRequest(

        @NotBlank(message = "topic is required")
        @Size(max = 200, message = "topic must be 200 characters or fewer")
        String topic,

        @NotBlank(message = "category is required")
        @Size(max = 100, message = "category must be 100 characters or fewer")
        String category,

        @Min(value = 1, message = "count must be at least 1")
        @Max(value = 20, message = "count must be 20 or fewer")
        int count
) {}
