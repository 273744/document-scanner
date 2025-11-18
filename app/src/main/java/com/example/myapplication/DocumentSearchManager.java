package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.example.myapplication.database.Document;
import com.example.myapplication.database.DocumentRepository;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * DocumentSearchManager - Comprehensive search functionality
 *
 * Features:
 * 1. Real-time search with debounced input
 * 2. Search across document content, filenames, tags
 * 3. Advanced search with filters (date, type, folder)
 * 4. Search suggestions and auto-complete
 * 5. Search history management
 * 6. Saved searches functionality
 * 7. Search result highlighting and ranking
 * 8. Search analytics and performance optimization
 */
public class DocumentSearchManager {

    private static final String TAG = "DocumentSearchManager";

    // Singleton instance
    private static DocumentSearchManager instance;

    // Context
    private final Context context;

    // Dependencies
    private final SearchableDatabase searchableDatabase;
    private final DocumentRepository documentRepository;

    // Threading
    private final ExecutorService executorService;
    private final Handler mainHandler;
    private final Handler debounceHandler;

    // Debouncing
    private static final long DEBOUNCE_DELAY_MS = 300; // 300ms delay
    private Runnable pendingSearchRunnable;

    // SharedPreferences
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "SearchManagerPrefs";
    private static final String KEY_SEARCH_HISTORY = "search_history";
    private static final String KEY_SAVED_SEARCHES = "saved_searches";
    private static final String KEY_SEARCH_ANALYTICS = "search_analytics";

    // Search history
    private static final int MAX_HISTORY_SIZE = 50;
    private List<SearchHistoryItem> searchHistory = new ArrayList<>();

    // Saved searches
    private List<SavedSearch> savedSearches = new ArrayList<>();

    // Search analytics
    private SearchAnalytics analytics = new SearchAnalytics();

    // Active search
    private String currentQuery = "";
    private SearchFilter currentFilter = new SearchFilter();
    private List<SearchableDatabase.SearchResult> currentResults = new ArrayList<>();

    // Callbacks
    private Set<SearchListener> searchListeners = new HashSet<>();

    /**
     * Private constructor (Singleton)
     */
    private DocumentSearchManager(Context context) {
        this.context = context.getApplicationContext();
        this.searchableDatabase = SearchableDatabase.getInstance(context);
        this.documentRepository = DocumentRepository.getInstance(context);
        this.executorService = Executors.newFixedThreadPool(2);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.debounceHandler = new Handler(Looper.getMainLooper());
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Load saved data
        loadSearchHistory();
        loadSavedSearches();
        loadAnalytics();

        Log.d(TAG, "DocumentSearchManager initialized");
    }

    /**
     * Get singleton instance
     */
    public static synchronized DocumentSearchManager getInstance(Context context) {
        if (instance == null) {
            instance = new DocumentSearchManager(context);
        }
        return instance;
    }

    // ================================
    // 1. Real-time Search with Debouncing
    // ================================

    /**
     * Perform real-time search with debouncing
     */
    public void searchRealTime(String query) {
        // Cancel pending search
        if (pendingSearchRunnable != null) {
            debounceHandler.removeCallbacks(pendingSearchRunnable);
        }

        // If query is empty, clear results immediately
        if (query == null || query.trim().isEmpty()) {
            clearResults();
            return;
        }

        // Create new debounced search
        pendingSearchRunnable = () -> {
            currentQuery = query;
            performSearch(query, currentFilter);
        };

        // Schedule debounced search
        debounceHandler.postDelayed(pendingSearchRunnable, DEBOUNCE_DELAY_MS);

        // Notify listeners that search is pending
        notifySearchPending(query);
    }

    /**
     * Perform immediate search (no debouncing)
     */
    public void searchImmediate(String query) {
        searchImmediate(query, currentFilter);
    }

    /**
     * Perform immediate search with filter
     */
    public void searchImmediate(String query, SearchFilter filter) {
        // Cancel any pending debounced search
        if (pendingSearchRunnable != null) {
            debounceHandler.removeCallbacks(pendingSearchRunnable);
            pendingSearchRunnable = null;
        }

        currentQuery = query;
        currentFilter = filter;
        performSearch(query, filter);
    }

