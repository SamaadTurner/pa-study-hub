package com.pastudyhub.user.security;

import com.pastudyhub.user.exception.TooManyLoginAttemptsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Login attempt limiter")
class LoginAttemptLimiterTest {

    private static final String EMAIL = "student@example.com";

    private MovableClock clock;
    private MapStore store;
    private LoginAttemptLimiter limiter;

    @BeforeEach
    void setUp() {
        clock = new MovableClock(Instant.parse("2026-11-19T10:00:00Z"));
        store = new MapStore();
        limiter = new LoginAttemptLimiter(store, 5, Duration.ofMinutes(15), clock);
    }

    @Test
    @DisplayName("Four failures are allowed, the fifth one locks the email")
    void fifthFailure_locks() {
        for (int i = 0; i < 4; i++) {
            limiter.recordFailure(EMAIL);
        }
        assertThatCode(() -> limiter.checkAllowed(EMAIL)).doesNotThrowAnyException();

        limiter.recordFailure(EMAIL);
        assertThatThrownBy(() -> limiter.checkAllowed(EMAIL))
                .isInstanceOf(TooManyLoginAttemptsException.class)
                .hasMessage("Too many failed logins. Try again in 15 minutes.");
    }

    @Test
    @DisplayName("The lock ends when the window that started with the first failure is over")
    void lock_endsWithWindow() {
        limiter.recordFailure(EMAIL);
        clock.advance(Duration.ofMinutes(10));
        for (int i = 0; i < 4; i++) {
            limiter.recordFailure(EMAIL);
        }

        // 5 minutes left in the window
        assertThatThrownBy(() -> limiter.checkAllowed(EMAIL))
                .isInstanceOfSatisfying(TooManyLoginAttemptsException.class,
                        ex -> assertThat(ex.getRetryAfterSeconds()).isEqualTo(300))
                .hasMessage("Too many failed logins. Try again in 5 minutes.");

        clock.advance(Duration.ofMinutes(5));
        assertThatCode(() -> limiter.checkAllowed(EMAIL)).doesNotThrowAnyException();

        // A new failure starts a new window with a count of one
        limiter.recordFailure(EMAIL);
        assertThatCode(() -> limiter.checkAllowed(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("A successful login clears the count")
    void success_clearsCount() {
        for (int i = 0; i < 4; i++) {
            limiter.recordFailure(EMAIL);
        }
        limiter.recordSuccess(EMAIL);
        for (int i = 0; i < 4; i++) {
            limiter.recordFailure(EMAIL);
        }
        assertThatCode(() -> limiter.checkAllowed(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Each email has its own count")
    void separateCountsPerEmail() {
        for (int i = 0; i < 5; i++) {
            limiter.recordFailure(EMAIL);
        }
        assertThatCode(() -> limiter.checkAllowed("someone.else@example.com")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Less than a minute left still says 1 minute")
    void lastSeconds_sayOneMinute() {
        for (int i = 0; i < 5; i++) {
            limiter.recordFailure(EMAIL);
        }
        clock.advance(Duration.ofMinutes(14).plusSeconds(50));
        assertThatThrownBy(() -> limiter.checkAllowed(EMAIL))
                .hasMessage("Too many failed logins. Try again in 1 minute.");
    }

    @Test
    @DisplayName("A successful login removes the stored count, and an unknown email stores nothing")
    void success_removesRow() {
        limiter.recordFailure(EMAIL);
        assertThat(store.rows).containsKey(EMAIL);
        limiter.recordSuccess(EMAIL);
        assertThat(store.rows).doesNotContainKey(EMAIL);

        limiter.recordSuccess("never-failed@example.com");
        assertThat(store.rows).isEmpty();
    }

    // The database table, as a map
    private static class MapStore implements LoginFailureStore {
        private final Map<String, LoginFailure> rows = new HashMap<>();

        @Override
        public Optional<LoginFailure> find(String email) {
            return Optional.ofNullable(rows.get(email));
        }

        @Override
        public void save(LoginFailure failure) {
            rows.put(failure.getEmail(), failure);
        }

        @Override
        public void delete(String email) {
            rows.remove(email);
        }

        @Override
        public void deleteEndedBefore(LocalDateTime time) {
            rows.values().removeIf(row -> row.getWindowEnd().isBefore(time));
        }
    }

    // A clock the test can move forward
    private static class MovableClock extends Clock {
        private Instant now;

        MovableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
