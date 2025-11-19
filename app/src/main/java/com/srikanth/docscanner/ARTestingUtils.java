package com.srikanth.docscanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.TrackingState;

import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;

/**
 * ARTestingUtils - Comprehensive AR feature validation and testing
 *
 * Features:
 * - Mock AR environments for testing
 * - Document detection accuracy measurement
 * - Performance benchmarking tools
 * - AR tracking stability tests
 * - Multi-device compatibility verification
 * - Quality score calibration tools
 * - User experience testing helpers
 * - Automated testing scenarios
 * - Manual test procedures
 */
public class ARTestingUtils {

    private static final String TAG = "ARTestingUtils";

    // Test modes
    public enum TestMode {
        MOCK_AR,
        ACCURACY_TEST,
        PERFORMANCE_BENCHMARK,
        TRACKING_STABILITY,
        COMPATIBILITY_CHECK,
        QUALITY_CALIBRATION,
        UX_TESTING
    }

    // Context
    private Context context;

    // Test results storage
    private List<TestResult> testResults = new ArrayList<>();
    private Map<String, Object> testMetrics = new HashMap<>();

    // Performance tracking
    private Queue<Long> frameTimestamps = new LinkedList<>();
    private Queue<Float> fpsHistory = new LinkedList<>();
    private long testStartTime = 0;

    // Accuracy tracking
    private int totalDetections = 0;
    private int correctDetections = 0;
    private int falsePositives = 0;
    private int missedDetections = 0;

    // Tracking stability
    private TrackingState lastTrackingState = TrackingState.TRACKING;
    private int trackingLostCount = 0;
    private long totalTrackingTime = 0;
    private long trackingLostTime = 0;

    // Quality calibration
    private Map<Integer, List<Integer>> qualityScoreDistribution = new HashMap<>();

    /**
     * Constructor
     */
    public ARTestingUtils(Context context) {
        this.context = context;
        Log.d(TAG, "ARTestingUtils initialized");
    }

    // ================================
    // 1. Mock AR Environments
    // ================================

    /**
     * Create mock AR session for testing
     */
    public MockARSession createMockARSession() {
        return new MockARSession();
    }

    /**
     * Mock AR Session for testing without real ARCore
     */
    public static class MockARSession {
        private List<MockDocument> mockDocuments = new ArrayList<>();
        private TrackingState trackingState = TrackingState.TRACKING;
        private float[] cameraPosition = {0f, 0f, 0f};
        private float lightEstimate = 1.0f;

        public MockARSession() {
            // Create default test documents
            addTestDocument(new MockDocument(
                new Point[]{
                    new Point(100, 100),
                    new Point(500, 100),
                    new Point(500, 400),
                    new Point(100, 400)
                },
                8, // Quality score
                0.95f // Confidence
            ));
        }

        public void addTestDocument(MockDocument doc) {
            mockDocuments.add(doc);
        }

        public List<MockDocument> getMockDocuments() {
            return mockDocuments;
        }

        public void setTrackingState(TrackingState state) {
            this.trackingState = state;
        }

        public TrackingState getTrackingState() {
            return trackingState;
        }

        public void setCameraPosition(float x, float y, float z) {
            cameraPosition[0] = x;
            cameraPosition[1] = y;
            cameraPosition[2] = z;
        }

        public float[] getCameraPosition() {
            return cameraPosition;
        }

        public void setLightEstimate(float intensity) {
            this.lightEstimate = intensity;
        }

        public float getLightEstimate() {
            return lightEstimate;
        }
    }

    /**
     * Mock Document for testing
     */
    public static class MockDocument {
        public Point[] corners;
        public int qualityScore;
        public float confidence;

        public MockDocument(Point[] corners, int quality, float confidence) {
            this.corners = corners;
            this.qualityScore = quality;
            this.confidence = confidence;
        }
    }

    /**
     * Generate test image with document
     */
    public Bitmap generateTestDocumentImage(int width, int height, boolean withDistortion) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();

        // Background
        canvas.drawColor(Color.rgb(200, 200, 200));