    /**
     * Perform the actual search
     */
    private void performSearch(String query, SearchFilter filter) {
        long startTime = System.currentTimeMillis();

        // Notify listeners that search started
        notifySearchStarted(query);

        // Convert filter to SearchOptions
        SearchableDatabase.SearchOptions options = convertFilterToOptions(filter);

        // Perform search
        searchableDatabase.search(query, options, new SearchableDatabase.SearchCallback() {
            @Override
            public void onSearchComplete(List<SearchableDatabase.SearchResult> results, int totalCount) {
                long searchTime = System.currentTimeMillis() - startTime;

                currentResults = results;

                // Add to search history
                addToSearchHistory(query, results.size());

                // Update analytics
                analytics.recordSearch(query, results.size(), searchTime);
                saveAnalytics();

                // Notify listeners
                notifySearchComplete(results, searchTime);

                Log.d(TAG, "Search completed: '" + query + "' - " + results.size() +
                    " results in " + searchTime + "ms");
            }

            @Override
            public void onSearchError(Exception e) {
                Log.e(TAG, "Search error for query: " + query, e);
                notifySearchError(e);
            }
        });
    }

    /**
     * Clear search results
     */
    public void clearResults() {
        currentQuery = "";
        currentResults.clear();
        notifySearchCleared();
    }

    /**
     * Cancel pending search
     */
    public void cancelPendingSearch() {
        if (pendingSearchRunnable != null) {
            debounceHandler.removeCallbacks(pendingSearchRunnable);
            pendingSearchRunnable = null;
        }
    }

    // ================================
    // 2. Search Across Content
    // ================================

    /**
     * Search across all document fields
     */
    public void searchAllFields(String query, SearchAllCallback callback) {
        executorService.execute(() -> {
            Map<String, List<SearchableDatabase.SearchResult>> results = new HashMap<>();

            // Search in document names
            SearchFilter nameFilter = new SearchFilter();
            nameFilter.searchFields = SearchField.DOCUMENT_NAME;
            searchInField(query, nameFilter, new SearchCallback() {
                @Override
                public void onSearchComplete(List<SearchableDatabase.SearchResult> nameResults, long time) {
                    results.put("names", nameResults);
                    checkAllFieldsComplete(results, callback);
                }

                @Override
                public void onSearchError(Exception e) {
                    Log.e(TAG, "Error searching document names", e);
                }
            });

            // Search in file names
            SearchFilter fileFilter = new SearchFilter();
            fileFilter.searchFields = SearchField.FILE_NAME;
            searchInField(query, fileFilter, new SearchCallback() {
                @Override
                public void onSearchComplete(List<SearchableDatabase.SearchResult> fileResults, long time) {
                    results.put("files", fileResults);
                    checkAllFieldsComplete(results, callback);
                }

                @Override
                public void onSearchError(Exception e) {
                    Log.e(TAG, "Error searching file names", e);
                }
            });

            // Search in OCR content
            SearchFilter contentFilter = new SearchFilter();
            contentFilter.searchFields = SearchField.OCR_TEXT;
            searchInField(query, contentFilter, new SearchCallback() {
                @Override
                public void onSearchComplete(List<SearchableDatabase.SearchResult> contentResults, long time) {
                    results.put("content", contentResults);
                    checkAllFieldsComplete(results, callback);
                }

                @Override
                public void onSearchError(Exception e) {
                    Log.e(TAG, "Error searching OCR content", e);
                }
            });

            // Search in tags
            SearchFilter tagFilter = new SearchFilter();
            tagFilter.searchFields = SearchField.TAGS;
            searchInField(query, tagFilter, new SearchCallback() {
                @Override
                public void onSearchComplete(List<SearchableDatabase.SearchResult> tagResults, long time) {
                    results.put("tags", tagResults);
                    checkAllFieldsComplete(results, callback);
                }

                @Override
                public void onSearchError(Exception e) {
                    Log.e(TAG, "Error searching tags", e);
                }
            });
        });
    }

    /**
     * Search in specific field
     */
    private void searchInField(String query, SearchFilter filter, SearchCallback callback) {
        SearchableDatabase.SearchOptions options = convertFilterToOptions(filter);
        searchableDatabase.search(query, options, new SearchableDatabase.SearchCallback() {
            @Override
            public void onSearchComplete(List<SearchableDatabase.SearchResult> results, int totalCount) {
                mainHandler.post(() -> callback.onSearchComplete(results, 0));
            }

            @Override
            public void onSearchError(Exception e) {
                Log.e(TAG, "Field search error", e);
            }
        });
    }

