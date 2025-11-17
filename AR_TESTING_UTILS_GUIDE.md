# ARTestingUtils Guide 🧪✅

## Overview
Comprehensive AR feature validation and testing toolkit for document scanning with automated and manual testing procedures.

## Features ✅
1. ✅ Mock AR environments for testing
2. ✅ Document detection accuracy measurement
3. ✅ Performance benchmarking tools
4. ✅ AR tracking stability tests
5. ✅ Multi-device compatibility verification
6. ✅ Quality score calibration tools
7. ✅ User experience testing helpers
8. ✅ Automated test scenarios
9. ✅ Test report generation

## Quick Start

```java
// Initialize
ARTestingUtils testUtils = new ARTestingUtils(context);

// Run performance benchmark
testUtils.startPerformanceBenchmark();
// ... run your AR code ...
PerformanceMetrics metrics = testUtils.getPerformanceMetrics();

// Generate report
String report = testUtils.generateTestReport();
File reportFile = testUtils.exportTestReport();
```

## 1. Mock AR Environments

### Create Mock Session

```java
// Create mock AR session
MockARSession mockSession = testUtils.createMockARSession();

// Add test documents
mockSession.addTestDocument(new MockDocument(
    new Point[]{
        new Point(100, 100),
        new Point(500, 100),
        new Point(500, 400),
        new Point(100, 400)
    },
    8,      // Quality score
    0.95f   // Confidence
));

// Set tracking state
mockSession.setTrackingState(TrackingState.TRACKING);

// Set camera position
mockSession.setCameraPosition(0f, 0f, -1f);

// Set lighting
mockSession.setLightEstimate(0.8f);

// Get mock data
List<MockDocument> docs = mockSession.getMockDocuments();
TrackingState state = mockSession.getTrackingState();
float[] cameraPos = mockSession.getCameraPosition();
float light = mockSession.getLightEstimate();
```

### Generate Test Images

```java
// Generate perfect document
Bitmap perfectDoc = testUtils.generateTestDocumentImage(
    1920, 1080, false);

// Generate distorted document
Bitmap distortedDoc = testUtils.generateTestDocumentImage(
    1920, 1080, true);

// Use for testing
Mat testImage = new Mat();
Utils.bitmapToMat(perfectDoc, testImage);
```

### Mock Test Scenarios

```java
// Scenario 1: Perfect conditions
MockARSession perfect = testUtils.createMockARSession();
perfect.setTrackingState(TrackingState.TRACKING);
perfect.setLightEstimate(1.0f);
perfect.setCameraPosition(0f, 0f, -0.5f);

// Scenario 2: Poor lighting
MockARSession darkEnv = testUtils.createMockARSession();
darkEnv.setLightEstimate(0.2f);

// Scenario 3: Tracking lost
MockARSession trackingLost = testUtils.createMockARSession();
trackingLost.setTrackingState(TrackingState.PAUSED);

// Scenario 4: Moving camera
MockARSession moving = testUtils.createMockARSession();
moving.setCameraPosition(0.5f, 0.3f, -1.0f);
```

## 2. Document Detection Accuracy

### Start Accuracy Test

```java
// Initialize accuracy test
testUtils.startAccuracyTest();

// For each test case
for (TestCase testCase : testCases) {
    boolean detected = performDetection(testCase.image);
    boolean correct = verifyDetection(detected, testCase.groundTruth);
    
    testUtils.recordDetection(
        true,      // Has ground truth
        detected,  // Was detected
        correct    // Detection correct
    );
}

// Get results
AccuracyMetrics metrics = testUtils.getAccuracyMetrics();
Log.d(TAG, "Precision: " + metrics.precision);
Log.d(TAG, "Recall: " + metrics.recall);
Log.d(TAG, "F1 Score: " + metrics.f1Score);
Log.d(TAG, "Accuracy: " + metrics.accuracy);
```

### Test with Ground Truth

```java
// Prepare test data
Mat testImage = loadTestImage();
Point[] groundTruthCorners = new Point[]{
    new Point(100, 100),
    new Point(500, 100),
    new Point(500, 400),
    new Point(100, 400)
};

// Test detection accuracy
TestResult result = testUtils.testDetectionAccuracy(
    detector, 
    testImage, 
    groundTruthCorners
);

Log.d(TAG, "Test Result: " + result);
Log.d(TAG, "Detection Time: " + result.metrics.get("detection_time_ms") + "ms");
```

