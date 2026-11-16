package com.pastudyhub.flashcard.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pastudyhub.BaseIntegrationTest;
import com.pastudyhub.flashcard.model.ReviewSchedule;
import com.pastudyhub.flashcard.repository.ReviewScheduleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FSRS is the default since session 20, but SM-2 can still be chosen with
 * scheduling.algorithm=sm2. These reviews run with SM-2 in charge.
 */
@DisplayName("Reviews scheduled by SM-2 (when chosen)")
@TestPropertySource(properties = "scheduling.algorithm=sm2")
class Sm2SchedulingIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReviewScheduleRepository reviewScheduleRepository;

    @Test
    @DisplayName("SM-2 sets the date, and FSRS still keeps its memory of the card")
    void sm2DecidesAndFsrsKeepsItsMemory() throws Exception {
        UUID user = UUID.randomUUID();
        String cardId = createCard(user);

        mockMvc.perform(post("/api/v1/cards/{cardId}/review", cardId)
                        .header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quality\":4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interval").value(1))
                .andExpect(jsonPath("$.message").value("Review tomorrow"));

        ReviewSchedule schedule = reviewScheduleRepository
                .findByCardIdAndUserId(UUID.fromString(cardId), user).orElseThrow();
        // SM-2 decides: 1 day after the first Good
        assertThat(schedule.getPlannedIntervalDays()).isEqualTo(1);
        assertThat(schedule.getNextReviewDate()).isEqualTo(LocalDate.now().plusDays(1));
        // ...and FSRS has its numbers ready in case the setting is switched back
        assertThat(schedule.getFsrsStability()).isEqualTo(2.3065);
        assertThat(schedule.getFsrsDifficulty()).isNotNull();
    }

    private String createCard(UUID user) throws Exception {
        MvcResult deck = mockMvc.perform(post("/api/v1/decks")
                        .header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"SM-2 deck\",\"category\":\"CARDIOLOGY\",\"isPublic\":false}"))
                .andExpect(status().isCreated())
                .andReturn();
        String deckId = objectMapper.readTree(deck.getResponse().getContentAsString()).get("id").asText();

        MvcResult card = mockMvc.perform(post("/api/v1/decks/{deckId}/cards", deckId)
                        .header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"front\":\"Question\",\"back\":\"Answer\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(card.getResponse().getContentAsString()).get("id").asText();
    }
}
