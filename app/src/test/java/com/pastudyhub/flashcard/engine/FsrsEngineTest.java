package com.pastudyhub.flashcard.engine;

import com.pastudyhub.flashcard.engine.FsrsEngine.MemoryState;
import com.pastudyhub.flashcard.engine.FsrsEngine.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Compares the Java port with the official TypeScript implementation.
 *
 * <p>The expected numbers were printed by ts-fsrs 5.4.2 with short-term steps and
 * fuzz turned off (the same setup as {@link FsrsEngine}). Each row of a sequence is
 * {days since the previous review, grade, stability, difficulty, interval in days}.
 */
@DisplayName("FSRS-6 scheduler, against the reference implementation")
class FsrsEngineTest {

    private final FsrsEngine engine = new FsrsEngine(0.9);

    // Runs a whole review sequence and checks every step
    private void assertSequence(double[][] steps) {
        MemoryState memory = null;
        for (int i = 0; i < steps.length; i++) {
            double[] step = steps[i];
            Result result = engine.review(memory, (int) step[0], (int) step[1]);

            String where = "step " + (i + 1);
            assertThat(result.memory().stability()).as(where + " stability").isCloseTo(step[2], within(1e-8));
            assertThat(result.memory().difficulty()).as(where + " difficulty").isCloseTo(step[3], within(1e-8));
            assertThat(result.intervalDays()).as(where + " interval").isEqualTo((int) step[4]);

            memory = result.memory();
        }
    }

    @Test
    @DisplayName("First review of a new card, for each of the four buttons")
    void newCards() {
        assertSequence(new double[][] {{0, 1, 0.212, 6.4133, 1}});
        // Stability 1.29 would round to 1 day, but Hard must come after Again, so 2
        assertSequence(new double[][] {{0, 2, 1.2931, 5.11217071, 2}});
        assertSequence(new double[][] {{0, 3, 2.3065, 2.11810397, 3}});
        assertSequence(new double[][] {{0, 4, 8.2956, 1, 8}});
    }

    @Test
    @DisplayName("Good every time, reviewed on the planned day")
    void goodOnTime() {
        assertSequence(new double[][] {
                {0, 3, 2.3065, 2.11810397, 3},
                {2, 3, 10.96433194, 2.11121424, 11},
                {11, 3, 46.28021494, 2.1043314, 46},
                {46, 3, 162.86219887, 2.09745544, 163}});
    }

    @Test
    @DisplayName("A mix of ratings, including a lapse")
    void mixedRatings() {
        assertSequence(new double[][] {
                {0, 3, 2.3065, 2.11810397, 3},
                {3, 2, 9.23487052, 4.75285849, 9},
                {4, 4, 30.18464453, 2.98473668, 30},
                {20, 1, 2.30524355, 7.67935902, 2},
                {1, 3, 4.17786825, 7.66690803, 4}});
    }

    @Test
    @DisplayName("A late review grows stability more than an on-time one")
    void lateReview() {
        assertSequence(new double[][] {
                {0, 3, 2.3065, 2.11810397, 3},
                {30, 3, 37.42063886, 2.11121424, 37}});
    }

    @Test
    @DisplayName("An early review grows stability less")
    void earlyReview() {
        assertSequence(new double[][] {
                {0, 4, 8.2956, 1, 8},
                {1, 3, 13.48506225, 1, 13}});
    }

    @Test
    @DisplayName("Repeated lapses push difficulty up and keep intervals short")
    void repeatedLapses() {
        assertSequence(new double[][] {
                {0, 1, 0.212, 6.4133, 1},
                {1, 1, 0.10088579, 8.80630447, 1},
                {1, 2, 0.45109367, 9.19279765, 2},
                {2, 3, 1.65443819, 9.17883322, 3}});
    }

    @Test
    @DisplayName("The forgetting curve matches the reference, and is 90% after 'stability' days")
    void forgettingCurve() {
        assertThat(engine.retrievability(1, 1)).isCloseTo(0.9, within(1e-8));
        assertThat(engine.retrievability(5, 10)).isCloseTo(0.94034429, within(1e-8));
        assertThat(engine.retrievability(30, 10)).isCloseTo(0.8093881, within(1e-8));
        assertThat(engine.retrievability(100, 25)).isCloseTo(0.78213212, within(1e-8));
    }

    @Test
    @DisplayName("A lower desired retention spaces reviews further apart")
    void lowerRetentionMeansLongerIntervals() {
        FsrsEngine relaxed = new FsrsEngine(0.8);
        MemoryState afterGood = new MemoryState(10.96433194, 2.11121424);

        int strict = engine.review(afterGood, 11, FsrsEngine.GOOD).intervalDays();
        int loose = relaxed.review(afterGood, 11, FsrsEngine.GOOD).intervalDays();

        assertThat(loose).isGreaterThan(strict);
    }

    @Test
    @DisplayName("The app's buttons map to FSRS grades")
    void buttonsMapToGrades() {
        assertThat(FsrsEngine.gradeFromQuality(1)).isEqualTo(FsrsEngine.AGAIN);
        assertThat(FsrsEngine.gradeFromQuality(2)).isEqualTo(FsrsEngine.HARD);
        assertThat(FsrsEngine.gradeFromQuality(3)).isEqualTo(FsrsEngine.GOOD);
        assertThat(FsrsEngine.gradeFromQuality(4)).isEqualTo(FsrsEngine.GOOD);
        assertThat(FsrsEngine.gradeFromQuality(5)).isEqualTo(FsrsEngine.EASY);
        assertThat(FsrsEngine.gradeFromQuality(0)).isEqualTo(FsrsEngine.AGAIN);
    }

    @Test
    @DisplayName("Bad input is refused")
    void badInput() {
        assertThatThrownBy(() -> engine.review(null, 0, 5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> engine.review(null, -1, 3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FsrsEngine(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FsrsEngine(1.2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FsrsEngine(new double[19], 0.9)).isInstanceOf(IllegalArgumentException.class);
    }
}
