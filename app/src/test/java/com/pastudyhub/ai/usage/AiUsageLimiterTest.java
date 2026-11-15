package com.pastudyhub.ai.usage;

import com.pastudyhub.ai.AiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Daily AI limits")
class AiUsageLimiterTest {

    private final UUID user = UUID.randomUUID();
    private final LocalDate today = LocalDate.now(ZoneOffset.UTC);

    private AiUsageRepository repository;
    private AiUsageLimiter limiter;

    @BeforeEach
    void setUp() {
        repository = mock(AiUsageRepository.class);

        AiProperties properties = new AiProperties();
        properties.setDailyLimitPerUser(3);
        properties.setDailyLimitTotal(10);

        limiter = new AiUsageLimiter(repository, properties);
    }

    private AiUsage usage(int count) {
        return AiUsage.builder().userId(user).usageDate(today).requestCount(count).build();
    }

    @Test
    @DisplayName("The first request of the day creates today's row with a count of 1")
    void firstRequestOfTheDay() {
        when(repository.findByUserIdAndUsageDate(user, today)).thenReturn(Optional.empty());

        limiter.useOne(user);

        ArgumentCaptor<AiUsage> saved = ArgumentCaptor.forClass(AiUsage.class);
        verify(repository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(user);
        assertThat(saved.getValue().getUsageDate()).isEqualTo(today);
        assertThat(saved.getValue().getRequestCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("A later request adds one to today's count")
    void laterRequest() {
        AiUsage existing = usage(2);
        when(repository.findByUserIdAndUsageDate(user, today)).thenReturn(Optional.of(existing));

        limiter.useOne(user);

        assertThat(existing.getRequestCount()).isEqualTo(3);
        verify(repository).saveAndFlush(existing);
    }

    @Test
    @DisplayName("At the per-user limit the request is refused and nothing is counted")
    void userLimitReached() {
        when(repository.findByUserIdAndUsageDate(user, today)).thenReturn(Optional.of(usage(3)));

        assertThatThrownBy(() -> limiter.useOne(user))
                .isInstanceOf(AiLimitReachedException.class)
                .hasMessageContaining("all 3 AI requests");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("At the site-wide limit everyone is refused, even a user who has made no requests")
    void siteLimitReached() {
        when(repository.totalRequestsOn(today)).thenReturn(10L);

        assertThatThrownBy(() -> limiter.useOne(user))
                .isInstanceOf(AiLimitReachedException.class)
                .hasMessageContaining("whole site");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Two first requests at the same moment: the one that loses gets a try-again message")
    void sameMomentRequests() {
        when(repository.findByUserIdAndUsageDate(user, today)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> limiter.useOne(user))
                .isInstanceOf(AiLimitReachedException.class)
                .hasMessageContaining("try again");
    }

    @Test
    @DisplayName("Remaining requests count down from the limit and never go below 0")
    void remaining() {
        when(repository.findByUserIdAndUsageDate(user, today)).thenReturn(Optional.empty());
        assertThat(limiter.remainingToday(user)).isEqualTo(3);

        when(repository.findByUserIdAndUsageDate(user, today)).thenReturn(Optional.of(usage(1)));
        assertThat(limiter.remainingToday(user)).isEqualTo(2);

        // The limit was lowered after the user had already used more
        when(repository.findByUserIdAndUsageDate(user, today)).thenReturn(Optional.of(usage(5)));
        assertThat(limiter.remainingToday(user)).isZero();
    }
}
