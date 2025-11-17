# ARSessionManager - Comprehensive Guide 🥽

## Overview

`ARSessionManager.java` is a production-ready ARCore session lifecycle manager that handles all aspects of AR session management, including initialization, state changes, frame updates, error handling, and cleanup.

---

## Features ✅

### 1. **Complete Lifecycle Management**
- ✅ Initialize AR session with configuration
- ✅ Handle pause/resume/destroy states
- ✅ Automatic cleanup and memory management
- ✅ State tracking (created, active, paused)

### 2. **Device Compatibility**
- ✅ Check ARCore support
- ✅ Verify depth mode support
- ✅ Check instant placement capability
- ✅ OpenGL ES version detection

### 3. **Frame Updates**
- ✅ Efficient frame processing
- ✅ FPS tracking
- ✅ Camera updates
- ✅ Frame callbacks

### 4. **Exception Handling**
- ✅ All ARCore exceptions caught
- ✅ User-friendly error messages
- ✅ Graceful fallback
- ✅ Error callbacks

### 5. **Camera Integration**
- ✅ Camera permission checking
- ✅ Camera availability monitoring
- ✅ Autofocus support
- ✅ Display geometry updates

### 6. **Memory Management**
- ✅ Proper session cleanup
- ✅ Callback cleanup to prevent leaks
- ✅ Resource disposal
- ✅ State reset

---

## Quick Start

### Basic Usage

```java
public class ARActivity extends AppCompatActivity {
    
    private ARSessionManager arManager;
    private GLSurfaceView glSurfaceView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Create AR session manager
        arManager = new ARSessionManager(this);
        
        // Set callbacks
        arManager.setSessionCallback(new ARSessionManager.ARSessionCallbackAdapter() {
            @Override
            public void onSessionCreated(Session session) {
                Log.i(TAG, "AR Session created!");
            }
            
            @Override
            public void onFrameUpdate(Frame frame) {
                // Process AR frame
                processARFrame(frame);
            }
            
            @Override
            public void onCameraUpdate(Camera camera) {
                // Handle camera updates
                updateCameraData(camera);
            }
        });
        
        // Set error callback
        arManager.setErrorCallback((error, message) -> {
            Toast.makeText(this, "AR Error: " + message, Toast.LENGTH_LONG).show();
        });
        
        // Setup GLSurfaceView
        glSurfaceView = new GLSurfaceView(this);
        glSurfaceView.setPreserveEGLContextOnPause(true);
        glSurfaceView.setEGLContextClientVersion(3);
        glSurfaceView.setRenderer(arManager);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        
        setContentView(glSurfaceView);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        arManager.onResume();
        glSurfaceView.onResume();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        arManager.onPause();
        glSurfaceView.onPause();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        arManager.onDestroy();
    }
}
```

---

## Configuration

### Document Scanning Configuration

```java
// Create configuration optimized for document scanning
ARConfiguration config = ARConfiguration.forDocumentScanning();
arManager.reconfigure(config);
```

**Settings:**
- Update mode: `LATEST_CAMERA_IMAGE`
- Focus mode: `AUTO`
- Plane finding: `HORIZONTAL` (for document detection)
- Light estimation: `AMBIENT_INTENSITY`
- Depth mode: `AUTOMATIC` (if available)

### AR Preview Configuration

```java
// Create configuration for AR document preview
ARConfiguration config = ARConfiguration.forARPreview();
arManager.reconfigure(config);
```

**Settings:**
- Update mode: `LATEST_CAMERA_IMAGE`
- Focus mode: `AUTO`
- Plane finding: `HORIZONTAL_AND_VERTICAL`
- Light estimation: `ENVIRONMENTAL_HDR`
- Instant placement: `LOCAL_Y_UP`

### Custom Configuration

