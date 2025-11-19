package com.srikanth.docscanner;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Test Category 7: Edge Case Handling & Error Recovery Tests
 */
@RunWith(AndroidJUnit4.class)
public class EdgeCaseHandlingTest {

    private Context context;
    private FeatureAnalytics analytics;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        analytics = FeatureAnalytics.getInstance(context);
        analytics.setUserConsent(true);
    }

    @After
    public void tearDown() {
        analytics.cleanup();
    }

    @Test
    public void testEdgeCase_NullValues_Handled() {
        // Should not crash with null error message
        FeatureAnalytics.CloudSyncMetrics metrics = new FeatureAnalytics.CloudSyncMetrics();
        metrics.timestamp = System.currentTimeMillis();
        metrics.success = false;
        metrics.filesCount = 0;
        metrics.bytesTransferred = 0;
        metrics.duration = 100;
        metrics.errorMessage = null; // Null error message

        analytics.trackCloudSync(metrics);

        // Should complete without exception
        FeatureAnalytics.CloudSyncStatistics stats = analytics.getCloudSyncStatistics();
        assertNotNull(stats);
    }

    @Test
    public void testEdgeCase_ZeroValues_Handled() {
        FeatureAnalytics.OCRMetrics metrics = new FeatureAnalytics.OCRMetrics();
        metrics.timestamp = System.currentTimeMillis();
        metrics.processingTime = 0;
        metrics.accuracy = 0.0f;
        metrics.success = false;
        metrics.characterCount = 0;

        analytics.trackOCR(metrics);

        FeatureAnalytics.OCRStatistics stats = analytics.getOCRStatistics();
        assertEquals(0, stats.averageProcessingTime);
    }

    @Test
    public void testEdgeCase_NegativeValues_Handled() {
        FeatureAnalytics.ResourceMetrics metrics = new FeatureAnalytics.ResourceMetrics();
        metrics.timestamp = System.currentTimeMillis();
        metrics.memoryUsage = 0.5f;
        metrics.batteryLevel = -1; // Unknown battery level

        // Should handle negative battery level gracefully
        FeatureAnalytics.ResourceStatistics stats = analytics.getResourceStatistics();
        assertNotNull(stats);
    }

    @Test
    public void testEdgeCase_VeryLargeValues_Handled() {
        FeatureAnalytics.CloudSyncMetrics metrics = new FeatureAnalytics.CloudSyncMetrics();
        metrics.timestamp = System.currentTimeMillis();
        metrics.success = true;
        metrics.filesCount = 1000000;
        metrics.bytesTransferred = Long.MAX_VALUE;
        metrics.duration = Long.MAX_VALUE;

        analytics.trackCloudSync(metrics);

        // Should handle very large values
        FeatureAnalytics.CloudSyncStatistics stats = analytics.getCloudSyncStatistics();
        assertNotNull(stats);
    }

    @Test
    public void testEdgeCase_EmptyString_Handled() {
        FeatureAnalytics.SearchMetrics metrics = new FeatureAnalytics.SearchMetrics();
        metrics.timestamp = System.currentTimeMillis();
        metrics.query = ""; // Empty query
        metrics.processingTime = 10;
        metrics.resultCount = 0;
        metrics.userSatisfied = false;

        analytics.trackSearch(metrics);

        FeatureAnalytics.SearchStatistics stats = analytics.getSearchStatistics();
        assertEquals(1, stats.totalSearches);
    }

    @Test
    public void testEdgeCase_VeryLongString_Handled() {
        StringBuilder longQuery = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longQuery.append("word");
        }

        FeatureAnalytics.SearchMetrics metrics = new FeatureAnalytics.SearchMetrics();
        metrics.timestamp = System.currentTimeMillis();
        metrics.query = longQuery.toString();
        metrics.processingTime = 500;
        metrics.resultCount = 0;
        metrics.userSatisfied = false;

        analytics.trackSearch(metrics);

        FeatureAnalytics.SearchStatistics stats = analytics.getSearchStatistics();
        assertEquals(1, stats.totalSearches);
    }

    @Test
    public void testEdgeCase_WithoutConsent_NoTracking() {
        analytics.setUserConsent(false);

        FeatureAnalytics.OCRMetrics metrics = new FeatureAnalytics.OCRMetrics();
        metrics.timestamp = System.currentTimeMillis();
        metrics.processingTime = 1500;
        metrics.accuracy = 0.95f;
        metrics.success = true;

        analytics.trackOCR(metrics);

        // Should not track without consent
        FeatureAnalytics.OCRStatistics stats = analytics.getOCRStatistics();
        assertEquals(0, stats.totalOperations);
    }

    @Test
    public void testErrorRecovery_CrashTracking() {
        String stackTrace = "java.lang.NullPointerException\n" +
                           "    at com.example.Test.method(Test.java:10)";

        analytics.trackCrash(stackTrace);

        FeatureAnalytics.ErrorStatistics stats = analytics.getErrorStatistics();
        assertEquals(1, stats.totalErrors);
        assertTrue(stats.criticalErrors > 0);
    }

    @Test
    public void testErrorRecovery_MultipleErrors() throws InterruptedException {
        FeatureAnalytics.ErrorSeverity[] severities = {
            FeatureAnalytics.ErrorSeverity.INFO,
            FeatureAnalytics.ErrorSeverity.WARNING,
            FeatureAnalytics.ErrorSeverity.ERROR,
            FeatureAnalytics.ErrorSeverity.CRITICAL
        };

        for (FeatureAnalytics.ErrorSeverity severity : severities) {
            FeatureAnalytics.ErrorMetrics metrics = new FeatureAnalytics.ErrorMetrics();
            metrics.timestamp = System.currentTimeMillis();
            metrics.errorType = "TestError";
            metrics.message = "Test message";
            metrics.stackTrace = "Test trace";
            metrics.severity = severity;

            analytics.trackError(metrics);
        }

        Thread.sleep(200);

        FeatureAnalytics.ErrorStatistics stats = analytics.getErrorStatistics();
        assertEquals(4, stats.totalErrors);
        assertTrue(stats.criticalErrors >= 1);
        assertTrue(stats.warnings >= 1);
    }

    @Test
    public void testErrorRecovery_ListenerException_Handled() {
        // Add listener that throws exception
        analytics.addListener((event, data) -> {
            throw new RuntimeException("Test exception");
        });

        // Should not crash when notifying listeners
        FeatureAnalytics.OCRMetrics metrics = new FeatureAnalytics.OCRMetrics();
        metrics.timestamp = System.currentTimeMillis();
        metrics.processingTime = 1500;
        metrics.accuracy = 0.95f;
        metrics.success = true;

        analytics.trackOCR(metrics);

        // Should complete despite listener exception
        FeatureAnalytics.OCRStatistics stats = analytics.getOCRStatistics();
        assertEquals(1, stats.totalOperations);
    }
}


