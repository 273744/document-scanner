package com.srikanth.docscanner.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Folder Entity - Represents a folder for organizing documents
 *
 * Features:
 * - Self-referencing foreign key for nested folders
 * - Parent-child relationship support
 * - Timestamps
 * - Sync status
 * - Indexes for performance
 */
@Entity(
    tableName = "folders",
    foreignKeys = @ForeignKey(
        entity = Folder.class,
        parentColumns = "folder_id",
        childColumns = "parent_folder_id",
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.CASCADE
    ),
    indices = {
        @Index(value = "folder_name"),
        @Index(value = "parent_folder_id"),
        @Index(value = "created_at")
    }
)
public class Folder {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "folder_id")
    private long folderId;

    @ColumnInfo(name = "folder_name", collate = ColumnInfo.NOCASE)
    private String folderName;

    @ColumnInfo(name = "parent_folder_id")
    private Long parentFolderId; // Nullable - null means root folder

    @ColumnInfo(name = "folder_path")
    private String folderPath; // Full path for display

    @ColumnInfo(name = "color")
    private String color; // Folder color for UI

    @ColumnInfo(name = "icon")
    private String icon; // Folder icon identifier

    // Timestamps
    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "modified_at")
    private long modifiedAt;

    // Sync status
    @ColumnInfo(name = "sync_status", defaultValue = "NOT_SYNCED")
    private String syncStatus; // NOT_SYNCED, SYNCING, SYNCED, FAILED

    @ColumnInfo(name = "cloud_id")
    private String cloudId; // ID from cloud storage

    @ColumnInfo(name = "cloud_provider")
    private String cloudProvider; // GOOGLE_DRIVE, DROPBOX

    // Metadata
    @ColumnInfo(name = "description")
    private String description;

    @ColumnInfo(name = "is_pinned", defaultValue = "0")
    private boolean isPinned;

    @ColumnInfo(name = "document_count", defaultValue = "0")
    private int documentCount; // Cached count

    // Constructors
    public Folder() {
        this.createdAt = System.currentTimeMillis();
        this.modifiedAt = System.currentTimeMillis();
        this.syncStatus = "NOT_SYNCED";
        this.documentCount = 0;
    }

    public Folder(String folderName) {
        this();
        this.folderName = folderName;
    }

    // Getters and Setters
    public long getFolderId() {
        return folderId;
    }

    public void setFolderId(long folderId) {
        this.folderId = folderId;
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public Long getParentFolderId() {
        return parentFolderId;
    }

    public void setParentFolderId(Long parentFolderId) {
        this.parentFolderId = parentFolderId;
    }

    public String getFolderPath() {
        return folderPath;
    }

    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
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

    public String getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(String syncStatus) {
        this.syncStatus = syncStatus;
    }

    public String getCloudId() {
        return cloudId;
    }

    public void setCloudId(String cloudId) {
        this.cloudId = cloudId;
    }

    public String getCloudProvider() {
        return cloudProvider;
    }

    public void setCloudProvider(String cloudProvider) {
        this.cloudProvider = cloudProvider;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isPinned() {
        return isPinned;
    }

    public void setPinned(boolean pinned) {
        isPinned = pinned;
    }

    public int getDocumentCount() {
        return documentCount;
    }

    public void setDocumentCount(int documentCount) {
        this.documentCount = documentCount;
    }

    /**
     * Check if this is a root folder
     */
    public boolean isRootFolder() {
        return parentFolderId == null;
    }
}


