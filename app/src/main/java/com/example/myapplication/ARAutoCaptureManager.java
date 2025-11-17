package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * ARAutoCaptureManager - Intelligent automatic photo capture
 *
 * Features:
 * - Continuous quality score monitoring
 * - Threshold detection (8/10 default)
 * - Stability verification (1 second)
 * - Automatic countdown and capture
 * - Manual override controls
 * - Multi-document batch capture
 * - AR success effects and confirmation
 * - Smart timing algorithms
 * - User preference handling
 */
public class ARAutoCaptureManager {

    private static final String TAG = "ARAutoCaptureManager";
    private static final String PREFS_NAME = "ARAutoCapturePrefs";

    // Quality thresholds
    private static final int DEFAULT_QUALITY_THRESHOLD = 8;
    private static final int MIN_QUALITY_THRESHOLD = 6;
    private static final int MAX_QUALITY_THRESHOLD = 10;

    // Stability requirements
    private static final long STABILITY_DURATION = 1000; // 1 second
    private static final float STABILITY_THRESHOLD = 0.05f; // Max score variation
    private static final int MIN_STABLE_FRAMES = 15; // ~15 frames at 30fps

    // Countdown settings
    private static final long COUNTDOWN_DURATION = 3000; // 3 seconds
    private static final long COUNTDOWN_TICK = 1000; // 1 second intervals

    // Batch capture
    private static final long BATCH_CAPTURE_INTERVAL = 2000; // 2 seconds between captures
    private static final int MAX_BATCH_SIZE = 10;

    // Context and preferences
    private Context context;
    private SharedPreferences preferences;

    // Components
    private Handler handler;
    private ARAnimationController animationController;
    private DocumentQualityAnalyzer qualityAnalyzer;

    // State tracking
    private boolean autoModeEnabled = true;
    private boolean isMonitoring = false;
    private boolean isCapturing = false;
    private boolean isStable = false;
    private int qualityThreshold = DEFAULT_QUALITY_THRESHOLD;

    // Quality history for stability
    private LinkedList<QualitySnapshot> qualityHistory = new LinkedList<>();
    private static final int HISTORY_SIZE = 30; // 1 second at 30fps

    // Stability tracking
    private long stabilityStartTime = 0;
    private int stableFrameCount = 0;

    // Countdown
    private Runnable countdownRunnable;
    private int currentCountdown = 0;

    // Batch capture
    private Queue<CaptureRequest> batchQueue = new LinkedList<>();
    private boolean isBatchMode = false;
    private int capturedInBatch = 0;

    // Callbacks
    private AutoCaptureCallback callback;

    /**
     * Constructor
     */
    public ARAutoCaptureManager(Context context) {
        this.context = context;
        this.handler = new Handler(Looper.getMainLooper());
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Load preferences
        loadPreferences();
    }

    // ================================
    // 1. Quality Monitoring
    // ================================

    /**
     * Monitor document quality continuously
     */
    public void monitorQuality(DocumentQualityAnalyzer.QualityResult quality,
                              ARDocumentDetector.DetectedDocument document) {
        if (!isMonitoring || quality == null) {
            return;
        }

        // Add to history
        QualitySnapshot snapshot = new QualitySnapshot(
            quality.overallScore,
            System.currentTimeMillis()
        );
        addToHistory(snapshot);

        // Check if quality meets threshold
        if (quality.overallScore >= qualityThreshold) {
            checkStability(document);
        } else {
            // Quality dropped - reset stability
            resetStability();
        }
    }

    /**
     * Add snapshot to quality history
     */
    private void addToHistory(QualitySnapshot snapshot) {
        qualityHistory.add(snapshot);

        // Keep only recent history
        while (qualityHistory.size() > HISTORY_SIZE) {
            qualityHistory.removeFirst();
        }
    }

    /**
     * Get average quality from history
     */
    private double getAverageQuality() {
        if (qualityHistory.isEmpty()) {
            return 0;
        }

        double sum = 0;
        for (QualitySnapshot snapshot : qualityHistory) {
            sum += snapshot.score;
        }

        return sum / qualityHistory.size();
    }

