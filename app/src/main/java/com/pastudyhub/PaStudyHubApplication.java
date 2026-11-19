package com.pastudyhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * PA Study Hub - Consolidated Monolith Application
 *
 * Spaced-repetition study platform for PA school students.
 * Combines flashcards, practice exams, and progress tracking.
 */
// Scheduling is on for ExamTimeLimitJob, which hands in exams whose time ran out
@SpringBootApplication
@EnableScheduling
public class PaStudyHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaStudyHubApplication.class, args);
    }
}
