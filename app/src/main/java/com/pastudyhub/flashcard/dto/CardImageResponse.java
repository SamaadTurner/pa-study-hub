package com.pastudyhub.flashcard.dto;

import java.util.UUID;

/** What the upload returns: the picture's id (used when the cards are made) and its size. */
public record CardImageResponse(UUID id, int width, int height, int sizeBytes) {
}
