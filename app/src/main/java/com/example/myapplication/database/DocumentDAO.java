package com.example.myapplication.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * DocumentDAO - Data Access Object for Document entity
 * Provides CRUD operations and advanced queries
 */
@Dao
public interface DocumentDAO {

    // ================== CREATE ==================

    /**
     * Insert a new document
     * @param document Document to insert
     * @return Row ID of inserted document
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Document document);

    /**
     * Insert multiple documents
     * @param documents List of documents to insert
     * @return Array of row IDs
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] insertAll(Document... documents);

    /**
     * Insert multiple documents from list
     * @param documents List of documents
     * @return List of row IDs
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAll(List<Document> documents);

    // ================== READ ==================

    /**
     * Get all documents
     * @return LiveData list of all documents
     */
    @Query("SELECT * FROM documents ORDER BY created_date DESC")
    LiveData<List<Document>> getAllDocuments();

    /**
     * Get all documents (non-LiveData)
     * @return List of all documents
     */
    @Query("SELECT * FROM documents ORDER BY created_date DESC")
    List<Document> getAllDocumentsSync();

    /**
     * Get document by ID
     * @param id Document ID
     * @return Document with specified ID
     */
    @Query("SELECT * FROM documents WHERE id = :id")
    Document getDocumentById(int id);

    /**
     * Get document by ID (LiveData)
     * @param id Document ID
     * @return LiveData document
     */
    @Query("SELECT * FROM documents WHERE id = :id")
    LiveData<Document> getDocumentByIdLive(int id);

    /**
     * Get documents by name (exact match)
     * @param name Document name
     * @return List of matching documents
     */
    @Query("SELECT * FROM documents WHERE name = :name")
    List<Document> getDocumentsByName(String name);

    /**
     * Get favorite documents
     * @return LiveData list of favorite documents
     */
    @Query("SELECT * FROM documents WHERE is_favorite = 1 ORDER BY created_date DESC")
    LiveData<List<Document>> getFavoriteDocuments();

    /**
     * Get recent documents (last 10)
     * @return LiveData list of recent documents
     */
    @Query("SELECT * FROM documents ORDER BY created_date DESC LIMIT 10")
    LiveData<List<Document>> getRecentDocuments();

    /**
     * Get documents count
     * @return Total number of documents
     */
    @Query("SELECT COUNT(*) FROM documents")
    int getDocumentCount();

    /**
     * Get documents count (LiveData)
     * @return LiveData document count
     */
    @Query("SELECT COUNT(*) FROM documents")
    LiveData<Integer> getDocumentCountLive();

    // ================== UPDATE ==================

    /**
     * Update document
     * @param document Document to update
     * @return Number of rows updated
     */
    @Update
    int update(Document document);

    /**
     * Update multiple documents
     * @param documents Documents to update
     * @return Number of rows updated
     */
    @Update
    int updateAll(Document... documents);

    /**
     * Update document name
     * @param id Document ID
     * @param name New name
     * @return Number of rows updated
     */
    @Query("UPDATE documents SET name = :name WHERE id = :id")
    int updateDocumentName(int id, String name);

    /**
     * Update document tags
     * @param id Document ID
     * @param tags New tags
     * @return Number of rows updated
     */
    @Query("UPDATE documents SET tags = :tags WHERE id = :id")
    int updateDocumentTags(int id, String tags);

    /**
     * Toggle favorite status
     * @param id Document ID
     * @param isFavorite Favorite status
     * @return Number of rows updated
     */
    @Query("UPDATE documents SET is_favorite = :isFavorite WHERE id = :id")
    int updateFavoriteStatus(int id, boolean isFavorite);

    /**
     * Update PDF path
     * @param id Document ID
     * @param pdfPath PDF file path
     * @return Number of rows updated
     */
    @Query("UPDATE documents SET pdf_path = :pdfPath WHERE id = :id")
    int updatePdfPath(int id, String pdfPath);

    // ================== DELETE ==================

    /**
     * Delete document
     * @param document Document to delete
     * @return Number of rows deleted
     */
    @Delete
    int delete(Document document);

    /**
     * Delete multiple documents
     * @param documents Documents to delete
     * @return Number of rows deleted
     */
    @Delete
    int deleteAll(Document... documents);

