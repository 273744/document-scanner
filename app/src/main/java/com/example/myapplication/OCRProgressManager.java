package com.example.myapplication;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;

/**
 * OCRProgressManager - User feedback during text extraction
 *
 * Features:
 * - Progress dialog with extraction status
 * - Processing time estimates
 * - Network issue handling for language models
 * - Retry mechanism for failed extractions
 * - User guidance for improving OCR accuracy
 * - Language detection and switching notifications
 * - Battery optimization warnings
 * - Accessibility announcements
 */
public class OCRProgressManager {

    private static final String TAG = "OCRProgressManager";

    // Progress states
    public enum ProgressState {
        INITIALIZING("Initializing OCR..."),
        LOADING_MODEL("Loading language model..."),
        DETECTING_LANGUAGE("Detecting language..."),
        PROCESSING_IMAGE("Processing image..."),
        EXTRACTING_TEXT("Extracting text..."),
        ANALYZING_CONFIDENCE("Analyzing confidence..."),
        COMPLETE("Extraction complete"),
        FAILED("Extraction failed");

        public final String message;

        ProgressState(String message) {
            this.message = message;
        }
    }

    // Context
    private Context context;
    private Activity activity;

    // Progress dialog
    private ProgressDialog progressDialog;
    private MaterialAlertDialogBuilder materialDialog;

    // Timing
    private long startTime;
    private long estimatedTimeMs = 5000; // Default 5 seconds
    private Handler handler;

    // Retry
    private int retryCount = 0;
    private static final int MAX_RETRIES = 3;

    // Accessibility
    private AccessibilityManager accessibilityManager;

    // Network
    private ConnectivityManager connectivityManager;

    // Battery
    private BatteryManager batteryManager;

    // Callbacks
    private ProgressCallback callback;

    /**
     * Constructor
     */
    public OCRProgressManager(Context context) {
        this.context = context;
        if (context instanceof Activity) {
            this.activity = (Activity) context;
        }

        this.handler = new Handler(Looper.getMainLooper());
        this.accessibilityManager = (AccessibilityManager)
            context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        this.connectivityManager = (ConnectivityManager)
            context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.batteryManager = (BatteryManager)
            context.getSystemService(Context.BATTERY_SERVICE);

        Log.d(TAG, "OCRProgressManager initialized");
    }

    // ================================
    // 1. Progress Dialog with Status
    // ================================

    /**
     * Show progress dialog
     */
    public void showProgress(ProgressState state) {
        if (activity == null) {
            Log.w(TAG, "Activity is null, cannot show dialog");
            return;
        }

        activity.runOnUiThread(() -> {
            if (progressDialog == null) {
                progressDialog = new ProgressDialog(activity);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setCancelable(false);
                progressDialog.setMax(100);
            }

            progressDialog.setMessage(state.message);

            if (!progressDialog.isShowing()) {
                progressDialog.show();
            }

            announceForAccessibility(state.message);
        });
    }

