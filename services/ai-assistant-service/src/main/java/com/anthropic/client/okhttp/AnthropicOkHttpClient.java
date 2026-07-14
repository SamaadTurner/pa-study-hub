package com.anthropic.client.okhttp;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.TextBlock;
import java.util.List;

/**
 * Stub implementation of Anthropic HTTP client (until official SDK is available)
 * TODO: Replace with actual SDK when available in Maven Central
 */
public class AnthropicOkHttpClient implements AnthropicClient {

    private final String apiKey;

    private AnthropicOkHttpClient(String apiKey) {
        this.apiKey = apiKey;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public AnthropicClient.Messages messages() {
        return new MessagesImpl();
    }

    public static class Builder {
        private String apiKey;

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public AnthropicOkHttpClient build() {
            return new AnthropicOkHttpClient(apiKey);
        }
    }

    private static class MessagesImpl implements AnthropicClient.Messages {
        @Override
        public Message create(MessageCreateParams params) {
            // Stub implementation - returns a dummy response
            // TODO: Implement actual HTTP call to Anthropic API
            return new MessageImpl();
        }
    }

    private static class MessageImpl implements Message {
        @Override
        public List<Object> content() {
            TextBlock textBlock = new TextBlock("This is a stub response. Anthropic SDK integration pending.");
            return List.of(textBlock);
        }
    }
}
