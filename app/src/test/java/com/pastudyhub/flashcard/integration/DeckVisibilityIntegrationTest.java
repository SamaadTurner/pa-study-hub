package com.pastudyhub.flashcard.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pastudyhub.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Checks the deck JSON the way the browser sends and reads it.
 *
 * <p>The other deck tests build the request from the Java class, so the same class
 * writes the JSON and reads it back, and a wrong field name goes unnoticed. That is
 * how the "isPublic" checkbox ended up being ignored: the server only understood
 * "public". These tests send the JSON as plain text, exactly like the frontend does.
 */
@DisplayName("Deck JSON and public decks")
class DeckVisibilityIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("isPublic in the request is saved and comes back as isPublic")
    void createDeck_readsAndWritesIsPublic() throws Exception {
        UUID user = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/decks")
                        .header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deckJson("Shared cardio deck", true)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isPublic").value(true))
                // the old, wrong name must not be there as well
                .andExpect(jsonPath("$.public").doesNotExist());
    }

    @Test
    @DisplayName("A deck is private when isPublic is left out")
    void createDeck_defaultsToPrivate() throws Exception {
        UUID user = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/decks")
                        .header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"My own deck\", \"category\": \"CARDIOLOGY\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isPublic").value(false));
    }

    @Test
    @DisplayName("A public deck shows up in the public list, a private one does not")
    void publicList_onlyHasPublicDecks() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();
        String publicDeck = createDeck(owner, "Public deck", true);
        String privateDeck = createDeck(owner, "Private deck", false);

        mockMvc.perform(get("/api/v1/decks/public")
                        .header("Authorization", bearerToken(otherUser))
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasItem(publicDeck)))
                .andExpect(jsonPath("$.content[*].id", not(hasItem(privateDeck))));
    }

    @Test
    @DisplayName("Editing a deck can make it public and private again")
    void updateDeck_changesVisibility() throws Exception {
        UUID owner = UUID.randomUUID();
        String deckId = createDeck(owner, "Starts private", false);

        mockMvc.perform(put("/api/v1/decks/{deckId}", deckId)
                        .header("Authorization", bearerToken(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deckJson("Now public", true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Now public"))
                .andExpect(jsonPath("$.isPublic").value(true));

        mockMvc.perform(put("/api/v1/decks/{deckId}", deckId)
                        .header("Authorization", bearerToken(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deckJson("Private again", false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPublic").value(false));

        mockMvc.perform(get("/api/v1/decks/{deckId}", deckId)
                        .header("Authorization", bearerToken(owner)))
                .andExpect(jsonPath("$.isPublic").value(false));
    }

    @Test
    @DisplayName("A new deck and a new card come back with their created date filled in")
    void createResponses_haveDates() throws Exception {
        UUID user = UUID.randomUUID();

        MvcResult deck = mockMvc.perform(post("/api/v1/decks")
                        .header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deckJson("Dates deck", false)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdAt").isString())
                .andExpect(jsonPath("$.updatedAt").isString())
                .andReturn();

        mockMvc.perform(post("/api/v1/decks/{deckId}/cards", idFrom(deck))
                        .header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"front\": \"Question\", \"back\": \"Answer\", \"tags\": [\"cardio\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdAt").isString());
    }

    // ---- helpers -----------------------------------------------------------

    // The same fields the deck form in the frontend sends
    private String deckJson(String title, boolean isPublic) {
        return "{\"title\": \"" + title + "\", \"description\": \"\", "
                + "\"category\": \"CARDIOLOGY\", \"isPublic\": " + isPublic + "}";
    }

    private String createDeck(UUID user, String title, boolean isPublic) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/decks")
                        .header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deckJson(title, isPublic)))
                .andExpect(status().isCreated())
                .andReturn();

        return idFrom(result);
    }

    private String idFrom(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
    }
}
