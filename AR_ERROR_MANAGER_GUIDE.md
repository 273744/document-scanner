# ARErrorManager Guide 🛡️⚠️

## Overview
Robust error handling system for AR document scanning with user-friendly guidance and automatic recovery.

## Features ✅
1. ✅ ARCore session interruption handling
2. ✅ Camera permission denial recovery
3. ✅ Lighting condition detection
4. ✅ AR tracking lost recovery
5. ✅ Device movement warnings
6. ✅ Hardware compatibility fallback
7. ✅ Network connectivity monitoring
8. ✅ Multi-language error messages

## Quick Start

```java
// Initialize
ARErrorManager errorManager = new ARErrorManager(context, rootView);

// Set callback
errorManager.setCallback(new ErrorCallback() {
    @Override
    public void onError(ErrorType type, ErrorSeverity severity, 
                       String message, String solution) {
        handleError(type, severity);
    }
});

// Check compatibility
if (!errorManager.checkARCoreAvailability()) {
    // Handle incompatibility
}
```

## Complete Integration

```java
public class ARCameraActivity extends AppCompatActivity {
    
    private ARErrorManager errorManager;
    private Session arSession;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize error manager
        errorManager = new ARErrorManager(this, findViewById(android.R.id.content));
        
        // Set language
        errorManager.setLanguage(Locale.getDefault().getLanguage());
        
        // Set callbacks
        setupErrorCallbacks();
        
        // Check hardware compatibility
        if (!errorManager.checkHardwareCompatibility()) {
            finish();
            return;
        }
        
        // Check ARCore availability
        if (!errorManager.checkARCoreAvailability()) {
            finish();
            return;
        }
        
        // Initialize AR session
        initializeARSession();
    }
    
    private void setupErrorCallbacks() {
        errorManager.setCallback(new ErrorCallback() {
            
            @Override
            public void onError(ErrorType type, ErrorSeverity severity, 
                               String message, String solution) {
                Log.e(TAG, "Error: " + type + " - " + message);
                
                if (severity == ErrorSeverity.CRITICAL) {
                    // Handle critical errors
                    pauseARSession();
                }
            }
            
            @Override
            public void onTrackingStateChanged(TrackingState oldState, 
                                              TrackingState newState) {
                updateTrackingUI(newState);
            }
            
            @Override
            public void onRequestPermission() {
                requestCameraPermission();
            }
            
            @Override
            public void onPermissionPermanentlyDenied() {
                finish();
            }
            
            @Override
            public void onFallbackToBasicMode() {
                switchToBasicCamera();
            }
            
            @Override
            public void onRetryRequested() {
                retryARSession();
            }
            
            @Override
            public void onExitRequested() {
                finish();
            }
            
            @Override
            public void onNetworkRestored() {
                resumeCloudFeatures();
            }
            
            @Override
            public void onNetworkLost() {
                pauseCloudFeatures();
            }
        });
    }
    
    private void initializeARSession() {
        try {
            arSession = new Session(this);
        } catch (Exception e) {
            errorManager.handleSessionException(e);
        }
    }
    
    @Override
    public void onDrawFrame(GL10 gl) {
        try {
            Frame frame = arSession.update();
            Camera camera = frame.getCamera();
            
            // Monitor tracking state
            errorManager.monitorTrackingState(camera);
            
            // Check lighting conditions
            errorManager.checkLightingConditions(frame);
            
            // Check device motion
            errorManager.checkDeviceMotion(camera);
            
            // Render AR content
            renderARContent(frame, camera);
            
        } catch (CameraNotAvailableException e) {
            errorManager.handleSessionException(e);
        }
    }
    
    @Override
    protected void onRequestPermissionsResult(int requestCode, String[] permissions, 
                                            int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            boolean granted = grantResults.length > 0 && 
                grantResults[0] == PackageManager.PERMISSION_GRANTED;
            
            if (!granted) {
                boolean shouldShow = shouldShowRequestPermissionRationale(
                    Manifest.permission.CAMERA);
                errorManager.handleCameraPermissionDenied(!shouldShow);
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        errorManager.cleanup();
    }
}
```

