package com.pastudyhub.user.integration;

import com.pastudyhub.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Without GOOGLE_CLIENT_ID (the test default) Google sign-in is off. */
@DisplayName("Google sign-in turned off")
class GoogleSignInOffIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("The status says off and gives no client ID, and sign-in is refused")
    void off() throws Exception {
        mockMvc.perform(get("/api/v1/auth/google/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.clientId").doesNotExist());

        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"credential\": \"anything\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Google sign-in is not set up on this server."));
    }
}
