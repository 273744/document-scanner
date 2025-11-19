package com.srikanth.docscanner;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;

/**
 * ARErrorManager - Robust error handling for AR operations
 *
 * Features:
 * - ARCore session interruption handling
 * - Camera permission denial recovery
 * - Lighting condition detection and guidance
 * - AR tracking lost recovery
 * - Device movement warnings
 * - Hardware compatibility fallback
 * - Network connectivity monitoring
 * - User-friendly error messages
 * - Multi-language error guidance
 */
public class ARErrorManager {

    private static final String TAG = "ARErrorManager";

    // Error types
    public enum ErrorType {
        ARCORE_NOT_INSTALLED,
        ARCORE_OUTDATED,
        DEVICE_NOT_COMPATIBLE,
        CAMERA_PERMISSION_DENIED,
        CAMERA_NOT_AVAILABLE,
        INSUFFICIENT_LIGHTING,
        TRACKING_LOST,
        EXCESSIVE_MOTION,
        SESSION_INTERRUPTED,
        NETWORK_UNAVAILABLE,
        HARDWARE_ERROR,
        UNKNOWN
    }

    // Error severity levels
    public enum ErrorSeverity {
        INFO,      // Informational message
        WARNING,   // Warning that can be recovered
        ERROR,     // Error requiring user action
        CRITICAL   // Critical error, cannot continue
    }

    // Context
    private Context context;
    private View rootView;

    // Error tracking
    private Map<ErrorType, Integer> errorCounts = new HashMap<>();
    private Queue<ErrorRecord> recentErrors = new LinkedList<>();
    private static final int MAX_ERROR_HISTORY = 20;

    // Lighting tracking
    private float[] lightingHistory = new float[30]; // Last 30 frames
    private int lightingIndex = 0;
    private static final float MIN_LIGHTING = 0.3f;
    private static final float OPTIMAL_LIGHTING = 0.7f;

    // Tracking state
    private TrackingState lastTrackingState = TrackingState.TRACKING;
    private long trackingLostTime = 0;
    private static final long TRACKING_LOST_THRESHOLD = 3000; // 3 seconds

    // Motion detection
    private float[] lastCameraPosition = new float[3];
    private long lastMotionCheckTime = 0;
    private static final float EXCESSIVE_MOTION_THRESHOLD = 0.5f; // meters
    private static final long MOTION_CHECK_INTERVAL = 100; // 100ms

    // Language support
    private String languageCode = "en";

    // Callbacks
    private ErrorCallback callback;

    /**
     * Constructor
     */
    public ARErrorManager(Context context, View rootView) {
        this.context = context;
        this.rootView = rootView;

        // Load device locale
        languageCode = Locale.getDefault().getLanguage();

        Log.d(TAG, "ARErrorManager initialized");
    }

    // ================================
    // 1. ARCore Session Interruptions
    // ================================

