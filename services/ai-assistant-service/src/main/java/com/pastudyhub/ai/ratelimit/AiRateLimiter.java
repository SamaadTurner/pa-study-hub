package com.pastudyhub.ai.ratelimit;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory per-user rate limiter for AI endpoints.
 *
 * Limits:
 *   - 20 requests per minute per user (sliding window approximation)
 *   - 100 requests per hour per user
 *
 * In production this would be backed by Redis for multi-instance support.
 */
@Component
public class AiRateLimiter {

    private static final int MAX_PER_MINUTE = 20;
    private static final int MAX_PER_HOUR   = 100;

    private final Map<UUID, WindowCounter> minuteWindows = new ConcurrentHashMap<>();
    private final Map<UUID, WindowCounter> hourWindows   = new ConcurrentHashMap<>();

    /**
     * Attempts to consume one request slot for the given user.
     *
     * @return true if the request is allowed; false if rate-limited
     */
    public boolean tryConsume(UUID userId) {
        Instant now = Instant.now();

        WindowCounter minute = minuteWindows.computeIfAbsent(userId,
                id -> new WindowCounter(60));
        WindowCounter hour = hourWindows.computeIfAbsent(userId,
                id -> new WindowCounter(3600));

        return minute.tryIncrement(now, MAX_PER_MINUTE)
                && hour.tryIncrement(now, MAX_PER_HOUR);
    }

    /**
     * Returns remaining requests allowed in the current minute window.
     */
    public int remainingMinute(UUID userId) {
        WindowCounter c = minuteWindows.get(userId);
        if (c == null) return MAX_PER_MINUTE;
        return Math.max(0, MAX_PER_MINUTE - c.currentCount(Instant.now()));
    }

    // -------------------------------------------------------------------------

    private static class WindowCounter {
        private final long windowSeconds;
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile Instant windowStart = Instant.now();

        WindowCounter(long windowSeconds) {
            this.windowSeconds = windowSeconds;
        }

        synchronized boolean tryIncrement(Instant now, int max) {
            if (now.getEpochSecond() - windowStart.getEpochSecond() >= windowSeconds) {
                windowStart = now;
                count.set(0);
            }
            if (count.get() >= max) return false;
            count.incrementAndGet();
            return true;
        }

        synchronized int currentCount(Instant now) {
            if (now.getEpochSecond() - windowStart.getEpochSecond() >= windowSeconds) {
                return 0;
            }
            return count.get();
        }
    }
}
