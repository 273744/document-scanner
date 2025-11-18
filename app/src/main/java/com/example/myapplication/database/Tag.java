package com.example.myapplication.database;
}
    }
        }
            this.modifiedAt = System.currentTimeMillis();
            this.usageCount--;
        if (this.usageCount > 0) {
    public void decrementUsageCount() {
     */
     * Decrement usage count
    /**

    }
        this.modifiedAt = System.currentTimeMillis();
        this.usageCount++;
    public void incrementUsageCount() {
     */
     * Increment usage count
    /**

    }
        isSystemTag = systemTag;
    public void setSystemTag(boolean systemTag) {

    }
        return isSystemTag;
    public boolean isSystemTag() {

    }
        this.description = description;
    public void setDescription(String description) {

    }
        return description;
    public String getDescription() {

    }
        this.modifiedAt = modifiedAt;
    public void setModifiedAt(long modifiedAt) {

    }
        return modifiedAt;
    public long getModifiedAt() {

    }
        this.createdAt = createdAt;
    public void setCreatedAt(long createdAt) {

    }
        return createdAt;
    public long getCreatedAt() {

    }
        this.usageCount = usageCount;
    public void setUsageCount(int usageCount) {

    }
        return usageCount;
    public int getUsageCount() {

    }
        this.tagColor = tagColor;
    public void setTagColor(String tagColor) {

    }
        return tagColor;
    public String getTagColor() {

    }
        this.tagName = tagName;
    public void setTagName(String tagName) {

    }
        return tagName;
    public String getTagName() {

    }
        this.tagId = tagId;
    public void setTagId(long tagId) {

    }
        return tagId;
    public long getTagId() {
    // Getters and Setters

    }
        this.tagColor = tagColor;
        this(tagName);
    public Tag(String tagName, String tagColor) {

    }
        this.tagName = tagName;
        this();
    public Tag(String tagName) {

    }
        this.isSystemTag = false;
        this.usageCount = 0;
        this.modifiedAt = System.currentTimeMillis();
        this.createdAt = System.currentTimeMillis();
    public Tag() {
    // Constructors

    private boolean isSystemTag; // System tags cannot be deleted
    @ColumnInfo(name = "is_system_tag", defaultValue = "0")

    private String description;
    @ColumnInfo(name = "description")
    // Metadata

    private long modifiedAt;
    @ColumnInfo(name = "modified_at")

    private long createdAt;
    @ColumnInfo(name = "created_at")
    // Timestamps

    private int usageCount; // Number of documents with this tag
    @ColumnInfo(name = "usage_count", defaultValue = "0")

    private String tagColor; // Hex color code
    @ColumnInfo(name = "tag_color")

    private String tagName;
    @ColumnInfo(name = "tag_name", collate = ColumnInfo.NOCASE)

    private long tagId;
    @ColumnInfo(name = "tag_id")
    @PrimaryKey(autoGenerate = true)

public class Tag {
)
    }
        @Index(value = "created_at")
        @Index(value = "tag_name", unique = true),
    indices = {
    tableName = "tags",
@Entity(
 */
 * - Indexes for performance
 * - Timestamps
 * - Usage count tracking
 * - Color coding
 * - Unique tag names
 * Features:
 *
 * Tag Entity - Represents a tag for categorizing documents
/**

import androidx.room.PrimaryKey;
import androidx.room.Index;
import androidx.room.Entity;
import androidx.room.ColumnInfo;


