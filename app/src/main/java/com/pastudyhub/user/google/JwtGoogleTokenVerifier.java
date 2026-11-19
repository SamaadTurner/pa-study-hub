package com.pastudyhub.user.google;

import com.pastudyhub.user.exception.InvalidTokenException;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Checks Google ID tokens the way Google's documentation asks for: the signature
 * against Google's published keys, the issuer, the audience (our client ID), the
 * expiry, and that Google has verified the email.
 *
 * <p>Uses Spring Security's JWT decoder, which downloads Google's keys and caches
 * them, instead of calling a Google endpoint for every sign-in.
 */
public class JwtGoogleTokenVerifier implements GoogleTokenVerifier {

    public static final String GOOGLE_KEYS_URL = "https://www.googleapis.com/oauth2/v3/certs";

    // Google uses both forms in the "iss" claim
    private static final Set<String> GOOGLE_ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");

    private final JwtDecoder decoder;

    public JwtGoogleTokenVerifier(JwtDecoder decoder) {
        this.decoder = decoder;
    }

    /** The real verifier: Google's keys and the checks for this client ID. */
    public static JwtGoogleTokenVerifier forClientId(String clientId) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(GOOGLE_KEYS_URL).build();
        decoder.setJwtValidator(validator(clientId));
        return new JwtGoogleTokenVerifier(decoder);
    }

    /** The checks besides the signature. Tests use them with their own signing key. */
    public static OAuth2TokenValidator<Jwt> validator(String clientId) {
        OAuth2TokenValidator<Jwt> issuer = jwt -> check(
                jwt.getClaimAsString("iss") != null && GOOGLE_ISSUERS.contains(jwt.getClaimAsString("iss")),
                "The token was not issued by Google");
        OAuth2TokenValidator<Jwt> audience = jwt -> check(
                jwt.getAudience() != null && jwt.getAudience().contains(clientId),
                "The token is for a different app");
        return new DelegatingOAuth2TokenValidator<>(List.of(new JwtTimestampValidator(), issuer, audience));
    }

    @Override
    public GoogleIdentity verify(String idToken) {
        Jwt jwt;
        try {
            jwt = decoder.decode(idToken);
        } catch (JwtException ex) {
            throw new InvalidTokenException("Google sign-in could not be checked");
        }

        String email = jwt.getClaimAsString("email");
        // Without a verified email anyone could claim any address, and the email is
        // what connects a Google account to an existing account here
        if (email == null || !isTrue(jwt.getClaims().get("email_verified"))) {
            throw new InvalidTokenException("Google did not confirm this email address");
        }

        return new GoogleIdentity(
                jwt.getSubject(),
                email.toLowerCase(Locale.ROOT).trim(),
                jwt.getClaimAsString("given_name"),
                jwt.getClaimAsString("family_name"));
    }

    // Google sends a boolean, but older tokens had the string "true"
    private static boolean isTrue(Object value) {
        return Boolean.TRUE.equals(value) || "true".equals(value);
    }

    private static OAuth2TokenValidatorResult check(boolean ok, String message) {
        if (ok) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", message, null));
    }
}
