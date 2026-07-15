package com.pastudyhub.exam.engine;

/**
 * Performance classification based on percentage score.
 */
public enum PerformanceBand {
    EXCELLENT("80%+", "Outstanding — well prepared for the PANCE"),
    GOOD("70–79%", "Good — keep reviewing weak areas"),
    PASSING("60–69%", "Borderline — focused review recommended"),
    NEEDS_IMPROVEMENT("Below 60%", "Intensive review needed before the exam");

    private final String range;
    private final String message;

    PerformanceBand(String range, String message) {
        this.range = range;
        this.message = message;
    }

    public String getRange() { return range; }
    public String getMessage() { return message; }
}
