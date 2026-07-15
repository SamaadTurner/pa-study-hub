package com.pastudyhub.flashcard.service;

import com.pastudyhub.flashcard.dto.CreateDeckRequest;
import com.pastudyhub.flashcard.dto.DeckResponse;
import com.pastudyhub.flashcard.dto.DeckStatsResponse;
import com.pastudyhub.flashcard.exception.DeckNotFoundException;
import com.pastudyhub.flashcard.exception.UnauthorizedDeckAccessException;
import com.pastudyhub.flashcard.mapper.CardMapper;
import com.pastudyhub.flashcard.mapper.DeckMapper;
import com.pastudyhub.flashcard.model.Deck;
import com.pastudyhub.flashcard.model.MedicalCategory;
import com.pastudyhub.flashcard.repository.CardRepository;
import com.pastudyhub.flashcard.repository.DeckRepository;
import com.pastudyhub.flashcard.repository.ReviewScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeckServiceImpl unit tests")
class DeckServiceImplTest {

    @Mock
    private DeckRepository deckRepository;
    @Mock
    private CardRepository cardRepository;
    @Mock
    private ReviewScheduleRepository reviewScheduleRepository;
    @Mock
    private DeckMapper deckMapper;
    @Mock
    private CardMapper cardMapper;

    @InjectMocks
    private DeckServiceImpl deckService;

    private UUID userId;
    private UUID deckId;
    private Deck deck;
    private DeckResponse deckResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        deckId = UUID.randomUUID();

        deck = Deck.builder()
                .id(deckId)
                .userId(userId)
                .title("Test Deck")
                .description("A test deck")
                .category(MedicalCategory.CARDIOLOGY)
                .isPublic(false)
                .isDeleted(false)
                .build();

