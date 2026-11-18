package com.pastudyhub.exam.integration;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Right answers per PANCE task area, on the results of one exam and over all exams.
 *
 * <p>Uses endocrinology questions that this class adds itself: two diagnostic study
 * questions and two clinical intervention questions. No other test or migration
 * adds endocrinology questions, so the counts are known on H2 and PostgreSQL.
 */
@DisplayName("Scores per task area")
class TaskAreaScoresIntegrationTest extends BaseIntegrationTest {

    private static final String RIGHT = "Right answer";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private QuestionRepository questionRepository;

    @BeforeEach
    void addEndocrinologyQuestions() {
        if (!questionRepository.findByCategoryAndIsActiveTrue(QuestionCategory.ENDOCRINOLOGY).isEmpty()) {
            return;
        }
        addQuestion("Which test confirms the diagnosis? (1)", TaskArea.DIAGNOSTIC_STUDIES);
        addQuestion("Which test confirms the diagnosis? (2)", TaskArea.DIAGNOSTIC_STUDIES);
        addQuestion("What is the next step in management? (1)", TaskArea.CLINICAL_INTERVENTION);
        addQuestion("What is the next step in management? (2)", TaskArea.CLINICAL_INTERVENTION);
    }

    @Test
    @DisplayName("A user with no finished exams gets an empty list")
    void noExams_emptyList() throws Exception {
        mockMvc.perform(get("/api/v1/exams/task-areas")
                        .header("Authorization", bearerToken(UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("The results list each task area with right answers out of questions, unanswered count as wrong")
    void results_haveTaskAreaBreakdown() throws Exception {
        UUID user = UUID.randomUUID();
        String sessionId = takeExam(user, true, false);

        // Diagnostic studies come first, the order of the blueprint
        mockMvc.perform(get("/api/v1/exams/{sessionId}/result", sessionId)
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskAreaBreakdown", hasSize(2)))
                .andExpect(jsonPath("$.taskAreaBreakdown[0].taskArea").value("DIAGNOSTIC_STUDIES"))
                .andExpect(jsonPath("$.taskAreaBreakdown[0].correct").value(2))
                .andExpect(jsonPath("$.taskAreaBreakdown[0].total").value(2))
                .andExpect(jsonPath("$.taskAreaBreakdown[0].percent").value(100.0))
                .andExpect(jsonPath("$.taskAreaBreakdown[1].taskArea").value("CLINICAL_INTERVENTION"))
                .andExpect(jsonPath("$.taskAreaBreakdown[1].correct").value(0))
                .andExpect(jsonPath("$.taskAreaBreakdown[1].total").value(2));
    }

    @Test
    @DisplayName("The totals add up every finished exam, and leave out running and abandoned ones")
    void totals_addUpFinishedExamsOnly() throws Exception {
        UUID user = UUID.randomUUID();
        takeExam(user, true, false);   // diagnostic 2/2, intervention 0/2
        takeExam(user, false, true);   // diagnostic 0/2, intervention 2/2

        // An exam left running and an abandoned one, both with right answers given
        String running = startExam(user);
        answerAll(user, running, true, true);
        String abandoned = startExam(user);
        answerAll(user, abandoned, true, true);
        mockMvc.perform(post("/api/v1/exams/{sessionId}/abandon", abandoned)
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().is2xxSuccessful());

        mockMvc.perform(get("/api/v1/exams/task-areas")
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].taskArea").value("DIAGNOSTIC_STUDIES"))
                .andExpect(jsonPath("$[0].correct").value(2))
                .andExpect(jsonPath("$[0].total").value(4))
                .andExpect(jsonPath("$[0].percent").value(50.0))
                .andExpect(jsonPath("$[1].taskArea").value("CLINICAL_INTERVENTION"))
                .andExpect(jsonPath("$[1].correct").value(2))
                .andExpect(jsonPath("$[1].total").value(4));
    }

    @Test
    @DisplayName("Another user's exams are not counted")
    void totals_onlyOwnExams() throws Exception {
        UUID someoneElse = UUID.randomUUID();
        takeExam(someoneElse, true, true);

        mockMvc.perform(get("/api/v1/exams/task-areas")
                        .header("Authorization", bearerToken(UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ---- helpers -----------------------------------------------------------

    private void addQuestion(String stem, TaskArea area) {
        Question question = Question.builder()
                .stem(stem)
                .category(QuestionCategory.ENDOCRINOLOGY)
                .taskArea(area)
                .difficulty(DifficultyLevel.MEDIUM)
                .explanation("Explanation")
                .isActive(true)
                .build();
        for (int i = 0; i < 4; i++) {
            question.getAnswerOptions().add(AnswerOption.builder()
                    .question(question)
                    .text(i == 0 ? RIGHT : "Wrong answer " + i)
                    .isCorrect(i == 0)
                    .orderIndex(i)
                    .build());
        }
        questionRepository.save(question);
    }

    private String startExam(UUID user) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/exams")
                        .header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionCount\": 4, \"categoryFilter\": \"ENDOCRINOLOGY\", \"timeLimitMinutes\": 0}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    /**
     * Answers the exam's questions. Diagnostic study questions are answered right when
     * {@code diagnosticRight} is true. Of the clinical intervention questions, with
     * {@code interventionRight} false the first one gets a wrong pick and the second
     * one is left unanswered; with true both are answered right.
     */
    private void answerAll(UUID user, String sessionId, boolean diagnosticRight, boolean interventionRight)
            throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/exams/{sessionId}", sessionId)
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode exam = objectMapper.readTree(result.getResponse().getContentAsString());

        for (JsonNode question : exam.get("questions")) {
            boolean diagnostic = question.get("taskArea").asText().equals("DIAGNOSTIC_STUDIES");
            boolean right = diagnostic ? diagnosticRight : interventionRight;
            if (!diagnostic && !interventionRight && question.get("stem").asText().endsWith("(2)")) {
                continue;  // left unanswered
            }
            String optionId = null;
            for (JsonNode option : question.get("answerOptions")) {
                if (option.get("text").asText().equals(RIGHT) == right) {
                    optionId = option.get("id").asText();
                    break;
                }
            }
            mockMvc.perform(post("/api/v1/exams/{sessionId}/questions/{questionId}/answer",
                                    sessionId, question.get("id").asText())
                            .header("Authorization", bearerToken(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"selectedOptionId\": \"" + optionId + "\", \"timeSpentSeconds\": 10}"))
                    .andExpect(status().isOk());
        }
        assertThat(exam.get("questions")).hasSize(4);
    }

    private String takeExam(UUID user, boolean diagnosticRight, boolean interventionRight) throws Exception {
        String sessionId = startExam(user);
        answerAll(user, sessionId, diagnosticRight, interventionRight);
        mockMvc.perform(post("/api/v1/exams/{sessionId}/complete", sessionId)
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk());
        return sessionId;
    }
}
