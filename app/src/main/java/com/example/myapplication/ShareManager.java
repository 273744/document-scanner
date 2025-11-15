package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * ShareManager - Comprehensive document sharing functionality
 * Features:
 * - Share via email, WhatsApp, Drive, etc.
 * - Share individual images or PDFs
 * - Export to different formats
 * - Batch sharing multiple documents
 * - Secure file sharing with FileProvider
 */
public class ShareManager {

    private static final String TAG = "ShareManager";
    private Context context;

    // Export formats
    public enum ExportFormat {
        PDF,
        JPG,
        PNG
    }

    // Share target apps
    public enum ShareTarget {
        ALL,           // Show all available apps
        EMAIL,         // Email apps only
        WHATSAPP,      // WhatsApp
        DRIVE,         // Google Drive
        MESSAGING      // SMS/Messaging apps
    }

    /**
     * Constructor
     */
    public ShareManager(Context context) {
        this.context = context.getApplicationContext();
    }

    // ================== Single File Sharing ==================

    /**
     * Share a single PDF file
     * @param pdfFile PDF file to share
     * @param target Share target (email, whatsapp, etc.)
     */
    public void sharePDF(File pdfFile, ShareTarget target) {
        if (!pdfFile.exists()) {
            showError("PDF file not found");
            return;
        }

        try {
            Uri fileUri = getFileUri(pdfFile);
            Intent shareIntent = createShareIntent("application/pdf", fileUri, target);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Document: " + pdfFile.getName());
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Sharing scanned document");

            startShareIntent(shareIntent, "Share PDF");

        } catch (Exception e) {
            showError("Error sharing PDF: " + e.getMessage());
        }
    }

    /**
     * Share a single image file
     * @param imageFile Image file to share
     * @param target Share target
     */
    public void shareImage(File imageFile, ShareTarget target) {
        if (!imageFile.exists()) {
            showError("Image file not found");
            return;
        }

        try {
            Uri fileUri = getFileUri(imageFile);
            Intent shareIntent = createShareIntent("image/*", fileUri, target);
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Sharing scanned image");

            startShareIntent(shareIntent, "Share Image");

        } catch (Exception e) {
            showError("Error sharing image: " + e.getMessage());
        }
    }

