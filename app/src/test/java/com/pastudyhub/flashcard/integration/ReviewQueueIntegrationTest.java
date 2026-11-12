package com.pastudyhub.flashcard.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pastudyhub.BaseIntegrationTest;
import com.pastudyhub.flashcard.dto.CreateCardRequest;
import com.pastudyhub.flashcard.dto.CreateDeckRequest;
import com.pastudyhub.flashcard.dto.ReviewRequest;
import com.pastudyhub.flashcard.model.MedicalCategory;
import com.pastudyhub.flashcard.model.ReviewSchedule;
import com.pastudyhub.flashcard.repository.ReviewScheduleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for GET /decks/{deckId}/review, the list of cards to study right now.
 *
 * <p>The list used to contain only cards that already had a review schedule. A card
 * gets its schedule on its first review, so a new deck always came back empty and
 * there was no way to start studying it. These tests cover new cards being included.
 */
@DisplayName("Review queue")
class ReviewQueueIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReviewScheduleRepository reviewScheduleRepository;

    @Test
    @DisplayName("A brand new card is ready to review")
    void newCard_isInTheQueue() throws Exception {
        UUID user = UUID.randomUUID();
        String deckId = createDeck(user, false);
        String cardId = createCard(user, deckId, "What does S3 suggest?");

        mockMvc.perform(get("/api/v1/decks/{deckId}/review", deckId)
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(cardId))
                .andExpect(jsonPath("$[0].front").value("What does S3 suggest?"))
                // never reviewed, so there is no schedule to show yet
                .andExpect(jsonPath("$[0].reviewSchedule").doesNotExist());
    }

    @Test
    @DisplayName("The deck's cards-to-review count includes new cards")
    void deckCount_includesNewCards() throws Exception {
        UUID user = UUID.randomUUID();
        String deckId = createDeck(user, false);
        createCard(user, deckId, "Card one");
        createCard(user, deckId, "Card two");

        mockMvc.perform(get("/api/v1/decks/{deckId}", deckId)
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardCount").value(2))
                .andExpect(jsonPath("$.cardsToReview").value(2));
    }

    @Test
    @DisplayName("A card leaves the queue once it has been reviewed")
    void reviewedCard_leavesTheQueue() throws Exception {
        UUID user = UUID.randomUUID();
        String deckId = createDeck(user, false);
        String reviewed = createCard(user, deckId, "Reviewed card");
        String stillNew = createCard(user, deckId, "Still new");

        submitReview(user, reviewed, 4);

        // "Good" schedules the card for tomorrow, so only the other card is left today
        mockMvc.perform(get("/api/v1/decks/{deckId}/review", deckId)
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(stillNew));

        mockMvc.perform(get("/api/v1/decks/{deckId}", deckId)
                        .header("Authorization", bearerToken(user)))
                .andExpect(jsonPath("$.cardsToReview").value(1));
    }

    @Test
    @DisplayName("Cards that are due come before new cards")
    void dueCards_comeBeforeNewCards() throws Exception {
        UUID user = UUID.randomUUID();
        String deckId = createDeck(user, false);
        String newCard = createCard(user, deckId, "New card, made first");
        String dueCard = createCard(user, deckId, "Due card, made second");

        // Review one card, then move its next review date to yesterday so it is due again
        submitReview(user, dueCard, 4);
        ReviewSchedule schedule = reviewScheduleRepository
                .findByCardIdAndUserId(UUID.fromString(dueCard), user)
                .orElseThrow();
        schedule.setNextReviewDate(LocalDate.now().minusDays(1));
        reviewScheduleRepository.save(schedule);

        mockMvc.perform(get("/api/v1/decks/{deckId}/review", deckId)
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(dueCard))
                .andExpect(jsonPath("$[0].reviewSchedule.repetitions").value(1))
                .andExpect(jsonPath("$[1].id").value(newCard));
    }

    @Test
    @DisplayName("One session never hands out more than 20 cards")
    void queue_isCappedAt20() throws Exception {
        UUID user = UUID.randomUUID();
        String deckId = createDeck(user, false);
        for (int i = 1; i <= 22; i++) {
            createCard(user, deckId, "Card " + i);
        }

        mockMvc.perform(get("/api/v1/decks/{deckId}/review", deckId)
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(20)));

        // The deck still says how many are waiting in total
        mockMvc.perform(get("/api/v1/decks/{deckId}", deckId)
                        .header("Authorization", bearerToken(user)))
                .andExpect(jsonPath("$.cardsToReview").value(22));
    }

    @Test
    @DisplayName("A deleted card is not in the queue")
    void deletedCard_isNotInTheQueue() throws Exception {
        UUID user = UUID.randomUUID();
        String deckId = createDeck(user, false);
        String cardId = createCard(user, deckId, "About to be deleted");

        mockMvc.perform(delete("/api/v1/cards/{cardId}", cardId)
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/decks/{deckId}/review", deckId)
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Someone else's private deck can't be studied")
    void privateDeckOfAnotherUser_returns403() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();
        String deckId = createDeck(owner, false);
        createCard(owner, deckId, "Private card");

        mockMvc.perform(get("/api/v1/decks/{deckId}/review", deckId)
                        .header("Authorization", bearerToken(stranger)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A public deck can be studied by anyone, each with their own progress")
    void publicDeck_canBeStudiedByOthers() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();
        String deckId = createDeck(owner, true);
        String cardId = createCard(owner, deckId, "Shared card");

        // The owner has reviewed it, so it is done for the owner today...
        submitReview(owner, cardId, 5);
        mockMvc.perform(get("/api/v1/decks/{deckId}/review", deckId)
                        .header("Authorization", bearerToken(owner)))
                .andExpect(jsonPath("$", hasSize(0)));

        // ...but it is still a new card for the other user
        mockMvc.perform(get("/api/v1/decks/{deckId}/review", deckId)
                        .header("Authorization", bearerToken(otherUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(cardId));
    }

    @Test
    @DisplayName("A deck that doesn't exist returns 404")
    void missingDeck_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/decks/{deckId}/review", UUID.randomUUID())
                        .header("Authorization", bearerToken(UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    // ---- helpers -----------------------------------------------------------

    private String createDeck(UUID user, boolean isPublic) throws Exception {
        CreateDeckRequest request = CreateDeckRequest.builder()
                .title("Review queue deck")
                .category(MedicalCategory.CARDIOLOGY)
                .isPublic(isPublic)
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
                .andExpect(status().isOk());
    }

    private String idFrom(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
    }
}
