package com.srikanth.docscanner;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.srikanth.docscanner.database.Document;
import com.srikanth.docscanner.database.DocumentRepository;
import com.srikanth.docscanner.database.Folder;
import com.srikanth.docscanner.database.FolderRepository;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
 * SmartCategorizationEngine - AI-powered document organization
 *
 * Features:
 * 1. Analyze document content using OCR results
 * 2. Machine learning model for document classification
 * 3. Keyword extraction and pattern matching
 * 4. Learn from user's manual organization patterns
 * 5. Suggest appropriate folders for new documents
 * 6. Confidence scoring for categorization suggestions
 * 7. User feedback integration for model improvement
 * 8. TensorFlow Lite for on-device ML inference
 */
public class SmartCategorizationEngine {

    private static final String TAG = "SmartCategorization";

    // Singleton instance
    private static SmartCategorizationEngine instance;

    // Context
    private final Context context;

    // Repositories
    private final DocumentRepository documentRepository;
    private final FolderRepository folderRepository;

    // Threading
    private final ExecutorService executorService;
    private final Handler mainHandler;

    // TensorFlow Lite (placeholder - would need actual TFLite integration)
    // private Interpreter tfliteInterpreter;
    private boolean mlModelLoaded = false;

    // Machine Learning Model
    private MLClassificationModel classificationModel;

    // Pattern matching
    private KeywordExtractor keywordExtractor;
    private PatternMatcher patternMatcher;

    // User behavior learning
    private UserBehaviorTracker behaviorTracker;

    // Confidence thresholds
    private static final float HIGH_CONFIDENCE_THRESHOLD = 0.80f;
    private static final float MEDIUM_CONFIDENCE_THRESHOLD = 0.60f;
    private static final float LOW_CONFIDENCE_THRESHOLD = 0.40f;

    // Training data
    private Map<Long, List<TrainingExample>> folderTrainingData = new HashMap<>();

    // SharedPreferences
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "SmartCategorizationPrefs";
    private static final String KEY_MODEL_VERSION = "model_version";
    private static final String KEY_TRAINING_COUNT = "training_count";

    /**
     * Private constructor (Singleton)
     */
    private SmartCategorizationEngine(Context context) {
        this.context = context.getApplicationContext();
        this.documentRepository = DocumentRepository.getInstance(context);
        this.folderRepository = new FolderRepository(context);
        this.executorService = Executors.newFixedThreadPool(2);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Initialize components
        this.classificationModel = new MLClassificationModel();
        this.keywordExtractor = new KeywordExtractor();
        this.patternMatcher = new PatternMatcher();
        this.behaviorTracker = new UserBehaviorTracker(context);

        // Load ML model
        initializeMLModel();

        Log.d(TAG, "SmartCategorizationEngine initialized");
    }

    /**
     * Get singleton instance
     */
    public static synchronized SmartCategorizationEngine getInstance(Context context) {
        if (instance == null) {
            instance = new SmartCategorizationEngine(context);
        }
        return instance;
    }

    // ================================
    // 1. Analyze Document Content
    // ================================

    /**
     * Analyze document content using OCR and metadata
     */
    public void analyzeDocument(Document document, AnalysisCallback callback) {
        executorService.execute(() -> {
            try {
                DocumentAnalysis analysis = new DocumentAnalysis();
                analysis.documentId = document.getDocumentId();
                analysis.documentName = document.getDocumentName();

                // Extract text features
                String ocrText = document.getOcrText();
                if (ocrText != null && !ocrText.isEmpty()) {
                    analysis.keywords = keywordExtractor.extractKeywords(ocrText);
                    analysis.entities = keywordExtractor.extractEntities(ocrText);
                    analysis.patterns = patternMatcher.findPatterns(ocrText);
                    analysis.textLength = ocrText.length();
                    analysis.wordCount = countWords(ocrText);
                }

                // Extract metadata features
                analysis.fileType = document.getFileType();
                analysis.fileName = document.getDocumentName();
                analysis.createdDate = new Date(document.getCreatedAt());
                analysis.pageCount = document.getPageCount();
                analysis.fileSize = document.getFileSize();

                // Calculate text statistics
                if (ocrText != null) {
                    analysis.hasNumbers = containsNumbers(ocrText);
                    analysis.hasDates = containsDates(ocrText);
                    analysis.hasEmails = containsEmails(ocrText);
                    analysis.hasPhones = containsPhones(ocrText);
                    analysis.hasUrls = containsUrls(ocrText);
                }

                // Call ML model for classification
                if (mlModelLoaded) {
                    analysis.mlFeatures = extractMLFeatures(analysis);
                }

                mainHandler.post(() -> callback.onAnalysisComplete(analysis));

            } catch (Exception e) {
                Log.e(TAG, "Error analyzing document", e);
                mainHandler.post(() -> callback.onAnalysisError(e));
            }
        });
    }

