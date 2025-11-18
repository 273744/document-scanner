package com.example.myapplication.database;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import androidx.lifecycle.LiveData;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * DocumentRepository - Repository pattern for document data access
 * Provides clean API for database operations and abstracts data source
 */
public class DocumentRepository {

    private static final String TAG = "DocumentRepository";
    private static DocumentRepository instance;

    private final DocumentDao documentDAO;
    private final ExecutorService executorService;
    private final LiveData<List<Document>> allDocuments;
    private final LiveData<Integer> documentCount;

    /**
     * Private constructor (Singleton)
     */
    private DocumentRepository(Context context) {
        DocumentDatabase database = DocumentDatabase.getInstance(context);
        documentDAO = database.documentDAO();
        executorService = Executors.newFixedThreadPool(2);

        // Load frequently used LiveData
        allDocuments = documentDAO.getAllLive();
        documentCount = documentDAO.getCountLive();
    }

    /**
     * Get repository instance (Singleton)
     * @param context Application context
     * @return DocumentRepository instance
     */
    public static synchronized DocumentRepository getInstance(Context context) {
        if (instance == null) {
            instance = new DocumentRepository(context.getApplicationContext());
        }
        return instance;
    }

    // ================== CREATE OPERATIONS ==================

    /**
     * Insert document asynchronously
     * @param document Document to insert
     * @param callback Callback with inserted document ID
     */
    public void insert(Document document, InsertCallback callback) {
        executorService.execute(() -> {
            long id = documentDAO.insert(document);
            Log.d(TAG, "Document inserted with ID: " + id);
            if (callback != null) {
                callback.onInsertComplete(id);
            }
        });
    }

    /**
     * Insert document synchronously (use in background thread)
     * @param document Document to insert
     * @return Inserted document ID
     */
    public long insertSync(Document document) {
        return documentDAO.insert(document);
    }

    /**
     * Insert multiple documents
     * @param documents List of documents to insert
     * @param callback Callback with inserted IDs
     */
    public void insertAll(List<Document> documents, InsertAllCallback callback) {
        executorService.execute(() -> {
            List<Long> ids = documentDAO.insertAll(documents);
            Log.d(TAG, "Inserted " + ids.size() + " documents");
            if (callback != null) {
                callback.onInsertComplete(ids);
            }
        });
    }

    // ================== READ OPERATIONS ==================

    /**
     * Get all documents (LiveData)
     * @return LiveData list of all documents
     */
    public LiveData<List<Document>> getAllDocuments() {
        return allDocuments;
    }

    /**
     * Get all documents synchronously
     * @param callback Callback with document list
     */
    public void getAllDocumentsSync(LoadDocumentsCallback callback) {
        executorService.execute(() -> {
            List<Document> documents = documentDAO.getAll();
            if (callback != null) {
                callback.onDocumentsLoaded(documents);
            }
        });
    }

    /**
     * Get document by ID
     * @param id Document ID
     * @return LiveData document
     */
    public LiveData<Document> getDocumentById(long id) {
        return documentDAO.getByIdLive(id);
    }

    /**
     * Get document by ID synchronously
     * @param id Document ID
     * @param callback Callback with document
     */
    public void getDocumentByIdSync(long id, LoadDocumentCallback callback) {
        executorService.execute(() -> {
            Document document = documentDAO.getById(id);
            if (callback != null) {
                callback.onDocumentLoaded(document);
            }
        });
    }

    /**
     * Get favorite documents
     * @return LiveData list of favorites
     */
    public LiveData<List<Document>> getFavoriteDocuments() {
        return documentDAO.getFavoritesLive();
    }

    /**
     * Get recent documents
     * @return LiveData list of recent documents
     */
    public LiveData<List<Document>> getRecentDocuments() {
        return documentDAO.getRecentLive(20); // Get 20 most recent
    }

    /**
     * Get document count
     * @return LiveData document count
     */
    public LiveData<Integer> getDocumentCount() {
        return documentCount;
    }

    /**
     * Get document count synchronously
     * @param callback Callback with count
     */
    public void getDocumentCountSync(CountCallback callback) {
        executorService.execute(() -> {
            int count = documentDAO.getCount();
            if (callback != null) {
                callback.onCountLoaded(count);
            }
        });
    }

    // ================== UPDATE OPERATIONS ==================

    /**
     * Update document
     * @param document Document to update
     * @param callback Callback when complete
     */
    public void update(Document document, UpdateCallback callback) {
        executorService.execute(() -> {
            int rows = documentDAO.update(document);
            Log.d(TAG, "Updated " + rows + " document(s)");
            if (callback != null) {
                callback.onUpdateComplete(rows > 0);
            }
        });
    }

