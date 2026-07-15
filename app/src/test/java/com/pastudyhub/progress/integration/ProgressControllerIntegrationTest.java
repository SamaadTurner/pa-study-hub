package com.pastudyhub.progress.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pastudyhub.progress.dto.LogActivityRequest;
import com.pastudyhub.progress.dto.UpdateGoalRequest;
import com.pastudyhub.progress.model.ActivityType;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("ProgressController Integration Tests")
class ProgressControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private static final UUID USER = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    // ---- POST /api/v1/progress/log ----------------------------------------

    @Test
    @Order(1)
    @DisplayName("POST /progress/log: 201 Created for valid activity log")
    void logActivity_returns201() throws Exception {
        LogActivityRequest request = LogActivityRequest.builder()
                .userId(USER)
                .activityType(ActivityType.FLASHCARD_REVIEW)
                .category("CARDIOLOGY")
                .durationMinutes(15)
                .cardsReviewed(20)
                .correctCount(16)
                .totalCount(20)
                .build();

        mockMvc.perform(post("/api/v1/progress/log")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @Order(2)
    @DisplayName("POST /progress/log: 400 when userId is null")
    void logActivity_nullUserId_returns400() throws Exception {
        LogActivityRequest request = LogActivityRequest.builder()
                .userId(null)
                .activityType(ActivityType.FLASHCARD_REVIEW)
                .category("CARDIOLOGY")
                .build();

        mockMvc.perform(post("/api/v1/progress/log")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.userId").exists());
    }

    // ---- GET /api/v1/progress/dashboard ------------------------------------

    @Test
    @Order(3)
    @DisplayName("GET /progress/dashboard: 200 OK with streak and category data")
    void getDashboard_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/progress/dashboard")
                        .header("X-User-Id", USER.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStreak").isNumber())
                .andExpect(jsonPath("$.longestStreak").isNumber())
                .andExpect(jsonPath("$.totalStudyDays").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.totalCardsReviewed").value(greaterThanOrEqualTo(20)))
                .andExpect(jsonPath("$.categoryAccuracy").isMap())
                .andExpect(jsonPath("$.last30DaysActivity").isArray())
                .andExpect(jsonPath("$.last30DaysActivity", hasSize(30)))
                .andExpect(jsonPath("$.todayGoal").isMap());
    }

    // ---- GET/PUT /api/v1/progress/goal -------------------------------------

    @Test
    @Order(4)
    @DisplayName("GET /progress/goal: 200 OK with default goal for new user")
    void getGoalProgress_defaultGoal_returns200() throws Exception {
        UUID newUser = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/progress/goal")
                        .header("X-User-Id", newUser.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetCardsPerDay").value(20))
                .andExpect(jsonPath("$.targetMinutesPerDay").value(30))
                .andExpect(jsonPath("$.cardGoalMet").value(false));
    }

    @Test
    @Order(5)
    @DisplayName("PUT /progress/goal: 200 OK after updating goal")
    void updateGoal_returns200() throws Exception {
        UpdateGoalRequest request = UpdateGoalRequest.builder()
                .targetCardsPerDay(50)
                .targetMinutesPerDay(60)
                .build();

        mockMvc.perform(put("/api/v1/progress/goal")
                        .header("X-User-Id", USER.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetCardsPerDay").value(50))
                .andExpect(jsonPath("$.targetMinutesPerDay").value(60));
    }

    @Test
    @Order(6)
    @DisplayName("PUT /progress/goal: 400 when targetCardsPerDay is 0")
    void updateGoal_zeroCards_returns400() throws Exception {
        UpdateGoalRequest request = UpdateGoalRequest.builder()
                .targetCardsPerDay(0)
                .targetMinutesPerDay(30)
                .build();

        mockMvc.perform(put("/api/v1/progress/goal")
                        .header("X-User-Id", USER.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
