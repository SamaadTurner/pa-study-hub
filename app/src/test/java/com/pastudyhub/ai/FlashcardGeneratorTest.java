package com.pastudyhub.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pastudyhub.ai.dto.GeneratedCard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Flashcard generator")
class FlashcardGeneratorTest {

    /** Answers with whatever the test sets, and remembers what it was asked. */
    private static class FakeChatClient implements ChatClient {
        boolean enabled = true;
        String reply = "{\"cards\": []}";
        String lastSystemPrompt;
        String lastUserPrompt;

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public String model() {
            return "fake-model";
        }

        @Override
        public String complete(String systemPrompt, String userPrompt) {
            lastSystemPrompt = systemPrompt;
            lastUserPrompt = userPrompt;
            return reply;
        }
    }

    private final FakeChatClient chatClient = new FakeChatClient();
    private final FlashcardGenerator generator = new FlashcardGenerator(chatClient, new ObjectMapper());

    @Test
    @DisplayName("Returns the cards from the model's answer")
    void returnsCards() {
        chatClient.reply = """
                {"cards": [
                  {"front": "What is Beck's triad?", "back": "Low blood pressure, distended neck veins, muffled heart sounds"},
                  {"front": "First-line drug for anaphylaxis?", "back": "Intramuscular epinephrine"}
                ]}
                """;

        List<GeneratedCard> cards = generator.generate("my notes", 2);

        assertThat(cards).containsExactly(
                new GeneratedCard("What is Beck's triad?",
                        "Low blood pressure, distended neck veins, muffled heart sounds"),
                new GeneratedCard("First-line drug for anaphylaxis?", "Intramuscular epinephrine"));
    }

    @Test
    @DisplayName("Sends the notes and the number of cards, and tells the model to stick to the notes")
    void buildsThePrompt() {
        chatClient.reply = "{\"cards\": [{\"front\": \"Q\", \"back\": \"A\"}]}";

        generator.generate("  Aortic stenosis: crescendo-decrescendo murmur.  ", 3);

        assertThat(chatClient.lastUserPrompt)
                .startsWith("Write 3 flashcards")
                .endsWith("Aortic stenosis: crescendo-decrescendo murmur.");
        assertThat(chatClient.lastSystemPrompt).contains("Use only facts that are in the student's notes");
    }

    @Test
    @DisplayName("Reads JSON that the model wrapped in a code block or a sentence")
    void readsWrappedJson() {
        chatClient.reply = "Here are your cards:\n```json\n{\"cards\": [{\"front\": \"Q\", \"back\": \"A\"}]}\n```";

        assertThat(generator.generate("notes", 1)).containsExactly(new GeneratedCard("Q", "A"));
    }

    @Test
    @DisplayName("Trims spaces and leaves out cards without a front or a back")
    void dropsIncompleteCards() {
        chatClient.reply = """
                {"cards": [
                  {"front": "  Q1  ", "back": "  A1 "},
                  {"front": "", "back": "no question"},
                  {"front": "no answer"},
                  {"back": "also no question"},
                  {"front": "Q2", "back": "A2"}
                ]}
                """;

        assertThat(generator.generate("notes", 5))
                .containsExactly(new GeneratedCard("Q1", "A1"), new GeneratedCard("Q2", "A2"));
    }

    @Test
    @DisplayName("Leaves out cards too long to be saved")
    void dropsCardsThatAreTooLong() {
        String longFront = "x".repeat(FlashcardGenerator.MAX_FRONT_LENGTH + 1);
        String longBack = "y".repeat(FlashcardGenerator.MAX_BACK_LENGTH + 1);
        chatClient.reply = "{\"cards\": ["
                + "{\"front\": \"" + longFront + "\", \"back\": \"A\"},"
                + "{\"front\": \"Q\", \"back\": \"" + longBack + "\"},"
                + "{\"front\": \"Q ok\", \"back\": \"A ok\"}]}";

        assertThat(generator.generate("notes", 3)).containsExactly(new GeneratedCard("Q ok", "A ok"));
    }

    @Test
    @DisplayName("Never returns more cards than were asked for")
    void stopsAtTheCount() {
        chatClient.reply = """
                {"cards": [{"front": "Q1", "back": "A1"}, {"front": "Q2", "back": "A2"}, {"front": "Q3", "back": "A3"}]}
                """;

        assertThat(generator.generate("notes", 2)).hasSize(2);
    }

    @Test
    @DisplayName("An answer that is not JSON is an error")
    void notJson() {
        chatClient.reply = "Sorry, I can't help with that.";

        assertThatThrownBy(() -> generator.generate("notes", 3))
                .isInstanceOf(AiResponseException.class)
                .hasMessageContaining("expected format");
    }

    @Test
    @DisplayName("Broken JSON is an error")
    void brokenJson() {
        chatClient.reply = "{\"cards\": [{\"front\": \"Q\", }";

        assertThatThrownBy(() -> generator.generate("notes", 3))
                .isInstanceOf(AiResponseException.class)
                .hasMessageContaining("expected format");
    }

    @Test
    @DisplayName("JSON without a list of cards is an error")
    void noCardsList() {
        chatClient.reply = "{\"flashcards\": \"none\"}";

        assertThatThrownBy(() -> generator.generate("notes", 3))
                .isInstanceOf(AiResponseException.class)
                .hasMessageContaining("did not contain any flashcards");
    }

    @Test
    @DisplayName("A list with no usable card is an error, not an empty result")
    void noUsableCards() {
        chatClient.reply = "{\"cards\": [{\"front\": \"\", \"back\": \"\"}]}";

        assertThatThrownBy(() -> generator.generate("notes", 3))
                .isInstanceOf(AiResponseException.class)
                .hasMessageContaining("usable");
    }

    @Test
    @DisplayName("When AI is off it says so and does not call the model")
    void turnedOff() {
        chatClient.enabled = false;

        assertThatThrownBy(() -> generator.generate("notes", 3))
                .isInstanceOf(AiUnavailableException.class);
        assertThat(chatClient.lastUserPrompt).isNull();
    }
}
