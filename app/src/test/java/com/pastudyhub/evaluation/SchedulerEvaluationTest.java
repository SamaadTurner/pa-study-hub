package com.pastudyhub.evaluation;

import com.pastudyhub.evaluation.Metrics.Outcome;
import com.pastudyhub.flashcard.engine.FsrsEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

@DisplayName("Scheduler evaluation")
class SchedulerEvaluationTest {

    // ---- metrics -----------------------------------------------------------

    @Test
    @DisplayName("Log loss matches the formula on a small hand-checked example")
    void logLoss() {
        // -(ln 0.8 + ln(1 - 0.3)) / 2 = (0.22314 + 0.35667) / 2
        List<Outcome> outcomes = List.of(new Outcome(0.8, true), new Outcome(0.3, false));

        assertThat(Metrics.logLoss(outcomes)).isCloseTo(0.289909, within(1e-6));
    }

    @Test
    @DisplayName("Log loss stays finite for a completely wrong 100% prediction")
    void logLossIsCapped() {
        double loss = Metrics.logLoss(List.of(new Outcome(1.0, false)));

        assertThat(loss).isFinite().isCloseTo(-Math.log(1e-6), within(1e-6));
    }

    @Test
    @DisplayName("Calibration error is 0 when predictions match what happened")
    void rmseBinsZeroWhenCalibrated() {
        List<Outcome> outcomes = new ArrayList<>();
        // 75% predicted, 3 of 4 remembered
        outcomes.add(new Outcome(0.75, true));
        outcomes.add(new Outcome(0.75, true));
        outcomes.add(new Outcome(0.75, true));
        outcomes.add(new Outcome(0.75, false));

        assertThat(Metrics.rmseBins(outcomes)).isCloseTo(0, within(1e-12));
    }

    @Test
    @DisplayName("Calibration error weights each bin by how many reviews it has")
    void rmseBinsWeightsBins() {
        // Bin 90-100%: predicted 0.95, remembered 1 of 1 -> gap 0.05
        // Bin 10-20%:  predicted 0.15, remembered 3 of 3 -> gap 0.85
        List<Outcome> outcomes = List.of(
                new Outcome(0.95, true),
                new Outcome(0.15, true), new Outcome(0.15, true), new Outcome(0.15, true));

        double expected = Math.sqrt((1 * 0.05 * 0.05 + 3 * 0.85 * 0.85) / 4);
        assertThat(Metrics.rmseBins(outcomes)).isCloseTo(expected, within(1e-12));
    }

    @Test
    @DisplayName("AUC is 1 for a perfect ranking, 0.5 when all predictions are the same")
    void auc() {
        List<Outcome> perfect = List.of(
                new Outcome(0.2, false), new Outcome(0.4, false), new Outcome(0.6, true), new Outcome(0.9, true));
        List<Outcome> allTied = List.of(
                new Outcome(0.5, false), new Outcome(0.5, true), new Outcome(0.5, true));
        List<Outcome> backwards = List.of(new Outcome(0.9, false), new Outcome(0.1, true));

        assertThat(Metrics.auc(perfect)).isEqualTo(1.0);
        assertThat(Metrics.auc(allTied)).isEqualTo(0.5);
        assertThat(Metrics.auc(backwards)).isEqualTo(0.0);
        assertThat(Metrics.auc(List.of(new Outcome(0.5, true)))).isNaN();
    }

    // ---- reading and grouping ------------------------------------------------

    @Test
    @DisplayName("Reads the CSV, skipping the header and blank lines")
    void readsCsv(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("reviews.csv");
        Files.writeString(file, "card_id,day,rating\na,0,3\n\na,3,1\nb, 5 , 4\n");

        List<Review> reviews = SchedulerEvaluation.readCsv(file);

        assertThat(reviews).containsExactly(new Review("a", 0, 3), new Review("a", 3, 1), new Review("b", 5, 4));
    }

    @Test
    @DisplayName("A line with the wrong number of columns or a bad rating is refused")
    void refusesBadLines(@TempDir Path dir) throws IOException {
        Path wrongColumns = dir.resolve("a.csv");
        Files.writeString(wrongColumns, "card_id,day,rating\na,0\n");
        Path badRating = dir.resolve("b.csv");
        Files.writeString(badRating, "card_id,day,rating\na,0,5\n");

        assertThatThrownBy(() -> SchedulerEvaluation.readCsv(wrongColumns)).hasMessageContaining("3 columns");
        assertThatThrownBy(() -> SchedulerEvaluation.readCsv(badRating)).hasMessageContaining("Rating must be 1 to 4");
    }

