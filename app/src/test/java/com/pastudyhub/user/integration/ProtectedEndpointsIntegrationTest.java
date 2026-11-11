package com.pastudyhub.user.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pastudyhub.BaseIntegrationTest;
import com.pastudyhub.flashcard.dto.CreateDeckRequest;
import com.pastudyhub.flashcard.model.MedicalCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Checks that everything outside /auth needs a valid login token.
 *
 * <p>The controllers still read the user from an X-User-Id header. That is left over
 * from when a gateway checked the token and passed the user ID along. Now a filter
 * fills that header in from the token. These tests make sure a caller can't skip the
 * token or pretend to be someone else by sending the header themselves.
 */
@DisplayName("Protected endpoints")
class ProtectedEndpointsIntegrationTest extends BaseIntegrationTest {

    private static final UUID USER_A = UUID.fromString("aaaaaaaa-1111-1111-1111-111111111111");
    private static final UUID USER_B = UUID.fromString("bbbbbbbb-2222-2222-2222-222222222222");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @ParameterizedTest(name = "GET {0} without a token returns 401")
    @ValueSource(strings = {
            "/api/v1/decks",
            "/api/v1/decks/public",
            "/api/v1/exams/history",
            "/api/v1/progress/dashboard",
            "/api/v1/progress/goal"
    })
    void noToken_returns401(String url) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("X-User-Id header on its own is not a login")
    void userIdHeaderWithoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/decks")
                        .header("X-User-Id", USER_A.toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A made-up token returns 401")
    void invalidToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/decks")
                        .header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Sending someone else's X-User-Id with your own token does not switch users")
    void spoofedUserIdHeader_isIgnored() throws Exception {
        CreateDeckRequest request = CreateDeckRequest.builder()
                .title("Spoof check")
                .category(MedicalCategory.CARDIOLOGY)
                .isPublic(false)
                .build();

        // User A is logged in but claims to be user B in the header
        MvcResult result = mockMvc.perform(post("/api/v1/decks")
                        .header("Authorization", bearerToken(USER_A))
                        .header("X-User-Id", USER_B.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String deckId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();

        // The deck has to belong to A, the user the token is for.
        // So A can open it...
        mockMvc.perform(get("/api/v1/decks/{deckId}", deckId)
                        .header("Authorization", bearerToken(USER_A)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Spoof check"));

        // ...and B is not allowed to change it
        CreateDeckRequest update = CreateDeckRequest.builder()
                .title("Taken over")
                .category(MedicalCategory.CARDIOLOGY)
                .build();

        mockMvc.perform(put("/api/v1/decks/{deckId}", deckId)
                        .header("Authorization", bearerToken(USER_B))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isForbidden());
    }
}