    /**
     * Handle ARCore session exceptions
     */
    public void handleSessionException(Exception exception) {
        ErrorType errorType = ErrorType.UNKNOWN;
        ErrorSeverity severity = ErrorSeverity.ERROR;
        String message = "";
        String solution = "";

        if (exception instanceof UnavailableArcoreNotInstalledException) {
            errorType = ErrorType.ARCORE_NOT_INSTALLED;
            severity = ErrorSeverity.CRITICAL;
            message = getLocalizedString("error_arcore_not_installed");
            solution = getLocalizedString("solution_install_arcore");

            showARCoreInstallDialog();

        } else if (exception instanceof UnavailableApkTooOldException) {
            errorType = ErrorType.ARCORE_OUTDATED;
            severity = ErrorSeverity.CRITICAL;
            message = getLocalizedString("error_arcore_outdated");
            solution = getLocalizedString("solution_update_arcore");

            showARCoreUpdateDialog();

        } else if (exception instanceof UnavailableDeviceNotCompatibleException) {
            errorType = ErrorType.DEVICE_NOT_COMPATIBLE;
            severity = ErrorSeverity.CRITICAL;
            message = getLocalizedString("error_device_not_compatible");
            solution = getLocalizedString("solution_device_not_compatible");

            showDeviceNotCompatibleDialog();

        } else if (exception instanceof UnavailableSdkTooOldException) {
            errorType = ErrorType.ARCORE_OUTDATED;
            severity = ErrorSeverity.CRITICAL;
            message = getLocalizedString("error_sdk_too_old");
            solution = getLocalizedString("solution_update_app");

        } else if (exception instanceof UnavailableUserDeclinedInstallationException) {
            errorType = ErrorType.ARCORE_NOT_INSTALLED;
            severity = ErrorSeverity.ERROR;
            message = getLocalizedString("error_user_declined_arcore");
            solution = getLocalizedString("solution_need_arcore");

        } else if (exception instanceof CameraNotAvailableException) {
            errorType = ErrorType.CAMERA_NOT_AVAILABLE;
            severity = ErrorSeverity.ERROR;
            message = getLocalizedString("error_camera_not_available");
            solution = getLocalizedString("solution_camera_not_available");

            showCameraNotAvailableDialog();
        }

        // Record error
        recordError(errorType, severity, message, exception);

        // Show error to user
        showError(message, solution, severity);

        // Notify callback
        if (callback != null) {
            callback.onError(errorType, severity, message, solution);
        }

        Log.e(TAG, "ARCore session error: " + errorType, exception);
    }

    /**
     * Check if ARCore is available
     */
    public boolean checkARCoreAvailability() {
        ArCoreApk.Availability availability = ArCoreApk.getInstance().checkAvailability(context);

        if (availability.isUnsupported()) {
            handleError(ErrorType.DEVICE_NOT_COMPATIBLE, ErrorSeverity.CRITICAL,
                getLocalizedString("error_device_not_compatible"),
                getLocalizedString("solution_device_not_compatible"));
            return false;
        }

        return true;
    }

    // ================================
    // 2. Camera Permission Handling
    // ================================

    /**
     * Handle camera permission denial
     */
    public void handleCameraPermissionDenied(boolean permanentlyDenied) {
        if (permanentlyDenied) {
            // User checked "Don't ask again"
            showCameraPermissionDialog(true);

            handleError(ErrorType.CAMERA_PERMISSION_DENIED, ErrorSeverity.CRITICAL,
                getLocalizedString("error_camera_permission_denied_permanent"),
                getLocalizedString("solution_camera_permission_settings"));
        } else {
            // First denial or temporary
            showCameraPermissionDialog(false);

            handleError(ErrorType.CAMERA_PERMISSION_DENIED, ErrorSeverity.ERROR,
                getLocalizedString("error_camera_permission_denied"),
                getLocalizedString("solution_camera_permission_needed"));
        }
    }

    /**
     * Show camera permission explanation dialog
     */
    private void showCameraPermissionDialog(boolean permanentlyDenied) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(getLocalizedString("dialog_camera_permission_title"));

        if (permanentlyDenied) {
            builder.setMessage(getLocalizedString("dialog_camera_permission_settings"));
            builder.setPositiveButton(getLocalizedString("button_settings"), (dialog, which) -> {
                openAppSettings();
            });
        } else {
            builder.setMessage(getLocalizedString("dialog_camera_permission_rationale"));
            builder.setPositiveButton(getLocalizedString("button_grant_permission"), (dialog, which) -> {
                if (callback != null) {
                    callback.onRequestPermission();
                }
            });
        }

        builder.setNegativeButton(getLocalizedString("button_cancel"), (dialog, which) -> {
            if (callback != null) {
                callback.onPermissionPermanentlyDenied();
            }
        });

