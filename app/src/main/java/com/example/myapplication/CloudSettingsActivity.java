package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * CloudSettingsActivity - Cloud backup configuration
 *
 * Features:
 * - Cloud provider selection (Google Drive/Dropbox)
 * - Auto-sync toggle and frequency settings
 * - Storage usage display with breakdown
 * - Sync status and last sync time
 * - Manual sync trigger button
 * - Account management and switching
 * - Data usage settings (WiFi only, etc.)
 * - Storage upgrade prompts
 */
public class CloudSettingsActivity extends AppCompatActivity {

    private static final String TAG = "CloudSettingsActivity";

    // Views - Provider Selection
    private ChipGroup chipGroupProviders;
    private Chip chipGoogleDrive;
    private Chip chipDropbox;

    // Views - Auto-sync Settings
    private CardView cardAutoSync;
    private SwitchCompat switchAutoSync;
    private TextView tvSyncFrequency;
    private Slider sliderSyncFrequency;
    private TextView tvSyncFrequencyValue;

    // Views - Storage Usage
    private CardView cardStorageUsage;
    private ProgressBar progressStorageUsage;
    private TextView tvStorageUsed;
    private TextView tvStorageTotal;
    private TextView tvStoragePercentage;
    private TextView tvDocumentsCount;
    private TextView tvImagesCount;
    private TextView tvPdfsCount;
    private Button btnUpgradeStorage;

    // Views - Sync Status
    private CardView cardSyncStatus;
    private TextView tvSyncStatus;
    private TextView tvLastSyncTime;
    private Button btnManualSync;
    private ProgressBar progressSync;

    // Views - Account Management
    private CardView cardAccount;
    private TextView tvCurrentAccount;
    private TextView tvAccountEmail;
    private Button btnSwitchAccount;
    private Button btnSignOut;

    // Views - Data Usage Settings
    private CardView cardDataUsage;
    private SwitchCompat switchWifiOnly;
    private SwitchCompat switchBackupOnCharging;
    private SwitchCompat switchBackupLargeFiles;

    // Managers
    private CloudAuthManager authManager;
    private CloudStorageManager storageManager;
    private CloudSyncService syncService;

