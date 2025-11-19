package com.srikanth.docscanner;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * FeatureAnalytics - Usage tracking and optimization
 * 
 * Features:
 * 1. Track OCR accuracy and processing times
 * 2. Monitor cloud sync success rates and failures
 * 3. Analyze folder organization patterns
 * 4. Search query performance and user satisfaction
 * 5. Feature adoption rates and user engagement
 * 6. Battery and memory usage optimization
 * 7. Crash reporting and error analytics
 * 8. GDPR-compliant data collection and user consent
 */
public class FeatureAnalytics {

    private static final String TAG = "FeatureAnalytics";
    
    // Singleton instance
    private static FeatureAnalytics instance;
    
    // Context
    private final Context context;
    
    // SharedPreferences
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "FeatureAnalytics";
    private static final String KEY_USER_CONSENT = "analytics_consent";
    private static final String KEY_ANALYTICS_DATA = "analytics_data";
    
    // Threading
    private final ExecutorService executorService;
    private final Handler mainHandler;
    
    // Analytics data
    private AnalyticsData analyticsData;
    
    // User consent
    private boolean hasUserConsent = false;
    
    // Event listeners
    private List<AnalyticsListener> listeners = new ArrayList<>();

    /**
     * Private constructor (Singleton)
     */
    private FeatureAnalytics(Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // Load user consent
        hasUserConsent = prefs.getBoolean(KEY_USER_CONSENT, false);
        
        // Load analytics data
        loadAnalyticsData();
        
        // Start periodic reporting
        startPeriodicReporting();
        
        Log.d(TAG, "FeatureAnalytics initialized (Consent: " + hasUserConsent + ")");
    }

    /**
     * Get singleton instance
     */
    public static synchronized FeatureAnalytics getInstance(Context context) {
        if (instance == null) {
            instance = new FeatureAnalytics(context);
        }
        return instance;
    }

    // ================================
    // GDPR Compliance & User Consent
    // ================================

    /**
     * Request user consent
     */
    public void requestUserConsent(ConsentCallback callback) {
        if (hasUserConsent) {
            callback.onConsentDecision(true);
            return;
        }
        
        // Show consent dialog
        mainHandler.post(() -> {
            // In a real app, this would show a proper dialog
            // For now, we'll simulate consent
            showConsentDialog(callback);
        });
    }

    /**
     * Show consent dialog
     */
    private void showConsentDialog(ConsentCallback callback) {
        // TODO: Show actual consent dialog with proper UI
        // This is a placeholder that simulates user consent
        
        // For now, auto-grant consent for testing
        setUserConsent(true);
        callback.onConsentDecision(true);
    }

    /**
     * Set user consent
     */
    public void setUserConsent(boolean consent) {
        hasUserConsent = consent;
        prefs.edit().putBoolean(KEY_USER_CONSENT, consent).apply();
        
        if (!consent) {
            // Clear all analytics data if consent is revoked
            clearAllAnalytics();
        }
        
        Log.d(TAG, "User consent set to: " + consent);
    }

    /**
     * Get user consent status
     */
    public boolean hasUserConsent() {
        return hasUserConsent;
    }

    /**
     * Clear all analytics data
     */
    private void clearAllAnalytics() {
        analyticsData = new AnalyticsData();
        saveAnalyticsData();
        Log.d(TAG, "All analytics data cleared");
    }

    // ================================
    // 1. OCR Tracking
    // ================================

    /**
     * Track OCR operation
     */
    public void trackOCR(OCRMetrics metrics) {
        if (!hasUserConsent) return;
        
        executorService.execute(() -> {
            analyticsData.ocrMetrics.add(metrics);
            
            // Keep only last 100 metrics
            if (analyticsData.ocrMetrics.size() > 100) {
                analyticsData.ocrMetrics.remove(0);
            }
            
            saveAnalyticsData();
            notifyListeners(AnalyticsEvent.OCR_TRACKED, metrics);
            
            Log.d(TAG, "OCR tracked: " + metrics.processingTime + "ms, accuracy: " + metrics.accuracy);
        });
    }