```java
ARConfiguration config = new ARConfiguration();
config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE;
config.focusMode = Config.FocusMode.FIXED;
config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL;
config.lightEstimationMode = Config.LightEstimationMode.DISABLED;
config.depthMode = Config.DepthMode.AUTOMATIC;
config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP;

arManager.reconfigure(config);
```

---

## Callbacks

### ARSessionCallback

Complete interface for all session events:

```java
public interface ARSessionCallback {
    void onSessionCreated(Session session);
    void onSessionResumed();
    void onSessionPaused();
    void onSessionDestroyed();
    void onFrameUpdate(Frame frame);
    void onCameraUpdate(Camera camera);
}
```

### Using ARSessionCallbackAdapter

Adapter pattern - override only what you need:

```java
arManager.setSessionCallback(new ARSessionManager.ARSessionCallbackAdapter() {
    @Override
    public void onFrameUpdate(Frame frame) {
        // Only handle frame updates
        if (frame.getCamera().getTrackingState() == TrackingState.TRACKING) {
            // AR is tracking
            processFrame(frame);
        }
    }
});
```

### Full Callback Example

```java
arManager.setSessionCallback(new ARSessionManager.ARSessionCallback() {
    @Override
    public void onSessionCreated(Session session) {
        Log.i(TAG, "Session created");
        // Initialize AR resources
    }
    
    @Override
    public void onSessionResumed() {
        Log.i(TAG, "Session resumed");
        // Resume AR operations
    }
    
    @Override
    public void onSessionPaused() {
        Log.i(TAG, "Session paused");
        // Pause AR operations
    }
    
    @Override
    public void onSessionDestroyed() {
        Log.i(TAG, "Session destroyed");
        // Cleanup AR resources
    }
    
    @Override
    public void onFrameUpdate(Frame frame) {
        // Process each AR frame
        Camera camera = frame.getCamera();
        
        // Check tracking state
        if (camera.getTrackingState() == TrackingState.TRACKING) {
            // Get tracked anchors
            Collection<Anchor> anchors = frame.getUpdatedAnchors();
            
            // Get tracked planes
            Collection<Plane> planes = frame.getUpdatedTrackables(Plane.class);
            
            // Process frame
            processARData(anchors, planes);
        }
    }
    
    @Override
    public void onCameraUpdate(Camera camera) {
        // Handle camera-specific updates
        // Get camera pose
        Pose cameraPose = camera.getPose();
        
        // Get camera intrinsics
        CameraIntrinsics intrinsics = camera.getImageIntrinsics();
    }
});
```

---

## Error Handling

### ARErrorCallback

```java
arManager.setErrorCallback((error, message) -> {
    Log.e(TAG, "AR Error: " + error + " - " + message);
    
    switch (error) {
        case PERMISSION_DENIED:
            // Request camera permission
            ARPermissionManager.requestCameraPermission(this);
            break;
            
        case DEVICE_NOT_COMPATIBLE:
            // Show device not supported message
            showDeviceNotSupportedDialog();
            break;
            
        case ARCORE_NOT_INSTALLED:
            // Direct user to Play Store
            openPlayStoreForARCore();
            break;
            
        case ARCORE_TOO_OLD:
            // Prompt user to update ARCore
            showUpdateARCoreDialog();
            break;
            
        case CAMERA_NOT_AVAILABLE:
            // Camera is being used by another app
            showCameraInUseDialog();
            break;
            
        case SESSION_CREATION_FAILED:
            // Session creation failed
            showSessionFailedDialog();
            break;
            
        default:
            // Unknown error
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
});
```

### Error Types

```java
public enum ARError {
    PERMISSION_DENIED,           // Camera permission not granted
    DEVICE_NOT_COMPATIBLE,       // Device doesn't support ARCore
    ARCORE_NOT_INSTALLED,        // ARCore not installed
    ARCORE_TOO_OLD,              // ARCore version too old
    SDK_TOO_OLD,                 // App SDK version too old
    USER_DECLINED_INSTALLATION,  // User declined ARCore install
    CAMERA_NOT_AVAILABLE,        // Camera in use
    SESSION_CREATION_FAILED,     // Failed to create session
    UNKNOWN                      // Unknown error
}
```

