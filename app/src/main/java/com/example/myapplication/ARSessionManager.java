package com.example.myapplication;

import android.app.Activity;
import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * ARSessionManager - Comprehensive ARCore session lifecycle manager
 *
 * Responsibilities:
 * - Initialize and configure ARCore session
 * - Handle session state changes (pause/resume/destroy)
 * - Check device AR compatibility
 * - Manage AR frame updates and callbacks
 * - Exception handling for AR failures
 * - Camera permission integration
 * - Memory management and cleanup
 *
 * Usage:
 * <pre>
 * ARSessionManager arManager = new ARSessionManager(activity);
 * arManager.setSessionCallback(new ARSessionCallback() {
 *     public void onFrameUpdate(Frame frame) {
 *         // Process AR frame
 *     }
 * });
 * arManager.onResume();
 * </pre>
 */
public class ARSessionManager implements GLSurfaceView.Renderer {

    private static final String TAG = "ARSessionManager";

    // ARCore components
    private Session session;
    private Config config;
    private Activity activity;
    private Context context;

    // Session state
    private boolean installRequested = false;
    private boolean sessionCreated = false;
    private boolean sessionPaused = true;

    // Callbacks
    private ARSessionCallback sessionCallback;
    private ARErrorCallback errorCallback;

    // Configuration
    private ARConfiguration arConfiguration;

    // Frame tracking
    private long frameCount = 0;
    private long lastFrameTime = 0;
    private float currentFps = 0;

    /**
     * Constructor
     */
    public ARSessionManager(Activity activity) {
        this.activity = activity;
        this.context = activity.getApplicationContext();
        this.arConfiguration = new ARConfiguration(); // Default configuration
    }

    // ================================
    // Lifecycle Methods
    // ================================

    /**
     * Called when activity resumes - Initialize AR session
     */
    public void onResume() {
        Log.d(TAG, "onResume() called");

        // Check camera permission first
        if (!ARPermissionManager.hasCameraPermission(context)) {
            Log.w(TAG, "Camera permission not granted");
            notifyError(ARError.PERMISSION_DENIED, "Camera permission required for AR");
            return;
        }

        // Check if AR is supported
        ARPermissionManager.ArCoreAvailability availability =
            ARPermissionManager.checkArCoreAvailability(context);

        if (availability == ARPermissionManager.ArCoreAvailability.NOT_SUPPORTED) {
            Log.e(TAG, "ARCore is not supported on this device");
            notifyError(ARError.DEVICE_NOT_COMPATIBLE, "ARCore not supported");
            return;
        }

        // Try to create session if not already created
        if (session == null) {
            try {
                // Request ARCore installation if needed
                switch (ArCoreApk.getInstance().requestInstall(activity, !installRequested)) {
                    case INSTALL_REQUESTED:
                        installRequested = true;
                        Log.i(TAG, "ARCore installation requested");
                        return;
                    case INSTALLED:
                        Log.i(TAG, "ARCore is installed");
                        break;
                }

                // Create ARCore session
                createSession();

            } catch (UnavailableUserDeclinedInstallationException e) {
                Log.e(TAG, "User declined ARCore installation", e);
                notifyError(ARError.USER_DECLINED_INSTALLATION,
                    "ARCore installation required");
                return;
            } catch (Exception e) {
                Log.e(TAG, "Error during ARCore setup", e);
                notifyError(ARError.UNKNOWN, e.getMessage());
                return;
            }
        }

        // Resume session
        try {
            if (session != null && sessionPaused) {
                session.resume();
                sessionPaused = false;
                Log.i(TAG, "AR session resumed");
                notifySessionResumed();
            }
        } catch (CameraNotAvailableException e) {
            Log.e(TAG, "Camera not available", e);
            notifyError(ARError.CAMERA_NOT_AVAILABLE, "Camera is in use by another app");
            session = null;
        }
    }

    /**
     * Called when activity pauses - Pause AR session
     */
    public void onPause() {
        Log.d(TAG, "onPause() called");

        if (session != null && !sessionPaused) {
            session.pause();
            sessionPaused = true;
            Log.i(TAG, "AR session paused");
            notifySessionPaused();
        }
    }

