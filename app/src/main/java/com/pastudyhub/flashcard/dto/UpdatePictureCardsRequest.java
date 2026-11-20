package com.pastudyhub.flashcard.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request body for PUT /api/v1/cards/{cardId}/picture: the new boxes, question,
 * notes and tags for all cards of one picture.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePictureCardsRequest {

    /** A box in its new place. cardId is the card it already has; null for a new box. */
    public record EditedBox(double x, double y, double width, double height, String label, UUID cardId) {
        OcclusionBox toBox() {
            return new OcclusionBox(x, y, width, height, label);
        }
    }

    @Size(max = 2000, message = "The question must not exceed 2000 characters")
    private String prompt;

    @Size(max = 5000, message = "Extra notes must not exceed 5000 characters")
    private String extra;

    // Checked in the service (OcclusionBoxes), so the message can name the box
    private List<EditedBox> boxes;

    private List<String> tags;

    /** The boxes as they are stored, without the card ids. */
    public List<OcclusionBox> plainBoxes() {
        return boxes == null ? null : boxes.stream().map(box -> box == null ? null : box.toBox()).toList();
    }
}
