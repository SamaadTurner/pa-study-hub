package com.pastudyhub.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pastudyhub.ai.dto.GeneratedCard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public List<GeneratedCard> generate(String notes, int count) {
        if (!chatClient.isEnabled()) {
            throw new AiUnavailableException("AI features are turned off on this server.");
        }

        String userPrompt = "Write " + count + " flashcards from these notes:\n\n" + notes.trim();
        String reply = chatClient.complete(SYSTEM_PROMPT, userPrompt);

        return readCards(reply, count);
    }

    /**
     * Reads the cards out of the model's reply. Models don't always follow the
     * format exactly, so this is forgiving about the wrapping but strict about
     * each card: a card without both a front and a back is left out.
     */
    List<GeneratedCard> readCards(String reply, int count) {
        JsonNode cardsNode = parseJson(reply).path("cards");
        if (!cardsNode.isArray()) {
            throw new AiResponseException("The AI answer did not contain any flashcards.");
        }

        List<GeneratedCard> cards = new ArrayList<>();
        for (JsonNode cardNode : cardsNode) {
            String front = cardNode.path("front").asText("").trim();
            String back = cardNode.path("back").asText("").trim();

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

    private JsonNode parseJson(String reply) {
        // Some models put the JSON in a ```json block or add a sentence around it,
        // so read from the first { to the last }
        int start = reply.indexOf('{');
        int end = reply.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new AiResponseException("The AI answer was not in the expected format.");
        }

        try {
            return objectMapper.readTree(reply.substring(start, end + 1));
        } catch (JsonProcessingException e) {
            throw new AiResponseException("The AI answer was not in the expected format.", e);
        }
    }
}
