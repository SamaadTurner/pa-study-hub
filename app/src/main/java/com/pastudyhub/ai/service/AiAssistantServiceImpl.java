package com.pastudyhub.ai.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.*;
import com.pastudyhub.ai.dto.*;
import com.pastudyhub.ai.exception.AiServiceException;
import com.pastudyhub.ai.exception.ChatSessionNotFoundException;
import com.pastudyhub.ai.exception.RateLimitExceededException;
import com.pastudyhub.ai.model.ChatSession;
import com.pastudyhub.ai.model.ChatTurn;
import com.pastudyhub.ai.parser.ResponseParser;
import com.pastudyhub.ai.prompt.PromptBuilder;
import com.pastudyhub.ai.ratelimit.AiRateLimiter;
import com.pastudyhub.ai.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiAssistantServiceImpl implements AiAssistantService {

    private final AnthropicClient anthropicClient;
    private final PromptBuilder promptBuilder;
    private final ResponseParser responseParser;
    private final AiRateLimiter rateLimiter;
    private final ChatSessionRepository chatSessionRepository;

    @Value("${anthropic.model:claude-sonnet-4-5-20250929}")
    private String model;

    @Value("${anthropic.max-tokens:2048}")
    private int maxTokens;

    // -------------------------------------------------------------------------
    // Feature 1: Flashcard generation
    // -------------------------------------------------------------------------

    @Override
    public GenerateFlashcardsResponse generateFlashcards(UUID userId, GenerateFlashcardsRequest request) {
        checkRateLimit(userId);

        String prompt = promptBuilder.buildFlashcardGeneratorPrompt(
                request.topic(), request.category(), request.count());

        String rawResponse = callClaude(prompt, maxTokens);

        List<GeneratedFlashcard> flashcards = responseParser.parseFlashcards(rawResponse);

        return new GenerateFlashcardsResponse(
                request.topic(),
                request.category(),
                request.count(),
                flashcards.size(),
                flashcards
        );
    }

    // -------------------------------------------------------------------------
    // Feature 2: Wrong answer explanation
    // -------------------------------------------------------------------------

    @Override
    public ExplainAnswerResponse explainWrongAnswer(UUID userId, ExplainAnswerRequest request) {
        checkRateLimit(userId);

        String prompt = promptBuilder.buildWrongAnswerExplanationPrompt(
                request.questionStem(),
                request.selectedAnswer(),
                request.correctAnswer(),
                request.explanation() != null ? request.explanation() : ""
        );

        String rawResponse = callClaude(prompt, 800);

        return new ExplainAnswerResponse(
                request.questionStem(),
                request.selectedAnswer(),
                request.correctAnswer(),
                rawResponse
        );
    }

    // -------------------------------------------------------------------------
    // Feature 3: Study plan generation
    // -------------------------------------------------------------------------

    @Override
    public StudyPlanResponse generateStudyPlan(UUID userId, StudyPlanRequest request) {
        checkRateLimit(userId);

        String prompt = promptBuilder.buildStudyPlanPrompt(
                request.weakCategories(),
                request.daysUntilExam(),
                request.dailyMinutes()
        );

        String rawResponse = callClaude(prompt, 2048);

        return new StudyPlanResponse(
                request.weakCategories(),
                request.daysUntilExam(),
                request.dailyMinutes(),
                rawResponse
        );
    }

    // -------------------------------------------------------------------------
    // Feature 4: Chat tutor
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public ChatResponse chat(UUID userId, ChatRequest request) {
        checkRateLimit(userId);

        ChatSession session = resolveSession(userId, request.sessionId());

        List<String> history = buildHistory(session);
        String prompt = promptBuilder.buildChatTutorPrompt(request.message(), history);
        String reply = callClaude(prompt, 1200);

        int turnNumber = session.getTurns().size() + 1;
        ChatTurn turn = ChatTurn.builder()
                .session(session)
                .turnNumber(turnNumber)
                .userMessage(request.message())
                .assistantReply(reply)
                .build();
        session.getTurns().add(turn);
        chatSessionRepository.save(session);

        return new ChatResponse(session.getId(), request.message(), reply, turnNumber);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private String callClaude(String prompt, int maxTok) {
        try {
            MessageCreateParams params = MessageCreateParams.builder()
                    .model(model)
                    .maxTokens(maxTok)
                    .addUserMessage(prompt)
                    .build();

            Message message = anthropicClient.messages().create(params);

            return message.content().stream()
                    .filter(block -> block instanceof ContentBlock)
                    .map(block -> ((ContentBlock) block).asText().text())
                    .reduce("", String::concat)
                    .strip();

        } catch (Exception ex) {
            log.error("Claude API call failed", ex);
            throw new AiServiceException("AI service is temporarily unavailable. Please try again.", ex);
        }
    }

    private void checkRateLimit(UUID userId) {
        if (!rateLimiter.tryConsume(userId)) {
            throw new RateLimitExceededException(
                    "AI rate limit exceeded. You have " +
                    rateLimiter.remainingMinute(userId) +
                    " requests remaining this minute.");
        }
    }

    private ChatSession resolveSession(UUID userId, UUID sessionId) {
        if (sessionId != null) {
            return chatSessionRepository.findByIdAndUserId(sessionId, userId)
                    .orElseThrow(() -> new ChatSessionNotFoundException(sessionId));
        }
        ChatSession newSession = ChatSession.builder()
                .userId(userId)
                .turns(new ArrayList<>())
                .build();
        return chatSessionRepository.save(newSession);
    }

    private List<String> buildHistory(ChatSession session) {
        return session.getTurns().stream()
                .flatMap(turn -> java.util.stream.Stream.of(
                        "Student: " + turn.getUserMessage(),
                        "Tutor: " + turn.getAssistantReply()
                ))
                .toList();
    }
}
