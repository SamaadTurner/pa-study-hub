package com.pastudyhub.ai.parser;

import com.pastudyhub.ai.dto.GeneratedFlashcard;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Parses structured text responses from Claude into domain objects.
 */
@Component
public class ResponseParser {

    /**
     * Parses Claude's flashcard response into a list of GeneratedFlashcard objects.
     *
     * Expected format per card:
     * FRONT: ...
     * BACK: ...
     * HINT: ...
     * TAGS: ...
     * ---
     */
    public List<GeneratedFlashcard> parseFlashcards(String rawResponse) {
        List<GeneratedFlashcard> flashcards = new ArrayList<>();
        if (rawResponse == null || rawResponse.isBlank()) {
            return flashcards;
        }

        String[] blocks = rawResponse.split("---");
        for (String block : blocks) {
            String trimmed = block.trim();
            if (trimmed.isEmpty()) continue;

            String front = extractField(trimmed, "FRONT");
            String back  = extractField(trimmed, "BACK");
            String hint  = extractField(trimmed, "HINT");
            String tags  = extractField(trimmed, "TAGS");

            if (front == null || back == null) continue;

            List<String> tagList = tags != null
                    ? Arrays.stream(tags.split(","))
                            .map(String::trim)
                            .filter(t -> !t.isEmpty())
                            .toList()
                    : List.of();

            flashcards.add(new GeneratedFlashcard(front, back, hint, tagList));
        }
        return flashcards;
    }

    /**
     * Extracts the value after "FIELD: " from a block of text.
     * Handles multi-line values by reading until the next field or end.
     */
    private String extractField(String block, String fieldName) {
        String marker = fieldName + ":";
        int start = block.indexOf(marker);
        if (start < 0) return null;

        int valueStart = start + marker.length();
        // find the next field start or end of block
        int nextField = findNextField(block, valueStart);
        String value = (nextField > 0)
                ? block.substring(valueStart, nextField)
                : block.substring(valueStart);

        return value.strip();
    }

    private int findNextField(String block, int from) {
        String[] knownFields = {"FRONT:", "BACK:", "HINT:", "TAGS:"};
        int earliest = -1;
        for (String field : knownFields) {
            int idx = block.indexOf("\n" + field, from);
            if (idx >= 0 && (earliest < 0 || idx < earliest)) {
                earliest = idx;
            }
        }
        return earliest;
    }
}