## 1. ARCore Session Errors

### Handle Session Exceptions

```java
try {
    arSession = new Session(context);
} catch (UnavailableArcoreNotInstalledException e) {
    // ARCore not installed
    errorManager.handleSessionException(e);
} catch (UnavailableApkTooOldException e) {
    // ARCore outdated
    errorManager.handleSessionException(e);
} catch (UnavailableDeviceNotCompatibleException e) {
    // Device not compatible
    errorManager.handleSessionException(e);
} catch (CameraNotAvailableException e) {
    // Camera in use
    errorManager.handleSessionException(e);
} catch (Exception e) {
    // Other errors
    errorManager.handleSessionException(e);
}
```

### Check Availability

```java
if (!errorManager.checkARCoreAvailability()) {
    // ARCore not available - handle gracefully
    showBasicCameraMode();
}
```

## 2. Camera Permission Handling

### Handle Permission Denial

```java
@Override
public void onRequestPermissionsResult(int requestCode, String[] permissions, 
                                      int[] grantResults) {
    if (requestCode == CAMERA_PERMISSION_REQUEST) {
        if (grantResults.length == 0 || 
            grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            
            // Check if permanently denied
            boolean permanentlyDenied = !shouldShowRequestPermissionRationale(
                Manifest.permission.CAMERA);
            
            errorManager.handleCameraPermissionDenied(permanentlyDenied);
        }
    }
}
```

### Permission Callback

```java
@Override
public void onRequestPermission() {
    // User clicked grant permission
    ActivityCompat.requestPermissions(this,
        new String[]{Manifest.permission.CAMERA},
        CAMERA_PERMISSION_REQUEST);
}

@Override
public void onPermissionPermanentlyDenied() {
    // User permanently denied - exit or use alternative
    Toast.makeText(this, "Camera required", Toast.LENGTH_LONG).show();
    finish();
}
```

## 3. Lighting Conditions

### Monitor Lighting

```java
@Override
public void onDrawFrame(GL10 gl) {
    Frame frame = arSession.update();
    
    // Check lighting every frame
    errorManager.checkLightingConditions(frame);
    
    // Get lighting quality description
    String lightingQuality = errorManager.getLightingQuality(frame);
    tvLightingStatus.setText(lightingQuality);
}
```

### Lighting Quality Levels

```
Too Dark:   < 0.3 intensity
Good:       0.3 - 0.7
Excellent:  0.7 - 1.05
Too Bright: > 1.05
```

### Handle Insufficient Lighting

```java
@Override
public void onError(ErrorType type, ErrorSeverity severity, 
                   String message, String solution) {
    if (type == ErrorType.INSUFFICIENT_LIGHTING) {
        // Show lighting guidance
        ivLightingIndicator.setImageResource(R.drawable.ic_lighting_low);
        tvLightingHint.setText("Move to brighter area");
    }
}
```

## 4. Tracking Lost Recovery

### Monitor Tracking State

```java
@Override
public void onDrawFrame(GL10 gl) {
    Frame frame = arSession.update();
    Camera camera = frame.getCamera();
    
    // Monitor tracking continuously
    errorManager.monitorTrackingState(camera);
}
```

### Tracking State Callbacks

```java
@Override
public void onTrackingStateChanged(TrackingState oldState, TrackingState newState) {
    switch (newState) {
        case TRACKING:
            // Tracking restored
            tvTrackingStatus.setText("Tracking");
            tvTrackingStatus.setTextColor(Color.GREEN);
            overlayView.setVisibility(View.VISIBLE);
            break;
            
        case PAUSED:
            // Tracking paused
            tvTrackingStatus.setText("Tracking Paused");
            tvTrackingStatus.setTextColor(Color.YELLOW);
            showMessage("Move device slowly");
            break;
            
        case STOPPED:
            // Tracking stopped
            tvTrackingStatus.setText("Tracking Lost");
            tvTrackingStatus.setTextColor(Color.RED);
            overlayView.setVisibility(View.INVISIBLE);
            showMessage("Point at textured surface");
            break;
    }
}
```

