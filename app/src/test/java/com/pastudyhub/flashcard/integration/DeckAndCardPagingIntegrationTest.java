package com.pastudyhub.flashcard.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pastudyhub.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Paging through a user's own decks (GET /decks) and through the cards of a deck
 * (GET /decks/{id}/cards).
 *
 * <p>The frontend asks for these lists page by page until it has everything, so
 * every deck and every card has to be on exactly one page, and the order must not
 * change between two requests.
 */
@DisplayName("Paging through decks and cards")
class DeckAndCardPagingIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // A new user for every test, so each test only sees its own decks
    private final UUID user = UUID.randomUUID();

    @Test
    @DisplayName("25 decks come back as pages of 10, 10 and 5, newest first, each deck once")
    void decks_everyDeckOnOnePage() throws Exception {
        List<String> created = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            created.add(createDeck("Deck " + i));
        }

        JsonNode first = getJson("/api/v1/decks", "size", "10", "page", "0");
        JsonNode second = getJson("/api/v1/decks", "size", "10", "page", "1");
        JsonNode third = getJson("/api/v1/decks", "size", "10", "page", "2");

        assertThat(first.get("totalElements").asInt()).isEqualTo(25);
        assertThat(first.get("last").asBoolean()).isFalse();
        assertThat(third.get("last").asBoolean()).isTrue();

        List<String> returned = new ArrayList<>();
        returned.addAll(idsOf(first));
        returned.addAll(idsOf(second));
        returned.addAll(idsOf(third));

        // Every deck exactly once across the three pages
        assertThat(returned).hasSize(25);
        assertThat(returned).containsExactlyInAnyOrderElementsOf(created);

        // Newest first. The dates are compared and not the ids, because two decks
        // made in the same instant have the same date and can come in either order.
        List<LocalDateTime> dates = new ArrayList<>();
        for (JsonNode page : List.of(first, second, third)) {
            for (JsonNode deck : page.get("content")) {
                dates.add(LocalDateTime.parse(deck.get("updatedAt").asText()));
            }
        }
        assertThat(dates).isSortedAccordingTo(Comparator.reverseOrder());
    }

    @Test
    @DisplayName("Out-of-range page and size values for decks are brought back into range")
    void decks_limitPageAndSize() throws Exception {
        createDeck("Only deck");

        assertThat(getJson("/api/v1/decks", "size", "5000").get("size").asInt()).isEqualTo(100);
        assertThat(getJson("/api/v1/decks", "size", "0").get("size").asInt()).isEqualTo(1);
        assertThat(getJson("/api/v1/decks", "page", "-1").get("number").asInt()).isEqualTo(0);
    }

    @Test
    @DisplayName("120 cards come back over two pages in the order they were added, each card once")
    void cards_everyCardOnOnePage() throws Exception {
        String deckId = createDeck("Big deck");
        List<String> created = new ArrayList<>();
        for (int i = 1; i <= 120; i++) {
            created.add(createCard(deckId, "Question " + i));
        }

        String url = "/api/v1/decks/" + deckId + "/cards";
        JsonNode first = getJson(url, "size", "100", "page", "0");
        JsonNode second = getJson(url, "size", "100", "page", "1");

        assertThat(first.get("totalElements").asInt()).isEqualTo(120);
        assertThat(first.get("last").asBoolean()).isFalse();
        assertThat(second.get("last").asBoolean()).isTrue();

        List<String> returned = new ArrayList<>();
        returned.addAll(idsOf(first));
        returned.addAll(idsOf(second));
        assertThat(returned).containsExactlyElementsOf(created);
    }

    @Test
    @DisplayName("Editing a card does not move it to another place in the list")
    void cards_keepTheirOrderAfterAnEdit() throws Exception {
        String deckId = createDeck("Order deck");
        List<String> created = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            created.add(createCard(deckId, "Question " + i));
        }

        // Change the third card. A list without a fixed order can come back from
        // PostgreSQL with the changed row at the end.
        mockMvc.perform(put("/api/v1/cards/{cardId}", created.get(2))
                        .header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"front\": \"Question 3, reworded\", \"back\": \"Answer\", \"tags\": []}"))
                .andExpect(status().isOk());

        String url = "/api/v1/decks/" + deckId + "/cards";
        List<String> returned = new ArrayList<>();
        returned.addAll(idsOf(getJson(url, "size", "5", "page", "0")));
        returned.addAll(idsOf(getJson(url, "size", "5", "page", "1")));
        returned.addAll(idsOf(getJson(url, "size", "5", "page", "2")));

        assertThat(returned).containsExactlyElementsOf(created);
    }

    @Test
    @DisplayName("A copied deck has its cards in the same order as the original")
    void cards_keepTheirOrderInACopy() throws Exception {
        String deckId = createDeck("Original");
        List<String> fronts = new ArrayList<>();
        for (int i = 1; i <= 40; i++) {
            fronts.add("Question " + i);
            createCard(deckId, "Question " + i);
        }

        // The copies are all saved in the same instant, so their dates can't
        // give the order. This is what the position number is for.
        String copyId = postAndGetId("/api/v1/decks/" + deckId + "/clone", "");

        JsonNode copyCards = getJson("/api/v1/decks/" + copyId + "/cards", "size", "100");
        List<String> copiedFronts = new ArrayList<>();
        for (JsonNode card : copyCards.get("content")) {
            copiedFronts.add(card.get("front").asText());
        }
        assertThat(copiedFronts).containsExactlyElementsOf(fronts);
    }

    @Test
    @DisplayName("A card added after one was deleted still goes to the end")
    void cards_newCardGoesToTheEndAfterADelete() throws Exception {
        String deckId = createDeck("Delete deck");
        String first = createCard(deckId, "Question 1");
        String second = createCard(deckId, "Question 2");
        String third = createCard(deckId, "Question 3");

        mockMvc.perform(delete("/api/v1/cards/{cardId}", third)
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isNoContent());
        String fourth = createCard(deckId, "Question 4");

        JsonNode cards = getJson("/api/v1/decks/" + deckId + "/cards");
        assertThat(idsOf(cards)).containsExactly(first, second, fourth);
    }

    @Test
    @DisplayName("Out-of-range page and size values for cards are brought back into range")
    void cards_limitPageAndSize() throws Exception {
        String deckId = createDeck("Limits deck");
        createCard(deckId, "Only question");
        String url = "/api/v1/decks/" + deckId + "/cards";

        assertThat(getJson(url, "size", "5000").get("size").asInt()).isEqualTo(100);
        assertThat(getJson(url, "size", "0").get("size").asInt()).isEqualTo(1);
        assertThat(getJson(url, "page", "-1").get("number").asInt()).isEqualTo(0);
    }

    // ---- helpers -----------------------------------------------------------

    private String createDeck(String title) throws Exception {
        String json = "{\"title\": \"" + title + "\", \"description\": \"\", "
                + "\"category\": \"CARDIOLOGY\", \"isPublic\": false}";
        return postAndGetId("/api/v1/decks", json);
    }

    private String createCard(String deckId, String front) throws Exception {
        String json = "{\"front\": \"" + front + "\", \"back\": \"Answer\", \"tags\": []}";
        return postAndGetId("/api/v1/decks/" + deckId + "/cards", json);
    }

    private String postAndGetId(String url, String json) throws Exception {
        MvcResult result = mockMvc.perform(post(url)
                        .header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    // Calls a list endpoint with the given parameters (name, value, name, value, ...)
    private JsonNode getJson(String url, String... params) throws Exception {
        var request = get(url).header("Authorization", bearerToken(user));
        for (int i = 0; i < params.length; i += 2) {
            request = request.param(params[i], params[i + 1]);
        }

        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private List<String> idsOf(JsonNode page) {
        List<String> ids = new ArrayList<>();
        for (JsonNode item : page.get("content")) {
            ids.add(item.get("id").asText());
        }
        return ids;
    }
}