    /**
     * Check if all field searches are complete
     */
    private void checkAllFieldsComplete(Map<String, List<SearchableDatabase.SearchResult>> results,
                                       SearchAllCallback callback) {
        if (results.size() == 4) { // All 4 fields searched
            mainHandler.post(() -> callback.onAllFieldsSearched(results));
        }
    }

    // ================================
    // 3. Advanced Search with Filters
    // ================================

    /**
     * Apply advanced filters to search
     */
    public void searchWithFilters(String query, SearchFilter filter, SearchCallback callback) {
        currentFilter = filter;

        SearchableDatabase.SearchOptions options = convertFilterToOptions(filter);

        searchableDatabase.search(query, options, new SearchableDatabase.SearchCallback() {
            @Override
            public void onSearchComplete(List<SearchableDatabase.SearchResult> results, int totalCount) {
                // Apply additional filters not supported by FTS5
                List<SearchableDatabase.SearchResult> filtered = applyAdditionalFilters(results, filter);

                mainHandler.post(() -> callback.onSearchComplete(filtered, 0));
            }

            @Override
            public void onSearchError(Exception e) {
                Log.e(TAG, "Filtered search error", e);
                mainHandler.post(() -> callback.onSearchError(e));
            }
        });
    }

    /**
     * Convert SearchFilter to SearchOptions
     */
    private SearchableDatabase.SearchOptions convertFilterToOptions(SearchFilter filter) {
        SearchableDatabase.SearchOptions options = new SearchableDatabase.SearchOptions();

        options.fileType = filter.fileType;
        options.folderId = filter.folderId;
        options.favoritesOnly = filter.favoritesOnly;
        options.minDate = filter.dateFrom;
        options.maxDate = filter.dateTo;
        options.limit = filter.limit;
        options.enableStemming = filter.enableStemming;
        options.removeStopWords = filter.removeStopWords;
        options.language = filter.language;

        return options;
    }

    /**
     * Apply additional filters
     */
    private List<SearchableDatabase.SearchResult> applyAdditionalFilters(
            List<SearchableDatabase.SearchResult> results, SearchFilter filter) {

        List<SearchableDatabase.SearchResult> filtered = new ArrayList<>();

        for (SearchableDatabase.SearchResult result : results) {
            boolean matches = true;

            // Page count filter
            if (filter.minPageCount > 0 && result.pageCount < filter.minPageCount) {
                matches = false;
            }
            if (filter.maxPageCount > 0 && result.pageCount > filter.maxPageCount) {
                matches = false;
            }

            // File size filter
            if (filter.minFileSize > 0 && result.fileSize < filter.minFileSize) {
                matches = false;
            }
            if (filter.maxFileSize > 0 && result.fileSize > filter.maxFileSize) {
                matches = false;
            }

            // Language filter
            if (filter.language != null && !filter.language.equals(result.language)) {
                matches = false;
            }

            if (matches) {
                filtered.add(result);
            }
        }

        return filtered;
    }

    /**
     * Get current filter
     */
    public SearchFilter getCurrentFilter() {
        return currentFilter;
    }

    /**
     * Set current filter
     */
    public void setCurrentFilter(SearchFilter filter) {
        this.currentFilter = filter;
    }

    /**
     * Reset filters to default
     */
    public void resetFilters() {
        currentFilter = new SearchFilter();
    }

    // ================================
    // 4. Search Suggestions and Auto-complete
    // ================================

