package com.pastudyhub.ai;

import com.pastudyhub.ai.dto.GenerateFlashcardsRequest;
import com.pastudyhub.ai.dto.GeneratedCard;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "AI", description = "Optional AI help. Off unless the server has an AI_API_KEY.")
@SecurityRequirement(name = "bearerAuth")
public class AiController {

    private final ChatClient chatClient;
    private final FlashcardGenerator flashcardGenerator;

    public record StatusResponse(boolean enabled, String model) {
    }

    public record FlashcardsResponse(List<GeneratedCard> cards) {
    }

    // The frontend asks this first, so it can hide the AI buttons when the feature is off
    @GetMapping("/status")
    @Operation(summary = "Whether the AI features are turned on")
    public StatusResponse status() {
        boolean enabled = chatClient.isEnabled();
        return new StatusResponse(enabled, enabled ? chatClient.model() : null);
    }

    @PostMapping("/flashcards")
    @Operation(summary = "Suggest flashcards from notes. Nothing is saved.")
    public FlashcardsResponse generateFlashcards(@Valid @RequestBody GenerateFlashcardsRequest request) {
        return new FlashcardsResponse(flashcardGenerator.generate(request.getNotes(), request.getCount()));
    }
}
