package com.pastudyhub.user.integration;

import com.pastudyhub.user.security.RefreshTokenCookie;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pastudyhub.BaseIntegrationTest;
import com.pastudyhub.mail.LoggingMailSender;
import com.pastudyhub.mail.MailSender;
import com.pastudyhub.user.integration.TestAccounts.Account;
import com.pastudyhub.user.model.User;
import com.pastudyhub.user.repository.EmailTokenRepository;
import com.pastudyhub.user.repository.UserRepository;
import com.pastudyhub.user.service.ClosedAccountCleanup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Email confirmation and "forgot password". The tests run without a Brevo key, so
 * emails go to the LoggingMailSender, and the tests read the links from there.
 */
@DisplayName("Links sent by email")
class EmailLinksIntegrationTest extends BaseIntegrationTest {

    private static final String NEW_PASSWORD = "N3w!Password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MailSender mailSender;

    @Autowired
    private EmailTokenRepository emailTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ClosedAccountCleanup closedAccountCleanup;

    @Test
    @DisplayName("Signing up sends a confirmation link; the link confirms the email once")
    void signUp_sendsConfirmation() throws Exception {
        Account account = TestAccounts.register(mockMvc, objectMapper);
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", account.authorization()))
                .andExpect(jsonPath("$.emailVerified").value(false));

        List<LoggingMailSender.SentMail> mails = outbox().sentTo(account.email());
        assertThat(mails).hasSize(1);
        assertThat(mails.get(0).subject()).isEqualTo("Confirm your email for PA Study Hub");
        String token = tokenFrom(mails.get(0), "/confirm-email?token=");

        confirm(token).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", account.authorization()))
                .andExpect(jsonPath("$.emailVerified").value(true));
        confirm(token).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Forgot password: a reset link sets a new password, signs in and signs out the old sessions")
    void reset_setsNewPassword() throws Exception {
        Account account = TestAccounts.register(mockMvc, objectMapper);
        String token = requestResetAndGetToken(account.email());

        // A weak password is refused and does not use up the link
        reset(token, "weak").andExpect(status().isBadRequest());
        reset(token, NEW_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.emailVerified").value(true));

        login(account.email(), TestAccounts.PASSWORD).andExpect(status().isUnauthorized());
        login(account.email(), NEW_PASSWORD).andExpect(status().isOk());
        refresh(account.refreshToken()).andExpect(status().isUnauthorized());
        reset(token, "An0ther!Password").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("This reset link is invalid, used or expired. Ask for a new one."));
    }

    @Test
    @DisplayName("The reset form answers the same for an email without an account, and sends nothing")
    void reset_unknownEmail_sameAnswer() throws Exception {
        String email = "nobody-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/api/v1/auth/password-reset/request").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("If an account uses that email, a reset link is on its way. "
                        + "It works for one hour. Check your spam folder too."));
        assertThat(outbox().sentTo(email)).isEmpty();
    }

    @Test
    @DisplayName("A reset unlocks a login that was locked by wrong passwords")
    void reset_clearsLoginLock() throws Exception {
        Account account = TestAccounts.register(mockMvc, objectMapper);
        for (int i = 0; i < 5; i++) {
            login(account.email(), "Wr0ng!Password").andExpect(status().isUnauthorized());
        }
        // The count is in the database, so it would survive a restart
        assertThat(jdbcTemplate.queryForObject("SELECT failure_count FROM login_failures WHERE email = ?",
                Integer.class, account.email())).isEqualTo(5);
        login(account.email(), TestAccounts.PASSWORD).andExpect(status().isTooManyRequests());

        reset(requestResetAndGetToken(account.email()), NEW_PASSWORD).andExpect(status().isOk());
        login(account.email(), NEW_PASSWORD).andExpect(status().isOk());
    }

    @Test
    @DisplayName("A new link makes the older one stop working, an expired one fails, and only 3 an hour are sent")
    void links_retiredExpiredAndLimited() throws Exception {
        Account account = TestAccounts.register(mockMvc, objectMapper);
        String first = requestResetAndGetToken(account.email());
        String second = requestResetAndGetToken(account.email());
        reset(first, NEW_PASSWORD).andExpect(status().isBadRequest());

        // Third request, then a fourth that sends nothing (3 an hour)
        String third = requestResetAndGetToken(account.email());
        requestReset(account.email());
        assertThat(outbox().sentTo(account.email()).stream()
                .filter(mail -> mail.subject().startsWith("Reset")).count()).isEqualTo(3);

        // Make the newest link expired and try it
        UUID userId = userRepository.findByEmail(account.email()).orElseThrow().getId();
        jdbcTemplate.update("UPDATE email_tokens SET expires_at = ? WHERE user_id = ? AND used_at IS NULL",
                java.sql.Timestamp.valueOf(LocalDateTime.now().minusMinutes(1)), userId);
        reset(third, NEW_PASSWORD).andExpect(status().isBadRequest());
        reset(second, NEW_PASSWORD).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("The confirmation can be sent again, 3 times an hour, and not at all once confirmed")
    void resendConfirmation() throws Exception {
        Account account = TestAccounts.register(mockMvc, objectMapper);
        // Sign-up already sent one; two more are allowed this hour
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/v1/users/me/confirmation-email").header("Authorization", account.authorization()))
                    .andExpect(status().isAccepted());
        }
        mockMvc.perform(post("/api/v1/users/me/confirmation-email").header("Authorization", account.authorization()))
                .andExpect(status().isTooManyRequests());
        assertThat(outbox().sentTo(account.email())).hasSize(3);

        confirm(tokenFrom(outbox().sentTo(account.email()).get(0), "/confirm-email?token="))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/users/me/confirmation-email").header("Authorization", account.authorization()))
                .andExpect(status().isAccepted());
        assertThat(outbox().sentTo(account.email())).hasSize(3);
    }

    @Test
    @DisplayName("At startup, accounts closed the old way (only switched off) are deleted")
    void cleanup_deletesOldClosedAccounts() throws Exception {
        Account closed = TestAccounts.register(mockMvc, objectMapper);
        Account open = TestAccounts.register(mockMvc, objectMapper);
        User user = userRepository.findByEmail(closed.email()).orElseThrow();
        user.setActive(false);
        userRepository.save(user);

        closedAccountCleanup.run(null);

        assertThat(userRepository.findByEmail(closed.email())).isEmpty();
        assertThat(userRepository.findByEmail(open.email())).isPresent();
        assertThat(emailTokenRepository.findAll().stream()
                .anyMatch(token -> token.getUser().getId().equals(user.getId()))).isFalse();
    }

    // ---- helpers -------------------------------------------------------------

    private LoggingMailSender outbox() {
        return (LoggingMailSender) mailSender;
    }

    private String tokenFrom(LoggingMailSender.SentMail mail, String marker) {
        String text = mail.text();
        int start = text.indexOf(marker) + marker.length();
        int end = text.indexOf('\n', start);
        return text.substring(start, end).trim();
    }

    private ResultActions requestReset(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/password-reset/request").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email.toUpperCase()))))
                .andExpect(status().isAccepted());
    }

    private String requestResetAndGetToken(String email) throws Exception {
        requestReset(email);
        LoggingMailSender.SentMail newest = outbox().sentTo(email).get(0);
        assertThat(newest.subject()).isEqualTo("Reset your PA Study Hub password");
        return tokenFrom(newest, "/reset-password?token=");
    }

    private ResultActions reset(String token, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/password-reset/confirm").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("token", token, "newPassword", password))));
    }

    private ResultActions confirm(String token) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/confirm-email").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("token", token))));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))));
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh")
                .cookie(new jakarta.servlet.http.Cookie(RefreshTokenCookie.NAME, refreshToken)));
    }
}
