package com.pastudyhub.flashcard.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * An individual flashcard within a deck.
 *
 * <p>Cards have a front (question/prompt) and back (answer/explanation).
 * An optional hint can be shown before flipping to give a memory cue.
 * Tags enable fine-grained search within a deck.
 *
 * <p>Tags are stored as a comma-separated string internally but exposed
 * as a List&lt;String&gt; to callers via the getTagsList()/setTagsList() helpers.
 */
@Entity
@Table(name = "cards",
    indexes = {
        @Index(name = "idx_cards_deck_id", columnList = "deck_id")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_id", nullable = false)
    private Deck deck;

    /** The question or prompt shown on the front of the card. */
    @Column(name = "front", nullable = false, length = 2000)
    private String front;

    /** The answer or explanation shown on the back of the card. */
    @Column(name = "back", nullable = false, length = 5000)
    private String back;

    /** Optional memory aid shown before the card is flipped. */
    @Column(name = "hint", length = 500)
    private String hint;

    /** Optional URL to an anatomy diagram or clinical image (S3/CDN URL). */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /**
     * Tags stored as comma-separated string: "heart-failure,ejection-fraction,bnp"
     * Use getTagsList() and setTagsList() for typed access.
     */
    @Column(name = "tags", length = 1000)
    private String tags;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Returns tags as a List&lt;String&gt;, or an empty list if no tags are set.
     */
    public List<String> getTagsList() {
        if (tags == null || tags.isBlank()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(tags.split(",")));
    }

    /**
     * Sets tags from a List&lt;String&gt;, storing as comma-separated string.
     *
     * @param tagList the list of tag strings (nulls and blanks are filtered out)
     */
    public void setTagsList(List<String> tagList) {
        if (tagList == null || tagList.isEmpty()) {
            this.tags = null;
        } else {
            this.tags = String.join(",",
                    tagList.stream()
                            .filter(t -> t != null && !t.isBlank())
                            .map(String::trim)
                            .toList());
        }
    }
}
