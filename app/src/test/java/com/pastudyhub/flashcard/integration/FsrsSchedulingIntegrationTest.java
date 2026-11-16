package com.pastudyhub.flashcard.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pastudyhub.BaseIntegrationTest;
import com.pastudyhub.flashcard.model.ReviewLog;
import com.pastudyhub.flashcard.model.ReviewSchedule;
import com.pastudyhub.flashcard.repository.ReviewLogRepository;
import com.pastudyhub.flashcard.repository.ReviewScheduleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Reviews with the default settings, where FSRS sets the dates (since session 20). The
 * numbers are the same reference values as in FsrsEngineTest.
 */
@DisplayName("Reviews scheduled by FSRS")
class FsrsSchedulingIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReviewScheduleRepository reviewScheduleRepository;

    @Autowired
    private ReviewLogRepository reviewLogRepository;

    @Test
    @DisplayName("A new card rated Good comes back in 3 days, and its FSRS memory is saved")
    void firstReview() throws Exception {
        UUID user = UUID.randomUUID();
        String cardId = createCard(user);

        JsonNode response = review(user, cardId, 4);

        assertThat(response.get("interval").asInt()).isEqualTo(3);
        assertThat(response.get("nextReviewDate").asText()).isEqualTo(LocalDate.now().plusDays(3).toString());

        ReviewSchedule schedule = schedule(user, cardId);
        assertThat(schedule.getFsrsStability()).isCloseTo(2.3065, within(1e-8));
        assertThat(schedule.getFsrsDifficulty()).isCloseTo(2.11810397, within(1e-8));
        assertThat(schedule.getPlannedIntervalDays()).isEqualTo(3);
        // SM-2 still keeps its own numbers, so switching back loses nothing
        assertThat(schedule.getInterval()).isEqualTo(1);
    }

    @Test
    @DisplayName("Good again 2 days later: FSRS plans 11 days, and the log records the 3 that were planned")
    void secondReview() throws Exception {
        UUID user = UUID.randomUUID();
        String cardId = createCard(user);
        review(user, cardId, 4);

        // As if the first review was 2 days ago
        ReviewSchedule schedule = schedule(user, cardId);
        schedule.setLastReviewedAt(schedule.getLastReviewedAt().minusDays(2));
        reviewScheduleRepository.save(schedule);

        JsonNode response = review(user, cardId, 4);

        assertThat(response.get("interval").asInt()).isEqualTo(11);
        assertThat(schedule(user, cardId).getFsrsStability()).isCloseTo(10.96433194, within(1e-8));

        List<ReviewLog> history = reviewLogRepository
                .findByUserIdAndCardIdOrderByReviewedAtAsc(user, UUID.fromString(cardId));
        assertThat(history.get(1).getDaysSinceLastReview()).isEqualTo(2);
        assertThat(history.get(1).getScheduledDays()).isEqualTo(3);
    }

    @Test
    @DisplayName("A card from before FSRS existed starts fresh in FSRS at its next review")
    void cardWithoutFsrsMemory() throws Exception {
        UUID user = UUID.randomUUID();
        String cardId = createCard(user);
        review(user, cardId, 4);

        // What a card reviewed before this version looks like: SM-2 numbers only
        ReviewSchedule schedule = schedule(user, cardId);
        schedule.setFsrsStability(null);
        schedule.setFsrsDifficulty(null);
        reviewScheduleRepository.save(schedule);

        JsonNode response = review(user, cardId, 5);

        // Treated as a first review rated Easy: 8 days
        assertThat(response.get("interval").asInt()).isEqualTo(8);
        assertThat(schedule(user, cardId).getFsrsStability()).isCloseTo(8.2956, within(1e-8));
    }

    @Test
    @DisplayName("The deck page shows the FSRS interval")
    void cardListShowsPlannedInterval() throws Exception {
        UUID user = UUID.randomUUID();
        String deckId = createDeck(user);
        String cardId = createCard(user, deckId);
        review(user, cardId, 4);

        mockMvc.perform(get("/api/v1/decks/{deckId}/cards", deckId)
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].reviewSchedule.interval").value(3));
    }

    // ---- helpers -----------------------------------------------------------

    private ReviewSchedule schedule(UUID user, String cardId) {
        return reviewScheduleRepository.findByCardIdAndUserId(UUID.fromString(cardId), user).orElseThrow();
    }

    private JsonNode review(UUID user, String cardId, int quality) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/cards/{cardId}/review", cardId)
                        .header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quality\":" + quality + "}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String createDeck(UUID user) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/decks")
                        .header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"FSRS deck\",\"category\":\"CARDIOLOGY\",\"isPublic\":false}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createCard(UUID user) throws Exception {
        return createCard(user, createDeck(user));
    }

    private String createCard(UUID user, String deckId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/decks/{deckId}/cards", deckId)
                        .header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"front\":\"Question\",\"back\":\"Answer\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
}
