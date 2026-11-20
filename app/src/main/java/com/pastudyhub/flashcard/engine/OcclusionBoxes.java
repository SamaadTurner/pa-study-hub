package com.pastudyhub.flashcard.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pastudyhub.flashcard.dto.OcclusionBox;

import java.util.List;

/**
 * Rules for the boxes of a picture card, and how they are stored.
 *
 * <p>Positions are fractions of the picture (0 = left or top edge, 1 = right or
 * bottom edge), so a box stays on the same spot whatever size the picture is shown at.
 */
public final class OcclusionBoxes {

    public static final int MAX_BOXES = 20;
    public static final int MAX_LABEL_LENGTH = 100;
    // Smaller than 1% of the picture can't be seen or tapped
    static final double MIN_SIZE = 0.01;
    // Room for rounding in the browser
    private static final double EDGE = 1.001;

    private static final ObjectMapper JSON = new ObjectMapper();

    private OcclusionBoxes() {
    }

    /** What is wrong with the boxes, in words for the user, or null when they are fine. */
    public static String problem(List<OcclusionBox> boxes) {
        if (boxes == null || boxes.isEmpty()) {
            return "Draw at least one box over the part to hide";
        }
        if (boxes.size() > MAX_BOXES) {
            return "A picture can have at most " + MAX_BOXES + " boxes";
        }
        for (int i = 0; i < boxes.size(); i++) {
            OcclusionBox box = boxes.get(i);
            if (box == null || !isOnPicture(box)) {
                return "Box " + (i + 1) + " is not on the picture";
            }
            if (box.label() != null && box.label().length() > MAX_LABEL_LENGTH) {
                return "The label of box " + (i + 1) + " must not exceed " + MAX_LABEL_LENGTH + " characters";
            }
        }
        return null;
    }

    private static boolean isOnPicture(OcclusionBox box) {
        double[] values = {box.x(), box.y(), box.width(), box.height()};
        for (double value : values) {
            if (!Double.isFinite(value)) {
                return false;
            }
        }
        return box.x() >= 0 && box.y() >= 0
                && box.width() >= MIN_SIZE && box.height() >= MIN_SIZE
                && box.x() + box.width() <= EDGE && box.y() + box.height() <= EDGE;
    }

    public static String toJson(List<OcclusionBox> boxes) {
        try {
            return JSON.writeValueAsString(boxes);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not store the boxes", e);
        }
    }

    /** The stored boxes, or an empty list for a card that has none. */
    public static List<OcclusionBox> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return JSON.readValue(json, new TypeReference<List<OcclusionBox>>() { });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Stored boxes could not be read", e);
        }
    }
}