    /**
     * Get quality variance (for stability check)
     */
    private double getQualityVariance() {
        if (qualityHistory.size() < 2) {
            return Double.MAX_VALUE;
        }

        double avg = getAverageQuality();
        double sumSq = 0;

        for (QualitySnapshot snapshot : qualityHistory) {
            double diff = snapshot.score - avg;
            sumSq += diff * diff;
        }

        return Math.sqrt(sumSq / qualityHistory.size());
    }

    // ================================
    // 2. Threshold Detection
    // ================================

    /**
     * Check if quality threshold is reached
     */
    private boolean isQualityThresholdReached() {
        return getAverageQuality() >= qualityThreshold;
    }

    /**
     * Set quality threshold
     */
    public void setQualityThreshold(int threshold) {
        this.qualityThreshold = Math.max(MIN_QUALITY_THRESHOLD,
                                        Math.min(MAX_QUALITY_THRESHOLD, threshold));

        // Save preference
        preferences.edit().putInt("quality_threshold", this.qualityThreshold).apply();

        Log.d(TAG, "Quality threshold set to: " + this.qualityThreshold);
    }

    /**
     * Get current quality threshold
     */
    public int getQualityThreshold() {
        return qualityThreshold;
    }

    // ================================
    // 3. Stability Verification
    // ================================

    /**
     * Check stability of document/camera
     */
    private void checkStability(ARDocumentDetector.DetectedDocument document) {
        // Check quality variance
        double variance = getQualityVariance();

        if (variance <= STABILITY_THRESHOLD) {
            // Quality is stable
            if (!isStable) {
                // Just became stable
                isStable = true;
                stabilityStartTime = System.currentTimeMillis();
                stableFrameCount = 0;

                if (callback != null) {
                    callback.onStabilityDetected();
                }
            }

            stableFrameCount++;

            // Check if stable for required duration
            long stableDuration = System.currentTimeMillis() - stabilityStartTime;

            if (stableDuration >= STABILITY_DURATION &&
                stableFrameCount >= MIN_STABLE_FRAMES) {
                // Stable for required time - trigger capture
                onStabilityVerified(document);
            } else {
                // Still stabilizing
                if (callback != null) {
                    float progress = (float) stableDuration / STABILITY_DURATION;
                    callback.onStabilityProgress(progress);
                }
            }
        } else {
            // Not stable - reset
            resetStability();
        }
    }

    /**
     * Reset stability state
     */
    private void resetStability() {
        if (isStable) {
            isStable = false;
            stabilityStartTime = 0;
            stableFrameCount = 0;

            if (callback != null) {
                callback.onStabilityLost();
            }
        }
    }

    /**
     * Called when stability is verified
     */
    private void onStabilityVerified(ARDocumentDetector.DetectedDocument document) {
        if (isCapturing) {
            return; // Already capturing
        }

        Log.d(TAG, "Stability verified - initiating capture");

        if (callback != null) {
            callback.onStabilityVerified();
        }

        // Start countdown if auto mode enabled
        if (autoModeEnabled) {
            startCountdown(document);
        }
    }

    // ================================
    // 4. Automatic Countdown & Capture
    // ================================

