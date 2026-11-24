package com.pastudyhub.user.integration;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.pastudyhub.BaseIntegrationTest;
import com.pastudyhub.user.integration.TestAccounts.Account;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Closing an account deletes the user's data everywhere, and leaves other people's
 * data working. The user here (Alex) has a public deck that another user (Blake)
 * studied and copied, took an exam, and wrote one question Blake's exam used and
 * one nobody used. Questions are in nephrology, which nothing else uses.
 */
@DisplayName("Closing an account")
class AccountDeletionIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Deletes the user's data in every table and keeps other people's results and copies working")
    void deletesEverythingOfTheUser() throws Exception {
        Account alex = TestAccounts.register(mockMvc, objectMapper);
        Account blake = TestAccounts.register(mockMvc, objectMapper);
        UUID alexId = userId(alex);

        // Alex: a public deck with a card, studied by Alex and by Blake; Blake copies it
        String deckId = post(alex, "/api/v1/decks",
                Map.of("title", "Alex's renal deck", "category", "NEPHROLOGY", "isPublic", true)).get("id").asString();
        String cardId = post(alex, "/api/v1/decks/" + deckId + "/cards",
                Map.of("front", "Front", "back", "Back")).get("id").asString();
        post(alex, "/api/v1/cards/" + cardId + "/review", Map.of("quality", 4, "timeSpentSeconds", 5));
        post(blake, "/api/v1/cards/" + cardId + "/review", Map.of("quality", 4, "timeSpentSeconds", 5));
        String copyId = post(blake, "/api/v1/decks/" + deckId + "/clone", Map.of()).get("id").asString();

        // Alex writes two questions; one is shared and ends up in Blake's exam
        String sharedQuestion = writeQuestion(alex, "Shared by Alex", true);
        String privateQuestion = writeQuestion(alex, "Only in Alex's own exam", false);
        String blakesExam = finishExam(blake, true);
        // Alex's own exam over both of Alex's questions
        finishExam(alex, false);

        mockMvc.perform(delete("/api/v1/users/me").header("Authorization", alex.authorization())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\": \"" + TestAccounts.PASSWORD + "\"}"))
                .andExpect(status().isNoContent());

        // Nothing of Alex is left
        for (String table : List.of("decks", "review_schedules", "review_logs", "exam_sessions",
                "activity_logs", "daily_goals", "ai_usage", "refresh_tokens")) {
            assertThat(count("SELECT COUNT(*) FROM " + table + " WHERE user_id = ?", alexId))
                    .as(table).isZero();
        }
        assertThat(count("SELECT COUNT(*) FROM users WHERE id = ?", alexId)).isZero();
        assertThat(count("SELECT COUNT(*) FROM questions WHERE owner_id = ?", alexId)).isZero();
        assertThat(count("SELECT COUNT(*) FROM cards WHERE id = ?", UUID.fromString(cardId))).isZero();
        // Blake's progress on Alex's original card went with it
        assertThat(count("SELECT COUNT(*) FROM review_schedules WHERE card_id = ?", UUID.fromString(cardId))).isZero();

        // The question only Alex's own exam used is gone; the one in Blake's exam stays, without author, out of every new exam
        assertThat(count("SELECT COUNT(*) FROM questions WHERE id = ?", UUID.fromString(privateQuestion))).isZero();
        Map<String, Object> kept = jdbcTemplate.queryForMap(
                "SELECT owner_id, is_active, is_public FROM questions WHERE id = ?", UUID.fromString(sharedQuestion));
        assertThat(kept.get("owner_id")).isNull();
        assertThat(kept.get("is_active")).isEqualTo(false);
        assertThat(kept.get("is_public")).isEqualTo(false);

        // Blake's exam result still loads, and Blake's copy still has its card
        mockMvc.perform(get("/api/v1/exams/{id}/result", blakesExam).header("Authorization", blake.authorization()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answerDetails[0].stem").value("Shared by Alex"))
                // Still marked as written by another user, not as a starter question
                .andExpect(jsonPath("$.answerDetails[0].source").value("ANOTHER_USER"));
        mockMvc.perform(get("/api/v1/decks/{id}/cards", copyId).header("Authorization", blake.authorization()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].front").value("Front"));
    }

    // ---- helpers -------------------------------------------------------------

    private UUID userId(Account account) throws Exception {
        String body = mockMvc.perform(get("/api/v1/users/me").header("Authorization", account.authorization()))
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(body).get("id").asString());
    }

    private JsonNode post(Account account, String url, Map<String, Object> body) throws Exception {
        // MockMvcRequestBuilders.post written out, because this helper is also called post
        String response = mockMvc.perform(
                        MockMvcRequestBuilders.post(url)
                                .header("Authorization", account.authorization())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        return response.isEmpty() ? objectMapper.createObjectNode() : objectMapper.readTree(response);
    }

    private String writeQuestion(Account account, String stem, boolean shared) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("stem", stem);
        body.put("category", "NEPHROLOGY");
        body.put("taskArea", "MOST_LIKELY_DIAGNOSIS");
        body.put("difficulty", "MEDIUM");
        body.put("explanation", "Explanation");
        body.put("isPublic", shared);
        body.put("answerOptions", List.of(Map.of("text", "Right", "isCorrect", true),
                Map.of("text", "Wrong", "isCorrect", false)));
        return post(account, "/api/v1/questions", body).get("id").asString();
    }

    // Blake's exam has 1 question (the shared one), Alex's has both of Alex's
    private String finishExam(Account account, boolean includeShared) throws Exception {
        int count = includeShared ? 1 : 2;
        JsonNode exam = post(account, "/api/v1/exams", Map.of("questionCount", count, "categoryFilter", "NEPHROLOGY",
                "timeLimitMinutes", 0, "includeSharedQuestions", includeShared));
        String sessionId = exam.get("id").asString();
        JsonNode question = exam.get("questions").get(0);
        post(account, "/api/v1/exams/" + sessionId + "/questions/" + question.get("id").asString() + "/answer",
                Map.of("selectedOptionId", question.get("answerOptions").get(0).get("id").asString(),
                        "timeSpentSeconds", 5));
        post(account, "/api/v1/exams/" + sessionId + "/complete", Map.of());
        return sessionId;
    }

    private int count(String sql, Object param) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class, param);
        return value == null ? 0 : value;
    }
}
