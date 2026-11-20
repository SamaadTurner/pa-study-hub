package com.pastudyhub.flashcard.dto;

/**
 * One box over a picture: position and size as fractions of the picture's width
 * and height, and an optional label (the answer) shown when the box is revealed.
 */
public record OcclusionBox(double x, double y, double width, double height, String label) {
}