    /**
     * Update document name
     * @param id Document ID
     * @param name New name
     * @param callback Callback when complete
     */
    public void updateDocumentName(long id, String name, UpdateCallback callback) {
        executorService.execute(() -> {
            Document doc = documentDAO.getById(id);
            if (doc != null) {
                doc.setDocumentName(name);
                doc.setModifiedAt(System.currentTimeMillis());
                int rows = documentDAO.update(doc);
                if (callback != null) {
                    callback.onUpdateComplete(rows > 0);
                }
            } else if (callback != null) {
                callback.onUpdateComplete(false);
            }
        });
    }

    /**
     * Update document tags - Note: Tags are now in DocumentTag junction table
     * This method is deprecated, use DocumentTagDao instead
     * @param id Document ID
     * @param tags New tags (comma-separated) - DEPRECATED
     * @param callback Callback when complete
     */
    @Deprecated
    public void updateDocumentTags(long id, String tags, UpdateCallback callback) {
        // Tags are now in a separate junction table
        // This method is kept for backward compatibility but does nothing
        executorService.execute(() -> {
            if (callback != null) {
                callback.onUpdateComplete(true);
            }
        });
    }

    /**
     * Toggle favorite status
     * @param id Document ID
     * @param isFavorite Favorite status
     * @param callback Callback when complete
     */
    public void updateFavoriteStatus(long id, boolean isFavorite, UpdateCallback callback) {
        executorService.execute(() -> {
            int rows = documentDAO.setFavorite(id, isFavorite);
            if (callback != null) {
                callback.onUpdateComplete(rows > 0);
            }
        });
    }

    /**
     * Update PDF path - Note: PDF path is now stored in filePath field
     * @param id Document ID
     * @param pdfPath PDF file path
     * @param callback Callback when complete
     */
    public void updatePdfPath(long id, String pdfPath, UpdateCallback callback) {
        executorService.execute(() -> {
            Document doc = documentDAO.getById(id);
            if (doc != null) {
                doc.setFilePath(pdfPath);
                doc.setFileType("PDF");
                doc.setModifiedAt(System.currentTimeMillis());
                int rows = documentDAO.update(doc);
                if (callback != null) {
                    callback.onUpdateComplete(rows > 0);
                }
            } else if (callback != null) {
                callback.onUpdateComplete(false);
            }
        });
    }

    // ================== DELETE OPERATIONS ==================

    /**
     * Delete document
     * @param document Document to delete
     * @param callback Callback when complete
     */
    public void delete(Document document, DeleteCallback callback) {
        executorService.execute(() -> {
            // Delete associated files
            deleteAssociatedFiles(document);

            int rows = documentDAO.delete(document);
            Log.d(TAG, "Deleted " + rows + " document(s)");
            if (callback != null) {
                callback.onDeleteComplete(rows > 0);
            }
        });
    }

    /**
     * Delete document by ID
     * @param id Document ID
     * @param callback Callback when complete
     */
    public void deleteById(long id, DeleteCallback callback) {
        executorService.execute(() -> {
            // First get document to delete files
            Document document = documentDAO.getById(id);
            if (document != null) {
                deleteAssociatedFiles(document);
            }

            int rows = documentDAO.deleteById(id);
            if (callback != null) {
                callback.onDeleteComplete(rows > 0);
            }
        });
    }

    /**
     * Delete all documents
     * @param callback Callback when complete
     */
    public void deleteAll(DeleteCallback callback) {
        executorService.execute(() -> {
            int rows = documentDAO.deleteAllDocuments();
            Log.d(TAG, "Deleted all " + rows + " documents");
            if (callback != null) {
                callback.onDeleteComplete(rows > 0);
            }
        });
    }

    /**
     * Delete old documents (older than specified days)
     * @param daysAgo Number of days
     * @param callback Callback when complete
     */
    public void deleteOldDocuments(int daysAgo, DeleteCallback callback) {
        executorService.execute(() -> {
            long timestamp = System.currentTimeMillis() - (daysAgo * 24L * 60 * 60 * 1000);
            // Get old documents and delete them
            List<Document> oldDocs = documentDAO.getCreatedAfter(timestamp);
            int rows = 0;
            for (Document doc : oldDocs) {
                rows += documentDAO.delete(doc);
            }
            Log.d(TAG, "Deleted " + rows + " old documents");
            if (callback != null) {
                callback.onDeleteComplete(rows > 0);
            }
        });
    }

