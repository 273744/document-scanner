package com.srikanth.docscanner;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Test Category 5: Database Migration & Data Integrity Tests
 */
@RunWith(AndroidJUnit4.class)
public class DatabaseIntegrityTest {

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
    public void testDatabase_DataPersistence() throws InterruptedException {
        // Save data
        FeatureAnalytics.OCRMetrics metrics = new FeatureAnalytics.OCRMetrics();
        metrics.timestamp = System.currentTimeMillis();
        metrics.processingTime = 1500;
        metrics.accuracy = 0.95f;
        metrics.success = true;
        metrics.language = "English";
        metrics.characterCount = 500;

        analytics.trackOCR(metrics);
        Thread.sleep(200);

        // Verify persistence
        FeatureAnalytics.OCRStatistics stats = analytics.getOCRStatistics();
        assertTrue(stats.totalOperations > 0);
    }

    @Test
    public void testDatabase_ConsentRevocation_DataCleared() {
        // Add data
        FeatureAnalytics.OCRMetrics metrics = new FeatureAnalytics.OCRMetrics();
        metrics.timestamp = System.currentTimeMillis();
        metrics.processingTime = 1500;
        metrics.accuracy = 0.95f;
        metrics.success = true;

        analytics.trackOCR(metrics);

        // Revoke consent
        analytics.setUserConsent(false);

        // Verify data cleared
        FeatureAnalytics.OCRStatistics stats = analytics.getOCRStatistics();
        assertEquals(0, stats.totalOperations);
    }

    @Test
    public void testDatabase_MaxRecordsLimit() throws InterruptedException {
        // Add more than max records (100 for OCR)
        for (int i = 0; i < 110; i++) {
            FeatureAnalytics.OCRMetrics metrics = new FeatureAnalytics.OCRMetrics();
            metrics.timestamp = System.currentTimeMillis();
            metrics.processingTime = 1500;
            metrics.accuracy = 0.95f;
            metrics.success = true;

            analytics.trackOCR(metrics);
        }

        Thread.sleep(500);

        // Verify limit enforced
        FeatureAnalytics.OCRStatistics stats = analytics.getOCRStatistics();
        assertTrue(stats.totalOperations <= 100);
    }

    @Test
    public void testDatabase_ConcurrentWrites() throws InterruptedException {
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                FeatureAnalytics.OCRMetrics metrics = new FeatureAnalytics.OCRMetrics();
                metrics.timestamp = System.currentTimeMillis();
                metrics.processingTime = 1500;
                metrics.accuracy = 0.95f;
                metrics.success = true;

                analytics.trackOCR(metrics);
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        Thread.sleep(500);

        FeatureAnalytics.OCRStatistics stats = analytics.getOCRStatistics();
        assertEquals(threadCount, stats.totalOperations);
    }

    @Test
    public void testDatabase_JSONExport() {
        // Add sample data
        FeatureAnalytics.OCRMetrics metrics = new FeatureAnalytics.OCRMetrics();
        metrics.timestamp = System.currentTimeMillis();
        metrics.processingTime = 1500;
        metrics.accuracy = 0.95f;
        metrics.success = true;

        analytics.trackOCR(metrics);

        // Export
        String json = analytics.exportAnalytics();

        // Verify valid JSON
        assertNotNull(json);
        assertTrue(json.contains("{"));
        assertTrue(json.contains("}"));
    }
}


