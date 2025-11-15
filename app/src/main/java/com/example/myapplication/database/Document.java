package com.example.myapplication.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Document Entity - Represents a scanned document in the database
 * Uses Room annotations for database mapping
 */
@Entity(tableName = "documents")
public class Document {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private int id;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "file_path")
    private String filePath;

    @ColumnInfo(name = "created_date")
    private long createdDate;

    @ColumnInfo(name = "tags")
    private String tags; // Comma-separated tags

    @ColumnInfo(name = "page_count")
    private int pageCount;

    @ColumnInfo(name = "file_size")
    private long fileSize; // In bytes

    @ColumnInfo(name = "is_favorite")
    private boolean isFavorite;

    @ColumnInfo(name = "pdf_path")
    private String pdfPath; // Path to generated PDF

    @ColumnInfo(name = "thumbnail_path")
    private String thumbnailPath; // Path to thumbnail image

    // Constructors
    public Document() {
        this.createdDate = System.currentTimeMillis();
        this.isFavorite = false;
        this.pageCount = 1;
    }

    @Ignore
    public Document(String name, String filePath) {
        this();
        this.name = name;
        this.filePath = filePath;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(long createdDate) {
        this.createdDate = createdDate;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public String getPdfPath() {
        return pdfPath;
    }

    public void setPdfPath(String pdfPath) {
        this.pdfPath = pdfPath;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    @Override
    public String toString() {
        return "Document{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", filePath='" + filePath + '\'' +
                ", createdDate=" + createdDate +
                ", tags='" + tags + '\'' +
                ", pageCount=" + pageCount +
                ", fileSize=" + fileSize +
                ", isFavorite=" + isFavorite +
                '}';
    }
}

