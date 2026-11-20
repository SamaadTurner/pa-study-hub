package com.pastudyhub;

import java.util.TimeZone;

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
        // Saved times are UTC wherever the app runs (the Render server already is);
        // the user's own day comes from UserClock
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(PaStudyHubApplication.class, args);
    }
}
