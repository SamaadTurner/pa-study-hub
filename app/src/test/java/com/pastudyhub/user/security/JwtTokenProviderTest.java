package com.pastudyhub.user.security;

import com.pastudyhub.user.exception.InvalidTokenException;
import com.pastudyhub.user.model.User;
import com.pastudyhub.user.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link JwtTokenProvider}.
 *
 * <p>Tests token generation, validation, and edge cases without any Spring context.
 */
@DisplayName("JwtTokenProvider Unit Tests")
class JwtTokenProviderTest {

    // Test secret — 32+ chars, base64-safe, NOT used in production
    private static final String TEST_SECRET = "test-secret-for-unit-tests-only-padding-to-32chars";
    private static final long ACCESS_TOKEN_EXPIRY_MS = 900_000L; // 15 minutes

    private JwtTokenProvider jwtTokenProvider;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(TEST_SECRET, ACCESS_TOKEN_EXPIRY_MS);

        testUser = User.builder()
                .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .email("jocelyn@test.com")
                .firstName("Jocelyn")
                .lastName("Turner")
                .role(UserRole.STUDENT)
                .passwordHash("hashed")
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("generateAccessToken should return non-null, non-empty JWT string")
    void generateAccessToken_shouldReturnValidJwtString() {
        String token = jwtTokenProvider.generateAccessToken(testUser);

        assertThat(token).isNotNull().isNotBlank();
        // JWT has 3 base64url-encoded parts separated by dots
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("validateAndParse should return correct claims from generated token")
    void validateAndParse_shouldReturnCorrectClaims() {
        String token = jwtTokenProvider.generateAccessToken(testUser);
        JwtTokenProvider.JwtClaims claims = jwtTokenProvider.validateAndParse(token);

        assertThat(claims.getUserId()).isEqualTo(testUser.getId());
        assertThat(claims.getEmail()).isEqualTo("jocelyn@test.com");
        assertThat(claims.getFirstName()).isEqualTo("Jocelyn");
        assertThat(claims.getRole()).isEqualTo("STUDENT");
    }

    @Test
    @DisplayName("validateAndParse should extract ADMIN role correctly")
    void validateAndParse_adminRole_shouldExtractCorrectly() {
        testUser.setRole(UserRole.ADMIN);
        String token = jwtTokenProvider.generateAccessToken(testUser);
        JwtTokenProvider.JwtClaims claims = jwtTokenProvider.validateAndParse(token);

        assertThat(claims.getRole()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("validateAndParse should throw InvalidTokenException for expired token")
    void validateAndParse_expiredToken_shouldThrow() {
        // Create provider with -1ms expiry — token is immediately expired
        JwtTokenProvider expiredProvider = new JwtTokenProvider(TEST_SECRET, -1L);
        String expiredToken = expiredProvider.generateAccessToken(testUser);

        assertThatThrownBy(() -> jwtTokenProvider.validateAndParse(expiredToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("validateAndParse should throw InvalidTokenException for malformed token")
    void validateAndParse_malformedToken_shouldThrow() {
        assertThatThrownBy(() -> jwtTokenProvider.validateAndParse("not.a.valid.jwt.token"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("validateAndParse should throw InvalidTokenException for token signed with different key")
    void validateAndParse_wrongSigningKey_shouldThrow() {
        JwtTokenProvider otherProvider = new JwtTokenProvider("different-secret-key-for-testing-purposes", ACCESS_TOKEN_EXPIRY_MS);
        String tokenFromOtherKey = otherProvider.generateAccessToken(testUser);

        assertThatThrownBy(() -> jwtTokenProvider.validateAndParse(tokenFromOtherKey))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("generateRefreshToken should return a valid UUID string")
    void generateRefreshToken_shouldReturnValidUUID() {
        String refreshToken = jwtTokenProvider.generateRefreshToken();

        assertThat(refreshToken).isNotNull().isNotBlank();
        // Should be parseable as UUID
        assertThat(UUID.fromString(refreshToken)).isNotNull();
    }

    @Test
    @DisplayName("generateRefreshToken should return unique tokens on each call")
    void generateRefreshToken_shouldReturnUniqueTokens() {
        String token1 = jwtTokenProvider.generateRefreshToken();
        String token2 = jwtTokenProvider.generateRefreshToken();

        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    @DisplayName("validateAndParse should throw InvalidTokenException for empty string")
    void validateAndParse_emptyString_shouldThrow() {
        assertThatThrownBy(() -> jwtTokenProvider.validateAndParse(""))
                .isInstanceOf(InvalidTokenException.class);
    }
}
