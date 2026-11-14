-- ============================================================
-- Give every card a position in its deck
-- ============================================================
-- The card list had no fixed order. Sorting by created_at is not
-- enough: when a deck is copied all its cards are saved in the
-- same instant, so their dates are equal and the copy would come
-- back in a random order. A number that counts up per deck does
-- not have that problem.

ALTER TABLE cards ADD COLUMN position_in_deck INT NOT NULL DEFAULT 0;

-- Number the cards that already exist: 1, 2, 3... per deck, oldest first
UPDATE cards
SET position_in_deck = numbered.new_position
FROM (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY deck_id ORDER BY created_at, id) AS new_position
    FROM cards
) AS numbered
WHERE cards.id = numbered.id;

CREATE INDEX idx_cards_deck_position ON cards (deck_id, position_in_deck);
