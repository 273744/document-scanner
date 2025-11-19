package com.srikanth.docscanner;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.LruCache;

import androidx.annotation.NonNull;

import com.srikanth.docscanner.database.Document;
import com.srikanth.docscanner.database.DocumentRepository;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * SearchableDatabase - Full-text search capabilities using SQLite FTS5
 *
 * Features:
 * 1. Full-text search (FTS5) virtual table creation
 * 2. Index document content, filenames, and metadata
 * 3. Search ranking and relevance scoring
 * 4. Incremental search index updates
 * 5. Search query optimization and caching
 * 6. Stemming and fuzzy search support
 * 7. Multi-language search tokenization
 */
public class SearchableDatabase extends SQLiteOpenHelper {

    private static final String TAG = "SearchableDatabase";

    // Database info
    private static final String DATABASE_NAME = "searchable_documents.db";
    private static final int DATABASE_VERSION = 1;

    // FTS5 table names
    private static final String FTS_DOCUMENTS_TABLE = "fts_documents";
    private static final String FTS_METADATA_TABLE = "documents_metadata";

    // Singleton instance
    private static SearchableDatabase instance;

    // Context
    private final Context context;

    // Threading
    private final ExecutorService executorService;
    private final Handler mainHandler;

    // Query cache
    private final LruCache<String, List<SearchResult>> queryCache;
    private static final int CACHE_SIZE = 100; // Cache 100 recent queries

    // Search statistics
    private final Map<String, SearchStats> searchStatsMap;

    // Stemming cache
    private final Map<String, String> stemmingCache;

    // Stop words for different languages
    private final Map<String, String[]> stopWords;

    /**
     * Private constructor (Singleton)
     */
    private SearchableDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context.getApplicationContext();
        this.executorService = Executors.newFixedThreadPool(2);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.queryCache = new LruCache<>(CACHE_SIZE);
        this.searchStatsMap = new HashMap<>();
        this.stemmingCache = new HashMap<>();
        this.stopWords = initializeStopWords();

