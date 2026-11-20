package com.pastudyhub.flashcard.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pastudyhub.BaseIntegrationTest;
import com.pastudyhub.flashcard.repository.CardImageRepository;
import com.pastudyhub.flashcard.service.CardImageService;
import com.pastudyhub.user.service.AccountDeletionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Picture cards (image occlusion)")
@TestPropertySource(properties = "images.max-per-user=4")
class PictureCardsIntegrationTest extends BaseIntegrationTest {

    private static final List<Map<String, Object>> TWO_BOXES = List.of(
            Map.of("x", 0.1, "y", 0.2, "width", 0.3, "height", 0.1, "label", "Left ventricle"),
            Map.of("x", 0.5, "y", 0.5, "width", 0.2, "height", 0.2, "label", ""));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CardImageRepository imageRepository;

    @Autowired
    private CardImageService imageService;

    @Autowired
    private AccountDeletionService accountDeletionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("A picture with two boxes makes two cards; the owner can load the picture")
    void createsOneCardPerBox() throws Exception {
        UUID user = UUID.randomUUID();
        String deckId = createDeck(user, false);
        byte[] png = png(40, 30);

        JsonNode uploaded = parse(upload(user, png, "heart.png", "image/png")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.width").value(40))
                .andExpect(jsonPath("$.height").value(30)));
        String imageId = uploaded.get("id").asText();

        createImageCards(user, deckId, imageId, "", TWO_BOXES)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].cardType").value("IMAGE"))
                .andExpect(jsonPath("$[0].clozeIndex").value(1))
                .andExpect(jsonPath("$[1].clozeIndex").value(2))
                .andExpect(jsonPath("$[0].front").value("What is under the highlighted box?"))
                .andExpect(jsonPath("$[0].imageId").value(imageId))
                .andExpect(jsonPath("$[0].boxes", hasSize(2)))
                .andExpect(jsonPath("$[0].boxes[0].label").value("Left ventricle"))
                .andExpect(jsonPath("$[0].boxes[1].x").value(0.5));

        JsonNode queue = getJson(user, "/api/v1/decks/" + deckId + "/review");
        assertThat(queue).hasSize(2);
        assertThat(queue.get(0).get("boxes")).hasSize(2);