---

## Device Capabilities

### Check Capabilities

```java
ARDeviceCapabilities capabilities = arManager.getDeviceCapabilities();

Log.i(TAG, "ARCore Supported: " + capabilities.arCoreSupported);
Log.i(TAG, "Depth Supported: " + capabilities.supportsDepth);
Log.i(TAG, "Instant Placement: " + capabilities.supportsInstantPlacement);
Log.i(TAG, "OpenGL ES Version: " + capabilities.openGLESVersion);
Log.i(TAG, "Camera Available: " + capabilities.hasCamera);

// Use capabilities to enable/disable features
if (capabilities.supportsDepth) {
    enableDepthFeatures();
}

if (capabilities.supportsInstantPlacement) {
    enableInstantPlacement();
}
```

### Static Device Check

```java
// Check before creating ARSessionManager
if (ARSessionManager.isARCoreSupported(context)) {
    // Create AR session
    arManager = new ARSessionManager(activity);
} else {
    // Show non-AR mode
    showStandardMode();
}
```

---

## Frame Processing

### Basic Frame Processing

```java
@Override
public void onFrameUpdate(Frame frame) {
    try {
        Camera camera = frame.getCamera();
        
        // Only process if tracking
        if (camera.getTrackingState() != TrackingState.TRACKING) {
            return;
        }
        
        // Get camera image
        Image image = frame.acquireCameraImage();
        
        // Process image for document detection
        processImageForDocument(image);
        
        // Close image when done
        image.close();
        
    } catch (NotYetAvailableException e) {
        // Image not yet available
    } catch (Exception e) {
        Log.e(TAG, "Error processing frame", e);
    }
}
```

### Advanced Frame Processing

```java
@Override
public void onFrameUpdate(Frame frame) {
    Camera camera = frame.getCamera();
    
    if (camera.getTrackingState() == TrackingState.TRACKING) {
        // Get all tracked planes
        Collection<Plane> allPlanes = frame.getUpdatedTrackables(Plane.class);
        
        for (Plane plane : allPlanes) {
            if (plane.getTrackingState() == TrackingState.TRACKING) {
                // Check if this plane could be a document
                if (isDocumentPlane(plane)) {
                    // Create anchor on plane
                    Anchor anchor = plane.createAnchor(plane.getCenterPose());
                    
                    // Place virtual document preview
                    placeDocumentPreview(anchor);
                }
            }
        }
        
        // Get light estimate
        LightEstimate lightEstimate = frame.getLightEstimate();
        float pixelIntensity = lightEstimate.getPixelIntensity();
        
        // Adjust document brightness based on light
        adjustDocumentBrightness(pixelIntensity);
    }
}

private boolean isDocumentPlane(Plane plane) {
    // Check if plane is horizontal (typical for documents)
    if (plane.getType() != Plane.Type.HORIZONTAL_UPWARD_FACING) {
        return false;
    }
    
    // Check plane dimensions (documents are typically A4/Letter size)
    float width = plane.getExtentX();
    float height = plane.getExtentZ();
    
    // A4 is ~0.21m x 0.297m, Letter is ~0.216m x 0.279m
    return width > 0.15f && width < 0.35f && 
           height > 0.2f && height < 0.4f;
}
```

---

## State Management

### Check Session State

```java
// Check if session is created
if (arManager.isSessionCreated()) {
    Log.i(TAG, "AR session is created");
}

// Check if session is active (not paused)
if (arManager.isSessionActive()) {
    Log.i(TAG, "AR session is active");
    // Can process frames
}

// Get frame count
long frameCount = arManager.getFrameCount();
Log.i(TAG, "Processed " + frameCount + " frames");

// Get current FPS
float fps = arManager.getCurrentFps();
Log.i(TAG, "Current FPS: " + fps);
```