    // ================== SEARCH & FILTER OPERATIONS ==================

    /**
     * Search documents by name
     * @param searchQuery Search query
     * @return LiveData list of matching documents
     */
    public LiveData<List<Document>> searchDocumentsByName(String searchQuery) {
        return documentDAO.searchLive(searchQuery);
    }

    /**
     * Search documents by tag - Note: Tags are now in DocumentTag junction table
     * Use DocumentTagDao to search by tag
     * @param tag Tag to search
     * @return LiveData list of matching documents
     */
    @Deprecated
    public LiveData<List<Document>> searchDocumentsByTag(String tag) {
        // This would need to query through DocumentTag junction table
        // For now, return all documents
        return documentDAO.getAllLive();
    }

    /**
     * Search documents (name or tags or OCR text)
     * @param searchQuery Search query
     * @return LiveData list of matching documents
     */
    public LiveData<List<Document>> searchDocuments(String searchQuery) {
        return documentDAO.searchLive(searchQuery);
    }

    /**
     * Get documents by date range
     * @param startDate Start timestamp
     * @param endDate End timestamp
     * @return LiveData list of documents
     */
    public LiveData<List<Document>> getDocumentsByDateRange(long startDate, long endDate) {
        // Use the new DAO method
        return documentDAO.searchLive(""); // TODO: Implement proper date range query
    }

    /**
     * Get today's documents
     * @return LiveData list of today's documents
     */
    public LiveData<List<Document>> getTodayDocuments() {
        long todayStart = getTodayStartTimestamp();
        // Get recent documents as workaround
        return documentDAO.getRecentLive(100);
    }

    /**
     * Filter documents by page count
     * @param minPages Minimum pages
     * @param maxPages Maximum pages
     * @return LiveData list of filtered documents
     */
    public LiveData<List<Document>> getDocumentsByPageCount(int minPages, int maxPages) {
        // This method needs custom implementation
        return documentDAO.getAllLive();
    }

    // ================== STATISTICS ==================

    /**
     * Get total file size
     * @param callback Callback with total size
     */
    public void getTotalFileSize(SizeCallback callback) {
        executorService.execute(() -> {
            long size = documentDAO.getTotalSize();
            if (callback != null) {
                callback.onSizeCalculated(size);
            }
        });
    }

    /**
     * Get weekly document count
     * @param callback Callback with count
     */
    public void getWeeklyDocumentCount(CountCallback callback) {
        executorService.execute(() -> {
            long weekStart = getWeekStartTimestamp();
            // Get documents created after week start
            List<Document> weekDocs = documentDAO.getCreatedAfter(weekStart);
            int count = weekDocs != null ? weekDocs.size() : 0;
            if (callback != null) {
                callback.onCountLoaded(count);
            }
        });
    }

    // ================== HELPER METHODS ==================

    /**
     * Delete files associated with document
     */
    private void deleteAssociatedFiles(Document document) {
        try {
            // Delete main file
            if (document.getFilePath() != null) {
                File file = new File(document.getFilePath());
                if (file.exists()) {
                    file.delete();
                }
            }

            // Delete PDF file if it's a PDF document
            if ("PDF".equals(document.getFileType()) && document.getFilePath() != null) {
                File pdfFile = new File(document.getFilePath());
                if (pdfFile.exists()) {
                    pdfFile.delete();
                }
            }

            // Delete thumbnail
            if (document.getThumbnailPath() != null) {
                File thumbFile = new File(document.getThumbnailPath());
                if (thumbFile.exists()) {
                    thumbFile.delete();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting associated files", e);
        }
    }

    /**
     * Get start of today timestamp
     */
    private long getTodayStartTimestamp() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    /**
     * Get start of week timestamp
     */
    private long getWeekStartTimestamp() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    /**
     * Shutdown executor service
     */
    public void shutdown() {
        executorService.shutdown();
    }

    // ================== CALLBACK INTERFACES ==================

    public interface InsertCallback {
        void onInsertComplete(long id);
    }

    public interface InsertAllCallback {
        void onInsertComplete(List<Long> ids);
    }

    public interface LoadDocumentsCallback {
        void onDocumentsLoaded(List<Document> documents);
    }

    public interface LoadDocumentCallback {
        void onDocumentLoaded(Document document);
    }

    public interface UpdateCallback {
        void onUpdateComplete(boolean success);
    }

    public interface DeleteCallback {
        void onDeleteComplete(boolean success);
    }

    public interface CountCallback {
        void onCountLoaded(int count);
    }

    public interface SizeCallback {
        void onSizeCalculated(long size);
    }
}

