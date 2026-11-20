package com.pastudyhub.user.integration;

import com.pastudyhub.user.security.RefreshTokenCookie;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pastudyhub.BaseIntegrationTest;
import com.pastudyhub.mail.LoggingMailSender;
import com.pastudyhub.mail.MailSender;
import com.pastudyhub.user.exception.InvalidTokenException;
import com.pastudyhub.user.google.GoogleIdentity;
import com.pastudyhub.user.google.GoogleTokenVerifier;
import com.pastudyhub.user.integration.TestAccounts.Account;
import com.pastudyhub.user.model.User;
import com.pastudyhub.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * "Continue with Google" with a fake token checker: the credential sent is the
 * text "token-for:<sub>:<email>", and the fake turns it into that identity.
 * The checks on real Google tokens are in JwtGoogleTokenVerifierTest.
 */
@DisplayName("Google sign-in")
@TestPropertySource(properties = "google.client-id=test-client.apps.googleusercontent.com")
class GoogleSignInIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private GoogleTokenVerifier googleTokenVerifier;

    // Lets every sign-up through unless a test says otherwise
    @MockBean
    private com.pastudyhub.user.security.SignupLimiter signupLimiter;

    // Off-like by default (lets everything through, no site key)
    @MockBean
    private com.pastudyhub.user.botcheck.BotCheck botCheck;

    @Autowired
    private MailSender mailSender;

    @Test
    @DisplayName("The status endpoint says Google sign-in is on and gives the client ID")
    void status_enabled() throws Exception {
        mockMvc.perform(get("/api/v1/auth/google/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.clientId").value("test-client.apps.googleusercontent.com"));
    }

    @Test
    @DisplayName("The first Google sign-in makes an account without a password, the next one finds it")
    void firstSignIn_makesAccount() throws Exception {
        String sub = "sub-" + UUID.randomUUID();
        String email = "new-" + UUID.randomUUID() + "@gmail.com";
        fakeGoogle(sub, email, "Riley", "Morgan");

        MvcResult first = googleSignIn(sub, email)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(cookie().httpOnly(RefreshTokenCookie.NAME, true))
                .andExpect(cookie().exists(RefreshTokenCookie.NAME))
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.firstName").value("Riley"))
                .andReturn();
        JsonNode body = objectMapper.readTree(first.getResponse().getContentAsString());

        assertThat(userRepository.findByEmail(email)).get()
                .satisfies(user -> {
                    assertThat(user.getPasswordHash()).isNull();
                    assertThat(user.getGoogleSubject()).isEqualTo(sub);
                });

        // The app's own tokens work like after a normal login
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + body.get("accessToken").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        googleSignIn(sub, email)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value(body.get("user").get("id").asText()));
    }

    @Test
    @DisplayName("Missing names from Google get a placeholder first name")
    void missingNames() throws Exception {
        String sub = "sub-" + UUID.randomUUID();
        String email = "noname-" + UUID.randomUUID() + "@gmail.com";
        fakeGoogle(sub, email, null, null);

        googleSignIn(sub, email)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.firstName").value("Student"));
    }

    @Test
    @DisplayName("An account made with Google cannot log in with a password")
    void googleAccount_noPasswordLogin() throws Exception {
        String sub = "sub-" + UUID.randomUUID();
        String email = "gonly-" + UUID.randomUUID() + "@gmail.com";
        fakeGoogle(sub, email, "Sam", "Lee");
        googleSignIn(sub, email).andExpect(status().isOk());

        login(email, TestAccounts.PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A password account with the same email is not taken over: linking needs its password")
    void existingEmail_needsPassword() throws Exception {
        Account account = TestAccounts.register(mockMvc, objectMapper);
        String sub = "sub-" + UUID.randomUUID();
        fakeGoogle(sub, account.email(), "Riley", "Morgan");

        googleSignIn(sub, account.email())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "An account with this email already exists. Enter its password once to add Google sign-in."));

        link(sub, account.email(), "Wr0ng!Password").andExpect(status().isUnauthorized());
        assertThat(userRepository.findByEmail(account.email()).get().getGoogleSubject()).isNull();

        link(sub, account.email(), TestAccounts.PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value(account.email()));

        // From now on Google signs in directly, and the password still works too
        googleSignIn(sub, account.email()).andExpect(status().isOk());
        login(account.email(), TestAccounts.PASSWORD).andExpect(status().isOk());
    }

    @Test
    @DisplayName("An account whose email is confirmed is linked to Google without asking for its password")
    void confirmedEmail_linksDirectly() throws Exception {
        Account account = TestAccounts.register(mockMvc, objectMapper);
        // Confirm the email with the link from the sign-up email
        String text = ((LoggingMailSender) mailSender).sentTo(account.email()).get(0).text();
        String marker = "/confirm-email?token=";
        int start = text.indexOf(marker) + marker.length();
        String token = text.substring(start, text.indexOf('\n', start)).trim();
        mockMvc.perform(post("/api/v1/auth/confirm-email").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", token))))
                .andExpect(status().isNoContent());

        String sub = "sub-" + UUID.randomUUID();
        fakeGoogle(sub, account.email(), "Riley", "Morgan");
        googleSignIn(sub, account.email())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value(account.email()))
                .andExpect(jsonPath("$.user.googleLinked").value(true));
        login(account.email(), TestAccounts.PASSWORD).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Wrong passwords when linking count toward the login lock")
    void link_countsTowardLock() throws Exception {
        Account account = TestAccounts.register(mockMvc, objectMapper);
        String sub = "sub-" + UUID.randomUUID();
        fakeGoogle(sub, account.email(), "Riley", "Morgan");

        for (int i = 0; i < 5; i++) {
            link(sub, account.email(), "Wr0ng!Password").andExpect(status().isUnauthorized());
        }
        link(sub, account.email(), TestAccounts.PASSWORD).andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("An account linked to one Google account cannot be linked to another")
    void link_otherGoogleAccount_refused() throws Exception {
        Account account = TestAccounts.register(mockMvc, objectMapper);
        String firstSub = "sub-" + UUID.randomUUID();
        String secondSub = "sub-" + UUID.randomUUID();
        fakeGoogle(firstSub, account.email(), "Riley", "Morgan");
        fakeGoogle(secondSub, account.email(), "Riley", "Morgan");

        link(firstSub, account.email(), TestAccounts.PASSWORD).andExpect(status().isOk());
        link(secondSub, account.email(), TestAccounts.PASSWORD)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("This account is already linked to a different Google account."));
    }

    @Test
    @DisplayName("An invalid Google token gets 401")
    void invalidToken() throws Exception {
        when(googleTokenVerifier.verify("broken"))
                .thenThrow(new InvalidTokenException("Google sign-in could not be checked"));

        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"credential\": \"broken\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A deactivated account cannot sign in with Google either")
    void deactivated_refused() throws Exception {
        String sub = "sub-" + UUID.randomUUID();
        String email = "closed-" + UUID.randomUUID() + "@gmail.com";
        fakeGoogle(sub, email, "Sam", "Lee");
        googleSignIn(sub, email).andExpect(status().isOk());

        User user = userRepository.findByEmail(email).orElseThrow();
        user.setActive(false);
        userRepository.save(user);

        googleSignIn(sub, email).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A Google-only account closes by typing its email, and a later Google sign-in starts fresh")
    void googleOnly_closeAndStartAgain() throws Exception {
        String sub = "sub-" + UUID.randomUUID();
        String email = "fresh-" + UUID.randomUUID() + "@gmail.com";
        fakeGoogle(sub, email, "Sam", "Lee");
        JsonNode first = objectMapper.readTree(googleSignIn(sub, email).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        String authorization = "Bearer " + first.get("accessToken").asText();

        mockMvc.perform(delete("/api/v1/users/me").header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmEmail\": \"someone-else@gmail.com\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(delete("/api/v1/users/me").header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmEmail\": \" " + email.toUpperCase() + " \"}"))
                .andExpect(status().isNoContent());

        googleSignIn(sub, email)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value(org.hamcrest.Matchers.not(first.get("user").get("id").asText())));
    }

    @Test
    @DisplayName("When sign-up is paused, new accounts get a 429 with a clear message; existing ones still sign in")
    void signupPaused() throws Exception {
        String sub = "paused-" + java.util.UUID.randomUUID();
        String existingEmail = sub + "-old@example.com";
        fakeGoogle(sub, existingEmail, "Old", "User");
        googleSignIn(sub, existingEmail).andExpect(status().isOk());

        org.mockito.Mockito.doThrow(new com.pastudyhub.user.exception.TooManySignupsException())
                .when(signupLimiter).checkRoomForOneMore();
        try {
            String newSub = sub + "-new";
            String newEmail = newSub + "@example.com";
            fakeGoogle(newSub, newEmail, "New", "User");
            googleSignIn(newSub, newEmail)
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.startsWith("A lot of accounts were made recently")));
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("email", "paused-" + sub + "@example.com",
                                    "password", "Str0ng!Passw0rd", "firstName", "A", "lastName", "B"))))
                    .andExpect(status().isTooManyRequests());
            // Someone who already has an account is not affected
            googleSignIn(sub, existingEmail).andExpect(status().isOk());
        } finally {
            org.mockito.Mockito.reset(signupLimiter);
        }
    }

    @Test
    @DisplayName("With the bot check on, sign-up needs a token Cloudflare accepts; the form gets the site key")
    void botCheckOnSignUp() throws Exception {
        when(botCheck.siteKey()).thenReturn("site-key-123");
        org.mockito.Mockito.doThrow(new com.pastudyhub.user.exception.BotCheckFailedException())
                .when(botCheck).verify(org.mockito.ArgumentMatchers.argThat(token -> !"good-token".equals(token)));
        try {
            mockMvc.perform(get("/api/v1/auth/bot-check"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.turnstileSiteKey").value("site-key-123"));

            String email = "bot-check-" + java.util.UUID.randomUUID() + "@example.com";
            Map<String, Object> form = new java.util.HashMap<>(Map.of("email", email, "password", "Str0ng!Passw0rd",
                    "firstName", "A", "lastName", "B"));
            mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(form)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("Please complete the check that you're not a robot, then try again."));
            form.put("botCheckToken", "good-token");
            mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(form)))
                    .andExpect(status().isCreated());
        } finally {
            org.mockito.Mockito.reset(botCheck);
        }
    }

    // ---- helpers -------------------------------------------------------------

    private void fakeGoogle(String sub, String email, String firstName, String lastName) {
        when(googleTokenVerifier.verify("token-for:" + sub + ":" + email))
                .thenReturn(new GoogleIdentity(sub, email, firstName, lastName));
    }

    private ResultActions googleSignIn(String sub, String email) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("credential", "token-for:" + sub + ":" + email))));
    }

    private ResultActions link(String sub, String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/google/link")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("credential", "token-for:" + sub + ":" + email, "password", password))));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))));
    }
}
