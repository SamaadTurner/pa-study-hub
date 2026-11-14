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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Searching and paging through the public deck list (GET /decks/public).
 *
 * <p>The page in the frontend used to load the first 20 public decks and search
 * only those, so older decks could never be found. The search now runs on the
 * server over every public deck.
 *
 * <p>Other tests add public decks to the same database, so every test here puts
 * its own random word in its deck titles and searches for that word.
 */
@DisplayName("Public decks: search and paging")
class PublicDecksIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private final UUID owner = UUID.randomUUID();
    private final UUID visitor = UUID.randomUUID();

    @Test
    @DisplayName("Search finds a deck by part of its title, whatever the capital letters")
    void search_matchesTitleIgnoringCase() throws Exception {
        String word = randomWord();
        String match = createDeck("Heart Failure " + word, "", "CARDIOLOGY", true);
        createDeck("Asthma " + word, "", "PULMONOLOGY", true);

        mockMvc.perform(get("/api/v1/decks/public")
                        .header("Authorization", bearerToken(visitor))
                        .param("search", "hEaRt fAiLuRe " + word.toUpperCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", contains(match)))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("Search also looks in the description")
    void search_matchesDescription() throws Exception {
        String word = randomWord();
        String match = createDeck("Murmurs", "Systolic and diastolic, " + word, "CARDIOLOGY", true);

        mockMvc.perform(get("/api/v1/decks/public")
                        .header("Authorization", bearerToken(visitor))
                        .param("search", word))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", contains(match)));
    }

    @Test
    @DisplayName("Search never returns private or deleted decks")
    void search_skipsPrivateAndDeletedDecks() throws Exception {
        String word = randomWord();
        String publicDeck = createDeck("Public " + word, "", "CARDIOLOGY", true);
        createDeck("Private " + word, "", "CARDIOLOGY", false);
        String deletedDeck = createDeck("Deleted " + word, "", "CARDIOLOGY", true);

        mockMvc.perform(delete("/api/v1/decks/{deckId}", deletedDeck)
                        .header("Authorization", bearerToken(owner)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/decks/public")
                        .header("Authorization", bearerToken(visitor))
                        .param("search", word))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", contains(publicDeck)));
    }

    @Test
    @DisplayName("Search and category can be used together")
    void search_withCategory() throws Exception {
        String word = randomWord();
        String cardiology = createDeck("Review " + word, "", "CARDIOLOGY", true);
        String neurology = createDeck("Review " + word, "", "NEUROLOGY", true);

        mockMvc.perform(get("/api/v1/decks/public")
                        .header("Authorization", bearerToken(visitor))
                        .param("search", word)
                        .param("category", "NEUROLOGY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", contains(neurology)));

        mockMvc.perform(get("/api/v1/decks/public")
                        .header("Authorization", bearerToken(visitor))
                        .param("search", word))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", containsInAnyOrder(cardiology, neurology)));
    }

    @Test
    @DisplayName("%, _ and ! in the search are plain characters, not wildcards")
    void search_treatsWildcardsAsText() throws Exception {
        String word = randomWord();
        String percent = createDeck(word + " 50% dose", "", "PHARMACOLOGY", true);
        createDeck(word + " 50 mg dose", "", "PHARMACOLOGY", true);
        String underscore = createDeck(word + " beta_blockers", "", "PHARMACOLOGY", true);
        createDeck(word + " beta blockers", "", "PHARMACOLOGY", true);
        String bang = createDeck(word + " stat! orders", "", "PHARMACOLOGY", true);

        // If % were a wildcard, "50% dose" would also match "50 mg dose"
        mockMvc.perform(get("/api/v1/decks/public")
                        .header("Authorization", bearerToken(visitor))
                        .param("search", word + " 50% dose"))
                .andExpect(jsonPath("$.content[*].id", contains(percent)));

        // If _ were a wildcard, it would also match the space in "beta blockers"
        mockMvc.perform(get("/api/v1/decks/public")
                        .header("Authorization", bearerToken(visitor))
                        .param("search", word + " beta_blockers"))
                .andExpect(jsonPath("$.content[*].id", contains(underscore)));

        mockMvc.perform(get("/api/v1/decks/public")
                        .header("Authorization", bearerToken(visitor))
                        .param("search", word + " stat!"))
                .andExpect(jsonPath("$.content[*].id", contains(bang)));
    }

    @Test
    @DisplayName("A blank search is the same as no search")
    void search_blankMeansEverything() throws Exception {
        createDeck("Any deck " + randomWord(), "", "CARDIOLOGY", true);

        long withoutSearch = publicDecks("size", "1").get("totalElements").asLong();
        long blankSearch = publicDecks("size", "1", "search", "   ").get("totalElements").asLong();

        assertThat(withoutSearch).isGreaterThan(0);
        assertThat(blankSearch).isEqualTo(withoutSearch);
    }

    @Test
    @DisplayName("A search with no match gives an empty page, not an error")
    void search_noMatch() throws Exception {
        mockMvc.perform(get("/api/v1/decks/public")
                        .header("Authorization", bearerToken(visitor))
                        .param("search", "nothing-is-called-" + randomWord()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("25 matching decks come back as pages of 10, 10 and 5, newest first, each deck once")
    void paging_returnsEveryDeckOnce() throws Exception {
        String word = randomWord();
        List<String> created = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            created.add(createDeck("Paging " + word + " " + i, "", "CARDIOLOGY", true));
        }

        JsonNode first = publicDecks("search", word, "size", "10", "page", "0");
        JsonNode second = publicDecks("search", word, "size", "10", "page", "1");
        JsonNode third = publicDecks("search", word, "size", "10", "page", "2");

        assertThat(first.get("totalElements").asInt()).isEqualTo(25);
        assertThat(first.get("totalPages").asInt()).isEqualTo(3);
        assertThat(first.get("last").asBoolean()).isFalse();
        assertThat(third.get("last").asBoolean()).isTrue();

        List<String> returned = new ArrayList<>();
        returned.addAll(idsOf(first));
        returned.addAll(idsOf(second));
        returned.addAll(idsOf(third));

        assertThat(idsOf(first)).hasSize(10);
        assertThat(idsOf(second)).hasSize(10);
        assertThat(idsOf(third)).hasSize(5);

        // Every deck exactly once across the three pages
        Set<String> distinct = new HashSet<>(returned);
        assertThat(distinct).hasSize(25);
        assertThat(distinct).containsExactlyInAnyOrderElementsOf(created);

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
    @DisplayName("Out-of-range page and size values are brought back into range")
    void paging_limitsPageAndSize() throws Exception {
        createDeck("Limits " + randomWord(), "", "CARDIOLOGY", true);

        // No more than 100 decks per request
        assertThat(publicDecks("size", "5000").get("size").asInt()).isEqualTo(100);
        // At least one
        assertThat(publicDecks("size", "0").get("size").asInt()).isEqualTo(1);
        // A negative page is the first page
        assertThat(publicDecks("page", "-3").get("number").asInt()).isEqualTo(0);
    }

    // ---- helpers -----------------------------------------------------------

    // A word that no other deck in the database has in its title
    private String randomWord() {
        return "zq" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String createDeck(String title, String description, String category, boolean isPublic)
            throws Exception {
        String json = "{\"title\": \"" + title + "\", \"description\": \"" + description + "\", "
                + "\"category\": \"" + category + "\", \"isPublic\": " + isPublic + "}";

        MvcResult result = mockMvc.perform(post("/api/v1/decks")
                        .header("Authorization", bearerToken(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    // Calls the public list with the given parameters (name, value, name, value, ...)
    private JsonNode publicDecks(String... params) throws Exception {
        var request = get("/api/v1/decks/public").header("Authorization", bearerToken(visitor));
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
        for (JsonNode deck : page.get("content")) {
            ids.add(deck.get("id").asText());
        }
        return ids;
    }
}
