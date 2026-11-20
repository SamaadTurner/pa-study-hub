-- Picture cards (image occlusion). A student uploads a picture (anatomy, an ECG,
-- an X-ray), draws boxes over the parts to learn, and every box becomes a card
-- that hides that part.
--
-- The pictures are kept in the database. The free server's disk is wiped on every
-- restart, and this keeps the app free of a separate storage service. The browser
-- shrinks each picture before upload (to about 100 to 300 KB), and the app caps
-- the count per user and the total size.
CREATE TABLE card_images (
    id           UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    -- Null once the owner has closed their account but copies of their cards still use it.
    -- No foreign key, like decks.user_id; AccountDeletionService clears it.
    owner_id     UUID,
    content_type VARCHAR(20)  NOT NULL,
    width        INT          NOT NULL,
    height       INT          NOT NULL,
    size_bytes   INT          NOT NULL,
    data         BYTEA        NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_card_images_owner ON card_images (owner_id);

-- A picture card keeps the picture, all of its boxes (JSON, positions as fractions
-- of the picture's width and height), and in cloze_index the number of the box it asks for.
ALTER TABLE cards ADD COLUMN image_id UUID REFERENCES card_images(id) ON DELETE SET NULL;
ALTER TABLE cards ADD COLUMN occlusion_boxes TEXT;

CREATE INDEX idx_cards_image_id ON cards (image_id);
