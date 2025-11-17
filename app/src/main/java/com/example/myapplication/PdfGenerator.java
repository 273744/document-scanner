package com.example.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.CompressionConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfDocumentInfo;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.WriterProperties;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * PDFGenerator - Advanced PDF generation using iText 7
 * Features:
 * - Custom page sizes (A4, Letter, Legal, etc.)
 * - Image scaling and compression
 * - Multi-page PDF support
 * - Metadata (title, author, date)
 * - File size optimization
 * - High-quality output
 */
public class PdfGenerator {

    private static final String TAG = "PdfGenerator";

    // PDF Configuration
    public enum PageSize {
        A4(com.itextpdf.kernel.geom.PageSize.A4),
        LETTER(com.itextpdf.kernel.geom.PageSize.LETTER),
        LEGAL(com.itextpdf.kernel.geom.PageSize.LEGAL),
        A3(com.itextpdf.kernel.geom.PageSize.A3),
        A5(com.itextpdf.kernel.geom.PageSize.A5);

        private final com.itextpdf.kernel.geom.PageSize size;

        PageSize(com.itextpdf.kernel.geom.PageSize size) {
            this.size = size;
        }

        public com.itextpdf.kernel.geom.PageSize getSize() {
            return size;
        }
    }

    // Compression levels
    public enum CompressionLevel {
        NONE(CompressionConstants.NO_COMPRESSION),
        DEFAULT(CompressionConstants.DEFAULT_COMPRESSION),
        BEST_SPEED(CompressionConstants.BEST_SPEED),
        BEST_COMPRESSION(CompressionConstants.BEST_COMPRESSION);

        private final int level;

        CompressionLevel(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }
    }

    // PDF Metadata
    public static class PdfMetadata {
        private String title = "Document Scanner PDF";
        private String author = "Document Scanner App";
        private String subject = "Scanned Document";
        private String creator = "Document Scanner";
        private String keywords = "scan, document, pdf";

        public PdfMetadata() {}

        public PdfMetadata setTitle(String title) {
            this.title = title;
            return this;
        }

        public PdfMetadata setAuthor(String author) {
            this.author = author;
            return this;
        }

        public PdfMetadata setSubject(String subject) {
            this.subject = subject;
            return this;
        }

        public PdfMetadata setCreator(String creator) {
            this.creator = creator;
            return this;
        }

        public PdfMetadata setKeywords(String keywords) {
            this.keywords = keywords;
            return this;
        }
    }

    // PDF Generation Options
    public static class PdfOptions {
        private PageSize pageSize = PageSize.A4;
        private CompressionLevel compressionLevel = CompressionLevel.BEST_COMPRESSION;
        private int imageQuality = 85; // JPEG quality 0-100
        private boolean addTitlePage = false;  // Changed to false - users just want their scanned pages!
        private boolean addPageNumbers = false;
        private PdfMetadata metadata = new PdfMetadata();

        public PdfOptions() {}

        public PdfOptions setPageSize(PageSize pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        public PdfOptions setCompressionLevel(CompressionLevel level) {
            this.compressionLevel = level;
            return this;
        }

        public PdfOptions setImageQuality(int quality) {
            this.imageQuality = Math.max(0, Math.min(100, quality));
            return this;
        }

        public PdfOptions setAddTitlePage(boolean add) {
            this.addTitlePage = add;
            return this;
        }

        public PdfOptions setAddPageNumbers(boolean add) {
            this.addPageNumbers = add;
            return this;
        }

        public PdfOptions setMetadata(PdfMetadata metadata) {
            this.metadata = metadata;
            return this;
        }
    }

    /**
     * Generate PDF from list of image files with default options
     *
     * @param context Android context
     * @param imagePaths List of image file paths
     * @param outputDir Output directory for PDF
     * @return Path to generated PDF file, or null if failed
     */
    public static String generatePdfFromImages(Context context,
                                              List<String> imagePaths,
                                              String outputDir) {
        return generatePdfFromImages(context, imagePaths, outputDir, new PdfOptions());
    }

