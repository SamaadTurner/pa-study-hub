package com.pastudyhub.ai;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Settings for the AI features, from the "ai" section of application.yml.
 *
 * <p>The client speaks the OpenAI chat completions format, which several providers
 * offer. The defaults point at Groq's free tier. Other options that work by only
 * changing these settings:
 * <ul>
 *   <li>Google Gemini: base-url https://generativelanguage.googleapis.com/v1beta/openai</li>
 *   <li>A local Ollama model: base-url http://localhost:11434/v1 (any api-key)</li>
 * </ul>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /** Address of the provider's OpenAI-compatible API, without /chat/completions. */
    private String baseUrl = "https://api.groq.com/openai/v1";

    /** The AI features are off while this is empty. */
    private String apiKey = "";

    // Groq retired llama-3.3-70b-versatile in 2026; check the model list before changing this
    private String model = "openai/gpt-oss-120b";

    /** How long to wait for an answer before giving up. */
    private int timeoutSeconds = 30;

    /** AI requests one user may make per day (UTC). */
    private int dailyLimitPerUser = 20;

    /**
     * AI requests the whole site may make per day. Keep it under the provider's own
     * daily limit (about 1,000 requests a day on Groq's free plan for the default model in Nov 2026).
     */
    private int dailyLimitTotal = 300;

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }
}
