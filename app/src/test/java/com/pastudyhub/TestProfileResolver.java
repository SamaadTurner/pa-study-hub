package com.pastudyhub;

import org.springframework.test.context.ActiveProfilesResolver;

/**
 * Picks the Spring profiles for the integration tests.
 *
 * <p>By default the tests run against an in-memory H2 database, which is fast and
 * needs nothing installed. Setting the environment variable TEST_DATABASE=postgres
 * adds the "postgres" profile on top, which points the same tests at a real
 * PostgreSQL database and builds the schema with the Flyway migrations. CI runs
 * them both ways.
 */
public class TestProfileResolver implements ActiveProfilesResolver {

    @Override
    public String[] resolve(Class<?> testClass) {
        String database = System.getenv("TEST_DATABASE");

        if ("postgres".equalsIgnoreCase(database)) {
            // Order matters: settings in the later profile win
            return new String[] {"test", "postgres"};
        }

        return new String[] {"test"};
    }
}
