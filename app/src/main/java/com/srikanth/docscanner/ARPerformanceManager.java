package com.srikanth.docscanner;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import org.opencv.core.Mat;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * ARPerformanceManager - Optimal AR experience management
 * 
 * Features:
 * - Frame rate monitoring and quality adjustment
 * - OpenCV processing optimization
 * - Memory management for AR objects
 * - Battery usage optimization
 * - Thermal throttling detection
 * - Device capability detection
 * - Background thread management
 * 
 * Target: 30fps AR rendering with <10% CPU overhead
 */
public class ARPerformanceManager {

    private static final String TAG = "ARPerformanceManager";

    // Performance targets
    private static final int TARGET_FPS = 30;
    private static final float MIN_FPS = 20f;
    private static final float MAX_FPS = 60f;
    private static final long FRAME_TIME_TARGET = 1000 / TARGET_FPS; // 33ms

    // CPU overhead target
    private static final float MAX_CPU_OVERHEAD = 0.10f; // 10%

    // Memory thresholds
    private static final long LOW_MEMORY_THRESHOLD = 50 * 1024 * 1024; // 50MB
    private static final long CRITICAL_MEMORY_THRESHOLD = 20 * 1024 * 1024; // 20MB

    // Battery thresholds
    private static final int LOW_BATTERY_LEVEL = 20;
    private static final float HIGH_TEMPERATURE = 40f; // Celsius

    // Quality levels
    public enum QualityLevel {
        ULTRA(1.0f, true, true, 4),
        HIGH(0.75f, true, true, 2),
        MEDIUM(0.5f, true, false, 2),
        LOW(0.35f, false, false, 1),
        MINIMAL(0.25f, false, false, 1);

        public final float resolutionScale;
        public final boolean enableOpenCVOptimizations;
        public final boolean enableAREffects;
        public final int maxDetectedDocuments;

        QualityLevel(float resolutionScale, boolean opencv, boolean effects, int maxDocs) {
            this.resolutionScale = resolutionScale;
            this.enableOpenCVOptimizations = opencv;
            this.enableAREffects = effects;
            this.maxDetectedDocuments = maxDocs;
        }
    }

    // Context
    private Context context;
    private ActivityManager activityManager;
    private PowerManager powerManager;

    // Frame rate tracking
    private Queue<Long> frameTimestamps = new LinkedList<>();
    private static final int FPS_SAMPLE_SIZE = 30;
    private float currentFPS = 0f;
    private long lastFrameTime = 0;
    private long frameDropCount = 0;

    // Quality management
    private QualityLevel currentQuality = QualityLevel.HIGH;
    private boolean autoQualityAdjustment = true;

    // Memory management
    private Runtime runtime = Runtime.getRuntime();
    private long lastMemoryCheck = 0;
    private static final long MEMORY_CHECK_INTERVAL = 1000; // 1 second

    // Battery management
    private int batteryLevel = 100;
    private boolean isCharging = false;
    private boolean lowPowerMode = false;

    // Thermal management
    private float currentTemperature = 0f;
    private boolean thermalThrottling = false;

    // Thread management
    private ExecutorService backgroundExecutor;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private Handler mainHandler;

    // Device capabilities
    private DeviceCapabilities deviceCapabilities;

    // Statistics
    private long totalFrames = 0;
    private long droppedFrames = 0;
    private float averageFrameTime = 0f;

    // Callbacks
    private PerformanceCallback callback;

    /**
     * Constructor
     */
    public ARPerformanceManager(Context context) {
        this.context = context;
        this.activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        this.powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        this.mainHandler = new Handler(Looper.getMainLooper());

        // Initialize components
        initializeThreadPool();
        detectDeviceCapabilities();
        checkBatteryStatus();
        
        Log.d(TAG, "ARPerformanceManager initialized");
    }

    // ================================
    // 1. Frame Rate Monitoring
    // ================================

