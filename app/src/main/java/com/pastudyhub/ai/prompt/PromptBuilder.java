package com.pastudyhub.ai.prompt;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Builds structured prompts for the four AI features.
 * Prompts are designed to be medically accurate and PA-school focused.
 */
@Component
public class PromptBuilder {

    private static final String SYSTEM_CONTEXT =
            "You are a knowledgeable PA (Physician Assistant) school tutor with expertise " +
            "in the NCCPA PANCE/PANRE content blueprint. Provide accurate, concise, and " +
            "clinically relevant responses appropriate for PA students preparing for boards. " +
            "Use medical terminology correctly and cite clinical reasoning.";

    /**
     * Feature 1: Generate flashcard front/back pairs for a given topic.
     */
    public String buildFlashcardGeneratorPrompt(String topic, String category, int count) {
        return SYSTEM_CONTEXT + "\n\n" +
               "Generate " + count + " high-yield PANCE flashcard question-answer pairs for the topic: " +
               "\"" + topic + "\" in the category: " + category + ".\n\n" +
               "Format each flashcard EXACTLY as follows (no other text, no numbering):\n" +
               "FRONT: [question or clinical scenario]\n" +
               "BACK: [concise but complete answer with key details]\n" +
               "HINT: [one-word or short phrase hint]\n" +
               "TAGS: [comma-separated relevant tags]\n" +
               "---\n\n" +
               "Focus on: mechanisms, key findings, first-line treatments, mnemonics, and PANCE-tested details.";
    }

    /**
     * Feature 2: Explain why a specific answer is wrong and what the correct answer is.
     */
    public String buildWrongAnswerExplanationPrompt(String questionStem, String selectedAnswer,
                                                      String correctAnswer, String explanation) {
        return SYSTEM_CONTEXT + "\n\n" +
               "A PA student answered a PANCE practice question incorrectly.\n\n" +
               "QUESTION: " + questionStem + "\n" +
               "STUDENT ANSWERED: " + selectedAnswer + "\n" +
               "CORRECT ANSWER: " + correctAnswer + "\n" +
               "OFFICIAL EXPLANATION: " + explanation + "\n\n" +
               "Please provide:\n" +
               "1. WHY the student's answer is incorrect (common reasoning trap)\n" +
               "2. WHY the correct answer is right (clinical reasoning)\n" +
               "3. A memory aid or mnemonic to remember this concept\n" +
               "4. Related concepts that are commonly tested together on the PANCE\n\n" +
               "Keep response under 300 words. Be encouraging but medically precise.";
    }

    /**
     * Feature 3: Generate a personalized study plan based on weak categories.
     */
    public String buildStudyPlanPrompt(List<String> weakCategories, int daysUntilExam,
                                        int dailyMinutes) {
        return SYSTEM_CONTEXT + "\n\n" +
               "Create a personalized PANCE study plan for a PA student with the following profile:\n" +
               "- Weak categories (by performance): " + String.join(", ", weakCategories) + "\n" +
               "- Days until exam: " + daysUntilExam + "\n" +
               "- Available study time per day: " + dailyMinutes + " minutes\n\n" +
               "Generate a structured weekly study plan that:\n" +
               "1. Prioritizes weak categories while maintaining strong areas\n" +
               "2. Alternates flashcard review and practice questions\n" +
               "3. Includes spaced repetition principles (review weak topics every 2-3 days)\n" +
               "4. Recommends specific high-yield resources for each category\n" +
               "5. Includes 1 full practice exam per week\n\n" +
               "Format as a weekly schedule (Monday-Sunday) with specific daily tasks. " +
               "Be realistic about time constraints.";
    }

    /**
     * Feature 4: Chat tutor — answer a medical question in conversational style.
     */
    public String buildChatTutorPrompt(String userMessage, List<String> conversationHistory) {
        StringBuilder prompt = new StringBuilder(SYSTEM_CONTEXT);
        prompt.append("\n\n");

        if (!conversationHistory.isEmpty()) {
            prompt.append("Previous conversation:\n");
            for (String msg : conversationHistory) {
                prompt.append(msg).append("\n");
            }
            prompt.append("\n");
        }

        prompt.append("Student question: ").append(userMessage).append("\n\n");
        prompt.append("Provide a clear, educational answer appropriate for a PA student preparing for boards. " +
                     "If the question involves clinical decision-making, walk through the reasoning. " +
                     "Keep responses focused and under 400 words unless the topic requires more detail.");

        return prompt.toString();
    }
}
