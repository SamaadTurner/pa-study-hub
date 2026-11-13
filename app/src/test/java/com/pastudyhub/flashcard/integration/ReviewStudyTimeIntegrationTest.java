package com.pastudyhub.flashcard.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pastudyhub.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Checks that the time spent on a card counts toward the daily minutes goal.
 *
 * <p>Every review used to be saved with 0 minutes, so the minutes goal on the
 * dashboard stayed at 0 no matter how long someone studied. The review request now
 * carries the seconds spent on the card. The JSON is written out as text here so the
 * tests use the same field name the frontend sends.
 */
@DisplayName("Study time from reviews")
class ReviewStudyTimeIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Seconds from several cards add up to minutes for today")
    void secondsAddUpToMinutes() throws Exception {
        UUID user = UUID.randomUUID();
        String deckId = createDeck(user);

        // 40 + 50 + 45 seconds = 2 minutes and 15 seconds
        review(user, createCard(user, deckId, "Card one"), 4, 40).andExpect(status().isOk());
        review(user, createCard(user, deckId, "Card two"), 4, 50).andExpect(status().isOk());
        review(user, createCard(user, deckId, "Card three"), 2, 45).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/progress/goal")
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardsReviewedToday").value(3))
                .andExpect(jsonPath("$.minutesStudiedToday").value(2));

        // The dashboard shows the same time in its total and in today's bar of the chart
        mockMvc.perform(get("/api/v1/progress/dashboard")
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStudyMinutes").value(2))
                .andExpect(jsonPath("$.todayGoal.minutesStudiedToday").value(2))
                .andExpect(jsonPath("$.last30DaysActivity[29].minutesStudied").value(2));
    }

    @Test
    @DisplayName("Less than a full minute does not count as a minute yet")
    void underAMinute_isZeroMinutes() throws Exception {
        UUID user = UUID.randomUUID();
        String deckId = createDeck(user);

        review(user, createCard(user, deckId, "Quick card"), 5, 59).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/progress/goal")
                        .header("Authorization", bearerToken(user)))
                .andExpect(jsonPath("$.cardsReviewedToday").value(1))
                .andExpect(jsonPath("$.minutesStudiedToday").value(0));
    }

    @Test
    @DisplayName("Enough study time meets the minutes goal")
    void enoughTime_meetsTheGoal() throws Exception {
        UUID user = UUID.randomUUID();
        String deckId = createDeck(user);

        // Smallest goal the app allows: 5 minutes a day
        mockMvc.perform(put("/api/v1/progress/goal")
                        .header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetCardsPerDay\": 50, \"targetMinutesPerDay\": 5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeGoalMet").value(false));

        // Two cards at two and a half minutes each
        review(user, createCard(user, deckId, "Slow card one"), 4, 150).andExpect(status().isOk());
        review(user, createCard(user, deckId, "Slow card two"), 4, 150).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/progress/goal")
                        .header("Authorization", bearerToken(user)))
                .andExpect(jsonPath("$.minutesStudiedToday").value(5))
                .andExpect(jsonPath("$.timeGoalMet").value(true))
                .andExpect(jsonPath("$.timeGoalPercent").value(100.0))
                // the card goal is separate and still far away
                .andExpect(jsonPath("$.cardGoalMet").value(false));
    }

    @Test
    @DisplayName("A card left open for an hour only counts as 5 minutes")
    void veryLongTime_isCapped() throws Exception {
        UUID user = UUID.randomUUID();
        String deckId = createDeck(user);

        review(user, createCard(user, deckId, "Went to lunch"), 4, 3600).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/progress/goal")
                        .header("Authorization", bearerToken(user)))
                .andExpect(jsonPath("$.minutesStudiedToday").value(5));
    }

    @Test
    @DisplayName("A review without a time is still saved and counts as no time")
    void reviewWithoutTime_stillWorks() throws Exception {
        UUID user = UUID.randomUUID();
        String deckId = createDeck(user);
        String cardId = createCard(user, deckId, "No timer");

        mockMvc.perform(post("/api/v1/cards/{cardId}/review", cardId)
                        .header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quality\": 4}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/progress/goal")
                        .header("Authorization", bearerToken(user)))
                .andExpect(jsonPath("$.cardsReviewedToday").value(1))
                .andExpect(jsonPath("$.minutesStudiedToday").value(0));
    }

    @Test
    @DisplayName("A negative time is rejected and nothing is recorded")
    void negativeTime_isRejected() throws Exception {
        UUID user = UUID.randomUUID();
        String deckId = createDeck(user);

        review(user, createCard(user, deckId, "Bad timer"), 4, -30).andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/progress/goal")
                        .header("Authorization", bearerToken(user)))
                .andExpect(jsonPath("$.cardsReviewedToday").value(0))
                .andExpect(jsonPath("$.minutesStudiedToday").value(0));
    }

    @Test
    @DisplayName("One user's study time does not show up for another user")
    void studyTime_isPerUser() throws Exception {
        UUID studier = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();
        String deckId = createDeck(studier);

        review(studier, createCard(studier, deckId, "Mine"), 4, 120).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/progress/goal")
                        .header("Authorization", bearerToken(otherUser)))
                .andExpect(jsonPath("$.minutesStudiedToday").value(0));
    }

    // ---- helpers -----------------------------------------------------------

    // Sends the review body the way the review page does
    private ResultActions review(UUID user, String cardId, int quality, int seconds) throws Exception {
        String body = "{\"quality\": " + quality + ", \"timeSpentSeconds\": " + seconds + "}";

        return mockMvc.perform(post("/api/v1/cards/{cardId}/review", cardId)
                .header("Authorization", bearerToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String createDeck(UUID user) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/decks")
                        .header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Timed deck\", \"category\": \"CARDIOLOGY\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        return idFrom(result);
    }

    private String createCard(UUID user, String deckId, String front) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/decks/{deckId}/cards", deckId)
                        .header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"front\": \"" + front + "\", \"back\": \"Answer\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        return idFrom(result);
    }

    private String idFrom(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
    }
}
