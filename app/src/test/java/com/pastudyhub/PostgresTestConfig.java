package com.pastudyhub;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Only used when the tests run against PostgreSQL.
 *
 * <p>H2 starts empty on every run because it lives in memory. A real database keeps
 * its rows, so a second run would trip over data left behind by the first one
 * (for example a user that is already registered). Wiping the database and running
 * every migration again gives each run the same clean start.
 */
@Configuration
@Profile("postgres")
public class PostgresTestConfig {

    @Bean
    public FlywayMigrationStrategy cleanThenMigrate() {
        return flyway -> {
            flyway.clean();
            flyway.migrate();
        };
    }
}