    /**
     * Get search suggestions based on input
     */
    public void getSuggestions(String prefix, SuggestionsCallback callback) {
        if (prefix == null || prefix.trim().isEmpty()) {
            mainHandler.post(() -> callback.onSuggestionsLoaded(new ArrayList<>()));
            return;
        }

        executorService.execute(() -> {
            List<SearchSuggestion> suggestions = new ArrayList<>();

            // Get suggestions from search history
            for (SearchHistoryItem item : searchHistory) {
                if (item.query.toLowerCase().startsWith(prefix.toLowerCase())) {
                    SearchSuggestion suggestion = new SearchSuggestion();
                    suggestion.text = item.query;
                    suggestion.type = SuggestionType.HISTORY;
                    suggestion.resultCount = item.resultCount;
                    suggestions.add(suggestion);
                }
            }

            // Get suggestions from saved searches
            for (SavedSearch saved : savedSearches) {
                if (saved.query.toLowerCase().startsWith(prefix.toLowerCase())) {
                    SearchSuggestion suggestion = new SearchSuggestion();
                    suggestion.text = saved.query;
                    suggestion.type = SuggestionType.SAVED;
                    suggestion.name = saved.name;
                    suggestions.add(suggestion);
                }
            }

            // Get suggestions from database
            searchableDatabase.getSearchSuggestions(prefix, 10,
                new SearchableDatabase.SuggestionCallback() {
                    @Override
                    public void onSuggestionsLoaded(List<String> dbSuggestions) {
                        for (String text : dbSuggestions) {
                            SearchSuggestion suggestion = new SearchSuggestion();
                            suggestion.text = text;
                            suggestion.type = SuggestionType.DATABASE;
                            suggestions.add(suggestion);
                        }

                        // Remove duplicates
                        List<SearchSuggestion> unique = removeDuplicateSuggestions(suggestions);

                        // Sort by relevance
                        Collections.sort(unique, (s1, s2) -> {
                            // Prioritize: SAVED > HISTORY > DATABASE
                            if (s1.type != s2.type) {
                                return s1.type.ordinal() - s2.type.ordinal();
                            }
                            return s1.text.compareToIgnoreCase(s2.text);
                        });

                        // Limit to 10
                        if (unique.size() > 10) {
                            unique = unique.subList(0, 10);
                        }

                        List<SearchSuggestion> finalSuggestions = unique;
                        mainHandler.post(() -> callback.onSuggestionsLoaded(finalSuggestions));
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Error getting suggestions", e);
                        mainHandler.post(() -> callback.onSuggestionsLoaded(suggestions));
                    }
                });
        });
    }

    /**
     * Remove duplicate suggestions
     */
    private List<SearchSuggestion> removeDuplicateSuggestions(List<SearchSuggestion> suggestions) {
        Map<String, SearchSuggestion> uniqueMap = new HashMap<>();

        for (SearchSuggestion suggestion : suggestions) {
            String key = suggestion.text.toLowerCase();
            if (!uniqueMap.containsKey(key) ||
                suggestion.type.ordinal() < uniqueMap.get(key).type.ordinal()) {
                uniqueMap.put(key, suggestion);
            }
        }

        return new ArrayList<>(uniqueMap.values());
    }

    /**
     * Get popular search terms
     */
    public void getPopularSearches(int limit, PopularSearchesCallback callback) {
        searchableDatabase.getPopularSearches(limit,
            new SearchableDatabase.PopularSearchCallback() {
                @Override
                public void onPopularSearchesLoaded(List<SearchableDatabase.PopularSearch> searches) {
                    mainHandler.post(() -> callback.onPopularSearchesLoaded(searches));
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Error loading popular searches", e);
                    mainHandler.post(() -> callback.onError(e));
                }
            });
    }

    // ================================
    // 5. Search History Management
    // ================================

    /**
     * Add query to search history
     */
    private void addToSearchHistory(String query, int resultCount) {
        // Check if already exists
        for (int i = 0; i < searchHistory.size(); i++) {
            if (searchHistory.get(i).query.equalsIgnoreCase(query)) {
                searchHistory.remove(i);
                break;
            }
        }

        // Add to beginning
        SearchHistoryItem item = new SearchHistoryItem();
        item.query = query;
        item.timestamp = System.currentTimeMillis();
        item.resultCount = resultCount;

        searchHistory.add(0, item);

        // Limit size
        if (searchHistory.size() > MAX_HISTORY_SIZE) {
            searchHistory = searchHistory.subList(0, MAX_HISTORY_SIZE);
        }

        // Save to preferences
        saveSearchHistory();
    }

    /**
     * Get search history
     */
    public List<SearchHistoryItem> getSearchHistory() {
        return new ArrayList<>(searchHistory);
    }

    /**
     * Clear search history
     */
    public void clearSearchHistory() {
        searchHistory.clear();
        saveSearchHistory();
        Log.d(TAG, "Search history cleared");
    }

    /**
     * Remove item from search history
     */
    public void removeFromHistory(String query) {
        for (int i = 0; i < searchHistory.size(); i++) {
            if (searchHistory.get(i).query.equalsIgnoreCase(query)) {
                searchHistory.remove(i);
                saveSearchHistory();
                break;
            }
        }
    }

