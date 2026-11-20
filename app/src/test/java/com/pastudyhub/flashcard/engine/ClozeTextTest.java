package com.pastudyhub.flashcard.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Cloze text")
class ClozeTextTest {

    @Test
    @DisplayName("Finds each blank number once, smallest first, also with hints and repeats")
    void numbers() {
        String text = "{{c2::Metoprolol}} is a {{c1::beta blocker::drug class}} that lowers {{c2::heart rate}}";
        assertThat(ClozeText.numbers(text)).containsExactly(1, 2);
    }

    @Test
    @DisplayName("Text without blanks, or with broken markup, has no numbers")
    void noBlanks() {
        assertThat(ClozeText.numbers("Plain question")).isEmpty();
        assertThat(ClozeText.numbers("{{c1:missing colon}} and {{c1::}}")).isEmpty();
        assertThat(ClozeText.numbers(null)).isEmpty();
    }

    @Test
    @DisplayName("Says what is wrong: no blanks, or a number out of range")
    void problems() {
        assertThat(ClozeText.problem("No blanks here")).isEqualTo("Mark at least one blank like this: {{c1::the answer}}");
        assertThat(ClozeText.problem("{{c0::zero}}")).isEqualTo("Blank numbers go from c1 to c20");
        assertThat(ClozeText.problem("{{c21::too high}}")).isEqualTo("Blank numbers go from c1 to c20");
        assertThat(ClozeText.problem("{{c1::fine}}")).isNull();
    }
}
