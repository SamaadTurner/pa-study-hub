package com.pastudyhub.flashcard.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pastudyhub.BaseIntegrationTest;
import com.pastudyhub.flashcard.repository.ReviewScheduleRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Who can read a deck.
 *
 * <p>Changing a deck was always limited to its owner, but reading was not checked at
 * all: any logged-in user who had a deck's ID could load the deck, its cards and its
 * stats, even when the deck was private. These tests cover the rule that a private
 * deck is only for its owner and a public deck is for everyone.
 */
@DisplayName("Who can read a deck")
class DeckAccessIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReviewScheduleRepository reviewScheduleRepository;

    @Test
    @DisplayName("The owner can read their own private deck, its cards and its stats")
    void owner_canReadPrivateDeck() throws Exception {
        UUID owner = UUID.randomUUID();
        String deckId = createDeck(owner, false);
        createCard(owner, deckId, "Owner's card");

        mockMvc.perform(get("/api/v1/decks/{deckId}", deckId)
                        .header("Authorization", bearerToken(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(deckId));

        mockMvc.perform(get("/api/v1/decks/{deckId}/cards", deckId)
                        .header("Authorization", bearerToken(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        mockMvc.perform(get("/api/v1/decks/{deckId}/stats", deckId)
                        .header("Authorization", bearerToken(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCards").value(1));
    }

    @Test
    @DisplayName("Another user can't open a private deck")
    void stranger_cantReadPrivateDeck() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();
        String deckId = createDeck(owner, false);

        mockMvc.perform(get("/api/v1/decks/{deckId}", deckId)
                        .header("Authorization", bearerToken(stranger)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("You do not have permission to view this deck"))
                // nothing about the deck leaks out in the error
                .andExpect(jsonPath("$.isPublic").doesNotExist());
    }

    @Test
    @DisplayName("Another user can't list the cards of a private deck")
    void stranger_cantReadPrivateCards() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();
        String deckId = createDeck(owner, false);
        createCard(owner, deckId, "Secret question");

        MvcResult result = mockMvc.perform(get("/api/v1/decks/{deckId}/cards", deckId)
                        .header("Authorization", bearerToken(stranger)))
                .andExpect(status().isForbidden())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain("Secret question");
    }

    @Test
    @DisplayName("Another user can't see the stats of a private deck")
    void stranger_cantReadPrivateStats() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();
        String deckId = createDeck(owner, false);
        createCard(owner, deckId, "Some card");

        mockMvc.perform(get("/api/v1/decks/{deckId}/stats", deckId)
                        .header("Authorization", bearerToken(stranger)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.totalCards").doesNotExist());
    }

    @Test
    @DisplayName("Another user can't copy a private deck")
    void stranger_cantClonePrivateDeck() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();
        String deckId = createDeck(owner, false);

        mockMvc.perform(post("/api/v1/decks/{deckId}/clone", deckId)
                        .header("Authorization", bearerToken(stranger)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Another user can't review a card from a private deck")
    void stranger_cantReviewPrivateCard() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();
        String deckId = createDeck(owner, false);
        String cardId = createCard(owner, deckId, "Private card");

        mockMvc.perform(post("/api/v1/cards/{cardId}/review", cardId)
                        .header("Authorization", bearerToken(stranger))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quality\": 4}"))
                .andExpect(status().isForbidden());

        // The refused review must not leave a schedule behind for that user
        assertThat(reviewScheduleRepository.findByCardIdAndUserId(UUID.fromString(cardId), stranger))
                .isEmpty();
    }

    @Test
    @DisplayName("Anyone logged in can read a public deck, its cards and its stats")
    void anyone_canReadPublicDeck() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();
        String deckId = createDeck(owner, true);
        createCard(owner, deckId, "Shared card");

        mockMvc.perform(get("/api/v1/decks/{deckId}", deckId)
                        .header("Authorization", bearerToken(otherUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(deckId))
                .andExpect(jsonPath("$.isPublic").value(true));

        mockMvc.perform(get("/api/v1/decks/{deckId}/cards", deckId)
                        .header("Authorization", bearerToken(otherUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].front").value("Shared card"));

        mockMvc.perform(get("/api/v1/decks/{deckId}/stats", deckId)
                        .header("Authorization", bearerToken(otherUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCards").value(1));
    }

    @Test
    @DisplayName("Reading a public deck still doesn't let another user change it")
    void publicDeck_isReadOnlyForOthers() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();
        String deckId = createDeck(owner, true);

        mockMvc.perform(put("/api/v1/decks/{deckId}", deckId)
                        .header("Authorization", bearerToken(otherUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deckJson("Taken over", true)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/decks/{deckId}/cards", deckId)
                        .header("Authorization", bearerToken(otherUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"front\": \"Not my deck\", \"back\": \"Answer\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Making a deck private again closes it to other users")
    void deckMadePrivate_isClosedAgain() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();
        String deckId = createDeck(owner, true);

        mockMvc.perform(get("/api/v1/decks/{deckId}", deckId)
                        .header("Authorization", bearerToken(otherUser)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/decks/{deckId}", deckId)
                        .header("Authorization", bearerToken(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deckJson("Access deck", false)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/decks/{deckId}", deckId)
                        .header("Authorization", bearerToken(otherUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Without a login even a public deck can't be read")
    void noLogin_cantReadPublicDeck() throws Exception {
        String deckId = createDeck(UUID.randomUUID(), true);

        mockMvc.perform(get("/api/v1/decks/{deckId}", deckId))
                .andExpect(status().isUnauthorized());
    }

    // ---- helpers -----------------------------------------------------------

    private String deckJson(String title, boolean isPublic) {
        return "{\"title\": \"" + title + "\", \"description\": \"\", "
                + "\"category\": \"CARDIOLOGY\", \"isPublic\": " + isPublic + "}";
    }

    private String createDeck(UUID user, boolean isPublic) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/decks")
                        .header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deckJson("Access deck", isPublic)))
                .andExpect(status().isCreated())
                .andReturn();

        return idFrom(result);
    }

    private String createCard(UUID user, String deckId, String front) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/decks/{deckId}/cards", deckId)
                        .header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"front\": \"" + front + "\", \"back\": \"Answer\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        return idFrom(result);
    }

    private String idFrom(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
    }
}
