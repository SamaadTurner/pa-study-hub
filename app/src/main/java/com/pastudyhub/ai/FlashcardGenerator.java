package com.pastudyhub.ai;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.pastudyhub.ai.dto.GeneratedCard;
import com.pastudyhub.ai.usage.AiUsageLimiter;
import com.pastudyhub.flashcard.engine.ClozeText;
import com.pastudyhub.flashcard.model.CardType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Turns a student's notes into suggested flashcards.
 *
 * <p>Nothing is saved here. The cards go back to the user to read and edit first,
 * and they save the ones they want with the normal "add card" endpoint. A model
 * can get facts wrong, and in medicine that matters, so a person stays in between.
 */
@Service
@RequiredArgsConstructor
public class FlashcardGenerator {

    // Same limits as CreateCardRequest, so every suggestion can be saved as it is
    static final int MAX_FRONT_LENGTH = 2000;
    static final int MAX_BACK_LENGTH = 5000;

    static final String SYSTEM_PROMPT = """
            You write flashcards for physician assistant students.
            Use only facts that are in the student's notes. Do not add facts of your own.
            Each card has a short question on the front and a clear answer on the back.
            Reply with JSON only, in exactly this shape:
            {"cards": [{"front": "question", "back": "answer"}]}
            """;

    static final String CLOZE_SYSTEM_PROMPT = """
            You write fill-in-the-blank flashcards for physician assistant students.
            Use only facts that are in the student's notes. Do not add facts of your own.
            Each card is one sentence from the notes in which the key term is marked as a
            blank like this: {{c1::term}}. Mark at most two terms per sentence, as
            {{c1::first}} and {{c2::second}}. "extra" is an optional short note.
            Reply with JSON only, in exactly this shape:
            {"cards": [{"text": "sentence with {{c1::blank}}", "extra": "note or empty"}]}
            """;

    private final ChatClient chatClient;
    private final AiUsageLimiter usageLimiter;
    private final ObjectMapper objectMapper;

    public List<GeneratedCard> generate(UUID userId, String notes, int count) {
        return generate(userId, notes, count, CardType.BASIC);
    }

    public List<GeneratedCard> generate(UUID userId, String notes, int count, CardType cardType) {
        if (!chatClient.isEnabled()) {
            throw new AiUnavailableException("AI features are turned off on this server.");
        }
        usageLimiter.useOne(userId);

        if (cardType == CardType.CLOZE) {
            String userPrompt = "Write " + count + " fill-in-the-blank flashcards from these notes:\n\n" + notes.trim();
            return readClozeCards(chatClient.complete(CLOZE_SYSTEM_PROMPT, userPrompt), count);
        }
        String userPrompt = "Write " + count + " flashcards from these notes:\n\n" + notes.trim();
        String reply = chatClient.complete(SYSTEM_PROMPT, userPrompt);

        return readCards(reply, count);
    }

    /**
     * Reads fill-in-the-blank cards. Each text goes through the same check as a card
     * a person writes (ClozeText), so a sentence without a proper blank is left out
     * before anyone sees it.
     */
    List<GeneratedCard> readClozeCards(String reply, int count) {
        JsonNode cardsNode = AiJson.readObject(objectMapper, reply).path("cards");
        if (!cardsNode.isArray()) {
            throw new AiResponseException("The AI answer did not contain any flashcards.");
        }

        List<GeneratedCard> cards = new ArrayList<>();
        for (JsonNode cardNode : cardsNode) {
            String text = cardNode.path("text").asString("").trim();
            String extra = cardNode.path("extra").asString("").trim();
            boolean usable = ClozeText.problem(text) == null
                    && text.length() <= MAX_FRONT_LENGTH && extra.length() <= MAX_BACK_LENGTH;
            if (usable) {
                cards.add(new GeneratedCard(text, extra, CardType.CLOZE));
            }
            if (cards.size() == count) {
                break;
            }
        }

        if (cards.isEmpty()) {
            throw new AiResponseException("The AI answer did not contain any usable fill-in-the-blank cards.");
        }
        return cards;
    }

    /**
     * Reads the cards out of the model's reply. Models don't always follow the
     * format exactly, so this is forgiving about the wrapping but strict about
     * each card: a card without both a front and a back is left out.
     */
    List<GeneratedCard> readCards(String reply, int count) {
        JsonNode cardsNode = AiJson.readObject(objectMapper, reply).path("cards");
        if (!cardsNode.isArray()) {
            throw new AiResponseException("The AI answer did not contain any flashcards.");
        }

        List<GeneratedCard> cards = new ArrayList<>();
        for (JsonNode cardNode : cardsNode) {
            String front = cardNode.path("front").asString("").trim();
            String back = cardNode.path("back").asString("").trim();

            boolean complete = !front.isEmpty() && !back.isEmpty();
            boolean fits = front.length() <= MAX_FRONT_LENGTH && back.length() <= MAX_BACK_LENGTH;
            if (complete && fits) {
                cards.add(new GeneratedCard(front, back));
            }

            // The model sometimes writes more than it was asked for
            if (cards.size() == count) {
                break;
            }
        }

        if (cards.isEmpty()) {
            throw new AiResponseException("The AI answer did not contain any usable flashcards.");
        }
        return cards;
    }
}
