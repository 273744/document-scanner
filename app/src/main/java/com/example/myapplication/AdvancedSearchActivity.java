package com.example.myapplication;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.RangeSlider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * AdvancedSearchActivity - Power user search interface
 *
 * Features:
 * 1. Search form with multiple criteria fields
 * 2. Date range picker for temporal filtering
 * 3. Folder and tag selection filters
 * 4. Document type and size filters
 * 5. Boolean search operators (AND, OR, NOT)
 * 6. Regular expressions support toggle
 * 7. Search query builder with visual interface
 * 8. Search tips and examples for user guidance
 */
public class AdvancedSearchActivity extends AppCompatActivity {

    private static final String TAG = "AdvancedSearch";

    // UI Components - Search Criteria
    private EditText etSearchQuery;
    private EditText etDocumentName;
    private EditText etOcrText;
    private EditText etDescription;
    private Spinner spSearchOperator;
    private CheckBox cbRegexMode;
    private CheckBox cbCaseSensitive;
    private TextView tvRegexError;

    // Date Range
    private Button btnDateFrom;
    private Button btnDateTo;
    private Button btnClearDateRange;
    private long dateFromMillis = 0;
    private long dateToMillis = 0;

    // Folder & Tags
    private Button btnSelectFolders;
    private Button btnSelectTags;
    private ChipGroup chipGroupFolders;
    private ChipGroup chipGroupTags;
    private List<Long> selectedFolderIds = new ArrayList<>();
    private List<String> selectedTags = new ArrayList<>();

    // Document Type
    private CheckBox cbPDF;
    private CheckBox cbImage;
    private CheckBox cbOther;
    private CheckBox cbFavoritesOnly;

    // Document Size
    private RangeSlider sliderFileSize;
    private TextView tvFileSizeRange;

    // Page Count
    private RangeSlider sliderPageCount;
    private TextView tvPageCountRange;

    // Query Builder
    private LinearLayout queryBuilderContainer;
    private TextView tvBuiltQuery;
    private Button btnAddCondition;
    private Button btnClearQuery;
    private List<QueryCondition> queryConditions = new ArrayList<>();

    // Action Buttons
    private Button btnSearch;
    private Button btnReset;
    private Button btnShowTips;

    // Search Manager
    private DocumentSearchManager searchManager;

    // Search operators
    private enum SearchOperator {
        AND("AND - All terms must match"),
        OR("OR - Any term can match"),
        NOT("NOT - Exclude these terms");

        private final String displayName;

        SearchOperator(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO: Create activity_advanced_search.xml layout
        // setContentView(R.layout.activity_advanced_search);

        // Setup toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Advanced Search");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize search manager
        searchManager = DocumentSearchManager.getInstance(this);

        // Temporary: Show message that UI is pending
        Toast.makeText(this, "Advanced Search - Layout pending implementation", Toast.LENGTH_LONG).show();
        finish();

        // Initialize views - commented out until layouts are created
        // initializeViews();
        // setupSearchForm();
        // setupDatePickers();
        // setupFilters();
        // setupQueryBuilder();
        // setupActionButtons();
    }

