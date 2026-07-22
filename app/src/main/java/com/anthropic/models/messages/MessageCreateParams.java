package com.anthropic.models.messages;

import java.util.ArrayList;
import java.util.List;

/**
 * Stub class for Anthropic Message creation parameters
 */
public class MessageCreateParams {
    private final String model;
    private final int maxTokens;
    private final List<String> messages;

    private MessageCreateParams(Builder builder) {
        this.model = builder.model;
        this.maxTokens = builder.maxTokens;
        this.messages = builder.messages;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String model() {
        return model;
    }

    public int maxTokens() {
        return maxTokens;
    }

    public List<String> messages() {
        return messages;
    }

    public static class Builder {
        private String model;
        private int maxTokens;
        private List<String> messages = new ArrayList<>();

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder addUserMessage(String message) {
            this.messages.add(message);
            return this;
        }

        public MessageCreateParams build() {
            return new MessageCreateParams(this);
        }
    }
}
