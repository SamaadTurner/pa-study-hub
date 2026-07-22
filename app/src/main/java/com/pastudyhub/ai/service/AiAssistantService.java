package com.pastudyhub.ai.service;

import com.pastudyhub.ai.dto.*;

import java.util.UUID;

public interface AiAssistantService {

    GenerateFlashcardsResponse generateFlashcards(UUID userId, GenerateFlashcardsRequest request);

    ExplainAnswerResponse explainWrongAnswer(UUID userId, ExplainAnswerRequest request);

    StudyPlanResponse generateStudyPlan(UUID userId, StudyPlanRequest request);

    ChatResponse chat(UUID userId, ChatRequest request);
}
