package com.pastudyhub.exam.engine;

import com.pastudyhub.exam.model.TaskArea;

/**
 * How many questions of one blueprint task area were answered right, out of how many.
 * Used for one exam (the results page) and for all of a user's exams (analytics).
 * Unanswered questions count as wrong, the same as in the score.
 */
public record TaskAreaScore(TaskArea taskArea, int correct, int total, double percent) {

    public static TaskAreaScore of(TaskArea taskArea, int correct, int total) {
        double percent = 0.0;
        if (total > 0) {
            percent = Math.round((double) correct / total * 1000.0) / 10.0;
        }
        return new TaskAreaScore(taskArea, correct, total, percent);
    }
}
