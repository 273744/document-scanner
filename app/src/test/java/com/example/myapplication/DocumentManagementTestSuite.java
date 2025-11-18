package com.example.myapplication;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Comprehensive test suite for all document management features
 *
 * Test Categories:
 * 1. OCR Accuracy Tests
 * 2. Cloud Sync & Conflict Resolution Tests
 * 3. Folder Organization & Hierarchy Tests
 * 4. Search Functionality & Ranking Tests
 * 5. Database Migration & Data Integrity Tests
 * 6. Performance Benchmark Tests
 * 7. Edge Case & Error Recovery Tests
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    OCRAccuracyTest.class,
    CloudSyncReliabilityTest.class,
    FolderOrganizationTest.class,
    SearchFunctionalityTest.class,
    DatabaseIntegrityTest.class,
    PerformanceBenchmarkTest.class,
    EdgeCaseHandlingTest.class
})
public class DocumentManagementTestSuite {
    // Test suite runner - executes all test classes
}