    /**
     * Extract ML features from analysis
     */
    private float[] extractMLFeatures(DocumentAnalysis analysis) {
        // Create feature vector for ML model
        // Features: [word_count, keyword_count, has_numbers, has_dates, has_emails, page_count, etc.]
        float[] features = new float[20];

        features[0] = normalizeValue(analysis.wordCount, 0, 10000);
        features[1] = normalizeValue(analysis.keywords.size(), 0, 100);
        features[2] = analysis.hasNumbers ? 1.0f : 0.0f;
        features[3] = analysis.hasDates ? 1.0f : 0.0f;
        features[4] = analysis.hasEmails ? 1.0f : 0.0f;
        features[5] = analysis.hasPhones ? 1.0f : 0.0f;
        features[6] = analysis.hasUrls ? 1.0f : 0.0f;
        features[7] = normalizeValue(analysis.pageCount, 0, 100);
        features[8] = normalizeValue(analysis.fileSize, 0, 10000000); // 10MB
        features[9] = analysis.fileType.equals("PDF") ? 1.0f : 0.0f;
        features[10] = analysis.fileType.equals("IMAGE") ? 1.0f : 0.0f;

        // Add pattern-based features
        features[11] = analysis.patterns.contains("INVOICE") ? 1.0f : 0.0f;
        features[12] = analysis.patterns.contains("RECEIPT") ? 1.0f : 0.0f;
        features[13] = analysis.patterns.contains("CONTRACT") ? 1.0f : 0.0f;
        features[14] = analysis.patterns.contains("LETTER") ? 1.0f : 0.0f;

        // Time-based features
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
        String dayOfWeek = dayFormat.format(analysis.createdDate);
        features[15] = dayOfWeek.equals("Monday") || dayOfWeek.equals("Friday") ? 1.0f : 0.0f;

        // Additional features
        features[16] = normalizeValue(analysis.entities.size(), 0, 50);
        features[17] = analysis.fileName.contains("scan") ? 1.0f : 0.0f;
        features[18] = analysis.fileName.contains("doc") ? 1.0f : 0.0f;
        features[19] = analysis.textLength > 1000 ? 1.0f : 0.0f;

        return features;
    }

    /**
     * Normalize value to 0-1 range
     */
    private float normalizeValue(float value, float min, float max) {
        return Math.max(0, Math.min(1, (value - min) / (max - min)));
    }

    // ================================
    // 2. Machine Learning Classification
    // ================================

    /**
     * Initialize ML model
     */
    private void initializeMLModel() {
        executorService.execute(() -> {
            try {
                // TODO: Load TensorFlow Lite model
                // File modelFile = new File(context.getFilesDir(), "document_classifier.tflite");
                // if (modelFile.exists()) {
                //     tfliteInterpreter = new Interpreter(loadModelFile(modelFile));
                //     mlModelLoaded = true;
                //     Log.d(TAG, "TFLite model loaded successfully");
                // } else {
                //     Log.w(TAG, "TFLite model file not found");
                // }

                // For now, use rule-based classification
                classificationModel.initialize();
                mlModelLoaded = true;

                Log.d(TAG, "Classification model initialized");

            } catch (Exception e) {
                Log.e(TAG, "Error initializing ML model", e);
                mlModelLoaded = false;
            }
        });
    }

    /**
     * Classify document using ML model
     */
    public void classifyDocument(Document document, ClassificationCallback callback) {
        analyzeDocument(document, new AnalysisCallback() {
            @Override
            public void onAnalysisComplete(DocumentAnalysis analysis) {
                executorService.execute(() -> {
                    try {
                        // Get classification results
                        ClassificationResult result = classificationModel.classify(analysis);

                        // Enhance with user behavior patterns
                        enhanceWithUserBehavior(result, analysis);

                        // Sort by confidence
                        Collections.sort(result.predictions, (p1, p2) ->
                            Float.compare(p2.confidence, p1.confidence));

                        mainHandler.post(() -> callback.onClassificationComplete(result));

                    } catch (Exception e) {
                        Log.e(TAG, "Error classifying document", e);
                        mainHandler.post(() -> callback.onClassificationError(e));
                    }
                });
            }

            @Override
            public void onAnalysisError(Exception e) {
                callback.onClassificationError(e);
            }
        });
    }

