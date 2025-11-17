package com.example.myapplication;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * OCRResultsActivity - Display and manage extracted OCR text
 *
 * Features:
 * - Display scanned image at top
 * - Show extracted text in editable TextView
 * - Copy to clipboard
 * - Share text via intent
 * - Edit and save corrections
 * - Highlight text regions on image tap
 * - Export as TXT file
 * - Material Design with proper scrolling
 */
public class OCRResultsActivity extends AppCompatActivity {

    private static final String TAG = "OCRResultsActivity";

    // Intent extras
    public static final String EXTRA_IMAGE_PATH = "image_path";
    public static final String EXTRA_OCR_RESULT = "ocr_result";
    public static final String EXTRA_DOCUMENT_ID = "document_id";

    // Views
    private Toolbar toolbar;
    private NestedScrollView scrollView;
    private ImageView imageView;
    private EditText editTextOCR;
    private TextView tvConfidence;
    private TextView tvLanguage;
    private TextView tvWordCount;
    private ProgressBar progressBar;
    private FloatingActionButton fabCopy;
    private FloatingActionButton fabShare;
    private ExtendedFloatingActionButton fabSave;

    // Data
    private String imagePath;
    private Bitmap originalBitmap;
    private Bitmap highlightedBitmap;
    private OCRTextRecognizer.OCRResult ocrResult;
    private List<OCRTextRecognizer.TextBoundingBox> boundingBoxes;
    private boolean textModified = false;
    private boolean highlightMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr_results);

        // Initialize views
        initializeViews();

        // Setup toolbar
        setupToolbar();

        // Load data from intent
        loadDataFromIntent();

        // Setup listeners
        setupListeners();

        // Display OCR results
        displayOCRResults();
    }

    /**
     * Initialize views
     */
    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        scrollView = findViewById(R.id.scrollView);
        imageView = findViewById(R.id.imageView);
        editTextOCR = findViewById(R.id.editTextOCR);
        tvConfidence = findViewById(R.id.tvConfidence);
        tvLanguage = findViewById(R.id.tvLanguage);
        tvWordCount = findViewById(R.id.tvWordCount);
        progressBar = findViewById(R.id.progressBar);
        fabCopy = findViewById(R.id.fabCopy);
        fabShare = findViewById(R.id.fabShare);
        fabSave = findViewById(R.id.fabSave);
    }

    /**
     * Setup toolbar
     */
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("OCR Results");
        }
    }

    /**
     * Load data from intent
     */
    private void loadDataFromIntent() {
        Intent intent = getIntent();

        // Get image path
        imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH);

        // Load image
        if (imagePath != null && !imagePath.isEmpty()) {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                originalBitmap = BitmapFactory.decodeFile(imagePath);
                imageView.setImageBitmap(originalBitmap);
            }
        }

        // Get OCR result (passed as serializable or parcelable)
        // For now, we'll retrieve it from a static holder or database
        // TODO: Implement proper result passing
    }

    /**
     * Setup listeners
     */
    private void setupListeners() {
        // Text change listener
        editTextOCR.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                textModified = true;
                updateWordCount();
                fabSave.show();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Copy FAB
        fabCopy.setOnClickListener(v -> copyToClipboard());

        // Share FAB
        fabShare.setOnClickListener(v -> shareText());

        // Save FAB
        fabSave.setOnClickListener(v -> saveText());

        // Image tap for highlighting
        imageView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN && highlightMode) {
                handleImageTap(event.getX(), event.getY());
                return true;
            }
            return false;
        });

        // Scroll listener for FAB behavior
        scrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener)
            (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (scrollY > oldScrollY) {
                    // Scrolling down - hide FABs
                    fabCopy.hide();
                    fabShare.hide();
                    if (!textModified) {
                        fabSave.hide();
                    }
                } else {
                    // Scrolling up - show FABs
                    fabCopy.show();
                    fabShare.show();
                    if (textModified) {
                        fabSave.show();
                    }
                }
            });
    }

    /**
     * Display OCR results
     */
    private void displayOCRResults() {
        if (ocrResult == null) {
            editTextOCR.setText("No OCR results available");
            return;
        }

        // Display extracted text
        editTextOCR.setText(ocrResult.fullText);

        // Display confidence
        String confidenceText = String.format(Locale.getDefault(),
            "Confidence: %.1f%%", ocrResult.overallConfidence * 100);
        tvConfidence.setText(confidenceText);

        // Display language
        String languageText = "Language: " + ocrResult.language.displayName;
        tvLanguage.setText(languageText);

        // Update word count
        updateWordCount();

        // Create highlighted image if we have bounding boxes
        if (boundingBoxes != null && !boundingBoxes.isEmpty()) {
            createHighlightedImage();
        }
    }

    /**
     * Update word count
     */
    private void updateWordCount() {
        String text = editTextOCR.getText().toString().trim();
        if (text.isEmpty()) {
            tvWordCount.setText("0 words");
            return;
        }

        String[] words = text.split("\\s+");
        int wordCount = words.length;
        int charCount = text.length();

        String countText = String.format(Locale.getDefault(),
            "%d words, %d characters", wordCount, charCount);
        tvWordCount.setText(countText);
    }

    // ================================
    // 3. Copy to Clipboard
    // ================================

    /**
     * Copy text to clipboard
     */
    private void copyToClipboard() {
        String text = editTextOCR.getText().toString();

        if (text.isEmpty()) {
            Toast.makeText(this, "No text to copy", Toast.LENGTH_SHORT).show();
            return;
        }

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("OCR Text", text);
        clipboard.setPrimaryClip(clip);

        Snackbar.make(fabCopy, "Text copied to clipboard", Snackbar.LENGTH_SHORT).show();
    }

    // ================================
    // 4. Share Text via Intent
    // ================================

    /**
     * Share text via intent
     */
    private void shareText() {
        String text = editTextOCR.getText().toString();

        if (text.isEmpty()) {
            Toast.makeText(this, "No text to share", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, text);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "OCR Text from Document Scanner");

        startActivity(Intent.createChooser(shareIntent, "Share text via"));
    }

    // ================================
    // 5. Edit and Save Corrected Text
    // ================================

    /**
     * Save edited text
     */
    private void saveText() {
        String editedText = editTextOCR.getText().toString();

        if (editedText.isEmpty()) {
            Toast.makeText(this, "No text to save", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show save options dialog
        showSaveOptionsDialog(editedText);
    }

    /**
     * Show save options dialog
     */
    private void showSaveOptionsDialog(String text) {
        String[] options = {
            "Update OCR Result",
            "Export as TXT File",
            "Both"
        };

        new AlertDialog.Builder(this)
            .setTitle("Save Options")
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0:
                        updateOCRResult(text);
                        break;
                    case 1:
                        exportAsTxtFile(text);
                        break;
                    case 2:
                        updateOCRResult(text);
                        exportAsTxtFile(text);
                        break;
                }
            })
            .show();
    }

    /**
     * Update OCR result in database
     */
    private void updateOCRResult(String editedText) {
        // TODO: Update database with edited text
        // For now, just update the result object
        if (ocrResult != null) {
            ocrResult.fullText = editedText;
        }

        textModified = false;
        fabSave.hide();

        Snackbar.make(fabSave, "Text updated successfully", Snackbar.LENGTH_SHORT).show();
    }

    // ================================
    // 6. Highlight Text Regions
    // ================================

    /**
     * Handle image tap for highlighting
     */
    private void handleImageTap(float x, float y) {
        if (boundingBoxes == null || boundingBoxes.isEmpty()) {
            Toast.makeText(this, "No text regions available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert tap coordinates to image coordinates
        float[] imageCoords = getImageCoordinates(x, y);
        float imageX = imageCoords[0];
        float imageY = imageCoords[1];

        // Find tapped text region
        for (OCRTextRecognizer.TextBoundingBox box : boundingBoxes) {
            if (box.rect.contains((int) imageX, (int) imageY)) {
                highlightTextRegion(box);
                return;
            }
        }

        Toast.makeText(this, "No text found at tap location", Toast.LENGTH_SHORT).show();
    }

    /**
     * Convert screen coordinates to image coordinates
     */
    private float[] getImageCoordinates(float screenX, float screenY) {
        if (originalBitmap == null) {
            return new float[]{0, 0};
        }

        // Get image view dimensions
        int viewWidth = imageView.getWidth();
        int viewHeight = imageView.getHeight();

        // Get bitmap dimensions
        int bitmapWidth = originalBitmap.getWidth();
        int bitmapHeight = originalBitmap.getHeight();

        // Calculate scale
        float scaleX = (float) bitmapWidth / viewWidth;
        float scaleY = (float) bitmapHeight / viewHeight;

        // Convert coordinates
        float imageX = screenX * scaleX;
        float imageY = screenY * scaleY;

        return new float[]{imageX, imageY};
    }

    /**
     * Highlight specific text region
     */
    private void highlightTextRegion(OCRTextRecognizer.TextBoundingBox box) {
        if (originalBitmap == null) return;

        // Create highlighted bitmap
        Bitmap bitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        paint.setColor(Color.YELLOW);

        // Draw highlight
        canvas.drawRect(box.rect, paint);

        // Show highlighted image
        imageView.setImageBitmap(bitmap);
        highlightedBitmap = bitmap;

        // Show text in snackbar
        Snackbar.make(imageView, "\"" + box.text + "\"", Snackbar.LENGTH_LONG)
            .setAction("Clear", v -> imageView.setImageBitmap(originalBitmap))
            .show();
    }

    /**
     * Create image with all text regions highlighted
     */
    private void createHighlightedImage() {
        if (originalBitmap == null || boundingBoxes == null) return;

        Bitmap bitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);

        // Draw all bounding boxes
        for (OCRTextRecognizer.TextBoundingBox box : boundingBoxes) {
            // Color based on confidence
            if (box.confidence > 0.8f) {
                paint.setColor(Color.GREEN);
            } else if (box.confidence > 0.5f) {
                paint.setColor(Color.YELLOW);
            } else {
                paint.setColor(Color.RED);
            }

            canvas.drawRect(box.rect, paint);
        }

        highlightedBitmap = bitmap;
    }

    /**
     * Toggle highlight mode
     */
    private void toggleHighlightMode() {
        highlightMode = !highlightMode;

        if (highlightMode) {
            if (highlightedBitmap != null) {
                imageView.setImageBitmap(highlightedBitmap);
            }
            Toast.makeText(this, "Tap on text to highlight", Toast.LENGTH_SHORT).show();
        } else {
            imageView.setImageBitmap(originalBitmap);
        }
    }

    // ================================
    // 7. Export as TXT File
    // ================================

    /**
     * Export text as TXT file
     */
    private void exportAsTxtFile(String text) {
        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                // Create file name with timestamp
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                    Locale.getDefault()).format(new Date());
                String fileName = "OCR_" + timestamp + ".txt";

                // Get documents directory
                File documentsDir = new File(getFilesDir(), "ocr_exports");
                if (!documentsDir.exists()) {
                    documentsDir.mkdirs();
                }

                File txtFile = new File(documentsDir, fileName);

                // Write text to file
                FileWriter writer = new FileWriter(txtFile);
                writer.write(text);
                writer.close();

                // Show success on UI thread
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    showExportSuccessDialog(txtFile);
                });

            } catch (IOException e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Export failed: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    /**
     * Show export success dialog
     */
    private void showExportSuccessDialog(File file) {
        String message = "Text exported to:\n" + file.getName();

        new AlertDialog.Builder(this)
            .setTitle("Export Successful")
            .setMessage(message)
            .setPositiveButton("Share File", (dialog, which) -> shareFile(file))
            .setNegativeButton("OK", null)
            .show();
    }

    /**
     * Share exported file
     */
    private void shareFile(File file) {
        Uri fileUri = FileProvider.getUriForFile(this,
            getPackageName() + ".fileprovider", file);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(shareIntent, "Share text file"));
    }

    // ================================
    // Options Menu
    // ================================

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_ocr_results, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_highlight) {
            toggleHighlightMode();
            return true;
        } else if (id == R.id.action_export_txt) {
            exportAsTxtFile(editTextOCR.getText().toString());
            return true;
        } else if (id == R.id.action_copy_all) {
            copyToClipboard();
            return true;
        } else if (id == R.id.action_select_language) {
            showLanguageSelector();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Show language selector dialog
     */
    private void showLanguageSelector() {
        String[] languages = {
            "Latin (English)",
            "Devanagari (Hindi)",
            "Chinese",
            "Japanese",
            "Korean",
            "Auto-detect"
        };

        new AlertDialog.Builder(this)
            .setTitle("Reprocess with Language")
            .setItems(languages, (dialog, which) -> {
                // TODO: Reprocess OCR with selected language
                Toast.makeText(this, "Selected: " + languages[which],
                    Toast.LENGTH_SHORT).show();
            })
            .show();
    }

    @Override
    public void onBackPressed() {
        if (textModified) {
            new AlertDialog.Builder(this)
                .setTitle("Unsaved Changes")
                .setMessage("You have unsaved changes. Save before leaving?")
                .setPositiveButton("Save", (dialog, which) -> {
                    saveText();
                    finish();
                })
                .setNegativeButton("Discard", (dialog, which) -> finish())
                .setNeutralButton("Cancel", null)
                .show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Recycle bitmaps
        if (originalBitmap != null && !originalBitmap.isRecycled()) {
            originalBitmap.recycle();
        }
        if (highlightedBitmap != null && !highlightedBitmap.isRecycled()) {
            highlightedBitmap.recycle();
        }
    }

    // ================================
    // Helper Methods
    // ================================

    /**
     * Set OCR result (call from outside)
     */
    public void setOCRResult(OCRTextRecognizer.OCRResult result) {
        this.ocrResult = result;
        displayOCRResults();
    }

    /**
     * Set bounding boxes (call from outside)
     */
    public void setBoundingBoxes(List<OCRTextRecognizer.TextBoundingBox> boxes) {
        this.boundingBoxes = boxes;
        if (boxes != null && !boxes.isEmpty()) {
            createHighlightedImage();
        }
    }

    /**
     * Static helper to start this activity
     */
    public static void startActivity(Context context, String imagePath,
                                     OCRTextRecognizer.OCRResult result,
                                     List<OCRTextRecognizer.TextBoundingBox> boundingBoxes) {
        Intent intent = new Intent(context, OCRResultsActivity.class);
        intent.putExtra(EXTRA_IMAGE_PATH, imagePath);

        // Store result and boxes in a static holder or database
        // For production, use proper serialization or database
        ResultsHolder.setResult(result);
        ResultsHolder.setBoundingBoxes(boundingBoxes);

        context.startActivity(intent);
    }

    /**
     * Temporary holder for OCR results
     * In production, use database or proper serialization
     */
    private static class ResultsHolder {
        private static OCRTextRecognizer.OCRResult result;
        private static List<OCRTextRecognizer.TextBoundingBox> boundingBoxes;

        static void setResult(OCRTextRecognizer.OCRResult r) {
            result = r;
        }

        static OCRTextRecognizer.OCRResult getResult() {
            return result;
        }

        static void setBoundingBoxes(List<OCRTextRecognizer.TextBoundingBox> boxes) {
            boundingBoxes = boxes;
        }

        static List<OCRTextRecognizer.TextBoundingBox> getBoundingBoxes() {
            return boundingBoxes;
        }

        static void clear() {
            result = null;
            boundingBoxes = null;
        }
    }
}

