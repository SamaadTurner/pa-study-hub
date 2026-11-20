package com.pastudyhub.user.integration;

import com.pastudyhub.user.security.RefreshTokenCookie;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pastudyhub.BaseIntegrationTest;
import com.pastudyhub.user.integration.TestAccounts.Account;
import com.pastudyhub.user.model.RefreshToken;
import com.pastudyhub.user.repository.RefreshTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The frontend uses POST /auth/refresh to get a new access token when the old one
 * runs out after 15 minutes. A refresh token works once: using it hands back a new one.
 */
@DisplayName("Refresh tokens and logout")
class TokenRefreshIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private ResultActions refresh(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh")
                .cookie(new jakarta.servlet.http.Cookie(RefreshTokenCookie.NAME, refreshToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"));
    }

    /** Refreshes and returns the new refresh token. */
    private String refreshAndGetNewToken(String refreshToken) throws Exception {
        return TestAccounts.refreshCookie(refresh(refreshToken).andExpect(status().isOk()).andReturn());
    }

    @Test
    @DisplayName("A refresh token gives a new access token that works, and a new refresh token")
    void refresh_returnsNewTokens() throws Exception {
        Account account = TestAccounts.register(mockMvc, objectMapper);

        MvcResult result = refresh(account.refreshToken())
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());

        assertThat(body.has("refreshToken")).isFalse();
        assertThat(TestAccounts.refreshCookie(result)).isNotEqualTo(account.refreshToken());
        assertThat(body.get("user").get("email").asText()).isEqualTo(account.email());

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + body.get("accessToken").asText()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("A refresh token cannot be used a second time")
    void refresh_sameTokenTwice_isRefused() throws Exception {
        Account account = TestAccounts.register(mockMvc, objectMapper);

        refresh(account.refreshToken()).andExpect(status().isOk());

        refresh(account.refreshToken()).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Reusing an old refresh token also shuts out the newer ones")
    void refresh_reusedToken_revokesEveryTokenOfTheUser() throws Exception {
        // If an old token shows up again, someone may have copied it. We can't tell
        // which side is the real user, so every token for the account stops working
        // and the user has to log in again.
        Account account = TestAccounts.register(mockMvc, objectMapper);
        String newerToken = refreshAndGetNewToken(account.refreshToken());

        refresh(account.refreshToken()).andExpect(status().isUnauthorized());

        refresh(newerToken).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A token the server never issued is refused")
    void refresh_unknownToken_isRefused() throws Exception {
        refresh("00000000-0000-0000-0000-000000000000").andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("An expired refresh token is refused")
    void refresh_expiredToken_isRefused() throws Exception {
        Account account = TestAccounts.register(mockMvc, objectMapper);

        // Tokens last 7 days, so move this one's end date into the past
        RefreshToken stored = refreshTokenRepository.findByToken(account.refreshToken()).orElseThrow();
        stored.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        refreshTokenRepository.save(stored);

        refresh(account.refreshToken()).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("An empty refresh token cookie is a 401 (not signed in)")
    void refresh_blankToken_isUnauthorized() throws Exception {
        refresh("").andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("After logout the refresh token no longer works")
    void logout_revokesTheToken() throws Exception {
        Account account = TestAccounts.register(mockMvc, objectMapper);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(new jakarta.servlet.http.Cookie(RefreshTokenCookie.NAME, account.refreshToken())))
                .andExpect(status().isNoContent())
                // The browser is told to delete the cookie
                .andExpect(cookie().maxAge(RefreshTokenCookie.NAME, 0));

        refresh(account.refreshToken()).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A tab opened before the cookie existed can still refresh once with the token in the body")
    void refresh_withTheOldBodyToken() throws Exception {
        Account account = TestAccounts.register(mockMvc, objectMapper);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + account.refreshToken() + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        // From then on it has the cookie
        refresh(TestAccounts.refreshCookie(result)).andExpect(status().isOk());
        // Logout with the body token works too
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + account.refreshToken() + "\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Refresh with no cookie and no body is a 401; logout without anything still clears the cookie")
    void noToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge(RefreshTokenCookie.NAME, 0));
    }

    @Test
    @DisplayName("Logout with an unknown token still answers 204")
    void logout_unknownToken_isNotAnError() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"00000000-0000-0000-0000-000000000000\"}"))
                .andExpect(status().isNoContent());
    }
}
