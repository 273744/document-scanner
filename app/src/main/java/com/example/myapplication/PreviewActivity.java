package com.example.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.example.myapplication.database.Document;
import com.example.myapplication.database.DocumentRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * PreviewActivity - Image enhancement with filter options
 * Displays cropped image and allows applying various enhancement filters
 */
public class PreviewActivity extends AppCompatActivity implements FilterAdapter.OnFilterSelectedListener {

    private static final String TAG = "PreviewActivity";
    private static final String EXTRA_IMAGE_PATH = "image_path";
    private static final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";

    // UI Components
    private ImageView ivPreview;
    private MaterialButton btnSave;
    private MaterialButton btnReset;
    private MaterialButton btnGeneratePdf;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private View bottomSheet;
    private RecyclerView rvFilters;
    private BottomSheetBehavior<View> bottomSheetBehavior;

    // Data
    private String imagePath;
    private Bitmap originalBitmap;
    private Bitmap currentBitmap;
    private FilterAdapter filterAdapter;
    private File outputDirectory;
    private DocumentRepository repository;

    // Current filter
    private FilterType currentFilter = FilterType.ORIGINAL;

    /**
     * Filter types enum
     */
    public enum FilterType {
        ORIGINAL("Original"),
        AUTO_ENHANCE("Auto Enhance"),
        BLACK_AND_WHITE("Black & White"),
        GRAYSCALE("Grayscale"),
        SHARPEN("Sharpen"),
        BRIGHTNESS("Brightness");

        private final String displayName;

