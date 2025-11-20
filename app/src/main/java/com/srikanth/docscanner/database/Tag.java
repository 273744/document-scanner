package com.srikanth.docscanner.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Tag Entity - Represents a tag for categorizing documents
 *
 * Features:
 * - Unique tag names
 * - Color coding
 * - Usage count tracking
 * - Timestamps
 * - Indexes for performance
 */
@Entity(
    tableName = "tags",
    indices = {
        @Index(value = "tag_name", unique = true),
        @Index(value = "created_at")
    }
)
public class Tag {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "tag_id")
    private long tagId;

    @ColumnInfo(name = "tag_name", collate = ColumnInfo.NOCASE)
    private String tagName;

    @ColumnInfo(name = "tag_color")
    private String tagColor; // Hex color code

    @ColumnInfo(name = "usage_count", defaultValue = "0")
    private int usageCount; // Number of documents with this tag

    // Timestamps
    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "modified_at")
    private long modifiedAt;

    // Metadata
    @ColumnInfo(name = "description")
    private String description;

    @ColumnInfo(name = "is_system_tag", defaultValue = "0")
    private boolean isSystemTag; // System tags cannot be deleted

    // Constructors
    public Tag() {
        this.createdAt = System.currentTimeMillis();
        this.modifiedAt = System.currentTimeMillis();
        this.usageCount = 0;
        this.isSystemTag = false;
    }

    @androidx.room.Ignore
    public Tag(String tagName) {
        this();
        this.tagName = tagName;
    }

    @androidx.room.Ignore
    public Tag(String tagName, String tagColor) {
        this(tagName);
        this.tagColor = tagColor;
    }

    // Getters and Setters
    public long getTagId() {
        return tagId;
    }

    public void setTagId(long tagId) {
        this.tagId = tagId;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getTagColor() {
        return tagColor;
    }

    public void setTagColor(String tagColor) {
        this.tagColor = tagColor;
    }

    public int getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(int usageCount) {
        this.usageCount = usageCount;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(long modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isSystemTag() {
        return isSystemTag;
    }

    public void setSystemTag(boolean systemTag) {
        isSystemTag = systemTag;
    }

    /**
     * Increment usage count
     */
    public void incrementUsageCount() {
        this.usageCount++;
        this.modifiedAt = System.currentTimeMillis();
    }

    /**
     * Decrement usage count
     */
    public void decrementUsageCount() {
        if (this.usageCount > 0) {
            this.usageCount--;
            this.modifiedAt = System.currentTimeMillis();
        }
    }
}


