package com.example.myapplication;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.DragEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.database.Document;
import com.example.myapplication.database.DocumentRepository;
import com.example.myapplication.database.Folder;
import com.example.myapplication.database.FolderRepository;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * DocumentOrganizerActivity - Advanced folder and document management
 *
 * Features:
 * 1. TreeView for hierarchical folder display
 * 2. Drag and drop document organization
 * 3. Multi-select documents for bulk operations
 * 4. Create/rename/delete folder operations
 * 5. Document move and copy functionality
 * 6. Folder color coding and icons
 * 7. Quick sort options (date, name, type)
 * 8. Material Design with smooth animations
 */
public class DocumentOrganizerActivity extends AppCompatActivity {

    private static final String TAG = "DocumentOrganizer";

    // UI Components
    private RecyclerView rvFolderTree;
    private RecyclerView rvDocuments;
    private FolderTreeAdapter folderAdapter;
    private DocumentOrganizerAdapter documentAdapter;
    private ExtendedFloatingActionButton fabNewFolder;
    private FloatingActionButton fabMultiSelect;
    private FloatingActionButton fabSort;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private TextView tvCurrentPath;
    private ChipGroup chipSelectedCount;
    private Chip chipSelectionCount;

    // Data
    private FolderRepository folderRepository;
    private DocumentRepository documentRepository;
    private List<FolderTreeNode> folderTree = new ArrayList<>();
    private List<Document> currentDocuments = new ArrayList<>();
    private Folder currentFolder = null;
    private Long currentFolderId = null;

    // Multi-select
    private boolean isMultiSelectMode = false;
    private Set<Long> selectedDocumentIds = new HashSet<>();

    // Drag and drop
    private Document draggingDocument = null;

    // Sort options
    private SortOption currentSort = SortOption.DATE_DESC;

    // Colors for folders
    private static final String[] FOLDER_COLORS = {
        "#2196F3", // Blue
        "#4CAF50", // Green
        "#FF9800", // Orange
        "#E91E63", // Pink
        "#9C27B0", // Purple
        "#FF5722", // Deep Orange
        "#00BCD4", // Cyan
        "#FFC107"  // Amber
    };

    // Icons for folders
    private static final int[] FOLDER_ICONS = {
        android.R.drawable.ic_menu_save,
        android.R.drawable.ic_menu_gallery,
        android.R.drawable.ic_menu_info_details,
        android.R.drawable.ic_menu_my_calendar
    };

    /**
     * Sort options
     */
    public enum SortOption {
        DATE_DESC("Date (Newest First)"),
        DATE_ASC("Date (Oldest First)"),
        NAME_ASC("Name (A-Z)"),
        NAME_DESC("Name (Z-A)"),
        TYPE_ASC("Type (A-Z)"),
        TYPE_DESC("Type (Z-A)");

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
        setContentView(R.layout.activity_document_organizer);

        // Setup toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Document Organizer");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize repositories
        folderRepository = new FolderRepository(this);
        documentRepository = DocumentRepository.getInstance(this);

        // Initialize views
        initializeViews();

        // Setup RecyclerViews
        setupFolderTreeView();
        setupDocumentsView();

        // Setup FABs
        setupFabs();

        // Load data
        loadFolderTree();
        loadRootDocuments();
    }

    /**
     * Initialize views
     */
    private void initializeViews() {
        rvFolderTree = findViewById(R.id.rvFolderTree);
        rvDocuments = findViewById(R.id.rvDocuments);
        fabNewFolder = findViewById(R.id.fabNewFolder);
        fabMultiSelect = findViewById(R.id.fabMultiSelect);
        fabSort = findViewById(R.id.fabSort);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvCurrentPath = findViewById(R.id.tvCurrentPath);
        chipSelectedCount = findViewById(R.id.chipSelectedCount);
        chipSelectionCount = findViewById(R.id.chipSelectionCount);
    }

    /**
     * Setup folder tree RecyclerView
     */
    private void setupFolderTreeView() {
        folderAdapter = new FolderTreeAdapter();
        rvFolderTree.setLayoutManager(new LinearLayoutManager(this));
        rvFolderTree.setAdapter(folderAdapter);

        // Enable drag and drop
        setupFolderDragAndDrop();
    }

