package com.example.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;

import com.example.myapplication.database.Document;
import com.example.myapplication.database.DocumentRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * GalleryActivity - Display all scanned documents in a grid view
 * Features:
 * - Grid layout with thumbnails
 * - Load from Room database
 * - Search functionality
 * - Sort options (date, name, size)
 * - Click to open document
 * - Long press for options
 * - FAB for new scan
 */
public class GalleryActivity extends AppCompatActivity implements DocumentGalleryAdapter.OnDocumentActionListener {

    private static final String TAG = "GalleryActivity";
    private static final int REQUEST_NEW_SCAN = 1001;

    // UI Components
    private RecyclerView rvDocuments;
    private DocumentGalleryAdapter adapter;
    private FloatingActionButton fabNewScan;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private SearchView searchView;

    // Data
    private DocumentRepository repository;
    private LiveData<List<Document>> currentDocuments;
    private List<Document> allDocuments = new ArrayList<>();
    private SortOption currentSort = SortOption.DATE_DESC;

    // Sort options
    public enum SortOption {
        DATE_DESC("Date (Newest First)"),
        DATE_ASC("Date (Oldest First)"),
        NAME_ASC("Name (A-Z)"),
        NAME_DESC("Name (Z-A)"),
        SIZE_ASC("Size (Smallest First)"),
        SIZE_DESC("Size (Largest First)");

        private final String displayName;

        SortOption(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        // Setup toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Document Gallery");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize repository
        repository = DocumentRepository.getInstance(this);

        // Initialize views
        initializeViews();

        // Setup RecyclerView
        setupRecyclerView();

        // Setup FAB
        setupFab();

        // Load documents
        loadDocuments();
    }

    /**
     * Initialize all UI components
     */
    private void initializeViews() {
        rvDocuments = findViewById(R.id.rvDocuments);
        fabNewScan = findViewById(R.id.fabNewScan);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);
    }

    /**
     * Setup RecyclerView with grid layout
     */
    private void setupRecyclerView() {
        // Grid layout with 2 columns
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        rvDocuments.setLayoutManager(layoutManager);

        // Setup adapter
        adapter = new DocumentGalleryAdapter(this, allDocuments, this);
        rvDocuments.setAdapter(adapter);
    }

    /**
     * Setup floating action button
     */
    private void setupFab() {
        fabNewScan.setOnClickListener(v -> openCamera());
    }

    /**
     * Load documents from database
     */
    private void loadDocuments() {
        showLoading(true);

        // Observe all documents
        currentDocuments = repository.getAllDocuments();
        currentDocuments.observe(this, documents -> {
            if (documents != null) {
                allDocuments = new ArrayList<>(documents);
                sortDocuments();
                updateUI();
            }
            showLoading(false);
        });
    }

    /**
     * Sort documents based on current sort option
     */
    private void sortDocuments() {
        if (allDocuments == null || allDocuments.isEmpty()) {
            return;
        }

        switch (currentSort) {
            case DATE_DESC:
                Collections.sort(allDocuments, (d1, d2) ->
                    Long.compare(d2.getCreatedDate(), d1.getCreatedDate()));
                break;

            case DATE_ASC:
                Collections.sort(allDocuments, (d1, d2) ->
                    Long.compare(d1.getCreatedDate(), d2.getCreatedDate()));
                break;

            case NAME_ASC:
                Collections.sort(allDocuments, (d1, d2) ->
                    d1.getName().compareToIgnoreCase(d2.getName()));
                break;

            case NAME_DESC:
                Collections.sort(allDocuments, (d1, d2) ->
                    d2.getName().compareToIgnoreCase(d1.getName()));
                break;

            case SIZE_ASC:
                Collections.sort(allDocuments, (d1, d2) ->
                    Long.compare(d1.getFileSize(), d2.getFileSize()));
                break;

            case SIZE_DESC:
                Collections.sort(allDocuments, (d1, d2) ->
                    Long.compare(d2.getFileSize(), d1.getFileSize()));
                break;
        }
    }

