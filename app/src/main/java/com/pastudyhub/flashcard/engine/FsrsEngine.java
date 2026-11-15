package com.pastudyhub.flashcard.engine;

/**
 * The FSRS-6 scheduler (Free Spaced Repetition Scheduler), in its "long-term"
 * form, which counts in whole days like the rest of this app.
 *
 * <p>SM-2 gives every card an ease factor and multiplies the interval by it.
 * FSRS instead models each card's memory with two numbers:
 * <ul>
 *   <li><b>stability</b>: how many days until the chance of remembering drops to 90%</li>
 *   <li><b>difficulty</b>: 1 to 10, how hard the card is for this user</li>
 * </ul>
 * After each review it updates both, using how long it has been since the last
 * review and the rating. The next review is planned for the day the chance of
 * remembering falls to the desired retention (90% by default).
 *
 * <p>This is a port of the official TypeScript implementation (ts-fsrs 5.4.2) with
 * short-term steps and fuzz turned off. The formulas and the default parameters come
 * from it, including its rounding to 8 decimals, so the results match it exactly.
 * {@code FsrsEngineTest} checks that against numbers printed by ts-fsrs.
 */
public class FsrsEngine {

    // The 21 default FSRS-6 parameters (w0 to w20), trained by the FSRS authors on
    // a large set of Anki reviews. They can later be fitted to one user's history.
    static final double[] DEFAULT_PARAMETERS = {
            0.212, 1.2931, 2.3065, 8.2956, 6.4133, 0.8334, 3.0194, 0.001, 1.8722, 0.1666,
            0.796, 1.4835, 0.0614, 0.2629, 1.6483, 0.6014, 1.8729, 0.5425, 0.0912, 0.0658,
            0.1542
    };

    public static final int AGAIN = 1;
    public static final int HARD = 2;
    public static final int GOOD = 3;
    public static final int EASY = 4;

    private static final double MIN_STABILITY = 0.001;
    private static final double MAX_STABILITY = 36500;
    private static final int MAX_INTERVAL_DAYS = 36500;

    private final double[] w;
    private final double decay;
    private final double factor;
    private final double intervalModifier;

    /** The memory of one card: what FSRS stores between reviews. */
    public record MemoryState(double stability, double difficulty) {
    }

    /** The memory after a review, and the days until the next one. */
    public record Result(MemoryState memory, int intervalDays) {
    }

    /**
     * @param desiredRetention the chance of remembering a card on the day it comes back,
     *                         between 0 and 1. 0.9 is the usual choice. Higher means more reviews.
     */
    public FsrsEngine(double desiredRetention) {
        this(DEFAULT_PARAMETERS, desiredRetention);
    }

    FsrsEngine(double[] parameters, double desiredRetention) {
        if (parameters.length != 21) {
            throw new IllegalArgumentException("FSRS-6 needs 21 parameters, got " + parameters.length);
        }
        if (desiredRetention <= 0 || desiredRetention > 1) {
            throw new IllegalArgumentException("Desired retention must be above 0 and at most 1, got " + desiredRetention);
        }
        this.w = parameters.clone();
        this.decay = -w[20];
        // FACTOR is chosen so that the chance of remembering is exactly 90% after "stability" days
        this.factor = roundTo(Math.exp(Math.log(0.9) / decay) - 1, 8);
        this.intervalModifier = roundTo((Math.pow(desiredRetention, 1 / decay) - 1) / factor, 8);
    }

    /**
     * The app's rating buttons send an SM-2 style quality (1 Again, 2 Hard, 4 Good,
     * 5 Easy). FSRS uses 1 to 4. A quality of 3 is not on any button; it is treated as Good.
     */
    public static int gradeFromQuality(int quality) {
        if (quality <= 1) {
            return AGAIN;
        }
        if (quality == 2) {
            return HARD;
        }
        if (quality <= 4) {
            return GOOD;
        }
        return EASY;
    }

    /**
     * The chance of remembering a card {@code elapsedDays} after the last review.
     * Session 20 uses this to compare how well SM-2 and FSRS predict forgetting.
     */
    public double retrievability(double elapsedDays, double stability) {
        return roundTo(Math.pow(1 + factor * elapsedDays / stability, decay), 8);
    }

