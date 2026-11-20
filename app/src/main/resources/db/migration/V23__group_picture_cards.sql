-- The cards made from one picture belong together: editing the boxes changes all
-- of them at once. picture_group is the same for every card of one picture in one
-- deck (copies of a deck keep it; the deck id tells the copies apart).
ALTER TABLE cards ADD COLUMN picture_group UUID;

-- Picture cards made before this: one group per picture per deck
UPDATE cards c
SET picture_group = g.group_id
FROM (SELECT deck_id, image_id, gen_random_uuid() AS group_id
      FROM cards
      WHERE card_type = 'IMAGE' AND image_id IS NOT NULL
      GROUP BY deck_id, image_id) g
WHERE c.card_type = 'IMAGE' AND c.deck_id = g.deck_id AND c.image_id = g.image_id;

CREATE INDEX idx_cards_picture_group ON cards (deck_id, picture_group);