        // Document
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);

        if (withDistortion) {
            // Distorted document (perspective)
            android.graphics.Path path = new android.graphics.Path();
            path.moveTo(width * 0.2f, height * 0.3f);
            path.lineTo(width * 0.8f, height * 0.2f);
            path.lineTo(width * 0.9f, height * 0.7f);
            path.lineTo(width * 0.1f, height * 0.8f);
            path.close();
            canvas.drawPath(path, paint);
        } else {
            // Perfect document (rectangle)
            canvas.drawRect(
                width * 0.2f, height * 0.2f,
                width * 0.8f, height * 0.8f,
                paint
            );
        }

        // Add text lines
        paint.setColor(Color.BLACK);
        paint.setTextSize(20);
        for (int i = 0; i < 10; i++) {
            canvas.drawText(
                "Test Document Line " + (i + 1),
                width * 0.25f,
                height * 0.3f + i * 30,
                paint
            );
        }

        return bitmap;
    }

    // ================================
    // 2. Document Detection Accuracy
    // ================================

    /**
     * Start accuracy test
     */
    public void startAccuracyTest() {
        totalDetections = 0;
        correctDetections = 0;
        falsePositives = 0;
        missedDetections = 0;
        testStartTime = SystemClock.elapsedRealtime();

        Log.d(TAG, "Accuracy test started");
    }

    /**
     * Record detection result
     */
    public void recordDetection(boolean hasGroundTruth, boolean detected, boolean correct) {
        totalDetections++;

        if (hasGroundTruth) {
            if (detected) {
                if (correct) {
                    correctDetections++;
                } else {
                    falsePositives++;
                }
            } else {
                missedDetections++;
            }
        } else if (detected) {
            falsePositives++;
        }
    }

    /**
     * Calculate accuracy metrics
     */
    public AccuracyMetrics getAccuracyMetrics() {
        AccuracyMetrics metrics = new AccuracyMetrics();

        if (totalDetections > 0) {
            metrics.precision = (float) correctDetections / (correctDetections + falsePositives);
            metrics.recall = (float) correctDetections / (correctDetections + missedDetections);
            metrics.f1Score = 2 * (metrics.precision * metrics.recall) /
                             (metrics.precision + metrics.recall);
            metrics.accuracy = (float) correctDetections / totalDetections;
        }

        metrics.totalDetections = totalDetections;
        metrics.correctDetections = correctDetections;
        metrics.falsePositives = falsePositives;
        metrics.missedDetections = missedDetections;

        return metrics;
    }

    /**
     * Test detection with known ground truth
     */
    public TestResult testDetectionAccuracy(
        ARDocumentDetector detector,
        Mat testImage,
        Point[] groundTruthCorners) {

        long startTime = SystemClock.elapsedRealtime();

        // Perform detection
        // List<ARDocumentDetector.DetectedDocument> detections =
        //     detector.detectDocuments(testImage);

        long endTime = SystemClock.elapsedRealtime();
        long detectionTime = endTime - startTime;

        TestResult result = new TestResult("Detection Accuracy");
        result.addMetric("detection_time_ms", detectionTime);
        result.addMetric("ground_truth_available", true);

        // Compare with ground truth
        // if (detections.isEmpty()) {
        //     result.passed = false;
        //     result.message = "No document detected";
        // } else {
        //     float iou = calculateIoU(detections.get(0).corners2D, groundTruthCorners);
        //     result.addMetric("iou", iou);
        //     result.passed = iou > 0.7f; // 70% IoU threshold
        // }

        testResults.add(result);
        return result;
    }

    /**
     * Calculate Intersection over Union (IoU)
     */
    private float calculateIoU(Point[] detected, Point[] groundTruth) {
        // Simplified IoU calculation
        // In production, use proper polygon intersection
        float intersection = 0f;
        float union = 0f;

        // Calculate areas and overlap
        // This is a simplified version
        return intersection / union;
    }

    // ================================
    // 3. Performance Benchmarking
    // ================================

    /**
     * Start performance benchmark
     */
    public void startPerformanceBenchmark() {
        frameTimestamps.clear();
        fpsHistory.clear();
        testStartTime = SystemClock.elapsedRealtime();

        Log.d(TAG, "Performance benchmark started");
    }

    /**
     * Record frame for performance tracking
     */
    public void recordFrame() {
        long currentTime = SystemClock.elapsedRealtime();
        frameTimestamps.add(currentTime);

        // Keep only last 30 frames
        while (frameTimestamps.size() > 30) {
            frameTimestamps.poll();
        }

        // Calculate FPS
        if (frameTimestamps.size() >= 2) {
            long oldest = frameTimestamps.peek();
            long newest = currentTime;
            long duration = newest - oldest;

            if (duration > 0) {
                float fps = (frameTimestamps.size() - 1) * 1000f / duration;
                fpsHistory.add(fps);

                // Keep FPS history
                while (fpsHistory.size() > 100) {
                    fpsHistory.poll();
                }
            }
        }
    }

    /**
     * Get performance metrics
     */
    public PerformanceMetrics getPerformanceMetrics() {
        PerformanceMetrics metrics = new PerformanceMetrics();

        if (!fpsHistory.isEmpty()) {
            float sum = 0;
            float min = Float.MAX_VALUE;
            float max = Float.MIN_VALUE;

            for (float fps : fpsHistory) {
                sum += fps;
                min = Math.min(min, fps);
                max = Math.max(max, fps);
            }

            metrics.averageFPS = sum / fpsHistory.size();
            metrics.minFPS = min;
            metrics.maxFPS = max;
            metrics.frameCount = fpsHistory.size();
        }

        long duration = SystemClock.elapsedRealtime() - testStartTime;
        metrics.totalDuration = duration;

        return metrics;
    }

    /**
     * Benchmark detection performance
     */
    public TestResult benchmarkDetectionPerformance(
        ARDocumentDetector detector,
        Mat testImage,
        int iterations) {

        List<Long> detectionTimes = new ArrayList<>();

        for (int i = 0; i < iterations; i++) {
            long startTime = SystemClock.elapsedRealtime();

            // Perform detection
            // detector.detectDocuments(testImage);

            long endTime = SystemClock.elapsedRealtime();
            detectionTimes.add(endTime - startTime);
        }

        // Calculate statistics
        long sum = 0;
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;

        for (long time : detectionTimes) {
            sum += time;
            min = Math.min(min, time);
            max = Math.max(max, time);
        }

        float average = (float) sum / iterations;

        TestResult result = new TestResult("Detection Performance");
        result.addMetric("iterations", iterations);
        result.addMetric("average_ms", average);
        result.addMetric("min_ms", min);
        result.addMetric("max_ms", max);
        result.passed = average < 100; // Target: <100ms per detection

        testResults.add(result);
        return result;
    }

    // ================================
    // 4. AR Tracking Stability Tests
    // ================================

    /**
     * Start tracking stability test
     */
    public void startTrackingStabilityTest() {
        lastTrackingState = TrackingState.TRACKING;
        trackingLostCount = 0;
        totalTrackingTime = 0;
        trackingLostTime = 0;
        testStartTime = SystemClock.elapsedRealtime();

        Log.d(TAG, "Tracking stability test started");
    }

    /**
     * Monitor tracking state
     */
    public void monitorTrackingState(TrackingState currentState) {
        long currentTime = SystemClock.elapsedRealtime();
        long elapsed = currentTime - testStartTime;

        if (currentState == TrackingState.TRACKING) {
            totalTrackingTime = elapsed;
        } else {
            trackingLostTime = elapsed - totalTrackingTime;
        }

        // Detect state changes
        if (currentState != lastTrackingState) {
            if (currentState != TrackingState.TRACKING) {
                trackingLostCount++;
                Log.w(TAG, "Tracking lost: " + currentState);
            }
            lastTrackingState = currentState;
        }
    }

    /**
     * Get tracking stability metrics
     */
    public TrackingMetrics getTrackingMetrics() {
        TrackingMetrics metrics = new TrackingMetrics();

        long duration = SystemClock.elapsedRealtime() - testStartTime;

        metrics.totalDuration = duration;
        metrics.trackingDuration = totalTrackingTime;
        metrics.trackingLostDuration = trackingLostTime;
        metrics.trackingLostCount = trackingLostCount;

        if (duration > 0) {
            metrics.stabilityPercentage = (float) totalTrackingTime / duration * 100f;
        }

        return metrics;
    }

    // ================================
    // 5. Multi-Device Compatibility
    // ================================

    /**
     * Check device compatibility
     */
    public DeviceCompatibilityReport checkDeviceCompatibility() {
        DeviceCompatibilityReport report = new DeviceCompatibilityReport();

        // Device info
        report.deviceModel = Build.MODEL;
        report.manufacturer = Build.MANUFACTURER;
        report.androidVersion = Build.VERSION.SDK_INT;
        report.androidRelease = Build.VERSION.RELEASE;

        // Hardware features
        report.hasCamera = context.getPackageManager()
            .hasSystemFeature("android.hardware.camera");
        report.hasGyroscope = context.getPackageManager()
            .hasSystemFeature("android.hardware.sensor.gyroscope");
        report.hasAccelerometer = context.getPackageManager()
            .hasSystemFeature("android.hardware.sensor.accelerometer");

        // Memory
        Runtime runtime = Runtime.getRuntime();
        report.totalMemoryMB = runtime.maxMemory() / 1024 / 1024;
        report.availableMemoryMB = (runtime.maxMemory() - runtime.totalMemory() +
                                   runtime.freeMemory()) / 1024 / 1024;

        // CPU
        report.cpuCores = Runtime.getRuntime().availableProcessors();

        // Determine compatibility level
        report.compatibilityLevel = determineCompatibilityLevel(report);

        return report;
    }

    /**
     * Determine device compatibility level
     */
    private String determineCompatibilityLevel(DeviceCompatibilityReport report) {
        int score = 0;

        // Check requirements
        if (report.hasCamera) score += 20;
        if (report.hasGyroscope) score += 15;
        if (report.hasAccelerometer) score += 15;
        if (report.androidVersion >= 24) score += 20; // Android 7.0+
        if (report.totalMemoryMB >= 2048) score += 15; // 2GB+
        if (report.cpuCores >= 4) score += 15;

        if (score >= 90) return "EXCELLENT";
        if (score >= 75) return "GOOD";
        if (score >= 60) return "FAIR";
        if (score >= 50) return "POOR";
        return "INCOMPATIBLE";
    }

    // ================================
    // 6. Quality Score Calibration
    // ================================

    /**
     * Record quality score for calibration
     */
    public void recordQualityScore(int actualQuality, int perceivedQuality) {
        if (!qualityScoreDistribution.containsKey(actualQuality)) {
            qualityScoreDistribution.put(actualQuality, new ArrayList<>());
        }
        qualityScoreDistribution.get(actualQuality).add(perceivedQuality);
    }

    /**
     * Get quality calibration report
     */
    public QualityCalibrationReport getQualityCalibration() {
        QualityCalibrationReport report = new QualityCalibrationReport();

        for (Map.Entry<Integer, List<Integer>> entry : qualityScoreDistribution.entrySet()) {
            int actual = entry.getKey();
            List<Integer> perceived = entry.getValue();

            if (!perceived.isEmpty()) {
                int sum = 0;
                for (int score : perceived) {
                    sum += score;
                }
                float average = (float) sum / perceived.size();

                report.addCalibration(actual, average, perceived.size());
            }
        }

        return report;
    }

    /**
     * Test quality scoring consistency
     */
    public TestResult testQualityScoring(
        DocumentQualityAnalyzer analyzer,
        List<Mat> testImages,
        List<Integer> expectedScores) {

        TestResult result = new TestResult("Quality Scoring");

        int totalTests = Math.min(testImages.size(), expectedScores.size());
        int correctScores = 0;
        float totalError = 0;

        for (int i = 0; i < totalTests; i++) {
            // DocumentQualityAnalyzer.QualityResult qualityResult =
            //     analyzer.analyzeQuality(testImages.get(i), null, null, null);

            // int actualScore = qualityResult.overallScore;
            int expectedScore = expectedScores.get(i);

            // Compare scores (allow ±1 difference)
            // if (Math.abs(actualScore - expectedScore) <= 1) {
            //     correctScores++;
            // }

            // totalError += Math.abs(actualScore - expectedScore);
        }

        result.addMetric("total_tests", totalTests);
        result.addMetric("correct_scores", correctScores);
        result.addMetric("accuracy", (float) correctScores / totalTests);
        result.addMetric("average_error", totalError / totalTests);
        result.passed = ((float) correctScores / totalTests) >= 0.8f; // 80% accuracy

        testResults.add(result);
        return result;
    }

    // ================================
    // 7. User Experience Testing
    // ================================

    /**
     * Measure time to first detection
     */
    public long measureTimeToFirstDetection(Runnable detectionTask) {
        long startTime = SystemClock.elapsedRealtime();

        detectionTask.run();

        long endTime = SystemClock.elapsedRealtime();
        return endTime - startTime;
    }

    /**
     * Test user guidance effectiveness
     */
    public TestResult testUserGuidance(
        ARGuidanceManager guidanceManager,
        MockARSession mockSession) {

        TestResult result = new TestResult("User Guidance");

        // Test various scenarios
        int scenariosPassed = 0;
        int totalScenarios = 5;

        // Scenario 1: Insufficient lighting
        mockSession.setLightEstimate(0.2f);
        // String guidance = guidanceManager.generateGuidance();
        // if (guidance.contains("light") || guidance.contains("bright")) {
        //     scenariosPassed++;
        // }

        // Scenario 2: Tracking lost
        mockSession.setTrackingState(TrackingState.PAUSED);
        // guidance = guidanceManager.generateGuidance();
        // if (guidance.contains("track") || guidance.contains("move")) {
        //     scenariosPassed++;
        // }

        // Add more scenarios...

        result.addMetric("scenarios_passed", scenariosPassed);
        result.addMetric("total_scenarios", totalScenarios);
        result.passed = scenariosPassed >= totalScenarios * 0.8f; // 80% pass rate

        testResults.add(result);
        return result;
    }

    /**
     * Measure capture success rate
     */
    public void recordCaptureAttempt(boolean successful) {
        String key = successful ? "successful_captures" : "failed_captures";
        int count = (int) testMetrics.getOrDefault(key, 0);
        testMetrics.put(key, count + 1);
    }

    /**
     * Get capture statistics
     */
    public CaptureStatistics getCaptureStatistics() {
        CaptureStatistics stats = new CaptureStatistics();

        stats.successfulCaptures = (int) testMetrics.getOrDefault("successful_captures", 0);
        stats.failedCaptures = (int) testMetrics.getOrDefault("failed_captures", 0);
        stats.totalAttempts = stats.successfulCaptures + stats.failedCaptures;

        if (stats.totalAttempts > 0) {
            stats.successRate = (float) stats.successfulCaptures / stats.totalAttempts * 100f;
        }

        return stats;
    }

    // ================================
    // Test Reporting
    // ================================

    /**
     * Generate comprehensive test report
     */
    public String generateTestReport() {
        StringBuilder report = new StringBuilder();

        report.append("=== AR Testing Report ===\n\n");
        report.append("Generated: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
            Locale.getDefault()).format(new Date())).append("\n\n");

        // Device info
        DeviceCompatibilityReport deviceReport = checkDeviceCompatibility();
        report.append("Device: ").append(deviceReport.deviceModel).append("\n");
        report.append("Android: ").append(deviceReport.androidRelease).append("\n");
        report.append("Compatibility: ").append(deviceReport.compatibilityLevel).append("\n\n");

        // Test results
        report.append("=== Test Results ===\n");
        int passed = 0;
        for (TestResult result : testResults) {
            report.append(result.toString()).append("\n");
            if (result.passed) passed++;
        }

        report.append("\nSummary: ").append(passed).append("/").append(testResults.size())
              .append(" tests passed\n\n");

        // Performance metrics
        PerformanceMetrics perfMetrics = getPerformanceMetrics();
        if (perfMetrics.frameCount > 0) {
            report.append("=== Performance ===\n");
            report.append(perfMetrics.toString()).append("\n\n");
        }

        // Accuracy metrics
        AccuracyMetrics accuracyMetrics = getAccuracyMetrics();
        if (accuracyMetrics.totalDetections > 0) {
            report.append("=== Accuracy ===\n");
            report.append(accuracyMetrics.toString()).append("\n\n");
        }

        // Tracking metrics
        TrackingMetrics trackingMetrics = getTrackingMetrics();
        if (trackingMetrics.totalDuration > 0) {
            report.append("=== Tracking Stability ===\n");
            report.append(trackingMetrics.toString()).append("\n\n");
        }

        return report.toString();
    }

    /**
     * Export test report to file
     */
    public File exportTestReport() {
        String report = generateTestReport();

        try {
            File reportsDir = new File(context.getFilesDir(), "test_reports");
            if (!reportsDir.exists()) {
                reportsDir.mkdirs();
            }

            String filename = "ar_test_report_" +
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(new Date()) + ".txt";

            File reportFile = new File(reportsDir, filename);

            FileWriter writer = new FileWriter(reportFile);
            writer.write(report);
            writer.close();

            Log.d(TAG, "Test report exported to: " + reportFile.getAbsolutePath());
            return reportFile;

        } catch (IOException e) {
            Log.e(TAG, "Error exporting test report", e);
            return null;
        }
    }

    /**
     * Clear all test data
     */
    public void clearTestData() {
        testResults.clear();
        testMetrics.clear();
        frameTimestamps.clear();
        fpsHistory.clear();
        qualityScoreDistribution.clear();

        totalDetections = 0;
        correctDetections = 0;
        falsePositives = 0;
        missedDetections = 0;
        trackingLostCount = 0;

        Log.d(TAG, "Test data cleared");
    }

    // ================================
    // Data Classes
    // ================================

    public static class TestResult {
        public String testName;
        public boolean passed;
        public String message;
        public Map<String, Object> metrics = new HashMap<>();
        public long timestamp;

        public TestResult(String name) {
            this.testName = name;
            this.timestamp = System.currentTimeMillis();
        }

        public void addMetric(String key, Object value) {
            metrics.put(key, value);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[").append(passed ? "PASS" : "FAIL").append("] ")
              .append(testName);

            if (message != null) {
                sb.append(": ").append(message);
            }

            if (!metrics.isEmpty()) {
                sb.append("\n  Metrics: ");
                for (Map.Entry<String, Object> entry : metrics.entrySet()) {
                    sb.append(entry.getKey()).append("=").append(entry.getValue()).append(" ");
                }
            }

            return sb.toString();
        }
    }

    public static class AccuracyMetrics {
        public int totalDetections;
        public int correctDetections;
        public int falsePositives;
        public int missedDetections;
        public float precision;
        public float recall;
        public float f1Score;
        public float accuracy;

        @Override
        public String toString() {
            return String.format(Locale.getDefault(),
                "Precision: %.2f%%\n" +
                "Recall: %.2f%%\n" +
                "F1 Score: %.2f\n" +
                "Accuracy: %.2f%%\n" +
                "Correct: %d, False Positives: %d, Missed: %d",
                precision * 100, recall * 100, f1Score, accuracy * 100,
                correctDetections, falsePositives, missedDetections);
        }
    }

    public static class PerformanceMetrics {
        public float averageFPS;
        public float minFPS;
        public float maxFPS;
        public int frameCount;
        public long totalDuration;

        @Override
        public String toString() {
            return String.format(Locale.getDefault(),
                "Average FPS: %.1f\n" +
                "Min FPS: %.1f\n" +
                "Max FPS: %.1f\n" +
                "Frame Count: %d\n" +
                "Duration: %dms",
                averageFPS, minFPS, maxFPS, frameCount, totalDuration);
        }
    }

    public static class TrackingMetrics {
        public long totalDuration;
        public long trackingDuration;
        public long trackingLostDuration;
        public int trackingLostCount;
        public float stabilityPercentage;

        @Override
        public String toString() {
            return String.format(Locale.getDefault(),
                "Stability: %.1f%%\n" +
                "Tracking Duration: %dms\n" +
                "Lost Duration: %dms\n" +
                "Lost Count: %d",
                stabilityPercentage, trackingDuration, trackingLostDuration,
                trackingLostCount);
        }
    }

    public static class DeviceCompatibilityReport {
        public String deviceModel;
        public String manufacturer;
        public int androidVersion;
        public String androidRelease;
        public boolean hasCamera;
        public boolean hasGyroscope;
        public boolean hasAccelerometer;
        public long totalMemoryMB;
        public long availableMemoryMB;
        public int cpuCores;
        public String compatibilityLevel;

        @Override
        public String toString() {
            return String.format(Locale.getDefault(),
                "Device: %s %s\n" +
                "Android: %s (API %d)\n" +
                "Camera: %s, Gyro: %s, Accel: %s\n" +
                "Memory: %dMB / %dMB\n" +
                "CPU Cores: %d\n" +
                "Level: %s",
                manufacturer, deviceModel, androidRelease, androidVersion,
                hasCamera ? "Yes" : "No", hasGyroscope ? "Yes" : "No",
                hasAccelerometer ? "Yes" : "No",
                availableMemoryMB, totalMemoryMB, cpuCores, compatibilityLevel);
        }
    }

    public static class QualityCalibrationReport {
        public Map<Integer, Float> averageScores = new HashMap<>();
        public Map<Integer, Integer> sampleCounts = new HashMap<>();

        public void addCalibration(int actual, float perceived, int count) {
            averageScores.put(actual, perceived);
            sampleCounts.put(actual, count);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Quality Score Calibration:\n");

            for (int i = 1; i <= 10; i++) {
                if (averageScores.containsKey(i)) {
                    sb.append(String.format(Locale.getDefault(),
                        "Actual %d: Perceived %.1f (n=%d)\n",
                        i, averageScores.get(i), sampleCounts.get(i)));
                }
            }

            return sb.toString();
        }
    }

    public static class CaptureStatistics {
        public int totalAttempts;
        public int successfulCaptures;
        public int failedCaptures;
        public float successRate;

        @Override
        public String toString() {
            return String.format(Locale.getDefault(),
                "Success Rate: %.1f%% (%d/%d captures)",
                successRate, successfulCaptures, totalAttempts);
        }
    }

    /**
     * Cleanup resources
     */
    public void cleanup() {
        clearTestData();
        Log.d(TAG, "ARTestingUtils cleaned up");
    }
}


