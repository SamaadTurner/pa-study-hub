package com.pastudyhub.user.security;

import com.pastudyhub.user.exception.InvalidTokenException;
import com.pastudyhub.user.model.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * Handles JWT access token generation and validation for PA Study Hub.
 *
 * <p>Token format (HS256, signed with JWT_SECRET env variable):
 * <pre>
 * {
 *   "sub":       "user-uuid-here",
 *   "email":     "jocelyn@example.com",
 *   "firstName": "Jocelyn",
 *   "role":      "STUDENT",
 *   "iat":       1707500000,
 *   "exp":       1707500900    (15 minutes from iat)
 * }
 * </pre>
 *
 * <p>Security notes:
 * <ul>
 *   <li>The JWT_SECRET must be at least 256 bits (32 chars) for HS256</li>
 *   <li>Access tokens expire in 15 minutes — short lifetime limits blast radius of leakage</li>
 *   <li>Tokens are never logged (logging frameworks may redact, but we don't log them at all)</li>
 *   <li>Token validation verifies both signature and expiration atomically</li>
 * </ul>
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey signingKey;
    private final long accessTokenExpiryMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${jwt.access-token-expiry-ms:900000}") long accessTokenExpiryMs) {
        // Derive a SecretKey from the base64-encoded secret. If the secret is shorter
        // than 256 bits (32 bytes), JJWT will throw during key derivation, which is
        // a fail-fast security guard.
        byte[] keyBytes = Decoders.BASE64.decode(
                java.util.Base64.getEncoder().encodeToString(jwtSecret.getBytes()));
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpiryMs = accessTokenExpiryMs;
    }

    /**
     * Generates a signed JWT access token for the given user.
     *
     * <p>The token includes userId, email, firstName, and role as claims
     * so downstream services can authorize without a database lookup.
     *
     * @param user the authenticated user
     * @return signed JWT string (never null)
     */
    public String generateAccessToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpiryMs);

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("firstName", user.getFirstName())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Generates a new opaque refresh token (a random UUID string).
     *
     * <p>Refresh tokens are NOT JWTs — they're random UUIDs stored in the database.
     * This means they can be individually revoked, which JWTs cannot.
     *
     * @return random UUID string
     */
    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * Validates a JWT access token and returns its parsed claims.
     *
     * <p>Validation checks:
     * <ol>
     *   <li>Signature is valid (prevents tampering)</li>
     *   <li>Token is not expired</li>
     *   <li>Token is not malformed</li>
     * </ol>
     *
     * @param token the JWT string to validate
     * @return parsed {@link JwtClaims} containing userId, email, firstName, role
     * @throws InvalidTokenException if the token is expired, malformed, or has invalid signature
     */
    public JwtClaims validateAndParse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return JwtClaims.builder()
                    .userId(UUID.fromString(claims.getSubject()))
                    .email(claims.get("email", String.class))
                    .firstName(claims.get("firstName", String.class))
                    .role(claims.get("role", String.class))
                    .build();

        } catch (ExpiredJwtException e) {
            throw new InvalidTokenException("JWT token has expired");
        } catch (SignatureException e) {
            throw new InvalidTokenException("JWT signature is invalid");
        } catch (MalformedJwtException e) {
            throw new InvalidTokenException("JWT token is malformed");
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("JWT token is invalid: " + e.getMessage());
        }
    }

    /**
     * Immutable record holding the claims extracted from a validated JWT.
     */
    @lombok.Builder
    @lombok.Getter
    public static class JwtClaims {
        private final UUID userId;
        private final String email;
        private final String firstName;
        private final String role;
    }
}
