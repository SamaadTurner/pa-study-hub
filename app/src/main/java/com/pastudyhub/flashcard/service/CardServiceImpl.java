package com.pastudyhub.flashcard.service;

import com.pastudyhub.flashcard.dto.CardResponse;
import com.pastudyhub.flashcard.dto.ClozeCardsRequest;
import com.pastudyhub.flashcard.model.CardImage;
import com.pastudyhub.flashcard.engine.OcclusionBoxes;
import com.pastudyhub.flashcard.dto.ImageCardsRequest;
import com.pastudyhub.flashcard.dto.UpdatePictureCardsRequest;
import com.pastudyhub.flashcard.dto.CreateCardRequest;
import com.pastudyhub.flashcard.engine.ClozeText;
import com.pastudyhub.flashcard.exception.CardNotFoundException;
import com.pastudyhub.flashcard.exception.DeckNotFoundException;
import com.pastudyhub.flashcard.exception.InvalidCardException;
import com.pastudyhub.flashcard.exception.UnauthorizedDeckAccessException;
import com.pastudyhub.flashcard.mapper.CardMapper;
import com.pastudyhub.flashcard.model.Card;
import com.pastudyhub.flashcard.model.CardType;
import com.pastudyhub.flashcard.model.Deck;
import com.pastudyhub.flashcard.model.ReviewSchedule;
import com.pastudyhub.flashcard.repository.CardRepository;
import com.pastudyhub.flashcard.repository.DeckRepository;
import com.pastudyhub.flashcard.repository.ReviewScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Service for card CRUD operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardServiceImpl {

    // The most cards one request for a deck's cards can ask for
    private static final int MAX_PAGE_SIZE = 100;

    private final CardRepository cardRepository;
    private final DeckRepository deckRepository;
    private final ReviewScheduleRepository reviewScheduleRepository;
    private final CardMapper cardMapper;
    private final CardImageService cardImageService;

    // Shown above the picture when the student did not write a question
    static final String DEFAULT_IMAGE_PROMPT = "What is under the highlighted box?";

    @Transactional(readOnly = true)
    public Page<CardResponse> getCardsForDeck(UUID deckId, UUID userId, int page, int size) {
        Deck deck = deckRepository.findByIdAndNotDeleted(deckId)
                .orElseThrow(() -> new DeckNotFoundException(deckId));

        // The cards of a private deck are only for its owner
        if (!deck.canBeViewedBy(userId)) {
            throw new UnauthorizedDeckAccessException("You do not have permission to view this deck");
        }

        // Keep odd values from the URL from turning into an error or a huge query
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        // In the order the cards were added. Without a sort the database may hand
        // the rows back in any order, and that order can change when a card is
        // edited, so a card could be on two pages or on none. The date and the id
        // only matter for two cards that somehow have the same position.
        Sort deckOrder = Sort.by(
                Sort.Order.asc("positionInDeck"), Sort.Order.asc("createdAt"), Sort.Order.asc("id"));

        return cardRepository.findByDeckIdAndNotDeleted(deckId, PageRequest.of(safePage, safeSize, deckOrder))
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

        if (request.getBack() == null || request.getBack().isBlank()) {
            throw new InvalidCardException("Card back (answer) is required");
        }
        Card card = cardMapper.toEntity(request, deck);
        // A new card goes to the end of the deck
        card.setPositionInDeck(cardRepository.findLastPositionInDeck(deckId) + 1);
        // saveAndFlush so createdAt is filled in before the response is built
        Card saved = cardRepository.saveAndFlush(card);
        log.info("Card created: id={}, deckId={}", saved.getId(), deckId);
        return cardMapper.toResponse(saved, null);
    }

    /**
     * Makes the cards of one fill-in-the-blank text: one card per blank number, in
     * number order, at the end of the deck. Every card keeps the whole text; when
     * it is studied only its own blank is hidden.
     */
    @Transactional
    public List<CardResponse> createClozeCards(UUID deckId, ClozeCardsRequest request, UUID userId) {
        Deck deck = deckRepository.findByIdAndNotDeleted(deckId)
                .orElseThrow(() -> new DeckNotFoundException(deckId));
        if (!deck.getUserId().equals(userId)) {
            throw new UnauthorizedDeckAccessException();
        }
        String problem = ClozeText.problem(request.getText());
        if (problem != null) {
            throw new InvalidCardException(problem);
        }

        String text = request.getText().trim();
        String extra = request.getExtra() == null ? "" : request.getExtra().trim();
        int position = cardRepository.findLastPositionInDeck(deckId);
        List<CardResponse> created = new ArrayList<>();
        for (int number : ClozeText.numbers(text)) {
            position++;
            Card card = Card.builder()
                    .deck(deck)
                    .front(text)
                    .back(extra)
                    .cardType(CardType.CLOZE)
                    .clozeIndex(number)
                    .positionInDeck(position)
                    .build();
            if (request.getTags() != null) {
                card.setTagsList(request.getTags());
            }
            created.add(cardMapper.toResponse(cardRepository.saveAndFlush(card), null));
        }
        log.info("Cloze cards created: {} in deckId={}", created.size(), deckId);
        return created;
    }

    /**
     * Makes the cards of one picture: one card per box, in box order, at the end of
     * the deck. Every card keeps the picture and all boxes; when it is studied, its
     * own box is the one to name.
     */
    @Transactional
    public List<CardResponse> createImageCards(UUID deckId, ImageCardsRequest request, UUID userId) {
        Deck deck = deckRepository.findByIdAndNotDeleted(deckId)
                .orElseThrow(() -> new DeckNotFoundException(deckId));
        if (!deck.getUserId().equals(userId)) {
            throw new UnauthorizedDeckAccessException();
        }
        String problem = OcclusionBoxes.problem(request.getBoxes());
        if (problem != null) {
            throw new InvalidCardException(problem);
        }
        CardImage image = cardImageService.findOwn(request.getImageId(), userId);

        String prompt = request.getPrompt() == null || request.getPrompt().isBlank()
                ? DEFAULT_IMAGE_PROMPT : request.getPrompt().trim();
        String extra = request.getExtra() == null ? "" : request.getExtra().trim();
        String boxes = OcclusionBoxes.toJson(request.getBoxes());
        UUID pictureGroup = UUID.randomUUID();
        int position = cardRepository.findLastPositionInDeck(deckId);
        List<CardResponse> created = new ArrayList<>();
        for (int number = 1; number <= request.getBoxes().size(); number++) {
            position++;
            Card card = Card.builder()
                    .deck(deck)
                    .front(prompt)
                    .back(extra)
                    .cardType(CardType.IMAGE)
                    .clozeIndex(number)
                    .imageId(image.getId())
                    .occlusionBoxes(boxes)
                    .pictureGroup(pictureGroup)
                    .positionInDeck(position)
                    .build();
            if (request.getTags() != null) {
                card.setTagsList(request.getTags());
            }
            created.add(cardMapper.toResponse(cardRepository.saveAndFlush(card), null));
        }
        log.info("Picture cards created: {} in deckId={}", created.size(), deckId);
        return created;
    }

    /**
     * Changes the boxes (and the question, notes and tags) of all cards of one picture.
     *
     * <p>A box that keeps its card keeps that card's review history, even when it was
     * moved or is now in another place in the list: the card simply asks for the box's
     * new number. A new box gets a new card at the end of the deck. A card whose box
     * is gone is deleted.
     */
    @Transactional
    public List<CardResponse> updatePictureCards(UUID cardId, UpdatePictureCardsRequest request, UUID userId) {
        Card card = cardRepository.findByIdAndNotDeleted(cardId)
                .orElseThrow(() -> new CardNotFoundException(cardId));
        Deck deck = card.getDeck();
        if (!deck.getUserId().equals(userId)) {
            throw new UnauthorizedDeckAccessException();
        }
        if (card.getCardType() != CardType.IMAGE) {
            throw new InvalidCardException("Only picture cards have boxes");
        }
        String problem = OcclusionBoxes.problem(request.plainBoxes());
        if (problem != null) {
            throw new InvalidCardException(problem);
        }

        // The cards of this picture. One made before picture groups existed stands alone.
        Map<UUID, Card> groupById = new HashMap<>();
        if (card.getPictureGroup() == null) {
            card.setPictureGroup(UUID.randomUUID());
        } else {
            cardRepository.findPictureGroup(deck.getId(), card.getPictureGroup())
                    .forEach(member -> groupById.put(member.getId(), member));
        }
        groupById.put(card.getId(), card);

        List<UpdatePictureCardsRequest.EditedBox> edited = request.getBoxes();
        Set<UUID> kept = new HashSet<>();
        for (int i = 0; i < edited.size(); i++) {
            UUID boxCard = edited.get(i).cardId();
            if (boxCard != null && (!groupById.containsKey(boxCard) || !kept.add(boxCard))) {
                throw new InvalidCardException("Box " + (i + 1) + " belongs to a card that is not part of this picture");
            }
        }

        String prompt = request.getPrompt() == null || request.getPrompt().isBlank()
                ? DEFAULT_IMAGE_PROMPT : request.getPrompt().trim();
        String extra = request.getExtra() == null ? "" : request.getExtra().trim();
        String boxes = OcclusionBoxes.toJson(request.plainBoxes());
        int position = cardRepository.findLastPositionInDeck(deck.getId());
        List<CardResponse> result = new ArrayList<>();
        for (int i = 0; i < edited.size(); i++) {
            UUID boxCard = edited.get(i).cardId();
            Card target;
            if (boxCard != null) {
                target = groupById.get(boxCard);
            } else {
                position++;
                target = Card.builder()
                        .deck(deck)
                        .cardType(CardType.IMAGE)
                        .imageId(card.getImageId())
                        .pictureGroup(card.getPictureGroup())
                        .positionInDeck(position)
                        .build();
            }
            target.setFront(prompt);
            target.setBack(extra);
            target.setClozeIndex(i + 1);
            target.setOcclusionBoxes(boxes);
            if (request.getTags() != null) {
                target.setTagsList(request.getTags());
            }
            Card saved = cardRepository.saveAndFlush(target);
            result.add(cardMapper.toResponse(saved,
                    reviewScheduleRepository.findByCardIdAndUserId(saved.getId(), userId).orElse(null)));
        }

        // Cards whose box was removed
        for (Card member : groupById.values()) {
            if (!kept.contains(member.getId())) {
                member.setDeleted(true);
                cardRepository.save(member);
            }
        }
        log.info("Picture cards updated: {} boxes, {} removed, group={}",
                edited.size(), groupById.size() - kept.size(), card.getPictureGroup());
        return result;
    }

    @Transactional
    public CardResponse updateCard(UUID cardId, CreateCardRequest request, UUID userId) {
        Card card = cardRepository.findByIdAndNotDeleted(cardId)
                .orElseThrow(() -> new CardNotFoundException(cardId));

        if (!card.getDeck().getUserId().equals(userId)) {
            throw new UnauthorizedDeckAccessException();
        }

        if (card.getCardType() == CardType.CLOZE) {
            // The card asks for one blank number; the text has to keep it
            if (request.getFront() != null && !ClozeText.numbers(request.getFront()).contains(card.getClozeIndex())) {
                throw new InvalidCardException("This card asks for blank c" + card.getClozeIndex()
                        + ", so the text needs a {{c" + card.getClozeIndex() + "::...}} blank");
            }
        } else if (card.getCardType() == CardType.BASIC && request.getBack() != null && request.getBack().isBlank()) {
            throw new InvalidCardException("Card back (answer) is required");
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
