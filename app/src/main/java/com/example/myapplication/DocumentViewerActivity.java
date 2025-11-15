package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.myapplication.database.Document;
import com.example.myapplication.database.DocumentRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * DocumentViewerActivity - View and interact with saved documents
 * Features:
 * - ViewPager2 for multi-page viewing
 * - Zoom and pan functionality
 * - PDF viewer integration
 * - Share, edit, delete options
 * - Page indicator
 */
public class DocumentViewerActivity extends AppCompatActivity {

    private static final String TAG = "DocumentViewerActivity";
    private static final String EXTRA_DOCUMENT_ID = "document_id";
    private static final String EXTRA_FILE_PATH = "file_path";
    private static final int REQUEST_EDIT = 2001;

    // UI Components
    private ViewPager2 viewPager;
    private TextView tvPageIndicator;
    private View pageIndicatorContainer;

    // Data
    private DocumentRepository repository;
    private Document currentDocument;
    private List<String> imagePaths = new ArrayList<>();
    private DocumentPagerAdapter pagerAdapter;

    // Current page tracking
    private int currentPage = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_viewer);

        // Setup toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Document Viewer");
        }

        // Initialize repository
        repository = DocumentRepository.getInstance(this);

        // Initialize views
        initializeViews();

        // Load document
        loadDocument();
    }

    /**
     * Initialize all UI components
     */
    private void initializeViews() {
        viewPager = findViewById(R.id.viewPager);
        tvPageIndicator = findViewById(R.id.tvPageIndicator);
        pageIndicatorContainer = findViewById(R.id.pageIndicatorContainer);
    }

    /**
     * Load document from intent
     */
    private void loadDocument() {
        Intent intent = getIntent();

        if (intent.hasExtra(EXTRA_DOCUMENT_ID)) {
            // Load from database by ID
            long documentId = intent.getLongExtra(EXTRA_DOCUMENT_ID, -1);
            loadDocumentById(documentId);
        } else if (intent.hasExtra(EXTRA_FILE_PATH)) {
            // Load single file by path
            String filePath = intent.getStringExtra(EXTRA_FILE_PATH);
            loadSingleFile(filePath);
        } else {
            Toast.makeText(this, "No document to display", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Load document by ID from database
     */
    private void loadDocumentById(long documentId) {
        repository.getDocumentById((int) documentId).observe(this, document -> {
            if (document != null) {
                currentDocument = document;
                setupDocument(document);
            } else {
                Toast.makeText(this, "Document not found", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    /**
     * Load single file
     */
    private void loadSingleFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            Toast.makeText(this, "Invalid file path", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        imagePaths.clear();

        // Check if it's a PDF
        if (filePath.toLowerCase().endsWith(".pdf")) {
            // For now, show toast - PDF viewing can be enhanced later
            Toast.makeText(this, "PDF viewing - coming soon", Toast.LENGTH_SHORT).show();
            imagePaths.add(filePath);
        } else {
            // Image file
            imagePaths.add(filePath);
        }

        setupViewPager();
        updatePageIndicator();
    }

    /**
     * Setup document for viewing
     */
    private void setupDocument(Document document) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(document.getName());
        }

        // Load all pages
        imagePaths.clear();

        // If multi-page document, load all pages
        if (document.getPageCount() > 1) {
            // Load multiple pages - assuming they're stored with suffix _page1, _page2, etc.
            String basePath = document.getFilePath();
            String extension = basePath.substring(basePath.lastIndexOf("."));
            String basePathWithoutExt = basePath.substring(0, basePath.lastIndexOf("."));

            for (int i = 1; i <= document.getPageCount(); i++) {
                String pagePath = basePathWithoutExt + "_page" + i + extension;
                File pageFile = new File(pagePath);
                if (pageFile.exists()) {
                    imagePaths.add(pagePath);
                }
            }

            // If no separate pages found, use main file
            if (imagePaths.isEmpty()) {
                imagePaths.add(document.getFilePath());
            }
        } else {
            // Single page document
            imagePaths.add(document.getFilePath());
        }

        setupViewPager();
        updatePageIndicator();
    }

    /**
     * Setup ViewPager2 for page viewing
     */
    private void setupViewPager() {
        pagerAdapter = new DocumentPagerAdapter(this, imagePaths);
        viewPager.setAdapter(pagerAdapter);

        // Page change callback
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                currentPage = position;
                updatePageIndicator();
            }
        });
    }

    /**
     * Update page indicator
     */
    private void updatePageIndicator() {
        if (imagePaths.size() > 1) {
            pageIndicatorContainer.setVisibility(View.VISIBLE);
            tvPageIndicator.setText((currentPage + 1) + " / " + imagePaths.size());
        } else {
            pageIndicatorContainer.setVisibility(View.GONE);
        }
    }

    // ================== Menu ==================

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_document_viewer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_share) {
            shareDocument();
            return true;
        } else if (id == R.id.action_edit) {
            editDocument();
            return true;
        } else if (id == R.id.action_delete) {
            showDeleteConfirmation();
            return true;
        } else if (id == R.id.action_info) {
            showDocumentInfo();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Share document
     */
    private void shareDocument() {
        if (imagePaths.isEmpty()) {
            Toast.makeText(this, "No document to share", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);

            // Get current page file
            String currentFilePath = imagePaths.get(currentPage);
            File file = new File(currentFilePath);

            if (!file.exists()) {
                Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
                return;
            }

            // Use FileProvider for secure file sharing
            Uri fileUri = FileProvider.getUriForFile(
                this,
                getApplicationContext().getPackageName() + ".fileprovider",
                file
            );

            // Determine MIME type
            String mimeType = currentFilePath.toLowerCase().endsWith(".pdf")
                ? "application/pdf"
                : "image/*";

            shareIntent.setType(mimeType);
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            String title = currentDocument != null ? currentDocument.getName() : "Document";
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, title);
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Sharing document: " + title);

            startActivity(Intent.createChooser(shareIntent, "Share Document"));

        } catch (Exception e) {
            Toast.makeText(this, "Error sharing document: " + e.getMessage(),
                Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Edit document
     */
    private void editDocument() {
        if (currentDocument == null) {
            Toast.makeText(this, "Cannot edit this document", Toast.LENGTH_SHORT).show();
            return;
        }

        // Navigate to PreviewActivity for editing
        Intent intent = new Intent(this, PreviewActivity.class);
        intent.putExtra("image_path", imagePaths.get(currentPage));
        intent.putExtra("document_id", currentDocument.getId());
        startActivityForResult(intent, REQUEST_EDIT);
    }

    /**
     * Show delete confirmation dialog
     */
    private void showDeleteConfirmation() {
        if (currentDocument == null) {
            Toast.makeText(this, "Cannot delete this document", Toast.LENGTH_SHORT).show();
            return;
        }

        String documentName = currentDocument.getName();

        new MaterialAlertDialogBuilder(this)
            .setTitle("Delete Document")
            .setMessage("Are you sure you want to delete \"" + documentName + "\"?\n\n" +
                "This action cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> {
                deleteDocument();
            })
            .setNegativeButton("Cancel", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show();
    }

    /**
     * Delete document
     */
    private void deleteDocument() {
        if (currentDocument == null) return;

        repository.delete(currentDocument, success -> {
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(this, "Document deleted", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(this, "Failed to delete document",
                        Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    /**
     * Show document info dialog
     */
    private void showDocumentInfo() {
        if (currentDocument == null) {
            Toast.makeText(this, "No document information available",
                Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder info = new StringBuilder();
        info.append("Name: ").append(currentDocument.getName()).append("\n\n");
        info.append("Pages: ").append(currentDocument.getPageCount()).append("\n\n");
        info.append("Size: ").append(formatFileSize(currentDocument.getFileSize())).append("\n\n");
        info.append("Created: ").append(formatDate(currentDocument.getCreatedDate())).append("\n\n");

        if (currentDocument.getTags() != null && !currentDocument.getTags().isEmpty()) {
            info.append("Tags: ").append(currentDocument.getTags()).append("\n\n");
        }

        info.append("Path: ").append(currentDocument.getFilePath());

        new MaterialAlertDialogBuilder(this)
            .setTitle("Document Information")
            .setMessage(info.toString())
            .setPositiveButton("OK", null)
            .setIcon(android.R.drawable.ic_dialog_info)
            .show();
    }

    /**
     * Format file size
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * Format date
     */
    private String formatDate(long timestamp) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
            "MMM dd, yyyy HH:mm", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(timestamp));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_EDIT && resultCode == RESULT_OK) {
            // Reload document after editing
            if (currentDocument != null) {
                loadDocumentById(currentDocument.getId());
            }
        }
    }

    /**
     * Static method to start this activity with document ID
     */
    public static void startWithDocument(Context context, long documentId) {
        Intent intent = new Intent(context, DocumentViewerActivity.class);
        intent.putExtra(EXTRA_DOCUMENT_ID, documentId);
        context.startActivity(intent);
    }

    /**
     * Static method to start this activity with file path
     */
    public static void startWithFile(Context context, String filePath) {
        Intent intent = new Intent(context, DocumentViewerActivity.class);
        intent.putExtra(EXTRA_FILE_PATH, filePath);
        context.startActivity(intent);
    }

    // ================== DocumentPagerAdapter ==================

    /**
     * Adapter for ViewPager2 - handles page viewing with zoom and pan
     */
    private static class DocumentPagerAdapter extends RecyclerView.Adapter<DocumentPagerAdapter.PageViewHolder> {

        private Context context;
        private List<String> imagePaths;

        public DocumentPagerAdapter(Context context, List<String> imagePaths) {
            this.context = context;
            this.imagePaths = imagePaths;
        }

        @NonNull
        @Override
        public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(
                R.layout.item_document_page, parent, false);
            return new PageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
            String imagePath = imagePaths.get(position);
            holder.bind(imagePath);
        }

        @Override
        public int getItemCount() {
            return imagePaths.size();
        }

        /**
         * ViewHolder with zoom and pan functionality
         */
        static class PageViewHolder extends RecyclerView.ViewHolder {

            private ZoomableImageView imageView;

            public PageViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.imageView);
            }

            public void bind(String imagePath) {
                File file = new File(imagePath);

                if (!file.exists()) {
                    // Show placeholder or error
                    return;
                }

                // Load image
                Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }

}