    @Test
    @DisplayName("Reviews are grouped by card, sorted by day, and only the first review of a day is kept")
    void groupsByCard() {
        List<Review> reviews = List.of(
                new Review("a", 5, 3),
                new Review("b", 0, 1),
                new Review("a", 0, 1),
                new Review("a", 0, 3), // same day as the review before: dropped
                new Review("a", 2, 4));

        Map<String, List<Review>> cards = SchedulerEvaluation.groupByCard(reviews);

        assertThat(cards.get("a")).containsExactly(new Review("a", 0, 1), new Review("a", 2, 4), new Review("a", 5, 3));
        assertThat(cards.get("b")).containsExactly(new Review("b", 0, 1));
    }

    // ---- scoring ---------------------------------------------------------------

    @Test
    @DisplayName("Every review except each card's first is predicted, using the days since the last one")
    void scoresAllButFirstReview() {
        Map<String, List<Review>> cards = SchedulerEvaluation.groupByCard(List.of(
                new Review("a", 0, 3), new Review("a", 3, 3), new Review("a", 14, 1),
                new Review("b", 0, 3)));

        List<String> calls = new ArrayList<>();
        Predictor recorder = new Predictor() {
            @Override
            public double chanceOfRemembering(int elapsedDays) {
                calls.add("predict " + elapsedDays);
                return 0.8;
            }

            @Override
            public void review(int elapsedDays, int rating) {
                calls.add("review " + elapsedDays + " rated " + rating);
            }
        };

        SchedulerEvaluation.Score score = SchedulerEvaluation.score("test", cards, () -> recorder);

        assertThat(score.reviews()).isEqualTo(2);
        assertThat(calls).containsExactly(
                "review 0 rated 3",
                "predict 3", "review 3 rated 3",
                "predict 11", "review 11 rated 1",
                "review 0 rated 3");
    }

    @Test
    @DisplayName("Both schedulers produce sensible predictions on a short real-looking history")
    void bothSchedulersPredict() {
        Map<String, List<Review>> cards = SchedulerEvaluation.groupByCard(List.of(
                new Review("a", 0, 3), new Review("a", 3, 3), new Review("a", 14, 3), new Review("a", 60, 1),
                new Review("b", 0, 1), new Review("b", 1, 3), new Review("b", 4, 1), new Review("b", 5, 3)));

        SchedulerEvaluation.Score sm2 = SchedulerEvaluation.score("SM-2", cards, Sm2Predictor::new);
        SchedulerEvaluation.Score fsrs = SchedulerEvaluation.score("FSRS-6",
                cards, () -> new FsrsPredictor(new FsrsEngine(0.9)));

        assertThat(sm2.reviews()).isEqualTo(6);
        assertThat(fsrs.reviews()).isEqualTo(6);
        assertThat(sm2.logLoss()).isPositive().isFinite();
        assertThat(fsrs.logLoss()).isPositive().isFinite();
        assertThat(fsrs.auc()).isBetween(0.0, 1.0);
    }

    @Test
    @DisplayName("SM-2's assumed curve is 90% on the day its interval ends")
    void sm2AssumedCurve() {
        Sm2Predictor sm2 = new Sm2Predictor();
        sm2.review(0, 3); // first Good: SM-2 sets 1 day
        sm2.review(1, 3); // second Good: 6 days

        assertThat(sm2.chanceOfRemembering(6)).isCloseTo(0.9, within(1e-12));
        assertThat(sm2.chanceOfRemembering(12)).isCloseTo(0.81, within(1e-12));
        assertThat(Sm2Predictor.qualityFor(3)).isEqualTo(4);
        assertThat(Sm2Predictor.qualityFor(4)).isEqualTo(5);
    }

    @Test
    @DisplayName("A file with no repeat reviews gives no scores instead of failing")
    void nothingToScore() {
        Map<String, List<Review>> cards = SchedulerEvaluation.groupByCard(List.of(new Review("a", 0, 3)));

        SchedulerEvaluation.Score score = SchedulerEvaluation.score("SM-2", cards, Sm2Predictor::new);

        assertThat(score.reviews()).isZero();
        assertThat(score.logLoss()).isNaN();
    }
}
