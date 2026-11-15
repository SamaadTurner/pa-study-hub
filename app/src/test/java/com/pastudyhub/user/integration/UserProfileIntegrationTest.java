package com.pastudyhub.user.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pastudyhub.BaseIntegrationTest;
import com.pastudyhub.user.integration.TestAccounts.Account;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Profile, password change and closing an account")
class UserProfileIntegrationTest extends BaseIntegrationTest {

    private static final String NEW_PASSWORD = "Diff3rent!Passw0rd";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private ResultActions logIn(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    private ResultActions changePassword(Account account, String current, String next) throws Exception {
        return mockMvc.perform(put("/api/v1/users/me/password")
                .header("Authorization", account.authorization())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"" + current + "\",\"newPassword\":\"" + next + "\"}"));
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"));
    }

    // ==================== PROFILE ====================

    @Test
    @DisplayName("GET /users/me returns the logged-in user's own profile")
    void getProfile_returnsOwnProfile() throws Exception {
        Account account = TestAccounts.register(mockMvc, objectMapper);

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", account.authorization()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(account.email()))
                .andExpect(jsonPath("$.firstName").value("Riley"))
                .andExpect(jsonPath("$.lastName").value("Morgan"))
                // The password hash must never leave the server
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("GET /users/me without a token is a 401")
    void getProfile_withoutToken_isUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /users/me with a token for a user that does not exist is a 404")
    void getProfile_unknownUser_isNotFound() throws Exception {
        // A correctly signed token, but no account behind it (for example a deleted one)
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", bearerToken(UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /users/me changes the fields that were sent and leaves the rest")
    void updateProfile_changesOnlyWhatWasSent() throws Exception {
        Account account = TestAccounts.register(mockMvc, objectMapper);

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", account.authorization())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"  Jamie  \",\"paSchoolName\":\"Example PA Program\",\"graduationYear\":2028}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Jamie"))
                .andExpect(jsonPath("$.paSchoolName").value("Example PA Program"))
                .andExpect(jsonPath("$.graduationYear").value(2028));

        // Read it back: the change was saved and the last name is still the old one
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", account.authorization()))
                .andExpect(jsonPath("$.firstName").value("Jamie"))
                .andExpect(jsonPath("$.lastName").value("Morgan"))
                .andExpect(jsonPath("$.email").value(account.email()));
    }

    @Test
    @DisplayName("PUT /users/me with a graduation year out of range is a 400")
    void updateProfile_badGraduationYear_isBadRequest() throws Exception {
        Account account = TestAccounts.register(mockMvc, objectMapper);

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", account.authorization())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"graduationYear\":1999}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.graduationYear").exists());
    }

    // ==================== PASSWORD ====================

    @Test
    @DisplayName("After a password change the new password logs in and the old one does not")
    void changePassword_newPasswordWorks_oldOneDoesNot() throws Exception {
        Account account = TestAccounts.register(mockMvc, objectMapper);

        changePassword(account, TestAccounts.PASSWORD, NEW_PASSWORD).andExpect(status().isNoContent());

        logIn(account.email(), TestAccounts.PASSWORD).andExpect(status().isUnauthorized());
        logIn(account.email(), NEW_PASSWORD).andExpect(status().isOk());
    }

    @Test
    @DisplayName("A password change with the wrong current password is refused and changes nothing")
    void changePassword_wrongCurrentPassword_isRefused() throws Exception {
        Account account = TestAccounts.register(mockMvc, objectMapper);

        changePassword(account, "Wr0ng!Passw0rd", NEW_PASSWORD).andExpect(status().isUnauthorized());

        logIn(account.email(), TestAccounts.PASSWORD).andExpect(status().isOk());
        logIn(account.email(), NEW_PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A new password that breaks the password rules is a 400")
    void changePassword_weakNewPassword_isBadRequest() throws Exception {
        Account account = TestAccounts.register(mockMvc, objectMapper);

        // Long enough, but no upper case letter, digit or special character
        changePassword(account, TestAccounts.PASSWORD, "alllowercase").andExpect(status().isBadRequest());

        logIn(account.email(), TestAccounts.PASSWORD).andExpect(status().isOk());
    }

    @Test
    @DisplayName("A password change signs out the other devices")
    void changePassword_revokesRefreshTokens() throws Exception {
        // People change a password when they think someone else knows it. A refresh
        // token handed out before the change must not keep that someone logged in.
        Account account = TestAccounts.register(mockMvc, objectMapper);

        changePassword(account, TestAccounts.PASSWORD, NEW_PASSWORD).andExpect(status().isNoContent());

        refresh(account.refreshToken()).andExpect(status().isUnauthorized());
    }

    // ==================== CLOSING THE ACCOUNT ====================

    @Test
    @DisplayName("After DELETE /users/me the account cannot log in or refresh")
    void deactivate_blocksLoginAndRefresh() throws Exception {
        Account account = TestAccounts.register(mockMvc, objectMapper);

        mockMvc.perform(delete("/api/v1/users/me").header("Authorization", account.authorization()))
                .andExpect(status().isNoContent());

        logIn(account.email(), TestAccounts.PASSWORD).andExpect(status().isForbidden());
        refresh(account.refreshToken()).andExpect(status().isForbidden());
    }
}
