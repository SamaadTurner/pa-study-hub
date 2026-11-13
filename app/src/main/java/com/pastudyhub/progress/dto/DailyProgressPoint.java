package com.pastudyhub.progress.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** A single data point in the 30-day activity chart. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyProgressPoint {
    private LocalDate date;
    private int cardsReviewed;
    /** Questions from practice exams finished that day. */
    private int examQuestions;
    private int minutesStudied;
    private double accuracy;
    private boolean goalMet;
}
