package com.pastudyhub.user.security;

import com.pastudyhub.user.exception.TooManySignupsException;
import com.pastudyhub.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Caps how many accounts the whole site makes per hour and per day.
 *
 * <p>Every new account with a password gets a confirmation email, and the free
 * Brevo plan sends 300 a day. A script making accounts could use that up and stop
 * password reset emails for everyone, or fill the free database. The cap is for the
 * site as a whole, not per IP address: behind Render's proxy the client address is
 * not something the app can trust (the same reason the login lock is per email).
 * It is counted from users.created_at, so it needs no table and survives restarts.
 */
@Slf4j
@Component
public class SignupLimiter {

    private final UserRepository userRepository;
    private final int perHour;
    private final int perDay;

    public SignupLimiter(UserRepository userRepository,
                         @Value("${auth.signup-limit.per-hour:30}") int perHour,
                         @Value("${auth.signup-limit.per-day:200}") int perDay) {
        this.userRepository = userRepository;
        this.perHour = perHour;
        this.perDay = perDay;
    }

    /** Throws when a new account would go over the limit. */
    public void checkRoomForOneMore() {
        LocalDateTime now = LocalDateTime.now();
        long lastHour = userRepository.countByCreatedAtAfter(now.minusHours(1));
        long lastDay = lastHour >= perHour ? lastHour : userRepository.countByCreatedAtAfter(now.minusDays(1));
        if (lastHour >= perHour || lastDay >= perDay) {
            log.warn("Sign-up paused: {} accounts in the last hour, {} in the last day", lastHour, lastDay);
            throw new TooManySignupsException();
        }
    }
}