### Recovery Guidance

```
PAUSED → Move device slowly
STOPPED → Point at flat, textured surface
Lost >3s → Restart AR session
```

## 5. Device Movement Detection

### Monitor Motion

```java
@Override
public void onDrawFrame(GL10 gl) {
    Frame frame = arSession.update();
    Camera camera = frame.getCamera();
    
    // Check device motion
    errorManager.checkDeviceMotion(camera);
}
```

### Motion Thresholds

```
Normal:     < 0.5 m/s
Excessive:  > 0.5 m/s → Warning shown
Check interval: 100ms
```

### Handle Excessive Motion

```java
@Override
public void onError(ErrorType type, ErrorSeverity severity, 
                   String message, String solution) {
    if (type == ErrorType.EXCESSIVE_MOTION) {
        // Show slow down guidance
        ivMotionIndicator.setImageResource(R.drawable.ic_slow_down);
        animateSlowDownHint();
    }
}
```

## 6. Hardware Compatibility

### Check Compatibility

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    // Check hardware before initializing AR
    if (!errorManager.checkHardwareCompatibility()) {
        // Device missing required hardware
        // Error dialogs will be shown automatically
    }
}
```

### Handle Incompatibility

```java
@Override
public void onFallbackToBasicMode() {
    // User chose to use basic camera mode
    Intent intent = new Intent(this, CameraActivity.class);
    intent.putExtra("basic_mode", true);
    startActivity(intent);
    finish();
}
```

### Hardware Requirements

```
Required:
- Camera (without = critical error)

Recommended:
- Gyroscope (without = warning)
- Accelerometer (without = warning)
- ARCore support
```

## 7. Network Connectivity

### Monitor Network

```java
// Check initially
boolean hasNetwork = errorManager.checkNetworkConnectivity();

// Listen for network changes
private BroadcastReceiver networkReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
        boolean isConnected = checkNetworkConnection();
        
        if (isConnected) {
            errorManager.onNetworkAvailable();
        } else {
            errorManager.onNetworkLost();
        }
    }
};
```

### Network Callbacks

```java
@Override
public void onNetworkRestored() {
    // Network back online
    enableCloudFeatures();
    syncDocuments();
}

@Override
public void onNetworkLost() {
    // Network offline
    disableCloudFeatures();
    showOfflineMode();
}
```

## Error Types & Severities

### Error Types

```java
public enum ErrorType {
    ARCORE_NOT_INSTALLED,        // ARCore missing
    ARCORE_OUTDATED,             // ARCore needs update
    DEVICE_NOT_COMPATIBLE,       // Device doesn't support AR
    CAMERA_PERMISSION_DENIED,    // Camera permission denied
    CAMERA_NOT_AVAILABLE,        // Camera in use
    INSUFFICIENT_LIGHTING,       // Too dark
    TRACKING_LOST,               // AR tracking lost
    EXCESSIVE_MOTION,            // Moving too fast
    SESSION_INTERRUPTED,         // AR session interrupted
    NETWORK_UNAVAILABLE,         // No internet
    HARDWARE_ERROR,              // Hardware issue
    UNKNOWN                      // Unknown error
}
```

### Severity Levels

```java
public enum ErrorSeverity {
    INFO,      // FYI message
    WARNING,   // Warning, can continue
    ERROR,     // Error, needs action
    CRITICAL   // Critical, cannot continue
}
```

## Multi-Language Support

### Set Language

```java
// Set from device locale
Locale locale = Locale.getDefault();
errorManager.setLanguage(locale.getLanguage());

// Or set explicitly
errorManager.setLanguage("es"); // Spanish
```

### Supported Languages

```
en - English (full)
es - Spanish (partial)
fr - French (add as needed)
de - German (add as needed)
zh - Chinese (add as needed)
ja - Japanese (add as needed)
```

### Add New Language

```java
// In getLocalizedStrings() method
case "fr":
    strings.put("error_camera_permission_denied", "Permission caméra requise");
    strings.put("solution_camera_permission_needed", "Accordez l'autorisation");
    // Add more strings...
    break;
