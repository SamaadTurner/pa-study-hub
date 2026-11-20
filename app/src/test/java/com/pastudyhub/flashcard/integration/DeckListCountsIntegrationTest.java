package com.pastudyhub.flashcard.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pastudyhub.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The deck lists get their counts from one grouped query per page instead of
 * three queries per deck. The numbers have to stay the same as a single deck's.
 */
@DisplayName("Card counts in the deck lists")
class DeckListCountsIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("The list counts cards and cards to study today like the single deck does")
    void listCounts_matchSingleDeck() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        // A word no other deck has, to find this deck in the public list
        String word = "counts" + UUID.randomUUID().toString().substring(0, 8);
        String deckId = createDeck(owner, "Deck " + word, true);
        String emptyDeckId = createDeck(owner, "Empty deck", false);
        List<String> cards = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            cards.add(createCard(owner, deckId, "Card " + i));
        }

        // Card 1: studied, next review in the future. Card 2: studied, due again.
        // Card 3: deleted. Cards 4 and 5: never studied by the owner; someone else
        // studied card 4, which must not change the owner's numbers.
        review(owner, cards.get(0));
        review(owner, cards.get(1));
        jdbcTemplate.update("UPDATE review_schedules SET next_review_date = ? WHERE user_id = ? AND card_id = ?",
                Date.valueOf(LocalDate.now().minusDays(1)), owner, UUID.fromString(cards.get(1)));
        mockMvc.perform(delete("/api/v1/cards/{id}", cards.get(2)).header("Authorization", bearerToken(owner)))
                .andExpect(status().is2xxSuccessful());
        review(other, cards.get(3));

        JsonNode list = getJson(owner, "/api/v1/decks?size=100").get("content");
        JsonNode inList = find(list, deckId);
        JsonNode emptyInList = find(list, emptyDeckId);
        JsonNode single = getJson(owner, "/api/v1/decks/" + deckId);

        assertThat(inList.get("cardCount").asInt()).isEqualTo(4);
        assertThat(inList.get("cardsToReview").asInt()).isEqualTo(3);
        assertThat(inList.get("cardCount").asInt()).isEqualTo(single.get("cardCount").asInt());
        assertThat(inList.get("cardsToReview").asInt()).isEqualTo(single.get("cardsToReview").asInt());
        assertThat(emptyInList.get("cardCount").asInt()).isZero();
        assertThat(emptyInList.get("cardsToReview").asInt()).isZero();

        // The public list shows the same card count to anyone
        JsonNode publicList = getJson(other, "/api/v1/decks/public?search=" + word + "&size=100").get("content");
        assertThat(find(publicList, deckId).get("cardCount").asInt()).isEqualTo(4);
    }

    // ---- helpers -------------------------------------------------------------

    private String createDeck(UUID user, String title, boolean isPublic) throws Exception {
        return postJson(user, "/api/v1/decks",
                Map.of("title", title, "description", "", "category", "CARDIOLOGY", "isPublic", isPublic))
                .get("id").asText();
    }

    private String createCard(UUID user, String deckId, String front) throws Exception {
        return postJson(user, "/api/v1/decks/" + deckId + "/cards",
                Map.of("front", front, "back", "Back", "tags", List.of())).get("id").asText();
    }

    private void review(UUID user, String cardId) throws Exception {
        postJson(user, "/api/v1/cards/" + cardId + "/review", Map.of("quality", 4));
    }

    private JsonNode postJson(UUID user, String url, Map<String, Object> body) throws Exception {
        String response = mockMvc.perform(post(url).header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private JsonNode getJson(UUID user, String url) throws Exception {
        String response = mockMvc.perform(get(url).header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private JsonNode find(JsonNode decks, String deckId) {
        for (JsonNode deck : decks) {
            if (deck.get("id").asText().equals(deckId)) {
                return deck;
            }
        }
        throw new AssertionError("Deck not in the list: " + deckId);
    }
}