    /**
     * Delete document by ID
     * @param id Document ID
     * @return Number of rows deleted
     */
    @Query("DELETE FROM documents WHERE id = :id")
    int deleteById(int id);

    /**
     * Delete all documents
     * @return Number of rows deleted
     */
    @Query("DELETE FROM documents")
    int deleteAllDocuments();

    /**
     * Delete old documents (older than specified days)
     * @param daysAgo Number of days ago
     * @return Number of rows deleted
     */
    @Query("DELETE FROM documents WHERE created_date < :timestamp")
    int deleteOldDocuments(long timestamp);

    // ================== SEARCH & FILTER ==================

    /**
     * Search documents by name (partial match)
     * @param searchQuery Search query
     * @return LiveData list of matching documents
     */
    @Query("SELECT * FROM documents WHERE name LIKE '%' || :searchQuery || '%' ORDER BY created_date DESC")
    LiveData<List<Document>> searchDocumentsByName(String searchQuery);

    /**
     * Search documents by tags
     * @param tag Tag to search
     * @return LiveData list of matching documents
     */
    @Query("SELECT * FROM documents WHERE tags LIKE '%' || :tag || '%' ORDER BY created_date DESC")
    LiveData<List<Document>> searchDocumentsByTag(String tag);

    /**
     * Search documents by name or tags
     * @param searchQuery Search query
     * @return LiveData list of matching documents
     */
    @Query("SELECT * FROM documents WHERE name LIKE '%' || :searchQuery || '%' OR tags LIKE '%' || :searchQuery || '%' ORDER BY created_date DESC")
    LiveData<List<Document>> searchDocuments(String searchQuery);

    /**
     * Get documents by date range
     * @param startDate Start timestamp
     * @param endDate End timestamp
     * @return LiveData list of documents in date range
     */
    @Query("SELECT * FROM documents WHERE created_date BETWEEN :startDate AND :endDate ORDER BY created_date DESC")
    LiveData<List<Document>> getDocumentsByDateRange(long startDate, long endDate);

    /**
     * Get documents created today
     * @param todayStart Start of today timestamp
     * @return LiveData list of today's documents
     */
    @Query("SELECT * FROM documents WHERE created_date >= :todayStart ORDER BY created_date DESC")
    LiveData<List<Document>> getTodayDocuments(long todayStart);

    /**
     * Filter documents by page count
     * @param minPages Minimum pages
     * @param maxPages Maximum pages
     * @return LiveData list of filtered documents
     */
    @Query("SELECT * FROM documents WHERE page_count BETWEEN :minPages AND :maxPages ORDER BY created_date DESC")
    LiveData<List<Document>> getDocumentsByPageCount(int minPages, int maxPages);

    /**
     * Filter documents by file size
     * @param minSize Minimum file size (bytes)
     * @param maxSize Maximum file size (bytes)
     * @return LiveData list of filtered documents
     */
    @Query("SELECT * FROM documents WHERE file_size BETWEEN :minSize AND :maxSize ORDER BY created_date DESC")
    LiveData<List<Document>> getDocumentsByFileSize(long minSize, long maxSize);

    // ================== STATISTICS ==================

    /**
     * Get total file size of all documents
     * @return Total size in bytes
     */
    @Query("SELECT SUM(file_size) FROM documents")
    long getTotalFileSize();

    /**
     * Get average file size
     * @return Average size in bytes
     */
    @Query("SELECT AVG(file_size) FROM documents")
    long getAverageFileSize();

    /**
     * Get total page count
     * @return Total pages across all documents
     */
    @Query("SELECT SUM(page_count) FROM documents")
    int getTotalPageCount();

    /**
     * Get documents created this week
     * @param weekStart Start of week timestamp
     * @return Document count
     */
    @Query("SELECT COUNT(*) FROM documents WHERE created_date >= :weekStart")
    int getWeeklyDocumentCount(long weekStart);

    /**
     * Get all unique tags
     * @return List of all tags (needs to be parsed)
     */
    @Query("SELECT DISTINCT tags FROM documents WHERE tags IS NOT NULL AND tags != ''")
    List<String> getAllTags();
}

