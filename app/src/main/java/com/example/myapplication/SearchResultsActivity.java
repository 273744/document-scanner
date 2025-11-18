package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SearchResultsActivity - Display and manage search results
 *
 * Features:
 * 1. RecyclerView with search result cards
 * 2. Highlighted text snippets showing matches
 * 3. Thumbnail previews of matching documents
 * 4. Filter panel for refining results
 * 5. Sort options (relevance, date, name)
 * 6. Infinite scroll for large result sets
 * 7. Search within results functionality
 * 8. Empty states and loading indicators
 */
public class SearchResultsActivity extends AppCompatActivity {

    private static final String TAG = "SearchResultsActivity";

    // Intent extras
    public static final String EXTRA_QUERY = "query";
    public static final String EXTRA_FILTER = "filter";

    // UI Components
    private RecyclerView rvResults;
    private SearchResultsAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private TextView tvResultCount;
    private EditText etSearchWithin;
    private ChipGroup chipGroupFilters;
    private FloatingActionButton fabFilter;
    private FloatingActionButton fabSort;

    // Data
    private DocumentSearchManager searchManager;
    private String originalQuery;
    private DocumentSearchManager.SearchFilter currentFilter;
    private List<SearchableDatabase.SearchResult> allResults = new ArrayList<>();
    private List<SearchableDatabase.SearchResult> filteredResults = new ArrayList<>();
    private SortOption currentSort = SortOption.RELEVANCE;

    // Search within results
    private String searchWithinQuery = "";
    private Handler searchWithinHandler = new Handler(Looper.getMainLooper());
    private Runnable searchWithinRunnable;

    // Infinite scroll
    private boolean isLoading = false;
    private boolean hasMoreResults = true;
    private int currentPage = 0;
    private static final int PAGE_SIZE = 20;

    // Threading
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    /**
     * Sort options
     */
    public enum SortOption {
        RELEVANCE("Relevance"),
        DATE_DESC("Date (Newest First)"),
        DATE_ASC("Date (Oldest First)"),
        NAME_ASC("Name (A-Z)"),
        NAME_DESC("Name (Z-A)"),
        FILE_SIZE_DESC("Size (Largest First)"),
        FILE_SIZE_ASC("Size (Smallest First)");

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
        // TODO: Create activity_search_results.xml layout
        // setContentView(R.layout.activity_search_results);

        // Setup toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Search Results");
        }

        // Get intent data
        originalQuery = getIntent().getStringExtra(EXTRA_QUERY);
        currentFilter = new DocumentSearchManager.SearchFilter();

        // Initialize search manager
        searchManager = DocumentSearchManager.getInstance(this);

        // Temporary: Show message that UI is pending
        Toast.makeText(this, "Search Results - Layout pending implementation", Toast.LENGTH_LONG).show();
        finish();

