package com.pastudyhub.flashcard.service;

import com.pastudyhub.flashcard.dto.*;
import com.pastudyhub.flashcard.exception.DeckNotFoundException;
import com.pastudyhub.flashcard.exception.UnauthorizedDeckAccessException;
import com.pastudyhub.flashcard.mapper.CardMapper;
import com.pastudyhub.flashcard.mapper.DeckMapper;
import com.pastudyhub.flashcard.model.Card;
import com.pastudyhub.flashcard.model.Deck;
import com.pastudyhub.flashcard.model.MedicalCategory;
import com.pastudyhub.flashcard.model.ReviewSchedule;
import com.pastudyhub.flashcard.repository.CardRepository;
import com.pastudyhub.flashcard.repository.DeckRepository;
import com.pastudyhub.flashcard.repository.ReviewScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of deck management operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeckServiceImpl implements DeckService {

    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;
    private final ReviewScheduleRepository reviewScheduleRepository;
    private final DeckMapper deckMapper;
    private final CardMapper cardMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<DeckResponse> getUserDecks(UUID userId, MedicalCategory category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
        return deckRepository.findByUserIdAndNotDeleted(userId, category, pageable)
                .map(deck -> {
                    int cardCount = cardRepository.countByDeckIdAndNotDeleted(deck.getId());
                    int cardsToReview = reviewScheduleRepository.countDueForReview(
                            deck.getId(), userId, LocalDate.now());
                    return deckMapper.toResponse(deck, cardCount, cardsToReview);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public DeckResponse getDeck(UUID deckId, UUID userId) {
        Deck deck = deckRepository.findByIdAndNotDeleted(deckId)
                .orElseThrow(() -> new DeckNotFoundException(deckId));
        int cardCount = cardRepository.countByDeckIdAndNotDeleted(deckId);
        int cardsToReview = reviewScheduleRepository.countDueForReview(deckId, userId, LocalDate.now());
        return deckMapper.toResponse(deck, cardCount, cardsToReview);
    }

    @Override
    @Transactional
    public DeckResponse createDeck(CreateDeckRequest request, UUID userId) {
        Deck deck = deckMapper.toEntity(request, userId);
        Deck saved = deckRepository.save(deck);
        log.info("Deck created: id={}, userId={}, title={}", saved.getId(), userId, saved.getTitle());
        return deckMapper.toResponse(saved, 0, 0);
    }

    @Override
    @Transactional
    public DeckResponse updateDeck(UUID deckId, CreateDeckRequest request, UUID userId) {
        Deck deck = deckRepository.findByIdAndNotDeleted(deckId)
                .orElseThrow(() -> new DeckNotFoundException(deckId));

        if (!deck.getUserId().equals(userId)) {
            throw new UnauthorizedDeckAccessException();
        }

        if (request.getTitle() != null) deck.setTitle(request.getTitle().trim());
        if (request.getDescription() != null) deck.setDescription(request.getDescription());
        if (request.getCategory() != null) deck.setCategory(request.getCategory());

        Deck saved = deckRepository.save(deck);
        int cardCount = cardRepository.countByDeckIdAndNotDeleted(deckId);
        int cardsToReview = reviewScheduleRepository.countDueForReview(deckId, userId, LocalDate.now());
        return deckMapper.toResponse(saved, cardCount, cardsToReview);
    }

    @Override
    @Transactional
    public void deleteDeck(UUID deckId, UUID userId) {
        Deck deck = deckRepository.findByIdAndNotDeleted(deckId)
                .orElseThrow(() -> new DeckNotFoundException(deckId));

        if (!deck.getUserId().equals(userId)) {
            throw new UnauthorizedDeckAccessException();
        }

        deck.setDeleted(true);
        deckRepository.save(deck);
        log.info("Deck soft-deleted: id={}, userId={}", deckId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DeckResponse> getPublicDecks(MedicalCategory category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
        return deckRepository.findPublicDecks(category, pageable)
                .map(deck -> {
                    int cardCount = cardRepository.countByDeckIdAndNotDeleted(deck.getId());
                    return deckMapper.toResponse(deck, cardCount, 0);
                });
    }

    @Override
    @Transactional
    public DeckResponse cloneDeck(UUID sourceDeckId, UUID userId) {
        Deck source = deckRepository.findByIdAndNotDeleted(sourceDeckId)
                .orElseThrow(() -> new DeckNotFoundException(sourceDeckId));

        if (!source.isPublic() && !source.getUserId().equals(userId)) {
            throw new UnauthorizedDeckAccessException();
        }

        // Create a new deck for this user
        Deck clone = Deck.builder()
                .userId(userId)
                .title(source.getTitle() + " (Clone)")
                .description(source.getDescription())
                .category(source.getCategory())
                .isPublic(false)
                .isDeleted(false)
                .build();
        Deck savedClone = deckRepository.save(clone);

        // Clone all cards from the source deck
        List<UUID> sourceCardIds = cardRepository.findCardIdsByDeckId(sourceDeckId);
        for (UUID cardId : sourceCardIds) {
            cardRepository.findByIdAndNotDeleted(cardId).ifPresent(sourceCard -> {
                Card clonedCard = Card.builder()
                        .deck(savedClone)
                        .front(sourceCard.getFront())
                        .back(sourceCard.getBack())
                        .hint(sourceCard.getHint())
                        .imageUrl(sourceCard.getImageUrl())
                        .tags(sourceCard.getTags())
                        .isDeleted(false)
                        .build();
                cardRepository.save(clonedCard);
            });
        }

        int cardCount = cardRepository.countByDeckIdAndNotDeleted(savedClone.getId());
        log.info("Deck cloned: sourceId={}, cloneId={}, userId={}", sourceDeckId, savedClone.getId(), userId);
        return deckMapper.toResponse(savedClone, cardCount, 0);
    }

    @Override
    @Transactional(readOnly = true)
    public DeckStatsResponse getDeckStats(UUID deckId, UUID userId) {
        deckRepository.findByIdAndNotDeleted(deckId)
                .orElseThrow(() -> new DeckNotFoundException(deckId));

        int totalCards = cardRepository.countByDeckIdAndNotDeleted(deckId);
        int cardsDueToday = reviewScheduleRepository.countDueForReview(deckId, userId, LocalDate.now());
        int cardsMastered = reviewScheduleRepository.countMastered(deckId, userId);
        Double avgEaseFactor = reviewScheduleRepository.findAverageEaseFactorForDeck(deckId, userId);

        return DeckStatsResponse.builder()
                .totalCards(totalCards)
                .cardsDueToday(cardsDueToday)
                .cardsMastered(cardsMastered)
                .averageEaseFactor(avgEaseFactor != null ? avgEaseFactor : 2.5)
                .build();
    }
}
