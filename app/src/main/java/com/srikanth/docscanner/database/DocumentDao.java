package com.srikanth.docscanner.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

/**
 * DocumentDao - Data Access Object for Document entity
 *
 * Features:
 * - CRUD operations
 * - Complex queries with joins
 * - LiveData support for reactive UI
 * - Transaction support
 */
@Dao
public interface DocumentDao {

    // ================================
    // INSERT
    // ================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Document document);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAll(List<Document> documents);

    // ================================
    // UPDATE
    // ================================

    @Update
    int update(Document document);

    @Update
    int updateAll(List<Document> documents);

    @Query("UPDATE documents SET modified_at = :timestamp WHERE document_id = :documentId")
    int updateModifiedTime(long documentId, long timestamp);

    @Query("UPDATE documents SET sync_status = :status, last_synced_at = :timestamp WHERE document_id = :documentId")
    int updateSyncStatus(long documentId, String status, long timestamp);

    @Query("UPDATE documents SET folder_id = :folderId WHERE document_id = :documentId")
    int moveToFolder(long documentId, Long folderId);

    @Query("UPDATE documents SET is_favorite = :isFavorite WHERE document_id = :documentId")
    int setFavorite(long documentId, boolean isFavorite);

    @Query("UPDATE documents SET is_archived = :isArchived WHERE document_id = :documentId")
    int setArchived(long documentId, boolean isArchived);

    // ================================
    // DELETE
    // ================================

    @Delete
    int delete(Document document);

    @Delete
    int deleteAll(List<Document> documents);

    @Query("DELETE FROM documents WHERE document_id = :documentId")
    int deleteById(long documentId);

    @Query("DELETE FROM documents WHERE folder_id = :folderId")
    int deleteByFolder(long folderId);

    @Query("DELETE FROM documents")
    int deleteAllDocuments();

    // ================================
    // SELECT - Basic
    // ================================

    @Query("SELECT * FROM documents WHERE document_id = :documentId")
    Document getById(long documentId);

    @Query("SELECT * FROM documents WHERE document_id = :documentId")
    LiveData<Document> getByIdLive(long documentId);

    @Query("SELECT * FROM documents ORDER BY created_at DESC")
    List<Document> getAll();

    @Query("SELECT * FROM documents ORDER BY created_at DESC")
    LiveData<List<Document>> getAllLive();

    @Query("SELECT * FROM documents ORDER BY created_at DESC LIMIT :limit")
    List<Document> getRecent(int limit);

    @Query("SELECT * FROM documents ORDER BY created_at DESC LIMIT :limit")
    LiveData<List<Document>> getRecentLive(int limit);

    // ================================
    // SELECT - By Folder
    // ================================

    @Query("SELECT * FROM documents WHERE folder_id = :folderId ORDER BY created_at DESC")
    List<Document> getByFolder(long folderId);

    @Query("SELECT * FROM documents WHERE folder_id = :folderId ORDER BY created_at DESC")
    LiveData<List<Document>> getByFolderLive(long folderId);

    @Query("SELECT * FROM documents WHERE folder_id IS NULL ORDER BY created_at DESC")
    List<Document> getRootDocuments();

    @Query("SELECT * FROM documents WHERE folder_id IS NULL ORDER BY created_at DESC")
    LiveData<List<Document>> getRootDocumentsLive();

    // ================================
    // SELECT - Favorites & Archived
    // ================================

    @Query("SELECT * FROM documents WHERE is_favorite = 1 ORDER BY created_at DESC")
    List<Document> getFavorites();

    @Query("SELECT * FROM documents WHERE is_favorite = 1 ORDER BY created_at DESC")
    LiveData<List<Document>> getFavoritesLive();

    @Query("SELECT * FROM documents WHERE is_archived = 1 ORDER BY created_at DESC")
    List<Document> getArchived();

    @Query("SELECT * FROM documents WHERE is_archived = 1 ORDER BY created_at DESC")
    LiveData<List<Document>> getArchivedLive();

