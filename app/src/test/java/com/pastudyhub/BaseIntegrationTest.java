package com.pastudyhub;

import com.pastudyhub.user.model.User;
import com.pastudyhub.user.model.UserRole;
import com.pastudyhub.user.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

/**
 * Base class for integration tests that require a full Spring context and database.
 *
 * <p>Uses H2 in-memory database configured in application-test.yml for fast,
 * isolated tests that don't require Docker/Testcontainers.
 *
 * <p>All integration test classes should extend this class to inherit the
 * common test configuration.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    /**
     * Builds an Authorization header value for a logged-in user with the given ID.
     *
     * <p>Every endpoint except /auth needs a valid JWT. The tests used to send an
     * X-User-Id header, which only worked back when a gateway did the login check.
     * The token is signed with the real provider, so requests go through the same
     * security filters they would in production.
     */
    protected String bearerToken(UUID userId) {
        User user = new User();
        user.setId(userId);
        user.setEmail("test-" + userId + "@example.com");
        user.setFirstName("Test");
        user.setRole(UserRole.STUDENT);

        return "Bearer " + jwtTokenProvider.generateAccessToken(user);
    }

    protected String bearerToken(String userId) {
        return bearerToken(UUID.fromString(userId));
    }
}
