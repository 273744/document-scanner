package com.srikanth.docscanner.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

/**
 * DocumentTag Junction Table - Many-to-many relationship between Documents and Tags
 *
 * Features:
 * - Composite primary key (document_id, tag_id)
 * - Foreign keys with cascade delete
 * - Timestamp for when tag was applied
 * - Indexes for performance
 */
@Entity(
    tableName = "document_tags",
    primaryKeys = {"document_id", "tag_id"},
    foreignKeys = {
        @ForeignKey(
            entity = Document.class,
            parentColumns = "document_id",
            childColumns = "document_id",
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        ),
        @ForeignKey(
            entity = Tag.class,
            parentColumns = "tag_id",
            childColumns = "tag_id",
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    },
    indices = {
        @Index(value = "document_id"),
        @Index(value = "tag_id"),
        @Index(value = "tagged_at")
    }
)
public class DocumentTag {

    @ColumnInfo(name = "document_id")
    private long documentId;

    @ColumnInfo(name = "tag_id")
    private long tagId;

    @ColumnInfo(name = "tagged_at")
    private long taggedAt;

    @ColumnInfo(name = "tagged_by")
    private String taggedBy; // User who applied the tag (for multi-user support)

    // Constructors
    public DocumentTag() {
        this.taggedAt = System.currentTimeMillis();
    }

    public DocumentTag(long documentId, long tagId) {
        this();
        this.documentId = documentId;
        this.tagId = tagId;
    }

    // Getters and Setters
    public long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(long documentId) {
        this.documentId = documentId;
    }

    public long getTagId() {
        return tagId;
    }

    public void setTagId(long tagId) {
        this.tagId = tagId;
    }

    public long getTaggedAt() {
        return taggedAt;
    }

    public void setTaggedAt(long taggedAt) {
        this.taggedAt = taggedAt;
    }

    public String getTaggedBy() {
        return taggedBy;
    }

    public void setTaggedBy(String taggedBy) {
        this.taggedBy = taggedBy;
    }
}