        builder.setCancelable(false);
        builder.show();
    }

    /**
     * Open app settings
     */
    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        context.startActivity(intent);
    }

    // ================================
    // 3. Lighting Condition Handling
    // ================================

    /**
     * Check lighting conditions
     */
    public void checkLightingConditions(Frame frame) {
        try {
            // Get light estimate from ARCore
            float pixelIntensity = 1.0f;

            if (frame.getLightEstimate() != null) {
                pixelIntensity = frame.getLightEstimate().getPixelIntensity();
            }

            // Add to history
            lightingHistory[lightingIndex] = pixelIntensity;
            lightingIndex = (lightingIndex + 1) % lightingHistory.length;

            // Calculate average
            float avgLighting = 0;
            for (float value : lightingHistory) {
                avgLighting += value;
            }
            avgLighting /= lightingHistory.length;

            // Check if lighting is insufficient
            if (avgLighting < MIN_LIGHTING) {
                handleError(ErrorType.INSUFFICIENT_LIGHTING, ErrorSeverity.WARNING,
                    getLocalizedString("error_insufficient_lighting"),
                    getLocalizedString("solution_insufficient_lighting"));
            } else if (avgLighting > OPTIMAL_LIGHTING * 1.5f) {
                // Too bright
                showWarning(getLocalizedString("warning_lighting_too_bright"));
            }

        } catch (Exception e) {
            Log.w(TAG, "Error checking lighting conditions", e);
        }
    }

    /**
     * Get lighting quality description
     */
    public String getLightingQuality(Frame frame) {
        try {
            float pixelIntensity = 1.0f;

            if (frame.getLightEstimate() != null) {
                pixelIntensity = frame.getLightEstimate().getPixelIntensity();
            }

            if (pixelIntensity < MIN_LIGHTING) {
                return getLocalizedString("lighting_too_dark");
            } else if (pixelIntensity > OPTIMAL_LIGHTING * 1.5f) {
                return getLocalizedString("lighting_too_bright");
            } else if (pixelIntensity >= OPTIMAL_LIGHTING) {
                return getLocalizedString("lighting_excellent");
            } else {
                return getLocalizedString("lighting_good");
            }
        } catch (Exception e) {
            return getLocalizedString("lighting_unknown");
        }
    }

    // ================================
    // 4. Tracking Lost Recovery
    // ================================

    /**
     * Monitor tracking state
     */
    public void monitorTrackingState(Camera camera) {
        TrackingState currentState = camera.getTrackingState();

        if (currentState != lastTrackingState) {
            onTrackingStateChanged(lastTrackingState, currentState);
            lastTrackingState = currentState;
        }

        // Check if tracking has been lost for too long
        if (currentState != TrackingState.TRACKING) {
            if (trackingLostTime == 0) {
                trackingLostTime = System.currentTimeMillis();
            }

            long lostDuration = System.currentTimeMillis() - trackingLostTime;

            if (lostDuration > TRACKING_LOST_THRESHOLD) {
                handleTrackingLostTooLong(lostDuration);
            }
        } else {
            trackingLostTime = 0;
        }
    }

    /**
     * Handle tracking state changes
     */
    private void onTrackingStateChanged(TrackingState oldState, TrackingState newState) {
        switch (newState) {
            case PAUSED:
                handleError(ErrorType.TRACKING_LOST, ErrorSeverity.WARNING,
                    getLocalizedString("error_tracking_paused"),
                    getLocalizedString("solution_tracking_paused"));
                break;

            case STOPPED:
                handleError(ErrorType.TRACKING_LOST, ErrorSeverity.ERROR,
                    getLocalizedString("error_tracking_stopped"),
                    getLocalizedString("solution_tracking_stopped"));
                break;

            case TRACKING:
                if (oldState != TrackingState.TRACKING) {
                    showSuccess(getLocalizedString("success_tracking_recovered"));
                }
                break;
        }

        if (callback != null) {
            callback.onTrackingStateChanged(oldState, newState);
        }
    }

    /**
     * Handle prolonged tracking loss
     */
    private void handleTrackingLostTooLong(long duration) {
        String message = String.format(
            getLocalizedString("error_tracking_lost_long"),
            duration / 1000);

        showWarning(message + " " + getLocalizedString("solution_tracking_lost_long"));
    }

    // ================================
    // 5. Device Movement Detection
    // ================================

    /**
     * Check for excessive device movement
     */
    public void checkDeviceMotion(Camera camera) {
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastMotionCheckTime < MOTION_CHECK_INTERVAL) {
            return;
        }

        lastMotionCheckTime = currentTime;

        float[] currentPosition = camera.getPose().getTranslation();

        if (lastCameraPosition[0] != 0 || lastCameraPosition[1] != 0 || lastCameraPosition[2] != 0) {
            float dx = currentPosition[0] - lastCameraPosition[0];
            float dy = currentPosition[1] - lastCameraPosition[1];
            float dz = currentPosition[2] - lastCameraPosition[2];

            float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

            // Calculate speed (meters per second)
            float speed = distance / (MOTION_CHECK_INTERVAL / 1000f);

            if (speed > EXCESSIVE_MOTION_THRESHOLD) {
                handleError(ErrorType.EXCESSIVE_MOTION, ErrorSeverity.WARNING,
                    getLocalizedString("error_excessive_motion"),
                    getLocalizedString("solution_excessive_motion"));
            }
        }

        // Update last position
        System.arraycopy(currentPosition, 0, lastCameraPosition, 0, 3);
    }

    // ================================
    // 6. Hardware Compatibility
    // ================================

    /**
     * Check hardware compatibility
     */
    public boolean checkHardwareCompatibility() {
        boolean compatible = true;

        // Check camera availability
        if (!context.getPackageManager().hasSystemFeature("android.hardware.camera")) {
            handleError(ErrorType.HARDWARE_ERROR, ErrorSeverity.CRITICAL,
                getLocalizedString("error_no_camera"),
                getLocalizedString("solution_no_camera"));
            compatible = false;
        }

        // Check gyroscope (recommended for AR)
        if (!context.getPackageManager().hasSystemFeature("android.hardware.sensor.gyroscope")) {
            handleError(ErrorType.HARDWARE_ERROR, ErrorSeverity.WARNING,
                getLocalizedString("warning_no_gyroscope"),
                getLocalizedString("solution_no_gyroscope"));
        }

        // Check accelerometer
        if (!context.getPackageManager().hasSystemFeature("android.hardware.sensor.accelerometer")) {
            handleError(ErrorType.HARDWARE_ERROR, ErrorSeverity.WARNING,
                getLocalizedString("warning_no_accelerometer"),
                getLocalizedString("solution_no_accelerometer"));
        }

        return compatible;
    }

    /**
     * Show device not compatible dialog
     */
    private void showDeviceNotCompatibleDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(getLocalizedString("dialog_device_not_compatible_title"));
        builder.setMessage(getLocalizedString("dialog_device_not_compatible_message"));
        builder.setPositiveButton(getLocalizedString("button_use_basic_mode"), (dialog, which) -> {
            if (callback != null) {
                callback.onFallbackToBasicMode();
            }
        });
        builder.setNegativeButton(getLocalizedString("button_exit"), (dialog, which) -> {
            if (callback != null) {
                callback.onExitRequested();
            }
        });
        builder.setCancelable(false);
        builder.show();
    }

    // ================================
    // 7. Network Connectivity
    // ================================

    /**
     * Check network connectivity
     */
    public boolean checkNetworkConnectivity() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        if (!isConnected) {
            handleError(ErrorType.NETWORK_UNAVAILABLE, ErrorSeverity.WARNING,
                getLocalizedString("error_network_unavailable"),
                getLocalizedString("solution_network_unavailable"));
        }

        return isConnected;
    }

    /**
     * Monitor network changes
     */
    public void onNetworkAvailable() {
        showSuccess(getLocalizedString("success_network_restored"));

        if (callback != null) {
            callback.onNetworkRestored();
        }
    }

    public void onNetworkLost() {
        handleError(ErrorType.NETWORK_UNAVAILABLE, ErrorSeverity.WARNING,
            getLocalizedString("error_network_lost"),
            getLocalizedString("solution_network_lost"));

        if (callback != null) {
            callback.onNetworkLost();
        }
    }

    // ================================
    // Common Error Handling
    // ================================

    /**
     * Generic error handler
     */
    private void handleError(ErrorType type, ErrorSeverity severity,
                            String message, String solution) {
        recordError(type, severity, message, null);
        showError(message, solution, severity);

        if (callback != null) {
            callback.onError(type, severity, message, solution);
        }
    }

    /**
     * Record error for tracking
     */
    private void recordError(ErrorType type, ErrorSeverity severity,
                            String message, Exception exception) {
        // Increment error count
        int count = errorCounts.getOrDefault(type, 0);
        errorCounts.put(type, count + 1);

        // Add to recent errors
        ErrorRecord record = new ErrorRecord(type, severity, message,
            System.currentTimeMillis(), exception);
        recentErrors.add(record);

        // Keep only recent errors
        while (recentErrors.size() > MAX_ERROR_HISTORY) {
            recentErrors.poll();
        }

        Log.e(TAG, String.format("[%s] %s: %s", severity, type, message), exception);
    }

    /**
     * Show error to user
     */
    private void showError(String message, String solution, ErrorSeverity severity) {
        String fullMessage = message;
        if (solution != null && !solution.isEmpty()) {
            fullMessage += "\n\n" + solution;
        }

        if (rootView != null) {
            int duration = severity == ErrorSeverity.CRITICAL ?
                Snackbar.LENGTH_INDEFINITE : Snackbar.LENGTH_LONG;

            Snackbar snackbar = Snackbar.make(rootView, fullMessage, duration);
            snackbar.setBackgroundTint(0xFFDC3545); // Red
            snackbar.setTextColor(0xFFFFFFFF);

            if (severity == ErrorSeverity.CRITICAL) {
                snackbar.setAction(getLocalizedString("button_dismiss"), v -> snackbar.dismiss());
            }

            snackbar.show();
        } else {
            Toast.makeText(context, fullMessage, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Show warning
     */
    private void showWarning(String message) {
        if (rootView != null) {
            Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT);
            snackbar.setBackgroundTint(0xFFFFC107); // Yellow
            snackbar.setTextColor(0xFF000000);
            snackbar.show();
        }
    }

    /**
     * Show success message
     */
    private void showSuccess(String message) {
        if (rootView != null) {
            Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT);
            snackbar.setBackgroundTint(0xFF28A745); // Green
            snackbar.setTextColor(0xFFFFFFFF);
            snackbar.show();
        }
    }

    /**
     * Show ARCore install dialog
     */
    private void showARCoreInstallDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(getLocalizedString("dialog_arcore_install_title"));
        builder.setMessage(getLocalizedString("dialog_arcore_install_message"));
        builder.setPositiveButton(getLocalizedString("button_install"), (dialog, which) -> {
            openPlayStore("com.google.ar.core");
        });
        builder.setNegativeButton(getLocalizedString("button_cancel"), null);
        builder.show();
    }

    /**
     * Show ARCore update dialog
     */
    private void showARCoreUpdateDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(getLocalizedString("dialog_arcore_update_title"));
        builder.setMessage(getLocalizedString("dialog_arcore_update_message"));
        builder.setPositiveButton(getLocalizedString("button_update"), (dialog, which) -> {
            openPlayStore("com.google.ar.core");
        });
        builder.setNegativeButton(getLocalizedString("button_later"), null);
        builder.show();
    }

    /**
     * Show camera not available dialog
     */
    private void showCameraNotAvailableDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(getLocalizedString("dialog_camera_unavailable_title"));
        builder.setMessage(getLocalizedString("dialog_camera_unavailable_message"));
        builder.setPositiveButton(getLocalizedString("button_retry"), (dialog, which) -> {
            if (callback != null) {
                callback.onRetryRequested();
            }
        });
        builder.setNegativeButton(getLocalizedString("button_exit"), (dialog, which) -> {
            if (callback != null) {
                callback.onExitRequested();
            }
        });
        builder.show();
    }

    /**
     * Open Play Store
     */
    private void openPlayStore(String packageName) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://details?id=" + packageName));
            context.startActivity(intent);
        } catch (Exception e) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
            context.startActivity(intent);
        }
    }

    // ================================
    // Localization
    // ================================

    /**
     * Get localized string
     */
    private String getLocalizedString(String key) {
        Map<String, String> strings = getLocalizedStrings();
        return strings.getOrDefault(key, key);
    }

    /**
     * Get localized strings for current language
     */
    private Map<String, String> getLocalizedStrings() {
        Map<String, String> strings = new HashMap<>();

        switch (languageCode) {
            case "en":
                // English strings
                strings.put("error_arcore_not_installed", "ARCore is not installed");
                strings.put("solution_install_arcore", "Please install ARCore from Play Store");
                strings.put("error_arcore_outdated", "ARCore needs to be updated");
                strings.put("solution_update_arcore", "Please update ARCore from Play Store");
                strings.put("error_device_not_compatible", "Your device doesn't support AR");
                strings.put("solution_device_not_compatible", "Switch to basic camera mode");
                strings.put("error_camera_permission_denied", "Camera permission is required");
                strings.put("solution_camera_permission_needed", "Please grant camera permission");
                strings.put("error_camera_permission_denied_permanent", "Camera permission was denied");
                strings.put("solution_camera_permission_settings", "Enable in Settings → Permissions");
                strings.put("error_camera_not_available", "Camera is not available");
                strings.put("solution_camera_not_available", "Close other apps using camera");
                strings.put("error_insufficient_lighting", "Lighting is too low");
                strings.put("solution_insufficient_lighting", "Move to a brighter area");
                strings.put("warning_lighting_too_bright", "Lighting is too bright, reduce glare");
                strings.put("error_tracking_paused", "AR tracking paused");
                strings.put("solution_tracking_paused", "Move device slowly to restore tracking");
                strings.put("error_tracking_stopped", "AR tracking stopped");
                strings.put("solution_tracking_stopped", "Point camera at a flat surface with texture");
                strings.put("error_tracking_lost_long", "Tracking lost for %d seconds");
                strings.put("solution_tracking_lost_long", "Restart the AR session");
                strings.put("error_excessive_motion", "Moving too fast");
                strings.put("solution_excessive_motion", "Move device slower and steadier");
                strings.put("error_network_unavailable", "No internet connection");
                strings.put("solution_network_unavailable", "Some features may be limited offline");
                strings.put("error_network_lost", "Internet connection lost");
                strings.put("solution_network_lost", "Reconnect to continue");
                strings.put("success_tracking_recovered", "AR tracking restored");
                strings.put("success_network_restored", "Internet connection restored");

                // Lighting quality
                strings.put("lighting_too_dark", "Too Dark");
                strings.put("lighting_too_bright", "Too Bright");
                strings.put("lighting_good", "Good");
                strings.put("lighting_excellent", "Excellent");
                strings.put("lighting_unknown", "Unknown");

                // Dialog strings
                strings.put("dialog_camera_permission_title", "Camera Permission Required");
                strings.put("dialog_camera_permission_rationale", "This app needs camera access to scan documents using AR technology.");
                strings.put("dialog_camera_permission_settings", "Camera permission is required. Please enable it in Settings.");
                strings.put("dialog_arcore_install_title", "Install ARCore");
                strings.put("dialog_arcore_install_message", "This app requires ARCore for AR features. Install now?");
                strings.put("dialog_arcore_update_title", "Update ARCore");
                strings.put("dialog_arcore_update_message", "ARCore needs to be updated for the best experience.");
                strings.put("dialog_device_not_compatible_title", "AR Not Supported");
                strings.put("dialog_device_not_compatible_message", "Your device doesn't support AR. Use basic camera mode instead?");
                strings.put("dialog_camera_unavailable_title", "Camera Unavailable");
                strings.put("dialog_camera_unavailable_message", "Camera is being used by another app. Please close other camera apps.");

                // Button strings
                strings.put("button_settings", "Settings");
                strings.put("button_grant_permission", "Grant Permission");
                strings.put("button_cancel", "Cancel");
                strings.put("button_dismiss", "Dismiss");
                strings.put("button_install", "Install");
                strings.put("button_update", "Update");
                strings.put("button_later", "Later");
                strings.put("button_retry", "Retry");
                strings.put("button_exit", "Exit");
                strings.put("button_use_basic_mode", "Use Basic Mode");

                // Hardware warnings
                strings.put("error_no_camera", "No camera detected");
                strings.put("solution_no_camera", "This device doesn't have a camera");
                strings.put("warning_no_gyroscope", "No gyroscope detected");
                strings.put("solution_no_gyroscope", "AR experience may be limited");
                strings.put("warning_no_accelerometer", "No accelerometer detected");
                strings.put("solution_no_accelerometer", "AR tracking may be affected");

                // SDK errors
                strings.put("error_sdk_too_old", "App needs to be updated");
                strings.put("solution_update_app", "Please update from Play Store");
                strings.put("error_user_declined_arcore", "ARCore installation declined");
                strings.put("solution_need_arcore", "ARCore is required for AR features");
                break;

            case "es":
                // Spanish strings (partial - add more as needed)
                strings.put("error_camera_permission_denied", "Se requiere permiso de cámara");
                strings.put("solution_camera_permission_needed", "Por favor otorgue permiso de cámara");
                strings.put("error_insufficient_lighting", "Iluminación insuficiente");
                strings.put("solution_insufficient_lighting", "Muévase a un área más iluminada");
                break;

            // Add more languages as needed
        }

        return strings;
    }

    /**
     * Set language
     */
    public void setLanguage(String languageCode) {
        this.languageCode = languageCode;
    }

    // ================================
    // Statistics
    // ================================

    /**
     * Get error statistics
     */
    public Map<ErrorType, Integer> getErrorStatistics() {
        return new HashMap<>(errorCounts);
    }

    /**
     * Get recent errors
     */
    public Queue<ErrorRecord> getRecentErrors() {
        return new LinkedList<>(recentErrors);
    }

    /**
     * Clear error history
     */
    public void clearErrorHistory() {
        errorCounts.clear();
        recentErrors.clear();
    }

    // ================================
    // Callbacks
    // ================================

    public void setCallback(ErrorCallback callback) {
        this.callback = callback;
    }

    public interface ErrorCallback {
        void onError(ErrorType type, ErrorSeverity severity, String message, String solution);
        void onTrackingStateChanged(TrackingState oldState, TrackingState newState);
        void onRequestPermission();
        void onPermissionPermanentlyDenied();
        void onFallbackToBasicMode();
        void onRetryRequested();
        void onExitRequested();
        void onNetworkRestored();
        void onNetworkLost();
    }

    // ================================
    // Data Classes
    // ================================

    public static class ErrorRecord {
        public ErrorType type;
        public ErrorSeverity severity;
        public String message;
        public long timestamp;
        public Exception exception;

        public ErrorRecord(ErrorType type, ErrorSeverity severity, String message,
                          long timestamp, Exception exception) {
            this.type = type;
            this.severity = severity;
            this.message = message;
            this.timestamp = timestamp;
            this.exception = exception;
        }
    }

    /**
     * Cleanup resources
     */
    public void cleanup() {
        callback = null;
        clearErrorHistory();

        Log.d(TAG, "ARErrorManager cleaned up");
    }
}


