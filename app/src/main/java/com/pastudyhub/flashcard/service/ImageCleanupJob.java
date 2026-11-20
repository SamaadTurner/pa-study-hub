package com.pastudyhub.flashcard.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Removes pictures no card uses (uploaded and never saved as cards, or whose cards
 * were deleted), so they do not fill up the database. Runs a few minutes after start
 * and then every six hours.
 */
@Component
@RequiredArgsConstructor
public class ImageCleanupJob {

    private final CardImageService imageService;

    @Scheduled(fixedDelayString = "${images.cleanup-interval-ms:21600000}", initialDelay = 300000)
    public void deleteUnusedImages() {
        imageService.deleteUnusedImages();
    }
}
