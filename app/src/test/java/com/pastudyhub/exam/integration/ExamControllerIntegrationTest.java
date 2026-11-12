package com.pastudyhub.exam.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pastudyhub.BaseIntegrationTest;
import com.pastudyhub.exam.dto.StartExamRequest;
import com.pastudyhub.exam.dto.SubmitAnswerRequest;
import com.pastudyhub.exam.model.*;
import com.pastudyhub.exam.repository.AnswerOptionRepository;
import com.pastudyhub.exam.repository.QuestionRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("ExamController Integration Tests")
class ExamControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private AnswerOptionRepository answerOptionRepository;

    private static final UUID USER = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static String sessionId;
    private static String questionId;
    private static String correctOptionId;

    // H2 starts with no questions, so the test adds one. On PostgreSQL the migrations
    // already put practice questions in the table and nothing is added here.
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
            questionRepository.save(q);
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
                        .header("Authorization", bearerToken(USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.questions").isArray())
                .andExpect(jsonPath("$.questions[0].explanation").doesNotExist())
                .andReturn();

        var session = objectMapper.readTree(result.getResponse().getContentAsString());
        sessionId = session.get("id").asText();
        assertThat(sessionId).isNotBlank();

        // Remember which question the exam picked and what its right answer is.
        // The exam picks at random, so the tests can't assume it is the one seeded above.
        questionId = session.get("questions").get(0).get("id").asText();
        correctOptionId = answerOptionRepository.findByQuestionId(UUID.fromString(questionId)).stream()
                .filter(AnswerOption::isCorrect)
                .findFirst()
                .orElseThrow()
                .getId()
                .toString();
    }

    @Test
    @Order(2)
    @DisplayName("POST /exams: 422 Unprocessable when insufficient questions")
    void startExam_insufficientQuestions_returns422() throws Exception {
        StartExamRequest request = StartExamRequest.builder()
                .questionCount(99)
                .build();

        mockMvc.perform(post("/api/v1/exams")
                        .header("Authorization", bearerToken(USER))
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
                        .header("Authorization", bearerToken(USER))
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
                        .header("Authorization", bearerToken(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionId))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @Order(5)
    @DisplayName("GET /exams/{sessionId}: 404 for non-existent session")
    void getExamSession_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/exams/{sessionId}", UUID.randomUUID())
                        .header("Authorization", bearerToken(USER)))
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
                        .header("Authorization", bearerToken(USER))
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
                        .header("Authorization", bearerToken(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(sessionId))
                .andExpect(jsonPath("$.rawScore").value(1))
                .andExpect(jsonPath("$.scorePercent").value(100.0))
                .andExpect(jsonPath("$.performanceBand").isString())
                .andExpect(jsonPath("$.answerDetails").isArray())
                .andExpect(jsonPath("$.answerDetails[0].explanation").isString());
    }

    @Test
    @Order(8)
    @DisplayName("POST /complete: 409 Conflict when exam already completed")
    void completeExam_alreadyCompleted_returns409() throws Exception {
        mockMvc.perform(post("/api/v1/exams/{sessionId}/complete", sessionId)
                        .header("Authorization", bearerToken(USER)))
                .andExpect(status().isConflict());
    }

    // ---- GET /api/v1/exams/history -----------------------------------------

    @Test
    @Order(9)
    @DisplayName("GET /exams/history: 200 OK with paginated exam history")
    void getExamHistory_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/exams/history")
                        .header("Authorization", bearerToken(USER))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
    }
}
