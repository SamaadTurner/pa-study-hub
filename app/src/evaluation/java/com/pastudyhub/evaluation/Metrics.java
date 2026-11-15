package com.pastudyhub.evaluation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The three scores the public spaced-repetition benchmark uses. All of them compare
 * the predicted chance of remembering with what really happened.
 */
public final class Metrics {

    private Metrics() {
    }

    /** One prediction and the real outcome. */
    public record Outcome(double predicted, boolean remembered) {
    }

    /**
     * Log loss: how surprised the model was, on average. Lower is better. A confident
     * wrong prediction ("99% sure" and then forgotten) costs a lot.
     */
    public static double logLoss(List<Outcome> outcomes) {
        double total = 0;
        for (Outcome o : outcomes) {
            // Keep away from 0 and 1, where the logarithm is infinite
            double p = Math.min(Math.max(o.predicted(), 1e-6), 1 - 1e-6);
            total += o.remembered() ? -Math.log(p) : -Math.log(1 - p);
        }
        return total / outcomes.size();
    }

    /**
     * Calibration error: put the predictions into 10 bins (0-10%, 10-20%, ...) and
     * compare each bin's average prediction with how often those cards really were
     * remembered. Bins with more reviews count more. Lower is better; 0 means "when it
     * says 80%, 80% are remembered".
     *
     * <p>The benchmark bins by interval, review number and lapses instead. Binning by
     * the prediction itself is simpler and measures the same idea.
     */
    public static double rmseBins(List<Outcome> outcomes) {
        int bins = 10;
        double[] predictedSum = new double[bins];
        double[] rememberedSum = new double[bins];
        int[] count = new int[bins];

        for (Outcome o : outcomes) {
            int bin = Math.min((int) (o.predicted() * bins), bins - 1);
            predictedSum[bin] += o.predicted();
            rememberedSum[bin] += o.remembered() ? 1 : 0;
            count[bin]++;
        }

        double weightedSquares = 0;
        for (int b = 0; b < bins; b++) {
            if (count[b] > 0) {
                double gap = predictedSum[b] / count[b] - rememberedSum[b] / count[b];
                weightedSquares += count[b] * gap * gap;
            }
        }
        return Math.sqrt(weightedSquares / outcomes.size());
    }

    /**
     * AUC: pick a remembered review and a forgotten one at random; how often did the
     * model give the remembered one the higher chance? 0.5 is guessing, 1 is perfect.
     * This measures ranking only, not calibration.
     */
    public static double auc(List<Outcome> outcomes) {
        List<Outcome> sorted = new ArrayList<>(outcomes);
        sorted.sort(Comparator.comparingDouble(Outcome::predicted));

        // Rank-sum method: give tied predictions their average rank
        double rememberedRankSum = 0;
        long rememberedCount = 0;
        int i = 0;
        while (i < sorted.size()) {
            int j = i;
            while (j < sorted.size() && sorted.get(j).predicted() == sorted.get(i).predicted()) {
                j++;
            }
            double averageRank = (i + 1 + j) / 2.0;
            for (int k = i; k < j; k++) {
                if (sorted.get(k).remembered()) {
                    rememberedRankSum += averageRank;
                    rememberedCount++;
                }
            }
            i = j;
        }

        long forgottenCount = sorted.size() - rememberedCount;
        if (rememberedCount == 0 || forgottenCount == 0) {
            return Double.NaN; // not defined when every review had the same outcome
        }
        return (rememberedRankSum - rememberedCount * (rememberedCount + 1) / 2.0)
                / ((double) rememberedCount * forgottenCount);
    }
}
