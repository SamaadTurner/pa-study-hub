package com.pastudyhub.progress.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalProgressResponse {
    private int targetCardsPerDay;
    private int targetMinutesPerDay;
    private int cardsReviewedToday;
    private int minutesStudiedToday;
    private boolean cardGoalMet;
    private boolean timeGoalMet;
    private double cardGoalPercent;
    private double timeGoalPercent;
}
