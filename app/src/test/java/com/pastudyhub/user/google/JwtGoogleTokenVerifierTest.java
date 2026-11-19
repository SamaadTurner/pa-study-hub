package com.pastudyhub.user.google;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.pastudyhub.user.exception.InvalidTokenException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The checks on Google ID tokens. The tokens are signed with a key made in the
 * test instead of Google's, everything else is the production code.
 */
@DisplayName("Google ID token checks")
class JwtGoogleTokenVerifierTest {

    private static final String CLIENT_ID = "test-client.apps.googleusercontent.com";

    private static KeyPair keys;
    private static KeyPair otherKeys;
    private static JwtGoogleTokenVerifier verifier;

    @BeforeAll
    static void makeKeys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keys = generator.generateKeyPair();
        otherKeys = generator.generateKeyPair();

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) keys.getPublic()).build();
        decoder.setJwtValidator(JwtGoogleTokenVerifier.validator(CLIENT_ID));
        verifier = new JwtGoogleTokenVerifier(decoder);
    }

    @Test
    @DisplayName("A valid token gives the account's id, email (lowercased) and names")
    void validToken() throws Exception {
        GoogleIdentity identity = verifier.verify(token(claims -> claims.claim("email", "Riley.Morgan@Gmail.com")));

        assertThat(identity.subject()).isEqualTo("google-sub-123");
        assertThat(identity.email()).isEqualTo("riley.morgan@gmail.com");
        assertThat(identity.firstName()).isEqualTo("Riley");
        assertThat(identity.lastName()).isEqualTo("Morgan");
    }

    @Test
    @DisplayName("Both forms of Google's issuer are accepted")
    void bothIssuers() throws Exception {
        assertThat(verifier.verify(token(claims -> claims.issuer("accounts.google.com"))).subject())
                .isEqualTo("google-sub-123");
    }

    @Test
    @DisplayName("A token for another app is refused")
    void wrongAudience() {
        assertRefused(claims -> claims.audience("someone-elses-app"));
    }

    @Test
    @DisplayName("A token from another issuer is refused")
    void wrongIssuer() {
        assertRefused(claims -> claims.issuer("https://evil.example.com"));
    }

    @Test
    @DisplayName("An expired token is refused")
    void expired() {
        assertRefused(claims -> claims.expirationTime(Date.from(Instant.now().minusSeconds(600))));
    }

    @Test
    @DisplayName("A token signed with another key is refused")
    void wrongSignature() throws Exception {
        String forged = sign(baseClaims().build(), (RSAPrivateKey) otherKeys.getPrivate());
        assertThatThrownBy(() -> verifier.verify(forged)).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("A token whose email Google has not verified is refused")
    void unverifiedEmail() {
        assertRefused(claims -> claims.claim("email_verified", false));
    }

    @Test
    @DisplayName("Garbage is refused")
    void garbage() {
        assertThatThrownBy(() -> verifier.verify("not-a-token")).isInstanceOf(InvalidTokenException.class);
    }

    // ---- helpers -------------------------------------------------------------

    private void assertRefused(Consumer<JWTClaimsSet.Builder> change) {
        assertThatThrownBy(() -> verifier.verify(token(change))).isInstanceOf(InvalidTokenException.class);
    }

    private static JWTClaimsSet.Builder baseClaims() {
        Instant now = Instant.now();
        return new JWTClaimsSet.Builder()
                .issuer("https://accounts.google.com")
                .audience(CLIENT_ID)
                .subject("google-sub-123")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(3600)))
                .claim("email", "riley.morgan@gmail.com")
                .claim("email_verified", true)
                .claim("given_name", "Riley")
                .claim("family_name", "Morgan");
    }

    private static String token(Consumer<JWTClaimsSet.Builder> change) throws Exception {
        JWTClaimsSet.Builder claims = baseClaims();
        change.accept(claims);
        return sign(claims.build(), (RSAPrivateKey) keys.getPrivate());
    }

    private static String sign(JWTClaimsSet claims, RSAPrivateKey key) throws Exception {
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }
}
