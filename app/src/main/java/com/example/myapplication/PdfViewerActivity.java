package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.IOException;

/**
 * PdfViewerActivity - Built-in PDF viewer using Android's PdfRenderer
 * Displays PDF pages as images with navigation controls
 */
public class PdfViewerActivity extends AppCompatActivity {

    private static final String TAG = "PdfViewerActivity";
    private static final String EXTRA_PDF_PATH = "pdf_path";

    // UI Components
    private ImageView ivPdfPage;
    private TextView tvPageInfo;
    private ImageButton btnPrevious;
    private ImageButton btnNext;
    private MaterialButton btnShare;
    private MaterialButton btnClose;

    // PDF Renderer
    private PdfRenderer pdfRenderer;
    private PdfRenderer.Page currentPage;
    private ParcelFileDescriptor fileDescriptor;

    // State
    private int currentPageIndex = 0;
    private int totalPages = 0;
    private String pdfPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);

        // Get PDF path from intent
        pdfPath = getIntent().getStringExtra(EXTRA_PDF_PATH);
        if (pdfPath == null || pdfPath.isEmpty()) {
            Toast.makeText(this, "No PDF file specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        initializeViews();

        // Open PDF
        openPdf();

        // Setup controls
        setupControls();
    }

    /**
     * Initialize UI components
     */
    private void initializeViews() {
        ivPdfPage = findViewById(R.id.ivPdfPage);
        tvPageInfo = findViewById(R.id.tvPageInfo);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);
        btnShare = findViewById(R.id.btnShare);
        btnClose = findViewById(R.id.btnClose);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("PDF Viewer");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * Open and render PDF
     */
    private void openPdf() {
        try {
            File pdfFile = new File(pdfPath);
            if (!pdfFile.exists()) {
                Toast.makeText(this, "PDF file not found", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Open file descriptor
            fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);

            // Create PDF renderer
            pdfRenderer = new PdfRenderer(fileDescriptor);
            totalPages = pdfRenderer.getPageCount();

            // Show first page
            showPage(0);

        } catch (IOException e) {
            Log.e(TAG, "Error opening PDF", e);
            Toast.makeText(this, "Error opening PDF: " + e.getMessage(),
                Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /**
     * Show specific page
     */
    private void showPage(int pageIndex) {
        if (pdfRenderer == null || pageIndex < 0 || pageIndex >= totalPages) {
            return;
        }

        // Close previous page if open
        if (currentPage != null) {
            currentPage.close();
        }

        // Open new page
        currentPage = pdfRenderer.openPage(pageIndex);

        // Create bitmap for rendering
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = (int) (width * ((float) currentPage.getHeight() / currentPage.getWidth()));

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        // Render page to bitmap (white background)
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

        // Display bitmap
        ivPdfPage.setImageBitmap(bitmap);

        // Update page info
        currentPageIndex = pageIndex;
        updatePageInfo();
        updateNavigationButtons();
    }

    /**
     * Update page info text
     */
    private void updatePageInfo() {
        tvPageInfo.setText(String.format("Page %d of %d", currentPageIndex + 1, totalPages));
    }

    /**
     * Update navigation button states
     */
    private void updateNavigationButtons() {
        btnPrevious.setEnabled(currentPageIndex > 0);
        btnNext.setEnabled(currentPageIndex < totalPages - 1);

        btnPrevious.setAlpha(currentPageIndex > 0 ? 1.0f : 0.3f);
        btnNext.setAlpha(currentPageIndex < totalPages - 1 ? 1.0f : 0.3f);
    }

    /**
     * Setup control buttons
     */
    private void setupControls() {
        // Previous page
        btnPrevious.setOnClickListener(v -> {
            if (currentPageIndex > 0) {
                showPage(currentPageIndex - 1);
            }
        });

        // Next page
        btnNext.setOnClickListener(v -> {
            if (currentPageIndex < totalPages - 1) {
                showPage(currentPageIndex + 1);
            }
        });

        // Share PDF
        btnShare.setOnClickListener(v -> sharePdf());

        // Close
        btnClose.setOnClickListener(v -> finish());
    }

    /**
     * Share PDF file
     */
    private void sharePdf() {
        try {
            File pdfFile = new File(pdfPath);
            Uri pdfUri = androidx.core.content.FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    pdfFile
            );

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share PDF"));

        } catch (Exception e) {
            Log.e(TAG, "Error sharing PDF", e);
            Toast.makeText(this, "Error sharing PDF", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closePdf();
    }

    /**
     * Close PDF and free resources
     */
    private void closePdf() {
        try {
            if (currentPage != null) {
                currentPage.close();
                currentPage = null;
            }
            if (pdfRenderer != null) {
                pdfRenderer.close();
                pdfRenderer = null;
            }
            if (fileDescriptor != null) {
                fileDescriptor.close();
                fileDescriptor = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing PDF", e);
        }
    }

    /**
     * Static method to start this activity
     */
    public static void start(Context context, String pdfPath) {
        Intent intent = new Intent(context, PdfViewerActivity.class);
        intent.putExtra(EXTRA_PDF_PATH, pdfPath);
        context.startActivity(intent);
    }
}