    /**
     * Share a file by path
     * @param filePath Path to file
     * @param target Share target
     */
    public void shareFile(String filePath, ShareTarget target) {
        File file = new File(filePath);

        if (!file.exists()) {
            showError("File not found");
            return;
        }

        // Determine file type
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".pdf")) {
            sharePDF(file, target);
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") ||
                   fileName.endsWith(".png")) {
            shareImage(file, target);
        } else {
            // Generic file sharing
            shareGenericFile(file, target);
        }
    }

    /**
     * Share a generic file
     */
    private void shareGenericFile(File file, ShareTarget target) {
        try {
            Uri fileUri = getFileUri(file);
            Intent shareIntent = createShareIntent("*/*", fileUri, target);
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Sharing file: " + file.getName());

            startShareIntent(shareIntent, "Share File");

        } catch (Exception e) {
            showError("Error sharing file: " + e.getMessage());
        }
    }

    // ================== Batch Sharing ==================

    /**
     * Share multiple files
     * @param filePaths List of file paths to share
     * @param target Share target
     */
    public void shareMultipleFiles(List<String> filePaths, ShareTarget target) {
        if (filePaths == null || filePaths.isEmpty()) {
            showError("No files to share");
            return;
        }

        try {
            ArrayList<Uri> fileUris = new ArrayList<>();
            String mimeType = "*/*";
            boolean allPdfs = true;
            boolean allImages = true;

            for (String path : filePaths) {
                File file = new File(path);
                if (file.exists()) {
                    fileUris.add(getFileUri(file));

                    // Check file types
                    String fileName = file.getName().toLowerCase();
                    if (!fileName.endsWith(".pdf")) allPdfs = false;
                    if (!fileName.endsWith(".jpg") && !fileName.endsWith(".jpeg") &&
                        !fileName.endsWith(".png")) allImages = false;
                }
            }

            if (fileUris.isEmpty()) {
                showError("No valid files found");
                return;
            }

            // Determine MIME type
            if (allPdfs) {
                mimeType = "application/pdf";
            } else if (allImages) {
                mimeType = "image/*";
            }

            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
            shareIntent.setType(mimeType);
            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Sharing " + fileUris.size() + " documents");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Sharing multiple scanned documents");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            applyShareTarget(shareIntent, target);
            startShareIntent(shareIntent, "Share Multiple Files");

        } catch (Exception e) {
            showError("Error sharing multiple files: " + e.getMessage());
        }
    }

    /**
     * Share multiple files as a single PDF
     * @param imagePaths List of image paths to combine
     * @param outputFileName Output PDF filename
     * @param target Share target
     */
    public void shareMultipleImagesAsPDF(List<String> imagePaths, String outputFileName,
                                         ShareTarget target) {
        if (imagePaths == null || imagePaths.isEmpty()) {
            showError("No images to share");
            return;
        }

        try {
            // Create combined PDF
            File pdfFile = createPDFFromImages(imagePaths, outputFileName);

            if (pdfFile != null && pdfFile.exists()) {
                sharePDF(pdfFile, target);
            } else {
                showError("Failed to create PDF");
            }

        } catch (Exception e) {
            showError("Error creating PDF: " + e.getMessage());
        }
    }

    // ================== Export and Convert ==================

    /**
     * Export file to different format
     * @param sourceFile Source file
     * @param format Target format
     * @param callback Callback with exported file
     */
    public void exportToFormat(File sourceFile, ExportFormat format, ExportCallback callback) {
        if (!sourceFile.exists()) {
            callback.onError("Source file not found");
            return;
        }

        new Thread(() -> {
            try {
                File exportedFile = null;
                String sourceName = sourceFile.getName();
                String baseName = sourceName.substring(0, sourceName.lastIndexOf('.'));

                switch (format) {
                    case PDF:
                        // Convert image to PDF
                        if (isImageFile(sourceFile)) {
                            exportedFile = convertImageToPDF(sourceFile, baseName + ".pdf");
                        } else {
                            exportedFile = sourceFile; // Already PDF
                        }
                        break;

                    case JPG:
                        // Convert to JPG
                        if (isPDFFile(sourceFile)) {
                            callback.onError("PDF to JPG conversion not yet implemented");
                            return;
                        } else {
                            exportedFile = convertToJPG(sourceFile, baseName + ".jpg");
                        }
                        break;

                    case PNG:
                        // Convert to PNG
                        if (isPDFFile(sourceFile)) {
                            callback.onError("PDF to PNG conversion not yet implemented");
                            return;
                        } else {
                            exportedFile = convertToPNG(sourceFile, baseName + ".png");
                        }
                        break;
                }

                if (exportedFile != null && exportedFile.exists()) {
                    File finalFile = exportedFile;
                    ((android.app.Activity) context).runOnUiThread(() ->
                        callback.onSuccess(finalFile));
                } else {
                    callback.onError("Export failed");
                }

            } catch (Exception e) {
                callback.onError("Export error: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Export callback interface
     */
    public interface ExportCallback {
        void onSuccess(File exportedFile);
        void onError(String error);
    }

    // ================== Specific App Sharing ==================

    /**
     * Share via Email
     */
    public void shareViaEmail(File file, String subject, String body, String[] recipients) {
        try {
            Uri fileUri = getFileUri(file);

            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType(getMimeType(file));
            emailIntent.putExtra(Intent.EXTRA_EMAIL, recipients);
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
            emailIntent.putExtra(Intent.EXTRA_TEXT, body);
            emailIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            emailIntent.setPackage("com.google.android.gm"); // Gmail

            try {
                context.startActivity(Intent.createChooser(emailIntent, "Send Email"));
            } catch (Exception e) {
                // Gmail not installed, try generic email
                emailIntent.setPackage(null);
                context.startActivity(Intent.createChooser(emailIntent, "Send Email"));
            }

        } catch (Exception e) {
            showError("Error sharing via email: " + e.getMessage());
        }
    }

    /**
     * Share via WhatsApp
     */
    public void shareViaWhatsApp(File file, String message) {
        try {
            Uri fileUri = getFileUri(file);

            Intent whatsappIntent = new Intent(Intent.ACTION_SEND);
            whatsappIntent.setType(getMimeType(file));
            whatsappIntent.setPackage("com.whatsapp");
            whatsappIntent.putExtra(Intent.EXTRA_TEXT, message);
            whatsappIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            whatsappIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            context.startActivity(whatsappIntent);

        } catch (Exception e) {
            showError("WhatsApp not installed or error sharing: " + e.getMessage());
        }
    }

    /**
     * Share via Google Drive
     */
    public void shareViaGoogleDrive(File file) {
        try {
            Uri fileUri = getFileUri(file);

            Intent driveIntent = new Intent(Intent.ACTION_SEND);
            driveIntent.setType(getMimeType(file));
            driveIntent.setPackage("com.google.android.apps.docs");
            driveIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            driveIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            context.startActivity(driveIntent);

        } catch (Exception e) {
            showError("Google Drive not installed or error sharing: " + e.getMessage());
        }
    }

    // ================== Helper Methods ==================

    /**
     * Create share intent with MIME type and URI
     */
    private Intent createShareIntent(String mimeType, Uri fileUri, ShareTarget target) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType(mimeType);
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        applyShareTarget(shareIntent, target);

        return shareIntent;
    }

    /**
     * Apply share target to intent
     */
    private void applyShareTarget(Intent intent, ShareTarget target) {
        switch (target) {
            case EMAIL:
                // Will show email apps in chooser
                intent.setType("message/rfc822");
                break;

            case WHATSAPP:
                intent.setPackage("com.whatsapp");
                break;

            case DRIVE:
                intent.setPackage("com.google.android.apps.docs");
                break;

            case MESSAGING:
                intent.setPackage("com.google.android.apps.messaging");
                break;

            case ALL:
            default:
                // No specific package - show all
                break;
        }
    }

    /**
     * Start share intent with chooser
     */
    private void startShareIntent(Intent shareIntent, String title) {
        try {
            Intent chooserIntent = Intent.createChooser(shareIntent, title);
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(chooserIntent);
        } catch (Exception e) {
            showError("No app available to share: " + e.getMessage());
        }
    }

    /**
     * Get file URI using FileProvider
     */
    private Uri getFileUri(File file) {
        return FileProvider.getUriForFile(
            context,
            context.getPackageName() + ".fileprovider",
            file
        );
    }

    /**
     * Get MIME type for file
     */
    private String getMimeType(File file) {
        String fileName = file.getName().toLowerCase();

        if (fileName.endsWith(".pdf")) {
            return "application/pdf";
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".png")) {
            return "image/png";
        } else {
            return "*/*";
        }
    }

    /**
     * Check if file is an image
     */
    private boolean isImageFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg") ||
               name.endsWith(".png") || name.endsWith(".bmp");
    }

    /**
     * Check if file is a PDF
     */
    private boolean isPDFFile(File file) {
        return file.getName().toLowerCase().endsWith(".pdf");
    }

    /**
     * Create PDF from multiple images
     */
    private File createPDFFromImages(List<String> imagePaths, String outputFileName) {
        try {
            File outputDir = new File(context.getFilesDir(), "exports");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            File pdfFile = new File(outputDir, outputFileName);

            PdfWriter writer = new PdfWriter(pdfFile);
            PdfDocument pdfDocument = new PdfDocument(writer);
            Document document = new Document(pdfDocument, PageSize.A4);
            document.setMargins(0, 0, 0, 0);

            for (String imagePath : imagePaths) {
                File imageFile = new File(imagePath);
                if (!imageFile.exists()) continue;

                // Add image to PDF
                Image pdfImage = new Image(ImageDataFactory.create(imagePath));

                // Scale to fit page
                float pageWidth = PageSize.A4.getWidth();
                float pageHeight = PageSize.A4.getHeight();
                pdfImage.scaleToFit(pageWidth, pageHeight);

                // Center image
                pdfImage.setFixedPosition(
                    (pageWidth - pdfImage.getImageScaledWidth()) / 2,
                    (pageHeight - pdfImage.getImageScaledHeight()) / 2
                );

                // Add new page for each image
                if (pdfDocument.getNumberOfPages() > 0) {
                    pdfDocument.addNewPage();
                }

                document.add(pdfImage);
            }

            document.close();
            return pdfFile;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Convert single image to PDF
     */
    private File convertImageToPDF(File imageFile, String outputFileName) {
        try {
            File outputDir = new File(context.getFilesDir(), "exports");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            File pdfFile = new File(outputDir, outputFileName);

            PdfWriter writer = new PdfWriter(pdfFile);
            PdfDocument pdfDocument = new PdfDocument(writer);
            Document document = new Document(pdfDocument, PageSize.A4);
            document.setMargins(0, 0, 0, 0);

            Image pdfImage = new Image(ImageDataFactory.create(imageFile.getAbsolutePath()));

            // Scale to fit page
            float pageWidth = PageSize.A4.getWidth();
            float pageHeight = PageSize.A4.getHeight();
            pdfImage.scaleToFit(pageWidth, pageHeight);

            document.add(pdfImage);
            document.close();

            return pdfFile;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Convert image to JPG
     */
    private File convertToJPG(File imageFile, String outputFileName) {
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());

            File outputDir = new File(context.getFilesDir(), "exports");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            File jpgFile = new File(outputDir, outputFileName);
            FileOutputStream out = new FileOutputStream(jpgFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();

            return jpgFile;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Convert image to PNG
     */
    private File convertToPNG(File imageFile, String outputFileName) {
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());

            File outputDir = new File(context.getFilesDir(), "exports");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            File pngFile = new File(outputDir, outputFileName);
            FileOutputStream out = new FileOutputStream(pngFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();

            return pngFile;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Show error toast
     */
    private void showError(String message) {
        if (context instanceof android.app.Activity) {
            ((android.app.Activity) context).runOnUiThread(() ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
        }
    }

    // ================== Convenience Methods ==================

    /**
     * Quick share - automatically determines best sharing method
     */
    public void quickShare(String filePath) {
        shareFile(filePath, ShareTarget.ALL);
    }

    /**
     * Share with text message
     */
    public void shareWithMessage(File file, String message, ShareTarget target) {
        try {
            Uri fileUri = getFileUri(file);
            Intent shareIntent = createShareIntent(getMimeType(file), fileUri, target);
            shareIntent.putExtra(Intent.EXTRA_TEXT, message);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, file.getName());

            startShareIntent(shareIntent, "Share Document");

        } catch (Exception e) {
            showError("Error sharing: " + e.getMessage());
        }
    }
}

