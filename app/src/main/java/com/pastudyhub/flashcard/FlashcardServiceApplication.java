package com.pastudyhub.flashcard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * PA Study Hub — Flashcard Service
 *
 * <p>Manages flashcard decks, individual cards, and the SM-2 spaced repetition
 * algorithm for scheduling card reviews. When a review is submitted, this service
 * calls the study-progress-service to log the activity.
 *
 * <p>Port: 8081
 */
@SpringBootApplication
@EnableScheduling
public class FlashcardServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlashcardServiceApplication.class, args);
    }
}