### Accuracy Metrics

```
Precision = TP / (TP + FP)
Recall = TP / (TP + FN)
F1 Score = 2 * (Precision * Recall) / (Precision + Recall)
Accuracy = TP / Total

Where:
TP = True Positives (correct detections)
FP = False Positives (incorrect detections)
FN = False Negatives (missed detections)
```

## 3. Performance Benchmarking

### Start Benchmark

```java
// Start performance tracking
testUtils.startPerformanceBenchmark();

// Your AR render loop
@Override
public void onDrawFrame(GL10 gl) {
    // Record each frame
    testUtils.recordFrame();
    
    // Your rendering code
    renderARContent();
}

// After testing period
PerformanceMetrics metrics = testUtils.getPerformanceMetrics();
Log.d(TAG, "Average FPS: " + metrics.averageFPS);
Log.d(TAG, "Min FPS: " + metrics.minFPS);
Log.d(TAG, "Max FPS: " + metrics.maxFPS);
```

### Benchmark Detection

```java
// Benchmark detection performance
Mat testImage = loadTestImage();

TestResult result = testUtils.benchmarkDetectionPerformance(
    detector,
    testImage,
    100  // 100 iterations
);

// Check results
float avgTime = (float) result.metrics.get("average_ms");
long minTime = (long) result.metrics.get("min_ms");
long maxTime = (long) result.metrics.get("max_ms");

Log.d(TAG, String.format("Detection: avg=%.1fms, min=%dms, max=%dms",
    avgTime, minTime, maxTime));

// Should be under 100ms for good performance
if (result.passed) {
    Log.d(TAG, "Performance target met!");
}
```

### Performance Targets

```
Excellent: >30 FPS average
Good:      25-30 FPS
Fair:      20-25 FPS
Poor:      <20 FPS

Detection Time Target: <100ms
Frame Time Target: <33ms (30 FPS)
```

## 4. AR Tracking Stability Tests

### Monitor Tracking

```java
// Start tracking stability test
testUtils.startTrackingStabilityTest();

// In your AR loop
@Override
public void onDrawFrame(GL10 gl) {
    Frame frame = arSession.update();
    Camera camera = frame.getCamera();
    
    // Monitor tracking state
    testUtils.monitorTrackingState(camera.getTrackingState());
    
    // Your code...
}

// After test period
TrackingMetrics metrics = testUtils.getTrackingMetrics();
Log.d(TAG, "Stability: " + metrics.stabilityPercentage + "%");
Log.d(TAG, "Lost Count: " + metrics.trackingLostCount);
Log.d(TAG, "Lost Duration: " + metrics.trackingLostDuration + "ms");
```

### Tracking Stability Criteria

```
Excellent: >95% stability
Good:      90-95% stability
Fair:      85-90% stability
Poor:      <85% stability

Lost Events: <5 per minute
Recovery Time: <1 second
```

### Test Tracking Recovery

```java
// Test tracking recovery
MockARSession mockSession = testUtils.createMockARSession();

// Simulate tracking loss
mockSession.setTrackingState(TrackingState.PAUSED);
testUtils.monitorTrackingState(TrackingState.PAUSED);

// Wait 1 second
Thread.sleep(1000);

// Simulate recovery
mockSession.setTrackingState(TrackingState.TRACKING);
testUtils.monitorTrackingState(TrackingState.TRACKING);

// Check if recovered quickly
TrackingMetrics metrics = testUtils.getTrackingMetrics();
boolean quickRecovery = metrics.trackingLostDuration < 1500; // <1.5s
```

## 5. Multi-Device Compatibility

### Check Compatibility

