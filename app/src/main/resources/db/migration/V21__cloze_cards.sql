-- Fill-in-the-blank cards. A cloze card keeps its whole text (with {{c1::...}}
-- markup) in "front", optional extra notes in "back", and in cloze_index the
-- number of the blank this card asks for. One text with c1 and c2 makes two cards.
ALTER TABLE cards ADD COLUMN card_type VARCHAR(10) NOT NULL DEFAULT 'BASIC';
ALTER TABLE cards ADD COLUMN cloze_index INT;
