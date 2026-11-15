package com.pastudyhub.user.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pastudyhub.BaseIntegrationTest;
import com.pastudyhub.user.dev.DemoAccountLoader;
import com.pastudyhub.user.dto.LoginRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The demo account must only exist with the "dev" profile. These tests start the
 * app without it, the way a real deployment would.
 */
@DisplayName("Without the dev profile there is no demo account")
class NoDemoAccountIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("The class that creates the account is not loaded")
    void loaderIsNotLoaded() {
        String[] loaders = applicationContext.getBeanNamesForType(DemoAccountLoader.class);

        assertThat(loaders).isEmpty();
    }

    @Test
    @DisplayName("Logging in with the demo email and password is refused")
    void demoLoginIsRefused() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail(DemoAccountLoader.EMAIL);
        request.setPassword(DemoAccountLoader.PASSWORD);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
