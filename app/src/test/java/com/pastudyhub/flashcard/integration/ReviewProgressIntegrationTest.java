package com.pastudyhub.flashcard.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pastudyhub.BaseIntegrationTest;
import com.pastudyhub.flashcard.dto.CreateCardRequest;
import com.pastudyhub.flashcard.dto.CreateDeckRequest;
import com.pastudyhub.flashcard.dto.ReviewRequest;
import com.pastudyhub.flashcard.model.MedicalCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Checks that reviewing a flashcard counts toward the user's progress.
 *
 * <p>This was broken for a while. The review code posted the activity to a separate
 * progress service that was removed when the app became a single service, so reviews
 * saved fine but streaks and daily goals never moved. These tests go through the real
 * endpoints from start to finish so that can't happen again without a test failing.
 */
@DisplayName("Reviewing cards updates progress")
class ReviewProgressIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("One review shows up on the dashboard and starts a streak")
    void review_showsUpOnDashboard() throws Exception {
        UUID user = UUID.randomUUID();
        String deckId = createDeck(user, MedicalCategory.CARDIOLOGY);
        String cardId = createCard(user, deckId, "First-line treatment for stable angina?");

        submitReview(user, cardId, 4);

        mockMvc.perform(get("/api/v1/progress/dashboard")
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStreak").value(1))
                .andExpect(jsonPath("$.totalStudyDays").value(1))
                .andExpect(jsonPath("$.totalCardsReviewed").value(1))
                .andExpect(jsonPath("$.overallAccuracy").value(100.0))
                .andExpect(jsonPath("$.categoryAccuracy.CARDIOLOGY").value(100.0));
    }

    @Test
    @DisplayName("Right and wrong answers are both counted, and accuracy reflects them")
    void reviews_countRightAndWrongAnswers() throws Exception {
        UUID user = UUID.randomUUID();
        String deckId = createDeck(user, MedicalCategory.PULMONOLOGY);
        String firstCard = createCard(user, deckId, "Most common cause of community-acquired pneumonia?");
        String secondCard = createCard(user, deckId, "Classic triad of pulmonary embolism?");

        // "Easy" counts as right, "Again" counts as wrong
        submitReview(user, firstCard, 5);
        submitReview(user, secondCard, 1);

        mockMvc.perform(get("/api/v1/progress/dashboard")
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCardsReviewed").value(2))
                .andExpect(jsonPath("$.overallAccuracy").value(50.0))
                .andExpect(jsonPath("$.categoryAccuracy.PULMONOLOGY").value(50.0));
    }

    @Test
    @DisplayName("Reviews count toward today's goal")
    void review_countsTowardDailyGoal() throws Exception {
        UUID user = UUID.randomUUID();
        String deckId = createDeck(user, MedicalCategory.CARDIOLOGY);
        String cardId = createCard(user, deckId, "ECG finding in pericarditis?");

        submitReview(user, cardId, 4);

        // The default goal is 20 cards, so one card is 5%
        mockMvc.perform(get("/api/v1/progress/goal")
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardsReviewedToday").value(1))
                .andExpect(jsonPath("$.cardGoalPercent").value(5.0))
                .andExpect(jsonPath("$.cardGoalMet").value(false));
    }

    @Test
    @DisplayName("A review of a card that doesn't exist logs nothing")
    void reviewOfMissingCard_logsNothing() throws Exception {
        UUID user = UUID.randomUUID();
        ReviewRequest review = new ReviewRequest();
        review.setQuality(4);

        mockMvc.perform(post("/api/v1/cards/{cardId}/review", UUID.randomUUID())
                        .header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(review)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/progress/dashboard")
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCardsReviewed").value(0));
    }

    // ---- helpers -----------------------------------------------------------

    private String createDeck(UUID user, MedicalCategory category) throws Exception {
        CreateDeckRequest request = CreateDeckRequest.builder()
                .title("Review test deck")
                .category(category)
                .isPublic(false)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/decks")
                        .header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return idFrom(result);
    }

    private String createCard(UUID user, String deckId, String front) throws Exception {
        CreateCardRequest request = new CreateCardRequest();
        request.setFront(front);
        request.setBack("Answer for: " + front);

        MvcResult result = mockMvc.perform(post("/api/v1/decks/{deckId}/cards", deckId)
                        .header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return idFrom(result);
    }

    private void submitReview(UUID user, String cardId, int quality) throws Exception {
        ReviewRequest review = new ReviewRequest();
        review.setQuality(quality);

        mockMvc.perform(post("/api/v1/cards/{cardId}/review", cardId)
                        .header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(review)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextReviewDate").exists());
    }

    private String idFrom(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
    }
}
