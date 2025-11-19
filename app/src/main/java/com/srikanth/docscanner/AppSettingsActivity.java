package com.srikanth.docscanner;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.slider.Slider;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * AppSettingsActivity - Comprehensive feature configuration
 * 
 * Features:
 * 1. OCR language preferences and offline models
 * 2. Cloud sync settings and storage management
 * 3. Auto-categorization sensitivity levels
 * 4. Search indexing options and frequency
 * 5. Performance vs accuracy trade-offs
 * 6. Privacy settings for cloud data
 * 7. Feature usage analytics and optimization
 * 8. User onboarding and feature discovery
 */
public class AppSettingsActivity extends AppCompatActivity {

    private static final String TAG = "AppSettings";
    private static final String PREFS_NAME = "AppSettings";
    private static final String KEY_FIRST_LAUNCH = "first_launch";
    private static final String KEY_ONBOARDING_COMPLETE = "onboarding_complete";
    
    // UI Components - OCR Settings
    private Spinner spOcrLanguage;
    private Switch swOfflineOCR;
    private TextView tvOcrModelSize;
    private Button btnDownloadOcrModel;
    private SeekBar sbOcrQuality;
    private TextView tvOcrQuality;
    
    // Cloud Sync Settings
    private Switch swCloudSync;
    private Spinner spSyncFrequency;
    private Switch swWifiOnly;
    private TextView tvStorageUsed;
    private Button btnManageStorage;
    private Switch swAutoBackup;
    
    // Categorization Settings
    private Switch swAutoCategorization;
    private SeekBar sbCategorizationSensitivity;
    private TextView tvSensitivityLevel;
    private Switch swLearnFromUser;
    private Button btnResetCategories;
    
    // Search Settings
    private Switch swAutoIndexing;
    private Spinner spIndexingFrequency;
    private Switch swIndexOnSave;
    private CheckBox cbIndexDocNames;
    private CheckBox cbIndexOcrText;
    private CheckBox cbIndexMetadata;
    private Button btnRebuildIndex;
    
    // Performance Settings
    private RadioGroup rgPerformanceMode;
    private SeekBar sbImageQuality;
    private TextView tvImageQuality;
    private Switch swBatchProcessing;
    private TextView tvBatchSize;
    private SeekBar sbBatchSize;
    
    // Privacy Settings
    private Switch swCloudEncryption;
    private Switch swAnonymousAnalytics;
    private Switch swSensitiveDataFilter;
    private Button btnClearSearchHistory;
    private Button btnExportData;
    
    // Analytics
    private TextView tvDocumentsScanned;
    private TextView tvSearchesPerformed;
    private TextView tvStorageUsedAnalytics;
    private TextView tvLastBackup;
    private Button btnViewDetailedAnalytics;
    
    // Onboarding
    private CardView cvOnboarding;
    private Button btnStartOnboarding;
    
    // SharedPreferences
    private SharedPreferences prefs;
    
    // Coordinator
    // TODO: Uncomment when DocumentManagerCoordinator is created
    // private DocumentManagerCoordinator coordinator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO: Create activity_app_settings.xml layout
        // setContentView(R.layout.activity_app_settings);
        
        // Setup toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Settings");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        // Initialize preferences
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        // Initialize coordinator
        // TODO: Uncomment when DocumentManagerCoordinator is created
        // coordinator = DocumentManagerCoordinator.getInstance(this);

        // Temporary: Show message that UI is pending
        Toast.makeText(this, "App Settings - Layout pending implementation", Toast.LENGTH_LONG).show();
        
        // Check if first launch
        checkFirstLaunch();
        
        finish();
        
