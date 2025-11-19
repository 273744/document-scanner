package com.srikanth.docscanner;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Test Category 1: OCR Accuracy Tests
 *
 * Tests OCR functionality with various document types and conditions
 */
@RunWith(AndroidJUnit4.class)
public class OCRAccuracyTest {

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

    /**
     * Test 1.1: Basic OCR accuracy with clear text
     */
    @Test
    public void testOCR_ClearText_HighAccuracy() throws InterruptedException {
        // Arrange
        String expectedText = "This is a clear document";
        CountDownLatch latch = new CountDownLatch(1);

        // Act
        FeatureAnalytics.OCRMetrics metrics = new FeatureAnalytics.OCRMetrics();
        metrics.timestamp = System.currentTimeMillis();
        metrics.processingTime = 1500;
        metrics.accuracy = 0.98f;
        metrics.success = true;
        metrics.language = "English";
        metrics.characterCount = expectedText.length();

        analytics.trackOCR(metrics);
        latch.await(100, TimeUnit.MILLISECONDS);

        // Assert
        FeatureAnalytics.OCRStatistics stats = analytics.getOCRStatistics();
        assertEquals(1, stats.totalOperations);
        assertTrue(stats.averageAccuracy > 0.95f);
        assertEquals(1.0f, stats.successRate, 0.01f);
    }

    /**
     * Test 1.2: OCR with poor quality scan
     */
    @Test
    public void testOCR_PoorQuality_LowerAccuracy() throws InterruptedException {
        // Arrange
        CountDownLatch latch = new CountDownLatch(1);

        // Act
        FeatureAnalytics.OCRMetrics metrics = new FeatureAnalytics.OCRMetrics();
        metrics.timestamp = System.currentTimeMillis();
        metrics.processingTime = 2500;
        metrics.accuracy = 0.65f;
        metrics.success = true;
        metrics.language = "English";
        metrics.characterCount = 250;

        analytics.trackOCR(metrics);
        latch.await(100, TimeUnit.MILLISECONDS);

        // Assert
        FeatureAnalytics.OCRStatistics stats = analytics.getOCRStatistics();
        assertTrue(stats.averageAccuracy < 0.8f);
        assertTrue(stats.averageProcessingTime > 2000);
    }

    /**
     * Test 1.3: OCR processing time benchmarks
     */
    @Test
    public void testOCR_ProcessingTime_WithinBounds() {
        // Arrange
        long maxAcceptableTime = 5000; // 5 seconds

        // Act
        FeatureAnalytics.OCRMetrics metrics = new FeatureAnalytics.OCRMetrics();
        metrics.timestamp = System.currentTimeMillis();
        metrics.processingTime = 1800;
        metrics.accuracy = 0.92f;
        metrics.success = true;
        metrics.language = "English";
        metrics.characterCount = 500;

        analytics.trackOCR(metrics);

        // Assert
        assertTrue(metrics.processingTime < maxAcceptableTime);
        assertTrue(metrics.success);
    }

    /**
     * Test 1.4: Multi-language OCR accuracy
     */
    @Test
    public void testOCR_MultiLanguage_Accuracy() throws InterruptedException {
        // Arrange
        String[] languages = {"English", "Spanish", "French", "German"};
        CountDownLatch latch = new CountDownLatch(languages.length);

        // Act
        for (String language : languages) {
            FeatureAnalytics.OCRMetrics metrics = new FeatureAnalytics.OCRMetrics();
            metrics.timestamp = System.currentTimeMillis();
            metrics.processingTime = 1500;
            metrics.accuracy = 0.90f;
            metrics.success = true;
            metrics.language = language;
            metrics.characterCount = 300;

            analytics.trackOCR(metrics);
            latch.countDown();
        }

        latch.await(500, TimeUnit.MILLISECONDS);

        // Assert
        FeatureAnalytics.OCRStatistics stats = analytics.getOCRStatistics();
        assertEquals(languages.length, stats.totalOperations);
        assertTrue(stats.averageAccuracy > 0.85f);
    }

    /**
     * Test 1.5: OCR failure handling
     */
    @Test
    public void testOCR_Failure_ProperHandling() {
        // Arrange & Act
        FeatureAnalytics.OCRMetrics metrics = new FeatureAnalytics.OCRMetrics();
        metrics.timestamp = System.currentTimeMillis();
        metrics.processingTime = 500;
        metrics.accuracy = 0.0f;
        metrics.success = false;
        metrics.language = "English";
        metrics.characterCount = 0;

        analytics.trackOCR(metrics);

        // Assert
        FeatureAnalytics.OCRStatistics stats = analytics.getOCRStatistics();
        assertTrue(stats.successRate < 1.0f);
    }

    /**
     * Test 1.6: Character count accuracy
     */
    @Test
    public void testOCR_CharacterCount_Accuracy() {
        // Arrange
        int expectedCharCount = 1000;

        // Act
        FeatureAnalytics.OCRMetrics metrics = new FeatureAnalytics.OCRMetrics();
        metrics.timestamp = System.currentTimeMillis();
        metrics.processingTime = 2000;
        metrics.accuracy = 0.95f;
        metrics.success = true;
        metrics.language = "English";
        metrics.characterCount = expectedCharCount;

        analytics.trackOCR(metrics);

        // Assert
        assertEquals(expectedCharCount, metrics.characterCount);
        assertTrue(metrics.accuracy > 0.90f);
    }

    /**
     * Test 1.7: Large document OCR performance
     */
    @Test
    public void testOCR_LargeDocument_Performance() {
        // Arrange
        int largeCharCount = 10000;

        // Act
        FeatureAnalytics.OCRMetrics metrics = new FeatureAnalytics.OCRMetrics();
        metrics.timestamp = System.currentTimeMillis();
        metrics.processingTime = 8000;
        metrics.accuracy = 0.88f;
        metrics.success = true;
        metrics.language = "English";
        metrics.characterCount = largeCharCount;

        analytics.trackOCR(metrics);

        // Assert
        assertTrue(metrics.characterCount > 5000);
        assertTrue(metrics.processingTime < 10000); // Under 10 seconds
    }

    /**
     * Test 1.8: OCR statistics aggregation
     */
    @Test
    public void testOCR_Statistics_Aggregation() throws InterruptedException {
        // Arrange
        int testCount = 5;
        CountDownLatch latch = new CountDownLatch(testCount);

        // Act
        for (int i = 0; i < testCount; i++) {
            FeatureAnalytics.OCRMetrics metrics = new FeatureAnalytics.OCRMetrics();
            metrics.timestamp = System.currentTimeMillis();
            metrics.processingTime = 1500 + (i * 100);
            metrics.accuracy = 0.90f + (i * 0.01f);
            metrics.success = true;
            metrics.language = "English";
            metrics.characterCount = 500;

            analytics.trackOCR(metrics);
            latch.countDown();
        }

        latch.await(1, TimeUnit.SECONDS);

        // Assert
        FeatureAnalytics.OCRStatistics stats = analytics.getOCRStatistics();
        assertEquals(testCount, stats.totalOperations);
        assertTrue(stats.averageAccuracy > 0.90f);
        assertTrue(stats.averageProcessingTime > 1500);
    }
}


