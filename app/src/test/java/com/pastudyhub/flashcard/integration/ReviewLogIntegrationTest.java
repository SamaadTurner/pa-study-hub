package com.pastudyhub.flashcard.integration;

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
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Every review adds a row to the review history (review_logs). The newer
 * schedulers and the scheduler evaluation (sessions 19 and 20) read from it.
 */
@DisplayName("Review history")
class ReviewLogIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReviewLogRepository reviewLogRepository;

    @Autowired
    private ReviewScheduleRepository reviewScheduleRepository;

    @Test
    @DisplayName("The first review of a card is logged with its rating, time and no previous review")
    void firstReviewIsLogged() throws Exception {
        UUID user = UUID.randomUUID();
        String cardId = createCard(user, createDeck(user, false));
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        review(user, cardId, 4, 42).andExpect(status().isOk());

        ReviewLog log = single(user, cardId);
        assertThat(log.getQuality()).isEqualTo(4);
        assertThat(log.getTimeSpentSeconds()).isEqualTo(42);
        assertThat(log.getDaysSinceLastReview()).isNull();
        assertThat(log.getScheduledDays()).isZero();
        assertThat(log.getReviewedAt()).isAfter(before).isBefore(LocalDateTime.now().plusSeconds(1));
    }

    @Test
    @DisplayName("A later review records the days since the last one and the interval that was planned")
    void laterReviewKnowsTheGap() throws Exception {
        UUID user = UUID.randomUUID();
        String cardId = createCard(user, createDeck(user, false));
        review(user, cardId, 4, 10).andExpect(status().isOk());

        // Pretend the first review was 3 days ago, the way it would be in real use
        ReviewSchedule schedule = reviewScheduleRepository
                .findByCardIdAndUserId(UUID.fromString(cardId), user).orElseThrow();
        schedule.setLastReviewedAt(schedule.getLastReviewedAt().minusDays(3));
        reviewScheduleRepository.save(schedule);

        review(user, cardId, 2, 15).andExpect(status().isOk());

        List<ReviewLog> history = reviewLogRepository
                .findByUserIdAndCardIdOrderByReviewedAtAsc(user, UUID.fromString(cardId));
        assertThat(history).hasSize(2);

        ReviewLog second = history.get(1);
        assertThat(second.getQuality()).isEqualTo(2);
        assertThat(second.getDaysSinceLastReview()).isEqualTo(3);
        // FSRS (the default) sets 3 days after a first Good; that was the plan when the card came back
        assertThat(second.getScheduledDays()).isEqualTo(3);
    }

    @Test
    @DisplayName("Time spent is capped the same way as study time, and left empty when not sent")
    void timeSpentIsCappedOrEmpty() throws Exception {
        UUID user = UUID.randomUUID();
        String deckId = createDeck(user, false);
        String slowCard = createCard(user, deckId);
        String noTimeCard = createCard(user, deckId);

        review(user, slowCard, 5, 4000).andExpect(status().isOk());
        review(user, noTimeCard, 5, null).andExpect(status().isOk());

        assertThat(single(user, slowCard).getTimeSpentSeconds()).isEqualTo(300);
        assertThat(single(user, noTimeCard).getTimeSpentSeconds()).isNull();
    }

    @Test
    @DisplayName("Two users studying the same public card each get their own history")
    void historyIsPerUser() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID visitor = UUID.randomUUID();
        String cardId = createCard(owner, createDeck(owner, true));

        review(owner, cardId, 4, 5).andExpect(status().isOk());
        review(visitor, cardId, 1, 5).andExpect(status().isOk());

        assertThat(single(owner, cardId).getQuality()).isEqualTo(4);
        assertThat(single(visitor, cardId).getQuality()).isEqualTo(1);
    }

    @Test
    @DisplayName("A refused review leaves no history row")
    void refusedReviewIsNotLogged() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();
        String cardId = createCard(owner, createDeck(owner, false));

        // Someone else's private deck
        review(stranger, cardId, 4, 5).andExpect(status().isForbidden());
        // A rating that does not exist
        review(owner, cardId, 9, 5).andExpect(status().isBadRequest());

        assertThat(reviewLogRepository.findByUserIdAndCardIdOrderByReviewedAtAsc(stranger, UUID.fromString(cardId)))
                .isEmpty();
        assertThat(reviewLogRepository.findByUserIdAndCardIdOrderByReviewedAtAsc(owner, UUID.fromString(cardId)))
                .isEmpty();
    }

    // ---- helpers -----------------------------------------------------------

    private ReviewLog single(UUID user, String cardId) {
        List<ReviewLog> history = reviewLogRepository
                .findByUserIdAndCardIdOrderByReviewedAtAsc(user, UUID.fromString(cardId));
        assertThat(history).hasSize(1);
        return history.get(0);
    }

    private String createDeck(UUID user, boolean isPublic) throws Exception {
        String body = "{\"title\":\"History test deck\",\"category\":\"CARDIOLOGY\",\"isPublic\":" + isPublic + "}";
        MvcResult result = mockMvc.perform(post("/api/v1/decks")
                        .header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return idFrom(result);
    }

    private String createCard(UUID user, String deckId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/decks/{deckId}/cards", deckId)
                        .header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"front\":\"Question\",\"back\":\"Answer\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return idFrom(result);
    }

    private ResultActions review(UUID user, String cardId, int quality, Integer seconds) throws Exception {
        String body = seconds == null
                ? "{\"quality\":" + quality + "}"
                : "{\"quality\":" + quality + ",\"timeSpentSeconds\":" + seconds + "}";
        return mockMvc.perform(post("/api/v1/cards/{cardId}/review", cardId)
                .header("Authorization", bearerToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String idFrom(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
}
