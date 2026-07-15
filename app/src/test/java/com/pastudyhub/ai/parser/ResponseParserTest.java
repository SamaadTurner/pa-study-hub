package com.pastudyhub.ai.parser;

import com.pastudyhub.ai.dto.GeneratedFlashcard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ResponseParser unit tests")
class ResponseParserTest {

    private final ResponseParser parser = new ResponseParser();

    // ---- parseFlashcards ---------------------------------------------------

    @Test
    @DisplayName("parse: returns empty list for null input")
    void parseFlashcards_null_returnsEmpty() {
        assertThat(parser.parseFlashcards(null)).isEmpty();
    }

    @Test
    @DisplayName("parse: returns empty list for blank input")
    void parseFlashcards_blank_returnsEmpty() {
        assertThat(parser.parseFlashcards("   ")).isEmpty();
    }

    @Test
    @DisplayName("parse: parses single flashcard correctly")
    void parseFlashcards_singleCard_parsedCorrectly() {
        String raw = """
                FRONT: What is the first-line treatment for hypertension in CKD patients?
                BACK: ACE inhibitors (e.g., lisinopril) or ARBs — they provide renoprotection.
                HINT: ACE/ARB
                TAGS: Cardiology, Nephrology, Pharmacology
                ---
                """;

        List<GeneratedFlashcard> result = parser.parseFlashcards(raw);

        assertThat(result).hasSize(1);
        GeneratedFlashcard card = result.get(0);
        assertThat(card.front()).contains("first-line treatment for hypertension");
        assertThat(card.back()).contains("ACE inhibitors");
        assertThat(card.hint()).isEqualTo("ACE/ARB");
        assertThat(card.tags()).containsExactlyInAnyOrder("Cardiology", "Nephrology", "Pharmacology");
    }

    @Test
    @DisplayName("parse: parses multiple flashcards")
    void parseFlashcards_multipleCards_parsedCorrectly() {
        String raw = """
                FRONT: What is the MOA of metformin?
                BACK: Activates AMPK; decreases hepatic gluconeogenesis; improves insulin sensitivity.
                HINT: AMPK
                TAGS: Pharmacology, Diabetes
                ---
                FRONT: Classic presentation of appendicitis?
                BACK: Periumbilical pain migrating to RLQ, fever, nausea; positive McBurney's point.
                HINT: McBurney
                TAGS: Surgery, GI
                ---
                """;

        List<GeneratedFlashcard> result = parser.parseFlashcards(raw);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).front()).contains("metformin");
        assertThat(result.get(1).front()).contains("appendicitis");
        assertThat(result.get(1).tags()).containsExactlyInAnyOrder("Surgery", "GI");
    }

    @Test
    @DisplayName("parse: skips malformed blocks that lack FRONT or BACK")
    void parseFlashcards_malformedBlock_skipped() {
        String raw = """
                HINT: something
                TAGS: SomeTag
                ---
                FRONT: Valid question?
                BACK: Valid answer.
                HINT: hint
                TAGS: Tag1
                ---
                """;

        List<GeneratedFlashcard> result = parser.parseFlashcards(raw);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).front()).isEqualTo("Valid question?");
    }

    @Test
    @DisplayName("parse: handles missing HINT gracefully (null hint)")
    void parseFlashcards_missingHint_nullHint() {
        String raw = """
                FRONT: What causes Type 1 DM?
                BACK: Autoimmune destruction of pancreatic beta cells.
                TAGS: Endocrine
                ---
                """;

        List<GeneratedFlashcard> result = parser.parseFlashcards(raw);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).hint()).isNull();
        assertThat(result.get(0).tags()).containsExactly("Endocrine");
    }

    @Test
    @DisplayName("parse: empty TAGS field yields empty tag list")
    void parseFlashcards_emptyTags_emptyList() {
        String raw = """
                FRONT: Q?
                BACK: A.
                HINT: h
                TAGS:
                ---
                """;

        List<GeneratedFlashcard> result = parser.parseFlashcards(raw);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).tags()).isEmpty();
    }

    @Test
    @DisplayName("parse: trims whitespace from fields")
    void parseFlashcards_tripsWhitespace() {
        String raw = "FRONT:   What is atrial fibrillation?   \n" +
                     "BACK:   Irregularly irregular rhythm; absent P waves.   \n" +
                     "HINT:   Afib   \n" +
                     "TAGS:   Cardiology  ,  EKG   \n" +
                     "---\n";

        List<GeneratedFlashcard> result = parser.parseFlashcards(raw);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).front()).isEqualTo("What is atrial fibrillation?");
        assertThat(result.get(0).tags()).containsExactlyInAnyOrder("Cardiology", "EKG");
    }
}
