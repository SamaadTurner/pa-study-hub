package com.pastudyhub.user.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * The refresh token travels in a cookie the page's scripts can't read (HttpOnly).
 *
 * <p>It used to be returned in the JSON and kept in localStorage, where any script
 * that got into the page could read it and keep a session for 7 days. Now only the
 * browser holds it, and only sends it to the two endpoints that need it (the path),
 * and never from another site (SameSite=Strict). The short access token (15 minutes)
 * still goes in the JSON.
 */
@Component
public class RefreshTokenCookie {

    public static final String NAME = "pa_refresh";
    private static final String PATH = "/api/v1/auth";

    private final boolean secure;
    private final Duration maxAge;

    public RefreshTokenCookie(@Value("${auth.refresh-cookie.secure:true}") boolean secure,
                              @Value("${jwt.refresh-token-expiry-days:7}") int expiryDays) {
        this.secure = secure;
        this.maxAge = Duration.ofDays(expiryDays);
    }

    public ResponseCookie create(String refreshToken) {
        return base(refreshToken).maxAge(maxAge).build();
    }

    /** Tells the browser to delete the cookie (sign-out, closed account). */
    public ResponseCookie clear() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                // Only over HTTPS. Off in the dev profile, which runs on plain http
                .secure(secure)
                .sameSite("Strict")
                .path(PATH);
    }
}
