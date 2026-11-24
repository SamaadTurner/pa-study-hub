package com.pastudyhub.flashcard.integration;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.pastudyhub.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * When a public deck goes away, the other students who studied it keep their own
 * copy, with their progress moved over.
 */
@DisplayName("Handing a removed public deck over to its students")
class DeckHandoverIntegrationTest extends BaseIntegrationTest {

    private static final String PASSWORD = "Str0ng!Passw0rd";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Deleting a public deck gives each student who studied it a copy with their progress")
    void deleteDeck_studentsKeepCopy() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID student = UUID.randomUUID();
        UUID onlyLooked = UUID.randomUUID();
        String title = "Shared deck " + UUID.randomUUID();
        String deckId = createDeck(owner, title);
        String firstCard = createCard(owner, deckId, "First");
        createCard(owner, deckId, "Second");
        review(student, firstCard);

        mockMvc.perform(delete("/api/v1/decks/{id}", deckId).header("Authorization", bearerToken(owner)))
                .andExpect(status().isNoContent());

        assertStudentHasCopy(student, title);
        // Someone who never studied a card gets nothing
        assertThat(decksOf(onlyLooked)).isEmpty();
    }

    @Test
    @DisplayName("Closing an account hands its studied public decks over before deleting them")
    void deleteAccount_studentsKeepCopy() throws Exception {
        String email = "owner-" + UUID.randomUUID() + "@example.com";
        String ownerToken = register(email);
        UUID student = UUID.randomUUID();
        String title = "Owner's deck " + UUID.randomUUID();

        String deckId = postJson(ownerToken, "/api/v1/decks",
                Map.of("title", title, "description", "", "category", "CARDIOLOGY", "isPublic", true)).get("id").asString();
        String firstCard = postJson(ownerToken, "/api/v1/decks/" + deckId + "/cards",
                Map.of("front", "First", "back", "Back", "tags", List.of())).get("id").asString();
        postJson(ownerToken, "/api/v1/decks/" + deckId + "/cards",
                Map.of("front", "Second", "back", "Back", "tags", List.of()));
        review(student, firstCard);

        mockMvc.perform(delete("/api/v1/users/me").header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\": \"" + PASSWORD + "\"}"))
                .andExpect(status().isNoContent());

        assertStudentHasCopy(student, title);
    }

    // The copy is private, has both cards in order, and the studied card is not due
    // again today (its schedule moved), so only the other card is to study
    private void assertStudentHasCopy(UUID student, String title) throws Exception {
        JsonNode decks = decksOf(student);
        assertThat(decks).hasSize(1);
        JsonNode copy = decks.get(0);
        assertThat(copy.get("title").asString()).isEqualTo(title);
        assertThat(copy.get("isPublic").asBoolean()).isFalse();
        assertThat(copy.get("cardCount").asInt()).isEqualTo(2);
        assertThat(copy.get("cardsToReview").asInt()).isEqualTo(1);

        JsonNode cards = getJson(bearerToken(student), "/api/v1/decks/" + copy.get("id").asString() + "/cards").get("content");
        assertThat(cards.get(0).get("front").asString()).isEqualTo("First");
        // The review history moved with the schedule
        Integer logs = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM review_logs WHERE user_id = ? AND card_id = ?",
                Integer.class, student, UUID.fromString(cards.get(0).get("id").asString()));
        assertThat(logs).isEqualTo(1);
    }

    // ---- helpers -------------------------------------------------------------

    private String register(String email) throws Exception {
        JsonNode body = objectMapper.readTree(mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", PASSWORD,
                                "firstName", "Owner", "lastName", "Test"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        return "Bearer " + body.get("accessToken").asString();
    }

    private String createDeck(UUID owner, String title) throws Exception {
        return postJson(bearerToken(owner), "/api/v1/decks",
                Map.of("title", title, "description", "", "category", "CARDIOLOGY", "isPublic", true)).get("id").asString();
    }

    private String createCard(UUID owner, String deckId, String front) throws Exception {
        return postJson(bearerToken(owner), "/api/v1/decks/" + deckId + "/cards",
                Map.of("front", front, "back", "Back", "tags", List.of())).get("id").asString();
    }

    private void review(UUID user, String cardId) throws Exception {
        postJson(bearerToken(user), "/api/v1/cards/" + cardId + "/review", Map.of("quality", 4));
    }

    private JsonNode decksOf(UUID user) throws Exception {
        return getJson(bearerToken(user), "/api/v1/decks?size=100").get("content");
    }

    private JsonNode postJson(String authorization, String url, Map<String, Object> body) throws Exception {
        String response = mockMvc.perform(post(url).header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private JsonNode getJson(String authorization, String url) throws Exception {
        return objectMapper.readTree(mockMvc.perform(get(url).header("Authorization", authorization))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }
}
