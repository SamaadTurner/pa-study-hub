package com.pastudyhub.ai.dto;

/** A suggested flashcard. It is not saved until the user adds it to a deck. */
public record GeneratedCard(String front, String back) {
}