    /**
     * Setup documents RecyclerView
     */
    private void setupDocumentsView() {
        documentAdapter = new DocumentOrganizerAdapter();
        rvDocuments.setLayoutManager(new LinearLayoutManager(this));
        rvDocuments.setAdapter(documentAdapter);

        // Enable drag and drop
        setupDocumentDragAndDrop();
    }

    /**
     * Setup FABs
     */
    private void setupFabs() {
        fabNewFolder.setOnClickListener(v -> showCreateFolderDialog());

        fabMultiSelect.setOnClickListener(v -> toggleMultiSelectMode());

        fabSort.setOnClickListener(v -> showSortOptionsDialog());
    }

    // ================================
    // Folder Tree Management
    // ================================

    /**
     * Load folder tree
     */
    private void loadFolderTree() {
        showProgress(true);

        folderRepository.getAllFolders(new FolderRepository.FolderListCallback() {
            @Override
            public void onSuccess(List<Folder> folders) {
                runOnUiThread(() -> {
                    buildFolderTree(folders);
                    folderAdapter.updateData(folderTree);
                    showProgress(false);
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(DocumentOrganizerActivity.this,
                        "Error loading folders: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                    showProgress(false);
                });
            }
        });
    }

    /**
     * Build folder tree structure
     */
    private void buildFolderTree(List<Folder> folders) {
        folderTree.clear();

        // Create map for quick lookup
        Map<Long, FolderTreeNode> nodeMap = new HashMap<>();

        // Create nodes for all folders
        for (Folder folder : folders) {
            FolderTreeNode node = new FolderTreeNode(folder);
            nodeMap.put(folder.getFolderId(), node);
        }

        // Build parent-child relationships
        for (Folder folder : folders) {
            FolderTreeNode node = nodeMap.get(folder.getFolderId());

            if (folder.getParentFolderId() != null) {
                FolderTreeNode parent = nodeMap.get(folder.getParentFolderId());
                if (parent != null) {
                    parent.addChild(node);
                    node.setParent(parent);
                }
            } else {
                // Root folder
                folderTree.add(node);
            }
        }
    }

    /**
     * Load documents in folder
     */
    private void loadDocumentsInFolder(Long folderId) {
        showProgress(true);
        currentFolderId = folderId;

        if (folderId == null) {
            loadRootDocuments();
        } else {
            folderRepository.getFolderById(folderId, new FolderRepository.FolderCallback() {
                @Override
                public void onSuccess(Folder folder) {
                    currentFolder = folder;
                    runOnUiThread(() -> updateCurrentPath(folder));

                    // Load documents
                    documentRepository.getDocumentByIdSync(folderId, documents -> {
                        runOnUiThread(() -> {
                            currentDocuments.clear();
                            if (documents != null) {
                                currentDocuments.addAll((List<Document>) documents);
                            }
                            sortDocuments();
                            documentAdapter.updateData(currentDocuments);
                            updateEmptyState();
                            showProgress(false);
                        });
                    });
                }

                @Override
                public void onError(Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(DocumentOrganizerActivity.this,
                            "Error loading folder: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                        showProgress(false);
                    });
                }
            });
        }
    }

    /**
     * Load root documents
     */
    private void loadRootDocuments() {
        currentFolder = null;
        currentFolderId = null;
        updateCurrentPath(null);

        // TODO: Load root documents from repository
        currentDocuments.clear();
        sortDocuments();
        documentAdapter.updateData(currentDocuments);
        updateEmptyState();
        showProgress(false);
    }

    /**
     * Update current path display
     */
    private void updateCurrentPath(Folder folder) {
        if (folder == null) {
            tvCurrentPath.setText("/ Root");
        } else {
            String path = folder.getFolderPath() != null ? folder.getFolderPath() : "/" + folder.getFolderName();
            tvCurrentPath.setText(path);
        }
    }

    // ================================
    // Folder Operations
    // ================================

