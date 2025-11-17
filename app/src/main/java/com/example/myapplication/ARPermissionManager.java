package com.example.myapplication;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

/**
 * ARPermissionManager - Handles runtime permissions and ARCore availability
 *
 * Features:
 * - Camera permission handling
 * - Storage permission handling
 * - ARCore availability checking
 * - Graceful fallback for non-ARCore devices
 * - OpenGL ES version checking
 */
public class ARPermissionManager {

    private static final String TAG = "ARPermissionManager";

    // Permission request codes
    public static final int CAMERA_PERMISSION_CODE = 100;
    public static final int STORAGE_PERMISSION_CODE = 101;
    public static final int ALL_PERMISSIONS_CODE = 102;

    // Required permissions
    private static final String[] CAMERA_PERMISSIONS = {
            Manifest.permission.CAMERA
    };

    private static final String[] STORAGE_PERMISSIONS_BELOW_33 = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private static final String[] STORAGE_PERMISSIONS_33_AND_ABOVE = {
            Manifest.permission.READ_MEDIA_IMAGES
    };

    /**
     * Check if camera permission is granted
     */
    public static boolean hasCameraPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Check if storage permissions are granted
     */
    public static boolean hasStoragePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            // Below Android 13
            return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED &&
                   ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * Check if all required permissions are granted
     */
    public static boolean hasAllPermissions(Context context) {
        return hasCameraPermission(context) && hasStoragePermission(context);
    }

