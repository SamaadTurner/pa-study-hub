package com.pastudyhub.ai;

/**
 * Sends one prompt to a language model and returns its reply.
 *
 * <p>It is an interface so the rest of the app does not depend on one provider,
 * and so the tests can use a fake that answers instantly and costs nothing.
 */
public interface ChatClient {

    /** False when no API key is set. Callers check this before calling complete. */
    boolean isEnabled();

    /** The model name, shown on the status endpoint. */
    String model();

    /**
     * Asks the model for a JSON answer.
     *
     * @param systemPrompt what the model is supposed to do
     * @param userPrompt   the user's input
     * @return the text of the model's reply
     * @throws AiUnavailableException when the provider can't be reached, is busy, or is off
     * @throws AiResponseException    when the provider answers with an error or nothing usable
     */
    String complete(String systemPrompt, String userPrompt);
}
