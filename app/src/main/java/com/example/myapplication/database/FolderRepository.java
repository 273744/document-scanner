package com.example.myapplication.database;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.LruCache;

import androidx.lifecycle.LiveData;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * FolderRepository - Repository for folder operations
 *
 * Features:
 * - CRUD operations for folders and subfolders
 * - Move documents between folders
 * - Bulk folder operations
 * - Folder hierarchy navigation
 * - Auto-categorization based on content
 * - Folder statistics
 * - Recent folders and quick access
 * - Caching strategies
 * - Offline support
 */
public class FolderRepository {

    private static final String TAG = "FolderRepository";

    // Database
    private final FolderDao folderDao;
    private final DocumentDao documentDao;
    private final AppDatabase database;

    // Threading
    private final ExecutorService executorService;
    private final Handler mainHandler;

    // Caching
    private final LruCache<Long, Folder> folderCache;
    private final LruCache<String, List<Folder>> folderListCache;
    private final Map<Long, FolderStatistics> statsCache;

    // Recent folders
    private final List<Long> recentFolderIds;
    private static final int MAX_RECENT_FOLDERS = 10;

    /**
     * Constructor
     */
    public FolderRepository(Context context) {
        this.database = AppDatabase.getInstance(context);
        this.folderDao = database.folderDao();
        this.documentDao = database.documentDao();

        this.executorService = Executors.newFixedThreadPool(4);
        this.mainHandler = new Handler(Looper.getMainLooper());

        // Initialize cache (max 100 folders)
        this.folderCache = new LruCache<>(100);
        this.folderListCache = new LruCache<>(20);
        this.statsCache = new HashMap<>();

        this.recentFolderIds = new ArrayList<>();

        Log.d(TAG, "FolderRepository initialized");
    }

    // ================================
    // 1. CRUD Operations for Folders
    // ================================

