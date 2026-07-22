package com.pastudyhub.ai.dto;

public record ExplainAnswerResponse(
        String questionStem,
        String selectedAnswer,
        String correctAnswer,
        String explanation
) {}
