package com.example.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MultiPageActivity - Combine multiple scanned documents into a single PDF
 * Features:
 * - RecyclerView with grid layout showing thumbnails
 * - Drag and drop to reorder pages
 * - Add/remove pages
 * - Generate multi-page PDF
 * - Preview PDF before saving
 * - Share PDF functionality
 * - Page numbering
 */
public class MultiPageActivity extends AppCompatActivity implements PageAdapter.OnPageActionListener {

    private static final String TAG = "MultiPageActivity";
    private static final int REQUEST_ADD_PAGE = 1001;

    // UI Components
    private RecyclerView rvPages;
    private MaterialButton btnGeneratePdf;
    private MaterialButton btnPreview;
    private MaterialButton btnShare;
    private FloatingActionButton fabAddPage;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private TextView tvPageCount;

    // Data
    private PageAdapter pageAdapter;
    private List<String> imagePaths = new ArrayList<>();
    private ItemTouchHelper itemTouchHelper;
    private String generatedPdfPath;
    private File outputDirectory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_page);

        // Initialize views
        initializeViews();

        // Setup RecyclerView
        setupRecyclerView();

        // Setup drag and drop
        setupDragAndDrop();

        // Setup click listeners
        setupClickListeners();

        // Load initial images if any
        loadInitialImages();

        // Create output directory
        outputDirectory = getOutputDirectory();

        // Update UI
        updatePageCount();
    }

    /**
     * Initialize all UI components
     */
    private void initializeViews() {
        rvPages = findViewById(R.id.rvPages);
        btnGeneratePdf = findViewById(R.id.btnGeneratePdf);
        btnPreview = findViewById(R.id.btnPreview);
        btnShare = findViewById(R.id.btnShare);
        fabAddPage = findViewById(R.id.fabAddPage);
        progressBar = findViewById(R.id.progressBar);
        tvStatus = findViewById(R.id.tvStatus);
        tvPageCount = findViewById(R.id.tvPageCount);

        // Initially hide progress and disable buttons
        progressBar.setVisibility(View.GONE);
        tvStatus.setVisibility(View.GONE);
        btnPreview.setEnabled(false);
        btnShare.setEnabled(false);
    }

    /**
     * Setup RecyclerView with grid layout
     */
    private void setupRecyclerView() {
        // Grid layout with 2 columns
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        rvPages.setLayoutManager(layoutManager);

        // Setup adapter
        pageAdapter = new PageAdapter(this, imagePaths, this);
        rvPages.setAdapter(pageAdapter);
    }

    /**
     * Setup drag and drop functionality
     */
    private void setupDragAndDrop() {
        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN |
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(RecyclerView recyclerView,
                                RecyclerView.ViewHolder viewHolder,
                                RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();

                // Swap items
                Collections.swap(imagePaths, fromPosition, toPosition);
                pageAdapter.notifyItemMoved(fromPosition, toPosition);

                Log.d(TAG, "Page moved from " + fromPosition + " to " + toPosition);
                return true;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                removePage(position);
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return true; // Enable long press to drag
            }
        };

        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(rvPages);
    }

    /**
     * Setup click listeners
     */
    private void setupClickListeners() {
        // Add page button
        fabAddPage.setOnClickListener(v -> addNewPage());

        // Generate PDF button
        btnGeneratePdf.setOnClickListener(v -> generatePdf());

        // Preview PDF button
        btnPreview.setOnClickListener(v -> previewPdf());

        // Share PDF button
        btnShare.setOnClickListener(v -> sharePdf());
    }

    /**
     * Load initial images from intent or storage
     */
    private void loadInitialImages() {
        // Check if images were passed via intent
        ArrayList<String> passedImages = getIntent().getStringArrayListExtra("image_paths");
        if (passedImages != null && !passedImages.isEmpty()) {
            imagePaths.addAll(passedImages);
            pageAdapter.notifyDataSetChanged();
            Log.d(TAG, "Loaded " + imagePaths.size() + " images from intent");
        } else {
            // Load recent images from storage
            loadRecentImages();
        }
    }

    /**
     * Load recent images from DocumentScanner directory
     */
    private void loadRecentImages() {
        new Thread(() -> {
            try {
                File docDir = new File(getOutputDirectory(), "");
                if (docDir.exists() && docDir.isDirectory()) {
                    File[] files = docDir.listFiles((dir, name) ->
                        name.toLowerCase().endsWith(".jpg") ||
                        name.toLowerCase().endsWith(".jpeg"));

                    if (files != null && files.length > 0) {
                        // Sort by last modified (newest first)
                        java.util.Arrays.sort(files, (f1, f2) ->
                            Long.compare(f2.lastModified(), f1.lastModified()));

                        // Take first 10 recent images
                        int count = Math.min(files.length, 10);
                        for (int i = 0; i < count; i++) {
                            imagePaths.add(files[i].getAbsolutePath());
                        }

                        runOnUiThread(() -> {
                            pageAdapter.notifyDataSetChanged();
                            updatePageCount();
                            Log.d(TAG, "Loaded " + count + " recent images");
                        });
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading recent images", e);
            }
        }).start();
    }

    /**
     * Add new page - launch camera or file picker
     */
    private void addNewPage() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Add Page")
                .setMessage("How would you like to add a new page?")
                .setPositiveButton("Take Photo", (dialog, which) -> {
                    // Launch camera activity
                    Intent intent = new Intent(this, CameraActivity.class);
                    startActivityForResult(intent, REQUEST_ADD_PAGE);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Remove page at position
     */
    private void removePage(int position) {
        if (position >= 0 && position < imagePaths.size()) {
            String removedPath = imagePaths.get(position);
            imagePaths.remove(position);
            pageAdapter.notifyItemRemoved(position);
            updatePageCount();

            Log.d(TAG, "Removed page at position " + position);

            // Show undo option
            Toast.makeText(this, "Page removed", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * PageAdapter callback - page clicked
     */
    @Override
    public void onPageClicked(int position) {
        // Show page options (view, edit, remove)
        showPageOptions(position);
    }

    /**
     * PageAdapter callback - page long clicked (start drag)
     */
    @Override
    public void onPageLongClicked(RecyclerView.ViewHolder viewHolder) {
        itemTouchHelper.startDrag(viewHolder);
    }

    /**
     * Show page options dialog
     */
    private void showPageOptions(int position) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Page " + (position + 1))
                .setItems(new String[]{"View Full Size", "Edit", "Remove"},
                    (dialog, which) -> {
                        switch (which) {
                            case 0: // View
                                viewPage(position);
                                break;
                            case 1: // Edit
                                editPage(position);
                                break;
                            case 2: // Remove
                                confirmRemovePage(position);
                                break;
                        }
                    })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * View page full size
     */
    private void viewPage(int position) {
        if (position >= 0 && position < imagePaths.size()) {
            String imagePath = imagePaths.get(position);
            PreviewActivity.start(this, imagePath);
        }
    }

    /**
     * Edit page (crop or enhance)
     */
    private void editPage(int position) {
        if (position >= 0 && position < imagePaths.size()) {
            String imagePath = imagePaths.get(position);
            PreviewActivity.start(this, imagePath);
        }
    }

    /**
     * Confirm remove page
     */
    private void confirmRemovePage(int position) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Remove Page")
                .setMessage("Are you sure you want to remove page " + (position + 1) + "?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    removePage(position);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Generate PDF from all pages
     */
    private void generatePdf() {
        if (imagePaths.isEmpty()) {
            Toast.makeText(this, "Add at least one page", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress(true, "Generating PDF...");

        new Thread(() -> {
            try {
                // Create PDF options
                PdfGenerator.PdfOptions options = new PdfGenerator.PdfOptions()
                        .setPageSize(PdfGenerator.PageSize.A4)
                        .setCompressionLevel(PdfGenerator.CompressionLevel.BEST_COMPRESSION)
                        .setImageQuality(85)
                        .setAddTitlePage(true);

                // Create metadata
                PdfGenerator.PdfMetadata metadata = new PdfGenerator.PdfMetadata()
                        .setTitle("Multi-Page Document")
                        .setAuthor("Document Scanner")
                        .setSubject("Scanned Document - " + imagePaths.size() + " pages");

                options.setMetadata(metadata);

                // Generate PDF
                generatedPdfPath = PdfGenerator.generatePdfFromImages(
                        this,
                        imagePaths,
                        outputDirectory.getAbsolutePath(),
                        options
                );

                if (generatedPdfPath != null) {
                    String fileSize = PdfGenerator.getFileSizeString(generatedPdfPath);

                    runOnUiThread(() -> {
                        showProgress(false, null);
                        btnPreview.setEnabled(true);
                        btnShare.setEnabled(true);

                        new MaterialAlertDialogBuilder(this)
                                .setTitle("PDF Generated")
                                .setMessage("Successfully created PDF with " + imagePaths.size() +
                                           " pages\n\nFile: " + new File(generatedPdfPath).getName() +
                                           "\nSize: " + fileSize)
                                .setPositiveButton("Preview", (d, w) -> previewPdf())
                                .setNeutralButton("Share", (d, w) -> sharePdf())
                                .setNegativeButton("OK", null)
                                .show();

                        Log.d(TAG, "PDF generated: " + generatedPdfPath);
                    });
                } else {
                    runOnUiThread(() -> {
                        showProgress(false, null);
                        Toast.makeText(this, "Failed to generate PDF",
                            Toast.LENGTH_SHORT).show();
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Error generating PDF", e);
                runOnUiThread(() -> {
                    showProgress(false, null);
                    Toast.makeText(this, "Error: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    /**
     * Preview generated PDF
     */
    private void previewPdf() {
        if (generatedPdfPath == null || !new File(generatedPdfPath).exists()) {
            Toast.makeText(this, "Generate PDF first", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri pdfUri = androidx.core.content.FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".provider",
                    new File(generatedPdfPath)
            );
            intent.setDataAndType(pdfUri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "No PDF viewer found", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening PDF", e);
            Toast.makeText(this, "Error opening PDF: " + e.getMessage(),
                Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Share PDF via intent
     */
    private void sharePdf() {
        if (generatedPdfPath == null || !new File(generatedPdfPath).exists()) {
            Toast.makeText(this, "Generate PDF first", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            Uri pdfUri = androidx.core.content.FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".provider",
                    new File(generatedPdfPath)
            );

            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Scanned Document");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Please find the attached scanned document.");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share PDF via"));

            Log.d(TAG, "Sharing PDF: " + generatedPdfPath);
        } catch (Exception e) {
            Log.e(TAG, "Error sharing PDF", e);
            Toast.makeText(this, "Error sharing PDF: " + e.getMessage(),
                Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Update page count display
     */
    private void updatePageCount() {
        int count = imagePaths.size();
        tvPageCount.setText(count + " page" + (count != 1 ? "s" : ""));
        btnGeneratePdf.setEnabled(count > 0);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ADD_PAGE && resultCode == RESULT_OK) {
            // Get newly captured image path
            // TODO: Get path from CameraActivity result
            Toast.makeText(this, "New page added", Toast.LENGTH_SHORT).show();
            updatePageCount();
        }
    }

    /**
     * Static method to start this activity
     */
    public static void start(AppCompatActivity activity, ArrayList<String> imagePaths) {
        Intent intent = new Intent(activity, MultiPageActivity.class);
        if (imagePaths != null) {
            intent.putStringArrayListExtra("image_paths", imagePaths);
        }
        activity.startActivity(intent);
    }

    public static void start(AppCompatActivity activity) {
        start(activity, null);
    }
}