    /**
     * Enhance classification with user behavior
     */
    private void enhanceWithUserBehavior(ClassificationResult result, DocumentAnalysis analysis) {
        Map<Long, Float> behaviorScores = behaviorTracker.getFolderAffinityScores(analysis);

        for (FolderPrediction prediction : result.predictions) {
            Float behaviorScore = behaviorScores.get(prediction.folderId);
            if (behaviorScore != null) {
                // Combine ML score with behavior score
                prediction.confidence = (prediction.confidence * 0.7f) + (behaviorScore * 0.3f);
                prediction.confidence = Math.min(1.0f, prediction.confidence);
            }
        }
    }

    // ================================
    // 3. Keyword Extraction and Pattern Matching
    // ================================

    /**
     * Keyword Extractor
     */
    private class KeywordExtractor {
        private Set<String> stopWords;

        KeywordExtractor() {
            stopWords = new HashSet<>(Arrays.asList(
                "the", "is", "at", "which", "on", "a", "an", "and", "or", "but",
                "in", "with", "to", "for", "of", "as", "by", "from", "this", "that"
            ));
        }

        /**
         * Extract keywords from text
         */
        List<String> extractKeywords(String text) {
            List<String> keywords = new ArrayList<>();

            if (text == null || text.isEmpty()) {
                return keywords;
            }

            // Convert to lowercase and split
            String[] words = text.toLowerCase().split("\\W+");

            // Count word frequencies
            Map<String, Integer> wordFrequency = new HashMap<>();
            for (String word : words) {
                if (word.length() > 3 && !stopWords.contains(word)) {
                    wordFrequency.put(word, wordFrequency.getOrDefault(word, 0) + 1);
                }
            }

            // Sort by frequency
            List<Map.Entry<String, Integer>> sortedWords = new ArrayList<>(wordFrequency.entrySet());
            sortedWords.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

            // Get top keywords
            int maxKeywords = Math.min(20, sortedWords.size());
            for (int i = 0; i < maxKeywords; i++) {
                keywords.add(sortedWords.get(i).getKey());
            }

            return keywords;
        }

        /**
         * Extract named entities (simple implementation)
         */
        List<String> extractEntities(String text) {
            List<String> entities = new ArrayList<>();

            if (text == null || text.isEmpty()) {
                return entities;
            }

            // Extract capitalized words (potential names/entities)
            Pattern pattern = Pattern.compile("\\b[A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*\\b");
            Matcher matcher = pattern.matcher(text);

            Set<String> uniqueEntities = new HashSet<>();
            while (matcher.find()) {
                String entity = matcher.group();
                if (entity.length() > 2) {
                    uniqueEntities.add(entity);
                }
            }

            entities.addAll(uniqueEntities);
            return entities;
        }
    }

    /**
     * Pattern Matcher
     */
    private class PatternMatcher {
        private Map<String, Pattern> patterns;

        PatternMatcher() {
            patterns = new HashMap<>();
            initializePatterns();
        }

        private void initializePatterns() {
            // Invoice patterns
            patterns.put("INVOICE", Pattern.compile(
                "\\b(invoice|bill|billing|payment due|total amount|invoice number|inv\\s*#)\\b",
                Pattern.CASE_INSENSITIVE));

            // Receipt patterns
            patterns.put("RECEIPT", Pattern.compile(
                "\\b(receipt|purchase|transaction|paid|thank you for|store|retail)\\b",
                Pattern.CASE_INSENSITIVE));

            // Contract patterns
            patterns.put("CONTRACT", Pattern.compile(
                "\\b(contract|agreement|terms and conditions|hereby agree|party|parties|signature)\\b",
                Pattern.CASE_INSENSITIVE));

            // Letter patterns
            patterns.put("LETTER", Pattern.compile(
                "\\b(dear|sincerely|regards|yours|respectfully|to whom|cc:|subject:)\\b",
                Pattern.CASE_INSENSITIVE));

            // Medical patterns
            patterns.put("MEDICAL", Pattern.compile(
                "\\b(patient|doctor|hospital|prescription|diagnosis|medical|health|treatment)\\b",
                Pattern.CASE_INSENSITIVE));

            // Financial patterns
            patterns.put("FINANCIAL", Pattern.compile(
                "\\b(bank|account|balance|deposit|withdrawal|statement|credit|debit|transaction)\\b",
                Pattern.CASE_INSENSITIVE));

            // Legal patterns
            patterns.put("LEGAL", Pattern.compile(
                "\\b(court|legal|law|attorney|plaintiff|defendant|case|whereas|wherefore)\\b",
                Pattern.CASE_INSENSITIVE));

            // Tax patterns
            patterns.put("TAX", Pattern.compile(
                "\\b(tax|irs|1040|w2|w-2|deduction|filing|return|refund|taxable)\\b",
                Pattern.CASE_INSENSITIVE));
        }