        mockMvc.perform(get("/api/v1/images/{id}", imageId).header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(png))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));

        // The question and notes can be edited like any card; the back may stay empty
        String cardId = queue.get(0).get("id").asText();
        mockMvc.perform(put("/api/v1/cards/{id}", cardId).header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("front", "Name the chamber", "back", ""))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.front").value("Name the chamber"))
                .andExpect(jsonPath("$.boxes", hasSize(2)));
    }

    @Test
    @DisplayName("Only real PNG or JPEG pictures up to 1 MB are taken, whatever the browser says")
    void uploadChecks() throws Exception {
        UUID user = UUID.randomUUID();

        upload(user, "<svg onload=alert(1)>".getBytes(), "x.png", "image/png")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Only PNG and JPEG pictures can be uploaded"));
        byte[] tooBig = new byte[1024 * 1024 + 1];
        upload(user, tooBig, "big.jpg", "image/jpeg")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("The picture is larger than 1 MB"));
        // Starts like a PNG but is not one
        byte[] fake = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};
        upload(user, fake, "fake.png", "image/png")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("The file could not be read as a picture"));
        // A JPEG is fine, even when the browser calls it something else
        upload(user, jpeg(20, 20), "photo.bin", "application/octet-stream")
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Boxes must be on the picture, and only your own picture and deck can be used")
    void cardChecks() throws Exception {
        UUID user = UUID.randomUUID();
        String deckId = createDeck(user, false);
        String imageId = parse(upload(user, png(10, 10), "a.png", "image/png")).get("id").asText();

        createImageCards(user, deckId, imageId, null, List.of())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Draw at least one box over the part to hide"));
        createImageCards(user, deckId, imageId, null,
                List.of(Map.of("x", 0.9, "y", 0.1, "width", 0.5, "height", 0.1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Box 1 is not on the picture"));

        UUID stranger = UUID.randomUUID();
        String strangersDeck = createDeck(stranger, false);
        createImageCards(stranger, strangersDeck, imageId, null, TWO_BOXES).andExpect(status().isNotFound());
        createImageCards(stranger, deckId, imageId, null, TWO_BOXES).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Others see a picture only through a public deck or their copy of it")
    void pictureAccess() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        String deckId = createDeck(owner, false);
        String imageId = parse(upload(owner, png(10, 10), "a.png", "image/png")).get("id").asText();
        createImageCards(owner, deckId, imageId, "Name it", TWO_BOXES).andExpect(status().isCreated());

        getImage(other, imageId).andExpect(status().isNotFound());

        // Public: anyone signed in can see it, and copy the deck
        setPublic(owner, deckId, true);
        getImage(other, imageId).andExpect(status().isOk());
        String copyId = parse(mockMvc.perform(post("/api/v1/decks/{id}/clone", deckId)
                .header("Authorization", bearerToken(other)))
                .andExpect(status().isCreated())).get("id").asText();
        JsonNode copied = getJson(other, "/api/v1/decks/" + copyId + "/cards").get("content");
        assertThat(copied.get(0).get("imageId").asText()).isEqualTo(imageId);
        assertThat(copied.get(0).get("boxes")).hasSize(2);

        // Private again: the copy still shows the picture to its owner, nobody else
        setPublic(owner, deckId, false);
        getImage(other, imageId).andExpect(status().isOk());
        getImage(UUID.randomUUID(), imageId).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Each user can keep a limited number of pictures")
    void perUserLimit() throws Exception {
        UUID user = UUID.randomUUID();
        for (int i = 0; i < 4; i++) {
            upload(user, png(5, 5), "p.png", "image/png").andExpect(status().isCreated());
        }
        upload(user, png(5, 5), "p.png", "image/png")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "You have reached the limit of 4 pictures. Delete picture cards you no longer need to make room."));
    }

    @Test
    @DisplayName("Pictures no card uses are deleted after a day; pictures in use are kept")
    void cleanup() throws Exception {
        UUID user = UUID.randomUUID();
        String deckId = createDeck(user, false);
        String used = parse(upload(user, png(5, 5), "a.png", "image/png")).get("id").asText();
        String unused = parse(upload(user, png(5, 5), "b.png", "image/png")).get("id").asText();
        String fresh = parse(upload(user, png(5, 5), "c.png", "image/png")).get("id").asText();
        JsonNode cards = parse(createImageCards(user, deckId, used, null, TWO_BOXES));
        jdbcTemplate.update("UPDATE card_images SET created_at = ? WHERE id IN (?, ?)",
                java.sql.Timestamp.valueOf(java.time.LocalDateTime.now().minusDays(2)),
                UUID.fromString(used), UUID.fromString(unused));

        imageService.deleteUnusedImages();

        assertThat(imageRepository.existsById(UUID.fromString(used))).isTrue();
        assertThat(imageRepository.existsById(UUID.fromString(unused))).isFalse();
        assertThat(imageRepository.existsById(UUID.fromString(fresh))).isTrue();

        // Once its cards are deleted, the old picture goes too
        for (JsonNode card : cards) {
            mockMvc.perform(delete("/api/v1/cards/{id}", card.get("id").asText())
                    .header("Authorization", bearerToken(user))).andExpect(status().isNoContent());
        }
        imageService.deleteUnusedImages();
        assertThat(imageRepository.existsById(UUID.fromString(used))).isFalse();
    }

    @Test
    @DisplayName("Closing an account deletes its pictures, except those a copied deck still uses")
    void accountDeletion() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        String publicDeck = createDeck(owner, true);
        String shared = parse(upload(owner, png(5, 5), "a.png", "image/png")).get("id").asText();
        String alone = parse(upload(owner, png(5, 5), "b.png", "image/png")).get("id").asText();
        createImageCards(owner, publicDeck, shared, null, TWO_BOXES).andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/decks/{id}/clone", publicDeck).header("Authorization", bearerToken(other)))
                .andExpect(status().isCreated());

        accountDeletionService.deleteEverything(owner);

        assertThat(imageRepository.existsById(UUID.fromString(alone))).isFalse();
        assertThat(imageRepository.findById(UUID.fromString(shared))).hasValueSatisfying(
                image -> assertThat(image.getOwnerId()).isNull());
        getImage(other, shared).andExpect(status().isOk());
    }

    // ---- helpers -------------------------------------------------------------

    private static byte[] png(int width, int height) throws Exception {
        return encode(width, height, "png", BufferedImage.TYPE_INT_ARGB);
    }

    private static byte[] jpeg(int width, int height) throws Exception {
        return encode(width, height, "jpg", BufferedImage.TYPE_INT_RGB);
    }

    private static byte[] encode(int width, int height, String format, int type) throws Exception {
        BufferedImage image = new BufferedImage(width, height, type);
        image.setRGB(0, 0, 0xFF3366);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, format, out);
        return out.toByteArray();
    }

    private ResultActions upload(UUID user, byte[] bytes, String name, String type) throws Exception {
        return mockMvc.perform(multipart("/api/v1/images")
                .file(new MockMultipartFile("file", name, type, bytes))
                .header("Authorization", bearerToken(user)));
    }

    private ResultActions getImage(UUID user, String imageId) throws Exception {
        return mockMvc.perform(get("/api/v1/images/{id}", imageId).header("Authorization", bearerToken(user)));
    }

    private ResultActions createImageCards(UUID user, String deckId, String imageId, String prompt,
                                           List<Map<String, Object>> boxes) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("imageId", imageId);
        body.put("boxes", boxes);
        if (prompt != null) {
            body.put("prompt", prompt);
        }
        return mockMvc.perform(post("/api/v1/decks/{id}/image-cards", deckId).header("Authorization", bearerToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private String createDeck(UUID user, boolean isPublic) throws Exception {
        return parse(mockMvc.perform(post("/api/v1/decks").header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "Anatomy", "description", "",
                                "category", "CARDIOLOGY", "isPublic", isPublic))))
                .andExpect(status().isCreated())).get("id").asText();
    }

    private void setPublic(UUID user, String deckId, boolean isPublic) throws Exception {
        mockMvc.perform(put("/api/v1/decks/{id}", deckId).header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "Anatomy", "description", "",
                                "category", "CARDIOLOGY", "isPublic", isPublic))))
                .andExpect(status().isOk());
    }

    private JsonNode getJson(UUID user, String url) throws Exception {
        return objectMapper.readTree(mockMvc.perform(get(url).header("Authorization", bearerToken(user)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private JsonNode parse(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }
}
