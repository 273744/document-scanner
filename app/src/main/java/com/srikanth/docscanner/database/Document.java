package com.srikanth.docscanner.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Document Entity - Represents a scanned document
 * 
 * Features:
 * - Primary key with auto-increment
 * - Foreign key to Folder entity
 * - Timestamps for creation, modification, and sync
 * - Sync status tracking
 * - Metadata fields
 * - Indexes for performance
 */
@Entity(
    tableName = "documents",
    foreignKeys = @ForeignKey(
        entity = Folder.class,
        parentColumns = "folder_id",
        childColumns = "folder_id",
        onDelete = ForeignKey.SET_NULL,
        onUpdate = ForeignKey.CASCADE
    ),
    indices = {
        @Index(value = "document_name"),
        @Index(value = "folder_id"),
        @Index(value = "created_at"),
        @Index(value = "sync_status")
    }
)
public class Document {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "document_id")
    private long documentId;

    @ColumnInfo(name = "document_name", collate = ColumnInfo.NOCASE)
    private String documentName;

    @ColumnInfo(name = "file_path")
    private String filePath;

    @ColumnInfo(name = "file_size")
    private long fileSize;

    @ColumnInfo(name = "file_type")
    private String fileType; // PDF, IMAGE, etc.

    @ColumnInfo(name = "folder_id")
    private Long folderId; // Nullable - can be in root

    @ColumnInfo(name = "thumbnail_path")
    private String thumbnailPath;

    @ColumnInfo(name = "page_count", defaultValue = "1")
    private int pageCount;

    // Timestamps
    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "modified_at")
    private long modifiedAt;

    @ColumnInfo(name = "last_synced_at")
    private Long lastSyncedAt; // Nullable

    // Sync status
    @ColumnInfo(name = "sync_status", defaultValue = "NOT_SYNCED")
    private String syncStatus; // NOT_SYNCED, SYNCING, SYNCED, FAILED

    @ColumnInfo(name = "cloud_id")
    private String cloudId; // ID from cloud storage

    @ColumnInfo(name = "cloud_provider")
    private String cloudProvider; // GOOGLE_DRIVE, DROPBOX

    // Metadata
    @ColumnInfo(name = "ocr_text")
    private String ocrText;

    @ColumnInfo(name = "ocr_language")
    private String ocrLanguage;

    @ColumnInfo(name = "ocr_confidence")
    private Float ocrConfidence;

    @ColumnInfo(name = "is_favorite", defaultValue = "0")
    private boolean isFavorite;

    @ColumnInfo(name = "is_archived", defaultValue = "0")
    private boolean isArchived;

    @ColumnInfo(name = "description")
    private String description;

    @ColumnInfo(name = "notes")
    private String notes;

    // Constructors
    public Document() {
        this.createdAt = System.currentTimeMillis();
        this.modifiedAt = System.currentTimeMillis();
        this.syncStatus = "NOT_SYNCED";
    }

    // Getters and Setters
    public long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(long documentId) {
        this.documentId = documentId;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Long getFolderId() {
        return folderId;
    }

    public void setFolderId(Long folderId) {
        this.folderId = folderId;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
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

    public Long getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(Long lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
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

    public String getOcrText() {
        return ocrText;
    }

    public void setOcrText(String ocrText) {
        this.ocrText = ocrText;
    }

    public String getOcrLanguage() {
        return ocrLanguage;
    }

    public void setOcrLanguage(String ocrLanguage) {
        this.ocrLanguage = ocrLanguage;
    }

    public Float getOcrConfidence() {
        return ocrConfidence;
    }

    public void setOcrConfidence(Float ocrConfidence) {
        this.ocrConfidence = ocrConfidence;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public boolean isArchived() {
        return isArchived;
    }

    public void setArchived(boolean archived) {
        isArchived = archived;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}