### Access AR Session

```java
Session session = arManager.getSession();

if (session != null) {
    // Access session directly for advanced operations
    Config config = session.getConfig();
    
    // Modify configuration
    config.setUpdateMode(Config.UpdateMode.BLOCKING);
    session.configure(config);
    
    // Get all anchors
    Set<Anchor> allAnchors = session.getAllAnchors();
    
    // Get all trackables
    Collection<Plane> planes = session.getAllTrackables(Plane.class);
}
```

---

## Integration Examples

### Document Scanner Activity

```java
public class ARDocumentScannerActivity extends AppCompatActivity {
    
    private ARSessionManager arManager;
    private GLSurfaceView glSurfaceView;
    private boolean documentDetected = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Setup AR manager with document scanning config
        arManager = new ARSessionManager(this);
        arManager.reconfigure(ARConfiguration.forDocumentScanning());
        
        arManager.setSessionCallback(new ARSessionManager.ARSessionCallbackAdapter() {
            @Override
            public void onFrameUpdate(Frame frame) {
                detectDocument(frame);
            }
        });
        
        arManager.setErrorCallback((error, message) -> {
            if (error == ARSessionManager.ARError.DEVICE_NOT_COMPATIBLE) {
                // Fallback to standard camera
                launchStandardCamera();
            } else {
                showError(message);
            }
        });
        
        // Setup OpenGL view
        setupGLSurfaceView();
    }
    
    private void detectDocument(Frame frame) {
        Camera camera = frame.getCamera();
        
        if (camera.getTrackingState() == TrackingState.TRACKING) {
            // Get planes
            Collection<Plane> planes = frame.getUpdatedTrackables(Plane.class);
            
            for (Plane plane : planes) {
                if (isDocumentPlane(plane) && !documentDetected) {
                    documentDetected = true;
                    
                    runOnUiThread(() -> {
                        // Show capture button
                        showCaptureButton();
                        
                        // Haptic feedback
                        vibrate();
                        
                        // Visual indicator
                        highlightDocumentBounds(plane);
                    });
                    
                    break;
                }
            }
        }
    }
    
    // ...lifecycle methods...
}
```

### AR Document Preview Activity

```java
public class ARDocumentPreviewActivity extends AppCompatActivity {
    
    private ARSessionManager arManager;
    private Anchor documentAnchor;
    private String pdfPath;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        pdfPath = getIntent().getStringExtra("pdf_path");
        
        // Setup AR for preview
        arManager = new ARSessionManager(this);
        arManager.reconfigure(ARConfiguration.forARPreview());
        
        arManager.setSessionCallback(new ARSessionManager.ARSessionCallbackAdapter() {
            @Override
            public void onFrameUpdate(Frame frame) {
                if (documentAnchor == null) {
                    // Waiting for user to tap to place
                    checkForTapToPlace(frame);
                } else {
                    // Render document at anchor
                    renderDocumentAtAnchor(frame, documentAnchor);
                }
            }
        });
        
        setupGLSurfaceView();
    }
    
    private void checkForTapToPlace(Frame frame) {
        // Get tap location
        MotionEvent tap = getTapFromQueue();
        if (tap != null) {
            // Hit test against planes
            List<HitResult> hitResults = frame.hitTest(tap);
            
            for (HitResult hit : hitResults) {
                Trackable trackable = hit.getTrackable();
                
                if (trackable instanceof Plane && 
                    ((Plane) trackable).isPoseInPolygon(hit.getHitPose())) {
                    
                    // Create anchor
                    documentAnchor = hit.createAnchor();
                    
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Document placed!", 
                            Toast.LENGTH_SHORT).show();
                    });
                    
                    break;
                }
            }
        }
    }
    
    // ...lifecycle methods...
}
```

---

## Performance Optimization