    /**
     * Create a new folder
     */
    public void createFolder(String folderName, Long parentId, FolderCallback callback) {
        executorService.execute(() -> {
            try {
                Folder folder = new Folder(folderName);
                folder.setParentFolderId(parentId);

                // Build folder path
                String path = buildFolderPath(parentId, folderName);
                folder.setFolderPath(path);

                long folderId = folderDao.insert(folder);
                folder.setFolderId(folderId);

                // Cache the folder
                folderCache.put(folderId, folder);
                invalidateFolderListCache();

                mainHandler.post(() -> callback.onSuccess(folder));

                Log.d(TAG, "Folder created: " + folderName + " (ID: " + folderId + ")");

            } catch (Exception e) {
                Log.e(TAG, "Error creating folder", e);
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    /**
     * Get folder by ID
     */
    public void getFolderById(long folderId, FolderCallback callback) {
        // Check cache first
        Folder cached = folderCache.get(folderId);
        if (cached != null) {
            callback.onSuccess(cached);
            return;
        }

        executorService.execute(() -> {
            try {
                Folder folder = folderDao.getById(folderId);

                if (folder != null) {
                    folderCache.put(folderId, folder);
                    mainHandler.post(() -> callback.onSuccess(folder));
                } else {
                    mainHandler.post(() -> callback.onError(
                        new Exception("Folder not found: " + folderId)));
                }

            } catch (Exception e) {
                Log.e(TAG, "Error getting folder", e);
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    /**
     * Get folder by ID (LiveData)
     */
    public LiveData<Folder> getFolderByIdLive(long folderId) {
        return folderDao.getByIdLive(folderId);
    }

    /**
     * Update folder
     */
    public void updateFolder(Folder folder, OperationCallback callback) {
        executorService.execute(() -> {
            try {
                folder.setModifiedAt(System.currentTimeMillis());
                int updated = folderDao.update(folder);

                // Update cache
                folderCache.put(folder.getFolderId(), folder);
                invalidateFolderListCache();

                boolean success = updated > 0;
                mainHandler.post(() -> callback.onComplete(success));

                Log.d(TAG, "Folder updated: " + folder.getFolderName());

            } catch (Exception e) {
                Log.e(TAG, "Error updating folder", e);
                mainHandler.post(() -> callback.onComplete(false));
            }
        });
    }

    /**
     * Delete folder
     */
    public void deleteFolder(long folderId, OperationCallback callback) {
        executorService.execute(() -> {
            try {
                // Check if folder has subfolders
                List<Folder> subfolders = folderDao.getSubfolders(folderId);
                if (!subfolders.isEmpty()) {
                    mainHandler.post(() -> callback.onComplete(false));
                    Log.w(TAG, "Cannot delete folder with subfolders: " + folderId);
                    return;
                }

                // Delete folder (documents will have folder_id set to NULL due to ON DELETE SET_NULL)
                int deleted = folderDao.deleteById(folderId);

                // Clear cache
                folderCache.remove(folderId);
                statsCache.remove(folderId);
                invalidateFolderListCache();

                boolean success = deleted > 0;
                mainHandler.post(() -> callback.onComplete(success));

                Log.d(TAG, "Folder deleted: " + folderId);

            } catch (Exception e) {
                Log.e(TAG, "Error deleting folder", e);
                mainHandler.post(() -> callback.onComplete(false));
            }
        });
    }

    /**
     * Delete folder with all contents
     */
    public void deleteFolderRecursively(long folderId, OperationCallback callback) {
        executorService.execute(() -> {
            try {
                // Delete all subfolders recursively
                List<Folder> subfolders = folderDao.getSubfolders(folderId);
                for (Folder subfolder : subfolders) {
                    deleteFolderRecursivelySync(subfolder.getFolderId());
                }

                // Delete all documents in folder
                documentDao.deleteByFolder(folderId);

                // Delete folder itself
                int deleted = folderDao.deleteById(folderId);

                // Clear cache
                folderCache.remove(folderId);
                statsCache.remove(folderId);
                invalidateFolderListCache();

                boolean success = deleted > 0;
                mainHandler.post(() -> callback.onComplete(success));

                Log.d(TAG, "Folder deleted recursively: " + folderId);

            } catch (Exception e) {
                Log.e(TAG, "Error deleting folder recursively", e);
                mainHandler.post(() -> callback.onComplete(false));
            }
        });
    }

    /**
     * Delete folder recursively (synchronous)
     */
    private void deleteFolderRecursivelySync(long folderId) {
        List<Folder> subfolders = folderDao.getSubfolders(folderId);
        for (Folder subfolder : subfolders) {
            deleteFolderRecursivelySync(subfolder.getFolderId());
        }
        documentDao.deleteByFolder(folderId);
        folderDao.deleteById(folderId);
    }

    // ================================
    // 2. Move Documents Between Folders
    // ================================

    /**
     * Move document to folder
     */
    public void moveDocumentToFolder(long documentId, Long targetFolderId, OperationCallback callback) {
        executorService.execute(() -> {
            try {
                int updated = documentDao.moveToFolder(documentId, targetFolderId);

                // Update folder statistics
                if (updated > 0) {
                    updateFolderDocumentCountsAsync();
                }

                boolean success = updated > 0;
                mainHandler.post(() -> callback.onComplete(success));

                Log.d(TAG, "Document moved: " + documentId + " to folder: " + targetFolderId);

            } catch (Exception e) {
                Log.e(TAG, "Error moving document", e);
                mainHandler.post(() -> callback.onComplete(false));
            }
        });
    }

    /**
     * Move multiple documents to folder
     */
    public void moveDocumentsToFolder(List<Long> documentIds, Long targetFolderId,
                                     BatchOperationCallback callback) {
        executorService.execute(() -> {
            int successCount = 0;
            int failureCount = 0;

            for (long documentId : documentIds) {
                try {
                    int updated = documentDao.moveToFolder(documentId, targetFolderId);
                    if (updated > 0) {
                        successCount++;
                    } else {
                        failureCount++;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error moving document: " + documentId, e);
                    failureCount++;
                }
            }

            // Update folder statistics
            updateFolderDocumentCountsAsync();

            int finalSuccess = successCount;
            int finalFailure = failureCount;
            mainHandler.post(() -> callback.onComplete(finalSuccess, finalFailure));

            Log.d(TAG, "Batch move complete: " + successCount + " success, " + failureCount + " failed");
        });
    }

    // ================================
    // 3. Bulk Folder Operations
    // ================================

    /**
     * Create multiple folders
     */
    public void createFolders(List<String> folderNames, Long parentId,
                             BatchOperationCallback callback) {
        executorService.execute(() -> {
            int successCount = 0;
            int failureCount = 0;

            for (String name : folderNames) {
                try {
                    Folder folder = new Folder(name);
                    folder.setParentFolderId(parentId);
                    folder.setFolderPath(buildFolderPath(parentId, name));

                    long id = folderDao.insert(folder);
                    if (id > 0) {
                        successCount++;
                    } else {
                        failureCount++;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error creating folder: " + name, e);
                    failureCount++;
                }
            }

            invalidateFolderListCache();

            int finalSuccess = successCount;
            int finalFailure = failureCount;
            mainHandler.post(() -> callback.onComplete(finalSuccess, finalFailure));

            Log.d(TAG, "Batch create complete: " + successCount + " success");
        });
    }

    /**
     * Delete multiple folders
     */
    public void deleteFolders(List<Long> folderIds, BatchOperationCallback callback) {
        executorService.execute(() -> {
            int successCount = 0;
            int failureCount = 0;

            for (long folderId : folderIds) {
                try {
                    int deleted = folderDao.deleteById(folderId);
                    if (deleted > 0) {
                        folderCache.remove(folderId);
                        statsCache.remove(folderId);
                        successCount++;
                    } else {
                        failureCount++;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error deleting folder: " + folderId, e);
                    failureCount++;
                }
            }

            invalidateFolderListCache();

            int finalSuccess = successCount;
            int finalFailure = failureCount;
            mainHandler.post(() -> callback.onComplete(finalSuccess, finalFailure));

            Log.d(TAG, "Batch delete complete: " + successCount + " success");
        });
    }

    /**
     * Merge folders - move all documents from source to target
     */
    public void mergeFolders(long sourceFolderId, long targetFolderId,
                            OperationCallback callback) {
        executorService.execute(() -> {
            try {
                // Get all documents in source folder
                List<Document> documents = documentDao.getByFolder(sourceFolderId);

                // Move all documents to target
                for (Document doc : documents) {
                    documentDao.moveToFolder(doc.getDocumentId(), targetFolderId);
                }

                // Delete source folder
                folderDao.deleteById(sourceFolderId);

                // Update statistics
                updateFolderDocumentCountsAsync();

                mainHandler.post(() -> callback.onComplete(true));

                Log.d(TAG, "Folders merged: " + sourceFolderId + " -> " + targetFolderId);

            } catch (Exception e) {
                Log.e(TAG, "Error merging folders", e);
                mainHandler.post(() -> callback.onComplete(false));
            }
        });
    }

    // ================================
    // 4. Folder Hierarchy Navigation
    // ================================

    /**
     * Get root folders
     */
    public void getRootFolders(FolderListCallback callback) {
        String cacheKey = "root_folders";

        // Check cache
        List<Folder> cached = folderListCache.get(cacheKey);
        if (cached != null) {
            callback.onSuccess(cached);
            return;
        }

        executorService.execute(() -> {
            try {
                List<Folder> folders = folderDao.getRootFolders();
                folderListCache.put(cacheKey, folders);

                mainHandler.post(() -> callback.onSuccess(folders));

            } catch (Exception e) {
                Log.e(TAG, "Error getting root folders", e);
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    /**
     * Get root folders (LiveData)
     */
    public LiveData<List<Folder>> getRootFoldersLive() {
        return folderDao.getRootFoldersLive();
    }

    /**
     * Get subfolders
     */
    public void getSubfolders(long parentId, FolderListCallback callback) {
        String cacheKey = "subfolders_" + parentId;

        // Check cache
        List<Folder> cached = folderListCache.get(cacheKey);
        if (cached != null) {
            callback.onSuccess(cached);
            return;
        }

        executorService.execute(() -> {
            try {
                List<Folder> folders = folderDao.getSubfolders(parentId);
                folderListCache.put(cacheKey, folders);

                mainHandler.post(() -> callback.onSuccess(folders));

            } catch (Exception e) {
                Log.e(TAG, "Error getting subfolders", e);
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    /**
     * Get subfolders (LiveData)
     */
    public LiveData<List<Folder>> getSubfoldersLive(long parentId) {
        return folderDao.getSubfoldersLive(parentId);
    }

    /**
     * Get folder hierarchy (parent to root)
     */
    public void getFolderHierarchy(long folderId, FolderListCallback callback) {
        executorService.execute(() -> {
            try {
                List<Folder> hierarchy = new ArrayList<>();
                Long currentId = folderId;

                while (currentId != null) {
                    Folder folder = folderDao.getById(currentId);
                    if (folder != null) {
                        hierarchy.add(0, folder); // Add to beginning
                        currentId = folder.getParentFolderId();
                    } else {
                        break;
                    }
                }

                mainHandler.post(() -> callback.onSuccess(hierarchy));

            } catch (Exception e) {
                Log.e(TAG, "Error getting folder hierarchy", e);
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    /**
     * Get all folders (flat list)
     */
    public void getAllFolders(FolderListCallback callback) {
        String cacheKey = "all_folders";

        List<Folder> cached = folderListCache.get(cacheKey);
        if (cached != null) {
            callback.onSuccess(cached);
            return;
        }

        executorService.execute(() -> {
            try {
                List<Folder> folders = folderDao.getAll();
                folderListCache.put(cacheKey, folders);

                mainHandler.post(() -> callback.onSuccess(folders));

            } catch (Exception e) {
                Log.e(TAG, "Error getting all folders", e);
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    /**
     * Get all folders (LiveData)
     */
    public LiveData<List<Folder>> getAllFoldersLive() {
        return folderDao.getAllLive();
    }

    // ================================
    // 5. Auto-Categorization
    // ================================

    /**
     * Auto-categorize document based on content
     */
    public void autoCategorizeDocument(Document document, FolderCallback callback) {
        executorService.execute(() -> {
            try {
                Long suggestedFolderId = determineFolderFromContent(document);

                if (suggestedFolderId != null) {
                    Folder folder = folderDao.getById(suggestedFolderId);
                    mainHandler.post(() -> callback.onSuccess(folder));
                } else {
                    mainHandler.post(() -> callback.onError(
                        new Exception("No suitable folder found")));
                }

            } catch (Exception e) {
                Log.e(TAG, "Error auto-categorizing document", e);
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    /**
     * Determine folder from document content
     */
    private Long determineFolderFromContent(Document document) {
        String fileName = document.getDocumentName();
        String ocrText = document.getOcrText();

        // Search for existing folders by name pattern
        List<Folder> allFolders = folderDao.getAll();

        // Check file name for keywords
        if (fileName != null) {
            String lowerName = fileName.toLowerCase();

            for (Folder folder : allFolders) {
                String folderName = folder.getFolderName().toLowerCase();

                if (lowerName.contains(folderName) || folderName.contains(lowerName)) {
                    return folder.getFolderId();
                }
            }
        }

        // Check OCR text for keywords
        if (ocrText != null && !ocrText.isEmpty()) {
            String lowerText = ocrText.toLowerCase();

            // Predefined categories with keywords
            Map<String, String[]> categoryKeywords = new HashMap<>();
            categoryKeywords.put("work", new String[]{"invoice", "receipt", "contract", "agreement"});
            categoryKeywords.put("personal", new String[]{"letter", "note", "reminder", "personal"});
            categoryKeywords.put("financial", new String[]{"bank", "statement", "tax", "payment"});

            for (Map.Entry<String, String[]> entry : categoryKeywords.entrySet()) {
                for (String keyword : entry.getValue()) {
                    if (lowerText.contains(keyword)) {
                        // Find folder with matching name
                        for (Folder folder : allFolders) {
                            if (folder.getFolderName().equalsIgnoreCase(entry.getKey())) {
                                return folder.getFolderId();
                            }
                        }
                    }
                }
            }
        }

        return null; // No match found
    }

    /**
     * Auto-categorize multiple documents
     */
    public void autoCategorizeDocuments(List<Document> documents,
                                       BatchOperationCallback callback) {
        executorService.execute(() -> {
            int successCount = 0;
            int failureCount = 0;

            for (Document document : documents) {
                try {
                    Long folderId = determineFolderFromContent(document);

                    if (folderId != null) {
                        documentDao.moveToFolder(document.getDocumentId(), folderId);
                        successCount++;
                    } else {
                        failureCount++;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error auto-categorizing document", e);
                    failureCount++;
                }
            }

            updateFolderDocumentCountsAsync();

            int finalSuccess = successCount;
            int finalFailure = failureCount;
            mainHandler.post(() -> callback.onComplete(finalSuccess, finalFailure));

            Log.d(TAG, "Auto-categorization complete: " + successCount + " categorized");
        });
    }

    // ================================
    // 6. Folder Statistics
    // ================================

    /**
     * Get folder statistics
     */
    public void getFolderStatistics(long folderId, StatisticsCallback callback) {
        // Check cache
        FolderStatistics cached = statsCache.get(folderId);
        if (cached != null) {
            callback.onSuccess(cached);
            return;
        }

        executorService.execute(() -> {
            try {
                FolderStatistics stats = calculateFolderStatistics(folderId);
                statsCache.put(folderId, stats);

                mainHandler.post(() -> callback.onSuccess(stats));

            } catch (Exception e) {
                Log.e(TAG, "Error getting folder statistics", e);
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    /**
     * Calculate folder statistics
     */
    private FolderStatistics calculateFolderStatistics(long folderId) {
        FolderStatistics stats = new FolderStatistics();
        stats.folderId = folderId;

        // Document count
        stats.documentCount = documentDao.getCountByFolder(folderId);

        // Total size
        stats.totalSize = documentDao.getTotalSizeByFolder(folderId);

        // File type breakdown
        stats.pdfCount = countDocumentsByTypeInFolder(folderId, "PDF");
        stats.imageCount = countDocumentsByTypeInFolder(folderId, "IMAGE");

        // Subfolder count
        List<Folder> subfolders = folderDao.getSubfolders(folderId);
        stats.subfolderCount = subfolders.size();

        // Calculate recursive totals
        for (Folder subfolder : subfolders) {
            FolderStatistics subStats = calculateFolderStatistics(subfolder.getFolderId());
            stats.totalDocumentsRecursive += subStats.documentCount + subStats.totalDocumentsRecursive;
            stats.totalSizeRecursive += subStats.totalSize + subStats.totalSizeRecursive;
        }

        stats.totalDocumentsRecursive += stats.documentCount;
        stats.totalSizeRecursive += stats.totalSize;

        return stats;
    }

    /**
     * Count documents by type in folder
     */
    private int countDocumentsByTypeInFolder(long folderId, String fileType) {
        List<Document> documents = documentDao.getByFolder(folderId);
        int count = 0;

        for (Document doc : documents) {
            if (fileType.equalsIgnoreCase(doc.getFileType())) {
                count++;
            }
        }

        return count;
    }

    /**
     * Update folder document counts
     */
    private void updateFolderDocumentCountsAsync() {
        executorService.execute(() -> {
            try {
                List<Folder> allFolders = folderDao.getAll();

                for (Folder folder : allFolders) {
                    int count = documentDao.getCountByFolder(folder.getFolderId());
                    folderDao.updateDocumentCount(folder.getFolderId(), count);
                }

                // Clear stats cache
                statsCache.clear();

                Log.d(TAG, "Folder document counts updated");

            } catch (Exception e) {
                Log.e(TAG, "Error updating folder document counts", e);
            }
        });
    }

    // ================================
    // 7. Recent Folders and Quick Access
    // ================================

    /**
     * Add folder to recent list
     */
    public void addToRecentFolders(long folderId) {
        synchronized (recentFolderIds) {
            // Remove if already exists
            recentFolderIds.remove(Long.valueOf(folderId));

            // Add to beginning
            recentFolderIds.add(0, folderId);

            // Keep only MAX_RECENT_FOLDERS
            while (recentFolderIds.size() > MAX_RECENT_FOLDERS) {
                recentFolderIds.remove(recentFolderIds.size() - 1);
            }
        }

        Log.d(TAG, "Added to recent folders: " + folderId);
    }

    /**
     * Get recent folders
     */
    public void getRecentFolders(FolderListCallback callback) {
        executorService.execute(() -> {
            try {
                List<Folder> recentFolders = new ArrayList<>();

                synchronized (recentFolderIds) {
                    for (Long folderId : recentFolderIds) {
                        Folder folder = folderDao.getById(folderId);
                        if (folder != null) {
                            recentFolders.add(folder);
                        }
                    }
                }

                mainHandler.post(() -> callback.onSuccess(recentFolders));

            } catch (Exception e) {
                Log.e(TAG, "Error getting recent folders", e);
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    /**
     * Get pinned folders
     */
    public void getPinnedFolders(FolderListCallback callback) {
        executorService.execute(() -> {
            try {
                List<Folder> folders = folderDao.getPinnedFolders();
                mainHandler.post(() -> callback.onSuccess(folders));

            } catch (Exception e) {
                Log.e(TAG, "Error getting pinned folders", e);
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    /**
     * Get pinned folders (LiveData)
     */
    public LiveData<List<Folder>> getPinnedFoldersLive() {
        return folderDao.getPinnedFoldersLive();
    }

    /**
     * Toggle folder pin status
     */
    public void toggleFolderPin(long folderId, OperationCallback callback) {
        executorService.execute(() -> {
            try {
                Folder folder = folderDao.getById(folderId);

                if (folder != null) {
                    folder.setPinned(!folder.isPinned());
                    folder.setModifiedAt(System.currentTimeMillis());

                    int updated = folderDao.update(folder);

                    // Update cache
                    folderCache.put(folderId, folder);
                    invalidateFolderListCache();

                    boolean success = updated > 0;
                    mainHandler.post(() -> callback.onComplete(success));

                    Log.d(TAG, "Folder pin toggled: " + folderId + " -> " + folder.isPinned());
                } else {
                    mainHandler.post(() -> callback.onComplete(false));
                }

            } catch (Exception e) {
                Log.e(TAG, "Error toggling folder pin", e);
                mainHandler.post(() -> callback.onComplete(false));
            }
        });
    }

    // ================================
    // Search
    // ================================

    /**
     * Search folders by name
     */
    public void searchFolders(String query, FolderListCallback callback) {
        executorService.execute(() -> {
            try {
                List<Folder> folders = folderDao.search(query);
                mainHandler.post(() -> callback.onSuccess(folders));

            } catch (Exception e) {
                Log.e(TAG, "Error searching folders", e);
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    // ================================
    // Helper Methods
    // ================================

    /**
     * Build folder path
     */
    private String buildFolderPath(Long parentId, String folderName) {
        if (parentId == null) {
            return "/" + folderName;
        }

        Folder parent = folderDao.getById(parentId);
        if (parent != null && parent.getFolderPath() != null) {
            return parent.getFolderPath() + "/" + folderName;
        }

        return "/" + folderName;
    }

    /**
     * Invalidate folder list cache
     */
    private void invalidateFolderListCache() {
        folderListCache.evictAll();
    }

    /**
     * Clear all caches
     */
    public void clearCache() {
        folderCache.evictAll();
        folderListCache.evictAll();
        statsCache.clear();
        Log.d(TAG, "All caches cleared");
    }

    // ================================
    // Data Classes
    // ================================

    /**
     * Folder statistics
     */
    public static class FolderStatistics {
        public long folderId;
        public int documentCount;
        public long totalSize;
        public int pdfCount;
        public int imageCount;
        public int subfolderCount;
        public int totalDocumentsRecursive;
        public long totalSizeRecursive;
    }

    // ================================
    // Callbacks
    // ================================

    public interface FolderCallback {
        void onSuccess(Folder folder);
        void onError(Exception e);
    }

    public interface FolderListCallback {
        void onSuccess(List<Folder> folders);
        void onError(Exception e);
    }

    public interface OperationCallback {
        void onComplete(boolean success);
    }

    public interface BatchOperationCallback {
        void onComplete(int successCount, int failureCount);
    }

    public interface StatisticsCallback {
        void onSuccess(FolderStatistics statistics);
        void onError(Exception e);
    }

    /**
     * Cleanup
     */
    public void cleanup() {
        executorService.shutdown();
        clearCache();
        Log.d(TAG, "FolderRepository cleaned up");
    }
}

