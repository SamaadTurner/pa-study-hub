package com.pastudyhub.flashcard.service;

import com.pastudyhub.flashcard.model.Card;
import com.pastudyhub.flashcard.model.Deck;
import com.pastudyhub.flashcard.repository.CardRepository;
import com.pastudyhub.flashcard.repository.DeckRepository;
import com.pastudyhub.flashcard.repository.ReviewLogRepository;
import com.pastudyhub.flashcard.repository.ReviewScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Other students can study a public deck without copying it. When its owner
 * deletes the deck, or their whole account, those students used to lose it and
 * their progress without a word. Now each of them gets their own private copy,
 * and their review schedules and history move over to the copied cards, so they
 * carry on where they were.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeckHandoverService {

    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;
    private final ReviewScheduleRepository reviewScheduleRepository;
    private final ReviewLogRepository reviewLogRepository;

    /** Gives a copy of the deck to everyone except its owner who has studied it. Returns how many. */
    @Transactional
    public int handOver(Deck deck) {
        List<UUID> students = reviewScheduleRepository.findStudentIds(deck.getId(), deck.getUserId());
        if (students.isEmpty()) {
            return 0;
        }
        List<Card> cards = cardRepository.findAllInDeckOrder(deck.getId());
        for (UUID student : students) {
            copyFor(deck, cards, student);
        }
        log.info("Deck handed over: deckId={}, students={}", deck.getId(), students.size());
        return students.size();
    }

    /** Before an account is deleted: hands over each of its decks that others studied. */
    @Transactional
    public void handOverAllDecksOf(UUID ownerId) {
        for (Deck deck : deckRepository.findAllActiveOf(ownerId)) {
            handOver(deck);
        }
    }

    private void copyFor(Deck source, List<Card> cards, UUID student) {
        Deck copy = deckRepository.saveAndFlush(Deck.builder()
                .userId(student)
                .title(source.getTitle())
                .description("Your own copy: the student who shared this deck removed it, so your progress was kept here.")
                .category(source.getCategory())
                .isPublic(false)
                .isDeleted(false)
                .build());

        int position = 0;
        for (Card card : cards) {
            position++;
            // saveAndFlush: the updates below point rows at this card, so it has to be
            // in the database first (a pending save is not written before them)
            Card copied = cardRepository.saveAndFlush(Card.builder()
                    .deck(copy)
                    .front(card.getFront())
                    .back(card.getBack())
                    .cardType(card.getCardType())
                    .clozeIndex(card.getClozeIndex())
                    .hint(card.getHint())
                    .imageUrl(card.getImageUrl())
                    .tags(card.getTags())
                    .isDeleted(false)
                    .positionInDeck(position)
                    .build());
            // This student's schedule and history for the card now belong to the copy
            reviewScheduleRepository.moveToCard(card, copied, student);
            reviewLogRepository.moveToCard(card, copied, student);
        }
    }
}
