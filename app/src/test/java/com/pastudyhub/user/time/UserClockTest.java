package com.pastudyhub.user.time;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("User clock")
class UserClockTest {

    // 2026-11-21 02:00 UTC is still the evening of Nov 20 in Seattle
    private static final Instant NOW = Instant.parse("2026-11-21T02:00:00Z");

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final UserClock clock = new UserClock(jdbc, Clock.fixed(NOW, ZoneOffset.UTC));
    private final UUID user = UUID.randomUUID();

    @Test
    @DisplayName("Today and saved times are on the user's calendar, not the server's")
    void usersCalendar() {
        clock.remember(user, "America/Los_Angeles");

        assertThat(clock.today(user)).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(clock.dateAt(user, LocalDateTime.of(2026, 11, 21, 7, 59))).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(clock.dateAt(user, LocalDateTime.of(2026, 11, 21, 8, 0))).isEqualTo(LocalDate.of(2026, 11, 21));
    }

    @Test
    @DisplayName("Without a zone, UTC is used; a saved zone is read once and kept")
    void fallbackAndSaved() {
        when(jdbc.queryForList(anyString(), eq(String.class), any(Object[].class))).thenReturn(List.of());
        assertThat(clock.today(user)).isEqualTo(LocalDate.of(2026, 11, 21));

        UUID other = UUID.randomUUID();
        when(jdbc.queryForList(anyString(), eq(String.class), eq(other))).thenReturn(List.of("Asia/Tokyo"));
        assertThat(clock.zoneOf(other)).isEqualTo(ZoneId.of("Asia/Tokyo"));
        clock.zoneOf(other);
        verify(jdbc, times(1)).queryForList(anyString(), eq(String.class), eq(other));
    }

    @Test
    @DisplayName("Only real zone names count, and an unchanged zone is not saved again")
    void parsing() {
        assertThat(UserClock.parse("Europe/Berlin")).isEqualTo(ZoneId.of("Europe/Berlin"));
        assertThat(UserClock.parse("UTC")).isEqualTo(ZoneId.of("UTC"));
        assertThat(UserClock.parse("+05:00")).isNull();
        assertThat(UserClock.parse("Mars/Olympus_Mons")).isNull();
        assertThat(UserClock.parse(" ")).isNull();
        assertThat(UserClock.parse(null)).isNull();
        assertThat(UserClock.parse("x".repeat(65))).isNull();

        clock.remember(user, "Europe/Berlin");
        clock.remember(user, "Europe/Berlin");
        clock.remember(user, "nonsense");
        verify(jdbc, times(1)).update(anyString(), eq("Europe/Berlin"), eq(user), eq("Europe/Berlin"));
        verify(jdbc, never()).update(anyString(), eq("nonsense"), any(), any());
    }
}