    /**
     * Generate PDF from list of image files with custom options
     *
     * @param context Android context
     * @param imagePaths List of image file paths
     * @param outputDir Output directory for PDF
     * @param options PDF generation options
     * @return Path to generated PDF file, or null if failed
     */
    public static String generatePdfFromImages(Context context,
                                              List<String> imagePaths,
                                              String outputDir,
                                              PdfOptions options) {
        if (imagePaths == null || imagePaths.isEmpty()) {
            Log.e(TAG, "No images provided for PDF generation");
            return null;
        }

        try {
            // CRITICAL: Log what we received
            Log.d(TAG, "=== PDF GENERATOR CALLED ===");
            Log.d(TAG, "Received " + imagePaths.size() + " image paths:");
            for (int i = 0; i < imagePaths.size(); i++) {
                Log.d(TAG, "  Path[" + i + "]: " + imagePaths.get(i));
                File f = new File(imagePaths.get(i));
                Log.d(TAG, "    Exists: " + f.exists() + ", Size: " + f.length() + " bytes");
            }
            Log.d(TAG, "============================");

            // Generate output filename
            String timestamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US)
                    .format(new Date());
            String filename = "MultiPage_" + timestamp + ".pdf";
            File outputFile = new File(outputDir, filename);

            Log.d(TAG, "Creating PDF: " + outputFile.getAbsolutePath());
            Log.d(TAG, "Options - Page: " + options.pageSize +
                       ", Compression: " + options.compressionLevel +
                       ", Quality: " + options.imageQuality + "%"  +
                       ", TitlePage: " + options.addTitlePage);

            // Create PDF writer with compression
            WriterProperties writerProperties = new WriterProperties()
                    .setCompressionLevel(options.compressionLevel.getLevel());
            PdfWriter writer = new PdfWriter(outputFile.getAbsolutePath(), writerProperties);

            // Create PDF document
            PdfDocument pdf = new PdfDocument(writer);

            // Add metadata
            addMetadata(pdf, options.metadata);

            // Create document with specified page size
            Document document = new Document(pdf, options.pageSize.getSize());
            document.setMargins(0, 0, 0, 0);

            // Add title page (optional)
            if (options.addTitlePage) {
                addTitlePage(document, options.metadata);
                document.add(new AreaBreak());
            }

            // Add images
            int successCount = 0;
            for (int i = 0; i < imagePaths.size(); i++) {
                String imagePath = imagePaths.get(i);

                if (addImageToDocument(document, imagePath, options)) {
                    successCount++;

                    // Add page break (except for last image)
                    if (i < imagePaths.size() - 1) {
                        document.add(new AreaBreak());
                    }
                }
            }

            // Close document
            document.close();

            if (successCount == 0) {
                Log.e(TAG, "No images were added to PDF");
                outputFile.delete();
                return null;
            }

            // Log file size
            long fileSize = outputFile.length();
            String fileSizeStr = fileSize < 1024 * 1024 ?
                    String.format("%.2f KB", fileSize / 1024.0) :
                    String.format("%.2f MB", fileSize / (1024.0 * 1024.0));

            Log.d(TAG, "PDF created successfully: " + outputFile.getAbsolutePath());
            Log.d(TAG, "Pages: " + successCount + ", Size: " + fileSizeStr);

            return outputFile.getAbsolutePath();

        } catch (Exception e) {
            Log.e(TAG, "Error generating PDF", e);
            return null;
        }
    }

    /**
     * Add metadata to PDF document
     */
    private static void addMetadata(PdfDocument pdf, PdfMetadata metadata) {
        try {
            PdfDocumentInfo info = pdf.getDocumentInfo();
            info.setTitle(metadata.title);
            info.setAuthor(metadata.author);
            info.setSubject(metadata.subject);
            info.setCreator(metadata.creator);
            info.setKeywords(metadata.keywords);

            Log.d(TAG, "PDF metadata added: " + metadata.title);
        } catch (Exception e) {
            Log.e(TAG, "Error adding metadata", e);
        }
    }