    /**
     * Start countdown for automatic capture
     */
    private void startCountdown(final ARDocumentDetector.DetectedDocument document) {
        isCapturing = true;
        currentCountdown = (int) (COUNTDOWN_DURATION / COUNTDOWN_TICK);

        Log.d(TAG, "Starting countdown: " + currentCountdown);

        if (callback != null) {
            callback.onCountdownStarted(currentCountdown);
        }

        // Countdown runnable
        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                currentCountdown--;

                if (callback != null) {
                    callback.onCountdownTick(currentCountdown);
                }

                if (currentCountdown > 0) {
                    // Continue countdown
                    handler.postDelayed(this, COUNTDOWN_TICK);
                } else {
                    // Countdown finished - capture!
                    performCapture(document);
                }
            }
        };

        handler.postDelayed(countdownRunnable, COUNTDOWN_TICK);
    }

    /**
     * Cancel ongoing countdown
     */
    public void cancelCountdown() {
        if (countdownRunnable != null) {
            handler.removeCallbacks(countdownRunnable);
            countdownRunnable = null;
        }

        isCapturing = false;
        currentCountdown = 0;

        if (callback != null) {
            callback.onCountdownCancelled();
        }

        Log.d(TAG, "Countdown cancelled");
    }

    /**
     * Perform actual capture
     */
    private void performCapture(ARDocumentDetector.DetectedDocument document) {
        Log.d(TAG, "Performing capture");

        if (callback != null) {
            callback.onCaptureTriggered(document);
        }

        // Reset state
        isCapturing = false;
        resetStability();
        qualityHistory.clear();

        // In batch mode, schedule next capture
        if (isBatchMode && !batchQueue.isEmpty()) {
            handler.postDelayed(() -> {
                processBatchQueue();
            }, BATCH_CAPTURE_INTERVAL);
        }
    }

    // ================================
    // 5. Manual Override
    // ================================

    /**
     * Manual capture (bypass auto capture)
     */
    public void manualCapture(ARDocumentDetector.DetectedDocument document) {
        Log.d(TAG, "Manual capture triggered");

        // Cancel any ongoing countdown
        cancelCountdown();

        // Perform immediate capture
        if (callback != null) {
            callback.onManualCaptureTriggered(document);
        }

        // Reset state
        resetStability();
        qualityHistory.clear();
    }

    /**
     * Enable/disable auto capture mode
     */
    public void setAutoModeEnabled(boolean enabled) {
        this.autoModeEnabled = enabled;

        // Save preference
        preferences.edit().putBoolean("auto_mode_enabled", enabled).apply();

        if (!enabled) {
            // Cancel any ongoing capture
            cancelCountdown();
        }

        if (callback != null) {
            callback.onAutoModeChanged(enabled);
        }

        Log.d(TAG, "Auto mode: " + (enabled ? "enabled" : "disabled"));
    }

    /**
     * Check if auto mode is enabled
     */
    public boolean isAutoModeEnabled() {
        return autoModeEnabled;
    }

    /**
     * Start/stop monitoring
     */
    public void setMonitoring(boolean monitoring) {
        this.isMonitoring = monitoring;

        if (!monitoring) {
            resetStability();
            qualityHistory.clear();
            cancelCountdown();
        }

        Log.d(TAG, "Monitoring: " + (monitoring ? "started" : "stopped"));
    }

    /**
     * Check if currently monitoring
     */
    public boolean isMonitoring() {
        return isMonitoring;
    }

    // ================================
    // 6. Batch Capture
    // ================================

    /**
     * Start batch capture mode
     */
    public void startBatchCapture(int documentCount) {
        if (documentCount <= 0 || documentCount > MAX_BATCH_SIZE) {
            Log.w(TAG, "Invalid batch size: " + documentCount);
            return;
        }

        isBatchMode = true;
        capturedInBatch = 0;
        batchQueue.clear();

        // Create capture requests
        for (int i = 0; i < documentCount; i++) {
            batchQueue.add(new CaptureRequest(i));
        }

        if (callback != null) {
            callback.onBatchCaptureStarted(documentCount);
        }

        Log.d(TAG, "Batch capture started: " + documentCount + " documents");
    }

    /**
     * Process batch queue
     */
    private void processBatchQueue() {
        if (!isBatchMode || batchQueue.isEmpty()) {
            completeBatchCapture();
            return;
        }

        CaptureRequest request = batchQueue.poll();

        if (callback != null) {
            callback.onBatchCaptureProgress(request.index, batchQueue.size() + 1);
        }

        // Wait for next document to be ready
        // Monitoring will trigger capture when ready
    }

    /**
     * Complete batch capture
     */
    private void completeBatchCapture() {
        isBatchMode = false;
        capturedInBatch = 0;
        batchQueue.clear();

        if (callback != null) {
            callback.onBatchCaptureCompleted();
        }

        Log.d(TAG, "Batch capture completed");
    }

    /**
     * Cancel batch capture
     */
    public void cancelBatchCapture() {
        isBatchMode = false;
        batchQueue.clear();
        cancelCountdown();

        if (callback != null) {
            callback.onBatchCaptureCancelled(capturedInBatch);
        }

        Log.d(TAG, "Batch capture cancelled");
    }

    /**
     * Get batch progress
     */
    public int getBatchProgress() {
        return capturedInBatch;
    }

    /**
     * Check if in batch mode
     */
    public boolean isBatchMode() {
        return isBatchMode;
    }

    // ================================
    // 7. Success Effects
    // ================================

    /**
     * Show capture success effects
     */
    public void showCaptureSuccess() {
        if (callback != null) {
            callback.onCaptureSuccess();
        }

        // Increment batch counter
        if (isBatchMode) {
            capturedInBatch++;
        }
    }

    /**
     * Show capture failure
     */
    public void showCaptureFailure(String reason) {
        if (callback != null) {
            callback.onCaptureFailure(reason);
        }

        // Reset state
        isCapturing = false;
        resetStability();
    }

    // ================================
    // Preferences
    // ================================

    /**
     * Load saved preferences
     */
    private void loadPreferences() {
        autoModeEnabled = preferences.getBoolean("auto_mode_enabled", true);
        qualityThreshold = preferences.getInt("quality_threshold", DEFAULT_QUALITY_THRESHOLD);

        Log.d(TAG, "Preferences loaded - Auto: " + autoModeEnabled +
                   ", Threshold: " + qualityThreshold);
    }

    /**
     * Reset to default preferences
     */
    public void resetPreferences() {
        preferences.edit().clear().apply();
        loadPreferences();
    }

    // ================================
    // Statistics
    // ================================

    /**
     * Get capture statistics
     */
    public CaptureStatistics getStatistics() {
        CaptureStatistics stats = new CaptureStatistics();
        stats.isStable = isStable;
        stats.stableDuration = isStable ?
            (System.currentTimeMillis() - stabilityStartTime) : 0;
        stats.averageQuality = getAverageQuality();
        stats.qualityVariance = getQualityVariance();
        stats.isCapturing = isCapturing;
        stats.currentCountdown = currentCountdown;
        stats.batchProgress = capturedInBatch;
        stats.batchTotal = batchQueue.size() + capturedInBatch;

        return stats;
    }

    // ================================
    // Callbacks
    // ================================

    public void setCallback(AutoCaptureCallback callback) {
        this.callback = callback;
    }

    /**
     * Auto capture callback interface
     */
    public interface AutoCaptureCallback {
        // Quality monitoring
        void onQualityThresholdReached(int score);

        // Stability
        void onStabilityDetected();
        void onStabilityProgress(float progress);
        void onStabilityVerified();
        void onStabilityLost();

        // Countdown
        void onCountdownStarted(int seconds);
        void onCountdownTick(int secondsRemaining);
        void onCountdownCancelled();

        // Capture
        void onCaptureTriggered(ARDocumentDetector.DetectedDocument document);
        void onManualCaptureTriggered(ARDocumentDetector.DetectedDocument document);
        void onCaptureSuccess();
        void onCaptureFailure(String reason);

        // Batch
        void onBatchCaptureStarted(int count);
        void onBatchCaptureProgress(int current, int total);
        void onBatchCaptureCompleted();
        void onBatchCaptureCancelled(int capturedCount);

        // Mode
        void onAutoModeChanged(boolean enabled);
    }

    // ================================
    // Data Classes
    // ================================

    /**
     * Quality snapshot for history
     */
    private static class QualitySnapshot {
        int score;
        long timestamp;

        QualitySnapshot(int score, long timestamp) {
            this.score = score;
            this.timestamp = timestamp;
        }
    }

    /**
     * Capture request for batch mode
     */
    private static class CaptureRequest {
        int index;

        CaptureRequest(int index) {
            this.index = index;
        }
    }

    /**
     * Capture statistics
     */
    public static class CaptureStatistics {
        public boolean isStable;
        public long stableDuration;
        public double averageQuality;
        public double qualityVariance;
        public boolean isCapturing;
        public int currentCountdown;
        public int batchProgress;
        public int batchTotal;
    }

    // ================================
    // Cleanup
    // ================================

    /**
     * Cleanup resources
     */
    public void cleanup() {
        cancelCountdown();
        handler.removeCallbacksAndMessages(null);
        qualityHistory.clear();
        batchQueue.clear();
        callback = null;

        Log.d(TAG, "ARAutoCaptureManager cleaned up");
    }
}

