package com.pastudyhub.flashcard.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for the SM-2 spaced repetition algorithm implementation.
 *
 * <p>Tests every branch of the algorithm with exact mathematical verification.
 * The SM-2 algorithm is the core of this application — thorough testing is critical.
 *
 * <p>These tests run in isolation without any Spring context, database, or mocks.
 */
@DisplayName("SpacedRepetitionEngine (SM-2 Algorithm) Unit Tests")
class SpacedRepetitionEngineTest {

    private static final double EASE_FACTOR_DELTA = 0.0001; // tolerance for floating-point comparisons

    private SpacedRepetitionEngine engine;

    @BeforeEach
    void setUp() {
        // Start each test with a fresh card (no prior reviews)
        engine = new SpacedRepetitionEngine(); // interval=0, repetitions=0, easeFactor=2.5
    }

    // ==================== FIRST REVIEW (repetitions = 0) ====================

    @Test
    @DisplayName("quality=5 (Easy) on new card → interval=1, repetitions=1")
    void firstReview_easy_intervalShouldBe1() {
        ReviewResult result = engine.calculateNextReview(5);

        assertThat(result.newInterval()).isEqualTo(1);
        assertThat(result.newRepetitions()).isEqualTo(1);
        // nextReviewDate should be tomorrow
        assertThat(result.nextReviewDate()).isEqualTo(LocalDate.now().plusDays(1));
    }

    @Test
    @DisplayName("quality=4 (Good) on new card → interval=1, repetitions=1")
    void firstReview_good_intervalShouldBe1() {
        ReviewResult result = engine.calculateNextReview(4);

        assertThat(result.newInterval()).isEqualTo(1);
        assertThat(result.newRepetitions()).isEqualTo(1);
    }

    @Test
    @DisplayName("quality=5 (Easy) on new card → easeFactor should increase from 2.5")
    void firstReview_easy_easeFactorShouldIncrease() {
        // quality=5: easeFactor = 2.5 + (0.1 - (5-5) * (0.08 + (5-5) * 0.02))
        //          = 2.5 + (0.1 - 0 * 0.08) = 2.5 + 0.1 = 2.6
        ReviewResult result = engine.calculateNextReview(5);

        assertThat(result.newEaseFactor()).isCloseTo(2.6, within(EASE_FACTOR_DELTA));
    }

    @Test
    @DisplayName("quality=4 (Good) on new card → easeFactor stays near 2.5")
    void firstReview_good_easeFactorShouldBeNearDefault() {
        // quality=4: easeFactor = 2.5 + (0.1 - (5-4) * (0.08 + (5-4) * 0.02))
        //          = 2.5 + (0.1 - 1 * (0.08 + 0.02)) = 2.5 + (0.1 - 0.1) = 2.5
        ReviewResult result = engine.calculateNextReview(4);

        assertThat(result.newEaseFactor()).isCloseTo(2.5, within(EASE_FACTOR_DELTA));
    }

    // ==================== SECOND REVIEW (repetitions = 1) ====================

    @Test
    @DisplayName("quality=5 (Easy) after first review → interval=6, repetitions=2")
    void secondReview_easy_intervalShouldBe6() {
        // After first correct review (rep=1), second review interval = 6
        engine = new SpacedRepetitionEngine(1, 1, 2.5);

        ReviewResult result = engine.calculateNextReview(5);

        assertThat(result.newInterval()).isEqualTo(6);
        assertThat(result.newRepetitions()).isEqualTo(2);
    }

    @Test
    @DisplayName("quality=4 (Good) after first review → interval=6")
    void secondReview_good_intervalShouldBe6() {
        engine = new SpacedRepetitionEngine(1, 1, 2.5);

        ReviewResult result = engine.calculateNextReview(4);

        assertThat(result.newInterval()).isEqualTo(6);
    }

    // ==================== SUBSEQUENT REVIEWS ====================

    @Test
    @DisplayName("quality=5 (Easy) after second review → interval=round(6 * 2.6)=16")
    void thirdReview_easy_intervalShouldBeRoundOf6TimesEaseFactor() {
        // After two "Easy" reviews:
        // After review 1: interval=1, rep=1, easeFactor=2.6
        // After review 2: interval=6, rep=2, easeFactor=2.7
        // After review 3: interval = round(6 * 2.7) = round(16.2) = 16
        engine = new SpacedRepetitionEngine(6, 2, 2.7);

        ReviewResult result = engine.calculateNextReview(5);

        assertThat(result.newInterval()).isEqualTo(16); // round(6 * 2.7) = round(16.2) = 16
        assertThat(result.newRepetitions()).isEqualTo(3);
    }

    @Test
    @DisplayName("quality=5 third review with easeFactor=2.5 → interval=round(6*2.5)=15")
    void thirdReview_withDefaultEaseFactor_shouldCalculateCorrectly() {
        engine = new SpacedRepetitionEngine(6, 2, 2.5);

        ReviewResult result = engine.calculateNextReview(5);

        // round(6 * 2.5) = round(15.0) = 15
        assertThat(result.newInterval()).isEqualTo(15);
    }

    // ==================== INCORRECT RESPONSES ====================

    @Test
    @DisplayName("quality=1 (Again) → repetitions reset to 0, interval=1")
    void review_again_shouldResetRepetitionsAndInterval() {
        // Simulate a card that had been reviewed several times
        engine = new SpacedRepetitionEngine(15, 3, 2.5);

        ReviewResult result = engine.calculateNextReview(1);

        assertThat(result.newRepetitions()).isEqualTo(0);
        assertThat(result.newInterval()).isEqualTo(1);
        // Review date should be tomorrow (not next week)
        assertThat(result.nextReviewDate()).isEqualTo(LocalDate.now().plusDays(1));
    }

