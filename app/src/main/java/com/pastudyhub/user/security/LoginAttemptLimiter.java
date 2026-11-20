package com.pastudyhub.user.security;

import com.pastudyhub.user.exception.TooManyLoginAttemptsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Slows down password guessing: after too many failed logins for one email, that
 * email cannot log in until the time window is over. A successful login, or a
 * password reset by email, clears the count.
 *
 * <p>The count is per email and not per client IP address. Behind Render the app
 * does not reliably see the visitor's address (the static site forwards /api
 * requests, and X-Forwarded-For can be set by the client), so an IP limit would
 * either be easy to dodge or would block everybody at once. The cost of a per-email
 * limit is that someone who knows your email can lock you out for one window; the
 * "forgot password" email unlocks it.
 *
 * <p>The counts are in the database (login_failures), so a restart does not clear
 * them. That matters on the free plan, where every wake-up is a restart.
 */
@Component
public class LoginAttemptLimiter {

    private final LoginFailureStore store;
    private final int maxFailures;
    private final Duration window;
    private final Clock clock;

    @Autowired
    public LoginAttemptLimiter(
            LoginFailureStore store,
            @Value("${auth.login-limit.max-failures:5}") int maxFailures,
            @Value("${auth.login-limit.window-minutes:15}") int windowMinutes) {
        this(store, maxFailures, Duration.ofMinutes(windowMinutes), Clock.systemDefaultZone());
    }

    // Tests pass their own store and clock so they don't have to wait out the window
    public LoginAttemptLimiter(LoginFailureStore store, int maxFailures, Duration window, Clock clock) {
        this.store = store;
        this.maxFailures = maxFailures;
        this.window = window;
        this.clock = clock;
    }

    /** Throws when this email has used up its failed attempts for the current window. */
    public void checkAllowed(String email) {
        LocalDateTime now = LocalDateTime.now(clock);
        Optional<LoginFailure> failures = store.find(email);
        if (failures.isEmpty() || !now.isBefore(failures.get().getWindowEnd())) {
            return;
        }
        if (failures.get().getFailureCount() >= maxFailures) {
            long secondsLeft = Duration.between(now, failures.get().getWindowEnd()).toSeconds();
            throw new TooManyLoginAttemptsException(Math.max(1, secondsLeft));
        }
    }

    public void recordFailure(String email) {
        LocalDateTime now = LocalDateTime.now(clock);
        Optional<LoginFailure> current = store.find(email);
        if (current.isEmpty() || !now.isBefore(current.get().getWindowEnd())) {
            // The window starts at the first failure
            store.save(new LoginFailure(email, 1, now.plus(window)));
        } else {
            LoginFailure failure = current.get();
            failure.setFailureCount(failure.getFailureCount() + 1);
            store.save(failure);
        }
    }

    public void recordSuccess(String email) {
        if (store.find(email).isPresent()) {
            store.delete(email);
        }
    }
}