    /**
     * Initialize views
     */
    private void initializeViews() {
        // TODO: Uncomment when layout XML is created
        /*
        // Search criteria
        etSearchQuery = findViewById(R.id.etSearchQuery);
        etDocumentName = findViewById(R.id.etDocumentName);
        etOcrText = findViewById(R.id.etOcrText);
        etDescription = findViewById(R.id.etDescription);
        spSearchOperator = findViewById(R.id.spSearchOperator);
        cbRegexMode = findViewById(R.id.cbRegexMode);
        cbCaseSensitive = findViewById(R.id.cbCaseSensitive);
        tvRegexError = findViewById(R.id.tvRegexError);

        // Date range
        btnDateFrom = findViewById(R.id.btnDateFrom);
        btnDateTo = findViewById(R.id.btnDateTo);
        btnClearDateRange = findViewById(R.id.btnClearDateRange);

        // Folder & Tags
        btnSelectFolders = findViewById(R.id.btnSelectFolders);
        btnSelectTags = findViewById(R.id.btnSelectTags);
        chipGroupFolders = findViewById(R.id.chipGroupFolders);
        chipGroupTags = findViewById(R.id.chipGroupTags);

        // Document type
        cbPDF = findViewById(R.id.cbPDF);
        cbImage = findViewById(R.id.cbImage);
        cbOther = findViewById(R.id.cbOther);
        cbFavoritesOnly = findViewById(R.id.cbFavoritesOnly);

        // Size & Page filters
        sliderFileSize = findViewById(R.id.sliderFileSize);
        tvFileSizeRange = findViewById(R.id.tvFileSizeRange);
        sliderPageCount = findViewById(R.id.sliderPageCount);
        tvPageCountRange = findViewById(R.id.tvPageCountRange);

        // Query builder
        queryBuilderContainer = findViewById(R.id.queryBuilderContainer);
        tvBuiltQuery = findViewById(R.id.tvBuiltQuery);
        btnAddCondition = findViewById(R.id.btnAddCondition);
        btnClearQuery = findViewById(R.id.btnClearQuery);

        // Action buttons
        btnSearch = findViewById(R.id.btnSearch);
        btnReset = findViewById(R.id.btnReset);
        btnShowTips = findViewById(R.id.btnShowTips);
        */
    }

    // ================================
    // 1. Search Form Setup
    // ================================

    /**
     * Setup search form
     */
    private void setupSearchForm() {
        // Setup operator spinner
        ArrayAdapter<String> operatorAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item);
        for (SearchOperator op : SearchOperator.values()) {
            operatorAdapter.add(op.getDisplayName());
        }
        operatorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSearchOperator.setAdapter(operatorAdapter);