    /**
     * Called when activity is destroyed - Clean up resources
     */
    public void onDestroy() {
        Log.d(TAG, "onDestroy() called");

        if (session != null) {
            try {
                session.close();
                session = null;
                sessionCreated = false;
                sessionPaused = true;
                Log.i(TAG, "AR session destroyed and cleaned up");
                notifySessionDestroyed();
            } catch (Exception e) {
                Log.e(TAG, "Error closing AR session", e);
            }
        }

        // Clear callbacks to prevent memory leaks
        sessionCallback = null;
        errorCallback = null;
    }

    // ================================
    // Session Creation & Configuration
    // ================================

    /**
     * Create and configure ARCore session
     */
    private void createSession() {
        try {
            Log.d(TAG, "Creating AR session...");

            // Create session
            session = new Session(context);

            // Configure session
            config = new Config(session);
            configureSession(config);
            session.configure(config);

            sessionCreated = true;
            sessionPaused = false;

            Log.i(TAG, "AR session created successfully");
            notifySessionCreated();

        } catch (UnavailableArcoreNotInstalledException e) {
            Log.e(TAG, "ARCore not installed", e);
            notifyError(ARError.ARCORE_NOT_INSTALLED,
                "Please install ARCore from Play Store");
        } catch (UnavailableApkTooOldException e) {
            Log.e(TAG, "ARCore APK too old", e);
            notifyError(ARError.ARCORE_TOO_OLD,
                "Please update ARCore from Play Store");
        } catch (UnavailableSdkTooOldException e) {
            Log.e(TAG, "ARCore SDK too old", e);
            notifyError(ARError.SDK_TOO_OLD,
                "Please update this app");
        } catch (UnavailableDeviceNotCompatibleException e) {
            Log.e(TAG, "Device not compatible", e);
            notifyError(ARError.DEVICE_NOT_COMPATIBLE,
                "This device does not support ARCore");
        } catch (Exception e) {
            Log.e(TAG, "Failed to create AR session", e);
            notifyError(ARError.SESSION_CREATION_FAILED, e.getMessage());
        }
    }

    /**
     * Configure AR session with settings
     */
    private void configureSession(Config config) {
        // Update mode - how ARCore delivers frames
        config.setUpdateMode(arConfiguration.updateMode);

        // Focus mode - autofocus or fixed
        config.setFocusMode(arConfiguration.focusMode);

        // Plane detection
        config.setPlaneFindingMode(arConfiguration.planeFindingMode);

        // Light estimation
        config.setLightEstimationMode(arConfiguration.lightEstimationMode);

        // Depth mode (if supported)
        if (arConfiguration.depthMode != Config.DepthMode.DISABLED) {
            if (session.isDepthModeSupported(arConfiguration.depthMode)) {
                config.setDepthMode(arConfiguration.depthMode);
                Log.d(TAG, "Depth mode enabled: " + arConfiguration.depthMode);
            } else {
                Log.w(TAG, "Depth mode not supported: " + arConfiguration.depthMode);
            }
        }

        // Instant placement (if supported)
        if (arConfiguration.instantPlacementMode != Config.InstantPlacementMode.DISABLED) {
            config.setInstantPlacementMode(arConfiguration.instantPlacementMode);
        }

        Log.d(TAG, "AR session configured with custom settings");
    }

    /**
     * Reconfigure session with new settings
     */
    public void reconfigure(ARConfiguration newConfiguration) {
        if (session != null) {
            this.arConfiguration = newConfiguration;
            configureSession(config);
            session.configure(config);
            Log.i(TAG, "AR session reconfigured");
        }
    }

    // ================================
    // Frame Updates
    // ================================

    /**
     * Update AR frame - Call this in your render loop
     */
    public Frame updateFrame() {
        if (session == null || sessionPaused) {
            return null;
        }

        try {
            // Update session and get latest frame
            Frame frame = session.update();

            // Track FPS
            updateFpsCounter();

            // Notify callback
            if (sessionCallback != null && frame != null) {
                sessionCallback.onFrameUpdate(frame);
            }

            frameCount++;
            return frame;

        } catch (CameraNotAvailableException e) {
            Log.e(TAG, "Camera not available during frame update", e);
            notifyError(ARError.CAMERA_NOT_AVAILABLE, "Camera lost during AR session");
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error updating AR frame", e);
            return null;
        }
    }

