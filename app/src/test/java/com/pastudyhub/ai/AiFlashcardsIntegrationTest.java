package com.pastudyhub.ai;

import com.pastudyhub.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The flashcard endpoint with the real controller and generator, and a fake model
 * in place of the provider.
 */
@DisplayName("AI flashcard endpoint with a fake model")
class AiFlashcardsIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatClient chatClient;

    @BeforeEach
    void turnOn() {
        when(chatClient.isEnabled()).thenReturn(true);
        when(chatClient.model()).thenReturn("fake-model");
    }

    private ResultActions generate(String body) throws Exception {
        return mockMvc.perform(post("/api/v1/ai/flashcards")
                .header("Authorization", bearerToken(UUID.randomUUID()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    @DisplayName("The status names the model when AI is on")
    void statusIsOn() throws Exception {
        mockMvc.perform(get("/api/v1/ai/status").header("Authorization", bearerToken(UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.model").value("fake-model"));
    }

    @Test
    @DisplayName("Notes in, suggested cards out")
    void returnsSuggestedCards() throws Exception {
        when(chatClient.complete(anyString(), contains("Write 2 flashcards")))
                .thenReturn("{\"cards\": [{\"front\": \"Q1\", \"back\": \"A1\"}, {\"front\": \"Q2\", \"back\": \"A2\"}]}");

        generate("{\"notes\": \"Mitral stenosis: diastolic rumble at the apex.\", \"count\": 2}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cards", hasSize(2)))
                .andExpect(jsonPath("$.cards[0].front").value("Q1"))
                .andExpect(jsonPath("$.cards[0].back").value("A1"));
    }

    @Test
    @DisplayName("The count defaults to 5")
    void defaultCount() throws Exception {
        when(chatClient.complete(anyString(), contains("Write 5 flashcards")))
                .thenReturn("{\"cards\": [{\"front\": \"Q\", \"back\": \"A\"}]}");

        generate("{\"notes\": \"Some notes\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cards", hasSize(1)));
    }

    @Test
    @DisplayName("Blank notes, too many cards or too much text are a 400, and the model is not called")
    void rejectsBadInput() throws Exception {
        generate("{\"notes\": \"   \", \"count\": 3}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.notes").exists());

        generate("{\"notes\": \"Some notes\", \"count\": 11}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.count").exists());

        generate("{\"notes\": \"" + "x".repeat(6001) + "\", \"count\": 3}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.notes").exists());

        verify(chatClient, never()).complete(anyString(), anyString());
    }

    @Test
    @DisplayName("An answer the app can't use is a 502")
    void unusableAnswer() throws Exception {
        when(chatClient.complete(anyString(), anyString())).thenReturn("I'd rather not.");

        generate("{\"notes\": \"Some notes\", \"count\": 3}")
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value("The AI answer was not in the expected format."));
    }

    @Test
    @DisplayName("A busy provider is a 503 with its message")
    void busyProvider() throws Exception {
        when(chatClient.complete(anyString(), anyString()))
                .thenThrow(new AiUnavailableException("The AI service is busy right now (free usage limit reached). Try again in a minute."));

        generate("{\"notes\": \"Some notes\", \"count\": 3}")
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("busy")));
    }
}
