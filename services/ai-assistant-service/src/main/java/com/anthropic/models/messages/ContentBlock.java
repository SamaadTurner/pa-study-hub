package com.anthropic.models.messages;

/**
 * Stub interface for Anthropic content blocks
 */
public interface ContentBlock {
    boolean isText();
    TextBlock asText();
}