    /**
     * Add image to document with quality options
     */
    private static boolean addImageToDocument(Document document, String imagePath, PdfOptions options) {
        try {
            // Load bitmap
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            if (bitmap == null) {
                Log.w(TAG, "Failed to load image: " + imagePath);
                return false;
            }

            // Compress bitmap with specified quality
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, options.imageQuality, stream);
            byte[] imageBytes = stream.toByteArray();

            // Log compression result
            long originalSize = bitmap.getByteCount();
            long compressedSize = imageBytes.length;
            float compressionRatio = (1 - (float)compressedSize / originalSize) * 100;
            Log.d(TAG, String.format("Image compressed: %.1f%% reduction (%.2f KB -> %.2f KB)",
                    compressionRatio, originalSize / 1024.0, compressedSize / 1024.0));

            // Create iText image
            ImageData imageData = ImageDataFactory.create(imageBytes);
            Image img = new Image(imageData);

            // Scale to fit page with margins
            com.itextpdf.kernel.geom.PageSize pageSize = options.pageSize.getSize();
            float pageWidth = pageSize.getWidth();
            float pageHeight = pageSize.getHeight();
            img.scaleToFit(pageWidth, pageHeight);

            // Add image
            document.add(img);

            // Clean up
            stream.close();
            bitmap.recycle();

            Log.d(TAG, "Added image to PDF: " + new File(imagePath).getName());
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error adding image to PDF: " + imagePath, e);
            return false;
        }
    }

    /**
     * Add title page to PDF with metadata
     */
    private static void addTitlePage(Document document, PdfMetadata metadata) {
        try {
            // Add some spacing
            document.add(new Paragraph("\n\n\n\n"));

            // Title
            Paragraph title = new Paragraph(metadata.title)
                    .setFontSize(24)
                    .setBold();
            document.add(title);

            // Author
            if (metadata.author != null && !metadata.author.isEmpty()) {
                Paragraph author = new Paragraph("By: " + metadata.author)
                        .setFontSize(14);
                document.add(author);
            }

            // Date
            String dateStr = new SimpleDateFormat("MMMM dd, yyyy", Locale.US)
                    .format(new Date());
            Paragraph date = new Paragraph("Generated on: " + dateStr)
                    .setFontSize(12);
            document.add(date);

            // Subject
            if (metadata.subject != null && !metadata.subject.isEmpty()) {
                document.add(new Paragraph("\n"));
                Paragraph subject = new Paragraph(metadata.subject)
                        .setFontSize(12)
                        .setItalic();
                document.add(subject);
            }

            // Note
            document.add(new Paragraph("\n\n"));
            Paragraph note = new Paragraph("This document contains scanned pages")
                    .setFontSize(10)
                    .setItalic();
            document.add(note);

        } catch (Exception e) {
            Log.e(TAG, "Error adding title page", e);
        }
    }

    /**
     * Generate PDF from single image with default options
     *
     * @param context Android context
     * @param imagePath Path to image file
     * @param outputDir Output directory for PDF
     * @return Path to generated PDF file, or null if failed
     */
    public static String generatePdfFromImage(Context context,
                                             String imagePath,
                                             String outputDir) {
        List<String> images = new ArrayList<>();
        images.add(imagePath);
        return generatePdfFromImages(context, images, outputDir);
    }

    /**
     * Generate PDF from single image with custom options
     *
     * @param context Android context
     * @param imagePath Path to image file
     * @param outputDir Output directory for PDF
     * @param options PDF generation options
     * @return Path to generated PDF file, or null if failed
     */
    public static String generatePdfFromImage(Context context,
                                             String imagePath,
                                             String outputDir,
                                             PdfOptions options) {
        List<String> images = new ArrayList<>();
        images.add(imagePath);
        return generatePdfFromImages(context, images, outputDir, options);
    }

    /**
     * Get formatted file size string
     */
    public static String getFileSizeString(String filePath) {
        try {
            File file = new File(filePath);
            long fileSize = file.length();

            if (fileSize < 1024) {
                return fileSize + " B";
            } else if (fileSize < 1024 * 1024) {
                return String.format(Locale.US, "%.2f KB", fileSize / 1024.0);
            } else {
                return String.format(Locale.US, "%.2f MB", fileSize / (1024.0 * 1024.0));
            }
        } catch (Exception e) {
            return "Unknown";
        }
    }
}