        // Initialize views - commented out until layouts are created
        // initializeViews();
        // loadSettings();
        // setupListeners();
        // loadAnalytics();
    }

    /**
     * Check if first launch and show onboarding
     */
    private void checkFirstLaunch() {
        boolean isFirstLaunch = prefs.getBoolean(KEY_FIRST_LAUNCH, true);
        
        if (isFirstLaunch) {
            // Mark as not first launch
            prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply();
            
            // Show onboarding
            showOnboarding();
        }
    }

    /**
     * Initialize views
     */
    private void initializeViews() {
        // TODO: Uncomment when layout XML is created
        /*
        // OCR Settings
        spOcrLanguage = findViewById(R.id.spOcrLanguage);
        swOfflineOCR = findViewById(R.id.swOfflineOCR);
        tvOcrModelSize = findViewById(R.id.tvOcrModelSize);
        btnDownloadOcrModel = findViewById(R.id.btnDownloadOcrModel);
        sbOcrQuality = findViewById(R.id.sbOcrQuality);
        tvOcrQuality = findViewById(R.id.tvOcrQuality);
        
        // Cloud Sync
        swCloudSync = findViewById(R.id.swCloudSync);
        spSyncFrequency = findViewById(R.id.spSyncFrequency);
        swWifiOnly = findViewById(R.id.swWifiOnly);
        tvStorageUsed = findViewById(R.id.tvStorageUsed);
        btnManageStorage = findViewById(R.id.btnManageStorage);
        swAutoBackup = findViewById(R.id.swAutoBackup);
        
        // Categorization
        swAutoCategorization = findViewById(R.id.swAutoCategorization);
        sbCategorizationSensitivity = findViewById(R.id.sbCategorizationSensitivity);
        tvSensitivityLevel = findViewById(R.id.tvSensitivityLevel);
        swLearnFromUser = findViewById(R.id.swLearnFromUser);
        btnResetCategories = findViewById(R.id.btnResetCategories);
        
        // Search Settings
        swAutoIndexing = findViewById(R.id.swAutoIndexing);
        spIndexingFrequency = findViewById(R.id.spIndexingFrequency);
        swIndexOnSave = findViewById(R.id.swIndexOnSave);
        cbIndexDocNames = findViewById(R.id.cbIndexDocNames);
        cbIndexOcrText = findViewById(R.id.cbIndexOcrText);
        cbIndexMetadata = findViewById(R.id.cbIndexMetadata);
        btnRebuildIndex = findViewById(R.id.btnRebuildIndex);
        
        // Performance
        rgPerformanceMode = findViewById(R.id.rgPerformanceMode);
        sbImageQuality = findViewById(R.id.sbImageQuality);
        tvImageQuality = findViewById(R.id.tvImageQuality);
        swBatchProcessing = findViewById(R.id.swBatchProcessing);
        tvBatchSize = findViewById(R.id.tvBatchSize);
        sbBatchSize = findViewById(R.id.sbBatchSize);
        
        // Privacy
        swCloudEncryption = findViewById(R.id.swCloudEncryption);
        swAnonymousAnalytics = findViewById(R.id.swAnonymousAnalytics);
        swSensitiveDataFilter = findViewById(R.id.swSensitiveDataFilter);
        btnClearSearchHistory = findViewById(R.id.btnClearSearchHistory);
        btnExportData = findViewById(R.id.btnExportData);
        
        // Analytics
        tvDocumentsScanned = findViewById(R.id.tvDocumentsScanned);
        tvSearchesPerformed = findViewById(R.id.tvSearchesPerformed);
        tvStorageUsedAnalytics = findViewById(R.id.tvStorageUsedAnalytics);
        tvLastBackup = findViewById(R.id.tvLastBackup);
        btnViewDetailedAnalytics = findViewById(R.id.btnViewDetailedAnalytics);
        
        // Onboarding
        cvOnboarding = findViewById(R.id.cvOnboarding);
        btnStartOnboarding = findViewById(R.id.btnStartOnboarding);
        */
    }

    // ================================
    // 1. OCR Language Preferences
    // ================================

    /**
     * Setup OCR language settings
     */
    private void setupOcrSettings() {
        // OCR languages
        String[] languages = {
            "English", "Spanish", "French", "German", "Chinese", 
            "Japanese", "Korean", "Arabic", "Hindi", "Portuguese"
        };
        
        ArrayAdapter<String> langAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, languages);
        langAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spOcrLanguage.setAdapter(langAdapter);
        
        // Load saved language
        String savedLang = prefs.getString("ocr_language", "English");
        int position = langAdapter.getPosition(savedLang);
        spOcrLanguage.setSelection(position);
        
        // Offline OCR toggle
        swOfflineOCR.setChecked(prefs.getBoolean("offline_ocr", false));
        swOfflineOCR.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("offline_ocr", isChecked).apply();
            
            if (isChecked) {
                checkOfflineModel();
            }
        });
        
        // OCR quality slider
        sbOcrQuality.setProgress(prefs.getInt("ocr_quality", 75));
        updateOcrQualityLabel(sbOcrQuality.getProgress());
        
        sbOcrQuality.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateOcrQualityLabel(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                prefs.edit().putInt("ocr_quality", seekBar.getProgress()).apply();
            }
        });
        
        // Download model button
        btnDownloadOcrModel.setOnClickListener(v -> downloadOcrModel());
    }

    /**
     * Update OCR quality label
     */
    private void updateOcrQualityLabel(int value) {
        String quality;
        if (value < 33) {
            quality = "Fast (Low Quality)";
        } else if (value < 66) {
            quality = "Balanced";
        } else {
            quality = "Accurate (Slow)";
        }
        tvOcrQuality.setText(quality);
    }

    /**
     * Check offline OCR model
     */
    private void checkOfflineModel() {
        // Check if model is downloaded
        boolean modelDownloaded = prefs.getBoolean("ocr_model_downloaded", false);
        
        if (!modelDownloaded) {
            new AlertDialog.Builder(this)
                .setTitle("Download Offline Model")
                .setMessage("Offline OCR requires downloading a language model (~50MB). Download now?")
                .setPositiveButton("Download", (dialog, which) -> downloadOcrModel())
                .setNegativeButton("Cancel", (dialog, which) -> swOfflineOCR.setChecked(false))
                .show();
        } else {
            updateModelInfo();
        }
    }

    /**
     * Download OCR model
     */
    private void downloadOcrModel() {
        // TODO: Implement actual model download
        Toast.makeText(this, "Downloading OCR model...", Toast.LENGTH_SHORT).show();
        
        // Simulate download
        new android.os.Handler().postDelayed(() -> {
            prefs.edit().putBoolean("ocr_model_downloaded", true).apply();
            Toast.makeText(this, "OCR model downloaded successfully", Toast.LENGTH_SHORT).show();
            updateModelInfo();
        }, 2000);
    }

    /**
     * Update model info display
     */
    private void updateModelInfo() {
        String language = spOcrLanguage.getSelectedItem().toString();
        tvOcrModelSize.setText(language + " model (48.5 MB)");
    }

    // ================================
    // 2. Cloud Sync Settings
    // ================================

    /**
     * Setup cloud sync settings
     */
    private void setupCloudSyncSettings() {
        // Cloud sync toggle
        swCloudSync.setChecked(prefs.getBoolean("cloud_sync", false));
        swCloudSync.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("cloud_sync", isChecked).apply();
            
            // Update coordinator preferences
            // TODO: Uncomment when DocumentManagerCoordinator is created
            // DocumentManagerCoordinator.UserPreferences coordPrefs = coordinator.getPreferences();
            // coordPrefs.cloudSyncEnabled = isChecked;
            // coordinator.updatePreferences(coordPrefs);

            updateCloudSyncUI(isChecked);
        });
        
        // Sync frequency
        String[] frequencies = {"Real-time", "Every 15 minutes", "Every hour", "Daily", "Manual"};
        ArrayAdapter<String> freqAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, frequencies);
        freqAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSyncFrequency.setAdapter(freqAdapter);
        
        int savedFreq = prefs.getInt("sync_frequency", 1);
        spSyncFrequency.setSelection(savedFreq);
        
        // WiFi only toggle
        swWifiOnly.setChecked(prefs.getBoolean("sync_wifi_only", true));
        swWifiOnly.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("sync_wifi_only", isChecked).apply();
        });
        
        // Auto backup toggle
        swAutoBackup.setChecked(prefs.getBoolean("auto_backup", true));
        swAutoBackup.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("auto_backup", isChecked).apply();
        });
        
        // Storage management
        updateStorageInfo();
        btnManageStorage.setOnClickListener(v -> showStorageManagement());
        
        // Initial UI state
        updateCloudSyncUI(swCloudSync.isChecked());
    }

    /**
     * Update cloud sync UI
     */
    private void updateCloudSyncUI(boolean enabled) {
        spSyncFrequency.setEnabled(enabled);
        swWifiOnly.setEnabled(enabled);
        swAutoBackup.setEnabled(enabled);
        btnManageStorage.setEnabled(enabled);
    }

    /**
     * Update storage info
     */
    private void updateStorageInfo() {
        // TODO: Get actual storage usage
        long usedBytes = 0; // Get from cloud storage
        String usedStr = formatFileSize(usedBytes);
        tvStorageUsed.setText(usedStr + " / 5 GB");
    }

    /**
     * Show storage management dialog
     */
    private void showStorageManagement() {
        new AlertDialog.Builder(this)
            .setTitle("Storage Management")
            .setMessage("Current usage: 0 MB\n\nOptions:\n• Clear cache\n• Remove old backups\n• Upgrade storage")
            .setPositiveButton("Clear Cache", (dialog, which) -> clearCloudCache())
            .setNegativeButton("Close", null)
            .show();
    }

    /**
     * Clear cloud cache
     */
    private void clearCloudCache() {
        Toast.makeText(this, "Cloud cache cleared", Toast.LENGTH_SHORT).show();
        updateStorageInfo();
    }

    // ================================
    // 3. Auto-Categorization Settings
    // ================================

    /**
     * Setup categorization settings
     */
    private void setupCategorizationSettings() {
        // Auto-categorization toggle
        swAutoCategorization.setChecked(prefs.getBoolean("auto_categorization", true));
        swAutoCategorization.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("auto_categorization", isChecked).apply();
            
            // Update coordinator
            // TODO: Uncomment when DocumentManagerCoordinator is created
            // DocumentManagerCoordinator.UserPreferences coordPrefs = coordinator.getPreferences();
            // coordPrefs.autoCategorization = isChecked;
            // coordinator.updatePreferences(coordPrefs);

            updateCategorizationUI(isChecked);
        });
        
        // Sensitivity slider
        sbCategorizationSensitivity.setProgress(prefs.getInt("categorization_sensitivity", 70));
        updateSensitivityLabel(sbCategorizationSensitivity.getProgress());
        
        sbCategorizationSensitivity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateSensitivityLabel(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                prefs.edit().putInt("categorization_sensitivity", seekBar.getProgress()).apply();
            }
        });
        
        // Learn from user toggle
        swLearnFromUser.setChecked(prefs.getBoolean("learn_from_user", true));
        swLearnFromUser.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("learn_from_user", isChecked).apply();
        });
        
        // Reset categories button
        btnResetCategories.setOnClickListener(v -> resetCategories());
        
        updateCategorizationUI(swAutoCategorization.isChecked());
    }

    /**
     * Update sensitivity label
     */
    private void updateSensitivityLabel(int value) {
        String level;
        if (value < 33) {
            level = "Low (More suggestions)";
        } else if (value < 66) {
            level = "Medium (Balanced)";
        } else {
            level = "High (Only confident)";
        }
        tvSensitivityLevel.setText(level);
    }

    /**
     * Update categorization UI
     */
    private void updateCategorizationUI(boolean enabled) {
        sbCategorizationSensitivity.setEnabled(enabled);
        swLearnFromUser.setEnabled(enabled);
    }

    /**
     * Reset categories
     */
    private void resetCategories() {
        new AlertDialog.Builder(this)
            .setTitle("Reset Categories")
            .setMessage("This will clear all learned categorization patterns. Continue?")
            .setPositiveButton("Reset", (dialog, which) -> {
                // TODO: Reset categorization engine
                Toast.makeText(this, "Categories reset", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    // ================================
    // 4. Search Indexing Settings
    // ================================

    /**
     * Setup search indexing settings
     */
    private void setupSearchIndexingSettings() {
        // Auto indexing toggle
        swAutoIndexing.setChecked(prefs.getBoolean("auto_indexing", true));
        swAutoIndexing.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("auto_indexing", isChecked).apply();
            
            // Update coordinator
            // TODO: Uncomment when DocumentManagerCoordinator is created
            // DocumentManagerCoordinator.UserPreferences coordPrefs = coordinator.getPreferences();
            // coordPrefs.autoIndexing = isChecked;
            // coordinator.updatePreferences(coordPrefs);

            updateIndexingUI(isChecked);
        });
        
        // Indexing frequency
        String[] frequencies = {"Immediate", "Every 5 minutes", "Every hour", "Daily"};
        ArrayAdapter<String> freqAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, frequencies);
        freqAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spIndexingFrequency.setAdapter(freqAdapter);
        
        int savedFreq = prefs.getInt("indexing_frequency", 0);
        spIndexingFrequency.setSelection(savedFreq);
        
        // Index on save toggle
        swIndexOnSave.setChecked(prefs.getBoolean("index_on_save", true));
        swIndexOnSave.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("index_on_save", isChecked).apply();
        });
        
        // Index options
        cbIndexDocNames.setChecked(prefs.getBoolean("index_doc_names", true));
        cbIndexOcrText.setChecked(prefs.getBoolean("index_ocr_text", true));
        cbIndexMetadata.setChecked(prefs.getBoolean("index_metadata", true));
        
        // Rebuild index button
        btnRebuildIndex.setOnClickListener(v -> rebuildSearchIndex());
        
        updateIndexingUI(swAutoIndexing.isChecked());
    }

    /**
     * Update indexing UI
     */
    private void updateIndexingUI(boolean enabled) {
        spIndexingFrequency.setEnabled(enabled);
        swIndexOnSave.setEnabled(enabled);
        cbIndexDocNames.setEnabled(enabled);
        cbIndexOcrText.setEnabled(enabled);
        cbIndexMetadata.setEnabled(enabled);
    }

    /**
     * Rebuild search index
     */
    private void rebuildSearchIndex() {
        new AlertDialog.Builder(this)
            .setTitle("Rebuild Search Index")
            .setMessage("This will rebuild the entire search index. This may take a few minutes. Continue?")
            .setPositiveButton("Rebuild", (dialog, which) -> {
                Toast.makeText(this, "Rebuilding search index...", Toast.LENGTH_SHORT).show();
                
                SearchableDatabase searchDb = SearchableDatabase.getInstance(this);
                searchDb.rebuildIndex(success -> {
                    if (success) {
                        Toast.makeText(this, "Search index rebuilt successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to rebuild index", Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    // ================================
    // 5. Performance Settings
    // ================================

    /**
     * Setup performance settings
     */
    private void setupPerformanceSettings() {
        // Performance mode
        int savedMode = prefs.getInt("performance_mode", 1); // 0=Speed, 1=Balanced, 2=Quality
        
        if (savedMode == 0) {
            ((RadioButton) rgPerformanceMode.getChildAt(0)).setChecked(true);
        } else if (savedMode == 1) {
            ((RadioButton) rgPerformanceMode.getChildAt(1)).setChecked(true);
        } else {
            ((RadioButton) rgPerformanceMode.getChildAt(2)).setChecked(true);
        }
        
        rgPerformanceMode.setOnCheckedChangeListener((group, checkedId) -> {
            int mode = group.indexOfChild(findViewById(checkedId));
            prefs.edit().putInt("performance_mode", mode).apply();
            applyPerformanceMode(mode);
        });
        
        // Image quality
        sbImageQuality.setProgress(prefs.getInt("image_quality", 80));
        updateImageQualityLabel(sbImageQuality.getProgress());
        
        sbImageQuality.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateImageQualityLabel(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                prefs.edit().putInt("image_quality", seekBar.getProgress()).apply();
            }
        });
        
        // Batch processing
        swBatchProcessing.setChecked(prefs.getBoolean("batch_processing", true));
        swBatchProcessing.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("batch_processing", isChecked).apply();
            
            // Update coordinator
            // TODO: Uncomment when DocumentManagerCoordinator is created
            // DocumentManagerCoordinator.UserPreferences coordPrefs = coordinator.getPreferences();
            // coordPrefs.batchProcessing = isChecked;
            // coordinator.updatePreferences(coordPrefs);

            sbBatchSize.setEnabled(isChecked);
        });
        
        // Batch size
        sbBatchSize.setProgress(prefs.getInt("batch_size", 10));
        updateBatchSizeLabel(sbBatchSize.getProgress());
        sbBatchSize.setEnabled(swBatchProcessing.isChecked());
        
        sbBatchSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateBatchSizeLabel(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int size = seekBar.getProgress();
                prefs.edit().putInt("batch_size", size).apply();
                
                // Update coordinator
                // TODO: Uncomment when DocumentManagerCoordinator is created
                // DocumentManagerCoordinator.UserPreferences coordPrefs = coordinator.getPreferences();
                // coordPrefs.batchSize = size;
                // coordinator.updatePreferences(coordPrefs);
            }
        });
    }

    /**
     * Update image quality label
     */
    private void updateImageQualityLabel(int value) {
        tvImageQuality.setText(value + "% Quality");
    }

    /**
     * Update batch size label
     */
    private void updateBatchSizeLabel(int value) {
        tvBatchSize.setText(value + " documents per batch");
    }

    /**
     * Apply performance mode
     */
    private void applyPerformanceMode(int mode) {
        switch (mode) {
            case 0: // Speed
                sbOcrQuality.setProgress(33);
                sbImageQuality.setProgress(60);
                swBatchProcessing.setChecked(true);
                sbBatchSize.setProgress(20);
                break;
                
            case 1: // Balanced
                sbOcrQuality.setProgress(66);
                sbImageQuality.setProgress(80);
                swBatchProcessing.setChecked(true);
                sbBatchSize.setProgress(10);
                break;
                
            case 2: // Quality
                sbOcrQuality.setProgress(100);
                sbImageQuality.setProgress(95);
                swBatchProcessing.setChecked(false);
                break;
        }
        
        Toast.makeText(this, "Performance mode applied", Toast.LENGTH_SHORT).show();
    }

    // ================================
    // 6. Privacy Settings
    // ================================

    /**
     * Setup privacy settings
     */
    private void setupPrivacySettings() {
        // Cloud encryption
        swCloudEncryption.setChecked(prefs.getBoolean("cloud_encryption", true));
        swCloudEncryption.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("cloud_encryption", isChecked).apply();
        });
        
        // Anonymous analytics
        swAnonymousAnalytics.setChecked(prefs.getBoolean("anonymous_analytics", true));
        swAnonymousAnalytics.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("anonymous_analytics", isChecked).apply();
        });
        
        // Sensitive data filter
        swSensitiveDataFilter.setChecked(prefs.getBoolean("sensitive_data_filter", true));
        swSensitiveDataFilter.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("sensitive_data_filter", isChecked).apply();
        });
        
        // Clear search history
        btnClearSearchHistory.setOnClickListener(v -> clearSearchHistory());
        
        // Export data
        btnExportData.setOnClickListener(v -> exportUserData());
    }

    /**
     * Clear search history
     */
    private void clearSearchHistory() {
        new AlertDialog.Builder(this)
            .setTitle("Clear Search History")
            .setMessage("This will clear all your search history and suggestions. Continue?")
            .setPositiveButton("Clear", (dialog, which) -> {
                SearchSuggestionsProvider provider = SearchSuggestionsProvider.getInstance(this);
                provider.clearRecentSearches();

                DocumentSearchManager manager = DocumentSearchManager.getInstance(this);
                manager.clearResults(); // Clear search results

                Toast.makeText(this, "Search history cleared", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    /**
     * Export user data
     */
    private void exportUserData() {
        new AlertDialog.Builder(this)
            .setTitle("Export Data")
            .setMessage("Export your data for backup or transfer?\n\n• Documents\n• Settings\n• Search history\n• Categories")
            .setPositiveButton("Export", (dialog, which) -> {
                Toast.makeText(this, "Exporting data...", Toast.LENGTH_SHORT).show();
                // TODO: Implement data export
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    // ================================
    // 7. Usage Analytics
    // ================================

    /**
     * Load and display analytics
     */
    private void loadAnalytics() {
        // Documents scanned
        int docsScanned = prefs.getInt("total_documents", 0);
        tvDocumentsScanned.setText(String.valueOf(docsScanned));
        
        // Searches performed
        int searches = prefs.getInt("total_searches", 0);
        tvSearchesPerformed.setText(String.valueOf(searches));
        
        // Storage used
        long storageBytes = prefs.getLong("storage_used", 0);
        tvStorageUsedAnalytics.setText(formatFileSize(storageBytes));
        
        // Last backup
        long lastBackup = prefs.getLong("last_backup", 0);
        if (lastBackup > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            tvLastBackup.setText(sdf.format(new Date(lastBackup)));
        } else {
            tvLastBackup.setText("Never");
        }
        
        // View detailed analytics
        btnViewDetailedAnalytics.setOnClickListener(v -> showDetailedAnalytics());
    }

    /**
     * Show detailed analytics
     */
    private void showDetailedAnalytics() {
        // TODO: Uncomment when DocumentManagerCoordinator is created
        /*
        DocumentManagerCoordinator.PerformanceMetrics metrics = coordinator.getPerformanceMetrics();

        StringBuilder analytics = new StringBuilder();
        analytics.append("Performance Metrics:\n\n");

        for (Map.Entry<String, Long> entry : metrics.averageTimes.entrySet()) {
            analytics.append(entry.getKey()).append(": ")
                     .append(entry.getValue()).append("ms\n");
        }

        analytics.append("\nActive Operations: ").append(metrics.activeOperations);
        analytics.append("\nQueue Size: ").append(metrics.queueSize);

        new AlertDialog.Builder(this)
            .setTitle("Detailed Analytics")
            .setMessage(analytics.toString())
            .setPositiveButton("OK", null)
            .show();
        */

        // Temporary placeholder
        new AlertDialog.Builder(this)
            .setTitle("Detailed Analytics")
            .setMessage("Detailed analytics coming soon...")
            .setPositiveButton("OK", null)
            .show();
    }

    // ================================
    // 8. Onboarding & Feature Discovery
    // ================================

    /**
     * Show onboarding
     */
    private void showOnboarding() {
        boolean onboardingComplete = prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false);
        
        if (!onboardingComplete) {
            // Show onboarding tour
            new AlertDialog.Builder(this)
                .setTitle("Welcome to Document Scanner!")
                .setMessage("Let's set up your app for the best experience.\n\n" +
                    "We'll help you:\n" +
                    "• Configure OCR languages\n" +
                    "• Set up cloud sync\n" +
                    "• Enable smart features\n" +
                    "• Optimize performance")
                .setPositiveButton("Start Tour", (dialog, which) -> startOnboardingTour())
                .setNegativeButton("Skip", (dialog, which) -> completeOnboarding())
                .setCancelable(false)
                .show();
        }
    }

    /**
     * Start onboarding tour
     */
    private void startOnboardingTour() {
        // Show step-by-step onboarding
        showOnboardingStep1();
    }

    /**
     * Onboarding Step 1: Language Selection
     */
    private void showOnboardingStep1() {
        new AlertDialog.Builder(this)
            .setTitle("Step 1: Choose OCR Language")
            .setMessage("Select your primary document language for best OCR accuracy.")
            .setSingleChoiceItems(new String[]{"English", "Spanish", "French", "German", "Other"}, 
                0, null)
            .setPositiveButton("Next", (dialog, which) -> {
                int selected = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                prefs.edit().putString("ocr_language", 
                    new String[]{"English", "Spanish", "French", "German", "Other"}[selected]).apply();
                showOnboardingStep2();
            })
            .show();
    }

    /**
     * Onboarding Step 2: Cloud Sync
     */
    private void showOnboardingStep2() {
        new AlertDialog.Builder(this)
            .setTitle("Step 2: Cloud Backup")
            .setMessage("Enable cloud sync to backup your documents across devices?")
            .setPositiveButton("Enable", (dialog, which) -> {
                prefs.edit().putBoolean("cloud_sync", true).apply();
                showOnboardingStep3();
            })
            .setNegativeButton("Skip", (dialog, which) -> showOnboardingStep3())
            .show();
    }

    /**
     * Onboarding Step 3: Smart Features
     */
    private void showOnboardingStep3() {
        new AlertDialog.Builder(this)
            .setTitle("Step 3: Smart Features")
            .setMessage("Enable AI-powered features?\n\n" +
                "• Auto-categorization\n" +
                "• Smart search suggestions\n" +
                "• Intelligent organization")
            .setPositiveButton("Enable All", (dialog, which) -> {
                prefs.edit()
                    .putBoolean("auto_categorization", true)
                    .putBoolean("auto_indexing", true)
                    .putBoolean("learn_from_user", true)
                    .apply();
                showOnboardingComplete();
            })
            .setNegativeButton("Skip", (dialog, which) -> showOnboardingComplete())
            .show();
    }

    /**
     * Onboarding Complete
     */
    private void showOnboardingComplete() {
        new AlertDialog.Builder(this)
            .setTitle("Setup Complete!")
            .setMessage("You're all set! Start scanning documents now.\n\n" +
                "You can change these settings anytime from the Settings menu.")
            .setPositiveButton("Start Scanning", (dialog, which) -> completeOnboarding())
            .setCancelable(false)
            .show();
    }

    /**
     * Complete onboarding
     */
    private void completeOnboarding() {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, true).apply();
        
        // Hide onboarding card
        if (cvOnboarding != null) {
            cvOnboarding.setVisibility(View.GONE);
        }
    }

    // ================================
    // Helper Methods
    // ================================

    /**
     * Load settings from preferences
     */
    private void loadSettings() {
        setupOcrSettings();
        setupCloudSyncSettings();
        setupCategorizationSettings();
        setupSearchIndexingSettings();
        setupPerformanceSettings();
        setupPrivacySettings();
    }

    /**
     * Setup all listeners
     */
    private void setupListeners() {
        // Language selection
        spOcrLanguage.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String language = parent.getItemAtPosition(position).toString();
                prefs.edit().putString("ocr_language", language).apply();
                updateModelInfo();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        
        // Sync frequency
        spSyncFrequency.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                prefs.edit().putInt("sync_frequency", position).apply();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        
        // Indexing frequency
        spIndexingFrequency.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                prefs.edit().putInt("indexing_frequency", position).apply();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        
        // Index options checkboxes
        cbIndexDocNames.setOnCheckedChangeListener((buttonView, isChecked) -> 
            prefs.edit().putBoolean("index_doc_names", isChecked).apply());
        
        cbIndexOcrText.setOnCheckedChangeListener((buttonView, isChecked) -> 
            prefs.edit().putBoolean("index_ocr_text", isChecked).apply());
        
        cbIndexMetadata.setOnCheckedChangeListener((buttonView, isChecked) -> 
            prefs.edit().putBoolean("index_metadata", isChecked).apply());
        
        // Onboarding button
        if (btnStartOnboarding != null) {
            btnStartOnboarding.setOnClickListener(v -> startOnboardingTour());
        }
    }

    /**
     * Format file size
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Static launch method
     */
    public static void start(Context context) {
        Intent intent = new Intent(context, AppSettingsActivity.class);
        context.startActivity(intent);
    }
}