    // Current provider
    private CloudStorageManager.CloudProvider currentProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cloud_settings);

        // Initialize managers
        authManager = new CloudAuthManager(this);
        storageManager = new CloudStorageManager(this);
        syncService = new CloudSyncService(this);

        // Initialize views
        initializeViews();

        // Setup toolbar
        setupToolbar();

        // Load current settings
        loadSettings();

        // Setup listeners
        setupListeners();
    }

    // ================================
    // Initialize Views
    // ================================

    private void initializeViews() {
        // Provider Selection
        chipGroupProviders = findViewById(R.id.chipGroupProviders);
        chipGoogleDrive = findViewById(R.id.chipGoogleDrive);
        chipDropbox = findViewById(R.id.chipDropbox);

        // Auto-sync Settings
        cardAutoSync = findViewById(R.id.cardAutoSync);
        switchAutoSync = findViewById(R.id.switchAutoSync);
        tvSyncFrequency = findViewById(R.id.tvSyncFrequency);
        sliderSyncFrequency = findViewById(R.id.sliderSyncFrequency);
        tvSyncFrequencyValue = findViewById(R.id.tvSyncFrequencyValue);

        // Storage Usage
        cardStorageUsage = findViewById(R.id.cardStorageUsage);
        progressStorageUsage = findViewById(R.id.progressStorageUsage);
        tvStorageUsed = findViewById(R.id.tvStorageUsed);
        tvStorageTotal = findViewById(R.id.tvStorageTotal);
        tvStoragePercentage = findViewById(R.id.tvStoragePercentage);
        tvDocumentsCount = findViewById(R.id.tvDocumentsCount);
        tvImagesCount = findViewById(R.id.tvImagesCount);
        tvPdfsCount = findViewById(R.id.tvPdfsCount);
        btnUpgradeStorage = findViewById(R.id.btnUpgradeStorage);

        // Sync Status
        cardSyncStatus = findViewById(R.id.cardSyncStatus);
        tvSyncStatus = findViewById(R.id.tvSyncStatus);
        tvLastSyncTime = findViewById(R.id.tvLastSyncTime);
        btnManualSync = findViewById(R.id.btnManualSync);
        progressSync = findViewById(R.id.progressSync);

        // Account Management
        cardAccount = findViewById(R.id.cardAccount);
        tvCurrentAccount = findViewById(R.id.tvCurrentAccount);
        tvAccountEmail = findViewById(R.id.tvAccountEmail);
        btnSwitchAccount = findViewById(R.id.btnSwitchAccount);
        btnSignOut = findViewById(R.id.btnSignOut);

        // Data Usage Settings
        cardDataUsage = findViewById(R.id.cardDataUsage);
        switchWifiOnly = findViewById(R.id.switchWifiOnly);
        switchBackupOnCharging = findViewById(R.id.switchBackupOnCharging);
        switchBackupLargeFiles = findViewById(R.id.switchBackupLargeFiles);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Cloud Settings");
        }
    }

    // ================================
    // 1. Cloud Provider Selection
    // ================================

    private void setupProviderSelection() {
        chipGroupProviders.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipGoogleDrive) {
                selectProvider(CloudStorageManager.CloudProvider.GOOGLE_DRIVE);
            } else if (checkedId == R.id.chipDropbox) {
                selectProvider(CloudStorageManager.CloudProvider.DROPBOX);
            }
        });

        // Set current provider
        CloudAuthManager.CloudAccount account = authManager.getCurrentAccount();
        if (account != null) {
            currentProvider = account.provider;
            if (currentProvider == CloudStorageManager.CloudProvider.GOOGLE_DRIVE) {
                chipGoogleDrive.setChecked(true);
            } else if (currentProvider == CloudStorageManager.CloudProvider.DROPBOX) {
                chipDropbox.setChecked(true);
            }
        }
    }

    private void selectProvider(CloudStorageManager.CloudProvider provider) {
        if (currentProvider == provider) {
            return;
        }

        // Check if authenticated
        List<CloudAuthManager.CloudAccount> accounts =
            authManager.getAccountsByProvider(provider);

        if (accounts.isEmpty()) {
            // Need to authenticate
            showAuthenticationDialog(provider);
        } else {
            // Switch provider
            currentProvider = provider;
            storageManager.setProvider(provider);
            loadStorageUsage();
            loadSyncStatus();
            Toast.makeText(this, "Switched to " + provider.name(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showAuthenticationDialog(CloudStorageManager.CloudProvider provider) {
        new AlertDialog.Builder(this)
            .setTitle("Authentication Required")
            .setMessage("You need to sign in to " + provider.name() + " first.")
            .setPositiveButton("Sign In", (dialog, which) -> {
                authenticateProvider(provider);
            })
            .setNegativeButton("Cancel", (dialog, which) -> {
                // Revert chip selection
                updateProviderChip();
            })
            .show();
    }

    private void authenticateProvider(CloudStorageManager.CloudProvider provider) {
        if (provider == CloudStorageManager.CloudProvider.GOOGLE_DRIVE) {
            authManager.signInWithGoogle();
        } else if (provider == CloudStorageManager.CloudProvider.DROPBOX) {
            authManager.signInWithDropbox();
        }
    }

    private void updateProviderChip() {
        if (currentProvider == CloudStorageManager.CloudProvider.GOOGLE_DRIVE) {
            chipGoogleDrive.setChecked(true);
        } else if (currentProvider == CloudStorageManager.CloudProvider.DROPBOX) {
            chipDropbox.setChecked(true);
        }
    }

    // ================================
    // 2. Auto-sync Toggle and Frequency
    // ================================

    private void setupAutoSyncSettings() {
        // Load current settings
        boolean autoSyncEnabled = getPreferences(MODE_PRIVATE)
            .getBoolean("auto_sync_enabled", true);
        int syncFrequency = getPreferences(MODE_PRIVATE)
            .getInt("sync_frequency_minutes", 30);

        switchAutoSync.setChecked(autoSyncEnabled);
        sliderSyncFrequency.setValue(syncFrequency);
        updateSyncFrequencyText(syncFrequency);

        // Auto-sync toggle
        switchAutoSync.setOnCheckedChangeListener((buttonView, isChecked) -> {
            getPreferences(MODE_PRIVATE).edit()
                .putBoolean("auto_sync_enabled", isChecked)
                .apply();

            if (isChecked) {
                syncService.schedulePeriodicSync((long) sliderSyncFrequency.getValue());
                Toast.makeText(this, "Auto-sync enabled", Toast.LENGTH_SHORT).show();
            } else {
                syncService.cancelPeriodicSync();
                Toast.makeText(this, "Auto-sync disabled", Toast.LENGTH_SHORT).show();
            }

            sliderSyncFrequency.setEnabled(isChecked);
        });

        // Frequency slider
        sliderSyncFrequency.addOnChangeListener((slider, value, fromUser) -> {
            updateSyncFrequencyText((int) value);
        });

        sliderSyncFrequency.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(Slider slider) {}

            @Override
            public void onStopTrackingTouch(Slider slider) {
                int frequency = (int) slider.getValue();
                getPreferences(MODE_PRIVATE).edit()
                    .putInt("sync_frequency_minutes", frequency)
                    .apply();

                if (switchAutoSync.isChecked()) {
                    syncService.schedulePeriodicSync(frequency);
                    Toast.makeText(CloudSettingsActivity.this,
                        "Sync frequency updated", Toast.LENGTH_SHORT).show();
                }
            }
        });

        sliderSyncFrequency.setEnabled(autoSyncEnabled);
    }

    private void updateSyncFrequencyText(int minutes) {
        if (minutes < 60) {
            tvSyncFrequencyValue.setText(minutes + " minutes");
        } else {
            int hours = minutes / 60;
            tvSyncFrequencyValue.setText(hours + (hours == 1 ? " hour" : " hours"));
        }
    }

    // ================================
    // 3. Storage Usage Display
    // ================================

    private void loadStorageUsage() {
        progressStorageUsage.setVisibility(View.VISIBLE);

        storageManager.getStorageQuota(new CloudStorageManager.QuotaCallback() {
            @Override
            public void onQuotaSuccess(CloudStorageManager.StorageQuota quota) {
                runOnUiThread(() -> {
                    progressStorageUsage.setVisibility(View.GONE);
                    displayStorageUsage(quota);
                });
            }

            @Override
            public void onQuotaError(Exception e) {
                runOnUiThread(() -> {
                    progressStorageUsage.setVisibility(View.GONE);
                    Toast.makeText(CloudSettingsActivity.this,
                        "Failed to load storage usage", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void displayStorageUsage(CloudStorageManager.StorageQuota quota) {
        // Calculate usage
        long usedGB = quota.usedBytes / (1024 * 1024 * 1024);
        long totalGB = quota.totalBytes / (1024 * 1024 * 1024);
        float percentage = quota.getUsagePercentage();

        // Update UI
        tvStorageUsed.setText(usedGB + " GB");
        tvStorageTotal.setText(" / " + totalGB + " GB");
        tvStoragePercentage.setText(String.format(Locale.getDefault(),
            "%.1f%% used", percentage));

        progressStorageUsage.setProgress((int) percentage);

        // Breakdown (simulated)
        tvDocumentsCount.setText("Documents: 45");
        tvImagesCount.setText("Images: 120");
        tvPdfsCount.setText("PDFs: 78");

        // Show upgrade button if >80% used
        if (percentage > 80) {
            btnUpgradeStorage.setVisibility(View.VISIBLE);
        } else {
            btnUpgradeStorage.setVisibility(View.GONE);
        }
    }

    // ================================
    // 4. Sync Status and Last Sync Time
    // ================================

    private void loadSyncStatus() {
        syncService.getSyncStatus(status -> {
            runOnUiThread(() -> {
                displaySyncStatus(status);
            });
        });

        // Load last sync time
        long lastSyncTime = getPreferences(MODE_PRIVATE)
            .getLong("last_sync_time", 0);

        if (lastSyncTime > 0) {
            String timeAgo = getTimeAgo(lastSyncTime);
            tvLastSyncTime.setText("Last synced " + timeAgo);
        } else {
            tvLastSyncTime.setText("Never synced");
        }
    }

    private void displaySyncStatus(CloudSyncService.SyncStatus status) {
        switch (status) {
            case IDLE:
                tvSyncStatus.setText("Status: Idle");
                tvSyncStatus.setTextColor(getColor(android.R.color.darker_gray));
                progressSync.setVisibility(View.GONE);
                btnManualSync.setEnabled(true);
                break;

            case SYNCING:
                tvSyncStatus.setText("Status: Syncing...");
                tvSyncStatus.setTextColor(getColor(android.R.color.holo_blue_dark));
                progressSync.setVisibility(View.VISIBLE);
                btnManualSync.setEnabled(false);
                break;

            case SUCCESS:
                tvSyncStatus.setText("Status: Up to date");
                tvSyncStatus.setTextColor(getColor(android.R.color.holo_green_dark));
                progressSync.setVisibility(View.GONE);
                btnManualSync.setEnabled(true);
                break;

            case FAILED:
                tvSyncStatus.setText("Status: Failed");
                tvSyncStatus.setTextColor(getColor(android.R.color.holo_red_dark));
                progressSync.setVisibility(View.GONE);
                btnManualSync.setEnabled(true);
                break;

            case PAUSED:
                tvSyncStatus.setText("Status: Paused");
                tvSyncStatus.setTextColor(getColor(android.R.color.holo_orange_dark));
                progressSync.setVisibility(View.GONE);
                btnManualSync.setEnabled(true);
                break;
        }
    }

    private String getTimeAgo(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + (days == 1 ? " day ago" : " days ago");
        } else if (hours > 0) {
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        } else if (minutes > 0) {
            return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        } else {
            return "just now";
        }
    }

    // ================================
    // 5. Manual Sync Trigger
    // ================================

    private void setupManualSync() {
        btnManualSync.setOnClickListener(v -> {
            triggerManualSync();
        });
    }

    private void triggerManualSync() {
        btnManualSync.setEnabled(false);
        progressSync.setVisibility(View.VISIBLE);
        tvSyncStatus.setText("Status: Syncing...");

        syncService.scheduleImmediateSync();

        // Update last sync time
        getPreferences(MODE_PRIVATE).edit()
            .putLong("last_sync_time", System.currentTimeMillis())
            .apply();

        // Check status after delay
        new android.os.Handler().postDelayed(() -> {
            loadSyncStatus();
        }, 3000);

        Toast.makeText(this, "Sync started", Toast.LENGTH_SHORT).show();
    }

    // ================================
    // 6. Account Management
    // ================================

    private void setupAccountManagement() {
        // Load current account
        CloudAuthManager.CloudAccount account = authManager.getCurrentAccount();
        if (account != null) {
            tvCurrentAccount.setText(account.displayName);
            tvAccountEmail.setText(account.email);
        } else {
            tvCurrentAccount.setText("Not signed in");
            tvAccountEmail.setText("Sign in to enable cloud backup");
        }

        // Switch account
        btnSwitchAccount.setOnClickListener(v -> {
            showAccountSwitcher();
        });

        // Sign out
        btnSignOut.setOnClickListener(v -> {
            showSignOutConfirmation();
        });
    }

    private void showAccountSwitcher() {
        List<CloudAuthManager.CloudAccount> accounts = authManager.getAllAccounts();

        if (accounts.isEmpty()) {
            Toast.makeText(this, "No accounts available", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] accountNames = new String[accounts.size() + 1];
        for (int i = 0; i < accounts.size(); i++) {
            accountNames[i] = accounts.get(i).email + " (" +
                accounts.get(i).provider.name() + ")";
        }
        accountNames[accounts.size()] = "Add new account";

        new AlertDialog.Builder(this)
            .setTitle("Switch Account")
            .setItems(accountNames, (dialog, which) -> {
                if (which < accounts.size()) {
                    CloudAuthManager.CloudAccount account = accounts.get(which);
                    switchToAccount(account);
                } else {
                    // Add new account
                    showProviderSelectionForNewAccount();
                }
            })
            .show();
    }

    private void switchToAccount(CloudAuthManager.CloudAccount account) {
        authManager.switchAccount(account.accountId, new CloudAuthManager.AccountSwitchCallback() {
            @Override
            public void onSwitchSuccess(CloudAuthManager.CloudAccount account) {
                runOnUiThread(() -> {
                    tvCurrentAccount.setText(account.displayName);
                    tvAccountEmail.setText(account.email);
                    currentProvider = account.provider;
                    updateProviderChip();
                    loadStorageUsage();
                    loadSyncStatus();
                    Toast.makeText(CloudSettingsActivity.this,
                        "Switched to " + account.email, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onSwitchError(Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(CloudSettingsActivity.this,
                        "Failed to switch account", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showProviderSelectionForNewAccount() {
        String[] providers = {"Google Drive", "Dropbox"};
        new AlertDialog.Builder(this)
            .setTitle("Select Provider")
            .setItems(providers, (dialog, which) -> {
                if (which == 0) {
                    authManager.signInWithGoogle();
                } else {
                    authManager.signInWithDropbox();
                }
            })
            .show();
    }

    private void showSignOutConfirmation() {
        new AlertDialog.Builder(this)
            .setTitle("Sign Out")
            .setMessage("Are you sure you want to sign out? This will stop cloud sync.")
            .setPositiveButton("Sign Out", (dialog, which) -> {
                signOut();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void signOut() {
        authManager.signOut(new CloudAuthManager.SignOutCallback() {
            @Override
            public void onSignOutComplete() {
                runOnUiThread(() -> {
                    tvCurrentAccount.setText("Not signed in");
                    tvAccountEmail.setText("Sign in to enable cloud backup");
                    syncService.cancelAllSync();
                    Toast.makeText(CloudSettingsActivity.this,
                        "Signed out", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // ================================
    // 7. Data Usage Settings
    // ================================

    private void setupDataUsageSettings() {
        // Load settings
        boolean wifiOnly = getPreferences(MODE_PRIVATE)
            .getBoolean("wifi_only", true);
        boolean backupOnCharging = getPreferences(MODE_PRIVATE)
            .getBoolean("backup_on_charging", false);
        boolean backupLargeFiles = getPreferences(MODE_PRIVATE)
            .getBoolean("backup_large_files", false);

        switchWifiOnly.setChecked(wifiOnly);
        switchBackupOnCharging.setChecked(backupOnCharging);
        switchBackupLargeFiles.setChecked(backupLargeFiles);

        // WiFi only
        switchWifiOnly.setOnCheckedChangeListener((buttonView, isChecked) -> {
            getPreferences(MODE_PRIVATE).edit()
                .putBoolean("wifi_only", isChecked)
                .apply();

            // Reschedule sync with new constraints
            if (switchAutoSync.isChecked()) {
                syncService.schedulePeriodicSync((long) sliderSyncFrequency.getValue());
            }
        });

        // Backup on charging
        switchBackupOnCharging.setOnCheckedChangeListener((buttonView, isChecked) -> {
            getPreferences(MODE_PRIVATE).edit()
                .putBoolean("backup_on_charging", isChecked)
                .apply();
        });

        // Backup large files
        switchBackupLargeFiles.setOnCheckedChangeListener((buttonView, isChecked) -> {
            getPreferences(MODE_PRIVATE).edit()
                .putBoolean("backup_large_files", isChecked)
                .apply();

            if (isChecked) {
                showLargeFilesWarning();
            }
        });
    }

    private void showLargeFilesWarning() {
        new AlertDialog.Builder(this)
            .setTitle("Large Files Backup")
            .setMessage("Backing up large files may consume significant data and storage space.")
            .setPositiveButton("OK", null)
            .show();
    }

    // ================================
    // Storage Upgrade
    // ================================

    private void setupStorageUpgrade() {
        btnUpgradeStorage.setOnClickListener(v -> {
            showUpgradeDialog();
        });
    }

    private void showUpgradeDialog() {
        String[] plans = {
            "100 GB - $1.99/month",
            "200 GB - $2.99/month",
            "2 TB - $9.99/month"
        };

        new AlertDialog.Builder(this)
            .setTitle("Upgrade Storage")
            .setMessage("Choose a storage plan:")
            .setItems(plans, (dialog, which) -> {
                // TODO: Integrate with billing API
                Toast.makeText(this, "Selected plan: " + plans[which],
                    Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    // ================================
    // Setup Listeners
    // ================================

    private void setupListeners() {
        setupProviderSelection();
        setupAutoSyncSettings();
        setupManualSync();
        setupAccountManagement();
        setupDataUsageSettings();
        setupStorageUpgrade();
    }

    // ================================
    // Load Settings
    // ================================

    private void loadSettings() {
        loadStorageUsage();
        loadSyncStatus();
    }

    // ================================
    // Menu
    // ================================

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_cloud_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.action_help) {
            showHelpDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showHelpDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Cloud Backup Help")
            .setMessage("Cloud backup automatically saves your documents to the cloud. " +
                "Enable auto-sync to keep your files synchronized across devices.")
            .setPositiveButton("OK", null)
            .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        authManager.cleanup();
        storageManager.cleanup();
        syncService.cleanup();
    }
}