    @Test
    @DisplayName("quality=2 (Hard) → repetitions reset to 0, interval=1 (quality < 3)")
    void review_hard_shouldResetBecauseQualityBelow3() {
        engine = new SpacedRepetitionEngine(6, 2, 2.5);

        ReviewResult result = engine.calculateNextReview(2);

        assertThat(result.newRepetitions()).isEqualTo(0);
        assertThat(result.newInterval()).isEqualTo(1);
    }

    @Test
    @DisplayName("quality=1 (Again) should decrease easeFactor")
    void review_again_shouldDecreaseEaseFactor() {
        // quality=1: easeFactor = 2.5 + (0.1 - (5-1) * (0.08 + (5-1) * 0.02))
        //          = 2.5 + (0.1 - 4 * (0.08 + 0.08)) = 2.5 + (0.1 - 4 * 0.16)
        //          = 2.5 + (0.1 - 0.64) = 2.5 - 0.54 = 1.96
        ReviewResult result = engine.calculateNextReview(1);

        assertThat(result.newEaseFactor()).isLessThan(2.5);
        assertThat(result.newEaseFactor()).isCloseTo(1.96, within(EASE_FACTOR_DELTA));
    }

    // ==================== EASE FACTOR FLOOR ====================

    @Test
    @DisplayName("easeFactor should never drop below 1.3 (the minimum)")
    void easeFactorFloor_shouldNeverDropBelow1Point3() {
        // Start with minimum ease factor and rate quality=0 (blackout)
        engine = new SpacedRepetitionEngine(1, 0, 1.3);

        ReviewResult result = engine.calculateNextReview(0);

        assertThat(result.newEaseFactor()).isGreaterThanOrEqualTo(1.3);
        assertThat(result.newEaseFactor()).isCloseTo(1.3, within(EASE_FACTOR_DELTA));
    }

    @Test
    @DisplayName("multiple 'Again' ratings in a row should not push easeFactor below 1.3")
    void repeatedAgain_easeFactorShouldStayAtFloor() {
        engine = new SpacedRepetitionEngine(1, 0, 1.4);

        ReviewResult result1 = engine.calculateNextReview(1);
        assertThat(result1.newEaseFactor()).isGreaterThanOrEqualTo(1.3);

        // Second "Again" starting from the decreased ease factor
        SpacedRepetitionEngine engine2 = new SpacedRepetitionEngine(
                result1.newInterval(), result1.newRepetitions(), result1.newEaseFactor());
        ReviewResult result2 = engine2.calculateNextReview(1);
        assertThat(result2.newEaseFactor()).isGreaterThanOrEqualTo(1.3);
    }

    // ==================== MASTERY THRESHOLD ====================

    @Test
    @DisplayName("card with interval >= 21 days should be considered mastered")
    void masteredCard_intervalAtLeast21Days() {
        engine = new SpacedRepetitionEngine(15, 4, 2.5);

        ReviewResult result = engine.calculateNextReview(5);
        // interval = round(15 * 2.6) = round(39.0) = 39 >= 21

        assertThat(result.isMastered()).isTrue();
    }

    @Test
    @DisplayName("card with interval < 21 days should not be mastered")
    void notMasteredCard_intervalLessThan21Days() {
        engine = new SpacedRepetitionEngine(6, 2, 2.5);

        ReviewResult result = engine.calculateNextReview(5);
        // interval = round(6 * 2.6) = 16 < 21

        assertThat(result.isMastered()).isFalse();
    }

    // ==================== NEXT REVIEW DATE ====================

    @Test
    @DisplayName("nextReviewDate should be today + interval days")
    void nextReviewDate_shouldBeCorrectlyCalculated() {
        ReviewResult result = engine.calculateNextReview(5); // interval=1

        assertThat(result.nextReviewDate()).isEqualTo(LocalDate.now().plusDays(1));
    }

    @Test
    @DisplayName("nextReviewDate with interval=6 should be 6 days from now")
    void nextReviewDateWithInterval6() {
        engine = new SpacedRepetitionEngine(1, 1, 2.5);

        ReviewResult result = engine.calculateNextReview(5); // interval=6

        assertThat(result.nextReviewDate()).isEqualTo(LocalDate.now().plusDays(6));
    }

    // ==================== INVALID INPUT ====================

    @Test
    @DisplayName("quality < 0 should throw IllegalArgumentException")
    void invalidQuality_negative_shouldThrow() {
        assertThatThrownBy(() -> engine.calculateNextReview(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quality must be between 0 and 5");
    }

    @Test
    @DisplayName("quality > 5 should throw IllegalArgumentException")
    void invalidQuality_greaterThan5_shouldThrow() {
        assertThatThrownBy(() -> engine.calculateNextReview(6))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quality must be between 0 and 5");
    }

    // ==================== PARAMETRIZED BOUNDARY TESTS ====================

    @ParameterizedTest
    @ValueSource(ints = {3, 4, 5})
    @DisplayName("quality >= 3 on new card should increment repetitions")
    void correctQuality_shouldIncrementRepetitions(int quality) {
        ReviewResult result = engine.calculateNextReview(quality);

        assertThat(result.newRepetitions()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    @DisplayName("quality < 3 should reset repetitions to 0")
    void incorrectQuality_shouldResetRepetitions(int quality) {
        engine = new SpacedRepetitionEngine(15, 4, 2.5); // Had been well-learned

        ReviewResult result = engine.calculateNextReview(quality);

        assertThat(result.newRepetitions()).isEqualTo(0);
        assertThat(result.newInterval()).isEqualTo(1);
    }
}