    /**
     * Works out the next memory state and interval.
     *
     * @param current     the card's memory before this review, or null for a card never reviewed
     * @param elapsedDays calendar days since the last review (ignored for a new card)
     * @param grade       1 Again, 2 Hard, 3 Good, 4 Easy
     */
    public Result review(MemoryState current, int elapsedDays, int grade) {
        if (grade < AGAIN || grade > EASY) {
            throw new IllegalArgumentException("Grade must be 1 to 4, got " + grade);
        }
        if (elapsedDays < 0) {
            throw new IllegalArgumentException("Elapsed days can't be negative, got " + elapsedDays);
        }

        // The reference works out all four outcomes and then keeps their intervals in
        // order (Again <= Hard < Good < Easy), so a better rating never means a sooner
        // review. To match it, this does the same and then picks the one that was pressed.
        MemoryState[] next = new MemoryState[EASY + 1];
        int[] interval = new int[EASY + 1];

        double recall = current == null ? 0 : retrievability(elapsedDays, current.stability());
        for (int g = AGAIN; g <= EASY; g++) {
            next[g] = current == null ? firstMemory(g) : nextMemory(current, recall, g);
            interval[g] = intervalFor(next[g].stability());
        }

        interval[AGAIN] = Math.min(interval[AGAIN], interval[HARD]);
        interval[HARD] = Math.max(interval[HARD], interval[AGAIN] + 1);
        interval[GOOD] = Math.max(interval[GOOD], interval[HARD] + 1);
        interval[EASY] = Math.max(interval[EASY], interval[GOOD] + 1);

        return new Result(next[grade], interval[grade]);
    }

    private MemoryState firstMemory(int grade) {
        double stability = Math.max(w[grade - 1], 0.1);
        double difficulty = clamp(initialDifficulty(grade), 1, 10);
        return new MemoryState(stability, difficulty);
    }

    private MemoryState nextMemory(MemoryState current, double recall, int grade) {
        double s = current.stability();
        double d = current.difficulty();

        double newStability;
        if (grade == AGAIN) {
            // Forgetting: stability starts over lower, but never goes above what it was
            double afterForgetting = roundTo(clamp(
                    w[11] * Math.pow(d, -w[12]) * (Math.pow(s + 1, w[13]) - 1) * Math.exp((1 - recall) * w[14]),
                    MIN_STABILITY, MAX_STABILITY), 8);
            newStability = clamp(roundTo(s, 8), MIN_STABILITY, afterForgetting);
        } else {
            // Remembering: stability grows more for easier cards, for cards that were
            // closer to being forgotten, and less for cards that are already stable
            double hardPenalty = grade == HARD ? w[15] : 1;
            double easyBonus = grade == EASY ? w[16] : 1;
            newStability = roundTo(clamp(
                    s * (1 + Math.exp(w[8]) * (11 - d) * Math.pow(s, -w[9])
                            * (Math.exp((1 - recall) * w[10]) - 1) * hardPenalty * easyBonus),
                    MIN_STABILITY, MAX_STABILITY), 8);
        }

        return new MemoryState(newStability, nextDifficulty(d, grade));
    }

    private double initialDifficulty(int grade) {
        return roundTo(w[4] - Math.exp((grade - 1) * w[5]) + 1, 8);
    }

    private double nextDifficulty(double d, int grade) {
        double change = -w[6] * (grade - 3);
        // The change gets smaller as difficulty gets close to 10, so it can't run away
        double damped = roundTo(change * (10 - d) / 9, 8);
        double moved = d + damped;
        // Pull difficulty a little back toward the "easy" starting value every time
        double reverted = roundTo(w[7] * initialDifficulty(EASY) + (1 - w[7]) * moved, 8);
        return clamp(reverted, 1, 10);
    }

    private int intervalFor(double stability) {
        long days = Math.round(stability * intervalModifier);
        return (int) Math.min(Math.max(1, days), MAX_INTERVAL_DAYS);
    }

    private static double clamp(double value, double min, double max) {
        return Math.min(Math.max(value, min), max);
    }

    // Same rounding as the reference implementation, so results match to the last digit
    private static double roundTo(double value, int decimals) {
        double scale = Math.pow(10, decimals);
        return Math.round(value * scale) / scale;
    }
}
