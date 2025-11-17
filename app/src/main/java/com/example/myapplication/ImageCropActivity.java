package com.example.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.example.myapplication.database.Document;
import com.example.myapplication.database.DocumentRepository;
import com.example.myapplication.PdfGenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * ImageCropActivity - Manual document cropping with corner adjustment
 * Allows user to drag corners to adjust crop area and apply perspective correction
 */
public class ImageCropActivity extends AppCompatActivity {

    private static final String TAG = "ImageCropActivity";
    private static final String EXTRA_IMAGE_PATH = "image_path";
    private static final String EXTRA_CROPPED_PATH = "cropped_path";
    private static final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";

    // UI Components
    private CropOverlayView cropOverlayView;
    private MaterialButton btnCrop;
    private MaterialButton btnReset;
    private MaterialButton btnCancel;
    private ProgressBar progressBar;
    private TextView tvStatus;

    // Data
    private String imagePath;
    private Bitmap originalBitmap;
    private File outputDirectory;
    private DocumentRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_crop);

        // Get image path from intent
        imagePath = getIntent().getStringExtra(EXTRA_IMAGE_PATH);
        if (imagePath == null || imagePath.isEmpty()) {
            Toast.makeText(this, "No image to crop", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        initializeViews();

        // Setup click listeners
        setupClickListeners();

        // Load and display image
        loadImage();

        // Create output directory
        outputDirectory = getOutputDirectory();

        // Initialize repository
        repository = DocumentRepository.getInstance(this);
    }

    /**
     * Initialize all UI components
     */
    private void initializeViews() {
        cropOverlayView = findViewById(R.id.cropOverlayView);
        btnCrop = findViewById(R.id.btnCrop);
        btnReset = findViewById(R.id.btnReset);
        btnCancel = findViewById(R.id.btnCancel);
        progressBar = findViewById(R.id.progressBar);
        tvStatus = findViewById(R.id.tvStatus);

        // Initially hide progress
        progressBar.setVisibility(View.GONE);
        tvStatus.setVisibility(View.GONE);
    }

    /**
     * Setup click listeners for all buttons
     */
    private void setupClickListeners() {
        // Crop button - apply crop and save
        btnCrop.setOnClickListener(v -> cropAndSave());

        // Reset button - reset corners to original position
        btnReset.setOnClickListener(v -> resetCorners());

        // Cancel button - exit without saving
        btnCancel.setOnClickListener(v -> confirmCancel());
    }

    /**
     * Load image from file path
     */
    private void loadImage() {
        try {
            showProgress(true, "Loading image...");

            File imageFile = new File(imagePath);
            if (!imageFile.exists()) {
                Toast.makeText(this, "Image file not found", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Decode bitmap
            originalBitmap = BitmapFactory.decodeFile(imagePath);
            if (originalBitmap == null) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Set bitmap to crop view
            cropOverlayView.setImageBitmap(originalBitmap);

            showProgress(false, null);

            Log.d(TAG, "Image loaded: " + originalBitmap.getWidth() + "x" + originalBitmap.getHeight());

        } catch (Exception e) {
            Log.e(TAG, "Error loading image", e);
            Toast.makeText(this, "Error loading image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Reset corners to default position
     */
    private void resetCorners() {
        cropOverlayView.resetCorners();
        Toast.makeText(this, "Corners reset", Toast.LENGTH_SHORT).show();
    }

    /**
     * Confirm cancel action
     */
    private void confirmCancel() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Cancel Cropping")
                .setMessage("Discard changes and exit?")
                .setPositiveButton("Exit", (dialog, which) -> {
                    setResult(RESULT_CANCELED);
                    finish();
                })
                .setNegativeButton("Stay", null)
                .show();
    }

    /**
     * Crop image and save
     */
    private void cropAndSave() {
        // Disable buttons during processing
        setButtonsEnabled(false);
        showProgress(true, "Cropping document...");

        // Process in background thread
        new Thread(() -> {
            try {
                // Get corner points from overlay view
                android.graphics.PointF[] corners = cropOverlayView.getCornerPoints();

                // Use simple crop (OpenCV perspective correction available when OpenCV added)
                cropSimple(corners);

            } catch (Exception e) {
                Log.e(TAG, "Error cropping image", e);
                runOnUiThread(() -> {
                    showProgress(false, null);
                    setButtonsEnabled(true);
                    Toast.makeText(this,
                        "Error cropping: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }


    /**
     * Simple crop without perspective correction
     */
    private void cropSimple(android.graphics.PointF[] corners) {
        try {
            // Calculate bounding rectangle
            float minX = Float.MAX_VALUE;
            float minY = Float.MAX_VALUE;
            float maxX = Float.MIN_VALUE;
            float maxY = Float.MIN_VALUE;

            for (android.graphics.PointF corner : corners) {
                minX = Math.min(minX, corner.x);
                minY = Math.min(minY, corner.y);
                maxX = Math.max(maxX, corner.x);
                maxY = Math.max(maxY, corner.y);
            }

            // Ensure within bounds
            minX = Math.max(0, minX);
            minY = Math.max(0, minY);
            maxX = Math.min(originalBitmap.getWidth(), maxX);
            maxY = Math.min(originalBitmap.getHeight(), maxY);

            int width = (int) (maxX - minX);
            int height = (int) (maxY - minY);

            if (width > 0 && height > 0) {
                // Crop bitmap
                Bitmap croppedBitmap = Bitmap.createBitmap(
                    originalBitmap,
                    (int) minX,
                    (int) minY,
                    width,
                    height
                );

                // Save and finish
                saveCroppedImage(croppedBitmap);
            } else {
                throw new Exception("Invalid crop dimensions");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error with simple crop", e);
            runOnUiThread(() -> {
                showProgress(false, null);
                setButtonsEnabled(true);
                Toast.makeText(this, "Error cropping image", Toast.LENGTH_SHORT).show();
            });
        }
    }

    /**
     * Save cropped image and return result
     */
    private void saveCroppedImage(Bitmap croppedBitmap) {
        try {
            // Generate output filename
            String timestamp = new SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                    .format(new Date());
            File outputFile = new File(outputDirectory, "CROPPED_" + timestamp + ".jpg");

            // Save bitmap
            FileOutputStream out = new FileOutputStream(outputFile);
            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();

            Log.d(TAG, "Cropped image saved: " + outputFile.getAbsolutePath());

            // Save to database
            Document document = new Document();
            document.setName(outputFile.getName());
            document.setFilePath(outputFile.getAbsolutePath());
            document.setCreatedDate(System.currentTimeMillis());
            document.setFileSize(outputFile.length());
            document.setPageCount(1);
            document.setTags("Cropped");

            repository.insert(document, success -> {
                Log.d(TAG, "Cropped document saved to database: " + success);
            });

            // Return result
            runOnUiThread(() -> {
                showProgress(false, null);

                String croppedPath = outputFile.getAbsolutePath();

                // Check if we're in multi-page mode
                boolean multiPageMode = getIntent().getBooleanExtra("multi_page_mode", false);

                if (multiPageMode) {
                    // Multi-page mode: Just return result and close
                    Toast.makeText(this, "✓ Page cropped and added!", Toast.LENGTH_SHORT).show();
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra(EXTRA_CROPPED_PATH, croppedPath);
                    resultIntent.putExtra("saved_image_path", croppedPath);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                } else {
                    // Normal mode: Show options dialog
                    new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                        .setTitle("✓ Image Cropped!")
                        .setMessage("Cropped image saved to gallery.\n\nWould you like to generate a PDF?")
                        .setPositiveButton("Generate PDF", (dialog, which) -> {
                            // Generate PDF from this cropped image
                            generateSinglePagePdf(croppedPath);
                        })
                        .setNeutralButton("Add More Pages", (dialog, which) -> {
                            // Start multi-page mode with this image
                            Intent intent = new Intent(this, MultiPageActivity.class);
                            intent.putExtra("initial_image", croppedPath);
                            intent.putExtra("continue_scanning", true);
                            startActivity(intent);
                            finish();
                        })
                        .setNegativeButton("Done", (dialog, which) -> {
                            // Just close and return
                            finish();
                        })
                        .setCancelable(false)
                        .show();
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error saving cropped image", e);
            runOnUiThread(() -> {
                showProgress(false, null);
                setButtonsEnabled(true);
                Toast.makeText(this, "Error saving: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }

    /**
     * Show/hide progress indicator
     */
    private void showProgress(boolean show, String message) {
        runOnUiThread(() -> {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            tvStatus.setVisibility(show ? View.VISIBLE : View.GONE);
            if (show && message != null) {
                tvStatus.setText(message);
            }
        });
    }

    /**
     * Enable/disable all buttons
     */
    private void setButtonsEnabled(boolean enabled) {
        btnCrop.setEnabled(enabled);
        btnReset.setEnabled(enabled);
        btnCancel.setEnabled(enabled);
    }

    /**
     * Generate PDF from a single cropped image
     */
    private void generateSinglePagePdf(String imagePath) {
        showProgress(true, "Generating PDF...");

        new Thread(() -> {
            try {
                java.util.ArrayList<String> paths = new java.util.ArrayList<>();
                paths.add(imagePath);

                PdfGenerator.PdfOptions options = new PdfGenerator.PdfOptions()
                        .setPageSize(PdfGenerator.PageSize.A4)
                        .setCompressionLevel(PdfGenerator.CompressionLevel.BEST_COMPRESSION)
                        .setImageQuality(85)
                        .setAddTitlePage(false);

                PdfGenerator.PdfMetadata metadata = new PdfGenerator.PdfMetadata()
                        .setTitle("Cropped Document")
                        .setAuthor("Document Scanner");

                options.setMetadata(metadata);

                String pdfPath = PdfGenerator.generatePdfFromImages(
                    this,
                    paths,
                    getOutputDirectory().getAbsolutePath(),
                    options
                );

                if (pdfPath != null) {
                    // Save PDF to database
                    File pdfFile = new File(pdfPath);
                    Document pdfDocument = new Document();
                    pdfDocument.setName(pdfFile.getName());
                    pdfDocument.setFilePath(pdfPath);
                    pdfDocument.setCreatedDate(System.currentTimeMillis());
                    pdfDocument.setFileSize(pdfFile.length());
                    pdfDocument.setPageCount(1);
                    pdfDocument.setTags("PDF,Cropped");

                    repository.insert(pdfDocument, success -> {
                        Log.d(TAG, "PDF saved to database: " + success);
                    });

                    runOnUiThread(() -> {
                        showProgress(false, null);
                        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                            .setTitle("✓ PDF Created!")
                            .setMessage("PDF generated successfully from cropped image!\n\nYou can find it in the Gallery.")
                            .setPositiveButton("View Gallery", (d, w) -> {
                                Intent intent = new Intent(this, GalleryActivity.class);
                                startActivity(intent);
                                finish();
                            })
                            .setNegativeButton("Done", (d, w) -> finish())
                            .show();
                    });
                } else {
                    runOnUiThread(() -> {
                        showProgress(false, null);
                        Toast.makeText(this, "Failed to generate PDF", Toast.LENGTH_SHORT).show();
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Error generating PDF", e);
                runOnUiThread(() -> {
                    showProgress(false, null);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    /**
     * Get output directory for cropped images
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
        // Recycle bitmap to free memory
        if (originalBitmap != null && !originalBitmap.isRecycled()) {
            originalBitmap.recycle();
            originalBitmap = null;
        }
    }

    /**
     * Static method to start this activity
     */
    public static void startForResult(AppCompatActivity activity, String imagePath, int requestCode) {
        Intent intent = new Intent(activity, ImageCropActivity.class);
        intent.putExtra(EXTRA_IMAGE_PATH, imagePath);
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * Get cropped image path from result intent
     */
    public static String getCroppedImagePath(Intent data) {
        if (data != null) {
            return data.getStringExtra(EXTRA_CROPPED_PATH);
        }
        return null;
    }
}