        /**
         * Find patterns in text
         */
        List<String> findPatterns(String text) {
            List<String> foundPatterns = new ArrayList<>();

            if (text == null || text.isEmpty()) {
                return foundPatterns;
            }

            for (Map.Entry<String, Pattern> entry : patterns.entrySet()) {
                Matcher matcher = entry.getValue().matcher(text);
                if (matcher.find()) {
                    foundPatterns.add(entry.getKey());
                }
            }

            return foundPatterns;
        }
    }

    // ================================
    // 4. User Behavior Learning
    // ================================

    /**
     * User Behavior Tracker
     */
    private class UserBehaviorTracker {
        private SharedPreferences behaviorPrefs;
        private static final String BEHAVIOR_PREFS = "UserBehaviorPrefs";

        UserBehaviorTracker(Context context) {
            behaviorPrefs = context.getSharedPreferences(BEHAVIOR_PREFS, Context.MODE_PRIVATE);
        }

        /**
         * Track user's folder selection
         */
        void trackFolderSelection(long folderId, DocumentAnalysis analysis) {
            executorService.execute(() -> {
                try {
                    // Save training example
                    String key = "folder_" + folderId + "_patterns";
                    Set<String> existingPatterns = behaviorPrefs.getStringSet(key, new HashSet<>());

                    // Add keywords as patterns
                    Set<String> newPatterns = new HashSet<>(existingPatterns);
                    newPatterns.addAll(analysis.keywords);
                    newPatterns.addAll(analysis.patterns);

                    behaviorPrefs.edit().putStringSet(key, newPatterns).apply();

                    // Increment folder usage count
                    String countKey = "folder_" + folderId + "_count";
                    int count = behaviorPrefs.getInt(countKey, 0);
                    behaviorPrefs.edit().putInt(countKey, count + 1).apply();

                    Log.d(TAG, "Tracked folder selection: " + folderId);

                } catch (Exception e) {
                    Log.e(TAG, "Error tracking folder selection", e);
                }
            });
        }