    /**
     * Show create folder dialog
     */
    private void showCreateFolderDialog() {
        EditText input = new EditText(this);
        input.setHint("Folder name");

        new MaterialAlertDialogBuilder(this)
            .setTitle("Create New Folder")
            .setView(input)
            .setPositiveButton("Create", (dialog, which) -> {
                String folderName = input.getText().toString().trim();
                if (!folderName.isEmpty()) {
                    createFolder(folderName);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    /**
     * Create new folder
     */
    private void createFolder(String folderName) {
        showProgress(true);

        folderRepository.createFolder(folderName, currentFolderId,
            new FolderRepository.FolderCallback() {
                @Override
                public void onSuccess(Folder folder) {
                    runOnUiThread(() -> {
                        Toast.makeText(DocumentOrganizerActivity.this,
                            "Folder created: " + folderName,
                            Toast.LENGTH_SHORT).show();
                        loadFolderTree();
                    });
                }

                @Override
                public void onError(Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(DocumentOrganizerActivity.this,
                            "Error creating folder: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                        showProgress(false);
                    });
                }
            });
    }

    /**
     * Show rename folder dialog
     */
    private void showRenameFolderDialog(Folder folder) {
        EditText input = new EditText(this);
        input.setText(folder.getFolderName());
        input.setSelection(folder.getFolderName().length());

        new MaterialAlertDialogBuilder(this)
            .setTitle("Rename Folder")
            .setView(input)
            .setPositiveButton("Rename", (dialog, which) -> {
                String newName = input.getText().toString().trim();
                if (!newName.isEmpty()) {
                    renameFolder(folder, newName);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    /**
     * Rename folder
     */
    private void renameFolder(Folder folder, String newName) {
        showProgress(true);

        folder.setFolderName(newName);
        folder.setModifiedAt(System.currentTimeMillis());

        folderRepository.updateFolder(folder, new FolderRepository.OperationCallback() {
            @Override
            public void onComplete(boolean success) {
                runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(DocumentOrganizerActivity.this,
                            "Folder renamed",
                            Toast.LENGTH_SHORT).show();
                        loadFolderTree();
                    } else {
                        Toast.makeText(DocumentOrganizerActivity.this,
                            "Error renaming folder",
                            Toast.LENGTH_SHORT).show();
                        showProgress(false);
                    }
                });
            }
        });
    }

    /**
     * Show delete folder confirmation
     */
    private void showDeleteFolderDialog(Folder folder) {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Delete Folder")
            .setMessage("Are you sure you want to delete \"" + folder.getFolderName() + "\"?\n\n" +
                "Documents in this folder will be moved to root.")
            .setPositiveButton("Delete", (dialog, which) -> {
                deleteFolder(folder);
            })
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Delete with Contents", (dialog, which) -> {
                deleteFolderWithContents(folder);
            })
            .show();
    }

    /**
     * Delete folder
     */
    private void deleteFolder(Folder folder) {
        showProgress(true);

        folderRepository.deleteFolder(folder.getFolderId(),
            new FolderRepository.OperationCallback() {
                @Override
                public void onComplete(boolean success) {
                    runOnUiThread(() -> {
                        if (success) {
                            Toast.makeText(DocumentOrganizerActivity.this,
                                "Folder deleted",
                                Toast.LENGTH_SHORT).show();
                            loadFolderTree();
                            if (currentFolderId != null && currentFolderId.equals(folder.getFolderId())) {
                                loadRootDocuments();
                            }
                        } else {
                            Toast.makeText(DocumentOrganizerActivity.this,
                                "Error deleting folder. It may contain subfolders.",
                                Toast.LENGTH_LONG).show();
                            showProgress(false);
                        }
                    });
                }
            });
    }

    /**
     * Delete folder with all contents
     */
    private void deleteFolderWithContents(Folder folder) {
        showProgress(true);

        folderRepository.deleteFolderRecursively(folder.getFolderId(),
            new FolderRepository.OperationCallback() {
                @Override
                public void onComplete(boolean success) {
                    runOnUiThread(() -> {
                        if (success) {
                            Toast.makeText(DocumentOrganizerActivity.this,
                                "Folder and contents deleted",
                                Toast.LENGTH_SHORT).show();
                            loadFolderTree();
                            if (currentFolderId != null && currentFolderId.equals(folder.getFolderId())) {
                                loadRootDocuments();
                            }
                        } else {
                            Toast.makeText(DocumentOrganizerActivity.this,
                                "Error deleting folder",
                                Toast.LENGTH_SHORT).show();
                            showProgress(false);
                        }
                    });
                }
            });
    }

    /**
     * Show folder color picker
     */
    private void showFolderColorPicker(Folder folder) {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_color_picker, null);

        LinearLayout colorContainer = view.findViewById(R.id.colorContainer);

        for (String color : FOLDER_COLORS) {
            View colorView = new View(this);
            colorView.setLayoutParams(new LinearLayout.LayoutParams(120, 120));
            colorView.setBackgroundColor(Color.parseColor(color));
            colorView.setOnClickListener(v -> {
                setFolderColor(folder, color);
                bottomSheet.dismiss();
            });
            colorContainer.addView(colorView);
        }

        bottomSheet.setContentView(view);
        bottomSheet.show();
    }

    /**
     * Set folder color
     */
    private void setFolderColor(Folder folder, String color) {
        folder.setColor(color);
        folder.setModifiedAt(System.currentTimeMillis());

        folderRepository.updateFolder(folder, success -> {
            if (success) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Color updated", Toast.LENGTH_SHORT).show();
                    loadFolderTree();
                });
            }
        });
    }

    // ================================
    // Document Operations
    // ================================

    /**
     * Move document to folder
     */
    private void moveDocumentToFolder(Document document, Long targetFolderId) {
        showProgress(true);

        folderRepository.moveDocumentToFolder(document.getDocumentId(), targetFolderId,
            new FolderRepository.OperationCallback() {
                @Override
                public void onComplete(boolean success) {
                    runOnUiThread(() -> {
                        if (success) {
                            Toast.makeText(DocumentOrganizerActivity.this,
                                "Document moved",
                                Toast.LENGTH_SHORT).show();
                            loadDocumentsInFolder(currentFolderId);
                        } else {
                            Toast.makeText(DocumentOrganizerActivity.this,
                                "Error moving document",
                                Toast.LENGTH_SHORT).show();
                            showProgress(false);
                        }
                    });
                }
            });
    }

    /**
     * Copy document to folder
     */
    private void copyDocumentToFolder(Document document, Long targetFolderId) {
        // TODO: Implement document copy functionality
        Toast.makeText(this, "Copy feature coming soon", Toast.LENGTH_SHORT).show();
    }

    /**
     * Move multiple documents
     */
    private void moveBulkDocuments(Set<Long> documentIds, Long targetFolderId) {
        showProgress(true);

        List<Long> idList = new ArrayList<>(documentIds);

        folderRepository.moveDocumentsToFolder(idList, targetFolderId,
            new FolderRepository.BatchOperationCallback() {
                @Override
                public void onComplete(int successCount, int failureCount) {
                    runOnUiThread(() -> {
                        String message = successCount + " document(s) moved";
                        if (failureCount > 0) {
                            message += ", " + failureCount + " failed";
                        }
                        Toast.makeText(DocumentOrganizerActivity.this,
                            message, Toast.LENGTH_SHORT).show();

                        clearSelection();
                        loadDocumentsInFolder(currentFolderId);
                    });
                }
            });
    }

    // ================================
    // Multi-Select Mode
    // ================================

    /**
     * Toggle multi-select mode
     */
    private void toggleMultiSelectMode() {
        isMultiSelectMode = !isMultiSelectMode;

        if (isMultiSelectMode) {
            fabMultiSelect.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            chipSelectedCount.setVisibility(View.VISIBLE);
            animateChipAppearance(chipSelectedCount);
        } else {
            fabMultiSelect.setImageResource(android.R.drawable.ic_menu_edit);
            clearSelection();
            chipSelectedCount.setVisibility(View.GONE);
        }

        documentAdapter.setMultiSelectMode(isMultiSelectMode);
        updateSelectionCount();
    }

    /**
     * Toggle document selection
     */
    private void toggleDocumentSelection(long documentId) {
        if (selectedDocumentIds.contains(documentId)) {
            selectedDocumentIds.remove(documentId);
        } else {
            selectedDocumentIds.add(documentId);
        }
        updateSelectionCount();
        documentAdapter.notifyDataSetChanged();
    }

    /**
     * Clear selection
     */
    private void clearSelection() {
        selectedDocumentIds.clear();
        isMultiSelectMode = false;
        updateSelectionCount();
        documentAdapter.setMultiSelectMode(false);
    }

    /**
     * Update selection count display
     */
    private void updateSelectionCount() {
        if (isMultiSelectMode && !selectedDocumentIds.isEmpty()) {
            chipSelectionCount.setText(selectedDocumentIds.size() + " selected");
            chipSelectedCount.setVisibility(View.VISIBLE);
        } else {
            chipSelectedCount.setVisibility(View.GONE);
        }
    }

    /**
     * Show bulk operations menu
     */
    private void showBulkOperationsMenu() {
        if (selectedDocumentIds.isEmpty()) {
            Toast.makeText(this, "No documents selected", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] options = {"Move to Folder", "Copy to Folder", "Delete Selected"};

        new MaterialAlertDialogBuilder(this)
            .setTitle("Bulk Operations")
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: // Move
                        showFolderPickerForMove();
                        break;
                    case 1: // Copy
                        showFolderPickerForCopy();
                        break;
                    case 2: // Delete
                        showBulkDeleteConfirmation();
                        break;
                }
            })
            .show();
    }

    /**
     * Show folder picker for move
     */
    private void showFolderPickerForMove() {
        // TODO: Implement folder picker dialog
        Toast.makeText(this, "Folder picker coming soon", Toast.LENGTH_SHORT).show();
    }

    /**
     * Show folder picker for copy
     */
    private void showFolderPickerForCopy() {
        // TODO: Implement folder picker dialog
        Toast.makeText(this, "Folder picker coming soon", Toast.LENGTH_SHORT).show();
    }

    /**
     * Show bulk delete confirmation
     */
    private void showBulkDeleteConfirmation() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Delete Documents")
            .setMessage("Delete " + selectedDocumentIds.size() + " selected document(s)?")
            .setPositiveButton("Delete", (dialog, which) -> {
                // TODO: Implement bulk delete
                Toast.makeText(this, "Bulk delete coming soon", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    // ================================
    // Drag and Drop
    // ================================

    /**
     * Setup folder drag and drop
     */
    private void setupFolderDragAndDrop() {
        rvFolderTree.setOnDragListener((v, event) -> {
            switch (event.getAction()) {
                case DragEvent.ACTION_DROP:
                    if (draggingDocument != null) {
                        // Get the folder where document was dropped
                        View droppedOnView = rvFolderTree.findChildViewUnder(
                            event.getX(), event.getY());

                        if (droppedOnView != null) {
                            int position = rvFolderTree.getChildAdapterPosition(droppedOnView);
                            if (position >= 0 && position < folderTree.size()) {
                                FolderTreeNode node = folderTree.get(position);
                                moveDocumentToFolder(draggingDocument, node.folder.getFolderId());
                            }
                        }
                        draggingDocument = null;
                    }
                    return true;

                case DragEvent.ACTION_DRAG_ENDED:
                    draggingDocument = null;
                    return true;
            }
            return true;
        });
    }

    /**
     * Setup document drag and drop
     */
    private void setupDocumentDragAndDrop() {
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView,
                                       @NonNull RecyclerView.ViewHolder viewHolder) {
                return makeMovementFlags(0, ItemTouchHelper.START | ItemTouchHelper.END);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                 @NonNull RecyclerView.ViewHolder viewHolder,
                                 @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // Handle swipe if needed
            }
        });

        itemTouchHelper.attachToRecyclerView(rvDocuments);
    }

    // ================================
    // Sorting
    // ================================

    /**
     * Show sort options dialog
     */
    private void showSortOptionsDialog() {
        String[] options = new String[SortOption.values().length];
        for (int i = 0; i < SortOption.values().length; i++) {
            options[i] = SortOption.values()[i].getDisplayName();
        }

        new MaterialAlertDialogBuilder(this)
            .setTitle("Sort Documents")
            .setSingleChoiceItems(options, currentSort.ordinal(), (dialog, which) -> {
                currentSort = SortOption.values()[which];
                sortDocuments();
                documentAdapter.updateData(currentDocuments);
                dialog.dismiss();
            })
            .show();
    }

    /**
     * Sort documents based on current sort option
     */
    private void sortDocuments() {
        switch (currentSort) {
            case DATE_DESC:
                Collections.sort(currentDocuments, (d1, d2) ->
                    Long.compare(d2.getCreatedAt(), d1.getCreatedAt()));
                break;

            case DATE_ASC:
                Collections.sort(currentDocuments, (d1, d2) ->
                    Long.compare(d1.getCreatedAt(), d2.getCreatedAt()));
                break;

            case NAME_ASC:
                Collections.sort(currentDocuments, (d1, d2) ->
                    d1.getDocumentName().compareToIgnoreCase(d2.getDocumentName()));
                break;

            case NAME_DESC:
                Collections.sort(currentDocuments, (d1, d2) ->
                    d2.getDocumentName().compareToIgnoreCase(d1.getDocumentName()));
                break;

            case TYPE_ASC:
                Collections.sort(currentDocuments, (d1, d2) -> {
                    String type1 = d1.getFileType() != null ? d1.getFileType() : "";
                    String type2 = d2.getFileType() != null ? d2.getFileType() : "";
                    return type1.compareToIgnoreCase(type2);
                });
                break;

            case TYPE_DESC:
                Collections.sort(currentDocuments, (d1, d2) -> {
                    String type1 = d1.getFileType() != null ? d1.getFileType() : "";
                    String type2 = d2.getFileType() != null ? d2.getFileType() : "";
                    return type2.compareToIgnoreCase(type1);
                });
                break;
        }
    }

    // ================================
    // UI Helpers
    // ================================

    /**
     * Show/hide progress
     */
    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /**
     * Update empty state
     */
    private void updateEmptyState() {
        if (currentDocuments.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            tvEmptyState.setText("No documents in this folder");
        } else {
            tvEmptyState.setVisibility(View.GONE);
        }
    }

    /**
     * Animate chip appearance
     */
    private void animateChipAppearance(View view) {
        view.setAlpha(0f);
        view.setScaleX(0.5f);
        view.setScaleY(0.5f);

        view.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(300)
            .setListener(null);
    }

    // ================================
    // Menu
    // ================================

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_document_organizer, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                documentAdapter.filter(newText);
                return true;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.action_bulk_operations) {
            showBulkOperationsMenu();
            return true;
        } else if (item.getItemId() == R.id.action_refresh) {
            loadFolderTree();
            loadDocumentsInFolder(currentFolderId);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (isMultiSelectMode) {
            toggleMultiSelectMode();
        } else {
            super.onBackPressed();
        }
    }

    // ================================
    // Inner Classes
    // ================================

    /**
     * Folder tree node
     */
    private static class FolderTreeNode {
        Folder folder;
        FolderTreeNode parent;
        List<FolderTreeNode> children = new ArrayList<>();
        int level = 0;
        boolean expanded = false;

        FolderTreeNode(Folder folder) {
            this.folder = folder;
        }

        void addChild(FolderTreeNode child) {
            children.add(child);
            child.level = this.level + 1;
        }

        void setParent(FolderTreeNode parent) {
            this.parent = parent;
        }

        boolean hasChildren() {
            return !children.isEmpty();
        }
    }

    /**
     * Folder tree adapter
     */
    private class FolderTreeAdapter extends RecyclerView.Adapter<FolderTreeAdapter.ViewHolder> {
        private List<FolderTreeNode> visibleNodes = new ArrayList<>();

        void updateData(List<FolderTreeNode> nodes) {
            buildVisibleList(nodes);
            notifyDataSetChanged();
        }

        private void buildVisibleList(List<FolderTreeNode> nodes) {
            visibleNodes.clear();
            for (FolderTreeNode node : nodes) {
                addNodeToVisible(node);
            }
        }

        private void addNodeToVisible(FolderTreeNode node) {
            visibleNodes.add(node);
            if (node.expanded && node.hasChildren()) {
                for (FolderTreeNode child : node.children) {
                    addNodeToVisible(child);
                }
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_folder_tree, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            FolderTreeNode node = visibleNodes.get(position);
            holder.bind(node);
        }

        @Override
        public int getItemCount() {
            return visibleNodes.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvFolderName;
            TextView tvDocCount;
            ImageView ivFolderIcon;
            ImageView ivExpandCollapse;
            View indentView;
            CardView cardFolder;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvFolderName = itemView.findViewById(R.id.tvFolderName);
                tvDocCount = itemView.findViewById(R.id.tvDocCount);
                ivFolderIcon = itemView.findViewById(R.id.ivFolderIcon);
                ivExpandCollapse = itemView.findViewById(R.id.ivExpandCollapse);
                indentView = itemView.findViewById(R.id.indentView);
                cardFolder = itemView.findViewById(R.id.cardFolder);
            }

            void bind(FolderTreeNode node) {
                Folder folder = node.folder;

                // Set folder name
                tvFolderName.setText(folder.getFolderName());

                // Set document count
                tvDocCount.setText(folder.getDocumentCount() + " docs");

                // Set indent based on level
                ViewGroup.LayoutParams params = indentView.getLayoutParams();
                params.width = node.level * 40;
                indentView.setLayoutParams(params);

                // Set expand/collapse icon
                if (node.hasChildren()) {
                    ivExpandCollapse.setVisibility(View.VISIBLE);
                    ivExpandCollapse.setImageResource(node.expanded ?
                        android.R.drawable.arrow_down_float :
                        android.R.drawable.arrow_up_float);
                } else {
                    ivExpandCollapse.setVisibility(View.INVISIBLE);
                }

                // Set folder color
                if (folder.getColor() != null) {
                    try {
                        cardFolder.setCardBackgroundColor(Color.parseColor(folder.getColor()));
                    } catch (Exception e) {
                        cardFolder.setCardBackgroundColor(
                            ContextCompat.getColor(itemView.getContext(), android.R.color.white));
                    }
                }

                // Click listeners
                itemView.setOnClickListener(v -> {
                    loadDocumentsInFolder(folder.getFolderId());
                    animateCardClick(cardFolder);
                });

                ivExpandCollapse.setOnClickListener(v -> {
                    node.expanded = !node.expanded;
                    updateData(folderTree);
                });

                itemView.setOnLongClickListener(v -> {
                    showFolderOptionsMenu(folder);
                    return true;
                });
            }
        }
    }

    /**
     * Document organizer adapter
     */
    private class DocumentOrganizerAdapter extends RecyclerView.Adapter<DocumentOrganizerAdapter.ViewHolder> {
        private List<Document> documents = new ArrayList<>();
        private List<Document> filteredDocuments = new ArrayList<>();
        private boolean multiSelectMode = false;

        void updateData(List<Document> newDocuments) {
            this.documents = newDocuments;
            this.filteredDocuments = new ArrayList<>(newDocuments);
            notifyDataSetChanged();
        }

        void setMultiSelectMode(boolean enabled) {
            this.multiSelectMode = enabled;
            notifyDataSetChanged();
        }

        void filter(String query) {
            if (query.isEmpty()) {
                filteredDocuments = new ArrayList<>(documents);
            } else {
                filteredDocuments.clear();
                for (Document doc : documents) {
                    if (doc.getDocumentName().toLowerCase().contains(query.toLowerCase())) {
                        filteredDocuments.add(doc);
                    }
                }
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_document_organizer, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Document document = filteredDocuments.get(position);
            holder.bind(document);
        }

        @Override
        public int getItemCount() {
            return filteredDocuments.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvDocName;
            TextView tvDocDate;
            TextView tvDocType;
            ImageView ivDocIcon;
            ImageView ivCheckbox;
            CardView cardDocument;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDocName = itemView.findViewById(R.id.tvDocName);
                tvDocDate = itemView.findViewById(R.id.tvDocDate);
                tvDocType = itemView.findViewById(R.id.tvDocType);
                ivDocIcon = itemView.findViewById(R.id.ivDocIcon);
                ivCheckbox = itemView.findViewById(R.id.ivCheckbox);
                cardDocument = itemView.findViewById(R.id.cardDocument);
            }

            void bind(Document document) {
                tvDocName.setText(document.getDocumentName());

                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                tvDocDate.setText(sdf.format(new Date(document.getCreatedAt())));

                String fileType = document.getFileType() != null ? document.getFileType() : "Unknown";
                tvDocType.setText(fileType);

                // Show/hide checkbox
                boolean isSelected = selectedDocumentIds.contains(document.getDocumentId());
                if (multiSelectMode) {
                    ivCheckbox.setVisibility(View.VISIBLE);
                    ivCheckbox.setImageResource(isSelected ?
                        android.R.drawable.checkbox_on_background :
                        android.R.drawable.checkbox_off_background);
                } else {
                    ivCheckbox.setVisibility(View.GONE);
                }

                // Highlight if selected
                cardDocument.setCardElevation(isSelected ? 8f : 2f);

                // Click listener
                itemView.setOnClickListener(v -> {
                    if (multiSelectMode) {
                        toggleDocumentSelection(document.getDocumentId());
                    } else {
                        openDocument(document);
                    }
                });

                // Long click to start drag
                itemView.setOnLongClickListener(v -> {
                    if (!multiSelectMode) {
                        draggingDocument = document;
                        ClipData data = ClipData.newPlainText("", "");
                        View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
                        v.startDrag(data, shadowBuilder, v, 0);
                    }
                    return true;
                });
            }
        }
    }

    /**
     * Show folder options menu
     */
    private void showFolderOptionsMenu(Folder folder) {
        String[] options = {"Rename", "Change Color", "Delete", "View Statistics"};

        new MaterialAlertDialogBuilder(this)
            .setTitle(folder.getFolderName())
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0:
                        showRenameFolderDialog(folder);
                        break;
                    case 1:
                        showFolderColorPicker(folder);
                        break;
                    case 2:
                        showDeleteFolderDialog(folder);
                        break;
                    case 3:
                        showFolderStatistics(folder);
                        break;
                }
            })
            .show();
    }

    /**
     * Show folder statistics
     */
    private void showFolderStatistics(Folder folder) {
        folderRepository.getFolderStatistics(folder.getFolderId(),
            new FolderRepository.StatisticsCallback() {
                @Override
                public void onSuccess(FolderRepository.FolderStatistics statistics) {
                    runOnUiThread(() -> {
                        String stats = "Folder Statistics\n\n" +
                            "Documents: " + statistics.documentCount + "\n" +
                            "PDFs: " + statistics.pdfCount + "\n" +
                            "Images: " + statistics.imageCount + "\n" +
                            "Subfolders: " + statistics.subfolderCount + "\n" +
                            "Total Size: " + formatFileSize(statistics.totalSize) + "\n" +
                            "Total (Recursive): " + statistics.totalDocumentsRecursive + " docs";

                        new MaterialAlertDialogBuilder(DocumentOrganizerActivity.this)
                            .setTitle(folder.getFolderName())
                            .setMessage(stats)
                            .setPositiveButton("OK", null)
                            .show();
                    });
                }

                @Override
                public void onError(Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(DocumentOrganizerActivity.this,
                            "Error loading statistics", Toast.LENGTH_SHORT).show();
                    });
                }
            });
    }

    /**
     * Format file size
     */
    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.2f MB", size / (1024.0 * 1024));
        return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
    }

    /**
     * Open document
     */
    private void openDocument(Document document) {
        Intent intent = new Intent(this, DocumentViewerActivity.class);
        intent.putExtra("document_id", document.getDocumentId());
        startActivity(intent);
    }

    /**
     * Animate card click
     */
    private void animateCardClick(View view) {
        ObjectAnimator scaleDown = ObjectAnimator.ofFloat(view, "scaleX", 0.95f);
        scaleDown.setDuration(100);
        scaleDown.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                ObjectAnimator scaleUp = ObjectAnimator.ofFloat(view, "scaleX", 1f);
                scaleUp.setDuration(100);
                scaleUp.start();
            }
        });
        scaleDown.start();

        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 0.95f);
        scaleDownY.setDuration(100);
        scaleDownY.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 1f);
                scaleUpY.setDuration(100);
                scaleUpY.start();
            }
        });
        scaleDownY.start();
    }
}

