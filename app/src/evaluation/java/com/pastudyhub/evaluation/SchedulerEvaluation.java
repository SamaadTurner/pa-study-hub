package com.pastudyhub.evaluation;

import com.pastudyhub.flashcard.engine.FsrsEngine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Replays review histories and scores how well SM-2 and FSRS predict forgetting.
 *
 * <p>Run it with: {@code ./gradlew :app:evaluateSchedulers --args="path/to/reviews.csv"}
 *
 * <p>The CSV has a header line and the columns {@code card_id,day,rating}, where day is
 * any running day number and rating is 1 to 4. Each card's reviews are replayed in
 * order. Before every review except the first, each scheduler predicts the chance of
 * remembering; then it is shown the real rating. Only the first review of a card on a
 * given day is used, because both schedulers here plan in whole days.
 */
public final class SchedulerEvaluation {

    private SchedulerEvaluation() {
    }

    /** The scores of one scheduler over the whole file. */
    public record Score(String scheduler, int reviews, double logLoss, double rmseBins, double auc) {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: evaluateSchedulers <reviews.csv>   (columns: card_id,day,rating)");
            System.exit(2);
        }

        List<Review> reviews = readCsv(Path.of(args[0]));
        Map<String, List<Review>> cards = groupByCard(reviews);
        System.out.printf("%d reviews of %d cards read from %s%n%n", reviews.size(), cards.size(), args[0]);

        List<Score> scores = List.of(
                score("SM-2", cards, Sm2Predictor::new),
                score("FSRS-6", cards, () -> new FsrsPredictor(new FsrsEngine(0.9))));

        System.out.printf("%-8s %10s %10s %12s %8s%n", "", "reviews", "log loss", "RMSE (bins)", "AUC");
        for (Score s : scores) {
            System.out.printf("%-8s %10d %10.4f %12.4f %8.4f%n", s.scheduler(), s.reviews(), s.logLoss(), s.rmseBins(), s.auc());
        }
        System.out.println();
        System.out.println("Lower log loss and RMSE are better; higher AUC is better (0.5 = guessing).");
    }

    public static List<Review> readCsv(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file);
        List<Review> reviews = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) { // line 0 is the header
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split(",");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Line " + (i + 1) + " needs 3 columns (card_id,day,rating): " + line);
            }
            reviews.add(new Review(parts[0].trim(), Integer.parseInt(parts[1].trim()), Integer.parseInt(parts[2].trim())));
        }
        return reviews;
    }

    /**
     * Each card's reviews in day order, keeping only the first review of each day.
     * Reviews with the same day keep their order from the file.
     */
    public static Map<String, List<Review>> groupByCard(List<Review> reviews) {
        Map<String, List<Review>> byCard = new LinkedHashMap<>();
        for (Review review : reviews) {
            byCard.computeIfAbsent(review.cardId(), id -> new ArrayList<>()).add(review);
        }

        Map<String, List<Review>> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<Review>> entry : byCard.entrySet()) {
            List<Review> history = new ArrayList<>(entry.getValue());
            history.sort((a, b) -> Integer.compare(a.day(), b.day())); // stable, so same-day order is kept

            List<Review> onePerDay = new ArrayList<>();
            for (Review review : history) {
                boolean sameDayAsPrevious = !onePerDay.isEmpty()
                        && onePerDay.get(onePerDay.size() - 1).day() == review.day();
                if (!sameDayAsPrevious) {
                    onePerDay.add(review);
                }
            }
            result.put(entry.getKey(), onePerDay);
        }
        return result;
    }

    /** Replays every card with a fresh predictor and scores the predictions. */
    public static Score score(String name, Map<String, List<Review>> cards, Supplier<Predictor> newPredictor) {
        List<Metrics.Outcome> outcomes = new ArrayList<>();

        for (List<Review> history : cards.values()) {
            Predictor predictor = newPredictor.get();
            Review previous = null;
            for (Review review : history) {
                if (previous == null) {
                    predictor.review(0, review.rating()); // nothing to predict before the first review
                } else {
                    int elapsed = review.day() - previous.day();
                    outcomes.add(new Metrics.Outcome(predictor.chanceOfRemembering(elapsed), review.remembered()));
                    predictor.review(elapsed, review.rating());
                }
                previous = review;
            }
        }

        if (outcomes.isEmpty()) {
            return new Score(name, 0, Double.NaN, Double.NaN, Double.NaN);
        }
        return new Score(name, outcomes.size(), Metrics.logLoss(outcomes), Metrics.rmseBins(outcomes), Metrics.auc(outcomes));
    }
}
