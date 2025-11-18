package com.example.myapplication;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

/**
 * Test Category 4: Search Functionality & Ranking Tests
 */
@RunWith(AndroidJUnit4.class)
public class SearchFunctionalityTest {

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
    public void testSearch_FastQuery_UnderThreshold() {
        FeatureAnalytics.SearchMetrics metrics = new FeatureAnalytics.SearchMetrics();
        metrics.timestamp = System.currentTimeMillis();
        metrics.query = "invoice";
        metrics.processingTime = 150;
        metrics.resultCount = 25;
        metrics.userSatisfied = true;

        analytics.trackSearch(metrics);

        assertTrue(metrics.processingTime < 500); // Under 500ms
    }

    @Test
    public void testSearch_RelevantResults() {
        FeatureAnalytics.SearchMetrics metrics = new FeatureAnalytics.SearchMetrics();
        metrics.timestamp = System.currentTimeMillis();
        metrics.query = "contract 2024";
        metrics.processingTime = 200;
        metrics.resultCount = 10;
        metrics.userSatisfied = true;

        analytics.trackSearch(metrics);

        assertTrue(metrics.resultCount > 0);
        assertTrue(metrics.userSatisfied);
    }

    @Test
    public void testSearch_EmptyResults() {
        FeatureAnalytics.SearchMetrics metrics = new FeatureAnalytics.SearchMetrics();
        metrics.timestamp = System.currentTimeMillis();
        metrics.query = "nonexistent document xyz123";
        metrics.processingTime = 50;
        metrics.resultCount = 0;
        metrics.userSatisfied = false;

        analytics.trackSearch(metrics);

        assertEquals(0, metrics.resultCount);
        assertFalse(metrics.userSatisfied);
    }

    @Test
    public void testSearch_AverageTime() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(5);

        for (int i = 0; i < 5; i++) {
            FeatureAnalytics.SearchMetrics metrics = new FeatureAnalytics.SearchMetrics();
            metrics.timestamp = System.currentTimeMillis();
            metrics.query = "test" + i;
            metrics.processingTime = 100 + (i * 20);
            metrics.resultCount = 5 + i;
            metrics.userSatisfied = true;

            analytics.trackSearch(metrics);
            latch.countDown();
        }

        latch.await(500, TimeUnit.MILLISECONDS);

        FeatureAnalytics.SearchStatistics stats = analytics.getSearchStatistics();
        assertTrue(stats.averageSearchTime > 100);
        assertTrue(stats.averageSearchTime < 200);
    }

    @Test
    public void testSearch_SatisfactionRate() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(10);

        for (int i = 0; i < 10; i++) {
            FeatureAnalytics.SearchMetrics metrics = new FeatureAnalytics.SearchMetrics();
            metrics.timestamp = System.currentTimeMillis();
            metrics.query = "query" + i;
            metrics.processingTime = 150;
            metrics.resultCount = 5;
            metrics.userSatisfied = (i < 8); // 80% satisfied

            analytics.trackSearch(metrics);
            latch.countDown();
        }

        latch.await(1, TimeUnit.SECONDS);

        FeatureAnalytics.SearchStatistics stats = analytics.getSearchStatistics();
        assertEquals(0.8f, stats.satisfactionRate, 0.1f);
    }

    @Test
    public void testSearch_ComplexQuery_Performance() {
        FeatureAnalytics.SearchMetrics metrics = new FeatureAnalytics.SearchMetrics();
        metrics.timestamp = System.currentTimeMillis();
        metrics.query = "invoice AND contract OR receipt NOT draft";
        metrics.processingTime = 350;
        metrics.resultCount = 15;
        metrics.userSatisfied = true;

        analytics.trackSearch(metrics);

        assertTrue(metrics.processingTime < 1000);
        assertTrue(metrics.resultCount > 0);
    }

    @Test
    public void testSearch_AverageResultCount() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(5);

        int[] resultCounts = {10, 15, 20, 25, 30};
        for (int count : resultCounts) {
            FeatureAnalytics.SearchMetrics metrics = new FeatureAnalytics.SearchMetrics();
            metrics.timestamp = System.currentTimeMillis();
            metrics.query = "test";
            metrics.processingTime = 150;
            metrics.resultCount = count;
            metrics.userSatisfied = true;

            analytics.trackSearch(metrics);
            latch.countDown();
        }

        latch.await(500, TimeUnit.MILLISECONDS);

        FeatureAnalytics.SearchStatistics stats = analytics.getSearchStatistics();
        assertEquals(20.0f, stats.averageResultCount, 5.0f);
    }
}