    /**
     * Record frame render time
     */
    public void recordFrame(long frameTime) {
        totalFrames++;
        
        // Calculate frame delta
        if (lastFrameTime > 0) {
            long frameDelta = frameTime - lastFrameTime;
            
            // Update average frame time
            averageFrameTime = (averageFrameTime * 0.9f) + (frameDelta * 0.1f);
            
            // Check for dropped frames
            if (frameDelta > FRAME_TIME_TARGET * 1.5f) {
                droppedFrames++;
                frameDropCount++;
            }
        }
        
        lastFrameTime = frameTime;
        
        // Add to FPS calculation
        frameTimestamps.add(frameTime);
        while (frameTimestamps.size() > FPS_SAMPLE_SIZE) {
            frameTimestamps.poll();
        }
        
        // Calculate current FPS
        if (frameTimestamps.size() >= 2) {
            long oldest = frameTimestamps.peek();
            long newest = frameTime;
            long duration = newest - oldest;
            
            if (duration > 0) {
                currentFPS = (frameTimestamps.size() - 1) * 1000f / duration;
            }
        }
        
        // Check if quality adjustment needed
        if (autoQualityAdjustment && totalFrames % 30 == 0) {
            adjustQualityBasedOnPerformance();
        }
    }

    /**
     * Get current FPS
     */
    public float getCurrentFPS() {
        return currentFPS;
    }

    /**
     * Get average frame time in milliseconds
     */
    public float getAverageFrameTime() {
        return averageFrameTime;
    }

    /**
     * Check if meeting performance target
     */
    public boolean isMeetingPerformanceTarget() {
        return currentFPS >= TARGET_FPS * 0.9f; // 90% of target
    }

    // ================================
    // 2. Quality Adjustment
    // ================================

    /**
     * Adjust quality based on current performance
     */
    private void adjustQualityBasedOnPerformance() {
        // Check FPS
        if (currentFPS < MIN_FPS) {
            // Performance too low - decrease quality
            decreaseQuality();
        } else if (currentFPS > TARGET_FPS * 1.2f && currentQuality != QualityLevel.ULTRA) {
            // Performance excellent - try increasing quality
            increaseQuality();
        }
        
        // Check memory
        if (isMemoryLow()) {
            if (currentQuality != QualityLevel.LOW && currentQuality != QualityLevel.MINIMAL) {
                decreaseQuality();
            }
        }
        
        // Check battery
        if (batteryLevel < LOW_BATTERY_LEVEL && !isCharging) {
            lowPowerMode = true;
            if (currentQuality != QualityLevel.LOW && currentQuality != QualityLevel.MINIMAL) {
                setQuality(QualityLevel.LOW);
            }
        }
        
        // Check thermal
        if (thermalThrottling && currentQuality != QualityLevel.MINIMAL) {
            setQuality(QualityLevel.LOW);
        }
    }

    /**
     * Set quality level
     */
    public void setQuality(QualityLevel quality) {
        if (this.currentQuality != quality) {
            QualityLevel oldQuality = this.currentQuality;
            this.currentQuality = quality;
            
            Log.d(TAG, "Quality changed: " + oldQuality + " -> " + quality);
            
            if (callback != null) {
                callback.onQualityChanged(oldQuality, quality);
            }
        }
    }

    /**
     * Decrease quality level
     */
    private void decreaseQuality() {
        QualityLevel newQuality = currentQuality;
        
        switch (currentQuality) {
            case ULTRA:
                newQuality = QualityLevel.HIGH;
                break;
            case HIGH:
                newQuality = QualityLevel.MEDIUM;
                break;
            case MEDIUM:
                newQuality = QualityLevel.LOW;
                break;
            case LOW:
                newQuality = QualityLevel.MINIMAL;
                break;
        }
        
        setQuality(newQuality);
    }

    /**
     * Increase quality level
     */
    private void increaseQuality() {
        QualityLevel newQuality = currentQuality;
        
        switch (currentQuality) {
            case MINIMAL:
                newQuality = QualityLevel.LOW;
                break;
            case LOW:
                newQuality = QualityLevel.MEDIUM;
                break;
            case MEDIUM:
                newQuality = QualityLevel.HIGH;
                break;
            case HIGH:
                if (!lowPowerMode && !thermalThrottling) {
                    newQuality = QualityLevel.ULTRA;
                }
                break;
        }
        
        setQuality(newQuality);
    }

    /**
     * Get current quality level
     */
    public QualityLevel getCurrentQuality() {
        return currentQuality;
    }

    /**
     * Set auto quality adjustment
     */
    public void setAutoQualityAdjustment(boolean enabled) {
        this.autoQualityAdjustment = enabled;
    }

