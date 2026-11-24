package com.pastudyhub.user.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pastudyhub.BaseIntegrationTest;
import com.pastudyhub.user.google.GoogleIdentity;
import com.pastudyhub.user.google.GoogleTokenVerifier;
import com.pastudyhub.user.integration.TestAccounts.Account;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The sign-in methods on the profile: a password for an account made with Google,
 * and adding or removing Google. Same fake token checker as GoogleSignInIntegrationTest.
 */
@DisplayName("Sign-in methods on the profile")
@TestPropertySource(properties = "google.client-id=test-client.apps.googleusercontent.com")
class ProfileSignInMethodsIntegrationTest extends BaseIntegrationTest {

    private static final String NEW_PASSWORD = "Diff3rent!Passw0rd";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GoogleTokenVerifier googleTokenVerifier;

    @Test
    @DisplayName("The profile says which sign-in methods an account has")
    void profile_showsSignInMethods() throws Exception {
        Account passwordAccount = TestAccounts.register(mockMvc, objectMapper);
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", passwordAccount.authorization()))
                .andExpect(jsonPath("$.hasPassword").value(true))
                .andExpect(jsonPath("$.googleLinked").value(false));

        String googleAuth = googleOnlyAccount();
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", googleAuth))
                .andExpect(jsonPath("$.hasPassword").value(false))
                .andExpect(jsonPath("$.googleLinked").value(true));
    }

    @Test
    @DisplayName("An account made with Google can set a password without a current one, and then log in with it")
    void googleOnly_setsPassword() throws Exception {
        String sub = "sub-" + UUID.randomUUID();
        String email = "g-" + UUID.randomUUID() + "@gmail.com";
        String auth = googleOnlyAccount(sub, email);

        mockMvc.perform(put("/api/v1/users/me/password").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("newPassword", NEW_PASSWORD))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", NEW_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.hasPassword").value(true));
    }

    @Test
    @DisplayName("A password account can add Google from the profile, and Google then signs in directly")
    void passwordAccount_addsGoogle() throws Exception {
        Account account = TestAccounts.register(mockMvc, objectMapper);
        String sub = "sub-" + UUID.randomUUID();
        // The Google account's email does not have to match
        fakeGoogle(sub, "other-" + UUID.randomUUID() + "@gmail.com");

        linkFromProfile(account.authorization(), sub)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.googleLinked").value(true));

        mockMvc.perform(post("/api/v1/auth/google").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("credential", "token-for:" + sub))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value(account.email()));
    }

    @Test
    @DisplayName("A Google account already used by someone else can't be added")
    void addGoogle_usedElsewhere_refused() throws Exception {
        String sub = "sub-" + UUID.randomUUID();
        googleOnlyAccount(sub, "taken-" + UUID.randomUUID() + "@gmail.com");
        Account account = TestAccounts.register(mockMvc, objectMapper);

        linkFromProfile(account.authorization(), sub)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("This Google account is already used by another PA Study Hub account."));
    }

    @Test
    @DisplayName("Google can be removed when the account has a password, never when it is the only way in")
    void removeGoogle() throws Exception {
        String googleOnly = googleOnlyAccount();
        mockMvc.perform(delete("/api/v1/users/me/google").header("Authorization", googleOnly))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "Set a password before removing Google, or you would have no way to sign in."));

        Account account = TestAccounts.register(mockMvc, objectMapper);
        String sub = "sub-" + UUID.randomUUID();
        fakeGoogle(sub, account.email());
        linkFromProfile(account.authorization(), sub).andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/users/me/google").header("Authorization", account.authorization()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.googleLinked").value(false));
    }

    // ---- helpers -------------------------------------------------------------

    private void fakeGoogle(String sub, String email) {
        when(googleTokenVerifier.verify("token-for:" + sub)).thenReturn(new GoogleIdentity(sub, email, "Sam", "Lee"));
    }

    private String googleOnlyAccount() throws Exception {
        return googleOnlyAccount("sub-" + UUID.randomUUID(), "g-" + UUID.randomUUID() + "@gmail.com");
    }

    // Makes an account through Google sign-in and returns its Authorization header
    private String googleOnlyAccount(String sub, String email) throws Exception {
        fakeGoogle(sub, email);
        String body = mockMvc.perform(post("/api/v1/auth/google").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("credential", "token-for:" + sub))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        return "Bearer " + json.get("accessToken").asText();
    }

    private ResultActions linkFromProfile(String authorization, String sub) throws Exception {
        return mockMvc.perform(post("/api/v1/users/me/google").header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("credential", "token-for:" + sub))));
    }
}
