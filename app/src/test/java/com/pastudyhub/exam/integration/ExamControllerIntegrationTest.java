package com.pastudyhub.exam.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pastudyhub.exam.dto.StartExamRequest;
import com.pastudyhub.exam.dto.SubmitAnswerRequest;
import com.pastudyhub.exam.model.*;
import com.pastudyhub.exam.repository.AnswerOptionRepository;
import com.pastudyhub.exam.repository.QuestionRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("ExamController Integration Tests")
class ExamControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private AnswerOptionRepository answerOptionRepository;

    private static final UUID USER = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static String sessionId;
    private static String questionId;
    private static String correctOptionId;

    @BeforeEach
    void seedQuestions() {
        if (questionRepository.count() == 0) {
            Question q = Question.builder()
                    .stem("What is the most common cause of CAP?")
                    .category(QuestionCategory.PULMONOLOGY)
                    .difficulty(DifficultyLevel.EASY)
                    .explanation("Streptococcus pneumoniae is the most common cause of CAP.")
                    .isActive(true)
                    .build();

            AnswerOption correct = AnswerOption.builder()
                    .question(q).text("Streptococcus pneumoniae").isCorrect(true).orderIndex(0).build();
            AnswerOption wrong1 = AnswerOption.builder()
                    .question(q).text("Mycoplasma pneumoniae").isCorrect(false).orderIndex(1).build();
            AnswerOption wrong2 = AnswerOption.builder()
                    .question(q).text("Haemophilus influenzae").isCorrect(false).orderIndex(2).build();
            AnswerOption wrong3 = AnswerOption.builder()
                    .question(q).text("Klebsiella pneumoniae").isCorrect(false).orderIndex(3).build();

            q.getAnswerOptions().addAll(java.util.List.of(correct, wrong1, wrong2, wrong3));
            Question saved = questionRepository.save(q);
            questionId = saved.getId().toString();
            correctOptionId = saved.getAnswerOptions().stream()
                    .filter(AnswerOption::isCorrect).findFirst().get().getId().toString();
        }
    }

    // ---- POST /api/v1/exams -----------------------------------------------

    @Test
    @Order(1)
    @DisplayName("POST /exams: 201 Created with valid request")
    void startExam_returns201() throws Exception {
        StartExamRequest request = StartExamRequest.builder()
                .questionCount(1)
                .timeLimitMinutes(0)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/exams")
                        .header("X-User-Id", USER.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.questions").isArray())
                .andExpect(jsonPath("$.questions[0].explanation").doesNotExist())
                .andReturn();

        sessionId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
        assertThat(sessionId).isNotBlank();
    }

    @Test
    @Order(2)
    @DisplayName("POST /exams: 422 Unprocessable when insufficient questions")
    void startExam_insufficientQuestions_returns422() throws Exception {
        StartExamRequest request = StartExamRequest.builder()
                .questionCount(99)
                .build();

        mockMvc.perform(post("/api/v1/exams")
                        .header("X-User-Id", USER.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @Order(3)
    @DisplayName("POST /exams: 400 Bad Request when questionCount < 1")
    void startExam_invalidCount_returns400() throws Exception {
        StartExamRequest request = StartExamRequest.builder().questionCount(0).build();

        mockMvc.perform(post("/api/v1/exams")
                        .header("X-User-Id", USER.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ---- GET /api/v1/exams/{sessionId} -------------------------------------

    @Test
    @Order(4)
    @DisplayName("GET /exams/{sessionId}: 200 OK for in-progress session")
    void getExamSession_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/exams/{sessionId}", sessionId)
                        .header("X-User-Id", USER.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionId))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @Order(5)
    @DisplayName("GET /exams/{sessionId}: 404 for non-existent session")
    void getExamSession_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/exams/{sessionId}", UUID.randomUUID())
                        .header("X-User-Id", USER.toString()))
                .andExpect(status().isNotFound());
    }

    // ---- POST /api/v1/exams/{sessionId}/questions/{questionId}/answer ------

    @Test
    @Order(6)
    @DisplayName("POST /answer: 200 OK on valid answer submission")
    void submitAnswer_returns200() throws Exception {
        SubmitAnswerRequest request = SubmitAnswerRequest.builder()
                .selectedOptionId(UUID.fromString(correctOptionId))
                .timeSpentSeconds(25)
                .build();

        mockMvc.perform(post("/api/v1/exams/{sessionId}/questions/{questionId}/answer",
                        sessionId, questionId)
                        .header("X-User-Id", USER.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answeredCount").value(1));
    }

    // ---- POST /api/v1/exams/{sessionId}/complete ---------------------------

    @Test
    @Order(7)
    @DisplayName("POST /complete: 200 OK with score results and answer details")
    void completeExam_returns200WithResults() throws Exception {
        mockMvc.perform(post("/api/v1/exams/{sessionId}/complete", sessionId)
                        .header("X-User-Id", USER.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(sessionId))
                .andExpect(jsonPath("$.rawScore").isNumber())
                .andExpect(jsonPath("$.scorePercent").isNumber())
                .andExpect(jsonPath("$.performanceBand").isString())
                .andExpect(jsonPath("$.answerDetails").isArray())
                .andExpect(jsonPath("$.answerDetails[0].explanation").isString());
    }

    @Test
    @Order(8)
    @DisplayName("POST /complete: 409 Conflict when exam already completed")
    void completeExam_alreadyCompleted_returns409() throws Exception {
        mockMvc.perform(post("/api/v1/exams/{sessionId}/complete", sessionId)
                        .header("X-User-Id", USER.toString()))
                .andExpect(status().isConflict());
    }

    // ---- GET /api/v1/exams/history -----------------------------------------

    @Test
    @Order(9)
    @DisplayName("GET /exams/history: 200 OK with paginated exam history")
    void getExamHistory_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/exams/history")
                        .header("X-User-Id", USER.toString())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
    }
}
