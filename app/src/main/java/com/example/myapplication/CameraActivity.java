package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * CameraActivity - Full-featured document scanner camera implementation
 * Uses CameraX for camera operations with preview, capture, and auto-focus
 */
public class CameraActivity extends AppCompatActivity {

    private static final String TAG = "CameraActivity";
    private static final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";

    // Camera components
    private PreviewView previewView;
    private ImageCapture imageCapture;
    private Camera camera;
    private ProcessCameraProvider cameraProvider;

    // UI components
    private MaterialButton btnBack;
    private MaterialButton btnFlash;
    private MaterialButton btnGallery;
    private FloatingActionButton btnCapture;
    private MaterialCardView cardLastImage;
    private ImageView ivLastCaptured;
    private TextView tvDocumentCount;
    private TextView tvAlignmentHint;
    private ProgressBar progressBar;
    private TextView tvCaptureStatus;

    // State variables
    private int flashMode = ImageCapture.FLASH_MODE_AUTO;
    private int capturedImageCount = 0;
    private File lastCapturedFile;
    private File outputDirectory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_activity);

        // Check camera permission
        if (!hasCameraPermission()) {
            Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        initializeViews();

        // Setup click listeners
        setupClickListeners();

        // Create output directory
        outputDirectory = getOutputDirectory();

        // Start camera
        startCamera();
    }

    /**
     * Initialize all UI components
     */
    private void initializeViews() {
        previewView = findViewById(R.id.previewView);
        btnBack = findViewById(R.id.btnBack);
        btnFlash = findViewById(R.id.btnFlash);
        btnGallery = findViewById(R.id.btnGallery);
        btnCapture = findViewById(R.id.btnCapture);
        cardLastImage = findViewById(R.id.cardLastImage);
        ivLastCaptured = findViewById(R.id.ivLastCaptured);
        tvDocumentCount = findViewById(R.id.tvDocumentCount);
        tvAlignmentHint = findViewById(R.id.tvAlignmentHint);
        progressBar = findViewById(R.id.progressBar);
        tvCaptureStatus = findViewById(R.id.tvCaptureStatus);

        // Set initial visibility
        cardLastImage.setVisibility(View.GONE);
        tvDocumentCount.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        tvCaptureStatus.setVisibility(View.GONE);

        // Update flash button icon based on initial mode
        updateFlashButton();
    }

    /**
     * Setup click listeners for all interactive elements
     */
    private void setupClickListeners() {
        // Back button - close camera
        btnBack.setOnClickListener(v -> finish());

        // Flash toggle button
        btnFlash.setOnClickListener(v -> toggleFlash());

        // Gallery button - open gallery activity
        btnGallery.setOnClickListener(v -> openGallery());

        // Capture button - take photo
        btnCapture.setOnClickListener(v -> capturePhoto());

        // Last captured image - view in gallery
        cardLastImage.setOnClickListener(v -> viewLastImage());

        // Touch to focus
        previewView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                focusOnTap(event.getX(), event.getY());
                return true;
            }
            return false;
        });
    }

    /**
     * Check if camera permission is granted
     */
    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Start CameraX and bind camera lifecycle
     */
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
                Toast.makeText(this,
                        "Failed to start camera: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    /**
     * Bind camera use cases (Preview and ImageCapture)
     */
    private void bindCameraUseCases() {
        // Unbind all use cases before rebinding
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }

        // Build Preview use case
        Preview preview = new Preview.Builder()
                .build();

        // Build ImageCapture use case with high quality settings
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setFlashMode(flashMode)
                .build();

        // Select back camera
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        try {
            // Bind use cases to camera
            camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
            );

            // Attach preview to PreviewView
            preview.setSurfaceProvider(previewView.getSurfaceProvider());

            Log.d(TAG, "Camera use cases bound successfully");

        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
            Toast.makeText(this,
                    "Failed to bind camera: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Capture photo and save to storage
     */
    private void capturePhoto() {
        // Ensure imageCapture is initialized
        if (imageCapture == null) {
            Log.e(TAG, "ImageCapture is null");
            Toast.makeText(this, "Camera not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading UI
        showProcessing(true);

        // Disable capture button to prevent multiple clicks
        btnCapture.setEnabled(false);

        // Create timestamped output file
        String timestamp = new SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(new Date());
        File photoFile = new File(outputDirectory, "DOC_" + timestamp + ".jpg");

        // Create output options
        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        // Take picture
        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        Uri savedUri = Uri.fromFile(photoFile);
                        String msg = "Document captured successfully";

                        Log.d(TAG, msg + ": " + savedUri);

                        // Update UI
                        runOnUiThread(() -> {
                            showProcessing(false);
                            btnCapture.setEnabled(true);

                            // Update last captured image preview
                            lastCapturedFile = photoFile;
                            updateLastImagePreview(savedUri);

                            // Increment counter
                            capturedImageCount++;
                            updateDocumentCount();

                            // Hide alignment hint after first capture
                            tvAlignmentHint.setVisibility(View.GONE);

                            Toast.makeText(CameraActivity.this, msg, Toast.LENGTH_SHORT).show();

                            // Show enhancement options dialog
                            showEnhancementDialog(photoFile);
                        });
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException error) {
                        Log.e(TAG, "Photo capture failed: " + error.getMessage(), error);

                        runOnUiThread(() -> {
                            showProcessing(false);
                            btnCapture.setEnabled(true);

                            Toast.makeText(CameraActivity.this,
                                    "Failed to capture: " + error.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                }
        );
    }

    /**
     * Toggle flash mode (Auto -> On -> Off -> Auto)
     */
    private void toggleFlash() {
        switch (flashMode) {
            case ImageCapture.FLASH_MODE_AUTO:
                flashMode = ImageCapture.FLASH_MODE_ON;
                break;
            case ImageCapture.FLASH_MODE_ON:
                flashMode = ImageCapture.FLASH_MODE_OFF;
                break;
            case ImageCapture.FLASH_MODE_OFF:
                flashMode = ImageCapture.FLASH_MODE_AUTO;
                break;
        }

        // Update ImageCapture flash mode
        if (imageCapture != null) {
            imageCapture.setFlashMode(flashMode);
        }

        // Update button icon and show toast
        updateFlashButton();

        String mode = flashMode == ImageCapture.FLASH_MODE_AUTO ? "Auto" :
                     flashMode == ImageCapture.FLASH_MODE_ON ? "On" : "Off";
        Toast.makeText(this, "Flash: " + mode, Toast.LENGTH_SHORT).show();
    }

    /**
     * Update flash button icon based on current flash mode
     */
    private void updateFlashButton() {
        int iconRes;
        switch (flashMode) {
            case ImageCapture.FLASH_MODE_ON:
                iconRes = android.R.drawable.ic_menu_day; // Flash on icon
                break;
            case ImageCapture.FLASH_MODE_OFF:
                iconRes = android.R.drawable.ic_menu_close_clear_cancel; // Flash off icon
                break;
            default: // FLASH_MODE_AUTO
                iconRes = android.R.drawable.ic_menu_gallery; // Auto icon
                break;
        }
        btnFlash.setIcon(ContextCompat.getDrawable(this, iconRes));
    }

    /**
     * Implement tap to focus functionality
     */
    private void focusOnTap(float x, float y) {
        if (camera == null) {
            return;
        }

        // Create metering point from tap coordinates
        MeteringPoint meteringPoint = previewView.getMeteringPointFactory()
                .createPoint(x, y);

        // Create focus and metering action
        FocusMeteringAction action = new FocusMeteringAction.Builder(meteringPoint)
                .setAutoCancelDuration(3, TimeUnit.SECONDS)
                .build();

        // Start focus and metering
        camera.getCameraControl().startFocusAndMetering(action);

        // Show visual feedback (optional)
        Toast.makeText(this, "Focusing...", Toast.LENGTH_SHORT).show();
    }

    /**
     * Open gallery activity to view all captured documents
     */
    private void openGallery() {
        Intent intent = new Intent(this, GalleryActivity.class);
        startActivity(intent);
    }

    /**
     * View the last captured image
     */
    private void viewLastImage() {
        if (lastCapturedFile != null && lastCapturedFile.exists()) {
            // Launch PreviewActivity for enhancement options
            PreviewActivity.start(this, lastCapturedFile.getAbsolutePath());
        } else {
            Toast.makeText(this, "No image to preview", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Show enhancement options dialog after capture
     */
    private void showEnhancementDialog(File capturedFile) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Document Captured")
                .setMessage("What would you like to do with this document?")
                .setPositiveButton("Enhance & Generate PDF", (dialog, which) -> {
                    // Launch PreviewActivity for enhancement and PDF generation
                    PreviewActivity.start(this, capturedFile.getAbsolutePath());
                })
                .setNeutralButton("Crop First", (dialog, which) -> {
                    // Launch ImageCropActivity for manual cropping
                    ImageCropActivity.startForResult(this, capturedFile.getAbsolutePath(), 1001);
                })
                .setNegativeButton("Keep As-Is", null)
                .setCancelable(true)
                .show();
    }

    /**
     * Update the last captured image preview
     */
    private void updateLastImagePreview(Uri imageUri) {
        try {
            // Load bitmap from file
            Bitmap bitmap = BitmapFactory.decodeFile(lastCapturedFile.getAbsolutePath());

            if (bitmap != null) {
                // Scale down for preview to avoid memory issues
                int maxSize = 200;
                float scale = Math.min(
                    (float) maxSize / bitmap.getWidth(),
                    (float) maxSize / bitmap.getHeight()
                );

                int scaledWidth = Math.round(bitmap.getWidth() * scale);
                int scaledHeight = Math.round(bitmap.getHeight() * scale);

                Bitmap scaledBitmap = Bitmap.createScaledBitmap(
                    bitmap,
                    scaledWidth,
                    scaledHeight,
                    true
                );

                // Set the scaled bitmap
                ivLastCaptured.setImageBitmap(scaledBitmap);
                cardLastImage.setVisibility(View.VISIBLE);

                // Recycle original bitmap to free memory
                if (bitmap != scaledBitmap) {
                    bitmap.recycle();
                }

                Log.d(TAG, "Preview updated successfully");
            } else {
                Log.e(TAG, "Failed to decode bitmap from file");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating preview", e);
            Toast.makeText(this, "Error loading preview", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Update document count badge
     */
    private void updateDocumentCount() {
        if (capturedImageCount > 0) {
            tvDocumentCount.setText(String.valueOf(capturedImageCount));
            tvDocumentCount.setVisibility(View.VISIBLE);
        } else {
            tvDocumentCount.setVisibility(View.GONE);
        }
    }

    /**
     * Show/hide processing UI
     */
    private void showProcessing(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        tvCaptureStatus.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /**
     * Get output directory for captured images
     */
    private File getOutputDirectory() {
        File mediaDir = getExternalMediaDirs().length > 0 ?
                new File(getExternalMediaDirs()[0], "DocumentScanner") : null;

        if (mediaDir != null && !mediaDir.exists()) {
            mediaDir.mkdirs();
        }

        return mediaDir != null && mediaDir.exists() ? mediaDir : getFilesDir();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up camera resources
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }

    @Override
    public void onBackPressed() {
        // Ask user to confirm if documents were captured
        if (capturedImageCount > 0) {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle("Exit Camera")
                    .setMessage("You have captured " + capturedImageCount +
                               " document(s). Exit camera?")
                    .setPositiveButton("Exit", (dialog, which) -> super.onBackPressed())
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }
}

