package com.pastudyhub.flashcard.service;

import com.pastudyhub.flashcard.dto.CardImageResponse;
import com.pastudyhub.flashcard.exception.ImageLimitException;
import com.pastudyhub.flashcard.exception.ImageNotFoundException;
import com.pastudyhub.flashcard.exception.InvalidImageException;
import com.pastudyhub.flashcard.model.CardImage;
import com.pastudyhub.flashcard.repository.CardImageRepository;
import com.pastudyhub.flashcard.repository.CardRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.UUID;

/**
 * Stores and hands out the pictures of picture cards.
 *
 * <p>The pictures live in the database (see V22 for why), so the limits here keep it
 * small: one picture is at most 1 MB, each user can keep a set number, and the site
 * as a whole stops taking uploads before the free database is full.
 */
@Slf4j
@Service
public class CardImageService {

    static final int MAX_BYTES = 1024 * 1024;
    // Bigger than any screen; also stops a tiny file that claims a huge size
    static final int MAX_SIDE = 4000;

    private static final byte[] PNG_START = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG_START = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    private final CardImageRepository imageRepository;
    private final CardRepository cardRepository;
    private final int maxPerUser;
    private final long maxTotalBytes;

    public CardImageService(CardImageRepository imageRepository,
                            CardRepository cardRepository,
                            @Value("${images.max-per-user:100}") int maxPerUser,
                            @Value("${images.max-total-mb:500}") long maxTotalMb) {
        this.imageRepository = imageRepository;
        this.cardRepository = cardRepository;
        this.maxPerUser = maxPerUser;
        this.maxTotalBytes = maxTotalMb * 1024 * 1024;
    }

    @Transactional
    public CardImageResponse upload(UUID userId, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new InvalidImageException("Choose a picture to upload");
        }
        if (bytes.length > MAX_BYTES) {
            throw new InvalidImageException("The picture is larger than 1 MB");
        }
        String contentType = detectType(bytes);
        int[] size = readSize(bytes);

        if (imageRepository.countByOwnerId(userId) >= maxPerUser) {
            throw new ImageLimitException("You have reached the limit of " + maxPerUser
                    + " pictures. Delete picture cards you no longer need to make room.");
        }
        if (imageRepository.totalBytes() + bytes.length > maxTotalBytes) {
            log.warn("Picture storage is full ({} MB limit)", maxTotalBytes / (1024 * 1024));
            throw new ImageLimitException("The site has no room for more pictures right now. Please try again later.");
        }

        CardImage saved = imageRepository.save(CardImage.builder()
                .ownerId(userId)
                .contentType(contentType)
                .width(size[0])
                .height(size[1])
                .sizeBytes(bytes.length)
                .data(bytes)
                .build());
        log.info("Picture uploaded: id={} {}x{} {} bytes", saved.getId(), size[0], size[1], bytes.length);
        return new CardImageResponse(saved.getId(), size[0], size[1], bytes.length);
    }

    /**
     * The picture, for its owner or for anyone who can see a deck that uses it
     * (a public deck, or a copy of one). Everyone else gets "not found".
     */
    @Transactional(readOnly = true)
    public CardImage load(UUID imageId, UUID viewerId) {
        CardImage image = imageRepository.findById(imageId).orElseThrow(ImageNotFoundException::new);
        boolean isOwner = viewerId.equals(image.getOwnerId());
        if (!isOwner && !cardRepository.existsVisibleCardWithImage(imageId, viewerId)) {
            throw new ImageNotFoundException();
        }
        return image;
    }

    /** The picture, if this user uploaded it. Used when cards are made from it. */
    @Transactional(readOnly = true)
    public CardImage findOwn(UUID imageId, UUID userId) {
        return imageRepository.findById(imageId)
                .filter(image -> userId.equals(image.getOwnerId()))
                .orElseThrow(ImageNotFoundException::new);
    }

    /** Deletes pictures no card uses, a day after upload (time to finish making the cards). */
    @Transactional
    public int deleteUnusedImages() {
        int deleted = imageRepository.deleteUnusedBefore(LocalDateTime.now().minusDays(1));
        if (deleted > 0) {
            log.info("Deleted {} pictures no card uses", deleted);
        }
        return deleted;
    }

    // Decided from the first bytes, not from the file name or what the browser sent
    private static String detectType(byte[] bytes) {
        if (startsWith(bytes, PNG_START)) {
            return "image/png";
        }
        if (startsWith(bytes, JPEG_START)) {
            return "image/jpeg";
        }
        throw new InvalidImageException("Only PNG and JPEG pictures can be uploaded");
    }

    private static boolean startsWith(byte[] bytes, byte[] start) {
        if (bytes.length < start.length) {
            return false;
        }
        for (int i = 0; i < start.length; i++) {
            if (bytes[i] != start[i]) {
                return false;
            }
        }
        return true;
    }

    // Reads only the header, so a picture that claims to be huge is never decoded
    private static int[] readSize(byte[] bytes) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new InvalidImageException("The file could not be read as a picture");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > MAX_SIDE || height > MAX_SIDE) {
                    throw new InvalidImageException("The picture must be at most " + MAX_SIDE + " pixels on each side");
                }
                return new int[] {width, height};
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new InvalidImageException("The file could not be read as a picture");
        }
    }
}