        /**
         * Get folder affinity scores based on user behavior
         */
        Map<Long, Float> getFolderAffinityScores(DocumentAnalysis analysis) {
            Map<Long, Float> scores = new HashMap<>();

            try {
                // Get all tracked folders
                Map<String, ?> allPrefs = behaviorPrefs.getAll();

                for (Map.Entry<String, ?> entry : allPrefs.entrySet()) {
                    if (entry.getKey().endsWith("_patterns")) {
                        // Extract folder ID
                        String folderIdStr = entry.getKey()
                            .replace("folder_", "")
                            .replace("_patterns", "");

                        try {
                            long folderId = Long.parseLong(folderIdStr);

                            // Get folder patterns
                            @SuppressWarnings("unchecked")
                            Set<String> folderPatterns = (Set<String>) entry.getValue();

                            // Calculate similarity score
                            float score = calculateSimilarity(analysis, folderPatterns);

                            // Apply usage frequency boost
                            int usageCount = behaviorPrefs.getInt("folder_" + folderId + "_count", 0);
                            float usageBoost = Math.min(0.2f, usageCount * 0.01f);
                            score += usageBoost;

                            scores.put(folderId, Math.min(1.0f, score));

                        } catch (NumberFormatException e) {
                            // Skip invalid folder ID
                        }
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Error calculating folder affinity", e);
            }

            return scores;
        }

        /**
         * Calculate similarity between document and folder patterns
         */
        private float calculateSimilarity(DocumentAnalysis analysis, Set<String> folderPatterns) {
            if (folderPatterns.isEmpty()) {
                return 0.0f;
            }

            int matchCount = 0;
            int totalPatterns = folderPatterns.size();

            for (String pattern : folderPatterns) {
                if (analysis.keywords.contains(pattern.toLowerCase()) ||
                    analysis.patterns.contains(pattern.toUpperCase())) {
                    matchCount++;
                }
            }

            return (float) matchCount / totalPatterns;
        }
    }

    // ================================
    // 5. Suggest Folders
    // ================================

    /**
     * Suggest appropriate folders for document
     */
    public void suggestFolders(Document document, SuggestionCallback callback) {
        classifyDocument(document, new ClassificationCallback() {
            @Override
            public void onClassificationComplete(ClassificationResult result) {
                executorService.execute(() -> {
                    try {
                        // Load folder information
                        List<FolderSuggestion> suggestions = new ArrayList<>();

                        for (FolderPrediction prediction : result.predictions) {
                            if (prediction.confidence >= LOW_CONFIDENCE_THRESHOLD) {
                                folderRepository.getFolderById(prediction.folderId,
                                    new FolderRepository.FolderCallback() {
                                        @Override
                                        public void onSuccess(Folder folder) {
                                            FolderSuggestion suggestion = new FolderSuggestion();
                                            suggestion.folder = folder;
                                            suggestion.confidence = prediction.confidence;
                                            suggestion.reason = prediction.reason;
                                            suggestion.confidenceLevel = getConfidenceLevel(prediction.confidence);
                                            suggestions.add(suggestion);

                                            // Return when all loaded
                                            if (suggestions.size() == result.predictions.size()) {
                                                mainHandler.post(() -> callback.onSuggestionsReady(suggestions));
                                            }
                                        }

                                        @Override
                                        public void onError(Exception e) {
                                            Log.e(TAG, "Error loading folder", e);
                                        }
                                    });
                            }
                        }

                        // Handle case when no suggestions meet threshold
                        if (result.predictions.isEmpty() ||
                            result.predictions.get(0).confidence < LOW_CONFIDENCE_THRESHOLD) {
                            mainHandler.post(() -> callback.onSuggestionsReady(new ArrayList<>()));
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Error generating suggestions", e);
                        mainHandler.post(() -> callback.onSuggestionsError(e));
                    }
                });
            }

            @Override
            public void onClassificationError(Exception e) {
                callback.onSuggestionsError(e);
            }
        });
    }

    // ================================
    // 6. Confidence Scoring
    // ================================

    /**
     * Get confidence level from score
     */
    private ConfidenceLevel getConfidenceLevel(float confidence) {
        if (confidence >= HIGH_CONFIDENCE_THRESHOLD) {
            return ConfidenceLevel.HIGH;
        } else if (confidence >= MEDIUM_CONFIDENCE_THRESHOLD) {
            return ConfidenceLevel.MEDIUM;
        } else if (confidence >= LOW_CONFIDENCE_THRESHOLD) {
            return ConfidenceLevel.LOW;
        } else {
            return ConfidenceLevel.VERY_LOW;
        }
    }

    /**
     * Calculate confidence score
     */
    public float calculateConfidenceScore(DocumentAnalysis analysis, Folder folder) {
        float score = 0.0f;

        // Keyword matching (40%)
        float keywordScore = calculateKeywordMatch(analysis, folder);
        score += keywordScore * 0.4f;

        // Pattern matching (30%)
        float patternScore = calculatePatternMatch(analysis, folder);
        score += patternScore * 0.3f;

        // User behavior (20%)
        Map<Long, Float> behaviorScores = behaviorTracker.getFolderAffinityScores(analysis);
        Float behaviorScore = behaviorScores.get(folder.getFolderId());
        if (behaviorScore != null) {
            score += behaviorScore * 0.2f;
        }

        // Historical accuracy (10%)
        float historicalScore = getHistoricalAccuracy(folder.getFolderId());
        score += historicalScore * 0.1f;

        return Math.min(1.0f, score);
    }

    /**
     * Calculate keyword match score
     */
    private float calculateKeywordMatch(DocumentAnalysis analysis, Folder folder) {
        // Get folder patterns from behavior tracker
        String key = "folder_" + folder.getFolderId() + "_patterns";
        Set<String> folderPatterns = behaviorTracker.behaviorPrefs.getStringSet(key, new HashSet<>());

        if (folderPatterns.isEmpty()) {
            // Use folder name as fallback
            String folderName = folder.getFolderName().toLowerCase();
            for (String keyword : analysis.keywords) {
                if (folderName.contains(keyword)) {
                    return 0.5f;
                }
            }
            return 0.0f;
        }

        return behaviorTracker.calculateSimilarity(analysis, folderPatterns);
    }

    /**
     * Calculate pattern match score
     */
    private float calculatePatternMatch(DocumentAnalysis analysis, Folder folder) {
        String folderName = folder.getFolderName().toLowerCase();

        for (String pattern : analysis.patterns) {
            if (folderName.contains(pattern.toLowerCase())) {
                return 1.0f;
            }
        }

        return 0.0f;
    }

    /**
     * Get historical accuracy for folder
     */
    private float getHistoricalAccuracy(long folderId) {
        String key = "folder_" + folderId + "_accuracy";
        return prefs.getFloat(key, 0.5f); // Default 50%
    }

    // ================================
    // 7. User Feedback Integration
    // ================================

    /**
     * Record positive feedback (user accepted suggestion)
     */
    public void recordPositiveFeedback(Document document, Folder folder, float confidence) {
        executorService.execute(() -> {
            try {
                // Analyze document again
                analyzeDocument(document, new AnalysisCallback() {
                    @Override
                    public void onAnalysisComplete(DocumentAnalysis analysis) {
                        // Track behavior
                        behaviorTracker.trackFolderSelection(folder.getFolderId(), analysis);

                        // Update historical accuracy
                        updateHistoricalAccuracy(folder.getFolderId(), true);

                        // Add to training data
                        addTrainingExample(folder.getFolderId(), analysis, true);

                        // Increment training count
                        int count = prefs.getInt(KEY_TRAINING_COUNT, 0);
                        prefs.edit().putInt(KEY_TRAINING_COUNT, count + 1).apply();

                        // Retrain model if enough examples
                        if (count % 50 == 0) {
                            retrainModel();
                        }

                        Log.d(TAG, "Positive feedback recorded for folder: " + folder.getFolderName());
                    }

                    @Override
                    public void onAnalysisError(Exception e) {
                        Log.e(TAG, "Error recording positive feedback", e);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error recording positive feedback", e);
            }
        });
    }

    /**
     * Record negative feedback (user rejected suggestion)
     */
    public void recordNegativeFeedback(Document document, Folder suggestedFolder,
                                      Folder actualFolder) {
        executorService.execute(() -> {
            try {
                // Update historical accuracy
                updateHistoricalAccuracy(suggestedFolder.getFolderId(), false);

                // Track correct folder
                analyzeDocument(document, new AnalysisCallback() {
                    @Override
                    public void onAnalysisComplete(DocumentAnalysis analysis) {
                        behaviorTracker.trackFolderSelection(actualFolder.getFolderId(), analysis);
                        addTrainingExample(actualFolder.getFolderId(), analysis, true);

                        Log.d(TAG, "Negative feedback recorded. Suggested: " +
                            suggestedFolder.getFolderName() + ", Actual: " +
                            actualFolder.getFolderName());
                    }

                    @Override
                    public void onAnalysisError(Exception e) {
                        Log.e(TAG, "Error recording negative feedback", e);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error recording negative feedback", e);
            }
        });
    }

    /**
     * Update historical accuracy
     */
    private void updateHistoricalAccuracy(long folderId, boolean correct) {
        String key = "folder_" + folderId + "_accuracy";
        String countKey = "folder_" + folderId + "_total";

        float currentAccuracy = prefs.getFloat(key, 0.5f);
        int totalCount = prefs.getInt(countKey, 0);

        // Update with moving average
        float newAccuracy = (currentAccuracy * totalCount + (correct ? 1.0f : 0.0f)) / (totalCount + 1);

        prefs.edit()
            .putFloat(key, newAccuracy)
            .putInt(countKey, totalCount + 1)
            .apply();
    }

    /**
     * Add training example
     */
    private void addTrainingExample(long folderId, DocumentAnalysis analysis, boolean positive) {
        if (!folderTrainingData.containsKey(folderId)) {
            folderTrainingData.put(folderId, new ArrayList<>());
        }

        TrainingExample example = new TrainingExample();
        example.analysis = analysis;
        example.folderId = folderId;
        example.positive = positive;
        example.timestamp = System.currentTimeMillis();

        folderTrainingData.get(folderId).add(example);

        // Keep only recent examples (max 100 per folder)
        List<TrainingExample> examples = folderTrainingData.get(folderId);
        if (examples.size() > 100) {
            examples.remove(0);
        }
    }

    /**
     * Retrain model with new data
     */
    private void retrainModel() {
        executorService.execute(() -> {
            try {
                Log.d(TAG, "Retraining model with " + folderTrainingData.size() + " folders");

                // TODO: Implement actual TFLite model retraining
                // For now, just update rule weights
                classificationModel.updateWeights(folderTrainingData);

                // Update model version
                int version = prefs.getInt(KEY_MODEL_VERSION, 0);
                prefs.edit().putInt(KEY_MODEL_VERSION, version + 1).apply();

                Log.d(TAG, "Model retrained. New version: " + (version + 1));

            } catch (Exception e) {
                Log.e(TAG, "Error retraining model", e);
            }
        });
    }

    // ================================
    // ML Classification Model
    // ================================

    /**
     * Machine Learning Classification Model
     */
    private class MLClassificationModel {
        private Map<String, Float> ruleWeights = new HashMap<>();

        void initialize() {
            // Initialize default weights
            ruleWeights.put("keyword_match", 0.4f);
            ruleWeights.put("pattern_match", 0.3f);
            ruleWeights.put("entity_match", 0.15f);
            ruleWeights.put("file_type_match", 0.1f);
            ruleWeights.put("name_match", 0.05f);
        }

        /**
         * Classify document
         */
        ClassificationResult classify(DocumentAnalysis analysis) {
            ClassificationResult result = new ClassificationResult();
            result.documentId = analysis.documentId;
            result.predictions = new ArrayList<>();

            // Get all folders
            List<Folder> folders = getFoldersSync();

            for (Folder folder : folders) {
                FolderPrediction prediction = new FolderPrediction();
                prediction.folderId = folder.getFolderId();
                prediction.folderName = folder.getFolderName();
                prediction.confidence = calculateConfidenceScore(analysis, folder);
                prediction.reason = generateReason(analysis, folder);

                if (prediction.confidence >= LOW_CONFIDENCE_THRESHOLD) {
                    result.predictions.add(prediction);
                }
            }

            return result;
        }

        /**
         * Update model weights based on training data
         */
        void updateWeights(Map<Long, List<TrainingExample>> trainingData) {
            // Simple weight adjustment based on feedback
            int totalPositive = 0;
            int totalNegative = 0;

            for (List<TrainingExample> examples : trainingData.values()) {
                for (TrainingExample example : examples) {
                    if (example.positive) {
                        totalPositive++;
                    } else {
                        totalNegative++;
                    }
                }
            }

            // Adjust weights
            if (totalPositive + totalNegative > 0) {
                float accuracy = (float) totalPositive / (totalPositive + totalNegative);

                // Increase weight if accuracy is high
                if (accuracy > 0.7f) {
                    ruleWeights.put("keyword_match", ruleWeights.get("keyword_match") * 1.1f);
                } else if (accuracy < 0.5f) {
                    ruleWeights.put("keyword_match", ruleWeights.get("keyword_match") * 0.9f);
                }
            }
        }

        /**
         * Generate reason for prediction
         */
        private String generateReason(DocumentAnalysis analysis, Folder folder) {
            StringBuilder reason = new StringBuilder();

            // Check keyword matches
            String folderName = folder.getFolderName().toLowerCase();
            int keywordMatches = 0;
            for (String keyword : analysis.keywords) {
                if (folderName.contains(keyword)) {
                    keywordMatches++;
                }
            }

            if (keywordMatches > 0) {
                reason.append("Keywords matched (").append(keywordMatches).append("). ");
            }

            // Check pattern matches
            for (String pattern : analysis.patterns) {
                if (folderName.contains(pattern.toLowerCase())) {
                    reason.append("Pattern '").append(pattern).append("' detected. ");
                }
            }

            // Check user behavior
            Map<Long, Float> behaviorScores = behaviorTracker.getFolderAffinityScores(analysis);
            Float behaviorScore = behaviorScores.get(folder.getFolderId());
            if (behaviorScore != null && behaviorScore > 0.5f) {
                reason.append("Similar to previously filed documents. ");
            }

            if (reason.length() == 0) {
                reason.append("Based on document characteristics.");
            }

            return reason.toString().trim();
        }
    }

    // ================================
    // Helper Methods
    // ================================

    /**
     * Get folders synchronously (for internal use)
     */
    private List<Folder> getFoldersSync() {
        // TODO: Implement synchronous folder loading
        // For now, return empty list
        return new ArrayList<>();
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
     * Check if text contains numbers
     */
    private boolean containsNumbers(String text) {
        return text != null && text.matches(".*\\d.*");
    }

    /**
     * Check if text contains dates
     */
    private boolean containsDates(String text) {
        if (text == null) return false;
        Pattern datePattern = Pattern.compile(
            "\\b\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4}\\b|\\b\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}\\b");
        return datePattern.matcher(text).find();
    }

    /**
     * Check if text contains emails
     */
    private boolean containsEmails(String text) {
        if (text == null) return false;
        Pattern emailPattern = Pattern.compile(
            "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
        return emailPattern.matcher(text).find();
    }

    /**
     * Check if text contains phone numbers
     */
    private boolean containsPhones(String text) {
        if (text == null) return false;
        Pattern phonePattern = Pattern.compile(
            "\\b\\d{3}[-.]?\\d{3}[-.]?\\d{4}\\b|\\(\\d{3}\\)\\s*\\d{3}[-.]?\\d{4}\\b");
        return phonePattern.matcher(text).find();
    }

    /**
     * Check if text contains URLs
     */
    private boolean containsUrls(String text) {
        if (text == null) return false;
        Pattern urlPattern = Pattern.compile(
            "\\bhttps?://[\\w.-]+\\.[a-zA-Z]{2,}[^\\s]*\\b");
        return urlPattern.matcher(text).find();
    }

    /**
     * Export training data for external ML training
     */
    public void exportTrainingData(ExportCallback callback) {
        executorService.execute(() -> {
            try {
                JSONObject exportData = new JSONObject();
                JSONArray examples = new JSONArray();

                for (Map.Entry<Long, List<TrainingExample>> entry : folderTrainingData.entrySet()) {
                    for (TrainingExample example : entry.getValue()) {
                        JSONObject exampleJson = new JSONObject();
                        exampleJson.put("folder_id", example.folderId);
                        exampleJson.put("positive", example.positive);
                        exampleJson.put("timestamp", example.timestamp);

                        // Add features
                        JSONArray keywords = new JSONArray(example.analysis.keywords);
                        exampleJson.put("keywords", keywords);

                        JSONArray patterns = new JSONArray(example.analysis.patterns);
                        exampleJson.put("patterns", patterns);

                        examples.put(exampleJson);
                    }
                }

                exportData.put("examples", examples);
                exportData.put("version", prefs.getInt(KEY_MODEL_VERSION, 0));
                exportData.put("training_count", prefs.getInt(KEY_TRAINING_COUNT, 0));

                String jsonString = exportData.toString(2);
                mainHandler.post(() -> callback.onExportComplete(jsonString));

            } catch (JSONException e) {
                Log.e(TAG, "Error exporting training data", e);
                mainHandler.post(() -> callback.onExportError(e));
            }
        });
    }

    /**
     * Cleanup resources
     */
    public void cleanup() {
        executorService.shutdown();
        // if (tfliteInterpreter != null) {
        //     tfliteInterpreter.close();
        // }
        Log.d(TAG, "SmartCategorizationEngine cleaned up");
    }

    // ================================
    // Data Classes
    // ================================

    /**
     * Document Analysis result
     */
    public static class DocumentAnalysis {
        public long documentId;
        public String documentName;
        public String fileName;
        public String fileType;
        public List<String> keywords = new ArrayList<>();
        public List<String> entities = new ArrayList<>();
        public List<String> patterns = new ArrayList<>();
        public int textLength;
        public int wordCount;
        public int pageCount;
        public long fileSize;
        public Date createdDate;
        public boolean hasNumbers;
        public boolean hasDates;
        public boolean hasEmails;
        public boolean hasPhones;
        public boolean hasUrls;
        public float[] mlFeatures;
    }

    /**
     * Classification Result
     */
    public static class ClassificationResult {
        public long documentId;
        public List<FolderPrediction> predictions = new ArrayList<>();
    }

    /**
     * Folder Prediction
     */
    public static class FolderPrediction {
        public long folderId;
        public String folderName;
        public float confidence;
        public String reason;
    }

    /**
     * Folder Suggestion
     */
    public static class FolderSuggestion {
        public Folder folder;
        public float confidence;
        public String reason;
        public ConfidenceLevel confidenceLevel;
    }

    /**
     * Training Example
     */
    private static class TrainingExample {
        DocumentAnalysis analysis;
        long folderId;
        boolean positive;
        long timestamp;
    }

    /**
     * Confidence Level
     */
    public enum ConfidenceLevel {
        HIGH("High Confidence", "#4CAF50"),
        MEDIUM("Medium Confidence", "#FF9800"),
        LOW("Low Confidence", "#FFC107"),
        VERY_LOW("Very Low Confidence", "#9E9E9E");

        public final String displayName;
        public final String colorHex;

        ConfidenceLevel(String displayName, String colorHex) {
            this.displayName = displayName;
            this.colorHex = colorHex;
        }
    }

    // ================================
    // Callbacks
    // ================================

    public interface AnalysisCallback {
        void onAnalysisComplete(DocumentAnalysis analysis);
        void onAnalysisError(Exception e);
    }

    public interface ClassificationCallback {
        void onClassificationComplete(ClassificationResult result);
        void onClassificationError(Exception e);
    }

    public interface SuggestionCallback {
        void onSuggestionsReady(List<FolderSuggestion> suggestions);
        void onSuggestionsError(Exception e);
    }

    public interface ExportCallback {
        void onExportComplete(String jsonData);
        void onExportError(Exception e);
    }
}


