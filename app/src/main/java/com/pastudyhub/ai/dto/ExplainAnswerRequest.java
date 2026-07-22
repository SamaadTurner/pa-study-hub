package com.pastudyhub.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExplainAnswerRequest(

        @NotBlank(message = "questionStem is required")
        @Size(max = 2000, message = "questionStem must be 2000 characters or fewer")
        String questionStem,

        @NotBlank(message = "selectedAnswer is required")
        @Size(max = 500, message = "selectedAnswer must be 500 characters or fewer")
        String selectedAnswer,

        @NotBlank(message = "correctAnswer is required")
        @Size(max = 500, message = "correctAnswer must be 500 characters or fewer")
        String correctAnswer,

        @Size(max = 2000, message = "explanation must be 2000 characters or fewer")
        String explanation
) {}