        // Setup regex mode toggle
        cbRegexMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                validateRegexQuery();
            } else {
                tvRegexError.setVisibility(View.GONE);
            }
        });

        // Add text watcher for regex validation
        etSearchQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (cbRegexMode.isChecked()) {
                    validateRegexQuery();
                }
                updateBuiltQuery();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Add text watchers for other fields
        TextWatcher updateQueryWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateBuiltQuery();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        };

        etDocumentName.addTextChangedListener(updateQueryWatcher);
        etOcrText.addTextChangedListener(updateQueryWatcher);
        etDescription.addTextChangedListener(updateQueryWatcher);
    }

    /**
     * Validate regex query
     */
    private void validateRegexQuery() {
        String query = etSearchQuery.getText().toString().trim();

        if (query.isEmpty()) {
            tvRegexError.setVisibility(View.GONE);
            return;
        }

        try {
            Pattern.compile(query);
            tvRegexError.setVisibility(View.GONE);
        } catch (PatternSyntaxException e) {
            tvRegexError.setText("Invalid regex: " + e.getDescription());
            tvRegexError.setVisibility(View.VISIBLE);
        }
    }

    // ================================
    // 2. Date Range Picker
    // ================================

    /**
     * Setup date pickers
     */
    private void setupDatePickers() {
        btnDateFrom.setOnClickListener(v -> showDatePicker(true));
        btnDateTo.setOnClickListener(v -> showDatePicker(false));
        btnClearDateRange.setOnClickListener(v -> clearDateRange());

        updateDateButtons();
    }

    /**
     * Show date picker dialog
     */
    private void showDatePicker(boolean isFromDate) {
        Calendar calendar = Calendar.getInstance();

        // Set current date if already selected
        if (isFromDate && dateFromMillis > 0) {
            calendar.setTimeInMillis(dateFromMillis);
        } else if (!isFromDate && dateToMillis > 0) {
            calendar.setTimeInMillis(dateToMillis);
        }

        DatePickerDialog dialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(year, month, dayOfMonth);

                if (isFromDate) {
                    selected.set(Calendar.HOUR_OF_DAY, 0);
                    selected.set(Calendar.MINUTE, 0);
                    selected.set(Calendar.SECOND, 0);
                    dateFromMillis = selected.getTimeInMillis();
                } else {
                    selected.set(Calendar.HOUR_OF_DAY, 23);
                    selected.set(Calendar.MINUTE, 59);
                    selected.set(Calendar.SECOND, 59);
                    dateToMillis = selected.getTimeInMillis();
                }

                updateDateButtons();
                updateBuiltQuery();
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }

    /**
     * Update date button labels
     */
    private void updateDateButtons() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

        if (dateFromMillis > 0) {
            btnDateFrom.setText("From: " + sdf.format(dateFromMillis));
        } else {
            btnDateFrom.setText("Select From Date");
        }

        if (dateToMillis > 0) {
            btnDateTo.setText("To: " + sdf.format(dateToMillis));
        } else {
            btnDateTo.setText("Select To Date");
        }

        btnClearDateRange.setVisibility(
            (dateFromMillis > 0 || dateToMillis > 0) ? View.VISIBLE : View.GONE
        );
    }

    /**
     * Clear date range
     */
    private void clearDateRange() {
        dateFromMillis = 0;
        dateToMillis = 0;
        updateDateButtons();
        updateBuiltQuery();
    }

    // ================================
    // 3. Folder and Tag Selection
    // ================================

    /**
     * Setup folder and tag selection
     */
    private void setupFilters() {
        btnSelectFolders.setOnClickListener(v -> showFolderSelector());
        btnSelectTags.setOnClickListener(v -> showTagSelector());

        // Setup file size slider
        sliderFileSize.setValueFrom(0f);
        sliderFileSize.setValueTo(100f); // 100 MB
        sliderFileSize.setValues(0f, 100f);
        sliderFileSize.addOnChangeListener((slider, value, fromUser) -> {
            updateFileSizeLabel();
            updateBuiltQuery();
        });

        // Setup page count slider
        sliderPageCount.setValueFrom(1f);
        sliderPageCount.setValueTo(100f);
        sliderPageCount.setValues(1f, 100f);
        sliderPageCount.addOnChangeListener((slider, value, fromUser) -> {
            updatePageCountLabel();
            updateBuiltQuery();
        });

        updateFileSizeLabel();
        updatePageCountLabel();
    }

    /**
     * Show folder selector dialog
     */
    private void showFolderSelector() {
        // TODO: Load folders from FolderRepository
        Toast.makeText(this, "Folder selector - Implementation pending", Toast.LENGTH_SHORT).show();

        // Example: Add a test folder
        selectedFolderIds.add(1L);
        addFolderChip(1L, "Work Documents");
    }

    /**
     * Show tag selector dialog
     */
    private void showTagSelector() {
        // TODO: Load tags from database
        Toast.makeText(this, "Tag selector - Implementation pending", Toast.LENGTH_SHORT).show();

        // Example: Add a test tag
        selectedTags.add("Important");
        addTagChip("Important");
    }

    /**
     * Add folder chip
     */
    private void addFolderChip(long folderId, String folderName) {
        Chip chip = new Chip(this);
        chip.setText(folderName);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> {
            selectedFolderIds.remove(folderId);
            chipGroupFolders.removeView(chip);
            updateBuiltQuery();
        });
        chipGroupFolders.addView(chip);
        updateBuiltQuery();
    }

    /**
     * Add tag chip
     */
    private void addTagChip(String tag) {
        Chip chip = new Chip(this);
        chip.setText(tag);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> {
            selectedTags.remove(tag);
            chipGroupTags.removeView(chip);
            updateBuiltQuery();
        });
        chipGroupTags.addView(chip);
        updateBuiltQuery();
    }

    /**
     * Update file size label
     */
    private void updateFileSizeLabel() {
        List<Float> values = sliderFileSize.getValues();
        String label = String.format(Locale.getDefault(),
            "%.0f MB - %.0f MB", values.get(0), values.get(1));
        tvFileSizeRange.setText(label);
    }

    /**
     * Update page count label
     */
    private void updatePageCountLabel() {
        List<Float> values = sliderPageCount.getValues();
        String label = String.format(Locale.getDefault(),
            "%.0f - %.0f pages", values.get(0), values.get(1));
        tvPageCountRange.setText(label);
    }

    // ================================
    // 4. Document Type and Size Filters
    // ================================

    /**
     * Get selected file types
     */
    private List<String> getSelectedFileTypes() {
        List<String> types = new ArrayList<>();

        if (cbPDF.isChecked()) types.add("PDF");
        if (cbImage.isChecked()) types.add("IMAGE");
        if (cbOther.isChecked()) types.add("OTHER");

        return types;
    }

    /**
     * Get file size range
     */
    private long[] getFileSizeRange() {
        List<Float> values = sliderFileSize.getValues();
        return new long[] {
            (long) (values.get(0) * 1024 * 1024), // Convert MB to bytes
            (long) (values.get(1) * 1024 * 1024)
        };
    }

    /**
     * Get page count range
     */
    private int[] getPageCountRange() {
        List<Float> values = sliderPageCount.getValues();
        return new int[] {
            values.get(0).intValue(),
            values.get(1).intValue()
        };
    }

    // ================================
    // 5. Boolean Search Operators
    // ================================

    /**
     * Build query with boolean operators
     */
    private String buildBooleanQuery() {
        StringBuilder query = new StringBuilder();
        SearchOperator operator = SearchOperator.values()[spSearchOperator.getSelectedItemPosition()];

        String mainQuery = etSearchQuery.getText().toString().trim();
        String nameQuery = etDocumentName.getText().toString().trim();
        String ocrQuery = etOcrText.getText().toString().trim();
        String descQuery = etDescription.getText().toString().trim();

        List<String> terms = new ArrayList<>();

        if (!mainQuery.isEmpty()) {
            terms.add("(" + mainQuery + ")");
        }

        if (!nameQuery.isEmpty()) {
            terms.add("document_name:(" + nameQuery + ")");
        }

        if (!ocrQuery.isEmpty()) {
            terms.add("ocr_text:(" + ocrQuery + ")");
        }

        if (!descQuery.isEmpty()) {
            terms.add("description:(" + descQuery + ")");
        }

        if (terms.isEmpty()) {
            return "";
        }

        // Combine terms with operator
        String operatorStr = " " + operator.name() + " ";
        for (int i = 0; i < terms.size(); i++) {
            if (i > 0) {
                query.append(operatorStr);
            }
            query.append(terms.get(i));
        }

        return query.toString();
    }

    // ================================
    // 6. Regular Expression Support
    // ================================

    /**
     * Apply regex mode to query
     */
    private String applyRegexMode(String query) {
        if (!cbRegexMode.isChecked()) {
            return query;
        }

        // Wrap query in regex syntax
        return "REGEX:" + query;
    }

    /**
     * Test regex pattern
     */
    private void testRegexPattern() {
        String pattern = etSearchQuery.getText().toString().trim();

        if (pattern.isEmpty()) {
            Toast.makeText(this, "Enter a regex pattern to test", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Pattern.compile(pattern);

            // Show test dialog
            showRegexTestDialog(pattern);

        } catch (PatternSyntaxException e) {
            Toast.makeText(this, "Invalid regex: " + e.getDescription(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Show regex test dialog
     */
    private void showRegexTestDialog(String pattern) {
        // TODO: Create regex test dialog
        Toast.makeText(this, "Regex test dialog - Implementation pending", Toast.LENGTH_SHORT).show();
    }

    // ================================
    // 7. Visual Query Builder
    // ================================

    /**
     * Setup query builder
     */
    private void setupQueryBuilder() {
        btnAddCondition.setOnClickListener(v -> addQueryCondition());
        btnClearQuery.setOnClickListener(v -> clearQueryBuilder());

        updateBuiltQuery();
    }

    /**
     * Add query condition
     */
    private void addQueryCondition() {
        // TODO: Create item_query_condition.xml layout
        Toast.makeText(this, "Query condition - Layout pending implementation", Toast.LENGTH_SHORT).show();

        /* Uncomment when layout is created
        View conditionView = getLayoutInflater().inflate(
            R.layout.item_query_condition, queryBuilderContainer, false);

        // Setup condition view
        Spinner spField = conditionView.findViewById(R.id.spField);
        Spinner spOperator = conditionView.findViewById(R.id.spOperator);
        EditText etValue = conditionView.findViewById(R.id.etValue);
        Button btnRemove = conditionView.findViewById(R.id.btnRemove);

        // Field options
        String[] fields = {"Document Name", "OCR Text", "Description", "Tags", "File Type"};
        ArrayAdapter<String> fieldAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, fields);
        fieldAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spField.setAdapter(fieldAdapter);

        // Operator options
        String[] operators = {"Contains", "Equals", "Starts With", "Ends With", "Regex"};
        ArrayAdapter<String> operatorAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, operators);
        operatorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spOperator.setAdapter(operatorAdapter);

        // Remove button
        btnRemove.setOnClickListener(v -> {
            queryBuilderContainer.removeView(conditionView);
            updateBuiltQuery();
        });

        // Add to container
        queryBuilderContainer.addView(conditionView);

        // Store condition
        QueryCondition condition = new QueryCondition(spField, spOperator, etValue);
        queryConditions.add(condition);

        updateBuiltQuery();
        */
    }

    /**
     * Clear query builder
     */
    private void clearQueryBuilder() {
        queryBuilderContainer.removeAllViews();
        queryConditions.clear();
        updateBuiltQuery();
    }

    /**
     * Update built query display
     */
    private void updateBuiltQuery() {
        StringBuilder query = new StringBuilder();

        // Add main query
        String mainQuery = buildBooleanQuery();
        if (!mainQuery.isEmpty()) {
            query.append(mainQuery);
        }

        // Add date range
        if (dateFromMillis > 0 || dateToMillis > 0) {
            if (query.length() > 0) query.append(" AND ");
            query.append("date:[");
            query.append(dateFromMillis > 0 ? dateFromMillis : "*");
            query.append(" TO ");
            query.append(dateToMillis > 0 ? dateToMillis : "*");
            query.append("]");
        }

        // Add folders
        if (!selectedFolderIds.isEmpty()) {
            if (query.length() > 0) query.append(" AND ");
            query.append("folder_id:(");
            for (int i = 0; i < selectedFolderIds.size(); i++) {
                if (i > 0) query.append(" OR ");
                query.append(selectedFolderIds.get(i));
            }
            query.append(")");
        }

        // Add tags
        if (!selectedTags.isEmpty()) {
            if (query.length() > 0) query.append(" AND ");
            query.append("tags:(");
            for (int i = 0; i < selectedTags.size(); i++) {
                if (i > 0) query.append(" OR ");
                query.append("\"" + selectedTags.get(i) + "\"");
            }
            query.append(")");
        }

        // Add file types
        List<String> fileTypes = getSelectedFileTypes();
        if (!fileTypes.isEmpty() && fileTypes.size() < 3) {
            if (query.length() > 0) query.append(" AND ");
            query.append("file_type:(");
            for (int i = 0; i < fileTypes.size(); i++) {
                if (i > 0) query.append(" OR ");
                query.append(fileTypes.get(i));
            }
            query.append(")");
        }

        // Add favorites filter
        if (cbFavoritesOnly.isChecked()) {
            if (query.length() > 0) query.append(" AND ");
            query.append("favorite:true");
        }

        // Display built query
        if (query.length() == 0) {
            tvBuiltQuery.setText("No search criteria specified");
            tvBuiltQuery.setTextColor(getResources().getColor(android.R.color.darker_gray));
        } else {
            tvBuiltQuery.setText(query.toString());
            tvBuiltQuery.setTextColor(getResources().getColor(android.R.color.black));
        }
    }

    // ================================
    // 8. Search Tips and Examples
    // ================================

    /**
     * Show search tips dialog
     */
    private void showSearchTips() {
        // Show tips in a simple alert dialog instead of bottom sheet
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Advanced Search Tips")
            .setMessage(getSearchTipsText())
            .setPositiveButton("Got It", null)
            .show();

        /* Uncomment when bottom_sheet_search_tips.xml is created
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_search_tips, null);

        TextView tvTips = view.findViewById(R.id.tvTips);
        tvTips.setText(getSearchTipsText());

        Button btnClose = view.findViewById(R.id.btnClose);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.setContentView(view);
        dialog.show();
        */
    }

    /**
     * Get search tips text
     */
    private String getSearchTipsText() {
        return "Advanced Search Tips\n\n" +
            "Boolean Operators:\n" +
            "• AND - All terms must match\n" +
            "  Example: invoice AND paid\n\n" +
            "• OR - Any term can match\n" +
            "  Example: receipt OR invoice\n\n" +
            "• NOT - Exclude terms\n" +
            "  Example: document NOT draft\n\n" +
            "Wildcards:\n" +
            "• * matches any characters\n" +
            "  Example: doc* (matches doc, document, etc.)\n\n" +
            "• ? matches single character\n" +
            "  Example: do? (matches dog, doc, etc.)\n\n" +
            "Phrase Search:\n" +
            "• Use quotes for exact phrases\n" +
            "  Example: \"annual report 2024\"\n\n" +
            "Regular Expressions:\n" +
            "• Enable regex mode for patterns\n" +
            "  Example: \\d{4}-\\d{2}-\\d{2} (dates)\n" +
            "  Example: [A-Z]{2,} (uppercase words)\n\n" +
            "Field-Specific Search:\n" +
            "• document_name:invoice\n" +
            "• ocr_text:contract\n" +
            "• description:important\n\n" +
            "Date Filters:\n" +
            "• Use date pickers for temporal filtering\n" +
            "• Supports date ranges (from-to)\n\n" +
            "Tips:\n" +
            "• Combine multiple criteria for precision\n" +
            "• Use visual query builder for complex searches\n" +
            "• Test regex patterns before searching\n" +
            "• Save frequently used searches";
    }

    // ================================
    // Action Buttons
    // ================================

    /**
     * Setup action buttons
     */
    private void setupActionButtons() {
        btnSearch.setOnClickListener(v -> performSearch());
        btnReset.setOnClickListener(v -> resetForm());
        btnShowTips.setOnClickListener(v -> showSearchTips());
    }

    /**
     * Perform search
     */
    private void performSearch() {
        // Build search filter
        DocumentSearchManager.SearchFilter filter = buildSearchFilter();

        // Get search query
        String query = buildBooleanQuery();

        if (query.isEmpty()) {
            Toast.makeText(this, "Please enter search criteria", Toast.LENGTH_SHORT).show();
            return;
        }

        // Apply regex mode
        if (cbRegexMode.isChecked()) {
            // Validate regex first
            try {
                Pattern.compile(query);
            } catch (PatternSyntaxException e) {
                Toast.makeText(this, "Invalid regex pattern", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Launch SearchResultsActivity
        SearchResultsActivity.start(this, query, filter);

        Log.d(TAG, "Performing advanced search: " + query);
    }

    /**
     * Build search filter from form
     */
    private DocumentSearchManager.SearchFilter buildSearchFilter() {
        DocumentSearchManager.SearchFilter filter = new DocumentSearchManager.SearchFilter();

        // Date range
        filter.dateFrom = dateFromMillis;
        filter.dateTo = dateToMillis;

        // Folder filter (use first folder if multiple selected)
        if (!selectedFolderIds.isEmpty()) {
            filter.folderId = selectedFolderIds.get(0);
        }

        // File type (use first type if multiple selected)
        List<String> types = getSelectedFileTypes();
        if (!types.isEmpty()) {
            filter.fileType = types.get(0);
        }

        // Favorites
        filter.favoritesOnly = cbFavoritesOnly.isChecked();

        // File size range
        long[] sizeRange = getFileSizeRange();
        filter.minFileSize = sizeRange[0];
        filter.maxFileSize = sizeRange[1];

        // Page count range
        int[] pageRange = getPageCountRange();
        filter.minPageCount = pageRange[0];
        filter.maxPageCount = pageRange[1];

        // Case sensitivity
        // filter.caseSensitive = cbCaseSensitive.isChecked();

        return filter;
    }

    /**
     * Reset form
     */
    private void resetForm() {
        // Clear text fields
        etSearchQuery.setText("");
        etDocumentName.setText("");
        etOcrText.setText("");
        etDescription.setText("");

        // Reset operator
        spSearchOperator.setSelection(0);

        // Reset checkboxes
        cbRegexMode.setChecked(false);
        cbCaseSensitive.setChecked(false);
        cbPDF.setChecked(false);
        cbImage.setChecked(false);
        cbOther.setChecked(false);
        cbFavoritesOnly.setChecked(false);

        // Clear date range
        clearDateRange();

        // Clear folders and tags
        selectedFolderIds.clear();
        selectedTags.clear();
        chipGroupFolders.removeAllViews();
        chipGroupTags.removeAllViews();

        // Reset sliders
        sliderFileSize.setValues(0f, 100f);
        sliderPageCount.setValues(1f, 100f);

        // Clear query builder
        clearQueryBuilder();

        Toast.makeText(this, "Form reset", Toast.LENGTH_SHORT).show();
    }

    // ================================
    // Menu
    // ================================

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // TODO: Create menu_advanced_search.xml
        // getMenuInflater().inflate(R.menu.menu_advanced_search, menu);
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
        else if (item.getItemId() == R.id.action_test_regex) {
            testRegexPattern();
            return true;
        } else if (item.getItemId() == R.id.action_save_search) {
            saveCurrentSearch();
            return true;
        } else if (item.getItemId() == R.id.action_load_search) {
            loadSavedSearch();
            return true;
        }
        */
        return super.onOptionsItemSelected(item);
    }

    /**
     * Save current search
     */
    private void saveCurrentSearch() {
        // TODO: Implement save search functionality
        Toast.makeText(this, "Save search - Implementation pending", Toast.LENGTH_SHORT).show();
    }

    /**
     * Load saved search
     */
    private void loadSavedSearch() {
        // TODO: Implement load search functionality
        Toast.makeText(this, "Load search - Implementation pending", Toast.LENGTH_SHORT).show();
    }

    // ================================
    // Helper Classes
    // ================================

    /**
     * Query Condition
     */
    private static class QueryCondition {
        Spinner fieldSpinner;
        Spinner operatorSpinner;
        EditText valueEdit;

        QueryCondition(Spinner field, Spinner operator, EditText value) {
            this.fieldSpinner = field;
            this.operatorSpinner = operator;
            this.valueEdit = value;
        }

        String getField() {
            return fieldSpinner.getSelectedItem().toString();
        }

        String getOperator() {
            return operatorSpinner.getSelectedItem().toString();
        }

        String getValue() {
            return valueEdit.getText().toString();
        }
    }

    /**
     * Static launch method
     */
    public static void start(Context context) {
        Intent intent = new Intent(context, AdvancedSearchActivity.class);
        context.startActivity(intent);
    }
}