    /**
     * Update FPS counter
     */
    private void updateFpsCounter() {
        long currentTime = System.currentTimeMillis();
        if (lastFrameTime > 0) {
            long deltaTime = currentTime - lastFrameTime;
            if (deltaTime > 0) {
                currentFps = 1000.0f / deltaTime;
            }
        }
        lastFrameTime = currentTime;
    }

    // ================================
    // GLSurfaceView.Renderer Implementation
    // ================================

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Clear screen
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        Log.d(TAG, "OpenGL surface created");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        // Update session display geometry
        if (session != null) {
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            session.setDisplayGeometry(rotation, width, height);
            Log.d(TAG, "Display geometry updated: " + width + "x" + height);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // Clear screen
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Update AR frame
        Frame frame = updateFrame();

        if (frame != null) {
            // Process AR frame
            try {
                Camera camera = frame.getCamera();

                // Notify callback with camera data
                if (sessionCallback != null) {
                    sessionCallback.onCameraUpdate(camera);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error processing AR frame", e);
            }
        }
    }

    // ================================
    // Device Compatibility Checks
    // ================================

    /**
     * Check if device supports ARCore
     */
    public static boolean isARCoreSupported(Context context) {
        ARPermissionManager.ArCoreAvailability availability =
            ARPermissionManager.checkArCoreAvailability(context);
        return availability == ARPermissionManager.ArCoreAvailability.SUPPORTED;
    }

    /**
     * Check if depth mode is supported
     */
    public boolean isDepthSupported() {
        return session != null &&
               session.isDepthModeSupported(Config.DepthMode.AUTOMATIC);
    }

    /**
     * Check if instant placement is supported
     */
    public boolean isInstantPlacementSupported() {
        return session != null;
    }

    /**
     * Get device capabilities
     */
    public ARDeviceCapabilities getDeviceCapabilities() {
        ARDeviceCapabilities capabilities = new ARDeviceCapabilities();

        if (session != null) {
            capabilities.supportsDepth =
                session.isDepthModeSupported(Config.DepthMode.AUTOMATIC);
            capabilities.supportsInstantPlacement = true;
            capabilities.arCoreSupported = true;
        } else {
            capabilities.arCoreSupported = isARCoreSupported(context);
        }

        capabilities.openGLESVersion =
            ARPermissionManager.getOpenGLESVersion(context);
        capabilities.hasCamera =
            ARPermissionManager.hasCameraPermission(context);

        return capabilities;
    }

    // ================================
    // Getters and State Management
    // ================================

    /**
     * Get current AR session
     */
    public Session getSession() {
        return session;
    }

    /**
     * Check if session is created
     */
    public boolean isSessionCreated() {
        return sessionCreated && session != null;
    }

    /**
     * Check if session is active (not paused)
     */
    public boolean isSessionActive() {
        return sessionCreated && !sessionPaused && session != null;
    }

    /**
     * Get frame count
     */
    public long getFrameCount() {
        return frameCount;
    }

    /**
     * Get current FPS
     */
    public float getCurrentFps() {
        return currentFps;
    }

    /**
     * Get session configuration
     */
    public ARConfiguration getConfiguration() {
        return arConfiguration;
    }

    // ================================
    // Callbacks Management
    // ================================

    /**
     * Set session callback
     */
    public void setSessionCallback(ARSessionCallback callback) {
        this.sessionCallback = callback;
    }

    /**
     * Set error callback
     */
    public void setErrorCallback(ARErrorCallback callback) {
        this.errorCallback = callback;
    }

    // Notify callbacks
    private void notifySessionCreated() {
        if (sessionCallback != null) {
            activity.runOnUiThread(() -> sessionCallback.onSessionCreated(session));
        }
    }

    private void notifySessionResumed() {
        if (sessionCallback != null) {
            activity.runOnUiThread(() -> sessionCallback.onSessionResumed());
        }
    }

    private void notifySessionPaused() {
        if (sessionCallback != null) {
            activity.runOnUiThread(() -> sessionCallback.onSessionPaused());
        }
    }

    private void notifySessionDestroyed() {
        if (sessionCallback != null) {
            activity.runOnUiThread(() -> sessionCallback.onSessionDestroyed());
        }
    }

    private void notifyError(ARError error, String message) {
        Log.e(TAG, "AR Error: " + error + " - " + message);

        if (errorCallback != null) {
            activity.runOnUiThread(() -> errorCallback.onARError(error, message));
        } else {
            // Show default toast if no error callback
            activity.runOnUiThread(() ->
                Toast.makeText(context, "AR Error: " + message, Toast.LENGTH_LONG).show()
            );
        }
    }

    // ================================
    // Configuration Class
    // ================================

    /**
     * AR Configuration holder
     */
    public static class ARConfiguration {
        public Config.UpdateMode updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE;
        public Config.FocusMode focusMode = Config.FocusMode.AUTO;
        public Config.PlaneFindingMode planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL;
        public Config.LightEstimationMode lightEstimationMode = Config.LightEstimationMode.AMBIENT_INTENSITY;
        public Config.DepthMode depthMode = Config.DepthMode.DISABLED;
        public Config.InstantPlacementMode instantPlacementMode = Config.InstantPlacementMode.DISABLED;

        /**
         * Create default configuration for document scanning
         */
        public static ARConfiguration forDocumentScanning() {
            ARConfiguration config = new ARConfiguration();
            config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE;
            config.focusMode = Config.FocusMode.AUTO;
            config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL;
            config.lightEstimationMode = Config.LightEstimationMode.AMBIENT_INTENSITY;
            config.depthMode = Config.DepthMode.AUTOMATIC; // Enable if available
            return config;
        }

        /**
         * Create configuration for AR preview
         */
        public static ARConfiguration forARPreview() {
            ARConfiguration config = new ARConfiguration();
            config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE;
            config.focusMode = Config.FocusMode.AUTO;
            config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL;
            config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR;
            config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP;
            return config;
        }
    }

    // ================================
    // Callback Interfaces
    // ================================

    /**
     * Callback interface for AR session events
     */
    public interface ARSessionCallback {
        void onSessionCreated(Session session);
        void onSessionResumed();
        void onSessionPaused();
        void onSessionDestroyed();
        void onFrameUpdate(Frame frame);
        void onCameraUpdate(Camera camera);
    }

    /**
     * Adapter class for ARSessionCallback with empty implementations
     */
    public static class ARSessionCallbackAdapter implements ARSessionCallback {
        @Override public void onSessionCreated(Session session) {}
        @Override public void onSessionResumed() {}
        @Override public void onSessionPaused() {}
        @Override public void onSessionDestroyed() {}
        @Override public void onFrameUpdate(Frame frame) {}
        @Override public void onCameraUpdate(Camera camera) {}
    }

    /**
     * Error callback interface
     */
    public interface ARErrorCallback {
        void onARError(ARError error, String message);
    }

    // ================================
    // Error Types
    // ================================

    public enum ARError {
        PERMISSION_DENIED,
        DEVICE_NOT_COMPATIBLE,
        ARCORE_NOT_INSTALLED,
        ARCORE_TOO_OLD,
        SDK_TOO_OLD,
        USER_DECLINED_INSTALLATION,
        CAMERA_NOT_AVAILABLE,
        SESSION_CREATION_FAILED,
        UNKNOWN
    }

    // ================================
    // Device Capabilities
    // ================================

    public static class ARDeviceCapabilities {
        public boolean arCoreSupported;
        public boolean supportsDepth;
        public boolean supportsInstantPlacement;
        public float openGLESVersion;
        public boolean hasCamera;

        @Override
        public String toString() {
            return "ARDeviceCapabilities{" +
                    "arCoreSupported=" + arCoreSupported +
                    ", supportsDepth=" + supportsDepth +
                    ", supportsInstantPlacement=" + supportsInstantPlacement +
                    ", openGLESVersion=" + openGLESVersion +
                    ", hasCamera=" + hasCamera +
                    '}';
        }
    }
}

