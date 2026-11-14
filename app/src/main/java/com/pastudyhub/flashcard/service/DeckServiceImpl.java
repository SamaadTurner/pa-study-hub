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

    // The most decks one request for a list of decks can ask for
    private static final int MAX_PAGE_SIZE = 100;

    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;
    private final ReviewScheduleRepository reviewScheduleRepository;
    private final DeckMapper deckMapper;
    private final CardMapper cardMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<DeckResponse> getUserDecks(UUID userId, MedicalCategory category, int page, int size) {
        return deckRepository.findByUserIdAndNotDeleted(userId, category, newestFirst(page, size))
                .map(deck -> {
                    int cardCount = cardRepository.countByDeckIdAndNotDeleted(deck.getId());
                    int cardsToReview = countCardsToReview(deck.getId(), userId);
                    return deckMapper.toResponse(deck, cardCount, cardsToReview);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public DeckResponse getDeck(UUID deckId, UUID userId) {
        Deck deck = deckRepository.findByIdAndNotDeleted(deckId)
                .orElseThrow(() -> new DeckNotFoundException(deckId));
        checkCanView(deck, userId);

        int cardCount = cardRepository.countByDeckIdAndNotDeleted(deckId);
        int cardsToReview = countCardsToReview(deckId, userId);
        return deckMapper.toResponse(deck, cardCount, cardsToReview);
    }

    @Override
    @Transactional
    public DeckResponse createDeck(CreateDeckRequest request, UUID userId) {
        Deck deck = deckMapper.toEntity(request, userId);
        // saveAndFlush so the created/updated dates are filled in before the response
        // is built. With a plain save they are still null at this point.
        Deck saved = deckRepository.saveAndFlush(deck);
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
        deck.setPublic(request.isPublic());

        Deck saved = deckRepository.saveAndFlush(deck);
        int cardCount = cardRepository.countByDeckIdAndNotDeleted(deckId);
        int cardsToReview = countCardsToReview(deckId, userId);
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
    public Page<DeckResponse> getPublicDecks(String search, MedicalCategory category, int page, int size) {
        return deckRepository.findPublicDecks(category, toLikePattern(search), newestFirst(page, size))
                .map(deck -> {
                    int cardCount = cardRepository.countByDeckIdAndNotDeleted(deck.getId());
                    return deckMapper.toResponse(deck, cardCount, 0);
                });
    }

    // One page of decks, the most recently changed first.
    private Pageable newestFirst(int page, int size) {
        // Keep odd values from the URL from turning into an error or a huge query
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        // The id is a second sort key so that decks saved at the same moment always
        // come back in the same order, otherwise a deck could show up on two pages
        // or on none.
        Sort sort = Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.asc("id"));
        return PageRequest.of(safePage, safeSize, sort);
    }

    // Turns what the user typed into a "contains this text" pattern for LIKE.
    // % and _ mean "anything" in LIKE, so they are escaped with ! to be searched
    // for as plain characters. No search text gives "%", which matches every deck.
    private String toLikePattern(String search) {
        if (search == null || search.isBlank()) {
            return "%";
        }
        String escaped = search.trim().toLowerCase()
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escaped + "%";
    }

    @Override
    @Transactional
    public DeckResponse cloneDeck(UUID sourceDeckId, UUID userId) {
        Deck source = deckRepository.findByIdAndNotDeleted(sourceDeckId)
                .orElseThrow(() -> new DeckNotFoundException(sourceDeckId));

        checkCanView(source, userId);

        // Create a new deck for this user
        Deck clone = Deck.builder()
                .userId(userId)
                .title(source.getTitle() + " (Clone)")
                .description(source.getDescription())
                .category(source.getCategory())
                .isPublic(false)
                .isDeleted(false)
                .build();
        Deck savedClone = deckRepository.saveAndFlush(clone);

        // Copy the cards in the order they have in the source deck. The copies are
        // numbered 1, 2, 3... so the new deck shows them in that same order.
        int position = 0;
        for (Card sourceCard : cardRepository.findAllInDeckOrder(sourceDeckId)) {
            position++;
            Card clonedCard = Card.builder()
                    .deck(savedClone)
                    .front(sourceCard.getFront())
                    .back(sourceCard.getBack())
                    .hint(sourceCard.getHint())
                    .imageUrl(sourceCard.getImageUrl())
                    .tags(sourceCard.getTags())
                    .isDeleted(false)
                    .positionInDeck(position)
                    .build();
            cardRepository.save(clonedCard);
        }

        int cardCount = cardRepository.countByDeckIdAndNotDeleted(savedClone.getId());
        log.info("Deck cloned: sourceId={}, cloneId={}, userId={}", sourceDeckId, savedClone.getId(), userId);
        // Every card in a fresh copy is new to this user, so all of them are ready to study
        return deckMapper.toResponse(savedClone, cardCount, cardCount);
    }

    @Override
    @Transactional(readOnly = true)
    public DeckStatsResponse getDeckStats(UUID deckId, UUID userId) {
        Deck deck = deckRepository.findByIdAndNotDeleted(deckId)
                .orElseThrow(() -> new DeckNotFoundException(deckId));
        checkCanView(deck, userId);

        int totalCards = cardRepository.countByDeckIdAndNotDeleted(deckId);
        int cardsDueToday = countCardsToReview(deckId, userId);
        int cardsMastered = reviewScheduleRepository.countMastered(deckId, userId);
        Double avgEaseFactor = reviewScheduleRepository.findAverageEaseFactorForDeck(deckId, userId);

        return DeckStatsResponse.builder()
                .totalCards(totalCards)
                .cardsDueToday(cardsDueToday)
                .cardsMastered(cardsMastered)
                .averageEaseFactor(avgEaseFactor != null ? avgEaseFactor : 2.5)
                .build();
    }

    // A private deck can only be opened by its owner. Before this check anyone who was
    // logged in and had the deck's ID could read it.
    private void checkCanView(Deck deck, UUID userId) {
        if (!deck.canBeViewedBy(userId)) {
            throw new UnauthorizedDeckAccessException("You do not have permission to view this deck");
        }
    }

    // Cards to study today: the ones that are due plus the ones never reviewed.
    // This has to match what ReviewServiceImpl.getCardsForReview hands out.
    private int countCardsToReview(UUID deckId, UUID userId) {
        int due = reviewScheduleRepository.countDueForReview(deckId, userId, LocalDate.now());
        int neverReviewed = cardRepository.countNeverReviewed(deckId, userId);
        return due + neverReviewed;
    }
}