        Log.d(TAG, "SearchableDatabase initialized");
    }

    /**
     * Get singleton instance
     */
    public static synchronized SearchableDatabase getInstance(Context context) {
        if (instance == null) {
            instance = new SearchableDatabase(context);
        }
        return instance;
    }

    // ================================
    // 1. FTS5 Virtual Table Creation
    // ================================

    @Override
    public void onCreate(SQLiteDatabase db) {
        createFTSTables(db);
        createMetadataTables(db);
        createIndexes(db);
        Log.d(TAG, "FTS5 tables created successfully");
    }

    /**
     * Create FTS5 virtual tables
     */
    private void createFTSTables(SQLiteDatabase db) {
        // Main FTS5 table for document content
        String createFtsTable = "CREATE VIRTUAL TABLE IF NOT EXISTS " + FTS_DOCUMENTS_TABLE +
            " USING fts5(" +
            "  document_id UNINDEXED," +           // Document ID (not searchable)
            "  document_name," +                    // Searchable document name
            "  file_name," +                        // Searchable file name
            "  ocr_text," +                         // Full OCR text content
            "  description," +                      // Document description
            "  tags," +                             // Comma-separated tags
            "  tokenize='porter unicode61 remove_diacritics 2'" + // Porter stemming + unicode
            ");";

        db.execSQL(createFtsTable);

        Log.d(TAG, "FTS5 virtual table created with porter stemming");
    }

    /**
     * Create metadata tables
     */
    private void createMetadataTables(SQLiteDatabase db) {
        // Metadata table for ranking and filtering
        String createMetadataTable = "CREATE TABLE IF NOT EXISTS " + FTS_METADATA_TABLE + " (" +
            "  document_id INTEGER PRIMARY KEY," +
            "  file_type TEXT," +
            "  file_size INTEGER," +
            "  page_count INTEGER," +
            "  created_at INTEGER," +
            "  modified_at INTEGER," +
            "  folder_id INTEGER," +
            "  favorite INTEGER DEFAULT 0," +
            "  word_count INTEGER," +
            "  language TEXT," +
            "  last_indexed INTEGER," +
            "  index_version INTEGER DEFAULT 1" +
            ");";

        db.execSQL(createMetadataTable);

        // Search statistics table
        String createStatsTable = "CREATE TABLE IF NOT EXISTS search_statistics (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  query TEXT NOT NULL," +
            "  result_count INTEGER," +
            "  search_time_ms INTEGER," +
            "  timestamp INTEGER," +
            "  clicked_document_id INTEGER" +
            ");";

        db.execSQL(createStatsTable);

        // Popular searches view
        String createPopularView = "CREATE VIEW IF NOT EXISTS popular_searches AS " +
            "SELECT query, COUNT(*) as search_count, AVG(result_count) as avg_results " +
            "FROM search_statistics " +
            "GROUP BY query " +
            "ORDER BY search_count DESC " +
            "LIMIT 50;";

        db.execSQL(createPopularView);
    }

    /**
     * Create indexes for performance
     */
    private void createIndexes(SQLiteDatabase db) {
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_metadata_created ON " +
            FTS_METADATA_TABLE + "(created_at DESC);");

        db.execSQL("CREATE INDEX IF NOT EXISTS idx_metadata_modified ON " +
            FTS_METADATA_TABLE + "(modified_at DESC);");

        db.execSQL("CREATE INDEX IF NOT EXISTS idx_metadata_folder ON " +
            FTS_METADATA_TABLE + "(folder_id);");

        db.execSQL("CREATE INDEX IF NOT EXISTS idx_metadata_favorite ON " +
            FTS_METADATA_TABLE + "(favorite);");

        db.execSQL("CREATE INDEX IF NOT EXISTS idx_stats_query ON " +
            "search_statistics(query);");

        db.execSQL("CREATE INDEX IF NOT EXISTS idx_stats_timestamp ON " +
            "search_statistics(timestamp DESC);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);

        // Drop and recreate tables
        db.execSQL("DROP TABLE IF EXISTS " + FTS_DOCUMENTS_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + FTS_METADATA_TABLE);
        db.execSQL("DROP TABLE IF EXISTS search_statistics");
        db.execSQL("DROP VIEW IF EXISTS popular_searches");

        onCreate(db);
    }

    // ================================
    // 2. Index Document Content
    // ================================

    /**
     * Index a document for full-text search
     */
    public void indexDocument(Document document, IndexCallback callback) {
        executorService.execute(() -> {
            SQLiteDatabase db = getWritableDatabase();

            try {
                db.beginTransaction();

                // Delete existing entries
                deleteDocumentFromIndex(db, document.getDocumentId());

                // Insert into FTS5 table
                String insertFts = "INSERT INTO " + FTS_DOCUMENTS_TABLE +
                    " (document_id, document_name, file_name, ocr_text, description, tags) " +
                    "VALUES (?, ?, ?, ?, ?, ?);";

                db.execSQL(insertFts, new Object[] {
                    document.getDocumentId(),
                    document.getDocumentName(),
                    extractFileName(document.getDocumentName()),
                    document.getOcrText() != null ? document.getOcrText() : "",
                    document.getDescription() != null ? document.getDescription() : "",
                    "" // Tags would come from DocumentTag junction table
                });

                // Insert metadata
                String insertMetadata = "INSERT OR REPLACE INTO " + FTS_METADATA_TABLE +
                    " (document_id, file_type, file_size, page_count, created_at, " +
                    "  modified_at, folder_id, favorite, word_count, language, last_indexed) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

                String language = detectLanguage(document.getOcrText());
                int wordCount = countWords(document.getOcrText());

                db.execSQL(insertMetadata, new Object[] {
                    document.getDocumentId(),
                    document.getFileType(),
                    document.getFileSize(),
                    document.getPageCount(),
                    document.getCreatedAt(),
                    document.getModifiedAt(),
                    document.getFolderId(),
                    document.isFavorite() ? 1 : 0,
                    wordCount,
                    language,
                    System.currentTimeMillis()
                });

                db.setTransactionSuccessful();

                Log.d(TAG, "Document indexed: " + document.getDocumentId() +
                    " (" + wordCount + " words, " + language + ")");

                if (callback != null) {
                    mainHandler.post(() -> callback.onIndexed(document.getDocumentId(), true));
                }

            } catch (Exception e) {
                Log.e(TAG, "Error indexing document: " + document.getDocumentId(), e);
                if (callback != null) {
                    mainHandler.post(() -> callback.onIndexed(document.getDocumentId(), false));
                }
            } finally {
                db.endTransaction();
            }
        });
    }

    /**
     * Index multiple documents (batch operation)
     */
    public void indexDocuments(List<Document> documents, BatchIndexCallback callback) {
        executorService.execute(() -> {
            SQLiteDatabase db = getWritableDatabase();
            int successCount = 0;
            int failureCount = 0;

            try {
                db.beginTransaction();

                for (Document document : documents) {
                    try {
                        deleteDocumentFromIndex(db, document.getDocumentId());

                        // Index document (simplified for batch)
                        String insertFts = "INSERT INTO " + FTS_DOCUMENTS_TABLE +
                            " (document_id, document_name, file_name, ocr_text, description, tags) " +
                            "VALUES (?, ?, ?, ?, ?, ?);";

                        db.execSQL(insertFts, new Object[] {
                            document.getDocumentId(),
                            document.getDocumentName(),
                            extractFileName(document.getDocumentName()),
                            document.getOcrText() != null ? document.getOcrText() : "",
                            document.getDescription() != null ? document.getDescription() : "",
                            ""
                        });

                        successCount++;

                    } catch (Exception e) {
                        Log.e(TAG, "Error indexing document in batch: " + document.getDocumentId(), e);
                        failureCount++;
                    }
                }

                db.setTransactionSuccessful();

                Log.d(TAG, "Batch indexing complete: " + successCount + " success, " +
                    failureCount + " failed");

            } finally {
                db.endTransaction();
            }

            if (callback != null) {
                int finalSuccess = successCount;
                int finalFailure = failureCount;
                mainHandler.post(() -> callback.onBatchIndexed(finalSuccess, finalFailure));
            }
        });
    }

    /**
     * Delete document from index
     */
    private void deleteDocumentFromIndex(SQLiteDatabase db, long documentId) {
        db.execSQL("DELETE FROM " + FTS_DOCUMENTS_TABLE + " WHERE document_id = ?;",
            new Object[] { documentId });
        db.execSQL("DELETE FROM " + FTS_METADATA_TABLE + " WHERE document_id = ?;",
            new Object[] { documentId });
    }

    /**
     * Remove document from index
     */
    public void removeDocumentFromIndex(long documentId, IndexCallback callback) {
        executorService.execute(() -> {
            SQLiteDatabase db = getWritableDatabase();

            try {
                db.beginTransaction();
                deleteDocumentFromIndex(db, documentId);
                db.setTransactionSuccessful();

                Log.d(TAG, "Document removed from index: " + documentId);

                if (callback != null) {
                    mainHandler.post(() -> callback.onIndexed(documentId, true));
                }

            } catch (Exception e) {
                Log.e(TAG, "Error removing document from index: " + documentId, e);
                if (callback != null) {
                    mainHandler.post(() -> callback.onIndexed(documentId, false));
                }
            } finally {
                db.endTransaction();
            }
        });
    }

    // ================================
    // 3. Search with Ranking and Relevance
    // ================================

    /**
     * Search documents with full-text search
     */
    public void search(String query, SearchOptions options, SearchCallback callback) {
        if (query == null || query.trim().isEmpty()) {
            mainHandler.post(() -> callback.onSearchComplete(new ArrayList<>(), 0));
            return;
        }

        // Check cache first
        String cacheKey = generateCacheKey(query, options);
        List<SearchResult> cachedResults = queryCache.get(cacheKey);
        if (cachedResults != null) {
            Log.d(TAG, "Returning cached results for: " + query);
            mainHandler.post(() -> callback.onSearchComplete(cachedResults, cachedResults.size()));
            return;
        }

        executorService.execute(() -> {
            long startTime = System.currentTimeMillis();
            List<SearchResult> results = new ArrayList<>();
            SQLiteDatabase db = getReadableDatabase();
            Cursor cursor = null;

            try {
                // Build optimized query
                String optimizedQuery = optimizeQuery(query, options);

                // Perform FTS5 search with BM25 ranking
                String sql = buildSearchQuery(optimizedQuery, options);

                cursor = db.rawQuery(sql, null);

                while (cursor.moveToNext()) {
                    SearchResult result = new SearchResult();
                    result.documentId = cursor.getLong(0);
                    result.documentName = cursor.getString(1);
                    result.fileName = cursor.getString(2);
                    result.snippet = cursor.getString(3);
                    result.rank = cursor.getDouble(4);

                    // Load metadata
                    loadMetadata(db, result);

                    // Calculate final relevance score
                    result.relevanceScore = calculateRelevanceScore(result, query, options);

                    results.add(result);
                }

                // Sort by relevance score
                Collections.sort(results, (r1, r2) ->
                    Double.compare(r2.relevanceScore, r1.relevanceScore));

                // Apply limit
                if (options.limit > 0 && results.size() > options.limit) {
                    results = results.subList(0, options.limit);
                }

                long searchTime = System.currentTimeMillis() - startTime;

                // Cache results
                queryCache.put(cacheKey, results);

                // Record search statistics
                recordSearchStatistics(query, results.size(), searchTime);

                Log.d(TAG, "Search completed: '" + query + "' - " + results.size() +
                    " results in " + searchTime + "ms");

                List<SearchResult> finalResults = results;
                mainHandler.post(() -> callback.onSearchComplete(finalResults, finalResults.size()));

            } catch (Exception e) {
                Log.e(TAG, "Search error for query: " + query, e);
                mainHandler.post(() -> callback.onSearchError(e));
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        });
    }

    /**
     * Build search query with ranking
     */
    private String buildSearchQuery(String query, SearchOptions options) {
        StringBuilder sql = new StringBuilder();

        sql.append("SELECT ");
        sql.append("  fts.document_id, ");
        sql.append("  fts.document_name, ");
        sql.append("  fts.file_name, ");

        // Snippet generation (highlight matching text)
        sql.append("  snippet(").append(FTS_DOCUMENTS_TABLE).append(", 2, '<b>', '</b>', '...', 64) as snippet, ");

        // BM25 ranking score
        sql.append("  bm25(").append(FTS_DOCUMENTS_TABLE).append(") as rank ");

        sql.append("FROM ").append(FTS_DOCUMENTS_TABLE).append(" fts ");
        sql.append("LEFT JOIN ").append(FTS_METADATA_TABLE).append(" meta ");
        sql.append("  ON fts.document_id = meta.document_id ");

        sql.append("WHERE ").append(FTS_DOCUMENTS_TABLE).append(" MATCH '");
        sql.append(escapeFtsQuery(query));
        sql.append("' ");

        // Apply filters
        if (options.fileType != null) {
            sql.append("AND meta.file_type = '").append(options.fileType).append("' ");
        }

        if (options.folderId != null) {
            sql.append("AND meta.folder_id = ").append(options.folderId).append(" ");
        }

        if (options.favoritesOnly) {
            sql.append("AND meta.favorite = 1 ");
        }

        if (options.minDate > 0) {
            sql.append("AND meta.created_at >= ").append(options.minDate).append(" ");
        }

        if (options.maxDate > 0) {
            sql.append("AND meta.created_at <= ").append(options.maxDate).append(" ");
        }

        // Order by rank
        sql.append("ORDER BY rank ");

        // Apply limit
        if (options.limit > 0) {
            sql.append("LIMIT ").append(options.limit);
        }

        return sql.toString();
    }

    /**
     * Calculate relevance score
     */
    private double calculateRelevanceScore(SearchResult result, String query, SearchOptions options) {
        double score = 0.0;

        // BM25 score (base ranking from FTS5)
        score += result.rank * 1.0;

        // Boost for exact name match
        if (result.documentName.toLowerCase().contains(query.toLowerCase())) {
            score += 10.0;
        }

        // Boost for file name match
        if (result.fileName.toLowerCase().contains(query.toLowerCase())) {
            score += 5.0;
        }

        // Boost for favorites
        if (result.isFavorite) {
            score += 3.0;
        }

        // Boost for recent documents
        long daysSinceCreation = (System.currentTimeMillis() - result.createdAt) / (1000 * 60 * 60 * 24);
        if (daysSinceCreation < 7) {
            score += 2.0;
        }

        // Boost for larger documents (more content)
        if (result.wordCount > 100) {
            score += 1.0;
        }

        // Normalize score
        score = Math.max(0, Math.min(100, score));

        return score;
    }

    /**
     * Load metadata for search result
     */
    private void loadMetadata(SQLiteDatabase db, SearchResult result) {
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                "SELECT file_type, file_size, page_count, created_at, modified_at, " +
                "       folder_id, favorite, word_count, language " +
                "FROM " + FTS_METADATA_TABLE + " WHERE document_id = ?",
                new String[] { String.valueOf(result.documentId) }
            );

            if (cursor.moveToFirst()) {
                result.fileType = cursor.getString(0);
                result.fileSize = cursor.getLong(1);
                result.pageCount = cursor.getInt(2);
                result.createdAt = cursor.getLong(3);
                result.modifiedAt = cursor.getLong(4);
                result.folderId = cursor.isNull(5) ? null : cursor.getLong(5);
                result.isFavorite = cursor.getInt(6) == 1;
                result.wordCount = cursor.getInt(7);
                result.language = cursor.getString(8);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    // ================================
    // 4. Incremental Index Updates
    // ================================

    /**
     * Update document in search index
     */
    public void updateDocumentIndex(Document document, IndexCallback callback) {
        executorService.execute(() -> {
            SQLiteDatabase db = getWritableDatabase();

            try {
                db.beginTransaction();

                // Check if document exists in index
                Cursor cursor = db.rawQuery(
                    "SELECT last_indexed FROM " + FTS_METADATA_TABLE + " WHERE document_id = ?",
                    new String[] { String.valueOf(document.getDocumentId()) }
                );

                boolean exists = cursor.moveToFirst();
                cursor.close();

                if (exists) {
                    // Update existing entry
                    deleteDocumentFromIndex(db, document.getDocumentId());
                }

                // Re-index document
                String insertFts = "INSERT INTO " + FTS_DOCUMENTS_TABLE +
                    " (document_id, document_name, file_name, ocr_text, description, tags) " +
                    "VALUES (?, ?, ?, ?, ?, ?);";

                db.execSQL(insertFts, new Object[] {
                    document.getDocumentId(),
                    document.getDocumentName(),
                    extractFileName(document.getDocumentName()),
                    document.getOcrText() != null ? document.getOcrText() : "",
                    document.getDescription() != null ? document.getDescription() : "",
                    ""
                });

                // Update metadata
                String updateMetadata = "INSERT OR REPLACE INTO " + FTS_METADATA_TABLE +
                    " (document_id, file_type, file_size, page_count, created_at, " +
                    "  modified_at, folder_id, favorite, word_count, language, last_indexed) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

                db.execSQL(updateMetadata, new Object[] {
                    document.getDocumentId(),
                    document.getFileType(),
                    document.getFileSize(),
                    document.getPageCount(),
                    document.getCreatedAt(),
                    document.getModifiedAt(),
                    document.getFolderId(),
                    document.isFavorite() ? 1 : 0,
                    countWords(document.getOcrText()),
                    detectLanguage(document.getOcrText()),
                    System.currentTimeMillis()
                });

                db.setTransactionSuccessful();

                // Clear cache since index changed
                queryCache.evictAll();

                Log.d(TAG, "Document index updated: " + document.getDocumentId());

                if (callback != null) {
                    mainHandler.post(() -> callback.onIndexed(document.getDocumentId(), true));
                }

            } catch (Exception e) {
                Log.e(TAG, "Error updating document index: " + document.getDocumentId(), e);
                if (callback != null) {
                    mainHandler.post(() -> callback.onIndexed(document.getDocumentId(), false));
                }
            } finally {
                db.endTransaction();
            }
        });
    }

    /**
     * Rebuild entire search index
     */
    public void rebuildIndex(RebuildCallback callback) {
        executorService.execute(() -> {
            SQLiteDatabase db = getWritableDatabase();

            try {
                db.beginTransaction();

                // Clear existing index
                db.execSQL("DELETE FROM " + FTS_DOCUMENTS_TABLE);
                db.execSQL("DELETE FROM " + FTS_METADATA_TABLE);

                // Optimize FTS5 table
                db.execSQL("INSERT INTO " + FTS_DOCUMENTS_TABLE +
                    "(" + FTS_DOCUMENTS_TABLE + ") VALUES('optimize');");

                db.setTransactionSuccessful();

                // Clear cache
                queryCache.evictAll();

                Log.d(TAG, "Search index cleared and optimized");

                if (callback != null) {
                    mainHandler.post(() -> callback.onRebuildComplete(true));
                }

            } catch (Exception e) {
                Log.e(TAG, "Error rebuilding search index", e);
                if (callback != null) {
                    mainHandler.post(() -> callback.onRebuildComplete(false));
                }
            } finally {
                db.endTransaction();
            }
        });
    }

    // ================================
    // 5. Query Optimization and Caching
    // ================================

    /**
     * Optimize search query
     */
    private String optimizeQuery(String query, SearchOptions options) {
        // Remove extra whitespace
        query = query.trim().replaceAll("\\s+", " ");

        // Remove stop words if enabled
        if (options.removeStopWords) {
            query = removeStopWords(query, options.language);
        }

        // Apply stemming if enabled
        if (options.enableStemming) {
            query = applyStemming(query);
        }

        // Handle phrase search
        if (query.contains("\"")) {
            return query; // Keep phrases as-is
        }

        // Add prefix matching for partial words
        if (options.enablePrefixSearch) {
            String[] terms = query.split("\\s+");
            StringBuilder optimized = new StringBuilder();

            for (int i = 0; i < terms.length; i++) {
                if (i > 0) optimized.append(" ");

                String term = terms[i];
                if (term.length() >= 3) {
                    optimized.append(term).append("*"); // Prefix search
                } else {
                    optimized.append(term);
                }
            }

            return optimized.toString();
        }

        return query;
    }

    /**
     * Generate cache key
     */
    private String generateCacheKey(String query, SearchOptions options) {
        return query + "|" +
               options.fileType + "|" +
               options.folderId + "|" +
               options.favoritesOnly + "|" +
               options.limit;
    }

    /**
     * Clear query cache
     */
    public void clearCache() {
        queryCache.evictAll();
        Log.d(TAG, "Query cache cleared");
    }

    /**
     * Get cache statistics
     */
    public CacheStats getCacheStats() {
        CacheStats stats = new CacheStats();
        stats.size = queryCache.size();
        stats.maxSize = queryCache.maxSize();
        stats.hitCount = queryCache.hitCount();
        stats.missCount = queryCache.missCount();
        stats.hitRate = stats.hitCount / (float) (stats.hitCount + stats.missCount);
        return stats;
    }

    // ================================
    // 6. Stemming and Fuzzy Search
    // ================================

    /**
     * Apply stemming to query terms
     */
    private String applyStemming(String query) {
        // Check cache first
        String cached = stemmingCache.get(query);
        if (cached != null) {
            return cached;
        }

        String[] terms = query.toLowerCase().split("\\s+");
        StringBuilder stemmed = new StringBuilder();

        for (int i = 0; i < terms.length; i++) {
            if (i > 0) stemmed.append(" ");

            String term = terms[i];
            String stemmedTerm = porterStem(term);
            stemmed.append(stemmedTerm);
        }

        String result = stemmed.toString();

        // Cache result
        if (stemmingCache.size() < 1000) {
            stemmingCache.put(query, result);
        }

        return result;
    }

    /**
     * Simple Porter Stemmer implementation
     */
    private String porterStem(String word) {
        if (word.length() < 3) {
            return word;
        }

        // Step 1: Remove common suffixes
        if (word.endsWith("ies") && word.length() > 3) {
            return word.substring(0, word.length() - 3) + "y";
        }
        if (word.endsWith("es") && word.length() > 2) {
            return word.substring(0, word.length() - 2);
        }
        if (word.endsWith("s") && word.length() > 2 && !word.endsWith("ss")) {
            return word.substring(0, word.length() - 1);
        }

        // Step 2: Handle -ed, -ing
        if (word.endsWith("ing") && word.length() > 3) {
            return word.substring(0, word.length() - 3);
        }
        if (word.endsWith("ed") && word.length() > 2) {
            return word.substring(0, word.length() - 2);
        }

        return word;
    }

    /**
     * Fuzzy search using edit distance
     */
    public void fuzzySearch(String query, int maxDistance, SearchOptions options, SearchCallback callback) {
        executorService.execute(() -> {
            List<SearchResult> results = new ArrayList<>();
            SQLiteDatabase db = getReadableDatabase();
            Cursor cursor = null;

            try {
                // Get all indexed terms
                cursor = db.rawQuery(
                    "SELECT DISTINCT document_name FROM " + FTS_DOCUMENTS_TABLE,
                    null
                );

                List<String> candidates = new ArrayList<>();
                while (cursor.moveToNext()) {
                    String name = cursor.getString(0);
                    if (calculateEditDistance(query.toLowerCase(), name.toLowerCase()) <= maxDistance) {
                        candidates.add(name);
                    }
                }

                // Search for matching documents
                if (!candidates.isEmpty()) {
                    StringBuilder fuzzyQuery = new StringBuilder();
                    for (int i = 0; i < candidates.size(); i++) {
                        if (i > 0) fuzzyQuery.append(" OR ");
                        fuzzyQuery.append("\"").append(escapeFtsQuery(candidates.get(i))).append("\"");
                    }

                    String sql = buildSearchQuery(fuzzyQuery.toString(), options);
                    cursor.close();
                    cursor = db.rawQuery(sql, null);

                    while (cursor.moveToNext()) {
                        SearchResult result = new SearchResult();
                        result.documentId = cursor.getLong(0);
                        result.documentName = cursor.getString(1);
                        result.fileName = cursor.getString(2);
                        result.snippet = cursor.getString(3);
                        result.rank = cursor.getDouble(4);

                        loadMetadata(db, result);
                        result.relevanceScore = calculateRelevanceScore(result, query, options);

                        results.add(result);
                    }
                }

                Collections.sort(results, (r1, r2) ->
                    Double.compare(r2.relevanceScore, r1.relevanceScore));

                Log.d(TAG, "Fuzzy search completed: " + results.size() + " results");

                mainHandler.post(() -> callback.onSearchComplete(results, results.size()));

            } catch (Exception e) {
                Log.e(TAG, "Fuzzy search error", e);
                mainHandler.post(() -> callback.onSearchError(e));
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        });
    }

    /**
     * Calculate Levenshtein distance (edit distance)
     */
    private int calculateEditDistance(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();

        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j - 1],
                                   Math.min(dp[i - 1][j], dp[i][j - 1]));
                }
            }
        }

        return dp[len1][len2];
    }

    // ================================
    // 7. Multi-Language Support
    // ================================

    /**
     * Initialize stop words for different languages
     */
    private Map<String, String[]> initializeStopWords() {
        Map<String, String[]> stopWords = new HashMap<>();

        // English stop words
        stopWords.put("en", new String[] {
            "a", "an", "and", "are", "as", "at", "be", "by", "for", "from",
            "has", "he", "in", "is", "it", "its", "of", "on", "that", "the",
            "to", "was", "will", "with"
        });

        // Spanish stop words
        stopWords.put("es", new String[] {
            "el", "la", "de", "que", "y", "a", "en", "un", "ser", "se",
            "no", "haber", "por", "con", "su", "para", "como", "estar", "tener"
        });

        // French stop words
        stopWords.put("fr", new String[] {
            "le", "de", "un", "être", "et", "à", "il", "avoir", "ne", "je",
            "son", "que", "se", "qui", "ce", "dans", "en", "du", "elle", "au"
        });

        // German stop words
        stopWords.put("de", new String[] {
            "der", "die", "das", "und", "in", "den", "von", "zu", "das", "mit",
            "sich", "des", "auf", "für", "ist", "im", "dem", "nicht", "ein", "eine"
        });

        return stopWords;
    }

    /**
     * Remove stop words from query
     */
    private String removeStopWords(String query, String language) {
        String[] stops = stopWords.get(language);
        if (stops == null) {
            stops = stopWords.get("en"); // Default to English
        }

        String[] terms = query.toLowerCase().split("\\s+");
        StringBuilder filtered = new StringBuilder();

        for (String term : terms) {
            boolean isStopWord = false;
            for (String stop : stops) {
                if (term.equals(stop)) {
                    isStopWord = true;
                    break;
                }
            }

            if (!isStopWord) {
                if (filtered.length() > 0) filtered.append(" ");
                filtered.append(term);
            }
        }

        return filtered.toString();
    }

    /**
     * Detect language from text
     */
    private String detectLanguage(String text) {
        if (text == null || text.isEmpty()) {
            return "en"; // Default to English
        }

        // Simple language detection based on common words
        String lower = text.toLowerCase();

        if (lower.contains(" el ") || lower.contains(" la ") || lower.contains(" que ")) {
            return "es"; // Spanish
        }
        if (lower.contains(" le ") || lower.contains(" de ") || lower.contains(" et ")) {
            return "fr"; // French
        }
        if (lower.contains(" der ") || lower.contains(" die ") || lower.contains(" das ")) {
            return "de"; // German
        }

        return "en"; // Default to English
    }

    /**
     * Normalize text for multi-language search
     */
    private String normalizeText(String text) {
        if (text == null) return "";

        // Remove diacritics
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        normalized = pattern.matcher(normalized).replaceAll("");

        return normalized.toLowerCase();
    }

    // ================================
    // Search Statistics and Analytics
    // ================================

    /**
     * Record search statistics
     */
    private void recordSearchStatistics(String query, int resultCount, long searchTimeMs) {
        SQLiteDatabase db = getWritableDatabase();

        try {
            String sql = "INSERT INTO search_statistics " +
                "(query, result_count, search_time_ms, timestamp) " +
                "VALUES (?, ?, ?, ?);";

            db.execSQL(sql, new Object[] {
                query,
                resultCount,
                searchTimeMs,
                System.currentTimeMillis()
            });

        } catch (Exception e) {
            Log.e(TAG, "Error recording search statistics", e);
        }
    }

    /**
     * Record clicked document (for learning)
     */
    public void recordClickedDocument(String query, long documentId) {
        executorService.execute(() -> {
            SQLiteDatabase db = getWritableDatabase();

            try {
                String sql = "UPDATE search_statistics " +
                    "SET clicked_document_id = ? " +
                    "WHERE query = ? AND clicked_document_id IS NULL " +
                    "ORDER BY timestamp DESC LIMIT 1;";

                db.execSQL(sql, new Object[] { documentId, query });

                Log.d(TAG, "Recorded click: " + query + " -> " + documentId);

            } catch (Exception e) {
                Log.e(TAG, "Error recording clicked document", e);
            }
        });
    }

    /**
     * Get popular searches
     */
    public void getPopularSearches(int limit, PopularSearchCallback callback) {
        executorService.execute(() -> {
            List<PopularSearch> searches = new ArrayList<>();
            SQLiteDatabase db = getReadableDatabase();
            Cursor cursor = null;

            try {
                cursor = db.rawQuery(
                    "SELECT query, search_count, avg_results " +
                    "FROM popular_searches LIMIT ?",
                    new String[] { String.valueOf(limit) }
                );

                while (cursor.moveToNext()) {
                    PopularSearch search = new PopularSearch();
                    search.query = cursor.getString(0);
                    search.searchCount = cursor.getInt(1);
                    search.avgResults = cursor.getInt(2);
                    searches.add(search);
                }

                mainHandler.post(() -> callback.onPopularSearchesLoaded(searches));

            } catch (Exception e) {
                Log.e(TAG, "Error loading popular searches", e);
                mainHandler.post(() -> callback.onError(e));
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        });
    }

    /**
     * Get search suggestions based on history
     */
    public void getSearchSuggestions(String prefix, int limit, SuggestionCallback callback) {
        executorService.execute(() -> {
            List<String> suggestions = new ArrayList<>();
            SQLiteDatabase db = getReadableDatabase();
            Cursor cursor = null;

            try {
                cursor = db.rawQuery(
                    "SELECT DISTINCT query FROM search_statistics " +
                    "WHERE query LIKE ? " +
                    "ORDER BY timestamp DESC LIMIT ?",
                    new String[] { prefix + "%", String.valueOf(limit) }
                );

                while (cursor.moveToNext()) {
                    suggestions.add(cursor.getString(0));
                }

                mainHandler.post(() -> callback.onSuggestionsLoaded(suggestions));

            } catch (Exception e) {
                Log.e(TAG, "Error loading search suggestions", e);
                mainHandler.post(() -> callback.onError(e));
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        });
    }

    // ================================
    // Helper Methods
    // ================================

    /**
     * Escape FTS query special characters
     */
    private String escapeFtsQuery(String query) {
        return query.replace("\"", "\"\"");
    }

    /**
     * Extract file name from full path/name
     */
    private String extractFileName(String fullName) {
        if (fullName == null) return "";

        int lastSlash = fullName.lastIndexOf('/');
        if (lastSlash >= 0) {
            return fullName.substring(lastSlash + 1);
        }
        return fullName;
    }

    /**
     * Count words in text
     */
    private int countWords(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.split("\\s+").length;
    }

    /**
     * Cleanup resources
     */
    public void cleanup() {
        executorService.shutdown();
        queryCache.evictAll();
        close();
        Log.d(TAG, "SearchableDatabase cleaned up");
    }

    // ================================
    // Data Classes
    // ================================

    /**
     * Search Options
     */
    public static class SearchOptions {
        public String fileType = null;
        public Long folderId = null;
        public boolean favoritesOnly = false;
        public long minDate = 0;
        public long maxDate = 0;
        public int limit = 50;
        public boolean removeStopWords = true;
        public boolean enableStemming = true;
        public boolean enablePrefixSearch = true;
        public String language = "en";
    }

    /**
     * Search Result
     */
    public static class SearchResult {
        public long documentId;
        public String documentName;
        public String fileName;
        public String snippet;
        public double rank;
        public double relevanceScore;
        public String fileType;
        public long fileSize;
        public int pageCount;
        public long createdAt;
        public long modifiedAt;
        public Long folderId;
        public boolean isFavorite;
        public int wordCount;
        public String language;
    }

    /**
     * Cache Statistics
     */
    public static class CacheStats {
        public int size;
        public int maxSize;
        public int hitCount;
        public int missCount;
        public float hitRate;
    }

    /**
     * Search Statistics
     */
    private static class SearchStats {
        int totalSearches;
        int avgResults;
        long avgSearchTime;
    }

    /**
     * Popular Search
     */
    public static class PopularSearch {
        public String query;
        public int searchCount;
        public int avgResults;
    }

    // ================================
    // Callbacks
    // ================================

    public interface SearchCallback {
        void onSearchComplete(List<SearchResult> results, int totalCount);
        void onSearchError(Exception e);
    }

    public interface IndexCallback {
        void onIndexed(long documentId, boolean success);
    }

    public interface BatchIndexCallback {
        void onBatchIndexed(int successCount, int failureCount);
    }

    public interface RebuildCallback {
        void onRebuildComplete(boolean success);
    }

    public interface PopularSearchCallback {
        void onPopularSearchesLoaded(List<PopularSearch> searches);
        void onError(Exception e);
    }

    public interface SuggestionCallback {
        void onSuggestionsLoaded(List<String> suggestions);
        void onError(Exception e);
    }
}


