package com.pastudyhub.user.time;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * Saves the time zone the browser sends (X-Time-Zone) for the signed-in user before
 * the request is handled, so everything the request does uses the user's "today".
 */
@Component
@RequiredArgsConstructor
public class TimeZoneInterceptor implements HandlerInterceptor {

    private final UserClock userClock;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userId = request.getHeader("X-User-Id");
        String zone = request.getHeader(UserClock.HEADER);
        if (userId != null && zone != null) {
            try {
                userClock.remember(UUID.fromString(userId), zone);
            } catch (IllegalArgumentException ignored) {
                // Not a user id; the request is refused later anyway
            }
        }
        return true;
    }
}
