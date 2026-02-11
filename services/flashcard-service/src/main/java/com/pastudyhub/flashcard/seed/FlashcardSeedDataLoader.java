package com.pastudyhub.flashcard.seed;

import com.pastudyhub.flashcard.repository.DeckRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Seed data loader for the flashcard service.
 *
 * <p>In the Docker/production profile, seed data is loaded via Flyway migration
 * (V1__create_flashcard_schema.sql), which embeds real PA school content directly
 * in the schema migration as a single transactional operation.
 *
 * <p>This runner exists for the dev profile only — it logs the current deck count
 * so developers can verify H2 schema creation succeeded on startup.
 */
@Slf4j
@Component
@Profile("!docker")
@RequiredArgsConstructor
public class FlashcardSeedDataLoader implements ApplicationRunner {

    private final DeckRepository deckRepository;

    @Override
    public void run(ApplicationArguments args) {
        long deckCount = deckRepository.count();
        if (deckCount == 0) {
            log.info("Flashcard service started with empty database (dev profile). " +
                    "Seed data is loaded via Flyway in the 'docker' profile.");
        } else {
            log.info("Flashcard service started with {} decks in database.", deckCount);
        }
    }
}
