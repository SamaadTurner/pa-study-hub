package com.pastudyhub.ai;

import com.pastudyhub.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The tests and CI have no AI key, which is the same as a server where the
 * feature was never turned on.
 */
@DisplayName("AI endpoints with no API key set")
class AiTurnedOffIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("The status says AI is off")
    void statusIsOff() throws Exception {
        mockMvc.perform(get("/api/v1/ai/status").header("Authorization", bearerToken(UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.model").doesNotExist());
    }

    @Test
    @DisplayName("Asking for flashcards answers 503 with a clear message")
    void generateIsUnavailable() throws Exception {
        mockMvc.perform(post("/api/v1/ai/flashcards")
                        .header("Authorization", bearerToken(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\": \"Some notes\", \"count\": 3}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("AI features are turned off on this server."));
    }

    @Test
    @DisplayName("The AI endpoints need a login")
    void needsLogin() throws Exception {
        mockMvc.perform(get("/api/v1/ai/status"))
                .andExpect(status().isUnauthorized());
    }
}
