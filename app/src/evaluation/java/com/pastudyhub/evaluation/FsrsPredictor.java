package com.pastudyhub.evaluation;

import com.pastudyhub.flashcard.engine.FsrsEngine;

/** FSRS predicts recall directly from the card's stability, so nothing is assumed here. */
public class FsrsPredictor implements Predictor {

    private final FsrsEngine engine;
    private FsrsEngine.MemoryState memory;

    public FsrsPredictor(FsrsEngine engine) {
        this.engine = engine;
    }

    @Override
    public double chanceOfRemembering(int elapsedDays) {
        return engine.retrievability(elapsedDays, memory.stability());
    }

    @Override
    public void review(int elapsedDays, int rating) {
        memory = engine.review(memory, elapsedDays, rating).memory();
    }
}
