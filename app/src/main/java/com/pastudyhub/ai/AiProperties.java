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

    private String model = "llama-3.3-70b-versatile";

    /** How long to wait for an answer before giving up. */
    private int timeoutSeconds = 30;

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }
}