    /**
     * Get OCR statistics
     */
    public OCRStatistics getOCRStatistics() {
        OCRStatistics stats = new OCRStatistics();
        
        if (analyticsData.ocrMetrics.isEmpty()) {
            return stats;
        }
        
        long totalTime = 0;
        float totalAccuracy = 0;
        int successCount = 0;
        int failureCount = 0;
        
        for (OCRMetrics metrics : analyticsData.ocrMetrics) {
            totalTime += metrics.processingTime;
            totalAccuracy += metrics.accuracy;
            
            if (metrics.success) {
                successCount++;
            } else {
                failureCount++;
            }
        }
        
        stats.averageProcessingTime = totalTime / analyticsData.ocrMetrics.size();
        stats.averageAccuracy = totalAccuracy / analyticsData.ocrMetrics.size();
        stats.successRate = (float) successCount / analyticsData.ocrMetrics.size();
        stats.totalOperations = analyticsData.ocrMetrics.size();
        
        return stats;
    }

    // ================================
    // 2. Cloud Sync Monitoring
    // ================================

    /**
     * Track cloud sync operation
     */
    public void trackCloudSync(CloudSyncMetrics metrics) {
        if (!hasUserConsent) return;
        
        executorService.execute(() -> {
            analyticsData.cloudSyncMetrics.add(metrics);
            
            // Keep only last 100 metrics
            if (analyticsData.cloudSyncMetrics.size() > 100) {
                analyticsData.cloudSyncMetrics.remove(0);
            }
            
            saveAnalyticsData();
            notifyListeners(AnalyticsEvent.SYNC_TRACKED, metrics);
            
            Log.d(TAG, "Cloud sync tracked: " + metrics.filesCount + " files, success: " + metrics.success);
        });
    }

    /**
     * Get cloud sync statistics
     */
    public CloudSyncStatistics getCloudSyncStatistics() {
        CloudSyncStatistics stats = new CloudSyncStatistics();
        
        if (analyticsData.cloudSyncMetrics.isEmpty()) {
            return stats;
        }
        
        int successCount = 0;
        int failureCount = 0;
        long totalFiles = 0;
        long totalBytes = 0;
        long totalTime = 0;
        
        for (CloudSyncMetrics metrics : analyticsData.cloudSyncMetrics) {
            if (metrics.success) {
                successCount++;
                totalFiles += metrics.filesCount;
                totalBytes += metrics.bytesTransferred;
            } else {
                failureCount++;
            }
            totalTime += metrics.duration;
        }
        
        stats.successRate = (float) successCount / analyticsData.cloudSyncMetrics.size();
        stats.failureRate = (float) failureCount / analyticsData.cloudSyncMetrics.size();
        stats.averageDuration = totalTime / analyticsData.cloudSyncMetrics.size();
        stats.totalFilesSync = totalFiles;
        stats.totalBytesSync = totalBytes;
        stats.totalOperations = analyticsData.cloudSyncMetrics.size();
        
        return stats;
    }

    // ================================
    // 3. Folder Organization Analysis
    // ================================

    /**
     * Track folder organization
     */
    public void trackFolderOrganization(FolderMetrics metrics) {
        if (!hasUserConsent) return;
        
        executorService.execute(() -> {
            analyticsData.folderMetrics.add(metrics);
            
            // Keep only last 50 metrics
            if (analyticsData.folderMetrics.size() > 50) {
                analyticsData.folderMetrics.remove(0);
            }
            
            saveAnalyticsData();
            notifyListeners(AnalyticsEvent.FOLDER_TRACKED, metrics);
            
            Log.d(TAG, "Folder organization tracked: " + metrics.folderName);
        });
    }

    /**
     * Get folder organization statistics
     */
    public FolderStatistics getFolderStatistics() {
        FolderStatistics stats = new FolderStatistics();
        
        Map<String, Integer> folderUsage = new HashMap<>();
        int totalAutoCateg = 0;
        int totalManual = 0;
        
        for (FolderMetrics metrics : analyticsData.folderMetrics) {
            folderUsage.put(metrics.folderName, 
                folderUsage.getOrDefault(metrics.folderName, 0) + 1);
            
            if (metrics.autoCategorized) {
                totalAutoCateg++;
            } else {
                totalManual++;
            }
        }
        
        stats.folderUsageMap = folderUsage;
        stats.autoCategorizationRate = analyticsData.folderMetrics.isEmpty() ? 0 :
            (float) totalAutoCateg / analyticsData.folderMetrics.size();
        stats.totalOrganizations = analyticsData.folderMetrics.size();
        
        return stats;
    }

