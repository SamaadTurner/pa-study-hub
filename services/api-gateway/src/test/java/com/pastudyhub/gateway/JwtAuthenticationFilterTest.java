package com.pastudyhub.gateway;

import com.pastudyhub.gateway.filter.JwtAuthenticationFilter;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DisplayName("JwtAuthenticationFilter unit tests")
class JwtAuthenticationFilterTest {

    private static final String SECRET =
            "testSecretKeyThatIsAtLeast256BitsLongForHS256TestingPurposes!";

    private JwtAuthenticationFilter filter;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter();
        ReflectionTestUtils.setField(filter, "jwtSecret", SECRET);

        chain = Mockito.mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    // ---- Public paths bypass auth -----------------------------------------

    @Test
    @DisplayName("OPTIONS request passes through without token check")
    void optionsRequest_passesThroughWithoutAuth() {
        MockServerHttpRequest request = MockServerHttpRequest
                .options("/api/v1/decks")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    @DisplayName("Login path passes through without token")
    void loginPath_bypassesAuth() {
        MockServerHttpRequest request = MockServerHttpRequest
                .post("/api/v1/auth/login")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();
    }

    @Test
    @DisplayName("Register path passes through without token")
    void registerPath_bypassesAuth() {
        MockServerHttpRequest request = MockServerHttpRequest
                .post("/api/v1/auth/register")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();
    }

    // ---- Missing / malformed Authorization header -------------------------

    @Test
    @DisplayName("Returns 401 when Authorization header is absent")
    void missingAuthHeader_returns401() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/decks")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Returns 401 when token does not start with Bearer")
    void bearerPrefixMissing_returns401() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/decks")
                .header(HttpHeaders.AUTHORIZATION, "Token abc123")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ---- Invalid tokens ---------------------------------------------------

    @Test
    @DisplayName("Returns 401 for tampered JWT")
    void tamperedToken_returns401() {
        String token = buildValidToken() + "tampered";

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/decks")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Returns 401 for expired JWT")
    void expiredToken_returns401() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String expired = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(key)
                .compact();

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/decks")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expired)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ---- Valid token ------------------------------------------------------

    @Test
    @DisplayName("Valid token injects X-User-Id header and passes chain")
    void validToken_injectsHeaderAndPassesChain() {
        String userId = UUID.randomUUID().toString();
        String token  = buildTokenForUser(userId, "student@example.com");

        // Capture the mutated request from chain
        final String[] capturedUserId = {null};
        GatewayFilterChain capturingChain = exchange -> {
            capturedUserId[0] = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            return Mono.empty();
        };

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/decks")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, capturingChain))
                .verifyComplete();

        assertThat(capturedUserId[0]).isEqualTo(userId);
        // Response should not have been set to error
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    // ---- Helpers ----------------------------------------------------------

    private String buildValidToken() {
        return buildTokenForUser(UUID.randomUUID().toString(), "test@example.com");
    }

    private String buildTokenForUser(String userId, String email) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(key)
                .compact();
    }
}
