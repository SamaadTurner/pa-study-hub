package com.pastudyhub.user.time;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "Today" for one user, in their own time zone.
 *
 * <p>The server runs on UTC. Streaks, the daily goal and due dates follow the
 * student's calendar, so a card studied at 6 pm in Seattle counts for that day,
 * not the next. The browser sends its time zone with every request (X-Time-Zone);
 * it is saved on the user, so work done without a request (an exam handed in by
 * the server when its time ran out) uses the same zone. Without one, UTC is used.
 */
@Slf4j
@Service
public class UserClock {

    public static final String HEADER = "X-Time-Zone";

    private static final Set<String> KNOWN_ZONES = ZoneId.getAvailableZoneIds();

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;
    // Saves a database read on every request; one small entry per active user
    private final Map<UUID, ZoneId> zones = new ConcurrentHashMap<>();

    @Autowired
    public UserClock(JdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, Clock.systemUTC());
    }

    // For tests: a fixed clock
    UserClock(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    /** The zone for a header value, or null when it is missing or not a real time zone name. */
    public static ZoneId parse(String value) {
        if (value == null || value.isBlank() || value.length() > 64) {
            return null;
        }
        String name = value.trim();
        // Only names like "America/Los_Angeles" (and UTC): they follow daylight saving
        if (!KNOWN_ZONES.contains(name) && !name.equals("UTC")) {
            return null;
        }
        return ZoneId.of(name);
    }

    /** Saves the user's zone when the browser sends a new one. Bad values are ignored. */
    public void remember(UUID userId, String headerValue) {
        ZoneId zone = parse(headerValue);
        if (zone == null || zone.equals(zones.get(userId))) {
            return;
        }
        zones.put(userId, zone);
        jdbcTemplate.update("UPDATE users SET time_zone = ? WHERE id = ? AND (time_zone IS NULL OR time_zone <> ?)",
                zone.getId(), userId, zone.getId());
        log.debug("Time zone for user {} is now {}", userId, zone);
    }

    public ZoneId zoneOf(UUID userId) {
        ZoneId known = zones.get(userId);
        if (known != null) {
            return known;
        }
        List<String> saved = jdbcTemplate.queryForList("SELECT time_zone FROM users WHERE id = ?", String.class, userId);
        ZoneId zone = saved.isEmpty() ? null : parse(saved.get(0));
        if (zone == null) {
            return ZoneOffset.UTC;
        }
        zones.put(userId, zone);
        return zone;
    }

    /** Today's date where the user is. */
    public LocalDate today(UUID userId) {
        return LocalDate.now(clock.withZone(zoneOf(userId)));
    }

    /** The user's calendar date at a moment the server saved (server times are UTC). */
    public LocalDate dateAt(UUID userId, LocalDateTime serverTime) {
        return serverTime.atOffset(ZoneOffset.UTC).atZoneSameInstant(zoneOf(userId)).toLocalDate();
    }
}
