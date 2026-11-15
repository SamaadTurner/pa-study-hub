package com.pastudyhub.user.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pastudyhub.user.dto.RegisterRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Registers real accounts through the API for tests that need a logged-in user
 * with a row in the database (a token alone is not enough for the profile endpoints).
 */
final class TestAccounts {

    static final String PASSWORD = "Str0ng!Passw0rd";

    private TestAccounts() {
    }

    /** What a test needs to know about the account it just registered. */
    record Account(String email, String accessToken, String refreshToken) {

        String authorization() {
            return "Bearer " + accessToken;
        }
    }

    /** Every call makes a new account, so tests never share one. */
    static Account register(MockMvc mockMvc, ObjectMapper objectMapper) throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";

        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setPassword(PASSWORD);
        request.setFirstName("Riley");
        request.setLastName("Morgan");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return new Account(email, body.get("accessToken").asText(), body.get("refreshToken").asText());
    }
}