    /**
     * Update progress percentage
     */
    public void updateProgress(int progress, String message) {
        if (activity == null) return;

        activity.runOnUiThread(() -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.setProgress(progress);
                progressDialog.setMessage(message);
            }

            if (callback != null) {
                callback.onProgressUpdate(progress, message);
            }
        });
    }

    /**
     * Show material progress dialog
     */
    public void showMaterialProgress(String title, String message) {
        if (activity == null) return;

        activity.runOnUiThread(() -> {
            if (materialDialog == null) {
                materialDialog = new MaterialAlertDialogBuilder(activity);
                materialDialog.setCancelable(false);
            }

            materialDialog.setTitle(title)
                .setMessage(message)
                .show();

            announceForAccessibility(message);
        });
    }

    /**
     * Dismiss progress dialog
     */
    public void dismissProgress() {
        if (activity == null) return;

        activity.runOnUiThread(() -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        });
    }

    // ================================
    // 2. Processing Time Estimates
    // ================================

    /**
     * Start timing
     */
    public void startTiming() {
        startTime = System.currentTimeMillis();
    }

    /**
     * Calculate estimated remaining time
     */
    public long calculateRemainingTime(int currentProgress) {
        if (currentProgress == 0 || startTime == 0) {
            return estimatedTimeMs;
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        long estimatedTotal = (elapsedTime * 100) / currentProgress;
        long remaining = estimatedTotal - elapsedTime;

        return Math.max(0, remaining);
    }

    /**
     * Update progress with time estimate
     */
    public void updateProgressWithTime(int progress) {
        long remainingMs = calculateRemainingTime(progress);
        int remainingSec = (int) (remainingMs / 1000);

        String timeText;
        if (remainingSec < 5) {
            timeText = "Almost done...";
        } else if (remainingSec < 60) {
            timeText = String.format(Locale.getDefault(),
                "About %d seconds remaining", remainingSec);
        } else {
            int minutes = remainingSec / 60;
            timeText = String.format(Locale.getDefault(),
                "About %d minute(s) remaining", minutes);
        }

        updateProgress(progress, timeText);
    }

    /**
     * Show completion time
     */
    public void showCompletionTime() {
        if (startTime == 0) return;

        long totalTime = System.currentTimeMillis() - startTime;
        String message = String.format(Locale.getDefault(),
            "Completed in %.1f seconds", totalTime / 1000.0);

        showToast(message);
        announceForAccessibility(message);
    }

    // ================================
    // 3. Network Issue Handling
    // ================================

    /**
     * Check network connectivity
     */
    public boolean checkNetworkConnectivity() {
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    /**
     * Handle network issues for model download
     */
    public void handleNetworkIssue(String modelName, NetworkCallback callback) {
        if (activity == null) return;

        activity.runOnUiThread(() -> {
            new MaterialAlertDialogBuilder(activity)
                .setTitle("Network Required")
                .setMessage("Language model '" + modelName + "' needs to be downloaded. " +
                    "Please connect to Wi-Fi or mobile data.")
                .setPositiveButton("Enable Wi-Fi", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                    activity.startActivity(intent);
                    callback.onUserAction(NetworkAction.ENABLE_WIFI);
                })
                .setNegativeButton("Use Mobile Data", (dialog, which) -> {
                    callback.onUserAction(NetworkAction.USE_MOBILE_DATA);
                })
                .setNeutralButton("Cancel", (dialog, which) -> {
                    callback.onUserAction(NetworkAction.CANCEL);
                })
                .show();

            announceForAccessibility("Network connection required to download language model");
        });
    }

    /**
     * Show model download progress
     */
    public void showModelDownloadProgress(String modelName, int progress) {
        String message = String.format(Locale.getDefault(),
            "Downloading %s model: %d%%", modelName, progress);

        updateProgress(progress, message);

        if (progress % 25 == 0) {
            announceForAccessibility(message);
        }
    }

    // ================================
    // 4. Retry Mechanism
    // ================================

    /**
     * Show retry dialog
     */
    public void showRetryDialog(String errorMessage, RetryCallback callback) {
        if (activity == null) return;

        retryCount++;

        activity.runOnUiThread(() -> {
            String message = errorMessage + "\n\n" +
                String.format("Retry attempt %d of %d", retryCount, MAX_RETRIES);

            new MaterialAlertDialogBuilder(activity)
                .setTitle("Extraction Failed")
                .setMessage(message)
                .setPositiveButton("Retry", (dialog, which) -> {
                    callback.onRetry(retryCount);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    callback.onCancel();
                })
                .setCancelable(false)
                .show();

            announceForAccessibility("Extraction failed. Retry option available.");
        });
    }

    /**
     * Check if can retry
     */
    public boolean canRetry() {
        return retryCount < MAX_RETRIES;
    }

    /**
     * Reset retry count
     */
    public void resetRetryCount() {
        retryCount = 0;
    }

    /**
     * Handle extraction failure
     */
    public void handleExtractionFailure(Exception e, RetryCallback callback) {
        String errorMessage = getErrorMessage(e);

        if (canRetry()) {
            showRetryDialog(errorMessage, callback);
        } else {
            showFinalFailureDialog(errorMessage);
        }
    }

    /**
     * Get user-friendly error message
     */
    private String getErrorMessage(Exception e) {
        String message = e.getMessage();

        if (message == null) {
            return "Unknown error occurred";
        }

        if (message.contains("UNAVAILABLE")) {
            return "OCR service is unavailable. Please try again later.";
        } else if (message.contains("NOT_ENOUGH_SPACE")) {
            return "Not enough storage space for OCR models.";
        } else if (message.contains("network") || message.contains("connection")) {
            return "Network connection error. Check your internet connection.";
        } else if (message.contains("timeout")) {
            return "Processing timeout. The image may be too large or complex.";
        } else {
            return "Text extraction failed: " + message;
        }
    }

    /**
     * Show final failure dialog
     */
    private void showFinalFailureDialog(String errorMessage) {
        if (activity == null) return;

        activity.runOnUiThread(() -> {
            new MaterialAlertDialogBuilder(activity)
                .setTitle("Extraction Failed")
                .setMessage(errorMessage + "\n\nMaximum retry attempts reached.")
                .setPositiveButton("OK", null)
                .show();

            announceForAccessibility("Text extraction failed after multiple attempts");
        });
    }

    // ================================
    // 5. User Guidance for Accuracy
    // ================================

    /**
     * Show accuracy improvement tips
     */
    public void showAccuracyTips() {
        if (activity == null) return;

        String tips = "Tips for better OCR accuracy:\n\n" +
            "• Ensure good lighting conditions\n" +
            "• Hold camera steady and avoid blur\n" +
            "• Position document flat and straight\n" +
            "• Avoid shadows and glare\n" +
            "• Use high-resolution images\n" +
            "• Remove background clutter\n" +
            "• Ensure text is clear and legible";

        activity.runOnUiThread(() -> {
            new MaterialAlertDialogBuilder(activity)
                .setTitle("Improve OCR Accuracy")
                .setMessage(tips)
                .setPositiveButton("Got it", null)
                .show();

            announceForAccessibility("Tips for improving OCR accuracy displayed");
        });
    }

    /**
     * Analyze and provide specific guidance
     */
    public void analyzeAndProvideGuidance(float confidence, String detectedIssue) {
        if (confidence < 0.5f) {
            showLowConfidenceGuidance(detectedIssue);
        } else if (confidence < 0.7f) {
            showMediumConfidenceGuidance();
        }
    }

    /**
     * Show low confidence guidance
     */
    private void showLowConfidenceGuidance(String issue) {
        String message = "Low confidence detected.\n\n";

        if (issue != null) {
            if (issue.contains("lighting")) {
                message += "Suggestion: Improve lighting conditions";
            } else if (issue.contains("blur")) {
                message += "Suggestion: Reduce camera shake or blur";
            } else if (issue.contains("angle")) {
                message += "Suggestion: Position document straight";
            } else {
                message += "Suggestion: Try recapturing with better conditions";
            }
        }

        showToast(message);
        announceForAccessibility(message);
    }

    /**
     * Show medium confidence guidance
     */
    private void showMediumConfidenceGuidance() {
        String message = "Moderate accuracy detected. " +
            "Consider recapturing for better results.";

        showToast(message);
    }

    // ================================
    // 6. Language Detection Notifications
    // ================================

    /**
     * Notify language detected
     */
    public void notifyLanguageDetected(String languageCode, String languageName) {
        String message = String.format("Detected language: %s", languageName);

        showSnackbar(message);
        announceForAccessibility(message);

        Log.d(TAG, "Language detected: " + languageCode + " (" + languageName + ")");
    }

    /**
     * Show language switching notification
     */
    public void notifyLanguageSwitching(String fromLanguage, String toLanguage) {
        if (activity == null) return;

        String message = String.format("Switching from %s to %s for better accuracy",
            fromLanguage, toLanguage);

        activity.runOnUiThread(() -> {
            updateProgress(50, message);
            announceForAccessibility(message);
        });
    }

    /**
     * Show language model download notification
     */
    public void notifyModelDownload(String languageName, boolean required) {
        if (activity == null) return;

        String message = required ?
            String.format("%s language model required. Downloading...", languageName) :
            String.format("Downloading %s language model for better accuracy...", languageName);

        activity.runOnUiThread(() -> {
            showMaterialProgress("Downloading Model", message);
            announceForAccessibility(message);
        });
    }

    // ================================
    // 7. Battery Optimization Warnings
    // ================================

    /**
     * Check battery level
     */
    public int getBatteryLevel() {
        if (batteryManager == null) return 100;

        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
    }

    /**
     * Check if battery is charging
     */
    public boolean isBatteryCharging() {
        if (batteryManager == null) return false;

        int status = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS);
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
               status == BatteryManager.BATTERY_STATUS_FULL;
    }

    /**
     * Show battery warning if needed
     */
    public boolean checkBatteryAndWarn() {
        int batteryLevel = getBatteryLevel();
        boolean isCharging = isBatteryCharging();

        if (batteryLevel < 15 && !isCharging) {
            showBatteryWarning(batteryLevel);
            return false; // Recommend not proceeding
        } else if (batteryLevel < 30 && !isCharging) {
            showLowBatteryNotification(batteryLevel);
        }

        return true; // OK to proceed
    }

    /**
     * Show battery warning dialog
     */
    private void showBatteryWarning(int batteryLevel) {
        if (activity == null) return;

        activity.runOnUiThread(() -> {
            String message = String.format(Locale.getDefault(),
                "Battery level is low (%d%%). OCR processing may drain battery quickly. " +
                "Consider charging your device or using a simpler extraction method.",
                batteryLevel);

            new MaterialAlertDialogBuilder(activity)
                .setTitle("Low Battery Warning")
                .setMessage(message)
                .setPositiveButton("Continue Anyway", null)
                .setNegativeButton("Cancel", (dialog, which) -> {
                    if (callback != null) {
                        callback.onBatteryCancelled();
                    }
                })
                .show();

            announceForAccessibility("Low battery warning");
        });
    }

    /**
     * Show low battery notification
     */
    private void showLowBatteryNotification(int batteryLevel) {
        String message = String.format(Locale.getDefault(),
            "Battery at %d%%. Processing may consume significant power.", batteryLevel);

        showToast(message);
        announceForAccessibility(message);
    }

    /**
     * Optimize for battery saving
     */
    public void suggestBatteryOptimization() {
        if (activity == null) return;

        activity.runOnUiThread(() -> {
            new MaterialAlertDialogBuilder(activity)
                .setTitle("Battery Optimization")
                .setMessage("To save battery during OCR:\n\n" +
                    "• Use smaller image resolutions\n" +
                    "• Process fewer languages\n" +
                    "• Disable real-time processing\n" +
                    "• Close other apps")
                .setPositiveButton("OK", null)
                .show();
        });
    }

    // ================================
    // 8. Accessibility Announcements
    // ================================

    /**
     * Announce for accessibility
     */
    public void announceForAccessibility(String message) {
        if (accessibilityManager == null || !accessibilityManager.isEnabled()) {
            return;
        }

        AccessibilityEvent event = AccessibilityEvent.obtain(
            AccessibilityEvent.TYPE_ANNOUNCEMENT);
        event.getText().add(message);

        if (activity != null && activity.findViewById(android.R.id.content) != null) {
            activity.findViewById(android.R.id.content)
                .sendAccessibilityEventUnchecked(event);
        }

        Log.d(TAG, "Accessibility announcement: " + message);
    }

    /**
     * Announce progress for accessibility
     */
    public void announceProgress(int progress, String state) {
        if (progress % 25 == 0 || progress == 100) {
            String message = String.format(Locale.getDefault(),
                "%s. %d percent complete", state, progress);
            announceForAccessibility(message);
        }
    }

    /**
     * Announce completion for accessibility
     */
    public void announceCompletion(int wordCount, float confidence) {
        String message = String.format(Locale.getDefault(),
            "Text extraction complete. %d words extracted with %.0f percent confidence",
            wordCount, confidence * 100);

        announceForAccessibility(message);
    }

    // ================================
    // Helper Methods
    // ================================

    /**
     * Show toast message
     */
    private void showToast(String message) {
        if (activity == null) return;

        activity.runOnUiThread(() -> {
            Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
        });
    }

    /**
     * Show snackbar message
     */
    private void showSnackbar(String message) {
        if (activity == null) return;

        activity.runOnUiThread(() -> {
            Snackbar.make(activity.findViewById(android.R.id.content),
                message, Snackbar.LENGTH_LONG).show();
        });
    }

    // ================================
    // Callbacks
    // ================================

    public void setCallback(ProgressCallback callback) {
        this.callback = callback;
    }

    public interface ProgressCallback {
        void onProgressUpdate(int progress, String message);
        void onBatteryCancelled();
    }

    public interface NetworkCallback {
        void onUserAction(NetworkAction action);
    }

    public interface RetryCallback {
        void onRetry(int attemptNumber);
        void onCancel();
    }

    public enum NetworkAction {
        ENABLE_WIFI,
        USE_MOBILE_DATA,
        CANCEL
    }

    // ================================
    // Cleanup
    // ================================

    /**
     * Cleanup resources
     */
    public void cleanup() {
        dismissProgress();

        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }

        callback = null;

        Log.d(TAG, "OCRProgressManager cleaned up");
    }
}

