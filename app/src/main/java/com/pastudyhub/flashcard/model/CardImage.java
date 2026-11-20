package com.pastudyhub.flashcard.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/** A picture uploaded for picture cards. The bytes are stored in the database. */
@Entity
@Table(name = "card_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Null when the owner closed their account and copies of their cards still use the picture. */
    @Column(name = "owner_id")
    private UUID ownerId;

    /** image/png or image/jpeg, worked out from the bytes, not from what the browser said. */
    @Column(name = "content_type", nullable = false, length = 20)
    private String contentType;

    @Column(name = "width", nullable = false)
    private int width;

    @Column(name = "height", nullable = false)
    private int height;

    @Column(name = "size_bytes", nullable = false)
    private int sizeBytes;

    // bytea in PostgreSQL; the explicit type keeps the H2 test database on varbinary too
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "data", nullable = false, length = 1_048_576)
    private byte[] data;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