    // ================================
    // 3. OpenCV Optimization
    // ================================

    /**
     * Optimize Mat for processing
     */
    public Mat optimizeMatForProcessing(Mat input) {
        if (!currentQuality.enableOpenCVOptimizations) {
            return input;
        }
        
        // Downsample based on quality
        float scale = currentQuality.resolutionScale;
        
        if (scale < 1.0f) {
            int newWidth = (int) (input.width() * scale);
            int newHeight = (int) (input.height() * scale);
            
            Mat resized = new Mat();
            org.opencv.imgproc.Imgproc.resize(input, resized, 
                new org.opencv.core.Size(newWidth, newHeight));
            
            return resized;
        }
        
        return input;
    }

    /**
     * Should skip frame processing
     */
    public boolean shouldSkipFrame() {
        // Skip frames if performance is poor
        if (currentFPS < MIN_FPS) {
            return totalFrames % 2 == 0; // Process every other frame
        }
        
        return false;
    }

    /**
     * Get max documents to detect based on quality
     */
    public int getMaxDocumentsToDetect() {
        return currentQuality.maxDetectedDocuments;
    }

    // ================================
    // 4. Memory Management
    // ================================

    /**
     * Check memory status
     */
    public void checkMemoryStatus() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMemoryCheck < MEMORY_CHECK_INTERVAL) {
            return;
        }
        
        lastMemoryCheck = currentTime;
        
        long freeMemory = runtime.freeMemory();
        long totalMemory = runtime.totalMemory();
        long maxMemory = runtime.maxMemory();
        long usedMemory = totalMemory - freeMemory;
        long availableMemory = maxMemory - usedMemory;
        
        // Check if memory is low
        if (availableMemory < CRITICAL_MEMORY_THRESHOLD) {
            // Critical - force GC and reduce quality
            System.gc();
            if (currentQuality != QualityLevel.MINIMAL) {
                setQuality(QualityLevel.MINIMAL);
            }
            
            if (callback != null) {
                callback.onMemoryCritical(availableMemory);
            }
        } else if (availableMemory < LOW_MEMORY_THRESHOLD) {
            // Low memory - suggest GC
            if (callback != null) {
                callback.onMemoryLow(availableMemory);
            }
        }
    }

    /**
     * Check if memory is low
     */
    public boolean isMemoryLow() {
        long freeMemory = runtime.freeMemory();
        long totalMemory = runtime.totalMemory();
        long maxMemory = runtime.maxMemory();
        long availableMemory = maxMemory - (totalMemory - freeMemory);
        
        return availableMemory < LOW_MEMORY_THRESHOLD;
    }

    /**
     * Request garbage collection
     */
    public void requestGarbageCollection() {
        System.gc();
        Log.d(TAG, "Garbage collection requested");
    }

    /**
     * Get memory statistics
     */
    public MemoryStats getMemoryStats() {
        MemoryStats stats = new MemoryStats();
        stats.freeMemory = runtime.freeMemory();
        stats.totalMemory = runtime.totalMemory();
        stats.maxMemory = runtime.maxMemory();
        stats.usedMemory = stats.totalMemory - stats.freeMemory;
        stats.availableMemory = stats.maxMemory - stats.usedMemory;
        stats.usagePercentage = (float) stats.usedMemory / stats.maxMemory * 100f;
        
        return stats;
    }

    // ================================
    // 5. Battery Optimization
    // ================================

    /**
     * Check battery status
     */
    public void checkBatteryStatus() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, filter);
        
        if (batteryStatus != null) {
            // Battery level
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            batteryLevel = (int) ((level / (float) scale) * 100);
            
            // Charging status
            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL;
            
            // Temperature
            int temp = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
            if (temp > 0) {
                currentTemperature = temp / 10f; // Convert to Celsius
                
                // Check thermal throttling
                if (currentTemperature > HIGH_TEMPERATURE) {
                    if (!thermalThrottling) {
                        thermalThrottling = true;
                        Log.w(TAG, "Thermal throttling detected: " + currentTemperature + "°C");
                        
                        if (callback != null) {
                            callback.onThermalThrottling(currentTemperature);
                        }
                    }
                } else {
                    thermalThrottling = false;
                }
            }
        }
        
        // Enable low power mode if battery is low
        if (batteryLevel < LOW_BATTERY_LEVEL && !isCharging) {
            enableLowPowerMode();
        } else if (batteryLevel > LOW_BATTERY_LEVEL + 10) {
            disableLowPowerMode();
        }
    }

    /**
     * Enable low power mode
     */
    private void enableLowPowerMode() {
        if (!lowPowerMode) {
            lowPowerMode = true;
            setQuality(QualityLevel.LOW);
            
            Log.d(TAG, "Low power mode enabled");
            
            if (callback != null) {
                callback.onLowPowerModeChanged(true);
            }
        }
    }

    /**
     * Disable low power mode
     */
    private void disableLowPowerMode() {
        if (lowPowerMode) {
            lowPowerMode = false;
            
            Log.d(TAG, "Low power mode disabled");
            
            if (callback != null) {
                callback.onLowPowerModeChanged(false);
            }
        }
    }

    /**
     * Get battery level
     */
    public int getBatteryLevel() {
        return batteryLevel;
    }

    /**
     * Is charging
     */
    public boolean isCharging() {
        return isCharging;
    }

    // ================================
    // 6. Thermal Management
    // ================================

    /**
     * Is thermal throttling active
     */
    public boolean isThermalThrottling() {
        return thermalThrottling;
    }

    /**
     * Get current temperature
     */
    public float getCurrentTemperature() {
        return currentTemperature;
    }

    // ================================
    // 7. Device Capabilities
    // ================================

    /**
     * Detect device capabilities
     */
    private void detectDeviceCapabilities() {
        deviceCapabilities = new DeviceCapabilities();
        
        // RAM
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memInfo);
        deviceCapabilities.totalRAM = memInfo.totalMem;
        
        // CPU cores
        deviceCapabilities.cpuCores = Runtime.getRuntime().availableProcessors();
        
        // Android version
        deviceCapabilities.androidVersion = Build.VERSION.SDK_INT;
        
        // Device model
        deviceCapabilities.deviceModel = Build.MODEL;
        deviceCapabilities.manufacturer = Build.MANUFACTURER;
        
        // OpenGL ES version
        deviceCapabilities.openGLVersion = getOpenGLVersion();
        
        // ARCore support
        deviceCapabilities.supportsARCore = checkARCoreSupport();
        
        // Set initial quality based on capabilities
        if (deviceCapabilities.totalRAM < 2L * 1024 * 1024 * 1024) {
            // Low-end device (<2GB RAM)
            setQuality(QualityLevel.LOW);
        } else if (deviceCapabilities.totalRAM < 4L * 1024 * 1024 * 1024) {
            // Mid-range device (2-4GB RAM)
            setQuality(QualityLevel.MEDIUM);
        } else {
            // High-end device (>4GB RAM)
            setQuality(QualityLevel.HIGH);
        }
        
        Log.d(TAG, "Device capabilities detected: " + deviceCapabilities);
    }

    /**
     * Get OpenGL ES version
     */
    private String getOpenGLVersion() {
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            android.content.pm.ConfigurationInfo configInfo = am.getDeviceConfigurationInfo();
            return configInfo.getGlEsVersion();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    /**
     * Check ARCore support
     */
    private boolean checkARCoreSupport() {
        // Simplified check - would need proper ARCore availability check
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
    }

    /**
     * Get device capabilities
     */
    public DeviceCapabilities getDeviceCapabilities() {
        return deviceCapabilities;
    }

    // ================================
    // 8. Background Thread Management
    // ================================

    /**
     * Initialize thread pool
     */
    private void initializeThreadPool() {
        // Create thread pool based on CPU cores
        int cores = Runtime.getRuntime().availableProcessors();
        int poolSize = Math.max(2, cores - 1); // Leave one core for UI
        
        backgroundExecutor = Executors.newFixedThreadPool(poolSize);
        
        // Create background handler thread
        backgroundThread = new HandlerThread("ARPerformanceBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
        
        Log.d(TAG, "Thread pool initialized with " + poolSize + " threads");
    }

    /**
     * Execute task on background thread
     */
    public void executeOnBackground(Runnable task) {
        if (backgroundExecutor != null && !backgroundExecutor.isShutdown()) {
            backgroundExecutor.execute(task);
        }
    }

    /**
     * Post task to background handler
     */
    public void postToBackground(Runnable task) {
        if (backgroundHandler != null) {
            backgroundHandler.post(task);
        }
    }

    /**
     * Post task to main thread
     */
    public void postToMain(Runnable task) {
        mainHandler.post(task);
    }

    /**
     * Get active thread count
     */
    public int getActiveThreadCount() {
        if (backgroundExecutor instanceof ThreadPoolExecutor) {
            return ((ThreadPoolExecutor) backgroundExecutor).getActiveCount();
        }
        return 0;
    }

    // ================================
    // Statistics
    // ================================

    /**
     * Get performance statistics
     */
    public PerformanceStats getPerformanceStats() {
        PerformanceStats stats = new PerformanceStats();
        stats.currentFPS = currentFPS;
        stats.targetFPS = TARGET_FPS;
        stats.averageFrameTime = averageFrameTime;
        stats.totalFrames = totalFrames;
        stats.droppedFrames = droppedFrames;
        stats.dropRate = totalFrames > 0 ? (float) droppedFrames / totalFrames * 100f : 0f;
        stats.currentQuality = currentQuality;
        stats.batteryLevel = batteryLevel;
        stats.isCharging = isCharging;
        stats.temperature = currentTemperature;
        stats.thermalThrottling = thermalThrottling;
        stats.lowPowerMode = lowPowerMode;
        
        return stats;
    }

    /**
     * Reset statistics
     */
    public void resetStatistics() {
        totalFrames = 0;
        droppedFrames = 0;
        frameDropCount = 0;
        averageFrameTime = 0f;
        frameTimestamps.clear();
    }

    // ================================
    // Callbacks
    // ================================

    public void setCallback(PerformanceCallback callback) {
        this.callback = callback;
    }

    public interface PerformanceCallback {
        void onQualityChanged(QualityLevel oldQuality, QualityLevel newQuality);
        void onMemoryLow(long availableMemory);
        void onMemoryCritical(long availableMemory);
        void onThermalThrottling(float temperature);
        void onLowPowerModeChanged(boolean enabled);
        void onPerformanceWarning(String message);
    }

    // ================================
    // Data Classes
    // ================================

    public static class DeviceCapabilities {
        public long totalRAM;
        public int cpuCores;
        public int androidVersion;
        public String deviceModel;
        public String manufacturer;
        public String openGLVersion;
        public boolean supportsARCore;

        @Override
        public String toString() {
            return String.format("Device[RAM=%dMB, Cores=%d, Android=%d, Model=%s, GL=%s, ARCore=%b]",
                totalRAM / 1024 / 1024, cpuCores, androidVersion, deviceModel, 
                openGLVersion, supportsARCore);
        }
    }

    public static class MemoryStats {
        public long freeMemory;
        public long totalMemory;
        public long maxMemory;
        public long usedMemory;
        public long availableMemory;
        public float usagePercentage;

        @Override
        public String toString() {
            return String.format("Memory[Used=%dMB, Available=%dMB, Usage=%.1f%%]",
                usedMemory / 1024 / 1024, availableMemory / 1024 / 1024, usagePercentage);
        }
    }

    public static class PerformanceStats {
        public float currentFPS;
        public int targetFPS;
        public float averageFrameTime;
        public long totalFrames;
        public long droppedFrames;
        public float dropRate;
        public QualityLevel currentQuality;
        public int batteryLevel;
        public boolean isCharging;
        public float temperature;
        public boolean thermalThrottling;
        public boolean lowPowerMode;

        @Override
        public String toString() {
            return String.format("Performance[FPS=%.1f, FrameTime=%.1fms, Drops=%.1f%%, Quality=%s, Battery=%d%%]",
                currentFPS, averageFrameTime, dropRate, currentQuality, batteryLevel);
        }
    }

    // ================================
    // Cleanup
    // ================================

    /**
     * Cleanup resources
     */
    public void cleanup() {
        if (backgroundExecutor != null) {
            backgroundExecutor.shutdown();
        }
        
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
        }
        
        mainHandler.removeCallbacksAndMessages(null);
        callback = null;
        
        Log.d(TAG, "ARPerformanceManager cleaned up");
    }
}


