package com.pastudyhub.progress.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pastudyhub.BaseIntegrationTest;
import com.pastudyhub.progress.dto.LogActivityRequest;
import com.pastudyhub.progress.dto.UpdateGoalRequest;
import com.pastudyhub.progress.model.ActivityType;
import com.pastudyhub.progress.service.ProgressService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the progress dashboard and daily goal endpoints.
 *
 * <p>Each test uses its own random user, so the tests don't depend on each other
 * or on the order they run in.
 */
@DisplayName("ProgressController Integration Tests")
class ProgressControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ProgressService progressService;

    // ---- GET /api/v1/progress/dashboard ------------------------------------

    @Test
    @DisplayName("GET /progress/dashboard: new user gets an empty dashboard")
    void getDashboard_newUser_returnsZeros() throws Exception {
        UUID user = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/progress/dashboard")
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStreak").value(0))
                .andExpect(jsonPath("$.longestStreak").value(0))
                .andExpect(jsonPath("$.totalStudyDays").value(0))
                .andExpect(jsonPath("$.totalCardsReviewed").value(0))
                .andExpect(jsonPath("$.last30DaysActivity", hasSize(30)));
    }

    @Test
    @DisplayName("GET /progress/dashboard: logged activity shows up in the totals")
    void getDashboard_withActivity_returnsTotals() throws Exception {
        UUID user = UUID.randomUUID();
        logReviews(user, "CARDIOLOGY", 20, 16);

        mockMvc.perform(get("/api/v1/progress/dashboard")
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStreak").value(1))
                .andExpect(jsonPath("$.longestStreak").value(1))
                .andExpect(jsonPath("$.totalStudyDays").value(1))
                .andExpect(jsonPath("$.totalCardsReviewed").value(20))
                .andExpect(jsonPath("$.overallAccuracy").value(80.0))
                .andExpect(jsonPath("$.categoryAccuracy.CARDIOLOGY").value(80.0))
                .andExpect(jsonPath("$.last30DaysActivity", hasSize(30)))
                // the last point in the list is today
                .andExpect(jsonPath("$.last30DaysActivity[29].cardsReviewed").value(20))
                .andExpect(jsonPath("$.todayGoal.cardsReviewedToday").value(20));
    }

    @Test
    @DisplayName("GET /progress/dashboard: one user does not see another user's activity")
    void getDashboard_onlyCountsOwnActivity() throws Exception {
        UUID userWithActivity = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();
        logReviews(userWithActivity, "PULMONOLOGY", 5, 5);

        mockMvc.perform(get("/api/v1/progress/dashboard")
                        .header("Authorization", bearerToken(otherUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStudyDays").value(0))
                .andExpect(jsonPath("$.totalCardsReviewed").value(0));
    }

    // ---- GET/PUT /api/v1/progress/goal -------------------------------------

    @Test
    @DisplayName("GET /progress/goal: 200 OK with default goal for new user")
    void getGoalProgress_defaultGoal_returns200() throws Exception {
        UUID newUser = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/progress/goal")
                        .header("Authorization", bearerToken(newUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetCardsPerDay").value(20))
                .andExpect(jsonPath("$.targetMinutesPerDay").value(30))
                .andExpect(jsonPath("$.cardGoalMet").value(false));
    }

    @Test
    @DisplayName("GET /progress/goal: card goal is met after enough reviews today")
    void getGoalProgress_goalMetAfterEnoughReviews() throws Exception {
        UUID user = UUID.randomUUID();
        // The default goal is 20 cards a day
        logReviews(user, "CARDIOLOGY", 20, 18);

        mockMvc.perform(get("/api/v1/progress/goal")
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardsReviewedToday").value(20))
                .andExpect(jsonPath("$.cardGoalMet").value(true))
                .andExpect(jsonPath("$.cardGoalPercent").value(100.0));
    }

    @Test
    @DisplayName("PUT /progress/goal: 200 OK after updating goal")
    void updateGoal_returns200() throws Exception {
        UUID user = UUID.randomUUID();
        UpdateGoalRequest request = UpdateGoalRequest.builder()
                .targetCardsPerDay(50)
                .targetMinutesPerDay(60)
                .build();

        mockMvc.perform(put("/api/v1/progress/goal")
                        .header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetCardsPerDay").value(50))
                .andExpect(jsonPath("$.targetMinutesPerDay").value(60));

        // The new goal should still be there on the next read
        mockMvc.perform(get("/api/v1/progress/goal")
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetCardsPerDay").value(50))
                .andExpect(jsonPath("$.targetMinutesPerDay").value(60));
    }

    @Test
    @DisplayName("PUT /progress/goal: 400 when targetCardsPerDay is 0")
    void updateGoal_zeroCards_returns400() throws Exception {
        UUID user = UUID.randomUUID();
        UpdateGoalRequest request = UpdateGoalRequest.builder()
                .targetCardsPerDay(0)
                .targetMinutesPerDay(30)
                .build();

        mockMvc.perform(put("/api/v1/progress/goal")
                        .header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Saves activity the same way the app does when a user reviews cards.
     * There is no HTTP endpoint for this any more, so the tests call the service.
     */
    private void logReviews(UUID userId, String category, int total, int correct) {
        progressService.logActivity(LogActivityRequest.builder()
                .userId(userId)
                .activityType(ActivityType.FLASHCARD_REVIEW)
                .category(category)
                .durationSeconds(0)
                .cardsReviewed(total)
                .correctCount(correct)
                .totalCount(total)
                .build());
    }
}