    /**
     * Save search history to preferences
     */
    private void saveSearchHistory() {
        try {
            JSONArray array = new JSONArray();

            for (SearchHistoryItem item : searchHistory) {
                JSONObject json = new JSONObject();
                json.put("query", item.query);
                json.put("timestamp", item.timestamp);
                json.put("resultCount", item.resultCount);
                array.put(json);
            }

            prefs.edit().putString(KEY_SEARCH_HISTORY, array.toString()).apply();

        } catch (JSONException e) {
            Log.e(TAG, "Error saving search history", e);
        }
    }

    /**
     * Load search history from preferences
     */
    private void loadSearchHistory() {
        try {
            String json = prefs.getString(KEY_SEARCH_HISTORY, "[]");
            JSONArray array = new JSONArray(json);

            searchHistory.clear();

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);

                SearchHistoryItem item = new SearchHistoryItem();
                item.query = obj.getString("query");
                item.timestamp = obj.getLong("timestamp");
                item.resultCount = obj.optInt("resultCount", 0);

                searchHistory.add(item);
            }

            Log.d(TAG, "Loaded " + searchHistory.size() + " search history items");

        } catch (JSONException e) {
            Log.e(TAG, "Error loading search history", e);
        }
    }

    // ================================
    // 6. Saved Searches Functionality
    // ================================

    /**
     * Save current search
     */
    public void saveCurrentSearch(String name, SavedSearchCallback callback) {
        saveSearch(name, currentQuery, currentFilter, callback);
    }

    /**
     * Save search with query and filter
     */
    public void saveSearch(String name, String query, SearchFilter filter, SavedSearchCallback callback) {
        // Check if name already exists
        for (SavedSearch saved : savedSearches) {
            if (saved.name.equalsIgnoreCase(name)) {
                mainHandler.post(() -> callback.onError(
                    new Exception("A saved search with this name already exists")));
                return;
            }
        }

        SavedSearch savedSearch = new SavedSearch();
        savedSearch.id = System.currentTimeMillis();
        savedSearch.name = name;
        savedSearch.query = query;
        savedSearch.filter = filter;
        savedSearch.createdAt = System.currentTimeMillis();

        savedSearches.add(savedSearch);
        saveSavedSearches();

        mainHandler.post(() -> callback.onSaved(savedSearch));

        Log.d(TAG, "Saved search: " + name);
    }

    /**
     * Get all saved searches
     */
    public List<SavedSearch> getSavedSearches() {
        return new ArrayList<>(savedSearches);
    }

    /**
     * Execute saved search
     */
    public void executeSavedSearch(SavedSearch savedSearch, SearchCallback callback) {
        searchWithFilters(savedSearch.query, savedSearch.filter, callback);
    }

    /**
     * Delete saved search
     */
    public void deleteSavedSearch(long id) {
        for (int i = 0; i < savedSearches.size(); i++) {
            if (savedSearches.get(i).id == id) {
                savedSearches.remove(i);
                saveSavedSearches();
                Log.d(TAG, "Deleted saved search: " + id);
                break;
            }
        }
    }

    /**
     * Update saved search
     */
    public void updateSavedSearch(SavedSearch savedSearch) {
        for (int i = 0; i < savedSearches.size(); i++) {
            if (savedSearches.get(i).id == savedSearch.id) {
                savedSearches.set(i, savedSearch);
                saveSavedSearches();
                Log.d(TAG, "Updated saved search: " + savedSearch.name);
                break;
            }
        }
    }

    /**
     * Save saved searches to preferences
     */
    private void saveSavedSearches() {
        try {
            JSONArray array = new JSONArray();

            for (SavedSearch saved : savedSearches) {
                JSONObject json = new JSONObject();
                json.put("id", saved.id);
                json.put("name", saved.name);
                json.put("query", saved.query);
                json.put("createdAt", saved.createdAt);

                // Save filter
                JSONObject filterJson = new JSONObject();
                filterJson.put("fileType", saved.filter.fileType);
                filterJson.put("folderId", saved.filter.folderId);
                filterJson.put("favoritesOnly", saved.filter.favoritesOnly);
                filterJson.put("dateFrom", saved.filter.dateFrom);
                filterJson.put("dateTo", saved.filter.dateTo);
                json.put("filter", filterJson);

                array.put(json);
            }

            prefs.edit().putString(KEY_SAVED_SEARCHES, array.toString()).apply();

        } catch (JSONException e) {
            Log.e(TAG, "Error saving saved searches", e);
        }
    }

    /**
     * Load saved searches from preferences
     */
    private void loadSavedSearches() {
        try {
            String json = prefs.getString(KEY_SAVED_SEARCHES, "[]");
            JSONArray array = new JSONArray(json);

            savedSearches.clear();

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);

                SavedSearch saved = new SavedSearch();
                saved.id = obj.getLong("id");
                saved.name = obj.getString("name");
                saved.query = obj.getString("query");
                saved.createdAt = obj.getLong("createdAt");

                // Load filter
                if (obj.has("filter")) {
                    JSONObject filterJson = obj.getJSONObject("filter");
                    saved.filter = new SearchFilter();
                    saved.filter.fileType = filterJson.optString("fileType", null);
                    saved.filter.folderId = filterJson.has("folderId") ?
                        filterJson.getLong("folderId") : null;
                    saved.filter.favoritesOnly = filterJson.optBoolean("favoritesOnly", false);
                    saved.filter.dateFrom = filterJson.optLong("dateFrom", 0);
                    saved.filter.dateTo = filterJson.optLong("dateTo", 0);
                }

                savedSearches.add(saved);
            }

            Log.d(TAG, "Loaded " + savedSearches.size() + " saved searches");

        } catch (JSONException e) {
            Log.e(TAG, "Error loading saved searches", e);
        }
    }

    // ================================
    // 7. Result Highlighting and Ranking
    // ================================

    /**
     * Highlight search terms in text
     */
    public SpannableString highlightText(String text, String query) {
        SpannableString spannableString = new SpannableString(text);

        if (query == null || query.trim().isEmpty()) {
            return spannableString;
        }

        String lowerText = text.toLowerCase();
        String lowerQuery = query.toLowerCase();

        // Split query into terms
        String[] terms = lowerQuery.split("\\s+");

        int highlightColor = ContextCompat.getColor(context, android.R.color.holo_orange_light);

        for (String term : terms) {
            if (term.length() < 2) continue;

            int start = 0;
            while ((start = lowerText.indexOf(term, start)) != -1) {
                spannableString.setSpan(
                    new BackgroundColorSpan(highlightColor),
                    start,
                    start + term.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                start += term.length();
            }
        }

        return spannableString;
    }

    /**
     * Re-rank results based on custom criteria
     */
    public List<SearchableDatabase.SearchResult> reRankResults(
            List<SearchableDatabase.SearchResult> results, RankingCriteria criteria) {

        List<SearchableDatabase.SearchResult> reranked = new ArrayList<>(results);

        Collections.sort(reranked, (r1, r2) -> {
            double score1 = calculateCustomScore(r1, criteria);
            double score2 = calculateCustomScore(r2, criteria);
            return Double.compare(score2, score1);
        });

        return reranked;
    }

    /**
     * Calculate custom ranking score
     */
    private double calculateCustomScore(SearchableDatabase.SearchResult result, RankingCriteria criteria) {
        double score = result.relevanceScore;

        if (criteria.favorRecent) {
            long daysSince = (System.currentTimeMillis() - result.createdAt) / (1000 * 60 * 60 * 24);
            score += Math.max(0, 10 - daysSince); // Up to +10 for today
        }

        if (criteria.favorLarge) {
            score += Math.min(5, result.wordCount / 200.0); // Up to +5 for large docs
        }

        if (criteria.favorFavorites && result.isFavorite) {
            score += 15; // Big boost for favorites
        }

        if (criteria.favorPDF && "PDF".equals(result.fileType)) {
            score += 5;
        }

        return score;
    }

    /**
     * Group results by category
     */
    public Map<String, List<SearchableDatabase.SearchResult>> groupResults(
            List<SearchableDatabase.SearchResult> results) {

        Map<String, List<SearchableDatabase.SearchResult>> grouped = new HashMap<>();
        grouped.put("Recent", new ArrayList<>());
        grouped.put("PDF Documents", new ArrayList<>());
        grouped.put("Images", new ArrayList<>());
        grouped.put("Favorites", new ArrayList<>());
        grouped.put("Other", new ArrayList<>());

        for (SearchableDatabase.SearchResult result : results) {
            long daysSince = (System.currentTimeMillis() - result.createdAt) / (1000 * 60 * 60 * 24);

            if (daysSince < 7) {
                grouped.get("Recent").add(result);
            }

            if (result.isFavorite) {
                grouped.get("Favorites").add(result);
            }

            if ("PDF".equals(result.fileType)) {
                grouped.get("PDF Documents").add(result);
            } else if ("IMAGE".equals(result.fileType)) {
                grouped.get("Images").add(result);
            } else {
                grouped.get("Other").add(result);
            }
        }

        // Remove empty categories
        grouped.entrySet().removeIf(entry -> entry.getValue().isEmpty());

        return grouped;
    }

    // ================================
    // 8. Search Analytics
    // ================================

    /**
     * Get search analytics
     */
    public SearchAnalytics getAnalytics() {
        return analytics;
    }

    /**
     * Get search performance metrics
     */
    public SearchPerformance getPerformanceMetrics() {
        SearchPerformance performance = new SearchPerformance();

        performance.totalSearches = analytics.totalSearches;
        performance.avgSearchTime = analytics.getTotalSearchTime() /
            Math.max(1, analytics.totalSearches);
        performance.avgResultCount = analytics.getTotalResults() /
            Math.max(1, analytics.totalSearches);

        // Cache hit rate
        SearchableDatabase.CacheStats cacheStats = searchableDatabase.getCacheStats();
        performance.cacheHitRate = cacheStats.hitRate;

        // Most searched terms
        performance.topSearches = analytics.getMostSearchedTerms(5);

        return performance;
    }

    /**
     * Save analytics
     */
    private void saveAnalytics() {
        try {
            JSONObject json = new JSONObject();
            json.put("totalSearches", analytics.totalSearches);
            json.put("recentSearchTimes", new JSONArray(analytics.recentSearchTimes));

            JSONObject termsJson = new JSONObject();
            for (Map.Entry<String, Integer> entry : analytics.searchTermCounts.entrySet()) {
                termsJson.put(entry.getKey(), entry.getValue());
            }
            json.put("searchTermCounts", termsJson);

            prefs.edit().putString(KEY_SEARCH_ANALYTICS, json.toString()).apply();

        } catch (JSONException e) {
            Log.e(TAG, "Error saving analytics", e);
        }
    }

    /**
     * Load analytics
     */
    private void loadAnalytics() {
        try {
            String json = prefs.getString(KEY_SEARCH_ANALYTICS, "{}");
            JSONObject obj = new JSONObject(json);

            analytics.totalSearches = obj.optInt("totalSearches", 0);

            JSONArray timesArray = obj.optJSONArray("recentSearchTimes");
            if (timesArray != null) {
                for (int i = 0; i < timesArray.length(); i++) {
                    analytics.recentSearchTimes.add(timesArray.getLong(i));
                }
            }

            JSONObject termsObj = obj.optJSONObject("searchTermCounts");
            if (termsObj != null) {
                JSONArray keys = termsObj.names();
                if (keys != null) {
                    for (int i = 0; i < keys.length(); i++) {
                        String key = keys.getString(i);
                        analytics.searchTermCounts.put(key, termsObj.getInt(key));
                    }
                }
            }

            Log.d(TAG, "Loaded analytics: " + analytics.totalSearches + " searches");

        } catch (JSONException e) {
            Log.e(TAG, "Error loading analytics", e);
        }
    }

    // ================================
    // Listener Management
    // ================================

    /**
     * Add search listener
     */
    public void addSearchListener(SearchListener listener) {
        searchListeners.add(listener);
    }

    /**
     * Remove search listener
     */
    public void removeSearchListener(SearchListener listener) {
        searchListeners.remove(listener);
    }

    /**
     * Notify search pending
     */
    private void notifySearchPending(String query) {
        for (SearchListener listener : searchListeners) {
            listener.onSearchPending(query);
        }
    }

    /**
     * Notify search started
     */
    private void notifySearchStarted(String query) {
        for (SearchListener listener : searchListeners) {
            listener.onSearchStarted(query);
        }
    }

    /**
     * Notify search complete
     */
    private void notifySearchComplete(List<SearchableDatabase.SearchResult> results, long searchTime) {
        for (SearchListener listener : searchListeners) {
            listener.onSearchComplete(results, searchTime);
        }
    }

    /**
     * Notify search error
     */
    private void notifySearchError(Exception e) {
        for (SearchListener listener : searchListeners) {
            listener.onSearchError(e);
        }
    }

    /**
     * Notify search cleared
     */
    private void notifySearchCleared() {
        for (SearchListener listener : searchListeners) {
            listener.onSearchCleared();
        }
    }

    /**
     * Cleanup resources
     */
    public void cleanup() {
        cancelPendingSearch();
        executorService.shutdown();
        searchListeners.clear();
        Log.d(TAG, "DocumentSearchManager cleaned up");
    }

    // ================================
    // Data Classes
    // ================================

    /**
     * Search Filter
     */
    public static class SearchFilter {
        public String fileType = null;
        public Long folderId = null;
        public boolean favoritesOnly = false;
        public long dateFrom = 0;
        public long dateTo = 0;
        public int minPageCount = 0;
        public int maxPageCount = 0;
        public long minFileSize = 0;
        public long maxFileSize = 0;
        public String language = "en";
        public int limit = 50;
        public boolean enableStemming = true;
        public boolean removeStopWords = true;
        public int searchFields = SearchField.ALL;
    }

    /**
     * Search Fields
     */
    public static class SearchField {
        public static final int DOCUMENT_NAME = 1;
        public static final int FILE_NAME = 2;
        public static final int OCR_TEXT = 4;
        public static final int DESCRIPTION = 8;
        public static final int TAGS = 16;
        public static final int ALL = DOCUMENT_NAME | FILE_NAME | OCR_TEXT | DESCRIPTION | TAGS;
    }

    /**
     * Search History Item
     */
    public static class SearchHistoryItem {
        public String query;
        public long timestamp;
        public int resultCount;
    }

    /**
     * Saved Search
     */
    public static class SavedSearch {
        public long id;
        public String name;
        public String query;
        public SearchFilter filter = new SearchFilter();
        public long createdAt;
    }

    /**
     * Search Suggestion
     */
    public static class SearchSuggestion {
        public String text;
        public SuggestionType type;
        public int resultCount;
        public String name; // For saved searches
    }

    /**
     * Suggestion Type
     */
    public enum SuggestionType {
        SAVED,
        HISTORY,
        DATABASE
    }

    /**
     * Ranking Criteria
     */
    public static class RankingCriteria {
        public boolean favorRecent = false;
        public boolean favorLarge = false;
        public boolean favorFavorites = false;
        public boolean favorPDF = false;
    }

    /**
     * Search Analytics
     */
    public static class SearchAnalytics {
        public int totalSearches = 0;
        public List<Long> recentSearchTimes = new ArrayList<>();
        public Map<String, Integer> searchTermCounts = new HashMap<>();

        void recordSearch(String query, int resultCount, long searchTime) {
            totalSearches++;

            recentSearchTimes.add(searchTime);
            if (recentSearchTimes.size() > 100) {
                recentSearchTimes.remove(0);
            }

            searchTermCounts.put(query, searchTermCounts.getOrDefault(query, 0) + 1);
        }

        long getTotalSearchTime() {
            long total = 0;
            for (long time : recentSearchTimes) {
                total += time;
            }
            return total;
        }

        int getTotalResults() {
            // This would need to be tracked separately
            return 0;
        }

        List<String> getMostSearchedTerms(int limit) {
            List<Map.Entry<String, Integer>> entries = new ArrayList<>(searchTermCounts.entrySet());
            Collections.sort(entries, (e1, e2) -> e2.getValue().compareTo(e1.getValue()));

            List<String> topTerms = new ArrayList<>();
            for (int i = 0; i < Math.min(limit, entries.size()); i++) {
                topTerms.add(entries.get(i).getKey());
            }
            return topTerms;
        }
    }

    /**
     * Search Performance
     */
    public static class SearchPerformance {
        public int totalSearches;
        public long avgSearchTime;
        public int avgResultCount;
        public float cacheHitRate;
        public List<String> topSearches;
    }

    // ================================
    // Callbacks
    // ================================

    public interface SearchListener {
        void onSearchPending(String query);
        void onSearchStarted(String query);
        void onSearchComplete(List<SearchableDatabase.SearchResult> results, long searchTime);
        void onSearchError(Exception e);
        void onSearchCleared();
    }

    public interface SearchCallback {
        void onSearchComplete(List<SearchableDatabase.SearchResult> results, long searchTime);
        void onSearchError(Exception e);
    }

    public interface SearchAllCallback {
        void onAllFieldsSearched(Map<String, List<SearchableDatabase.SearchResult>> results);
    }

    public interface SuggestionsCallback {
        void onSuggestionsLoaded(List<SearchSuggestion> suggestions);
    }

    public interface PopularSearchesCallback {
        void onPopularSearchesLoaded(List<SearchableDatabase.PopularSearch> searches);
        void onError(Exception e);
    }

    public interface SavedSearchCallback {
        void onSaved(SavedSearch savedSearch);
        void onError(Exception e);
    }
}

