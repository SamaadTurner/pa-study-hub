package com.pastudyhub.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pastudyhub.ai.usage.AiUsageLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Explains one question of a finished exam in plain words: why the right answer
 * is right, and why the one the student picked is not.
 *
 * <p>Every question already has a short teaching note. That note is the same for
 * everyone, so it can't say why "B" in particular was tempting. The model gets the
 * note and the student's pick, and is told to stay with the facts in the note.
 */
@Service
@RequiredArgsConstructor
public class AnswerExplainer {

    // Long enough for a few short paragraphs. A longer answer is cut, not refused.
    static final int MAX_EXPLANATION_LENGTH = 3000;

    static final String SYSTEM_PROMPT = """
            You are a tutor for physician assistant students reviewing a practice exam question.
            Explain in plain words why the right answer is right. If the student picked a different
            answer, explain why that answer is wrong and what might have made it look right.
            Base the medical facts on the teaching note. Do not add facts that are not in it.
            Keep it under 150 words. Reply with JSON only, in exactly this shape:
            {"explanation": "your explanation"}
            """;

    private final ChatClient chatClient;
    private final ExplanationPromptBuilder promptBuilder;
    private final AiUsageLimiter usageLimiter;
    private final ObjectMapper objectMapper;

    public String explain(UUID userId, UUID sessionId, UUID questionId) {
        if (!chatClient.isEnabled()) {
            throw new AiUnavailableException("AI features are turned off on this server.");
        }

        // Checks the exam first, so a bad request never uses up a request from the limit
        String userPrompt = promptBuilder.build(userId, sessionId, questionId);
        usageLimiter.useOne(userId);

        String reply = chatClient.complete(SYSTEM_PROMPT, userPrompt);
        String explanation = AiJson.readObject(objectMapper, reply).path("explanation").asText("").trim();

        if (explanation.isEmpty()) {
            throw new AiResponseException("The AI answer did not contain an explanation.");
        }
        if (explanation.length() > MAX_EXPLANATION_LENGTH) {
            explanation = explanation.substring(0, MAX_EXPLANATION_LENGTH) + "...";
        }
        return explanation;
    }
}
