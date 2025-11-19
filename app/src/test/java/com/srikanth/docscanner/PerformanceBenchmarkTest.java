package com.srikanth.docscanner;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Test Category 6: Performance Benchmark Tests
 */
@RunWith(AndroidJUnit4.class)
public class PerformanceBenchmarkTest {

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
    public void testPerformance_OCR_ProcessingSpeed() {
        long startTime = System.currentTimeMillis();

        FeatureAnalytics.OCRMetrics metrics = new FeatureAnalytics.OCRMetrics();
        metrics.timestamp = System.currentTimeMillis();
        metrics.processingTime = 1500;
        metrics.accuracy = 0.95f;
        metrics.success = true;

        analytics.trackOCR(metrics);

        long endTime = System.currentTimeMillis();
        long trackingTime = endTime - startTime;

        assertTrue("Tracking should be fast", trackingTime < 100);
    }

    @Test
    public void testPerformance_MemoryUsage_WithinBounds() {
        Runtime runtime = Runtime.getRuntime();
        runtime.gc(); // Force garbage collection

        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        // Track 100 operations
        for (int i = 0; i < 100; i++) {
            FeatureAnalytics.OCRMetrics metrics = new FeatureAnalytics.OCRMetrics();
            metrics.timestamp = System.currentTimeMillis();
            metrics.processingTime = 1500;
            metrics.accuracy = 0.95f;
            metrics.success = true;

            analytics.trackOCR(metrics);
        }

        runtime.gc();
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = memoryAfter - memoryBefore;

        // Should use less than 5MB
        assertTrue("Memory usage should be reasonable",
            memoryUsed < 5 * 1024 * 1024);
    }

    @Test
    public void testPerformance_BulkTracking_Speed() {
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 1000; i++) {
            FeatureAnalytics.SearchMetrics metrics = new FeatureAnalytics.SearchMetrics();
            metrics.timestamp = System.currentTimeMillis();
            metrics.query = "test" + i;
            metrics.processingTime = 150;
            metrics.resultCount = 10;
            metrics.userSatisfied = true;

            analytics.trackSearch(metrics);
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        // 1000 operations should complete in under 5 seconds
        assertTrue("Bulk tracking should be fast", totalTime < 5000);
    }

    @Test
    public void testPerformance_StatisticsCalculation_Speed() {
        // Add sample data
        for (int i = 0; i < 50; i++) {
            FeatureAnalytics.OCRMetrics metrics = new FeatureAnalytics.OCRMetrics();
            metrics.timestamp = System.currentTimeMillis();
            metrics.processingTime = 1500;
            metrics.accuracy = 0.95f;
            metrics.success = true;

            analytics.trackOCR(metrics);
        }

        // Benchmark statistics calculation
        long startTime = System.currentTimeMillis();

        FeatureAnalytics.OCRStatistics stats = analytics.getOCRStatistics();

        long endTime = System.currentTimeMillis();
        long calcTime = endTime - startTime;

        // Statistics should be calculated quickly
        assertTrue("Statistics calculation should be fast", calcTime < 100);
        assertNotNull(stats);
    }

    @Test
    public void testPerformance_ReportGeneration_Speed() {
        // Add various data types
        FeatureAnalytics.OCRMetrics ocr = new FeatureAnalytics.OCRMetrics();
        ocr.timestamp = System.currentTimeMillis();
        ocr.processingTime = 1500;
        ocr.accuracy = 0.95f;
        ocr.success = true;
        analytics.trackOCR(ocr);

        FeatureAnalytics.SearchMetrics search = new FeatureAnalytics.SearchMetrics();
        search.timestamp = System.currentTimeMillis();
        search.query = "test";
        search.processingTime = 150;
        search.resultCount = 10;
        search.userSatisfied = true;
        analytics.trackSearch(search);

        // Benchmark report generation
        long startTime = System.currentTimeMillis();

        FeatureAnalytics.AnalyticsReport report = analytics.generateReport();

        long endTime = System.currentTimeMillis();
        long reportTime = endTime - startTime;

        assertTrue("Report generation should be fast", reportTime < 200);
        assertNotNull(report);
    }

    @Test
    public void testPerformance_ResourceTracking_Overhead() {
        long startTime = System.currentTimeMillis();

        analytics.trackResourceUsage();

        long endTime = System.currentTimeMillis();
        long trackingTime = endTime - startTime;

        // Resource tracking should have minimal overhead
        assertTrue("Resource tracking should be lightweight", trackingTime < 50);
    }

    @Test
    public void testPerformance_ConcurrentAccess_Throughput() throws InterruptedException {
        int threadCount = 20;
        Thread[] threads = new Thread[threadCount];
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 50; j++) {
                    FeatureAnalytics.OCRMetrics metrics = new FeatureAnalytics.OCRMetrics();
                    metrics.timestamp = System.currentTimeMillis();
                    metrics.processingTime = 1500;
                    metrics.accuracy = 0.95f;
                    metrics.success = true;

                    analytics.trackOCR(metrics);
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        // 1000 operations across 20 threads should complete in reasonable time
        assertTrue("Concurrent operations should be efficient", totalTime < 10000);
    }
}