```

## Error Statistics

### Get Statistics

```java
// Get error counts by type
Map<ErrorType, Integer> stats = errorManager.getErrorStatistics();

for (Map.Entry<ErrorType, Integer> entry : stats.entrySet()) {
    Log.d(TAG, entry.getKey() + ": " + entry.getValue() + " occurrences");
}

// Get recent errors
Queue<ErrorRecord> recentErrors = errorManager.getRecentErrors();
for (ErrorRecord error : recentErrors) {
    Log.d(TAG, "Error: " + error.type + " at " + 
          new Date(error.timestamp));
}
```

### Clear History

```java
// Clear error tracking
errorManager.clearErrorHistory();
```

## UI Integration

### Error Display Options

```java
// Snackbar (default)
// - Shown at bottom
// - Auto-dismiss for warnings
// - Manual dismiss for critical errors
// - Color-coded by severity

// Toast (fallback)
// - Used if rootView is null
// - Simple text message

// Dialog (for critical errors)
// - Used for ARCore install/update
// - Used for camera permission
// - Used for device compatibility
```

### Custom Error UI

```java
@Override
public void onError(ErrorType type, ErrorSeverity severity, 
                   String message, String solution) {
    runOnUiThread(() -> {
        // Show custom error UI
        errorCard.setVisibility(View.VISIBLE);
        tvErrorMessage.setText(message);
        tvErrorSolution.setText(solution);
        
        // Set color by severity
        int color = getSeverityColor(severity);
        errorCard.setCardBackgroundColor(color);
    });
}

private int getSeverityColor(ErrorSeverity severity) {
    switch (severity) {
        case INFO:
            return 0xFF2196F3; // Blue
        case WARNING:
            return 0xFFFFC107; // Yellow
        case ERROR:
            return 0xFFFF9800; // Orange
        case CRITICAL:
            return 0xFFDC3545; // Red
        default:
            return 0xFF9E9E9E; // Gray
    }
}
```

## Best Practices

### 1. Initialize Early

```java
// Initialize error manager before AR session
errorManager = new ARErrorManager(context, rootView);
errorManager.setCallback(callback);

// Check compatibility first
if (!errorManager.checkHardwareCompatibility()) {
    return;
}

// Then initialize AR
initializeARSession();
```

### 2. Monitor Continuously

```java
@Override
public void onDrawFrame(GL10 gl) {
    // Monitor every frame
    errorManager.monitorTrackingState(camera);
    errorManager.checkDeviceMotion(camera);
    
    // Monitor periodically
    if (frameCount % 30 == 0) {
        errorManager.checkLightingConditions(frame);
    }
}
```

### 3. Handle Gracefully

```java
// Don't crash on errors
try {
    Frame frame = arSession.update();
} catch (CameraNotAvailableException e) {
    errorManager.handleSessionException(e);
    // Provide fallback or retry
}
```

### 4. Provide Guidance

```java
// Always provide solution
if (type == ErrorType.TRACKING_LOST) {
    showVisualGuidance();  // Arrows, animations
    speakGuidance();       // Voice instructions
    vibrate();             // Haptic feedback
}
```

## Troubleshooting

### Error Not Shown

```java
// Ensure rootView is set
errorManager = new ARErrorManager(context, findViewById(android.R.id.content));

// Or use Toast as fallback
errorManager = new ARErrorManager(context, null);
```

### Callback Not Called

```java
// Set callback before operations
errorManager.setCallback(callback);

// Check callback is not null
if (callback != null) {
    callback.onError(...);
}
```

### Language Not Working

```java
// Set language explicitly
errorManager.setLanguage("es");

// Add missing translations
// in getLocalizedStrings()
```

## Status: ✅ PRODUCTION-READY
- Handles all ARCore exceptions
- Camera permission recovery
- Lighting detection
- Tracking recovery
- Motion detection
- Hardware fallback
- Network monitoring
- Multi-language support
- User-friendly guidance

**Robust error handling for reliable AR experience!** 🛡️✨

