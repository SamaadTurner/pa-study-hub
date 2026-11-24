package com.pastudyhub.exam.integration;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.pastudyhub.BaseIntegrationTest;
import com.pastudyhub.exam.engine.ExamTimeLimit;
import com.pastudyhub.exam.service.ExamTimeLimitJob;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The server ends timed exams itself. Every test makes a 1 minute exam from two
 * questions its user writes (EENT, which no migration or other test uses), then
 * moves the exam's start time into the past instead of waiting.
 */
@DisplayName("Exam time limit on the server")
class ExamTimeLimitIntegrationTest extends BaseIntegrationTest {

    private static final String RIGHT = "Right answer";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ExamTimeLimitJob examTimeLimitJob;

    @Test
    @DisplayName("Opening an exam after its time ran out hands it in with the saved answers")
    void open_afterTimeUp_handsIn() throws Exception {
        UUID user = UUID.randomUUID();
        String sessionId = startTimedExam(user);
        answerFirstQuestionRight(user, sessionId);
        startedSecondsAgo(sessionId, 60 + ExamTimeLimit.GRACE_SECONDS + 5);

        mockMvc.perform(get("/api/v1/exams/{id}", sessionId).header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.timeRemainingSeconds").doesNotExist());

        // Scored out of both questions, and it took exactly the time limit
        mockMvc.perform(get("/api/v1/exams/{id}/result", sessionId).header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rawScore").value(1))
                .andExpect(jsonPath("$.totalQuestions").value(2))
                .andExpect(jsonPath("$.durationSeconds").value(60));
    }

    @Test
    @DisplayName("An answer after the time is up is refused, and the exam is handed in")
    void answer_afterTimeUp_refused() throws Exception {
        UUID user = UUID.randomUUID();
        String sessionId = startTimedExam(user);
        answerFirstQuestionRight(user, sessionId);
        // Read before the time runs out: opening the exam afterwards would hand it in
        JsonNode second = getExam(user, sessionId).get("questions").get(1);
        startedSecondsAgo(sessionId, 60 + ExamTimeLimit.GRACE_SECONDS + 5);

        mockMvc.perform(post("/api/v1/exams/{id}/questions/{q}/answer", sessionId, second.get("id").asString())
                        .header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(answerBody(rightOption(second))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "Time is up. The exam was handed in with the answers saved before the time ran out."));

        // The hand-in was kept although the request failed, without the late answer
        mockMvc.perform(get("/api/v1/exams/{id}/result", sessionId).header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rawScore").value(1));
    }

    @Test
    @DisplayName("An answer in the grace period right after the deadline still counts")
    void answer_inGracePeriod_accepted() throws Exception {
        UUID user = UUID.randomUUID();
        String sessionId = startTimedExam(user);
        startedSecondsAgo(sessionId, 60 + 10);

        answerFirstQuestionRight(user, sessionId);
        mockMvc.perform(post("/api/v1/exams/{id}/complete", sessionId).header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rawScore").value(1))
                .andExpect(jsonPath("$.durationSeconds").value(60));
    }

    @Test
    @DisplayName("The background job hands in expired exams and leaves running and untimed ones alone")
    void job_handsInExpiredExamsOnly() throws Exception {
        UUID user = UUID.randomUUID();
        String expired = startTimedExam(user);
        String running = startTimedExam(user);
        String untimed = startExam(user, 0);
        startedSecondsAgo(expired, 60 + ExamTimeLimit.GRACE_SECONDS + 5);
        startedSecondsAgo(untimed, 3600);

        examTimeLimitJob.handInExpiredExams();

        assertStatus(expired, "COMPLETED");
        assertStatus(running, "IN_PROGRESS");
        assertStatus(untimed, "IN_PROGRESS");

        // It counts toward the dashboard like an exam the user finished
        mockMvc.perform(get("/api/v1/progress/dashboard").header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStreak").value(1));
    }

    @Test
    @DisplayName("Leaving an exam after its time ran out hands it in instead of abandoning it")
    void abandon_afterTimeUp_handsIn() throws Exception {
        UUID user = UUID.randomUUID();
        String sessionId = startTimedExam(user);
        startedSecondsAgo(sessionId, 60 + ExamTimeLimit.GRACE_SECONDS + 5);

        mockMvc.perform(post("/api/v1/exams/{id}/abandon", sessionId).header("Authorization", bearerToken(user)))
                .andExpect(status().isNoContent());
        assertStatus(sessionId, "COMPLETED");
    }

    // ---- helpers -------------------------------------------------------------

    private String startTimedExam(UUID user) throws Exception {
        return startExam(user, 1);
    }

    private String startExam(UUID user, int minutes) throws Exception {
        writeQuestions(user);
        MvcResult result = mockMvc.perform(post("/api/v1/exams")
                        .header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionCount\": 2, \"categoryFilter\": \"EENT\", \"timeLimitMinutes\": "
                                + minutes + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asString();
    }

    // Two questions for the user, written only the first time
    private void writeQuestions(UUID user) throws Exception {
        MvcResult mine = mockMvc.perform(get("/api/v1/questions/mine").header("Authorization", bearerToken(user)))
                .andReturn();
        if (objectMapper.readTree(mine.getResponse().getContentAsString()).get("totalElements").asInt() > 0) {
            return;
        }
        for (int i = 1; i <= 2; i++) {
            Map<String, Object> body = new HashMap<>();
            body.put("stem", "Ear, nose and throat question " + i);
            body.put("category", "EENT");
            body.put("taskArea", "MOST_LIKELY_DIAGNOSIS");
            body.put("difficulty", "MEDIUM");
            body.put("explanation", "Explanation");
            body.put("isPublic", false);
            body.put("answerOptions", List.of(
                    Map.of("text", RIGHT, "isCorrect", true),
                    Map.of("text", "Wrong answer", "isCorrect", false)));
            mockMvc.perform(post("/api/v1/questions")
                            .header("Authorization", bearerToken(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isCreated());
        }
    }

    // started_at can't be changed through the entity (it is not updatable), so plain SQL
    private void startedSecondsAgo(String sessionId, int seconds) {
        jdbcTemplate.update("UPDATE exam_sessions SET started_at = ? WHERE id = ?",
                Timestamp.valueOf(LocalDateTime.now().minusSeconds(seconds)), UUID.fromString(sessionId));
    }

    private JsonNode getExam(UUID user, String sessionId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/exams/{id}", sessionId)
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String rightOption(JsonNode question) {
        for (JsonNode option : question.get("answerOptions")) {
            if (option.get("text").asString().equals(RIGHT)) {
                return option.get("id").asString();
            }
        }
        throw new IllegalStateException("No right option");
    }

    private String answerBody(String optionId) {
        return "{\"selectedOptionId\": \"" + optionId + "\", \"timeSpentSeconds\": 10}";
    }

    private void answerFirstQuestionRight(UUID user, String sessionId) throws Exception {
        JsonNode first = getExam(user, sessionId).get("questions").get(0);
        mockMvc.perform(post("/api/v1/exams/{id}/questions/{q}/answer", sessionId, first.get("id").asString())
                        .header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(answerBody(rightOption(first))))
                .andExpect(status().isOk());
    }

    private void assertStatus(String sessionId, String expected) {
        String actual = jdbcTemplate.queryForObject("SELECT status FROM exam_sessions WHERE id = ?",
                String.class, UUID.fromString(sessionId));
        assertThat(actual).isEqualTo(expected);
    }
}
