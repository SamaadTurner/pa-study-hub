package com.pastudyhub.ai.dto;

import java.util.List;

public record StudyPlanResponse(
        List<String> weakCategories,
        int daysUntilExam,
        int dailyMinutes,
        String studyPlan
) {}