    // ================================
    // SELECT - By Sync Status
    // ================================

    @Query("SELECT * FROM documents WHERE sync_status = :status ORDER BY created_at DESC")
    List<Document> getBySyncStatus(String status);

    @Query("SELECT * FROM documents WHERE sync_status = 'NOT_SYNCED' OR sync_status = 'FAILED' ORDER BY created_at DESC")
    List<Document> getUnsynced();

    @Query("SELECT * FROM documents WHERE sync_status = 'NOT_SYNCED' OR sync_status = 'FAILED' ORDER BY created_at DESC")
    LiveData<List<Document>> getUnsyncedLive();

    // ================================
    // SELECT - Search
    // ================================

    @Query("SELECT * FROM documents WHERE document_name LIKE '%' || :query || '%' OR ocr_text LIKE '%' || :query || '%' ORDER BY created_at DESC")
    List<Document> search(String query);

    @Query("SELECT * FROM documents WHERE document_name LIKE '%' || :query || '%' OR ocr_text LIKE '%' || :query || '%' ORDER BY created_at DESC")
    LiveData<List<Document>> searchLive(String query);

    @Query("SELECT * FROM documents WHERE ocr_text LIKE '%' || :query || '%' ORDER BY ocr_confidence DESC")
    List<Document> searchByOCR(String query);

    // ================================
    // SELECT - By Date Range
    // ================================

    @Query("SELECT * FROM documents WHERE created_at BETWEEN :startTime AND :endTime ORDER BY created_at DESC")
    List<Document> getByDateRange(long startTime, long endTime);

    @Query("SELECT * FROM documents WHERE created_at >= :startTime ORDER BY created_at DESC")
    List<Document> getCreatedAfter(long startTime);

    // ================================
    // SELECT - Statistics
    // ================================

    @Query("SELECT COUNT(*) FROM documents")
    int getCount();

    @Query("SELECT COUNT(*) FROM documents")
    LiveData<Integer> getCountLive();

    @Query("SELECT COUNT(*) FROM documents WHERE folder_id = :folderId")
    int getCountByFolder(long folderId);

    @Query("SELECT SUM(file_size) FROM documents")
    long getTotalSize();

    @Query("SELECT SUM(file_size) FROM documents WHERE folder_id = :folderId")
    long getTotalSizeByFolder(long folderId);

    @Query("SELECT COUNT(*) FROM documents WHERE is_favorite = 1")
    int getFavoriteCount();

    @Query("SELECT COUNT(*) FROM documents WHERE sync_status = 'SYNCED'")
    int getSyncedCount();

    // ================================
    // SELECT - By File Type
    // ================================

    @Query("SELECT * FROM documents WHERE file_type = :fileType ORDER BY created_at DESC")
    List<Document> getByFileType(String fileType);

    @Query("SELECT COUNT(*) FROM documents WHERE file_type = :fileType")
    int getCountByFileType(String fileType);

    // ================================
    // Complex Queries with Joins
    // ================================

    /**
     * Get documents with their tags
     */
    @Transaction
    @Query("SELECT * FROM documents WHERE document_id = :documentId")
    DocumentWithTags getDocumentWithTags(long documentId);

    @Transaction
    @Query("SELECT * FROM documents ORDER BY created_at DESC")
    LiveData<List<DocumentWithTags>> getAllDocumentsWithTags();

    /**
     * Get documents by tag
     */
    @Query("SELECT d.* FROM documents d " +
           "INNER JOIN document_tags dt ON d.document_id = dt.document_id " +
           "WHERE dt.tag_id = :tagId " +
           "ORDER BY d.created_at DESC")
    List<Document> getByTag(long tagId);

    @Query("SELECT d.* FROM documents d " +
           "INNER JOIN document_tags dt ON d.document_id = dt.document_id " +
           "WHERE dt.tag_id = :tagId " +
           "ORDER BY d.created_at DESC")
    LiveData<List<Document>> getByTagLive(long tagId);
}


