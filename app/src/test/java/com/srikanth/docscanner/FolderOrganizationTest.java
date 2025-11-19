package com.srikanth.docscanner;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

/**
 * Test Category 3: Folder Organization & Hierarchy Tests
 */
@RunWith(AndroidJUnit4.class)
public class FolderOrganizationTest {

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
    public void testFolder_AutoCategorization_Success() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        FeatureAnalytics.FolderMetrics metrics = new FeatureAnalytics.FolderMetrics();
        metrics.timestamp = System.currentTimeMillis();
        metrics.folderName = "Invoices";
        metrics.autoCategorized = true;
        metrics.documentsCount = 5;

        analytics.trackFolderOrganization(metrics);
        latch.await(100, TimeUnit.MILLISECONDS);

        FeatureAnalytics.FolderStatistics stats = analytics.getFolderStatistics();
        assertTrue(stats.autoCategorizationRate > 0.0f);
        assertEquals(1, stats.totalOrganizations);
    }

    @Test
    public void testFolder_ManualOrganization() {
        FeatureAnalytics.FolderMetrics metrics = new FeatureAnalytics.FolderMetrics();
        metrics.timestamp = System.currentTimeMillis();
        metrics.folderName = "Personal";
        metrics.autoCategorized = false;
        metrics.documentsCount = 10;

        analytics.trackFolderOrganization(metrics);

        FeatureAnalytics.FolderStatistics stats = analytics.getFolderStatistics();
        assertEquals(0.0f, stats.autoCategorizationRate, 0.01f);
    }

    @Test
    public void testFolder_UsagePatterns() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);

        String[] folders = {"Work", "Personal", "Work"};
        for (String folder : folders) {
            FeatureAnalytics.FolderMetrics metrics = new FeatureAnalytics.FolderMetrics();
            metrics.timestamp = System.currentTimeMillis();
            metrics.folderName = folder;
            metrics.autoCategorized = true;
            metrics.documentsCount = 1;

            analytics.trackFolderOrganization(metrics);
            latch.countDown();
        }

        latch.await(500, TimeUnit.MILLISECONDS);

        FeatureAnalytics.FolderStatistics stats = analytics.getFolderStatistics();
        assertEquals(2, stats.folderUsageMap.get("Work").intValue());
        assertEquals(1, stats.folderUsageMap.get("Personal").intValue());
    }

    @Test
    public void testFolder_LargeDocumentCount() {
        FeatureAnalytics.FolderMetrics metrics = new FeatureAnalytics.FolderMetrics();
        metrics.timestamp = System.currentTimeMillis();
        metrics.folderName = "Archive";
        metrics.autoCategorized = false;
        metrics.documentsCount = 1000;

        analytics.trackFolderOrganization(metrics);

        assertTrue(metrics.documentsCount > 500);
    }

    @Test
    public void testFolder_AutoCategorizationRate() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(10);

        for (int i = 0; i < 10; i++) {
            FeatureAnalytics.FolderMetrics metrics = new FeatureAnalytics.FolderMetrics();
            metrics.timestamp = System.currentTimeMillis();
            metrics.folderName = "Folder" + i;
            metrics.autoCategorized = (i < 7); // 70% auto-categorized
            metrics.documentsCount = 1;

            analytics.trackFolderOrganization(metrics);
            latch.countDown();
        }

        latch.await(1, TimeUnit.SECONDS);

        FeatureAnalytics.FolderStatistics stats = analytics.getFolderStatistics();
        assertEquals(0.7f, stats.autoCategorizationRate, 0.1f);
    }
}


