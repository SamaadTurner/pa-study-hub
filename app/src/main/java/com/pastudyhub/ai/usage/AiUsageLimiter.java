package com.pastudyhub.ai.usage;

import com.pastudyhub.ai.AiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Keeps AI use within two daily limits: one per user, and one for the whole site.
 *
 * <p>The provider's free plan allows a fixed number of requests per day for the
 * whole key. The per-user limit stops one person from using it all. The site-wide
 * limit stops many accounts (or a script that registers them) from doing the same,
 * and keeps us under the provider's own limit, so users get our clear message
 * instead of a provider error.
 *
 * <p>A request is counted before the model is called, the same way the webhook
 * relay counts its budget: a call that fails still cost the provider a request.
 * Days are counted in UTC.
 */
@Service
@RequiredArgsConstructor
public class AiUsageLimiter {

    private final AiUsageRepository repository;
    private final AiProperties properties;

    /**
     * Counts one request for the user, or throws if a limit is reached.
     *
     * @throws AiLimitReachedException when the user or the site has no requests left today
     */
    @Transactional
    public void useOne(UUID userId) {
        LocalDate today = today();

        if (repository.totalRequestsOn(today) >= properties.getDailyLimitTotal()) {
            throw new AiLimitReachedException(
                    "The AI features have reached today's limit for the whole site. They reset at midnight UTC.");
        }

        AiUsage usage = repository.findByUserIdAndUsageDate(userId, today)
                .orElseGet(() -> AiUsage.builder().userId(userId).usageDate(today).requestCount(0).build());

        if (usage.getRequestCount() >= properties.getDailyLimitPerUser()) {
            throw new AiLimitReachedException("You have used all " + properties.getDailyLimitPerUser()
                    + " AI requests for today. They reset at midnight UTC.");
        }

        usage.setRequestCount(usage.getRequestCount() + 1);
        try {
            repository.saveAndFlush(usage);
        } catch (DataIntegrityViolationException e) {
            // Two first requests of the day from the same user at the same moment
            // (a double click) both tried to create today's row. One of them wins.
            throw new AiLimitReachedException("Too many AI requests at the same time. Please try again.");
        }
    }

    /** How many requests the user has left today. */
    @Transactional(readOnly = true)
    public int remainingToday(UUID userId) {
        int used = repository.findByUserIdAndUsageDate(userId, today())
                .map(AiUsage::getRequestCount)
                .orElse(0);
        return Math.max(0, properties.getDailyLimitPerUser() - used);
    }

    private LocalDate today() {
        return LocalDate.now(ZoneOffset.UTC);
    }
}