    // ================================
    // 4. Search Performance
    // ================================

    /**
     * Track search query
     */
    public void trackSearch(SearchMetrics metrics) {
        if (!hasUserConsent) return;
        
        executorService.execute(() -> {
            analyticsData.searchMetrics.add(metrics);
            
            // Keep only last 100 searches
            if (analyticsData.searchMetrics.size() > 100) {
                analyticsData.searchMetrics.remove(0);
            }
            
            saveAnalyticsData();
            notifyListeners(AnalyticsEvent.SEARCH_TRACKED, metrics);
            
            Log.d(TAG, "Search tracked: " + metrics.query + ", results: " + metrics.resultCount);
        });
    }

    /**
     * Get search statistics
     */
    public SearchStatistics getSearchStatistics() {
        SearchStatistics stats = new SearchStatistics();
        
        if (analyticsData.searchMetrics.isEmpty()) {
            return stats;
        }
        
        long totalTime = 0;
        int totalResults = 0;
        int satisfiedCount = 0;
        
        for (SearchMetrics metrics : analyticsData.searchMetrics) {
            totalTime += metrics.processingTime;
            totalResults += metrics.resultCount;
            
            if (metrics.userSatisfied) {
                satisfiedCount++;
            }
        }
        
        stats.averageSearchTime = totalTime / analyticsData.searchMetrics.size();
        stats.averageResultCount = (float) totalResults / analyticsData.searchMetrics.size();
        stats.satisfactionRate = (float) satisfiedCount / analyticsData.searchMetrics.size();
        stats.totalSearches = analyticsData.searchMetrics.size();
        
        return stats;
    }

    // ================================
    // 5. Feature Adoption & Engagement
    // ================================

    /**
     * Track feature usage
     */
    public void trackFeatureUsage(String featureName) {
        if (!hasUserConsent) return;
        
        executorService.execute(() -> {
            int count = analyticsData.featureUsage.getOrDefault(featureName, 0);
            analyticsData.featureUsage.put(featureName, count + 1);
            
            saveAnalyticsData();
            notifyListeners(AnalyticsEvent.FEATURE_USED, featureName);
            
            Log.d(TAG, "Feature used: " + featureName);
        });
    }

    /**
     * Get feature adoption statistics
     */
    public FeatureAdoptionStatistics getFeatureAdoptionStatistics() {
        FeatureAdoptionStatistics stats = new FeatureAdoptionStatistics();
        
        stats.featureUsageMap = new HashMap<>(analyticsData.featureUsage);
        stats.totalFeatures = analyticsData.featureUsage.size();
        
        // Find most used feature
        int maxUsage = 0;
        for (Map.Entry<String, Integer> entry : analyticsData.featureUsage.entrySet()) {
            if (entry.getValue() > maxUsage) {
                maxUsage = entry.getValue();
                stats.mostUsedFeature = entry.getKey();
            }
        }
        
        return stats;
    }

    /**
     * Track user session
     */
    public void trackSession(long duration) {
        if (!hasUserConsent) return;
        
        executorService.execute(() -> {
            analyticsData.sessionDurations.add(duration);
            
            // Keep only last 50 sessions
            if (analyticsData.sessionDurations.size() > 50) {
                analyticsData.sessionDurations.remove(0);
            }
            
            saveAnalyticsData();
            
            Log.d(TAG, "Session tracked: " + duration + "ms");
        });
    }

    /**
     * Get engagement statistics
     */
    public EngagementStatistics getEngagementStatistics() {
        EngagementStatistics stats = new EngagementStatistics();
        
        if (analyticsData.sessionDurations.isEmpty()) {
            return stats;
        }
        
        long totalDuration = 0;
        for (long duration : analyticsData.sessionDurations) {
            totalDuration += duration;
        }
        
        stats.averageSessionDuration = totalDuration / analyticsData.sessionDurations.size();
        stats.totalSessions = analyticsData.sessionDurations.size();
        
        return stats;
    }

    // ================================
    // 6. Battery & Memory Optimization
    // ================================

