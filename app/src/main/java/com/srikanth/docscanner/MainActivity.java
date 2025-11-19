package com.srikanth.docscanner;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * MainActivity for Document Scanner App
 * Handles document capture and gallery navigation with proper permission management
 */
public class MainActivity extends AppCompatActivity {

    // UI Components
    private MaterialButton btnCapture;
    private MaterialButton btnGallery;
    private ImageView ivDocumentPreview;
    private TextView tvPlaceholder;

    // Permission launcher for camera access
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;

    // Request codes
    private static final int CAMERA_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize OpenCV for image processing (uncomment when OpenCV is added)
        // OpenCVHelper.initOpenCV(this);

        // Initialize UI components
        initializeViews();

        // Setup permission launcher
        setupPermissionLauncher();

        // Setup button click listeners
        setupClickListeners();
    }

    /**
     * Initialize all UI components using findViewById
     */
    private void initializeViews() {
        btnCapture = findViewById(R.id.btnCapture);
        btnGallery = findViewById(R.id.btnGallery);
        ivDocumentPreview = findViewById(R.id.ivDocumentPreview);
        tvPlaceholder = findViewById(R.id.tvPlaceholder);
    }

    /**
     * Setup modern permission launcher using Activity Result API
     */
    private void setupPermissionLauncher() {
        requestCameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        // Permission granted - proceed with camera
                        openCamera();
                    } else {
                        // Permission denied - show explanation
                        showPermissionDeniedDialog();
                    }
                }
        );
    }

    /**
     * Setup click listeners for all buttons using modern practices
     */
    private void setupClickListeners() {
        // Capture Document button click handler
        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleCaptureButtonClick();
            }
        });

        // View Gallery button click handler
        btnGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleGalleryButtonClick();
            }
        });
    }

    /**
     * Handle Capture Document button click
     * Checks camera permission and initiates capture
     */
    private void handleCaptureButtonClick() {
        if (checkCameraPermission()) {
            // Permission already granted
            openCamera();
        } else {
            // Request camera permission
            requestCameraPermission();
        }
    }

    /**
     * Handle View Gallery button click
     * Navigate to gallery activity
     */
    private void handleGalleryButtonClick() {
        // Show toast notification
        Toast.makeText(this, R.string.opening_gallery, Toast.LENGTH_SHORT).show();

        // Navigate to Gallery Activity
        Intent intent = new Intent(MainActivity.this, GalleryActivity.class);
        startActivity(intent);
    }

    /**
     * Check if camera permission is granted
     * @return true if permission granted, false otherwise
     */
    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request camera permission using modern Activity Result API
     */
    private void requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            // Show explanation dialog before requesting permission
            showPermissionRationaleDialog();
        } else {
            // Directly request permission
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    /**
     * Show dialog explaining why camera permission is needed
     */
    private void showPermissionRationaleDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Camera Permission Required")
                .setMessage(R.string.camera_permission_required)
                .setPositiveButton("Grant", (dialog, which) -> {
                    requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    /**
     * Show dialog when permission is denied
     */
    private void showPermissionDeniedDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Permission Denied")
                .setMessage(R.string.permission_denied)
                .setPositiveButton("OK", null)
                .show();
    }

    /**
     * Open camera to capture document
     * Launches CameraActivity with full CameraX implementation
     */
    private void openCamera() {
        // Launch CameraActivity
        Intent intent = new Intent(MainActivity.this, CameraActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if we have any captured documents to display
        // This is where you would load saved documents if needed
    }
}