        // Initialize views - commented out until layouts are created
        // initializeViews();
        // setupRecyclerView();
        // setupSearchWithin();
        // setupFabs();
        // if (originalQuery != null && !originalQuery.isEmpty()) {
        //     performSearch();
        // }
    }

    /**
     * Initialize views
     */
    private void initializeViews() {
        // TODO: Uncomment when layout XML is created
        /*
        rvResults = findViewById(R.id.rvResults);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvResultCount = findViewById(R.id.tvResultCount);
        etSearchWithin = findViewById(R.id.etSearchWithin);
        chipGroupFilters = findViewById(R.id.chipGroupFilters);
        fabFilter = findViewById(R.id.fabFilter);
        fabSort = findViewById(R.id.fabSort);
        */
    }

    /**
     * Setup RecyclerView
     */
    private void setupRecyclerView() {
        adapter = new SearchResultsAdapter();
        rvResults.setLayoutManager(new LinearLayoutManager(this));
        rvResults.setAdapter(adapter);

        // Setup infinite scroll
        rvResults.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (!isLoading && hasMoreResults) {
                    LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    if (layoutManager != null) {
                        int visibleItemCount = layoutManager.getChildCount();
                        int totalItemCount = layoutManager.getItemCount();
                        int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5) {
                            loadMoreResults();
                        }
                    }
                }
            }
        });
    }

    /**
     * Setup search within results
     */
    private void setupSearchWithin() {
        etSearchWithin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Cancel previous search
                if (searchWithinRunnable != null) {
                    searchWithinHandler.removeCallbacks(searchWithinRunnable);
                }

                // Schedule new search with debouncing
                searchWithinRunnable = () -> {
                    searchWithinQuery = s.toString();
                    filterResults();
                };

                searchWithinHandler.postDelayed(searchWithinRunnable, 300);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * Setup FABs
     */
    private void setupFabs() {
        fabFilter.setOnClickListener(v -> showFilterDialog());
        fabSort.setOnClickListener(v -> showSortDialog());
    }

    // ================================
    // Search Operations
    // ================================

    /**
     * Perform initial search
     */
    private void performSearch() {
        showLoading(true);
        currentPage = 0;
        allResults.clear();
        filteredResults.clear();

        searchManager.searchImmediate(originalQuery, currentFilter);

        // Add listener for results
        searchManager.addSearchListener(new DocumentSearchManager.SearchListener() {
            @Override
            public void onSearchPending(String query) {}

            @Override
            public void onSearchStarted(String query) {}

            @Override
            public void onSearchComplete(List<SearchableDatabase.SearchResult> results, long searchTime) {
                runOnUiThread(() -> {
                    allResults.clear();
                    allResults.addAll(results);

                    // Apply sorting
                    sortResults();

                    // Apply filtering
                    filterResults();

                    // Update UI
                    showLoading(false);
                    updateResultCount();
                    updateFilterChips();

                    // Check if there are more results
                    hasMoreResults = results.size() >= currentFilter.limit;

                    Log.d(TAG, "Search completed: " + results.size() + " results in " + searchTime + "ms");
                });

                // Remove listener after first result
                searchManager.removeSearchListener(this);
            }

            @Override
            public void onSearchError(Exception e) {
                runOnUiThread(() -> {
                    showLoading(false);
                    showEmptyState("Error: " + e.getMessage());
                    Toast.makeText(SearchResultsActivity.this,
                        "Search error: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                });
                searchManager.removeSearchListener(this);
            }

            @Override
            public void onSearchCleared() {}
        });
    }

    /**
     * Load more results (infinite scroll)
     */
    private void loadMoreResults() {
        if (isLoading || !hasMoreResults) return;

        isLoading = true;
        currentPage++;

        // Show loading indicator
        adapter.setLoading(true);

        // Simulate loading more results
        // In a real implementation, you would adjust the filter.limit and offset
        executorService.execute(() -> {
            try {
                Thread.sleep(500); // Simulate network delay

                runOnUiThread(() -> {
                    // For now, just mark as no more results
                    hasMoreResults = false;
                    isLoading = false;
                    adapter.setLoading(false);
                });

            } catch (InterruptedException e) {
                Log.e(TAG, "Error loading more results", e);
            }
        });
    }

    /**
     * Filter results based on search within query
     */
    private void filterResults() {
        filteredResults.clear();

        if (searchWithinQuery.isEmpty()) {
            filteredResults.addAll(allResults);
        } else {
            String lowerQuery = searchWithinQuery.toLowerCase();

            for (SearchableDatabase.SearchResult result : allResults) {
                if (result.documentName.toLowerCase().contains(lowerQuery) ||
                    result.snippet.toLowerCase().contains(lowerQuery) ||
                    (result.fileType != null && result.fileType.toLowerCase().contains(lowerQuery))) {
                    filteredResults.add(result);
                }
            }
        }

        // Paginate results
        int endIndex = Math.min((currentPage + 1) * PAGE_SIZE, filteredResults.size());
        List<SearchableDatabase.SearchResult> paginatedResults =
            filteredResults.subList(0, endIndex);

        adapter.updateResults(paginatedResults, originalQuery + " " + searchWithinQuery);
        updateResultCount();

        if (filteredResults.isEmpty()) {
            showEmptyState("No results found");
        } else {
            hideEmptyState();
        }
    }

    /**
     * Sort results
     */
    private void sortResults() {
        switch (currentSort) {
            case RELEVANCE:
                // Already sorted by relevance from search
                break;

            case DATE_DESC:
                allResults.sort((r1, r2) ->
                    Long.compare(r2.createdAt, r1.createdAt));
                break;

            case DATE_ASC:
                allResults.sort((r1, r2) ->
                    Long.compare(r1.createdAt, r2.createdAt));
                break;

            case NAME_ASC:
                allResults.sort((r1, r2) ->
                    r1.documentName.compareToIgnoreCase(r2.documentName));
                break;

            case NAME_DESC:
                allResults.sort((r1, r2) ->
                    r2.documentName.compareToIgnoreCase(r1.documentName));
                break;

            case FILE_SIZE_DESC:
                allResults.sort((r1, r2) ->
                    Long.compare(r2.fileSize, r1.fileSize));
                break;

            case FILE_SIZE_ASC:
                allResults.sort((r1, r2) ->
                    Long.compare(r1.fileSize, r2.fileSize));
                break;
        }
    }

    // ================================
    // Filter & Sort Dialogs
    // ================================

    /**
     * Show filter dialog
     */
    private void showFilterDialog() {
        // TODO: Create bottom_sheet_filter.xml layout
        Toast.makeText(this, "Filter dialog - Layout pending implementation", Toast.LENGTH_SHORT).show();

        /* Uncomment when layout is created
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_filter, null);

        // TODO: Setup filter options
        // - File type checkboxes
        // - Date range picker
        // - Folder selector
        // - Favorites toggle
        // - Page count range
        // - File size range

        TextView tvApply = view.findViewById(R.id.tvApply);
        TextView tvReset = view.findViewById(R.id.tvReset);

        tvApply.setOnClickListener(v -> {
            // Apply filters and re-search
            performSearch();
            dialog.dismiss();
        });

        tvReset.setOnClickListener(v -> {
            currentFilter = new DocumentSearchManager.SearchFilter();
            performSearch();
            dialog.dismiss();
        });

        dialog.setContentView(view);
        dialog.show();
        */
    }

    /**
     * Show sort dialog
     */
    private void showSortDialog() {
        String[] options = new String[SortOption.values().length];
        for (int i = 0; i < SortOption.values().length; i++) {
            options[i] = SortOption.values()[i].getDisplayName();
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Sort Results")
            .setSingleChoiceItems(options, currentSort.ordinal(), (dialog, which) -> {
                currentSort = SortOption.values()[which];
                sortResults();
                filterResults();
                dialog.dismiss();

                Toast.makeText(this,
                    "Sorted by " + currentSort.getDisplayName(),
                    Toast.LENGTH_SHORT).show();
            })
            .show();
    }

    /**
     * Update filter chips
     */
    private void updateFilterChips() {
        chipGroupFilters.removeAllViews();

        // Add chip for each active filter
        if (currentFilter.fileType != null) {
            addFilterChip("Type: " + currentFilter.fileType, () -> {
                currentFilter.fileType = null;
                performSearch();
            });
        }

        if (currentFilter.folderId != null) {
            addFilterChip("Folder", () -> {
                currentFilter.folderId = null;
                performSearch();
            });
        }

        if (currentFilter.favoritesOnly) {
            addFilterChip("Favorites", () -> {
                currentFilter.favoritesOnly = false;
                performSearch();
            });
        }

        if (currentFilter.dateFrom > 0 || currentFilter.dateTo > 0) {
            addFilterChip("Date Range", () -> {
                currentFilter.dateFrom = 0;
                currentFilter.dateTo = 0;
                performSearch();
            });
        }
    }

    /**
     * Add filter chip
     */
    private void addFilterChip(String text, Runnable onClose) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> onClose.run());
        chipGroupFilters.addView(chip);
    }

    // ================================
    // UI Updates
    // ================================

    /**
     * Update result count
     */
    private void updateResultCount() {
        int total = filteredResults.size();
        int showing = Math.min((currentPage + 1) * PAGE_SIZE, total);

        String text = showing + " of " + total + " results";
        if (!searchWithinQuery.isEmpty()) {
            text += " (filtered)";
        }

        tvResultCount.setText(text);
        tvResultCount.setVisibility(total > 0 ? View.VISIBLE : View.GONE);
    }

    /**
     * Show loading indicator
     */
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        rvResults.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    /**
     * Show empty state
     */
    private void showEmptyState(String message) {
        tvEmptyState.setText(message);
        tvEmptyState.setVisibility(View.VISIBLE);
        rvResults.setVisibility(View.GONE);
    }

    /**
     * Hide empty state
     */
    private void hideEmptyState() {
        tvEmptyState.setVisibility(View.GONE);
        rvResults.setVisibility(View.VISIBLE);
    }

    // ================================
    // Menu
    // ================================

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // TODO: Create menu_search_results.xml
        // getMenuInflater().inflate(R.menu.menu_search_results, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        // TODO: Add menu items when menu XML is created
        /*
        else if (item.getItemId() == R.id.action_save_search) {
            showSaveSearchDialog();
            return true;
        } else if (item.getItemId() == R.id.action_share_results) {
            shareResults();
            return true;
        }
        */
        return super.onOptionsItemSelected(item);
    }

    /**
     * Show save search dialog
     */
    private void showSaveSearchDialog() {
        EditText input = new EditText(this);
        input.setHint("Search name");

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Save Search")
            .setView(input)
            .setPositiveButton("Save", (dialog, which) -> {
                String name = input.getText().toString().trim();
                if (!name.isEmpty()) {
                    saveSearch(name);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    /**
     * Save current search
     */
    private void saveSearch(String name) {
        searchManager.saveCurrentSearch(name, new DocumentSearchManager.SavedSearchCallback() {
            @Override
            public void onSaved(DocumentSearchManager.SavedSearch savedSearch) {
                Toast.makeText(SearchResultsActivity.this,
                    "Search saved: " + name,
                    Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(SearchResultsActivity.this,
                    "Error: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Share results
     */
    private void shareResults() {
        StringBuilder text = new StringBuilder();
        text.append("Search Results for \"").append(originalQuery).append("\"\n\n");
        text.append("Found ").append(filteredResults.size()).append(" documents:\n\n");

        for (int i = 0; i < Math.min(10, filteredResults.size()); i++) {
            SearchableDatabase.SearchResult result = filteredResults.get(i);
            text.append((i + 1)).append(". ").append(result.documentName).append("\n");
        }

        if (filteredResults.size() > 10) {
            text.append("\n... and ").append(filteredResults.size() - 10).append(" more");
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, text.toString());
        startActivity(Intent.createChooser(shareIntent, "Share Search Results"));
    }

    // ================================
    // Adapter
    // ================================

    /**
     * Search Results Adapter
     */
    private class SearchResultsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int VIEW_TYPE_ITEM = 0;
        private static final int VIEW_TYPE_LOADING = 1;

        private List<SearchableDatabase.SearchResult> results = new ArrayList<>();
        private String highlightQuery = "";
        private boolean showLoading = false;

        void updateResults(List<SearchableDatabase.SearchResult> newResults, String query) {
            this.results = newResults;
            this.highlightQuery = query;
            notifyDataSetChanged();
        }

        void setLoading(boolean loading) {
            this.showLoading = loading;
            if (loading) {
                notifyItemInserted(results.size());
            } else {
                notifyItemRemoved(results.size());
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == results.size() && showLoading) {
                return VIEW_TYPE_LOADING;
            }
            return VIEW_TYPE_ITEM;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // TODO: Create item_loading.xml and item_search_result.xml layouts
            if (viewType == VIEW_TYPE_LOADING) {
                // View view = LayoutInflater.from(parent.getContext())
                //     .inflate(R.layout.item_loading, parent, false);
                View view = new View(parent.getContext());
                return new LoadingViewHolder(view);
            }

            // View view = LayoutInflater.from(parent.getContext())
            //     .inflate(R.layout.item_search_result, parent, false);
            View view = new View(parent.getContext());
            return new ResultViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof ResultViewHolder) {
                ((ResultViewHolder) holder).bind(results.get(position));
            }
        }

        @Override
        public int getItemCount() {
            return results.size() + (showLoading ? 1 : 0);
        }

        /**
         * Result ViewHolder
         */
        class ResultViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle;
            TextView tvSnippet;
            TextView tvMetadata;
            ImageView ivThumbnail;
            ImageView ivFileType;
            CardView cardView;

            ResultViewHolder(@NonNull View itemView) {
                super(itemView);
                // TODO: Uncomment when layout is created
                /*
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvSnippet = itemView.findViewById(R.id.tvSnippet);
                tvMetadata = itemView.findViewById(R.id.tvMetadata);
                ivThumbnail = itemView.findViewById(R.id.ivThumbnail);
                ivFileType = itemView.findViewById(R.id.ivFileType);
                cardView = itemView.findViewById(R.id.cardView);
                */
            }

            void bind(SearchableDatabase.SearchResult result) {
                // TODO: Implement binding when layout views are available
                if (tvTitle == null) return;

                // Set title with highlighting
                SpannableString highlightedTitle =
                    searchManager.highlightText(result.documentName, highlightQuery);
                tvTitle.setText(highlightedTitle);

                // Set snippet with highlighting
                SpannableString highlightedSnippet =
                    searchManager.highlightText(result.snippet, highlightQuery);
                tvSnippet.setText(highlightedSnippet);

                // Set metadata
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                String date = sdf.format(new Date(result.createdAt));
                String size = formatFileSize(result.fileSize);
                String metadata = date + " • " + size + " • " + result.pageCount + " pages";
                tvMetadata.setText(metadata);

                // Set file type icon
                setFileTypeIcon(ivFileType, result.fileType);

                // Load thumbnail
                loadThumbnail(ivThumbnail, result);

                // Click listener
                itemView.setOnClickListener(v -> openDocument(result));
            }
        }

        /**
         * Loading ViewHolder
         */
        class LoadingViewHolder extends RecyclerView.ViewHolder {
            LoadingViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }
    }

    /**
     * Set file type icon
     */
    private void setFileTypeIcon(ImageView imageView, String fileType) {
        int iconRes = android.R.drawable.ic_menu_gallery;

        if ("PDF".equals(fileType)) {
            iconRes = android.R.drawable.ic_menu_save;
        } else if ("IMAGE".equals(fileType)) {
            iconRes = android.R.drawable.ic_menu_gallery;
        }

        imageView.setImageResource(iconRes);
    }

    /**
     * Load thumbnail
     */
    private void loadThumbnail(ImageView imageView, SearchableDatabase.SearchResult result) {
        executorService.execute(() -> {
            try {
                // Load thumbnail from file path
                File file = new File(result.fileName);
                if (file.exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                    if (bitmap != null) {
                        runOnUiThread(() -> imageView.setImageBitmap(bitmap));
                        return;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading thumbnail", e);
            }

            // Set default thumbnail
            runOnUiThread(() -> imageView.setImageResource(android.R.drawable.ic_menu_gallery));
        });
    }

    /**
     * Format file size
     */
    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }

    /**
     * Open document
     */
    private void openDocument(SearchableDatabase.SearchResult result) {
        Intent intent = new Intent(this, DocumentViewerActivity.class);
        intent.putExtra("document_id", result.documentId);
        startActivity(intent);
    }

    // ================================
    // Static Methods
    // ================================

    /**
     * Start this activity with search query
     */
    public static void start(Context context, String query) {
        Intent intent = new Intent(context, SearchResultsActivity.class);
        intent.putExtra(EXTRA_QUERY, query);
        context.startActivity(intent);
    }

    /**
     * Start this activity with search query and filter
     */
    public static void start(Context context, String query, DocumentSearchManager.SearchFilter filter) {
        Intent intent = new Intent(context, SearchResultsActivity.class);
        intent.putExtra(EXTRA_QUERY, query);
        // intent.putExtra(EXTRA_FILTER, filter); // Would need Serializable
        context.startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}