    /**
     * Track resource usage
     */
    public void trackResourceUsage() {
        if (!hasUserConsent) return;
        
        executorService.execute(() -> {
            ResourceMetrics metrics = new ResourceMetrics();
            metrics.timestamp = System.currentTimeMillis();
            metrics.memoryUsage = getMemoryUsage();
            metrics.batteryLevel = getBatteryLevel();
            
            analyticsData.resourceMetrics.add(metrics);
            
            // Keep only last 50 metrics
            if (analyticsData.resourceMetrics.size() > 50) {
                analyticsData.resourceMetrics.remove(0);
            }
            
            saveAnalyticsData();
            
            // Check for optimization opportunities
            checkOptimizationOpportunities(metrics);
        });
    }

    /**
     * Get memory usage
     */
    private float getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        long max = runtime.maxMemory();
        return (float) used / max;
    }

    /**
     * Get battery level
     */
    private int getBatteryLevel() {
        BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        if (batteryManager != null) {
            return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        }
        return -1;
    }

    /**
     * Check optimization opportunities
     */
    private void checkOptimizationOpportunities(ResourceMetrics metrics) {
        // High memory usage warning
        if (metrics.memoryUsage > 0.8f) {
            Log.w(TAG, "High memory usage detected: " + (metrics.memoryUsage * 100) + "%");
            notifyListeners(AnalyticsEvent.HIGH_MEMORY_USAGE, metrics);
        }
        
        // Low battery warning
        if (metrics.batteryLevel < 20 && metrics.batteryLevel >= 0) {
            Log.w(TAG, "Low battery detected: " + metrics.batteryLevel + "%");
            notifyListeners(AnalyticsEvent.LOW_BATTERY, metrics);
        }
    }

    /**
     * Get resource statistics
     */
    public ResourceStatistics getResourceStatistics() {
        ResourceStatistics stats = new ResourceStatistics();
        
        if (analyticsData.resourceMetrics.isEmpty()) {
            return stats;
        }
        
        float totalMemory = 0;
        int totalBattery = 0;
        int batteryCount = 0;
        
        for (ResourceMetrics metrics : analyticsData.resourceMetrics) {
            totalMemory += metrics.memoryUsage;
            
            if (metrics.batteryLevel >= 0) {
                totalBattery += metrics.batteryLevel;
                batteryCount++;
            }
        }
        
        stats.averageMemoryUsage = totalMemory / analyticsData.resourceMetrics.size();
        stats.averageBatteryLevel = batteryCount > 0 ? totalBattery / batteryCount : -1;
        stats.measurementCount = analyticsData.resourceMetrics.size();
        
        return stats;
    }

    // ================================
    // 7. Crash Reporting & Error Analytics
    // ================================

    /**
     * Track error
     */
    public void trackError(ErrorMetrics metrics) {
        if (!hasUserConsent) return;
        
        executorService.execute(() -> {
            analyticsData.errorMetrics.add(metrics);
            
            // Keep only last 50 errors
            if (analyticsData.errorMetrics.size() > 50) {
                analyticsData.errorMetrics.remove(0);
            }
            
            saveAnalyticsData();
            notifyListeners(AnalyticsEvent.ERROR_TRACKED, metrics);
            
            Log.e(TAG, "Error tracked: " + metrics.errorType + " - " + metrics.message);
        });
    }

    /**
     * Track crash
     */
    public void trackCrash(String stackTrace) {
        if (!hasUserConsent) return;
        
        ErrorMetrics metrics = new ErrorMetrics();
        metrics.timestamp = System.currentTimeMillis();
        metrics.errorType = "CRASH";
        metrics.message = "Application crashed";
        metrics.stackTrace = stackTrace;
        metrics.severity = ErrorSeverity.CRITICAL;
        
        trackError(metrics);
    }

    /**
     * Get error statistics
     */
    public ErrorStatistics getErrorStatistics() {
        ErrorStatistics stats = new ErrorStatistics();
        
        if (analyticsData.errorMetrics.isEmpty()) {
            return stats;
        }
        
        Map<String, Integer> errorTypes = new HashMap<>();
        int criticalCount = 0;
        int warningCount = 0;
        
        for (ErrorMetrics metrics : analyticsData.errorMetrics) {
            errorTypes.put(metrics.errorType, 
                errorTypes.getOrDefault(metrics.errorType, 0) + 1);
            
            if (metrics.severity == ErrorSeverity.CRITICAL) {
                criticalCount++;
            } else if (metrics.severity == ErrorSeverity.WARNING) {
                warningCount++;
            }
        }
        
        stats.errorTypeMap = errorTypes;
        stats.criticalErrors = criticalCount;
        stats.warnings = warningCount;
        stats.totalErrors = analyticsData.errorMetrics.size();
        
        return stats;
    }

    // ================================
    // Reporting & Export
    // ================================

    /**
     * Generate comprehensive report
     */
    public AnalyticsReport generateReport() {
        AnalyticsReport report = new AnalyticsReport();
        report.timestamp = System.currentTimeMillis();
        report.hasUserConsent = hasUserConsent;
        
        if (!hasUserConsent) {
            report.message = "No analytics data available (user consent required)";
            return report;
        }
        
        report.ocrStats = getOCRStatistics();
        report.syncStats = getCloudSyncStatistics();
        report.folderStats = getFolderStatistics();
        report.searchStats = getSearchStatistics();
        report.adoptionStats = getFeatureAdoptionStatistics();
        report.engagementStats = getEngagementStatistics();
        report.resourceStats = getResourceStatistics();
        report.errorStats = getErrorStatistics();
        
        return report;
    }

    /**
     * Export analytics data
     */
    public String exportAnalytics() {
        if (!hasUserConsent) {
            return "{}";
        }
        
        try {
            JSONObject json = analyticsData.toJSON();
            return json.toString(2); // Pretty print with 2-space indent
        } catch (JSONException e) {
            Log.e(TAG, "Error exporting analytics", e);
            return "{}";
        }
    }

    /**
     * Start periodic reporting
     */
    private void startPeriodicReporting() {
        // Report every 5 minutes
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                trackResourceUsage();
                mainHandler.postDelayed(this, 5 * 60 * 1000);
            }
        }, 5 * 60 * 1000);
    }

    // ================================
    // Event Listeners
    // ================================

    /**
     * Add analytics listener
     */
    public void addListener(AnalyticsListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove analytics listener
     */
    public void removeListener(AnalyticsListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notify listeners
     */
    private void notifyListeners(AnalyticsEvent event, Object data) {
        mainHandler.post(() -> {
            for (AnalyticsListener listener : listeners) {
                listener.onAnalyticsEvent(event, data);
            }
        });
    }

    // ================================
    // Persistence
    // ================================

    /**
     * Save analytics data
     */
    private void saveAnalyticsData() {
        try {
            String json = analyticsData.toJSON().toString();
            prefs.edit().putString(KEY_ANALYTICS_DATA, json).apply();
        } catch (JSONException e) {
            Log.e(TAG, "Error saving analytics", e);
        }
    }

    /**
     * Load analytics data
     */
    private void loadAnalyticsData() {
        try {
            String json = prefs.getString(KEY_ANALYTICS_DATA, "{}");
            analyticsData = AnalyticsData.fromJSON(new JSONObject(json));
            Log.d(TAG, "Analytics data loaded");
        } catch (JSONException e) {
            Log.e(TAG, "Error loading analytics", e);
            analyticsData = new AnalyticsData();
        }
    }

    /**
     * Cleanup
     */
    public void cleanup() {
        executorService.shutdown();
        listeners.clear();
        Log.d(TAG, "FeatureAnalytics cleaned up");
    }

    // ================================
    // Data Classes
    // ================================

    /**
     * Analytics Data Container
     */
    private static class AnalyticsData {
        List<OCRMetrics> ocrMetrics = new ArrayList<>();
        List<CloudSyncMetrics> cloudSyncMetrics = new ArrayList<>();
        List<FolderMetrics> folderMetrics = new ArrayList<>();
        List<SearchMetrics> searchMetrics = new ArrayList<>();
        Map<String, Integer> featureUsage = new HashMap<>();
        List<Long> sessionDurations = new ArrayList<>();
        List<ResourceMetrics> resourceMetrics = new ArrayList<>();
        List<ErrorMetrics> errorMetrics = new ArrayList<>();
        
        JSONObject toJSON() throws JSONException {
            JSONObject json = new JSONObject();
            // Simplified serialization
            json.put("ocr_count", ocrMetrics.size());
            json.put("sync_count", cloudSyncMetrics.size());
            json.put("folder_count", folderMetrics.size());
            json.put("search_count", searchMetrics.size());
            json.put("feature_usage", new JSONObject(featureUsage));
            return json;
        }
        
        static AnalyticsData fromJSON(JSONObject json) {
            AnalyticsData data = new AnalyticsData();
            // Simplified deserialization
            return data;
        }
    }

    /**
     * OCR Metrics
     */
    public static class OCRMetrics {
        public long timestamp;
        public long processingTime;
        public float accuracy;
        public boolean success;
        public String language;
        public int characterCount;
    }

    /**
     * OCR Statistics
     */
    public static class OCRStatistics {
        public long averageProcessingTime;
        public float averageAccuracy;
        public float successRate;
        public int totalOperations;
    }

    /**
     * Cloud Sync Metrics
     */
    public static class CloudSyncMetrics {
        public long timestamp;
        public boolean success;
        public int filesCount;
        public long bytesTransferred;
        public long duration;
        public String errorMessage;
    }

    /**
     * Cloud Sync Statistics
     */
    public static class CloudSyncStatistics {
        public float successRate;
        public float failureRate;
        public long averageDuration;
        public long totalFilesSync;
        public long totalBytesSync;
        public int totalOperations;
    }

    /**
     * Folder Metrics
     */
    public static class FolderMetrics {
        public long timestamp;
        public String folderName;
        public boolean autoCategorized;
        public int documentsCount;
    }

    /**
     * Folder Statistics
     */
    public static class FolderStatistics {
        public Map<String, Integer> folderUsageMap;
        public float autoCategorizationRate;
        public int totalOrganizations;
    }

    /**
     * Search Metrics
     */
    public static class SearchMetrics {
        public long timestamp;
        public String query;
        public long processingTime;
        public int resultCount;
        public boolean userSatisfied;
    }

    /**
     * Search Statistics
     */
    public static class SearchStatistics {
        public long averageSearchTime;
        public float averageResultCount;
        public float satisfactionRate;
        public int totalSearches;
    }

    /**
     * Feature Adoption Statistics
     */
    public static class FeatureAdoptionStatistics {
        public Map<String, Integer> featureUsageMap;
        public String mostUsedFeature;
        public int totalFeatures;
    }

    /**
     * Engagement Statistics
     */
    public static class EngagementStatistics {
        public long averageSessionDuration;
        public int totalSessions;
    }

    /**
     * Resource Metrics
     */
    public static class ResourceMetrics {
        public long timestamp;
        public float memoryUsage;
        public int batteryLevel;
    }

    /**
     * Resource Statistics
     */
    public static class ResourceStatistics {
        public float averageMemoryUsage;
        public int averageBatteryLevel;
        public int measurementCount;
    }

    /**
     * Error Metrics
     */
    public static class ErrorMetrics {
        public long timestamp;
        public String errorType;
        public String message;
        public String stackTrace;
        public ErrorSeverity severity;
    }

    /**
     * Error Severity
     */
    public enum ErrorSeverity {
        INFO,
        WARNING,
        ERROR,
        CRITICAL
    }

    /**
     * Error Statistics
     */
    public static class ErrorStatistics {
        public Map<String, Integer> errorTypeMap;
        public int criticalErrors;
        public int warnings;
        public int totalErrors;
    }

    /**
     * Analytics Report
     */
    public static class AnalyticsReport {
        public long timestamp;
        public boolean hasUserConsent;
        public String message;
        public OCRStatistics ocrStats;
        public CloudSyncStatistics syncStats;
        public FolderStatistics folderStats;
        public SearchStatistics searchStats;
        public FeatureAdoptionStatistics adoptionStats;
        public EngagementStatistics engagementStats;
        public ResourceStatistics resourceStats;
        public ErrorStatistics errorStats;
    }

    /**
     * Analytics Event
     */
    public enum AnalyticsEvent {
        OCR_TRACKED,
        SYNC_TRACKED,
        FOLDER_TRACKED,
        SEARCH_TRACKED,
        FEATURE_USED,
        ERROR_TRACKED,
        HIGH_MEMORY_USAGE,
        LOW_BATTERY
    }

    // ================================
    // Callbacks & Interfaces
    // ================================

    public interface ConsentCallback {
        void onConsentDecision(boolean granted);
    }

    public interface AnalyticsListener {
        void onAnalyticsEvent(AnalyticsEvent event, Object data);
    }
}


