package com.pastudyhub;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks the Flyway migrations against a real PostgreSQL database.
 *
 * <p>These only run when TEST_DATABASE=postgres, because H2 builds its tables from
 * the entity classes and never runs the migration files. Just starting the app in
 * this mode is already a check: Hibernate is set to "validate", so the tests can't
 * start at all if an entity and its table don't match.
 */
@EnabledIfEnvironmentVariable(named = "TEST_DATABASE", matches = "postgres")
@DisplayName("Database migrations (PostgreSQL only)")
class MigrationsIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("The tests are really talking to PostgreSQL")
    void databaseIsPostgres() {
        String version = jdbcTemplate.queryForObject("SELECT version()", String.class);

        assertThat(version).startsWith("PostgreSQL");
    }

    @Test
    @DisplayName("Every migration file was applied successfully")
    void everyMigrationWasApplied() throws Exception {
        // Count the files instead of hard-coding a number, so adding V7 doesn't break this
        Resource[] migrationFiles = new PathMatchingResourcePatternResolver()
                .getResources("classpath:db/migration/V*.sql");

        Integer applied = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = TRUE AND version IS NOT NULL",
                Integer.class);

        assertThat(migrationFiles).isNotEmpty();
        assertThat(applied).isEqualTo(migrationFiles.length);
    }

    @Test
    @DisplayName("The old AI chat tables are gone")
    void chatTablesWereDropped() {
        Integer chatTables = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_name IN ('chat_sessions', 'chat_turns')",
                Integer.class);

        assertThat(chatTables).isZero();
    }

    @Test
    @DisplayName("Study time is stored in seconds, the old minutes column is gone")
    void studyTimeColumnIsInSeconds() {
        Integer secondsColumn = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_name = 'activity_logs' AND column_name = 'duration_seconds'",
                Integer.class);
        Integer minutesColumn = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_name = 'activity_logs' AND column_name = 'duration_minutes'",
                Integer.class);

        assertThat(secondsColumn).isEqualTo(1);
        assertThat(minutesColumn).isZero();
    }

    @Test
    @DisplayName("The migrations load the starter practice questions with their answers")
    void starterQuestionsAreLoaded() {
        Integer questions = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM questions", Integer.class);
        Integer questionsWithoutRightAnswer = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM questions q WHERE NOT EXISTS "
                        + "(SELECT 1 FROM answer_options o WHERE o.question_id = q.id AND o.is_correct = TRUE)",
                Integer.class);

        assertThat(questions).isGreaterThan(0);
        assertThat(questionsWithoutRightAnswer).isZero();
    }
}