```java
// Run compatibility check
DeviceCompatibilityReport report = testUtils.checkDeviceCompatibility();

// Display results
Log.d(TAG, "Device: " + report.deviceModel);
Log.d(TAG, "Manufacturer: " + report.manufacturer);
Log.d(TAG, "Android: " + report.androidRelease);
Log.d(TAG, "Compatibility: " + report.compatibilityLevel);

// Check features
if (!report.hasCamera) {
    Log.e(TAG, "No camera available!");
}

if (!report.hasGyroscope) {
    Log.w(TAG, "No gyroscope - AR may be limited");
}

// Check resources
Log.d(TAG, "Memory: " + report.availableMemoryMB + "MB available");
Log.d(TAG, "CPU Cores: " + report.cpuCores);

// Adjust features based on compatibility
if (report.compatibilityLevel.equals("EXCELLENT")) {
    enableAdvancedFeatures();
} else if (report.compatibilityLevel.equals("POOR")) {
    enableBasicMode();
}
```

### Compatibility Levels

```
EXCELLENT (90-100 points):
- All hardware features
- 2GB+ RAM
- 4+ CPU cores
- Android 7.0+
- Gyroscope & Accelerometer

GOOD (75-89 points):
- Most features available
- Some hardware missing

FAIR (60-74 points):
- Basic features work
- Limited AR capabilities

POOR (50-59 points):
- Minimal functionality
- Consider fallback mode

INCOMPATIBLE (<50 points):
- Cannot run AR features
```

## 6. Quality Score Calibration

### Record Quality Scores

```java
// During testing, record quality scores
for (TestDocument testDoc : testDocuments) {
    // Perform detection
    int actualQuality = testDoc.getExpectedQuality();
    
    // Get detected quality
    QualityResult result = analyzer.analyzeQuality(
        testDoc.image, frame, camera, document);
    int perceivedQuality = result.overallScore;
    
    // Record for calibration
    testUtils.recordQualityScore(actualQuality, perceivedQuality);
}

// Get calibration report
QualityCalibrationReport calibration = testUtils.getQualityCalibration();
Log.d(TAG, calibration.toString());
```

### Test Quality Scoring

```java
// Prepare test set
List<Mat> testImages = new ArrayList<>();
List<Integer> expectedScores = new ArrayList<>();

// Add test cases
testImages.add(perfectImage);
expectedScores.add(10);

testImages.add(goodImage);
expectedScores.add(8);

testImages.add(fairImage);
expectedScores.add(6);

// Test quality scoring consistency
TestResult result = testUtils.testQualityScoring(
    analyzer,
    testImages,
    expectedScores
);

float accuracy = (float) result.metrics.get("accuracy");
Log.d(TAG, "Quality scoring accuracy: " + (accuracy * 100) + "%");
```

### Calibration Targets

```
Accuracy: >80% within ±1 point
Precision: <0.5 average error
Consistency: <10% variance
```

## 7. User Experience Testing

### Measure Time to First Detection

```java
// Measure how long until first detection
long timeToDetection = testUtils.measureTimeToFirstDetection(() -> {
    // Perform detection
    detector.processFrame(frame, camera);
});

Log.d(TAG, "Time to first detection: " + timeToDetection + "ms");

// Target: <2 seconds
if (timeToDetection < 2000) {
    Log.d(TAG, "Fast detection!");
}
```

### Test User Guidance

```java
// Test guidance system
MockARSession mockSession = testUtils.createMockARSession();

TestResult result = testUtils.testUserGuidance(
    guidanceManager,
    mockSession
);

int scenariosPassed = (int) result.metrics.get("scenarios_passed");
int totalScenarios = (int) result.metrics.get("total_scenarios");

Log.d(TAG, String.format("Guidance test: %d/%d scenarios passed",
    scenariosPassed, totalScenarios));
```

### Track Capture Success

```java
// During capture attempts
btnCapture.setOnClickListener(v -> {
    boolean success = performCapture();
    
    // Record result
    testUtils.recordCaptureAttempt(success);
});

// Get statistics
CaptureStatistics stats = testUtils.getCaptureStatistics();
Log.d(TAG, "Success Rate: " + stats.successRate + "%");
Log.d(TAG, "Successful: " + stats.successfulCaptures);
Log.d(TAG, "Failed: " + stats.failedCaptures);

// Target: >90% success rate
```

### UX Metrics

```
Time to First Detection: <2s
Capture Success Rate: >90%
Guidance Accuracy: >80%
User Satisfaction: Survey-based
```

## Complete Test Suite Example

