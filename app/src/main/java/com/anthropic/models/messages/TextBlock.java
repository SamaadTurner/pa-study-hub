package com.anthropic.models.messages;

/**
 * Stub class for Anthropic text content blocks
 */
public class TextBlock implements ContentBlock {
    private final String text;

    public TextBlock(String text) {
        this.text = text;
    }

    public String text() {
        return text;
    }

    public Type type() {
        return Type.TEXT;
    }

    @Override
    public boolean isText() {
        return true;
    }

    @Override
    public TextBlock asText() {
        return this;
    }

    public enum Type {
        TEXT
    }
}
