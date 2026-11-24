package com.pastudyhub.user.integration;

import tools.jackson.databind.ObjectMapper;
import com.pastudyhub.BaseIntegrationTest;
import com.pastudyhub.user.integration.TestAccounts.Account;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Failed logins are limited per email: 5 in 15 minutes (the defaults). Every test
 * uses a fresh email, so the counts never carry over between tests.
 */
@DisplayName("Login attempt limit")
class LoginLimitIntegrationTest extends BaseIntegrationTest {

    private static final String WRONG = "Wr0ng!Password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("After 5 wrong passwords even the right one is refused with 429 and Retry-After")
    void fiveFailures_lockTheEmail() throws Exception {
        Account account = TestAccounts.register(mockMvc, objectMapper);

        for (int i = 0; i < 5; i++) {
            login(account.email(), WRONG).andExpect(status().isUnauthorized());
        }

        login(account.email(), TestAccounts.PASSWORD)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", hasSecondsWithinWindow()))
                .andExpect(jsonPath("$.detail").value("Too many failed logins. Try again in 15 minutes."));
    }

    @Test
    @DisplayName("The email is matched without case, like the login itself")
    void lock_ignoresCase() throws Exception {
        Account account = TestAccounts.register(mockMvc, objectMapper);
        for (int i = 0; i < 5; i++) {
            login(account.email().toUpperCase(), WRONG).andExpect(status().isUnauthorized());
        }
        login(account.email(), TestAccounts.PASSWORD).andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("A successful login resets the count")
    void success_resetsCount() throws Exception {
        Account account = TestAccounts.register(mockMvc, objectMapper);

        for (int i = 0; i < 4; i++) {
            login(account.email(), WRONG).andExpect(status().isUnauthorized());
        }
        login(account.email(), TestAccounts.PASSWORD).andExpect(status().isOk());
        for (int i = 0; i < 4; i++) {
            login(account.email(), WRONG).andExpect(status().isUnauthorized());
        }
        login(account.email(), TestAccounts.PASSWORD).andExpect(status().isOk());
    }

    @Test
    @DisplayName("An email without an account is locked the same way, so the lock reveals nothing")
    void unknownEmail_isLockedToo() throws Exception {
        String email = "nobody-" + UUID.randomUUID() + "@example.com";
        for (int i = 0; i < 5; i++) {
            login(email, WRONG).andExpect(status().isUnauthorized());
        }
        login(email, WRONG).andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("Locking one email does not affect another")
    void otherEmail_notAffected() throws Exception {
        Account locked = TestAccounts.register(mockMvc, objectMapper);
        Account other = TestAccounts.register(mockMvc, objectMapper);
        for (int i = 0; i < 5; i++) {
            login(locked.email(), WRONG).andExpect(status().isUnauthorized());
        }
        login(other.email(), TestAccounts.PASSWORD).andExpect(status().isOk());
    }

    private ResultActions login(String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("email", email, "password", password));
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    // The header is a whole number of seconds, at most the 15 minute window
    private static Matcher<String> hasSecondsWithinWindow() {
        return new TypeSafeMatcher<>() {
            @Override
            protected boolean matchesSafely(String value) {
                return allOf(greaterThan(0L), lessThanOrEqualTo(900L)).matches(Long.parseLong(value));
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("seconds between 1 and 900");
            }
        };
    }
}