```java
public class ARFeatureTestSuite {
    
    private ARTestingUtils testUtils;
    private ARDocumentDetector detector;
    private DocumentQualityAnalyzer analyzer;
    
    public void runCompleteTest() {
        testUtils = new ARTestingUtils(context);
        
        // 1. Device Compatibility
        testDeviceCompatibility();
        
        // 2. Performance Benchmark
        testPerformance();
        
        // 3. Detection Accuracy
        testAccuracy();
        
        // 4. Tracking Stability
        testTracking();
        
        // 5. Quality Calibration
        testQuality();
        
        // 6. UX Metrics
        testUserExperience();
        
        // Generate report
        generateReport();
    }
    
    private void testDeviceCompatibility() {
        DeviceCompatibilityReport report = 
            testUtils.checkDeviceCompatibility();
        
        Log.d(TAG, "=== Device Compatibility ===");
        Log.d(TAG, report.toString());
        
        if (report.compatibilityLevel.equals("INCOMPATIBLE")) {
            Log.e(TAG, "Device not compatible!");
            return;
        }
    }
    
    private void testPerformance() {
        Log.d(TAG, "=== Performance Test ===");
        
        testUtils.startPerformanceBenchmark();
        
        // Run for 30 seconds
        long endTime = System.currentTimeMillis() + 30000;
        while (System.currentTimeMillis() < endTime) {
            testUtils.recordFrame();
            // Simulate frame processing
            Thread.sleep(33); // ~30 FPS
        }
        
        PerformanceMetrics metrics = testUtils.getPerformanceMetrics();
        Log.d(TAG, metrics.toString());
        
        // Verify targets
        assertTrue("FPS too low", metrics.averageFPS >= 25f);
    }
    
    private void testAccuracy() {
        Log.d(TAG, "=== Accuracy Test ===");
        
        testUtils.startAccuracyTest();
        
        // Test with ground truth data
        for (int i = 0; i < 50; i++) {
            Mat testImage = generateTestImage(i);
            Point[] groundTruth = getGroundTruth(i);
            
            TestResult result = testUtils.testDetectionAccuracy(
                detector, testImage, groundTruth);
        }
        
        AccuracyMetrics metrics = testUtils.getAccuracyMetrics();
        Log.d(TAG, metrics.toString());
        
        // Verify targets
        assertTrue("Accuracy too low", metrics.accuracy >= 0.8f);
    }
    
    private void testTracking() {
        Log.d(TAG, "=== Tracking Stability Test ===");
        
        testUtils.startTrackingStabilityTest();
        
        // Simulate tracking for 60 seconds
        for (int i = 0; i < 60; i++) {
            TrackingState state = simulateTracking(i);
            testUtils.monitorTrackingState(state);
            Thread.sleep(1000);
        }
        
        TrackingMetrics metrics = testUtils.getTrackingMetrics();
        Log.d(TAG, metrics.toString());
        
        // Verify targets
        assertTrue("Stability too low", metrics.stabilityPercentage >= 90f);
    }
    
    private void testQuality() {
        Log.d(TAG, "=== Quality Calibration Test ===");
        
        List<Mat> testImages = loadTestImages();
        List<Integer> expectedScores = loadExpectedScores();
        
        TestResult result = testUtils.testQualityScoring(
            analyzer, testImages, expectedScores);
        
        Log.d(TAG, result.toString());
        
        float accuracy = (float) result.metrics.get("accuracy");
        assertTrue("Quality accuracy too low", accuracy >= 0.8f);
    }
    
    private void testUserExperience() {
        Log.d(TAG, "=== User Experience Test ===");
        
        // Test time to first detection
        long timeToDetection = testUtils.measureTimeToFirstDetection(() -> {
            performDetection();
        });
        
        Log.d(TAG, "Time to detection: " + timeToDetection + "ms");
        assertTrue("Detection too slow", timeToDetection < 2000);
        
        // Test capture success
        for (int i = 0; i < 20; i++) {
            boolean success = simulateCapture();
            testUtils.recordCaptureAttempt(success);
        }
        
        CaptureStatistics stats = testUtils.getCaptureStatistics();
        Log.d(TAG, stats.toString());
        
        assertTrue("Success rate too low", stats.successRate >= 90f);
    }
    
    private void generateReport() {
        String report = testUtils.generateTestReport();
        Log.d(TAG, report);
        
        File reportFile = testUtils.exportTestReport();
        Log.d(TAG, "Report saved to: " + reportFile.getAbsolutePath());
    }
}
```

