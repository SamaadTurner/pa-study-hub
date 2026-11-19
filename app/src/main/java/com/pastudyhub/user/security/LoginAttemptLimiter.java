package com.pastudyhub.user.security;

import com.pastudyhub.user.exception.TooManyLoginAttemptsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Slows down password guessing: after too many failed logins for one email, that
 * email cannot log in until the time window is over. A successful login clears
 * the count.
 *
 * <p>The count is per email and not per client IP address. Behind Render the app
 * does not reliably see the visitor's address (the static site forwards /api
 * requests, and X-Forwarded-For can be set by the client), so an IP limit would
 * either be easy to dodge or would block everybody at once. The cost of a per-email
 * limit is that someone who knows your email can lock you out for one window.
 *
 * <p>The counts live in memory. That is enough for one server; a restart forgets
 * them, and several servers would each count on their own.
 */
@Component
public class LoginAttemptLimiter {

    // Old entries are only cleaned up once the map is this big, not on every call
    private static final int CLEANUP_SIZE = 10_000;

    private final int maxFailures;
    private final Duration window;
    private final Clock clock;
    private final Map<String, Failures> failuresByEmail = new HashMap<>();

    @Autowired
    public LoginAttemptLimiter(
            @Value("${auth.login-limit.max-failures:5}") int maxFailures,
            @Value("${auth.login-limit.window-minutes:15}") int windowMinutes) {
        this(maxFailures, Duration.ofMinutes(windowMinutes), Clock.systemUTC());
    }

    // Tests pass their own clock so they don't have to wait out the window
    public LoginAttemptLimiter(int maxFailures, Duration window, Clock clock) {
        this.maxFailures = maxFailures;
        this.window = window;
        this.clock = clock;
    }

    /** Throws when this email has used up its failed attempts for the current window. */
    public synchronized void checkAllowed(String email) {
        Failures failures = failuresByEmail.get(email);
        if (failures == null || failures.isOver(clock.instant())) {
            return;
        }
        if (failures.count >= maxFailures) {
            long secondsLeft = Duration.between(clock.instant(), failures.windowEnd).toSeconds();
            throw new TooManyLoginAttemptsException(Math.max(1, secondsLeft));
        }
    }

    public synchronized void recordFailure(String email) {
        Instant now = clock.instant();
        Failures failures = failuresByEmail.get(email);
        if (failures == null || failures.isOver(now)) {
            if (failuresByEmail.size() >= CLEANUP_SIZE) {
                removeOldEntries(now);
            }
            // The window starts at the first failure
            failuresByEmail.put(email, new Failures(now.plus(window)));
        } else {
            failures.count++;
        }
    }

    public synchronized void recordSuccess(String email) {
        failuresByEmail.remove(email);
    }

    private void removeOldEntries(Instant now) {
        Iterator<Failures> entries = failuresByEmail.values().iterator();
        while (entries.hasNext()) {
            if (entries.next().isOver(now)) {
                entries.remove();
            }
        }
    }

    private static class Failures {
        private int count = 1;
        private final Instant windowEnd;

        Failures(Instant windowEnd) {
            this.windowEnd = windowEnd;
        }

        boolean isOver(Instant now) {
            return !now.isBefore(windowEnd);
        }
    }
}
