package com.pastudyhub;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

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
    // Configuration provided by application-test.yml
}
