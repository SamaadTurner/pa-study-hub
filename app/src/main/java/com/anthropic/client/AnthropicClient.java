package com.anthropic.client;

import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;

/**
 * Stub interface for Anthropic SDK (until official SDK is available in Maven Central)
 */
public interface AnthropicClient {
    Messages messages();

    /**
     * Nested interface for Messages API
     */
    interface Messages {
        Message create(MessageCreateParams params);
    }
}
