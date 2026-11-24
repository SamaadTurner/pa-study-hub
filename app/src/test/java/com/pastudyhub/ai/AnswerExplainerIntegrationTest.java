package com.pastudyhub.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pastudyhub.BaseIntegrationTest;
import com.pastudyhub.exam.model.AnswerOption;
import com.pastudyhub.exam.model.DifficultyLevel;
import com.pastudyhub.exam.model.Question;
import com.pastudyhub.exam.model.QuestionCategory;
import com.pastudyhub.exam.model.TaskArea;
import com.pastudyhub.exam.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Explaining a question of a finished exam. The exams are real, taken through the
 * API; only the model is fake. The questions are neurology ones this class adds
 * itself, so no other test class picks them.
 */
@DisplayName("AI explanation of an exam question")
class AnswerExplainerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private QuestionRepository questionRepository;

    @MockitoBean
    private ChatClient chatClient;

    @BeforeEach
    void setUp() {
        when(chatClient.isEnabled()).thenReturn(true);
        when(chatClient.model()).thenReturn("fake-model");
        when(chatClient.complete(anyString(), anyString()))
                .thenReturn("{\"explanation\": \"Option B looks right because of the headache, but...\"}");

        if (!questionRepository.findByCategoryAndIsActiveTrue(QuestionCategory.NEUROLOGY).isEmpty()) {
            return;
        }
        for (int i = 1; i <= 2; i++) {
            Question question = Question.builder()
                    .stem("Neurology test question " + i)
                    .clinicalVignette("A 30-year-old with a sudden severe headache.")
                    .category(QuestionCategory.NEUROLOGY)
                    .taskArea(TaskArea.MOST_LIKELY_DIAGNOSIS)
                    .difficulty(DifficultyLevel.MEDIUM)
                    .explanation("Teaching note " + i)
                    .isActive(true)
                    .build();
            for (int option = 0; option < 4; option++) {
                question.getAnswerOptions().add(AnswerOption.builder()
                        .question(question)
                        .text("Question " + i + " option " + option)
                        .isCorrect(option == 0)
                        .orderIndex(option)
                        .build());
            }
            questionRepository.save(question);
        }
    }

    // ---- helpers: take an exam through the API ------------------------------

    private JsonNode startExam(UUID user) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/exams")
                        .header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionCount\": 2, \"categoryFilter\": \"NEUROLOGY\", \"timeLimitMinutes\": 0}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private void answer(UUID user, String sessionId, String questionId, String optionId) throws Exception {
        mockMvc.perform(post("/api/v1/exams/{sessionId}/questions/{questionId}/answer", sessionId, questionId)
                        .header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selectedOptionId\": \"" + optionId + "\", \"timeSpentSeconds\": 10}"))
                .andExpect(status().isOk());
    }

    private void finish(UUID user, String sessionId) throws Exception {
        mockMvc.perform(post("/api/v1/exams/{sessionId}/complete", sessionId)
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk());
    }

    private ResultActions explain(UUID user, String sessionId, String questionId) throws Exception {
        return mockMvc.perform(post("/api/v1/ai/exams/{sessionId}/questions/{questionId}/explanation",
                sessionId, questionId)
                .header("Authorization", bearerToken(user)));
    }

    private String sentPrompt() {
        ArgumentCaptor<String> userPrompt = ArgumentCaptor.forClass(String.class);
        verify(chatClient).complete(eq(AnswerExplainer.SYSTEM_PROMPT), userPrompt.capture());
        return userPrompt.getValue();
    }

    // The letter of the option the API listed at this position, as the page shows it
    private static String letter(int position) {
        return String.valueOf((char) ('A' + position));
    }

    // ---- tests ----------------------------------------------------------------

    @Test
    @DisplayName("Explains a wrong answer, naming the options by the letters the user saw")
    void explainsAWrongAnswer() throws Exception {
        UUID user = UUID.randomUUID();
        JsonNode exam = startExam(user);
        String sessionId = exam.get("id").asText();
        JsonNode question = exam.get("questions").get(0);
        String questionId = question.get("id").asText();

        // Find the right option and a wrong one in the order the exam shows them
        int rightPosition = -1;
        int wrongPosition = -1;
        JsonNode options = question.get("answerOptions");
        for (int i = 0; i < options.size(); i++) {
            boolean right = options.get(i).get("text").asText().endsWith("option 0");
            if (right) {
                rightPosition = i;
            } else if (wrongPosition < 0) {
                wrongPosition = i;
            }
        }
        answer(user, sessionId, questionId, options.get(wrongPosition).get("id").asText());
        finish(user, sessionId);

        explain(user, sessionId, questionId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.explanation").value("Option B looks right because of the headache, but..."));

        String prompt = sentPrompt();
        assertThat(prompt)
                .contains("Case: A 30-year-old with a sudden severe headache.")
                .contains("Question: " + question.get("stem").asText())
                .contains(letter(rightPosition) + ". " + options.get(rightPosition).get("text").asText())
                .contains("Right answer: " + letter(rightPosition))
                .contains("The student picked: " + letter(wrongPosition))
                .contains("Teaching note from the question bank: Teaching note");
    }

    @Test
    @DisplayName("For a question left blank the prompt says it was not answered")
    void explainsAnUnansweredQuestion() throws Exception {
        UUID user = UUID.randomUUID();
        JsonNode exam = startExam(user);
        String sessionId = exam.get("id").asText();
        finish(user, sessionId);

        explain(user, sessionId, exam.get("questions").get(1).get("id").asText())
                .andExpect(status().isOk());

        assertThat(sentPrompt()).contains("The student did not answer this question.");
    }

    @Test
    @DisplayName("An exam that is still running can't be explained, because that would give the answer away")
    void runningExam() throws Exception {
        UUID user = UUID.randomUUID();
        JsonNode exam = startExam(user);

        explain(user, exam.get("id").asText(), exam.get("questions").get(0).get("id").asText())
                .andExpect(status().isConflict());

        verify(chatClient, never()).complete(anyString(), anyString());
    }

    @Test
    @DisplayName("Someone else's exam answers 404")
    void otherUsersExam() throws Exception {
        UUID owner = UUID.randomUUID();
        JsonNode exam = startExam(owner);
        String sessionId = exam.get("id").asText();
        finish(owner, sessionId);

        explain(UUID.randomUUID(), sessionId, exam.get("questions").get(0).get("id").asText())
                .andExpect(status().isNotFound());

        verify(chatClient, never()).complete(anyString(), anyString());
    }

    @Test
    @DisplayName("A question from outside the exam answers 400, and does not use up the limit")
    void questionNotInExam() throws Exception {
        UUID user = UUID.randomUUID();
        JsonNode exam = startExam(user);
        String sessionId = exam.get("id").asText();
        finish(user, sessionId);

        explain(user, sessionId, UUID.randomUUID().toString())
                .andExpect(status().isBadRequest());

        verify(chatClient, never()).complete(anyString(), anyString());
        mockMvc.perform(get("/api/v1/ai/status").header("Authorization", bearerToken(user)))
                .andExpect(jsonPath("$.remainingToday").value(20));
    }

    @Test
    @DisplayName("An answer without an explanation is a 502")
    void emptyExplanation() throws Exception {
        when(chatClient.complete(anyString(), anyString())).thenReturn("{\"explanation\": \"  \"}");
        UUID user = UUID.randomUUID();
        JsonNode exam = startExam(user);
        String sessionId = exam.get("id").asText();
        finish(user, sessionId);

        explain(user, sessionId, exam.get("questions").get(0).get("id").asText())
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value("The AI answer did not contain an explanation."));
    }

    @Test
    @DisplayName("A very long explanation is cut, not refused")
    void longExplanationIsCut() throws Exception {
        String longText = "x".repeat(AnswerExplainer.MAX_EXPLANATION_LENGTH + 500);
        when(chatClient.complete(anyString(), anyString())).thenReturn("{\"explanation\": \"" + longText + "\"}");
        UUID user = UUID.randomUUID();
        JsonNode exam = startExam(user);
        String sessionId = exam.get("id").asText();
        finish(user, sessionId);

        MvcResult result = explain(user, sessionId, exam.get("questions").get(0).get("id").asText())
                .andExpect(status().isOk())
                .andReturn();

        String explanation = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("explanation").asText();
        assertThat(explanation).hasSize(AnswerExplainer.MAX_EXPLANATION_LENGTH + 3).endsWith("...");
    }
}