        deckResponse = DeckResponse.builder()
                .id(deckId)
                .userId(userId)
                .title("Test Deck")
                .description("A test deck")
                .category(MedicalCategory.CARDIOLOGY)
                .isPublic(false)
                .cardCount(0)
                .cardsToReview(0)
                .build();
    }

    // ---- createDeck --------------------------------------------------------

    @Test
    @DisplayName("createDeck: returns DeckResponse with id and cardCount=0")
    void createDeck_success() {
        CreateDeckRequest request = CreateDeckRequest.builder()
                .title("Test Deck")
                .description("A test deck")
                .category(MedicalCategory.CARDIOLOGY)
                .isPublic(false)
                .build();

        when(deckMapper.toEntity(request, userId)).thenReturn(deck);
        when(deckRepository.save(deck)).thenReturn(deck);
        when(deckMapper.toResponse(deck, 0, 0)).thenReturn(deckResponse);

        DeckResponse result = deckService.createDeck(request, userId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(deckId);
        assertThat(result.getCardCount()).isEqualTo(0);
        assertThat(result.getCardsToReview()).isEqualTo(0);

        verify(deckRepository).save(deck);
    }

    // ---- getDeck -----------------------------------------------------------

    @Test
    @DisplayName("getDeck: returns DeckResponse for valid deckId")
    void getDeck_success() {
        when(deckRepository.findByIdAndNotDeleted(deckId)).thenReturn(Optional.of(deck));
        when(cardRepository.countByDeckIdAndNotDeleted(deckId)).thenReturn(5);
        when(reviewScheduleRepository.countDueForReview(eq(deckId), eq(userId), any())).thenReturn(2);
        when(deckMapper.toResponse(deck, 5, 2)).thenReturn(
                DeckResponse.builder().id(deckId).cardCount(5).cardsToReview(2).build());

        DeckResponse result = deckService.getDeck(deckId, userId);

        assertThat(result.getId()).isEqualTo(deckId);
        assertThat(result.getCardCount()).isEqualTo(5);
        assertThat(result.getCardsToReview()).isEqualTo(2);
    }

    @Test
    @DisplayName("getDeck: throws DeckNotFoundException when deck not found")
    void getDeck_notFound_throws() {
        when(deckRepository.findByIdAndNotDeleted(deckId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deckService.getDeck(deckId, userId))
                .isInstanceOf(DeckNotFoundException.class);
    }

    // ---- updateDeck --------------------------------------------------------

    @Test
    @DisplayName("updateDeck: updates title and returns DeckResponse")
    void updateDeck_success() {
        CreateDeckRequest request = CreateDeckRequest.builder()
                .title("Updated Title")
                .build();

        when(deckRepository.findByIdAndNotDeleted(deckId)).thenReturn(Optional.of(deck));
        when(deckRepository.save(deck)).thenReturn(deck);
        when(cardRepository.countByDeckIdAndNotDeleted(deckId)).thenReturn(3);
        when(reviewScheduleRepository.countDueForReview(eq(deckId), eq(userId), any())).thenReturn(1);

        DeckResponse updatedResponse = DeckResponse.builder()
                .id(deckId)
                .title("Updated Title")
                .cardCount(3)
                .cardsToReview(1)
                .build();
        when(deckMapper.toResponse(deck, 3, 1)).thenReturn(updatedResponse);

        DeckResponse result = deckService.updateDeck(deckId, request, userId);

        assertThat(result.getTitle()).isEqualTo("Updated Title");
        verify(deckRepository).save(deck);
    }

    @Test
    @DisplayName("updateDeck: throws UnauthorizedDeckAccessException when different user")
    void updateDeck_wrongUser_throws() {
        UUID differentUserId = UUID.randomUUID();
        when(deckRepository.findByIdAndNotDeleted(deckId)).thenReturn(Optional.of(deck));

        assertThatThrownBy(() -> deckService.updateDeck(deckId, new CreateDeckRequest(), differentUserId))
                .isInstanceOf(UnauthorizedDeckAccessException.class);

        verify(deckRepository, never()).save(any());
    }

    // ---- deleteDeck --------------------------------------------------------

    @Test
    @DisplayName("deleteDeck: soft-deletes deck for owner")
    void deleteDeck_success() {
        when(deckRepository.findByIdAndNotDeleted(deckId)).thenReturn(Optional.of(deck));
        when(deckRepository.save(deck)).thenReturn(deck);

        deckService.deleteDeck(deckId, userId);

        assertThat(deck.isDeleted()).isTrue();
        verify(deckRepository).save(deck);
    }

    @Test
    @DisplayName("deleteDeck: throws DeckNotFoundException for missing deck")
    void deleteDeck_notFound_throws() {
        when(deckRepository.findByIdAndNotDeleted(deckId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deckService.deleteDeck(deckId, userId))
                .isInstanceOf(DeckNotFoundException.class);
    }

    @Test
    @DisplayName("deleteDeck: throws UnauthorizedDeckAccessException for non-owner")
    void deleteDeck_wrongUser_throws() {
        UUID intruder = UUID.randomUUID();
        when(deckRepository.findByIdAndNotDeleted(deckId)).thenReturn(Optional.of(deck));

        assertThatThrownBy(() -> deckService.deleteDeck(deckId, intruder))
                .isInstanceOf(UnauthorizedDeckAccessException.class);

        assertThat(deck.isDeleted()).isFalse();  // deck was NOT soft-deleted
    }

    // ---- getDeckStats ------------------------------------------------------

    @Test
    @DisplayName("getDeckStats: returns aggregated stats")
    void getDeckStats_success() {
        when(deckRepository.findByIdAndNotDeleted(deckId)).thenReturn(Optional.of(deck));
        when(cardRepository.countByDeckIdAndNotDeleted(deckId)).thenReturn(10);
        when(reviewScheduleRepository.countDueForReview(eq(deckId), eq(userId), any())).thenReturn(3);
        when(reviewScheduleRepository.countMastered(deckId, userId)).thenReturn(4);
        when(reviewScheduleRepository.findAverageEaseFactorForDeck(deckId, userId)).thenReturn(2.7);

        DeckStatsResponse stats = deckService.getDeckStats(deckId, userId);

        assertThat(stats.getTotalCards()).isEqualTo(10);
        assertThat(stats.getCardsDueToday()).isEqualTo(3);
        assertThat(stats.getCardsMastered()).isEqualTo(4);
        assertThat(stats.getAverageEaseFactor()).isEqualTo(2.7);
    }

    @Test
    @DisplayName("getDeckStats: defaults averageEaseFactor to 2.5 when no review data")
    void getDeckStats_noReviewData_defaultsEaseFactor() {
        when(deckRepository.findByIdAndNotDeleted(deckId)).thenReturn(Optional.of(deck));
        when(cardRepository.countByDeckIdAndNotDeleted(deckId)).thenReturn(5);
        when(reviewScheduleRepository.countDueForReview(eq(deckId), eq(userId), any())).thenReturn(0);
        when(reviewScheduleRepository.countMastered(deckId, userId)).thenReturn(0);
        when(reviewScheduleRepository.findAverageEaseFactorForDeck(deckId, userId)).thenReturn(null);

        DeckStatsResponse stats = deckService.getDeckStats(deckId, userId);

        assertThat(stats.getAverageEaseFactor()).isEqualTo(2.5);
    }
}
