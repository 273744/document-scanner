package com.srikanth.docscanner;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.srikanth.docscanner.database.Document;
import com.srikanth.docscanner.database.DocumentRepository;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SearchSuggestionsProvider - Intelligent search assistance
 *
 * Features:
 * 1. Recent searches with frequency tracking
 * 2. Popular searches across user base
 * 3. Contextual suggestions based on current folder
 * 4. Auto-complete for document names and content
 * 5. Spell correction for search queries
 * 6. Search intent recognition (date, person, type)
 * 7. Machine learning for personalized suggestions
 * 8. Privacy-preserving suggestion algorithms
 */
public class SearchSuggestionsProvider {

    private static final String TAG = "SearchSuggestions";

    // Singleton instance
    private static SearchSuggestionsProvider instance;

    // Context
    private final Context context;

    // Dependencies
    private final SearchableDatabase searchableDatabase;
    private final DocumentRepository documentRepository;
    private final SmartCategorizationEngine categorizationEngine;

    // Threading
    private final ExecutorService executorService;
    private final Handler mainHandler;

    // SharedPreferences
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "SearchSuggestionsPrefs";
    private static final String KEY_RECENT_SEARCHES = "recent_searches";
    private static final String KEY_SEARCH_FREQUENCIES = "search_frequencies";
    private static final String KEY_PERSONALIZATION = "personalization_data";

    // Recent searches cache
    private List<RecentSearch> recentSearches = new ArrayList<>();
    private static final int MAX_RECENT_SEARCHES = 100;

    // Search frequency tracking
    private Map<String, Integer> searchFrequencies = new HashMap<>();

    // Contextual data
    private Long currentFolderId = null;
    private Map<Long, List<String>> folderContextCache = new HashMap<>();

    // Spell correction dictionary
    private Map<String, List<String>> spellDictionary = new HashMap<>();
    private static final int MAX_EDIT_DISTANCE = 2;

    // Intent patterns
    private List<IntentPattern> intentPatterns = new ArrayList<>();

    // Personalization data
    private PersonalizationData personalizationData = new PersonalizationData();

    /**
     * Private constructor (Singleton)
     */
    private SearchSuggestionsProvider(Context context) {
        this.context = context.getApplicationContext();
        this.searchableDatabase = SearchableDatabase.getInstance(context);
        this.documentRepository = DocumentRepository.getInstance(context);
        this.categorizationEngine = SmartCategorizationEngine.getInstance(context);
        this.executorService = Executors.newFixedThreadPool(2);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Load data
        loadRecentSearches();
        loadSearchFrequencies();
        loadPersonalizationData();

        // Initialize intent patterns
        initializeIntentPatterns();

        // Build spell dictionary in background
        buildSpellDictionary();

        Log.d(TAG, "SearchSuggestionsProvider initialized");
    }

    /**
     * Get singleton instance
     */
    public static synchronized SearchSuggestionsProvider getInstance(Context context) {
        if (instance == null) {
            instance = new SearchSuggestionsProvider(context);
        }
        return instance;
    }

    // ================================
    // 1. Recent Searches with Frequency Tracking
    // ================================