    /**
     * Request camera permission
     */
    public static void requestCameraPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity, CAMERA_PERMISSIONS, CAMERA_PERMISSION_CODE);
    }

    /**
     * Request storage permissions
     */
    public static void requestStoragePermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(activity, STORAGE_PERMISSIONS_33_AND_ABOVE, STORAGE_PERMISSION_CODE);
        } else {
            ActivityCompat.requestPermissions(activity, STORAGE_PERMISSIONS_BELOW_33, STORAGE_PERMISSION_CODE);
        }
    }

    /**
     * Request all required permissions at once
     */
    public static void requestAllPermissions(Activity activity) {
        String[] allPermissions;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            allPermissions = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_MEDIA_IMAGES
            };
        } else {
            allPermissions = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        }

        ActivityCompat.requestPermissions(activity, allPermissions, ALL_PERMISSIONS_CODE);
    }

    /**
     * Handle permission request results
     */
    public static boolean handlePermissionResult(int requestCode, String[] permissions,
                                                 int[] grantResults, Context context) {
        if (grantResults.length == 0) {
            return false;
        }

        // Check if all permissions were granted
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, "Permission denied. Some features may not work.",
                        Toast.LENGTH_LONG).show();
                return false;
            }
        }

        Toast.makeText(context, "Permissions granted!", Toast.LENGTH_SHORT).show();
        return true;
    }

    // ================================
    // ARCore Availability Checking
    // ================================

    /**
     * Check if ARCore is supported on this device
     */
    public static ArCoreAvailability checkArCoreAvailability(Context context) {
        ArCoreApk.Availability availability = ArCoreApk.getInstance().checkAvailability(context);

        if (availability.isTransient()) {
            // Check again after a short delay
            return ArCoreAvailability.CHECKING;
        }

        if (availability.isSupported()) {
            return ArCoreAvailability.SUPPORTED;
        } else {
            return ArCoreAvailability.NOT_SUPPORTED;
        }
    }

    /**
     * Request ARCore installation if needed
     * Returns true if ARCore is ready, false if installation is needed
     */
    public static boolean requestArCoreInstallation(Activity activity) {
        try {
            ArCoreApk.InstallStatus installStatus = ArCoreApk.getInstance()
                    .requestInstall(activity, true);

            switch (installStatus) {
                case INSTALL_REQUESTED:
                    Log.i(TAG, "ARCore installation requested");
                    return false;
                case INSTALLED:
                    Log.i(TAG, "ARCore is installed");
                    return true;
            }
        } catch (UnavailableUserDeclinedInstallationException e) {
            Log.e(TAG, "User declined ARCore installation", e);
            Toast.makeText(activity, "ARCore installation required for AR features",
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Error requesting ARCore installation", e);
        }

        return false;
    }

    /**
     * Create ARCore session with error handling
     */
    public static Session createArSession(Activity activity) {
        Session session = null;
        String errorMessage = null;

        try {
            // Check if ARCore is installed
            if (!requestArCoreInstallation(activity)) {
                return null;
            }

            // Create session
            session = new Session(activity);

            // Configure session
            Config config = new Config(session);
            config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
            config.setFocusMode(Config.FocusMode.AUTO);
            session.configure(config);

            Log.i(TAG, "ARCore session created successfully");

        } catch (UnavailableArcoreNotInstalledException e) {
            errorMessage = "ARCore not installed. Please install ARCore from Play Store.";
            Log.e(TAG, errorMessage, e);
        } catch (UnavailableApkTooOldException e) {
            errorMessage = "ARCore APK is too old. Please update ARCore.";
            Log.e(TAG, errorMessage, e);
        } catch (UnavailableSdkTooOldException e) {
            errorMessage = "ARCore SDK is too old. Please update the app.";
            Log.e(TAG, errorMessage, e);
        } catch (UnavailableDeviceNotCompatibleException e) {
            errorMessage = "This device does not support ARCore.";
            Log.e(TAG, errorMessage, e);
        } catch (Exception e) {
            errorMessage = "Failed to create ARCore session: " + e.getMessage();
            Log.e(TAG, errorMessage, e);
        }

        if (errorMessage != null) {
            Toast.makeText(activity, errorMessage, Toast.LENGTH_LONG).show();
        }

        return session;
    }

    // ================================
    // OpenGL ES Version Checking
    // ================================

    /**
     * Check if device supports OpenGL ES 3.0
     */
    public static boolean supportsOpenGLES30(Context context) {
        return getOpenGLESVersion(context) >= 3.0f;
    }

    /**
     * Check if device supports OpenGL ES 2.0 (minimum requirement)
     */
    public static boolean supportsOpenGLES20(Context context) {
        return getOpenGLESVersion(context) >= 2.0f;
    }

    /**
     * Get OpenGL ES version
     */
    public static float getOpenGLESVersion(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        android.content.pm.ConfigurationInfo configInfo = am.getDeviceConfigurationInfo();

        // The version is a hex value with high 16 bits as major and low 16 bits as minor
        int majorVersion = (configInfo.reqGlEsVersion & 0xFFFF0000) >> 16;
        int minorVersion = configInfo.reqGlEsVersion & 0x0000FFFF;

        return majorVersion + (minorVersion / 10.0f);
    }

    /**
     * Get OpenGL ES version as string
     */
    public static String getOpenGLESVersionString(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        android.content.pm.ConfigurationInfo configInfo = am.getDeviceConfigurationInfo();
        return configInfo.getGlEsVersion();
    }

    // ================================
    // Device Capability Checking
    // ================================

    /**
     * Check if device has all required features for AR document scanning
     */
    public static DeviceCapabilities checkDeviceCapabilities(Context context) {
        DeviceCapabilities capabilities = new DeviceCapabilities();

        // Check camera
        capabilities.hasCamera = context.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA);
        capabilities.hasAutofocus = context.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS);

        // Check ARCore
        capabilities.arCoreAvailability = checkArCoreAvailability(context);

        // Check OpenGL ES
        capabilities.openGLESVersion = getOpenGLESVersion(context);
        capabilities.supportsOpenGLES30 = supportsOpenGLES30(context);
        capabilities.supportsOpenGLES20 = supportsOpenGLES20(context);

        // Check sensors (for AR)
        capabilities.hasAccelerometer = context.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER);
        capabilities.hasGyroscope = context.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_SENSOR_GYROSCOPE);

        return capabilities;
    }

    /**
     * Log device capabilities
     */
    public static void logDeviceCapabilities(Context context) {
        DeviceCapabilities caps = checkDeviceCapabilities(context);

        Log.i(TAG, "=== Device Capabilities ===");
        Log.i(TAG, "Camera: " + caps.hasCamera);
        Log.i(TAG, "Autofocus: " + caps.hasAutofocus);
        Log.i(TAG, "ARCore: " + caps.arCoreAvailability);
        Log.i(TAG, "OpenGL ES Version: " + caps.openGLESVersion);
        Log.i(TAG, "OpenGL ES 3.0: " + caps.supportsOpenGLES30);
        Log.i(TAG, "OpenGL ES 2.0: " + caps.supportsOpenGLES20);
        Log.i(TAG, "Accelerometer: " + caps.hasAccelerometer);
        Log.i(TAG, "Gyroscope: " + caps.hasGyroscope);
        Log.i(TAG, "===========================");
    }

    // ================================
    // Helper Classes
    // ================================

    public enum ArCoreAvailability {
        SUPPORTED,
        NOT_SUPPORTED,
        CHECKING
    }

    public static class DeviceCapabilities {
        public boolean hasCamera;
        public boolean hasAutofocus;
        public ArCoreAvailability arCoreAvailability;
        public float openGLESVersion;
        public boolean supportsOpenGLES30;
        public boolean supportsOpenGLES20;
        public boolean hasAccelerometer;
        public boolean hasGyroscope;

        public boolean isFullyCapable() {
            return hasCamera &&
                   hasAutofocus &&
                   arCoreAvailability == ArCoreAvailability.SUPPORTED &&
                   supportsOpenGLES30 &&
                   hasAccelerometer &&
                   hasGyroscope;
        }

        public boolean meetsMinimumRequirements() {
            return hasCamera && supportsOpenGLES20;
        }

        public String getCapabilitySummary() {
            StringBuilder summary = new StringBuilder();
            summary.append("Device Capabilities:\n");
            summary.append("✓ Camera: ").append(hasCamera ? "Yes" : "No").append("\n");
            summary.append("✓ Autofocus: ").append(hasAutofocus ? "Yes" : "No").append("\n");
            summary.append("✓ ARCore: ").append(arCoreAvailability).append("\n");
            summary.append("✓ OpenGL ES: ").append(openGLESVersion).append("\n");
            summary.append("✓ Accelerometer: ").append(hasAccelerometer ? "Yes" : "No").append("\n");
            summary.append("✓ Gyroscope: ").append(hasGyroscope ? "Yes" : "No");
            return summary.toString();
        }
    }
}