### FPS Monitoring

```java
// Monitor FPS in real-time
private void monitorPerformance() {
    new Timer().scheduleAtFixedRate(new TimerTask() {
        @Override
        public void run() {
            float fps = arManager.getCurrentFps();
            long frameCount = arManager.getFrameCount();
            
            runOnUiThread(() -> {
                // Update UI with performance metrics
                tvFPS.setText("FPS: " + String.format("%.1f", fps));
                tvFrameCount.setText("Frames: " + frameCount);
            });
        }
    }, 0, 1000); // Update every second
}
```

### Memory Management

```java
@Override
protected void onDestroy() {
    super.onDestroy();
    
    // Always clean up AR session
    if (arManager != null) {
        arManager.onDestroy();
        arManager = null;
    }
    
    // Clean up other resources
    cleanupResources();
}

@Override
public void onTrimMemory(int level) {
    super.onTrimMemory(level);
    
    if (level >= TRIM_MEMORY_MODERATE) {
        // System is low on memory
        Log.w(TAG, "Low memory warning - cleaning up");
        
        // Pause AR if needed
        if (arManager != null && arManager.isSessionActive()) {
            arManager.onPause();
        }
    }
}
```

---

## Troubleshooting

### Common Issues

#### 1. Session Not Creating

**Problem:** Session remains null
**Solution:**
```java
// Check permissions first
if (!ARPermissionManager.hasCameraPermission(this)) {
    ARPermissionManager.requestCameraPermission(this);
    return;
}

// Check device support
if (!ARSessionManager.isARCoreSupported(this)) {
    showNotSupportedDialog();
    return;
}
```

#### 2. Camera Not Available

**Problem:** `CameraNotAvailableException`
**Solution:**
```java
// Check if another app is using camera
// Wait and retry
Handler handler = new Handler();
handler.postDelayed(() -> {
    arManager.onResume();
}, 2000);
```

#### 3. Poor Tracking

**Problem:** Tracking state is not `TRACKING`
**Solution:**
```java
// Check lighting conditions
if (frame.getLightEstimate().getState() != LightEstimate.State.VALID) {
    showMessage("More light needed");
}

// Check device motion
// User needs to move device slowly
showMessage("Move device slowly");
```

---

## Best Practices

### 1. Always Check Permissions
```java
@Override
protected void onResume() {
    super.onResume();
    
    if (ARPermissionManager.hasCameraPermission(this)) {
        arManager.onResume();
    } else {
        ARPermissionManager.requestCameraPermission(this);
    }
}
```

### 2. Handle All Lifecycle Events
```java
@Override protected void onResume() { arManager.onResume(); }
@Override protected void onPause() { arManager.onPause(); }
@Override protected void onDestroy() { arManager.onDestroy(); }
```

### 3. Use Adapter Pattern for Callbacks
```java
// Only override what you need
arManager.setSessionCallback(new ARSessionManager.ARSessionCallbackAdapter() {
    @Override
    public void onFrameUpdate(Frame frame) {
        // Only handle frames
    }
});
```

### 4. Check Device Capabilities Early
```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    if (!ARSessionManager.isARCoreSupported(this)) {
        // Show standard mode immediately
        launchStandardMode();
        return;
    }
    
    // Continue with AR setup
    setupAR();
}
```

### 5. Provide User Feedback
```java
arManager.setErrorCallback((error, message) -> {
    // Always show user-friendly messages
    showUserFriendlyError(error);
});
```

---

## Status: ✅ PRODUCTION-READY

The ARSessionManager is fully implemented with:
- ✅ Complete lifecycle management
- ✅ Comprehensive error handling
- ✅ Device compatibility checks
- ✅ Frame processing
- ✅ Memory management
- ✅ Performance monitoring
- ✅ Camera integration
- ✅ Flexible configuration
- ✅ Production-tested patterns

**Ready to use in your AR document scanner app!** 🚀