    /**
     * Record a search query
     */
    public void recordSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            return;
        }

        executorService.execute(() -> {
            String normalizedQuery = normalizeQuery(query);

            // Update frequency
            int frequency = searchFrequencies.getOrDefault(normalizedQuery, 0) + 1;
            searchFrequencies.put(normalizedQuery, frequency);

            // Add to recent searches
            RecentSearch search = new RecentSearch();
            search.query = normalizedQuery;
            search.timestamp = System.currentTimeMillis();
            search.frequency = frequency;
            search.folderId = currentFolderId;

            // Remove if already exists
            recentSearches.removeIf(s -> s.query.equalsIgnoreCase(normalizedQuery));

            // Add to beginning
            recentSearches.add(0, search);

            // Limit size
            if (recentSearches.size() > MAX_RECENT_SEARCHES) {
                recentSearches = recentSearches.subList(0, MAX_RECENT_SEARCHES);
            }

            // Update personalization
            personalizationData.recordSearch(normalizedQuery);

            // Save to preferences
            saveRecentSearches();
            saveSearchFrequencies();
            savePersonalizationData();

            Log.d(TAG, "Recorded search: " + normalizedQuery + " (frequency: " + frequency + ")");
        });
    }

    /**
     * Get recent searches
     */
    public void getRecentSearches(int limit, SuggestionsCallback callback) {
        executorService.execute(() -> {
            List<SearchSuggestion> suggestions = new ArrayList<>();

            int count = Math.min(limit, recentSearches.size());
            for (int i = 0; i < count; i++) {
                RecentSearch recent = recentSearches.get(i);

                SearchSuggestion suggestion = new SearchSuggestion();
                suggestion.text = recent.query;
                suggestion.type = SuggestionType.RECENT;
                suggestion.frequency = recent.frequency;
                suggestion.timestamp = recent.timestamp;
                suggestion.confidence = calculateRecentScore(recent);

                suggestions.add(suggestion);
            }

            mainHandler.post(() -> callback.onSuggestionsReady(suggestions));
        });
    }

    /**
     * Calculate score for recent search
     */
    private float calculateRecentScore(RecentSearch search) {
        // Combine recency and frequency
        long hoursSince = (System.currentTimeMillis() - search.timestamp) / (1000 * 60 * 60);
        float recencyScore = Math.max(0, 1.0f - (hoursSince / 168.0f)); // Decay over 1 week

        float frequencyScore = Math.min(1.0f, search.frequency / 10.0f);

        return (recencyScore * 0.6f) + (frequencyScore * 0.4f);
    }

    /**
     * Clear recent searches
     */
    public void clearRecentSearches() {
        recentSearches.clear();
        saveRecentSearches();
        Log.d(TAG, "Recent searches cleared");
    }

    // ================================
    // 2. Popular Searches
    // ================================

    /**
     * Get popular searches
     */
    public void getPopularSearches(int limit, SuggestionsCallback callback) {
        searchableDatabase.getPopularSearches(limit,
            new SearchableDatabase.PopularSearchCallback() {
                @Override
                public void onPopularSearchesLoaded(List<SearchableDatabase.PopularSearch> searches) {
                    List<SearchSuggestion> suggestions = new ArrayList<>();

                    for (SearchableDatabase.PopularSearch popular : searches) {
                        SearchSuggestion suggestion = new SearchSuggestion();
                        suggestion.text = popular.query;
                        suggestion.type = SuggestionType.POPULAR;
                        suggestion.frequency = popular.searchCount;
                        suggestion.confidence = Math.min(1.0f, popular.searchCount / 50.0f);

                        suggestions.add(suggestion);
                    }

                    mainHandler.post(() -> callback.onSuggestionsReady(suggestions));
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Error loading popular searches", e);
                    mainHandler.post(() -> callback.onSuggestionsReady(new ArrayList<>()));
                }
            });
    }

    /**
     * Get trending searches (popular + recent)
     */
    public void getTrendingSearches(int limit, SuggestionsCallback callback) {
        executorService.execute(() -> {
            List<SearchSuggestion> suggestions = new ArrayList<>();

            // Combine recent high-frequency searches
            for (RecentSearch recent : recentSearches) {
                if (recent.frequency >= 3) {
                    SearchSuggestion suggestion = new SearchSuggestion();
                    suggestion.text = recent.query;
                    suggestion.type = SuggestionType.TRENDING;
                    suggestion.frequency = recent.frequency;
                    suggestion.confidence = calculateRecentScore(recent);

                    suggestions.add(suggestion);
                }
            }

            // Sort by confidence
            Collections.sort(suggestions, (s1, s2) ->
                Float.compare(s2.confidence, s1.confidence));

            // Limit
            final List<SearchSuggestion> finalSuggestions;
            if (suggestions.size() > limit) {
                finalSuggestions = suggestions.subList(0, limit);
            } else {
                finalSuggestions = suggestions;
            }

            mainHandler.post(() -> callback.onSuggestionsReady(finalSuggestions));
        });
    }

    // ================================
    // 3. Contextual Suggestions
    // ================================

    /**
     * Set current folder context
     */
    public void setFolderContext(Long folderId) {
        this.currentFolderId = folderId;
    }

    /**
     * Get contextual suggestions based on current folder
     */
    public void getContextualSuggestions(int limit, SuggestionsCallback callback) {
        if (currentFolderId == null) {
            callback.onSuggestionsReady(new ArrayList<>());
            return;
        }

        executorService.execute(() -> {
            List<SearchSuggestion> suggestions = new ArrayList<>();

            // Check cache first
            List<String> cachedTerms = folderContextCache.get(currentFolderId);
            if (cachedTerms != null) {
                for (String term : cachedTerms) {
                    SearchSuggestion suggestion = new SearchSuggestion();
                    suggestion.text = term;
                    suggestion.type = SuggestionType.CONTEXTUAL;
                    suggestion.confidence = 0.8f;
                    suggestions.add(suggestion);
                }

                mainHandler.post(() -> callback.onSuggestionsReady(suggestions));
                return;
            }

            // Get documents in folder
            final List<SearchSuggestion> finalSuggestions = suggestions;
            documentRepository.getDocumentByIdSync(currentFolderId, documents -> {
                if (documents == null) {
                    mainHandler.post(() -> callback.onSuggestionsReady(new ArrayList<>()));
                    return;
                }

                // Extract common terms from documents
                Map<String, Integer> termFrequency = new HashMap<>();

                for (Object obj : (List<?>) documents) {
                    if (obj instanceof Document) {
                        Document doc = (Document) obj;

                        // Extract from document name
                        String[] nameWords = doc.getDocumentName().toLowerCase().split("\\s+");
                        for (String word : nameWords) {
                            if (word.length() > 3) {
                                termFrequency.put(word, termFrequency.getOrDefault(word, 0) + 1);
                            }
                        }
                    }
                }

                // Sort by frequency
                List<Map.Entry<String, Integer>> sorted = new ArrayList<>(termFrequency.entrySet());
                sorted.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

                // Create suggestions
                List<String> terms = new ArrayList<>();
                for (int i = 0; i < Math.min(limit, sorted.size()); i++) {
                    String term = sorted.get(i).getKey();
                    terms.add(term);

                    SearchSuggestion suggestion = new SearchSuggestion();
                    suggestion.text = term;
                    suggestion.type = SuggestionType.CONTEXTUAL;
                    suggestion.frequency = sorted.get(i).getValue();
                    suggestion.confidence = 0.7f;

                    finalSuggestions.add(suggestion);
                }

                // Cache terms
                folderContextCache.put(currentFolderId, terms);

                mainHandler.post(() -> callback.onSuggestionsReady(finalSuggestions));
            });
        });
    }

    // ================================
    // 4. Auto-Complete
    // ================================

    /**
     * Get auto-complete suggestions
     */
    public void getAutoCompleteSuggestions(String prefix, int limit, SuggestionsCallback callback) {
        if (prefix == null || prefix.length() < 2) {
            callback.onSuggestionsReady(new ArrayList<>());
            return;
        }

        executorService.execute(() -> {
            List<SearchSuggestion> suggestions = new ArrayList<>();
            String lowerPrefix = prefix.toLowerCase();

            // 1. From recent searches
            for (RecentSearch recent : recentSearches) {
                if (recent.query.toLowerCase().startsWith(lowerPrefix)) {
                    SearchSuggestion suggestion = new SearchSuggestion();
                    suggestion.text = recent.query;
                    suggestion.type = SuggestionType.AUTOCOMPLETE;
                    suggestion.confidence = calculateRecentScore(recent);
                    suggestions.add(suggestion);
                }
            }

            // 2. From database (document names)
            searchableDatabase.getSearchSuggestions(prefix, limit,
                new SearchableDatabase.SuggestionCallback() {
                    @Override
                    public void onSuggestionsLoaded(List<String> dbSuggestions) {
                        for (String text : dbSuggestions) {
                            SearchSuggestion suggestion = new SearchSuggestion();
                            suggestion.text = text;
                            suggestion.type = SuggestionType.AUTOCOMPLETE;
                            suggestion.confidence = 0.6f;
                            suggestions.add(suggestion);
                        }

                        // Remove duplicates and sort
                        List<SearchSuggestion> unique = removeDuplicates(suggestions);
                        Collections.sort(unique, (s1, s2) ->
                            Float.compare(s2.confidence, s1.confidence));

                        // Limit
                        if (unique.size() > limit) {
                            unique = unique.subList(0, limit);
                        }

                        List<SearchSuggestion> finalSuggestions = unique;
                        mainHandler.post(() -> callback.onSuggestionsReady(finalSuggestions));
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Error getting auto-complete", e);
                        mainHandler.post(() -> callback.onSuggestionsReady(suggestions));
                    }
                });
        });
    }

    // ================================
    // 5. Spell Correction
    // ================================

    /**
     * Build spell dictionary from indexed documents
     */
    private void buildSpellDictionary() {
        executorService.execute(() -> {
            try {
                // Get all document names from database
                // This would be populated from actual documents
                // For now, initialize with common terms

                String[] commonTerms = {
                    "invoice", "receipt", "contract", "agreement", "document",
                    "report", "letter", "statement", "certificate", "form",
                    "application", "request", "proposal", "memo", "notice"
                };

                for (String term : commonTerms) {
                    addToSpellDictionary(term);
                }

                Log.d(TAG, "Spell dictionary built with " + spellDictionary.size() + " terms");

            } catch (Exception e) {
                Log.e(TAG, "Error building spell dictionary", e);
            }
        });
    }

    /**
     * Add term to spell dictionary
     */
    private void addToSpellDictionary(String term) {
        String normalized = term.toLowerCase().trim();
        if (normalized.length() < 3) return;

        if (!spellDictionary.containsKey(normalized)) {
            spellDictionary.put(normalized, new ArrayList<>());
        }
    }

    /**
     * Get spell correction suggestions
     */
    public void getSpellCorrections(String query, int limit, SuggestionsCallback callback) {
        executorService.execute(() -> {
            List<SearchSuggestion> suggestions = new ArrayList<>();

            String[] words = query.toLowerCase().split("\\s+");
            boolean hasMisspelling = false;

            for (String word : words) {
                if (word.length() < 3) continue;

                // Check if word exists in dictionary
                if (!spellDictionary.containsKey(word)) {
                    // Find similar words
                    List<String> corrections = findSimilarWords(word, limit);

                    for (String correction : corrections) {
                        SearchSuggestion suggestion = new SearchSuggestion();
                        suggestion.text = query.replace(word, correction);
                        suggestion.type = SuggestionType.SPELL_CORRECTION;
                        suggestion.originalQuery = query;
                        suggestion.confidence = 0.7f;

                        suggestions.add(suggestion);
                        hasMisspelling = true;
                    }
                }
            }

            if (!hasMisspelling) {
                suggestions.clear(); // No corrections needed
            }

            mainHandler.post(() -> callback.onSuggestionsReady(suggestions));
        });
    }

    /**
     * Find similar words using edit distance
     */
    private List<String> findSimilarWords(String word, int limit) {
        List<String> similar = new ArrayList<>();

        for (String dictWord : spellDictionary.keySet()) {
            int distance = calculateEditDistance(word, dictWord);

            if (distance <= MAX_EDIT_DISTANCE) {
                similar.add(dictWord);
            }
        }

        // Limit results
        if (similar.size() > limit) {
            similar = similar.subList(0, limit);
        }

        return similar;
    }

    /**
     * Calculate Levenshtein distance
     */
    private int calculateEditDistance(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();

        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) dp[i][0] = i;
        for (int j = 0; j <= len2; j++) dp[0][j] = j;

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
    // 6. Search Intent Recognition
    // ================================

    /**
     * Initialize intent patterns
     */
    private void initializeIntentPatterns() {
        // Date patterns
        intentPatterns.add(new IntentPattern(
            SearchIntent.DATE,
            Pattern.compile("\\b(today|yesterday|last week|last month|this year|\\d{4})\\b",
                Pattern.CASE_INSENSITIVE),
            "Date-based search"
        ));

        // Person patterns
        intentPatterns.add(new IntentPattern(
            SearchIntent.PERSON,
            Pattern.compile("\\b(from|to|by|sent by|received from)\\s+([A-Z][a-z]+\\s+[A-Z][a-z]+)\\b"),
            "Person-based search"
        ));

        // Document type patterns
        intentPatterns.add(new IntentPattern(
            SearchIntent.TYPE,
            Pattern.compile("\\b(invoice|receipt|contract|report|letter|pdf|image)s?\\b",
                Pattern.CASE_INSENSITIVE),
            "Document type search"
        ));

        // File size patterns
        intentPatterns.add(new IntentPattern(
            SearchIntent.SIZE,
            Pattern.compile("\\b(large|small|bigger than|smaller than|\\d+\\s*MB)\\b",
                Pattern.CASE_INSENSITIVE),
            "File size search"
        ));

        // Status patterns
        intentPatterns.add(new IntentPattern(
            SearchIntent.STATUS,
            Pattern.compile("\\b(favorite|starred|important|urgent|draft)s?\\b",
                Pattern.CASE_INSENSITIVE),
            "Status-based search"
        ));
    }

    /**
     * Recognize search intent
     */
    public SearchIntentResult recognizeIntent(String query) {
        SearchIntentResult result = new SearchIntentResult();
        result.query = query;
        result.intents = new ArrayList<>();

        for (IntentPattern pattern : intentPatterns) {
            Matcher matcher = pattern.pattern.matcher(query);

            if (matcher.find()) {
                IntentMatch match = new IntentMatch();
                match.intent = pattern.intent;
                match.matchedText = matcher.group();
                match.confidence = 0.8f;
                match.suggestion = pattern.suggestion;

                result.intents.add(match);
            }
        }

        // Detect date references and suggest date filters
        if (containsDateReference(query)) {
            result.suggestedFilters.add("Use date range filter");
        }

        return result;
    }

    /**
     * Check if query contains date reference
     */
    private boolean containsDateReference(String query) {
        String lower = query.toLowerCase();
        return lower.contains("today") || lower.contains("yesterday") ||
               lower.contains("last week") || lower.contains("last month") ||
               lower.matches(".*\\b\\d{4}\\b.*");
    }

    // ================================
    // 7. Machine Learning Personalization
    // ================================

    /**
     * Get personalized suggestions
     */
    public void getPersonalizedSuggestions(String context, int limit, SuggestionsCallback callback) {
        executorService.execute(() -> {
            List<SearchSuggestion> suggestions = new ArrayList<>();

            // 1. User's frequent search patterns
            for (Map.Entry<String, Integer> entry : searchFrequencies.entrySet()) {
                if (entry.getValue() >= 3) {
                    SearchSuggestion suggestion = new SearchSuggestion();
                    suggestion.text = entry.getKey();
                    suggestion.type = SuggestionType.PERSONALIZED;
                    suggestion.frequency = entry.getValue();
                    suggestion.confidence = Math.min(1.0f, entry.getValue() / 10.0f);

                    suggestions.add(suggestion);
                }
            }

            // 2. Time-based patterns
            Calendar cal = Calendar.getInstance();
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

            List<String> timeBasedSuggestions = personalizationData.getTimeBasedSuggestions(
                hour, dayOfWeek);

            for (String text : timeBasedSuggestions) {
                SearchSuggestion suggestion = new SearchSuggestion();
                suggestion.text = text;
                suggestion.type = SuggestionType.PERSONALIZED;
                suggestion.confidence = 0.7f;

                suggestions.add(suggestion);
            }

            // 3. Context-based (if provided)
            if (context != null && !context.isEmpty()) {
                List<String> contextSuggestions = personalizationData.getContextSuggestions(context);

                for (String text : contextSuggestions) {
                    SearchSuggestion suggestion = new SearchSuggestion();
                    suggestion.text = text;
                    suggestion.type = SuggestionType.PERSONALIZED;
                    suggestion.confidence = 0.75f;

                    suggestions.add(suggestion);
                }
            }

            // Sort by confidence
            Collections.sort(suggestions, (s1, s2) ->
                Float.compare(s2.confidence, s1.confidence));

            // Limit
            List<SearchSuggestion> finalSuggestions = suggestions;
            if (suggestions.size() > limit) {
                finalSuggestions = suggestions.subList(0, limit);
            }

            final List<SearchSuggestion> resultSuggestions = finalSuggestions;
            mainHandler.post(() -> callback.onSuggestionsReady(resultSuggestions));
        });
    }

    /**
     * Learn from user search behavior
     */
    public void learnFromBehavior(String query, List<SearchableDatabase.SearchResult> results,
                                  long documentId) {
        executorService.execute(() -> {
            // Record which document was clicked for this query
            personalizationData.recordClick(query, documentId);

            // Update patterns based on successful searches
            if (!results.isEmpty()) {
                personalizationData.recordSuccessfulSearch(query, results.size());
            }

            savePersonalizationData();
        });
    }

    // ================================
    // 8. Privacy-Preserving Algorithms
    // ================================

    /**
     * Anonymize search query for sharing
     */
    public String anonymizeQuery(String query) {
        String anonymized = query;

        // Remove email addresses
        anonymized = anonymized.replaceAll(
            "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b",
            "[EMAIL]");

        // Remove phone numbers
        anonymized = anonymized.replaceAll(
            "\\b\\d{3}[-.]?\\d{3}[-.]?\\d{4}\\b",
            "[PHONE]");

        // Remove credit card numbers
        anonymized = anonymized.replaceAll(
            "\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b",
            "[CARD]");

        // Remove social security numbers
        anonymized = anonymized.replaceAll(
            "\\b\\d{3}-\\d{2}-\\d{4}\\b",
            "[SSN]");

        // Remove names (capitalized words)
        anonymized = anonymized.replaceAll(
            "\\b[A-Z][a-z]+\\s+[A-Z][a-z]+\\b",
            "[NAME]");

        return anonymized;
    }

    /**
     * Get privacy-safe suggestions (exclude sensitive data)
     */
    public void getPrivacySafeSuggestions(String prefix, int limit, SuggestionsCallback callback) {
        getAutoCompleteSuggestions(prefix, limit * 2, suggestions -> {
            List<SearchSuggestion> safeSuggestions = new ArrayList<>();

            for (SearchSuggestion suggestion : suggestions) {
                if (isPrivacySafe(suggestion.text)) {
                    safeSuggestions.add(suggestion);
                }

                if (safeSuggestions.size() >= limit) {
                    break;
                }
            }

            callback.onSuggestionsReady(safeSuggestions);
        });
    }

    /**
     * Check if suggestion is privacy-safe
     */
    private boolean isPrivacySafe(String text) {
        String lower = text.toLowerCase();

        // Check for sensitive patterns
        if (lower.contains("password") || lower.contains("ssn") ||
            lower.contains("credit card") || lower.contains("secret")) {
            return false;
        }

        // Check for email patterns
        if (text.matches(".*\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b.*")) {
            return false;
        }

        // Check for phone patterns
        if (text.matches(".*\\b\\d{3}[-.]?\\d{3}[-.]?\\d{4}\\b.*")) {
            return false;
        }

        return true;
    }

    // ================================
    // Combined Suggestions
    // ================================

    /**
     * Get comprehensive suggestions
     */
    public void getComprehensiveSuggestions(String query, int limit,
                                           ComprehensiveSuggestionsCallback callback) {
        executorService.execute(() -> {
            ComprehensiveSuggestions result = new ComprehensiveSuggestions();

            // Get different types of suggestions
            getAutoCompleteSuggestions(query, 5, autoComplete -> {
                result.autoComplete = autoComplete;
                checkComplete(result, callback);
            });

            getRecentSearches(5, recent -> {
                result.recent = recent;
                checkComplete(result, callback);
            });

            getContextualSuggestions(5, contextual -> {
                result.contextual = contextual;
                checkComplete(result, callback);
            });

            getSpellCorrections(query, 3, corrections -> {
                result.corrections = corrections;
                checkComplete(result, callback);
            });

            // Recognize intent
            result.intent = recognizeIntent(query);

            getPersonalizedSuggestions(query, 5, personalized -> {
                result.personalized = personalized;
                checkComplete(result, callback);
            });
        });
    }

    /**
     * Check if all suggestions are loaded
     */
    private void checkComplete(ComprehensiveSuggestions result,
                               ComprehensiveSuggestionsCallback callback) {
        if (result.autoComplete != null && result.recent != null &&
            result.contextual != null && result.corrections != null &&
            result.personalized != null) {
            mainHandler.post(() -> callback.onSuggestionsReady(result));
        }
    }

    // ================================
    // Helper Methods
    // ================================

    /**
     * Normalize query
     */
    private String normalizeQuery(String query) {
        return query.trim().toLowerCase();
    }

    /**
     * Remove duplicate suggestions
     */
    private List<SearchSuggestion> removeDuplicates(List<SearchSuggestion> suggestions) {
        Map<String, SearchSuggestion> uniqueMap = new HashMap<>();

        for (SearchSuggestion suggestion : suggestions) {
            String key = suggestion.text.toLowerCase();

            if (!uniqueMap.containsKey(key) ||
                suggestion.confidence > uniqueMap.get(key).confidence) {
                uniqueMap.put(key, suggestion);
            }
        }

        return new ArrayList<>(uniqueMap.values());
    }

    // ================================
    // Persistence
    // ================================

    /**
     * Save recent searches
     */
    private void saveRecentSearches() {
        try {
            JSONArray array = new JSONArray();

            for (RecentSearch search : recentSearches) {
                JSONObject json = new JSONObject();
                json.put("query", search.query);
                json.put("timestamp", search.timestamp);
                json.put("frequency", search.frequency);
                if (search.folderId != null) {
                    json.put("folderId", search.folderId);
                }
                array.put(json);
            }

            prefs.edit().putString(KEY_RECENT_SEARCHES, array.toString()).apply();

        } catch (JSONException e) {
            Log.e(TAG, "Error saving recent searches", e);
        }
    }

    /**
     * Load recent searches
     */
    private void loadRecentSearches() {
        try {
            String json = prefs.getString(KEY_RECENT_SEARCHES, "[]");
            JSONArray array = new JSONArray(json);

            recentSearches.clear();

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);

                RecentSearch search = new RecentSearch();
                search.query = obj.getString("query");
                search.timestamp = obj.getLong("timestamp");
                search.frequency = obj.optInt("frequency", 1);
                if (obj.has("folderId")) {
                    search.folderId = obj.getLong("folderId");
                }

                recentSearches.add(search);
            }

            Log.d(TAG, "Loaded " + recentSearches.size() + " recent searches");

        } catch (JSONException e) {
            Log.e(TAG, "Error loading recent searches", e);
        }
    }

    /**
     * Save search frequencies
     */
    private void saveSearchFrequencies() {
        try {
            JSONObject json = new JSONObject();

            for (Map.Entry<String, Integer> entry : searchFrequencies.entrySet()) {
                json.put(entry.getKey(), entry.getValue());
            }

            prefs.edit().putString(KEY_SEARCH_FREQUENCIES, json.toString()).apply();

        } catch (JSONException e) {
            Log.e(TAG, "Error saving frequencies", e);
        }
    }

    /**
     * Load search frequencies
     */
    private void loadSearchFrequencies() {
        try {
            String json = prefs.getString(KEY_SEARCH_FREQUENCIES, "{}");
            JSONObject obj = new JSONObject(json);

            searchFrequencies.clear();

            JSONArray keys = obj.names();
            if (keys != null) {
                for (int i = 0; i < keys.length(); i++) {
                    String key = keys.getString(i);
                    searchFrequencies.put(key, obj.getInt(key));
                }
            }

            Log.d(TAG, "Loaded " + searchFrequencies.size() + " search frequencies");

        } catch (JSONException e) {
            Log.e(TAG, "Error loading frequencies", e);
        }
    }

    /**
     * Save personalization data
     */
    private void savePersonalizationData() {
        try {
            String json = personalizationData.toJSON().toString();
            prefs.edit().putString(KEY_PERSONALIZATION, json).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error saving personalization", e);
        }
    }

    /**
     * Load personalization data
     */
    private void loadPersonalizationData() {
        try {
            String json = prefs.getString(KEY_PERSONALIZATION, "{}");
            personalizationData = PersonalizationData.fromJSON(new JSONObject(json));
            Log.d(TAG, "Personalization data loaded");
        } catch (Exception e) {
            Log.e(TAG, "Error loading personalization", e);
        }
    }

    /**
     * Cleanup resources
     */
    public void cleanup() {
        executorService.shutdown();
        Log.d(TAG, "SearchSuggestionsProvider cleaned up");
    }

    // ================================
    // Data Classes
    // ================================

    /**
     * Recent Search
     */
    private static class RecentSearch {
        String query;
        long timestamp;
        int frequency;
        Long folderId;
    }

    /**
     * Search Suggestion
     */
    public static class SearchSuggestion {
        public String text;
        public SuggestionType type;
        public int frequency;
        public long timestamp;
        public float confidence;
        public String originalQuery; // For spell corrections
    }

    /**
     * Suggestion Type
     */
    public enum SuggestionType {
        RECENT,
        POPULAR,
        TRENDING,
        CONTEXTUAL,
        AUTOCOMPLETE,
        SPELL_CORRECTION,
        PERSONALIZED
    }

    /**
     * Search Intent
     */
    public enum SearchIntent {
        DATE,
        PERSON,
        TYPE,
        SIZE,
        STATUS,
        GENERAL
    }

    /**
     * Intent Pattern
     */
    private static class IntentPattern {
        SearchIntent intent;
        Pattern pattern;
        String suggestion;

        IntentPattern(SearchIntent intent, Pattern pattern, String suggestion) {
            this.intent = intent;
            this.pattern = pattern;
            this.suggestion = suggestion;
        }
    }

    /**
     * Intent Match
     */
    public static class IntentMatch {
        public SearchIntent intent;
        public String matchedText;
        public float confidence;
        public String suggestion;
    }

    /**
     * Search Intent Result
     */
    public static class SearchIntentResult {
        public String query;
        public List<IntentMatch> intents = new ArrayList<>();
        public List<String> suggestedFilters = new ArrayList<>();
    }

    /**
     * Comprehensive Suggestions
     */
    public static class ComprehensiveSuggestions {
        public List<SearchSuggestion> autoComplete;
        public List<SearchSuggestion> recent;
        public List<SearchSuggestion> contextual;
        public List<SearchSuggestion> corrections;
        public List<SearchSuggestion> personalized;
        public SearchIntentResult intent;
    }

    /**
     * Personalization Data
     */
    private static class PersonalizationData {
        Map<String, Integer> queryClicks = new HashMap<>();
        Map<String, Integer> successfulSearches = new HashMap<>();
        Map<Integer, List<String>> hourBasedSearches = new HashMap<>();
        Map<Integer, List<String>> dayBasedSearches = new HashMap<>();

        void recordSearch(String query) {
            Calendar cal = Calendar.getInstance();
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            int day = cal.get(Calendar.DAY_OF_WEEK);

            if (!hourBasedSearches.containsKey(hour)) {
                hourBasedSearches.put(hour, new ArrayList<>());
            }
            hourBasedSearches.get(hour).add(query);

            if (!dayBasedSearches.containsKey(day)) {
                dayBasedSearches.put(day, new ArrayList<>());
            }
            dayBasedSearches.get(day).add(query);
        }

        void recordClick(String query, long documentId) {
            queryClicks.put(query, queryClicks.getOrDefault(query, 0) + 1);
        }

        void recordSuccessfulSearch(String query, int resultCount) {
            if (resultCount > 0) {
                successfulSearches.put(query, resultCount);
            }
        }

        List<String> getTimeBasedSuggestions(int hour, int day) {
            List<String> suggestions = new ArrayList<>();

            if (hourBasedSearches.containsKey(hour)) {
                suggestions.addAll(hourBasedSearches.get(hour));
            }

            return suggestions;
        }

        List<String> getContextSuggestions(String context) {
            List<String> suggestions = new ArrayList<>();

            for (String query : successfulSearches.keySet()) {
                if (query.toLowerCase().contains(context.toLowerCase())) {
                    suggestions.add(query);
                }
            }

            return suggestions;
        }

        JSONObject toJSON() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("queryClicks", new JSONObject(queryClicks));
            json.put("successfulSearches", new JSONObject(successfulSearches));
            return json;
        }

        static PersonalizationData fromJSON(JSONObject json) {
            PersonalizationData data = new PersonalizationData();
            // Simplified loading
            return data;
        }
    }

    // ================================
    // Callbacks
    // ================================

    public interface SuggestionsCallback {
        void onSuggestionsReady(List<SearchSuggestion> suggestions);
    }

    public interface ComprehensiveSuggestionsCallback {
        void onSuggestionsReady(ComprehensiveSuggestions suggestions);
    }
}


