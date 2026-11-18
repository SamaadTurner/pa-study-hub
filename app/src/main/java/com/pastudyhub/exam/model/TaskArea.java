package com.pastudyhub.exam.model;

/**
 * The task a question tests, the second dimension of the NCCPA PANCE content
 * blueprint (the first one is the organ system, see {@link QuestionCategory}).
 *
 * <p>The percentages are the share of the exam for each task, from the blueprint
 * that took effect in January 2025. They add up to 100. NCCPA says a real exam can
 * be a little off from these numbers.
 */
public enum TaskArea {
    HISTORY_AND_PHYSICAL(16),
    DIAGNOSTIC_STUDIES(10),
    MOST_LIKELY_DIAGNOSIS(18),
    HEALTH_MAINTENANCE(11),
    CLINICAL_INTERVENTION(16),
    PHARMACEUTICAL_THERAPEUTICS(15),
    BASIC_SCIENCE(8),
    PROFESSIONAL_PRACTICE(6);

    private final int blueprintPercent;

    TaskArea(int blueprintPercent) {
        this.blueprintPercent = blueprintPercent;
    }

    public int getBlueprintPercent() {
        return blueprintPercent;
    }
}
