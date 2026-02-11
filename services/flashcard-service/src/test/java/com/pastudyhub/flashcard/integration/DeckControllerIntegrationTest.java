package com.pastudyhub.flashcard.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pastudyhub.flashcard.dto.CreateDeckRequest;
import com.pastudyhub.flashcard.model.MedicalCategory;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for DeckController.
 *
 * <p>Uses the 'test' Spring profile which configures H2 in-memory database with
 * hibernate create-drop, so tests are isolated without needing Docker/Testcontainers.
 * For full PostgreSQL compatibility tests, run with the 'docker' profile and a live DB.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("DeckController Integration Tests")
class DeckControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final UUID USER_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_B = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static String createdDeckId;

    // ---- POST /api/v1/decks -----------------------------------------------

    @Test
    @Order(1)
    @DisplayName("POST /decks: 201 Created with valid request")
    void createDeck_returns201() throws Exception {
        CreateDeckRequest request = CreateDeckRequest.builder()
                .title("Cardiology Essentials")
                .description("High-yield cardiology for PANCE")
                .category(MedicalCategory.CARDIOLOGY)
                .isPublic(false)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/decks")
                        .header("X-User-Id", USER_A.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.title").value("Cardiology Essentials"))
                .andExpect(jsonPath("$.category").value("CARDIOLOGY"))
                .andExpect(jsonPath("$.cardCount").value(0))
                .andExpect(jsonPath("$.cardsToReview").value(0))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        createdDeckId = objectMapper.readTree(responseBody).get("id").asText();
        assertThat(createdDeckId).isNotBlank();
    }

    @Test
    @Order(2)
    @DisplayName("POST /decks: 400 Bad Request when title is blank")
    void createDeck_blankTitle_returns400() throws Exception {
        CreateDeckRequest request = CreateDeckRequest.builder()
                .title("")
                .category(MedicalCategory.CARDIOLOGY)
                .build();

        mockMvc.perform(post("/api/v1/decks")
                        .header("X-User-Id", USER_A.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists());
    }

    @Test
    @Order(3)
    @DisplayName("POST /decks: 400 Bad Request when category is null")
    void createDeck_nullCategory_returns400() throws Exception {
        CreateDeckRequest request = CreateDeckRequest.builder()
                .title("Valid Title")
                .category(null)
                .build();

        mockMvc.perform(post("/api/v1/decks")
                        .header("X-User-Id", USER_A.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ---- GET /api/v1/decks/{deckId} ----------------------------------------

    @Test
    @Order(4)
    @DisplayName("GET /decks/{deckId}: 200 OK for existing deck")
    void getDeck_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/decks/{deckId}", createdDeckId)
                        .header("X-User-Id", USER_A.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdDeckId))
                .andExpect(jsonPath("$.title").value("Cardiology Essentials"));
    }

    @Test
    @Order(5)
    @DisplayName("GET /decks/{deckId}: 404 Not Found for non-existent deck")
    void getDeck_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/decks/{deckId}", UUID.randomUUID())
                        .header("X-User-Id", USER_A.toString()))
                .andExpect(status().isNotFound());
    }

    // ---- PUT /api/v1/decks/{deckId} ----------------------------------------

    @Test
    @Order(6)
    @DisplayName("PUT /decks/{deckId}: 200 OK when owner updates title")
    void updateDeck_ownerCanUpdate() throws Exception {
        CreateDeckRequest update = CreateDeckRequest.builder()
                .title("Updated Cardiology")
                .build();

        mockMvc.perform(put("/api/v1/decks/{deckId}", createdDeckId)
                        .header("X-User-Id", USER_A.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Cardiology"));
    }

    @Test
    @Order(7)
    @DisplayName("PUT /decks/{deckId}: 403 Forbidden when different user tries to update")
    void updateDeck_nonOwner_returns403() throws Exception {
        CreateDeckRequest update = CreateDeckRequest.builder()
                .title("Hijacked Title")
                .build();

        mockMvc.perform(put("/api/v1/decks/{deckId}", createdDeckId)
                        .header("X-User-Id", USER_B.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isForbidden());
    }

    // ---- DELETE /api/v1/decks/{deckId} -------------------------------------

    @Test
    @Order(8)
    @DisplayName("DELETE /decks/{deckId}: 403 Forbidden when non-owner tries to delete")
    void deleteDeck_nonOwner_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/decks/{deckId}", createdDeckId)
                        .header("X-User-Id", USER_B.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(9)
    @DisplayName("DELETE /decks/{deckId}: 204 No Content for owner")
    void deleteDeck_owner_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/decks/{deckId}", createdDeckId)
                        .header("X-User-Id", USER_A.toString()))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(10)
    @DisplayName("GET /decks/{deckId}: 404 after soft delete")
    void getDeck_afterSoftDelete_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/decks/{deckId}", createdDeckId)
                        .header("X-User-Id", USER_A.toString()))
                .andExpect(status().isNotFound());
    }

    // ---- GET /api/v1/decks/public ------------------------------------------

    @Test
    @Order(11)
    @DisplayName("GET /decks/public: 200 OK — returns page (may be empty)")
    void getPublicDecks_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/decks/public")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
