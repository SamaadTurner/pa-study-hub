package com.pastudyhub.flashcard.service;

import com.pastudyhub.flashcard.dto.CardResponse;
import com.pastudyhub.flashcard.dto.CreateCardRequest;
import com.pastudyhub.flashcard.exception.CardNotFoundException;
import com.pastudyhub.flashcard.exception.DeckNotFoundException;
import com.pastudyhub.flashcard.exception.UnauthorizedDeckAccessException;
import com.pastudyhub.flashcard.mapper.CardMapper;
import com.pastudyhub.flashcard.model.Card;
import com.pastudyhub.flashcard.model.Deck;
import com.pastudyhub.flashcard.model.ReviewSchedule;
import com.pastudyhub.flashcard.repository.CardRepository;
import com.pastudyhub.flashcard.repository.DeckRepository;
import com.pastudyhub.flashcard.repository.ReviewScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Service for card CRUD operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardServiceImpl {

    private final CardRepository cardRepository;
    private final DeckRepository deckRepository;
    private final ReviewScheduleRepository reviewScheduleRepository;
    private final CardMapper cardMapper;

    @Transactional(readOnly = true)
    public Page<CardResponse> getCardsForDeck(UUID deckId, UUID userId, int page, int size) {
        deckRepository.findByIdAndNotDeleted(deckId)
                .orElseThrow(() -> new DeckNotFoundException(deckId));

        return cardRepository.findByDeckIdAndNotDeleted(deckId, PageRequest.of(page, size))
                .map(card -> {
                    Optional<ReviewSchedule> schedule = reviewScheduleRepository
                            .findByCardIdAndUserId(card.getId(), userId);
                    return cardMapper.toResponse(card, schedule.orElse(null));
                });
    }

    @Transactional
    public CardResponse createCard(UUID deckId, CreateCardRequest request, UUID userId) {
        Deck deck = deckRepository.findByIdAndNotDeleted(deckId)
                .orElseThrow(() -> new DeckNotFoundException(deckId));

        if (!deck.getUserId().equals(userId)) {
            throw new UnauthorizedDeckAccessException();
        }

        Card card = cardMapper.toEntity(request, deck);
        Card saved = cardRepository.save(card);
        log.info("Card created: id={}, deckId={}", saved.getId(), deckId);
        return cardMapper.toResponse(saved, null);
    }

    @Transactional
    public CardResponse updateCard(UUID cardId, CreateCardRequest request, UUID userId) {
        Card card = cardRepository.findByIdAndNotDeleted(cardId)
                .orElseThrow(() -> new CardNotFoundException(cardId));

        if (!card.getDeck().getUserId().equals(userId)) {
            throw new UnauthorizedDeckAccessException();
        }

        if (request.getFront() != null) card.setFront(request.getFront().trim());
        if (request.getBack() != null) card.setBack(request.getBack().trim());
        if (request.getHint() != null) card.setHint(request.getHint());
        if (request.getImageUrl() != null) card.setImageUrl(request.getImageUrl());
        if (request.getTags() != null) card.setTagsList(request.getTags());

        Card saved = cardRepository.save(card);
        Optional<ReviewSchedule> schedule = reviewScheduleRepository
                .findByCardIdAndUserId(cardId, userId);
        return cardMapper.toResponse(saved, schedule.orElse(null));
    }

    @Transactional
    public void deleteCard(UUID cardId, UUID userId) {
        Card card = cardRepository.findByIdAndNotDeleted(cardId)
                .orElseThrow(() -> new CardNotFoundException(cardId));

        if (!card.getDeck().getUserId().equals(userId)) {
            throw new UnauthorizedDeckAccessException();
        }

        card.setDeleted(true);
        cardRepository.save(card);
        log.info("Card soft-deleted: id={}", cardId);
    }
}