        FilterType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        // Get image path from intent
        imagePath = getIntent().getStringExtra(EXTRA_IMAGE_PATH);
        if (imagePath == null || imagePath.isEmpty()) {
            Toast.makeText(this, "No image to preview", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Check if we're in multi-page mode
        boolean multiPageMode = getIntent().getBooleanExtra("multi_page_mode", false);
        int pageNumber = getIntent().getIntExtra("page_number", 1);

        // Initialize views
        initializeViews();

        // Setup bottom sheet
        setupBottomSheet();

        // Setup filter RecyclerView
        setupFilterRecyclerView();

        // Setup click listeners
        setupClickListeners();

        // Load image
        loadImage();

        // Create output directory
        outputDirectory = getOutputDirectory();

        // Initialize repository
        repository = DocumentRepository.getInstance(this);

        // Show appropriate message for multi-page mode
        if (multiPageMode) {
            Toast.makeText(this, "Adding page " + pageNumber + "...\nApply filter or tap Save",
                Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Initialize all UI components
     */
    private void initializeViews() {
        ivPreview = findViewById(R.id.ivPreview);
        btnSave = findViewById(R.id.btnSave);
        btnReset = findViewById(R.id.btnReset);
        btnGeneratePdf = findViewById(R.id.btnGeneratePdf);
        progressBar = findViewById(R.id.progressBar);
        tvStatus = findViewById(R.id.tvStatus);
        bottomSheet = findViewById(R.id.bottomSheet);
        rvFilters = findViewById(R.id.rvFilters);

        // Initially hide progress
        progressBar.setVisibility(View.GONE);
        tvStatus.setVisibility(View.GONE);
    }

    /**
     * Setup bottom sheet behavior
     */
    private void setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        bottomSheetBehavior.setPeekHeight(300);
        bottomSheetBehavior.setHideable(false);

        // Add callback for state changes
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(View bottomSheet, int newState) {
                // Handle state changes if needed
            }

            @Override
            public void onSlide(View bottomSheet, float slideOffset) {
                // Handle sliding if needed
            }
        });
    }

    /**
     * Setup filter RecyclerView
     */
    private void setupFilterRecyclerView() {
        // Create filter list
        List<FilterType> filters = new ArrayList<>();
        filters.add(FilterType.ORIGINAL);
        filters.add(FilterType.AUTO_ENHANCE);
        filters.add(FilterType.BLACK_AND_WHITE);
        filters.add(FilterType.GRAYSCALE);
        filters.add(FilterType.SHARPEN);
        filters.add(FilterType.BRIGHTNESS);

        // Setup adapter
        filterAdapter = new FilterAdapter(this, filters, null, this);

        // Setup RecyclerView
        rvFilters.setLayoutManager(
            new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );
        rvFilters.setAdapter(filterAdapter);
    }

    /**
     * Setup click listeners
     */
    private void setupClickListeners() {
        // Save button - save enhanced image
        btnSave.setOnClickListener(v -> saveEnhancedImage());

        // Reset button - reset to original
        btnReset.setOnClickListener(v -> resetToOriginal());

        // Generate PDF button - navigate to PDF generation
        btnGeneratePdf.setOnClickListener(v -> generatePdf());
    }

    /**
     * Load image from file
     */
    private void loadImage() {
        showProgress(true, "Loading image...");

        new Thread(() -> {
            try {
                File imageFile = new File(imagePath);
                if (!imageFile.exists()) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Image file not found", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    return;
                }

                // Decode bitmap
                originalBitmap = BitmapFactory.decodeFile(imagePath);
                if (originalBitmap == null) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    return;
                }

                currentBitmap = originalBitmap.copy(originalBitmap.getConfig(), true);

                // Update UI
                runOnUiThread(() -> {
                    ivPreview.setImageBitmap(currentBitmap);
                    showProgress(false, null);

                    // Update filter adapter with original bitmap
                    filterAdapter.setOriginalBitmap(originalBitmap);
                });

                Log.d(TAG, "Image loaded: " + originalBitmap.getWidth() + "x" + originalBitmap.getHeight());

            } catch (Exception e) {
                Log.e(TAG, "Error loading image", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error loading image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        }).start();
    }

    /**
     * Filter selected callback
     */
    @Override
    public void onFilterSelected(FilterType filterType) {
        currentFilter = filterType;
        applyFilter(filterType);
    }

    /**
     * Apply selected filter
     */
    private void applyFilter(FilterType filterType) {
        showProgress(true, "Applying filter...");

        new Thread(() -> {
            try {
                Bitmap filtered;

                switch (filterType) {
                    case AUTO_ENHANCE:
                        // Simulate auto-enhance (increase contrast)
                        filtered = adjustBrightnessContrast(originalBitmap, 0, 30);
                        break;

                    case BLACK_AND_WHITE:
                        // Convert to black and white
                        filtered = convertToBlackAndWhite(originalBitmap);
                        break;

                    case GRAYSCALE:
                        // Convert to grayscale
                        filtered = convertToGrayscale(originalBitmap);
                        break;

                    case SHARPEN:
                        // Simulate sharpening
                        filtered = adjustBrightnessContrast(originalBitmap, 10, 20);
                        break;

                    case BRIGHTNESS:
                        // Increase brightness
                        filtered = adjustBrightnessContrast(originalBitmap, 30, 0);
                        break;

                    case ORIGINAL:
                    default:
                        filtered = originalBitmap.copy(originalBitmap.getConfig(), true);
                        break;
                }

                final Bitmap finalFiltered = filtered;

                runOnUiThread(() -> {
                    currentBitmap = finalFiltered;
                    ivPreview.setImageBitmap(currentBitmap);
                    showProgress(false, null);

                    Toast.makeText(this,
                        "Filter applied: " + filterType.getDisplayName(),
                        Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                Log.e(TAG, "Error applying filter", e);
                runOnUiThread(() -> {
                    showProgress(false, null);
                    Toast.makeText(this, "Error applying filter", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    /**
     * Simple brightness/contrast adjustment
     * Note: This is a basic implementation. For better results, use OpenCV
     */
    private Bitmap adjustBrightnessContrast(Bitmap bitmap, int brightness, int contrast) {
        android.graphics.ColorMatrix cm = new android.graphics.ColorMatrix();

        // Brightness
        cm.set(new float[] {
            1, 0, 0, 0, brightness,
            0, 1, 0, 0, brightness,
            0, 0, 1, 0, brightness,
            0, 0, 0, 1, 0
        });

        // Contrast
        float scale = (contrast + 100f) / 100f;
        float translate = (-.5f * scale + .5f) * 255.f;
        cm.set(new float[] {
            scale, 0, 0, 0, translate,
            0, scale, 0, 0, translate,
            0, 0, scale, 0, translate,
            0, 0, 0, 1, 0
        });

        Bitmap ret = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        android.graphics.Canvas canvas = new android.graphics.Canvas(ret);
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setColorFilter(new android.graphics.ColorMatrixColorFilter(cm));
        canvas.drawBitmap(bitmap, 0, 0, paint);

        return ret;
    }

    /**
     * Convert to grayscale
     */
    private Bitmap convertToGrayscale(Bitmap bitmap) {
        android.graphics.ColorMatrix cm = new android.graphics.ColorMatrix();
        cm.setSaturation(0);

        Bitmap ret = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        android.graphics.Canvas canvas = new android.graphics.Canvas(ret);
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setColorFilter(new android.graphics.ColorMatrixColorFilter(cm));
        canvas.drawBitmap(bitmap, 0, 0, paint);

        return ret;
    }

    /**
     * Convert to black and white (high contrast)
     */
    private Bitmap convertToBlackAndWhite(Bitmap bitmap) {
        // First convert to grayscale
        Bitmap gray = convertToGrayscale(bitmap);

        // Then apply high contrast
        return adjustBrightnessContrast(gray, 0, 80);
    }

    /**
     * Reset to original image
     */
    private void resetToOriginal() {
        currentFilter = FilterType.ORIGINAL;
        currentBitmap = originalBitmap.copy(originalBitmap.getConfig(), true);
        ivPreview.setImageBitmap(currentBitmap);
        filterAdapter.setSelectedFilter(FilterType.ORIGINAL);
        Toast.makeText(this, "Reset to original", Toast.LENGTH_SHORT).show();
    }

    /**
     * Save enhanced image
     */
    private void saveEnhancedImage() {
        showProgress(true, "Saving image...");

        new Thread(() -> {
            try {
                // Generate filename
                String timestamp = new SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                        .format(new Date());
                String prefix = currentFilter == FilterType.ORIGINAL ? "DOC_" : "ENHANCED_";
                File outputFile = new File(outputDirectory, prefix + timestamp + ".jpg");

                // Save bitmap
                FileOutputStream out = new FileOutputStream(outputFile);
                currentBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                out.flush();
                out.close();

                String savedPath = outputFile.getAbsolutePath();
                Log.d(TAG, "Image saved: " + savedPath);

                // Save to database
                Document document = new Document();
                document.setName(outputFile.getName());
                document.setFilePath(savedPath);
                document.setCreatedDate(System.currentTimeMillis());
                document.setFileSize(outputFile.length());
                document.setPageCount(1);
                document.setTags(currentFilter.getDisplayName());

                repository.insert(document, success -> {
                    Log.d(TAG, "Document saved to database: " + success);
                });

                // Always set result with saved path for activities that need it
                Intent resultIntent = new Intent();
                resultIntent.putExtra("saved_image_path", savedPath);
                setResult(RESULT_OK, resultIntent);

                Log.d(TAG, "=== RESULT SET ===");
                Log.d(TAG, "setResult called with RESULT_OK");
                Log.d(TAG, "saved_image_path: " + savedPath);
                Log.d(TAG, "==================");

                runOnUiThread(() -> {
                    showProgress(false, null);

                    // Check if we're in multi-page mode
                    boolean multiPageMode = getIntent().getBooleanExtra("multi_page_mode", false);

                    if (multiPageMode) {
                        // AUTO MODE: Just show quick feedback and return immediately
                        Toast.makeText(this, "✓ Page added!", Toast.LENGTH_SHORT).show();
                        // Automatically return to MultiPageActivity (no dialog!)
                        finish();
                        return;
                    }

                    // NORMAL MODE: Show dialog with options
                    new MaterialAlertDialogBuilder(this)
                        .setTitle("✓ Page Saved!")
                        .setMessage("Page saved successfully!\n\nWhat would you like to do next?")
                        .setPositiveButton("Add More Pages", (dialog, which) -> {
                            // Start multi-page session with this image
                            addToMultiPageAndContinue(savedPath);
                        })
                        .setNeutralButton("Generate PDF", (dialog, which) -> {
                            // Generate PDF from this single page
                            generatePdf();
                        })
                        .setNegativeButton("Done", (dialog, which) -> {
                            finish();
                        })
                        .show();
                });

            } catch (Exception e) {
                Log.e(TAG, "Error saving image", e);
                runOnUiThread(() -> {
                    showProgress(false, null);
                    Toast.makeText(this, "Error saving: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    /**
     * Navigate to PDF generation
     */
    private void generatePdf() {
        showProgress(true, "Generating PDF...");

        new Thread(() -> {
            try {
                // Generate PDF from current image
                String pdfPath = PdfGenerator.generatePdfFromImage(
                    this,
                    imagePath,
                    outputDirectory.getAbsolutePath()
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
                    pdfDocument.setTags("PDF");

                    repository.insert(pdfDocument, success -> {
                        Log.d(TAG, "PDF saved to database: " + success);
                    });

                    runOnUiThread(() -> {
                        showProgress(false, null);

                        new MaterialAlertDialogBuilder(this)
                            .setTitle("PDF Generated")
                            .setMessage("PDF saved successfully!\n\n" + pdfFile.getName() + "\n\nYou can view it in the Gallery.")
                            .setPositiveButton("View Gallery", (dialog, which) -> {
                                // Open gallery
                                Intent intent = new Intent(this, GalleryActivity.class);
                                startActivity(intent);
                                finish();
                            })
                            .setNeutralButton("Share", (dialog, which) -> {
                                // Share PDF
                                ShareManager shareManager = new ShareManager(this);
                                shareManager.sharePDF(pdfFile, ShareManager.ShareTarget.ALL);
                            })
                            .setNegativeButton("Done", (dialog, which) -> {
                                finish();
                            })
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
     * Add current page to multi-page document and continue scanning
     */
    private void addToMultiPageAndContinue(String imagePath) {
        // Start MultiPageActivity with the current image
        Intent intent = new Intent(this, MultiPageActivity.class);
        intent.putExtra("initial_image", imagePath);
        intent.putExtra("continue_scanning", true);
        startActivity(intent);
        finish();
    }

    /**
     * Get output directory
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
        // Recycle bitmaps
        if (originalBitmap != null && !originalBitmap.isRecycled()) {
            originalBitmap.recycle();
        }
        if (currentBitmap != null && !currentBitmap.isRecycled() && currentBitmap != originalBitmap) {
            currentBitmap.recycle();
        }
    }

    /**
     * Static method to start this activity
     */
    public static void start(AppCompatActivity activity, String imagePath) {
        Intent intent = new Intent(activity, PreviewActivity.class);
        intent.putExtra(EXTRA_IMAGE_PATH, imagePath);
        activity.startActivity(intent);
    }
}

