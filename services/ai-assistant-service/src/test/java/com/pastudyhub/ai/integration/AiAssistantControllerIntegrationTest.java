package com.pastudyhub.ai.integration;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pastudyhub.ai.dto.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("AiAssistantController Integration Tests")
class AiAssistantControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private AnthropicClient anthropicClient;

    private static final String USER_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";

    // Fake Claude response for flashcard tests
    private static final String FLASHCARD_RESPONSE =
            "FRONT: What is the first-line treatment for hypertension?\n" +
            "BACK: Thiazide diuretics (e.g., hydrochlorothiazide) per JNC guidelines.\n" +
            "HINT: Thiazide\n" +
            "TAGS: Cardiology, Pharmacology\n" +
            "---\n" +
            "FRONT: What defines Stage 2 hypertension?\n" +
            "BACK: SBP >= 140 or DBP >= 90 mmHg on two separate occasions.\n" +
            "HINT: 140/90\n" +
            "TAGS: Cardiology\n" +
            "---";

    // Fake Claude response for plain-text features
    private static final String TEXT_RESPONSE =
            "The student chose the wrong answer because they confused the mechanism of beta-blockers " +
            "with ACE inhibitors. Remember: ACE inhibitors block angiotensin conversion, while " +
            "beta-blockers reduce heart rate and contractility. Mnemonic: 'ACE is the conversion enzyme'.";

    @BeforeEach
    void setupMock() {
        // Build a minimal stubbed Message
        AnthropicClient.Messages messagesMock = org.mockito.Mockito.mock(AnthropicClient.Messages.class);
        when(anthropicClient.messages()).thenReturn(messagesMock);

        Message mockMessage = buildMockMessage(FLASHCARD_RESPONSE);
        Message mockTextMessage = buildMockMessage(TEXT_RESPONSE);

        // Default to text response; individual tests override via argument matching
        when(messagesMock.create(any(MessageCreateParams.class))).thenReturn(mockMessage);
    }

    // ---- POST /ai/flashcards -----------------------------------------------

    @Test
    @Order(1)
    @DisplayName("POST /ai/flashcards: 200 OK returns parsed flashcards")
    void generateFlashcards_returns200() throws Exception {
        GenerateFlashcardsRequest request = new GenerateFlashcardsRequest(
                "Hypertension Management", "CARDIOLOGY", 2);

        mockMvc.perform(post("/api/v1/ai/flashcards")
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topic").value("Hypertension Management"))
                .andExpect(jsonPath("$.category").value("CARDIOLOGY"))
                .andExpect(jsonPath("$.requestedCount").value(2))
                .andExpect(jsonPath("$.flashcards", hasSize(2)))
                .andExpect(jsonPath("$.flashcards[0].front").exists())
                .andExpect(jsonPath("$.flashcards[0].back").exists())
                .andExpect(jsonPath("$.flashcards[0].tags").isArray());
    }

    @Test
    @Order(2)
    @DisplayName("POST /ai/flashcards: 400 when topic is blank")
    void generateFlashcards_blankTopic_returns400() throws Exception {
        GenerateFlashcardsRequest request = new GenerateFlashcardsRequest("", "CARDIOLOGY", 5);

        mockMvc.perform(post("/api/v1/ai/flashcards")
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.topic").exists());
    }

    @Test
    @Order(3)
    @DisplayName("POST /ai/flashcards: 400 when count exceeds max (20)")
    void generateFlashcards_countTooHigh_returns400() throws Exception {
        GenerateFlashcardsRequest request = new GenerateFlashcardsRequest("Diabetes", "ENDOCRINE", 25);

        mockMvc.perform(post("/api/v1/ai/flashcards")
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.count").exists());
    }

    // ---- POST /ai/explain --------------------------------------------------

    @Test
    @Order(4)
    @DisplayName("POST /ai/explain: 200 OK returns explanation")
    void explainWrongAnswer_returns200() throws Exception {
        // Re-stub with text response
        AnthropicClient.Messages messagesMock = org.mockito.Mockito.mock(AnthropicClient.Messages.class);
        when(anthropicClient.messages()).thenReturn(messagesMock);
        when(messagesMock.create(any())).thenReturn(buildMockMessage(TEXT_RESPONSE));

        ExplainAnswerRequest request = new ExplainAnswerRequest(
                "A 55-year-old patient presents with a BP of 165/95. Which drug is first-line?",
                "Metoprolol",
                "Lisinopril",
                "ACE inhibitors are first-line for hypertension with comorbid diabetes or CKD."
        );

        mockMvc.perform(post("/api/v1/ai/explain")
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selectedAnswer").value("Metoprolol"))
                .andExpect(jsonPath("$.correctAnswer").value("Lisinopril"))
                .andExpect(jsonPath("$.explanation").isString());
    }

    @Test
    @Order(5)
    @DisplayName("POST /ai/explain: 400 when questionStem is blank")
    void explainWrongAnswer_blankStem_returns400() throws Exception {
        ExplainAnswerRequest request = new ExplainAnswerRequest("", "A", "B", "explanation");

        mockMvc.perform(post("/api/v1/ai/explain")
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.questionStem").exists());
    }

    // ---- POST /ai/study-plan -----------------------------------------------

    @Test
    @Order(6)
    @DisplayName("POST /ai/study-plan: 200 OK returns study plan text")
    void generateStudyPlan_returns200() throws Exception {
        AnthropicClient.Messages messagesMock = org.mockito.Mockito.mock(AnthropicClient.Messages.class);
        when(anthropicClient.messages()).thenReturn(messagesMock);
        when(messagesMock.create(any())).thenReturn(buildMockMessage("Monday: Cardiology flashcards..."));

        StudyPlanRequest request = new StudyPlanRequest(
                List.of("Cardiology", "Pulmonology"), 30, 60);

        mockMvc.perform(post("/api/v1/ai/study-plan")
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.daysUntilExam").value(30))
                .andExpect(jsonPath("$.dailyMinutes").value(60))
                .andExpect(jsonPath("$.studyPlan").isString());
    }

    @Test
    @Order(7)
    @DisplayName("POST /ai/study-plan: 400 when weakCategories is empty")
    void generateStudyPlan_emptyCategories_returns400() throws Exception {
        StudyPlanRequest request = new StudyPlanRequest(List.of(), 30, 60);

        mockMvc.perform(post("/api/v1/ai/study-plan")
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.weakCategories").exists());
    }

    // ---- POST /ai/chat -----------------------------------------------------

    @Test
    @Order(8)
    @DisplayName("POST /ai/chat: 200 OK starts new session")
    void chat_newSession_returns200() throws Exception {
        AnthropicClient.Messages messagesMock = org.mockito.Mockito.mock(AnthropicClient.Messages.class);
        when(anthropicClient.messages()).thenReturn(messagesMock);
        when(messagesMock.create(any())).thenReturn(
                buildMockMessage("Great question! Atrial fibrillation is characterized by..."));

        ChatRequest request = new ChatRequest("What is atrial fibrillation?", null);

        mockMvc.perform(post("/api/v1/ai/chat")
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").isString())
                .andExpect(jsonPath("$.userMessage").value("What is atrial fibrillation?"))
                .andExpect(jsonPath("$.assistantReply").isString())
                .andExpect(jsonPath("$.turnNumber").value(1));
    }

    @Test
    @Order(9)
    @DisplayName("POST /ai/chat: 400 when message is blank")
    void chat_blankMessage_returns400() throws Exception {
        ChatRequest request = new ChatRequest("", null);

        mockMvc.perform(post("/api/v1/ai/chat")
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.message").exists());
    }

    // ---- Helper ------------------------------------------------------------

    /**
     * Builds a minimal mock Message with a single TextBlock content.
     * Uses Mockito to avoid instantiating sealed Anthropic SDK classes directly.
     */
    private Message buildMockMessage(String text) {
        TextBlock textBlock = org.mockito.Mockito.mock(TextBlock.class);
        when(textBlock.type()).thenReturn(TextBlock.Type.TEXT);
        when(textBlock.text()).thenReturn(text);

        // ContentBlock is the sealed parent; TextBlock is a ContentBlock
        ContentBlock contentBlock = org.mockito.Mockito.mock(ContentBlock.class);
        when(contentBlock.isText()).thenReturn(true);
        when(contentBlock.asText()).thenReturn(textBlock);

        Message message = org.mockito.Mockito.mock(Message.class);
        when(message.content()).thenReturn(List.of(contentBlock));

        return message;
    }
}