    /**
     * Update UI based on data
     */
    private void updateUI() {
        adapter.setDocuments(allDocuments);

        // Show/hide empty state
        if (allDocuments.isEmpty()) {
            rvDocuments.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);
            tvEmptyState.setText("No documents yet\n\nTap + to scan your first document");
        } else {
            rvDocuments.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.GONE);
        }
    }

    /**
     * Show/hide loading indicator
     */
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /**
     * Open camera for new scan
     */
    private void openCamera() {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivityForResult(intent, REQUEST_NEW_SCAN);
    }

    // ================== DocumentGalleryAdapter Callbacks ==================

    @Override
    public void onDocumentClick(Document document) {
        // Check if it's a PDF or image
        String filePath = document.getFilePath();
        if (filePath.toLowerCase().endsWith(".pdf")) {
            // Open PDF with external viewer
            openPdfDocument(filePath);
        } else {
            // Open image in DocumentViewerActivity
            DocumentViewerActivity.startWithFile(this, filePath);
        }
    }

    @Override
    public void onDocumentLongClick(Document document) {
        showDocumentOptionsDialog(document);
    }

    /**
     * Show document options dialog (rename, delete, etc.)
     */
    private void showDocumentOptionsDialog(Document document) {
        String[] options = {
            "Open",
            "Rename",
            "Add to Favorites",
            "Share",
            "Delete"
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle(document.getName())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Open
                            onDocumentClick(document);
                            break;
                        case 1: // Rename
                            showRenameDialog(document);
                            break;
                        case 2: // Add to Favorites
                            toggleFavorite(document);
                            break;
                        case 3: // Share
                            shareDocument(document);
                            break;
                        case 4: // Delete
                            showDeleteConfirmation(document);
                            break;
                    }
                })
                .show();
    }

    /**
     * Show rename dialog
     */
    private void showRenameDialog(Document document) {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setText(document.getName());
        input.setSelection(document.getName().length());

        new MaterialAlertDialogBuilder(this)
                .setTitle("Rename Document")
                .setView(input)
                .setPositiveButton("Rename", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        renameDocument(document, newName);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Rename document
     */
    private void renameDocument(Document document, String newName) {
        repository.updateDocumentName(document.getId(), newName, success -> {
            if (success) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Renamed to: " + newName,
                        Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Toggle favorite status
     */
    private void toggleFavorite(Document document) {
        boolean newStatus = !document.isFavorite();
        repository.updateFavoriteStatus(document.getId(), newStatus, success -> {
            if (success) {
                runOnUiThread(() -> {
                    String message = newStatus ? "Added to favorites" : "Removed from favorites";
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Share document
     */
    private void shareDocument(Document document) {
        // TODO: Implement sharing functionality
        Toast.makeText(this, "Share: " + document.getName(), Toast.LENGTH_SHORT).show();
    }

    /**
     * Show delete confirmation dialog
     */
    private void showDeleteConfirmation(Document document) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Document")
                .setMessage("Are you sure you want to delete \"" + document.getName() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteDocument(document);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Delete document
     */
    private void deleteDocument(Document document) {
        repository.delete(document, success -> {
            if (success) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Document deleted", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // ================== Menu ==================

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_gallery, menu);

        // Setup search view
        MenuItem searchItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) searchItem.getActionView();
        setupSearchView();

        return true;
    }

    /**
     * Setup search view
     */
    private void setupSearchView() {
        if (searchView == null) return;

        searchView.setQueryHint("Search documents...");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    // Show all documents
                    if (currentDocuments != null) {
                        currentDocuments.removeObservers(GalleryActivity.this);
                    }
                    loadDocuments();
                } else {
                    // Search documents
                    searchDocuments(newText);
                }
                return true;
            }
        });
    }

    /**
     * Search documents
     */
    private void searchDocuments(String query) {
        if (currentDocuments != null) {
            currentDocuments.removeObservers(this);
        }

        currentDocuments = repository.searchDocuments(query);
        currentDocuments.observe(this, documents -> {
            if (documents != null) {
                allDocuments = new ArrayList<>(documents);
                sortDocuments();
                updateUI();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_search) {
            return true;
        } else if (id == R.id.action_sort) {
            showSortDialog();
            return true;
        } else if (id == R.id.action_favorites) {
            showFavorites();
            return true;
        } else if (id == R.id.action_refresh) {
            loadDocuments();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Show sort options dialog
     */
    private void showSortDialog() {
        String[] sortOptions = new String[SortOption.values().length];
        int selectedIndex = 0;

        for (int i = 0; i < SortOption.values().length; i++) {
            sortOptions[i] = SortOption.values()[i].getDisplayName();
            if (SortOption.values()[i] == currentSort) {
                selectedIndex = i;
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Sort By")
                .setSingleChoiceItems(sortOptions, selectedIndex, (dialog, which) -> {
                    currentSort = SortOption.values()[which];
                    sortDocuments();
                    updateUI();
                    dialog.dismiss();
                    Toast.makeText(this, "Sorted by: " + currentSort.getDisplayName(),
                        Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Open PDF document with built-in viewer
     */
    private void openPdfDocument(String filePath) {
        try {
            File pdfFile = new File(filePath);
            if (!pdfFile.exists()) {
                Toast.makeText(this, "PDF file not found", Toast.LENGTH_SHORT).show();
                return;
            }

            // Use built-in PDF viewer
            PdfViewerActivity.start(this, filePath);

        } catch (Exception e) {
            Log.e(TAG, "Error opening PDF", e);
            Toast.makeText(this, "Error opening PDF: " + e.getMessage(),
                Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Show favorite documents only
     */
    private void showFavorites() {
        if (currentDocuments != null) {
            currentDocuments.removeObservers(this);
        }

        currentDocuments = repository.getFavoriteDocuments();
        currentDocuments.observe(this, documents -> {
            if (documents != null) {
                allDocuments = new ArrayList<>(documents);
                sortDocuments();
                updateUI();

                if (allDocuments.isEmpty()) {
                    tvEmptyState.setText("No favorite documents yet");
                }
            }
        });

        Toast.makeText(this, "Showing favorites", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_NEW_SCAN && resultCode == RESULT_OK) {
            // Refresh gallery after new scan
            loadDocuments();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh on resume in case documents were modified
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * Static method to start this activity
     */
    public static void start(AppCompatActivity activity) {
        Intent intent = new Intent(activity, GalleryActivity.class);
        activity.startActivity(intent);
    }
}