## Test Report

### Generate Report

```java
// Generate comprehensive test report
String report = testUtils.generateTestReport();

// Display in UI
textView.setText(report);

// Export to file
File reportFile = testUtils.exportTestReport();

// Share report
Intent shareIntent = new Intent(Intent.ACTION_SEND);
shareIntent.setType("text/plain");
shareIntent.putExtra(Intent.EXTRA_STREAM, 
    FileProvider.getUriForFile(context, 
        context.getPackageName() + ".fileprovider", reportFile));
startActivity(Intent.createChooser(shareIntent, "Share Test Report"));
```

### Sample Report

```
=== AR Testing Report ===

Generated: 2025-11-17 18:45:23

Device: Pixel 6
Android: 13
Compatibility: EXCELLENT

=== Test Results ===
[PASS] Detection Accuracy: iou=0.85 detection_time_ms=75
[PASS] Detection Performance: average_ms=82.5 min_ms=65 max_ms=120
[PASS] Quality Scoring: accuracy=0.85 average_error=0.3
[FAIL] User Guidance: scenarios_passed=4/5

Summary: 3/4 tests passed

=== Performance ===
Average FPS: 29.5
Min FPS: 25.0
Max FPS: 31.0
Frame Count: 900
Duration: 30000ms

=== Accuracy ===
Precision: 90.00%
Recall: 85.00%
F1 Score: 0.87
Accuracy: 87.50%
Correct: 35, False Positives: 4, Missed: 6

=== Tracking Stability ===
Stability: 94.5%
Tracking Duration: 56700ms
Lost Duration: 3300ms
Lost Count: 3
```

## Manual Test Procedures

### Test Checklist

```
□ Device Compatibility
  □ Check device model compatibility
  □ Verify all sensors present
  □ Check available memory
  □ Test on multiple devices

□ Detection Accuracy
  □ Test with various document types
  □ Test with different lighting
  □ Test with perspective angles
  □ Measure false positive rate

□ Performance
  □ Measure FPS during operation
  □ Check frame drop count
  □ Monitor memory usage
  □ Test battery consumption

□ Tracking Stability
  □ Test tracking in various conditions
  □ Measure tracking loss frequency
  □ Test recovery procedures
  □ Verify stability percentage

□ Quality Scoring
  □ Test with known quality documents
  □ Verify score consistency
  □ Check calibration accuracy
  □ Test edge cases

□ User Experience
  □ Measure time to first detection
  □ Track capture success rate
  □ Test guidance effectiveness
  □ Gather user feedback
```

## Best Practices

### 1. Test Early and Often

```java
// Integrate testing into development
@Before
public void setUp() {
    testUtils = new ARTestingUtils(context);
}

@Test
public void testDetectionPerformance() {
    TestResult result = testUtils.benchmarkDetectionPerformance(
        detector, testImage, 100);
    assertTrue(result.passed);
}
```

### 2. Use Realistic Test Data

```java
// Use real-world test cases
Mat realDocument = loadRealDocument();
Mat poorLighting = loadDarkImage();
Mat distorted = loadDistortedImage();

// Test all scenarios
testAllScenarios(new Mat[]{realDocument, poorLighting, distorted});
```

### 3. Track Metrics Over Time

```java
// Save metrics for trending
testUtils.exportTestReport();

// Compare with previous results
compareWithBaseline(currentMetrics, baselineMetrics);
```

### 4. Test on Multiple Devices

```java
// Test device matrix
String[] devices = {"Pixel 6", "Samsung S21", "OnePlus 9"};

for (String device : devices) {
    runTestSuiteOnDevice(device);
}
```

## Status: ✅ PRODUCTION-READY
- Mock AR environments
- Accuracy measurement (Precision, Recall, F1)
- Performance benchmarking (FPS, timing)
- Tracking stability tests
- Device compatibility checks
- Quality calibration tools
- UX testing helpers
- Automated test scenarios
- Comprehensive reporting

**Complete AR testing toolkit for validation and QA!** 🧪✅✨

