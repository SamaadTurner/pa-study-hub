package com.pastudyhub.flashcard.engine;

import com.pastudyhub.flashcard.dto.OcclusionBox;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Picture card boxes")
class OcclusionBoxesTest {

    private static OcclusionBox box(double x, double y, double width, double height) {
        return new OcclusionBox(x, y, width, height, "");
    }

    @Test
    @DisplayName("Boxes inside the picture are fine, right up to the edge")
    void validBoxes() {
        assertThat(OcclusionBoxes.problem(List.of(box(0, 0, 1, 1)))).isNull();
        assertThat(OcclusionBoxes.problem(List.of(box(0.5, 0.5, 0.5, 0.5), box(0.1, 0.1, 0.01, 0.01)))).isNull();
    }

    @Test
    @DisplayName("No boxes, too many boxes, and boxes off the picture or too small are refused")
    void invalidBoxes() {
        assertThat(OcclusionBoxes.problem(null)).isEqualTo("Draw at least one box over the part to hide");
        List<OcclusionBox> many = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            many.add(box(0, 0, 0.1, 0.1));
        }
        assertThat(OcclusionBoxes.problem(many)).isEqualTo("A picture can have at most 20 boxes");
        assertThat(OcclusionBoxes.problem(List.of(box(0, 0, 0.1, 0.1), box(-0.1, 0, 0.2, 0.2))))
                .isEqualTo("Box 2 is not on the picture");
        assertThat(OcclusionBoxes.problem(List.of(box(0, 0.95, 0.1, 0.1)))).isEqualTo("Box 1 is not on the picture");
        assertThat(OcclusionBoxes.problem(List.of(box(0, 0, 0.005, 0.5)))).isEqualTo("Box 1 is not on the picture");
        assertThat(OcclusionBoxes.problem(List.of(box(Double.NaN, 0, 0.1, 0.1)))).isEqualTo("Box 1 is not on the picture");
        assertThat(OcclusionBoxes.problem(Arrays.asList((OcclusionBox) null))).isEqualTo("Box 1 is not on the picture");
    }

    @Test
    @DisplayName("Labels are optional and at most 100 characters")
    void labels() {
        assertThat(OcclusionBoxes.problem(List.of(new OcclusionBox(0, 0, 0.1, 0.1, null)))).isNull();
        assertThat(OcclusionBoxes.problem(List.of(new OcclusionBox(0, 0, 0.1, 0.1, "a".repeat(101)))))
                .isEqualTo("The label of box 1 must not exceed 100 characters");
    }

    @Test
    @DisplayName("Boxes are stored as JSON and read back the same; no JSON means no boxes")
    void storage() {
        List<OcclusionBox> boxes = List.of(new OcclusionBox(0.1, 0.2, 0.3, 0.4, "Aorta"));
        assertThat(OcclusionBoxes.fromJson(OcclusionBoxes.toJson(boxes))).isEqualTo(boxes);
        assertThat(OcclusionBoxes.fromJson(null)).isEmpty();
        assertThat(OcclusionBoxes.fromJson(" ")).isEmpty();
    }
}
