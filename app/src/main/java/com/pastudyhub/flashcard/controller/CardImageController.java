package com.pastudyhub.flashcard.controller;

import com.pastudyhub.flashcard.dto.CardImageResponse;
import com.pastudyhub.flashcard.exception.InvalidImageException;
import com.pastudyhub.flashcard.model.CardImage;
import com.pastudyhub.flashcard.service.CardImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
@Tag(name = "Pictures", description = "Pictures for picture cards")
public class CardImageController {

    private final CardImageService imageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload a PNG or JPEG picture (at most 1 MB) for picture cards")
    public CardImageResponse upload(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-User-Id") UUID userId) {
        try {
            return imageService.upload(userId, file.getBytes());
        } catch (IOException e) {
            throw new InvalidImageException("The upload could not be read. Please try again.");
        }
    }

    @GetMapping("/{imageId}")
    @Operation(summary = "Get a picture (its owner, or anyone who can see a deck that uses it)")
    public ResponseEntity<byte[]> get(
            @PathVariable UUID imageId,
            @RequestHeader("X-User-Id") UUID userId) {
        CardImage image = imageService.load(imageId, userId);
        // A picture never changes after upload, so the browser can keep it
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePrivate())
                .header("X-Content-Type-Options", "nosniff")
                .body(image.getData());
    }
}
