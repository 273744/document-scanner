# iText PDF Library Setup - Complete Guide

## ✅ iText PDF Dependencies Added!

**Library:** iText 7.2.5  
**Purpose:** PDF generation and manipulation  
**Status:** Dependencies added and configured  
**Android Compatibility:** Min SDK 24+  

---

## 📦 What Was Added

### 1. **iText Dependencies** (build.gradle.kts)
```kotlin
// iText Core library for creating and editing PDFs
implementation("com.itextpdf:itext7-core:7.2.5")

// iText Layout module for advanced layout features
implementation("com.itextpdf:layout:7.2.5")

// iText Kernel module for low-level PDF operations
implementation("com.itextpdf:kernel:7.2.5")

// iText IO module for font and image handling
implementation("com.itextpdf:io:7.2.5")
```

### 2. **ProGuard Rules** (proguard-rules.pro)
```proguard
# Keep all iText classes
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**

# Keep iText annotations
-keepattributes *Annotation*

# Keep bouncy castle and SLF4J
-keep class org.bouncycastle.** { *; }
-keep class org.slf4j.** { *; }

# Prevent reflection issues
-keepclassmembers class * {
    @com.itextpdf.kernel.pdf.* <methods>;
}
```

---

## 🎯 iText Modules Explained

### **itext7-core** (Main Library)
- Core PDF functionality
- PDF document creation
- PDF manipulation
- Base classes and interfaces

### **layout** (Layout Module)
- High-level layout API
- Paragraphs, tables, lists
- Images and shapes
- Automatic text wrapping

### **kernel** (Kernel Module)
- Low-level PDF operations
- PDF structure manipulation
- Page operations
- Annotations and forms

### **io** (I/O Module)
- Font handling
- Image processing
- External file operations
- Resource management

---

## 💻 Basic Usage Examples

### Example 1: Create Simple PDF
```java
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

public class SimplePdfGenerator {
    
    public static void createPdf(String dest) throws Exception {
        // Initialize PDF writer
        PdfWriter writer = new PdfWriter(dest);
        
        // Initialize PDF document
        PdfDocument pdf = new PdfDocument(writer);
        
        // Initialize document
        Document document = new Document(pdf);
        
        // Add content
        document.add(new Paragraph("Hello, PDF World!"));
        
        // Close document
        document.close();
    }
}
```

### Example 2: Add Image to PDF
```java
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.layout.element.Image;

public void addImageToPdf(Document document, String imagePath) throws Exception {
    // Create image from file
    Image img = new Image(ImageDataFactory.create(imagePath));
    
    // Scale to fit page
    img.setAutoScale(true);
    
    // Add to document
    document.add(img);
}
```

### Example 3: Create Multi-Page PDF from Images
```java
import com.itextpdf.kernel.geom.PageSize;

public static void createPdfFromImages(String dest, List<String> imagePaths) throws Exception {
    PdfWriter writer = new PdfWriter(dest);
    PdfDocument pdf = new PdfDocument(writer);
    Document document = new Document(pdf, PageSize.A4);
    
    // Remove margins for full-page images
    document.setMargins(0, 0, 0, 0);
    
    for (String imagePath : imagePaths) {
        // Create image
        Image img = new Image(ImageDataFactory.create(imagePath));
        
        // Scale to fit page
        img.scaleToFit(PageSize.A4.getWidth(), PageSize.A4.getHeight());
        
        // Center on page
        img.setFixedPosition(0, 0);
        
        // Add image
        document.add(img);
        
        // Add new page for next image (except last)
        if (imagePaths.indexOf(imagePath) < imagePaths.size() - 1) {
            document.add(new AreaBreak());
        }
    }
    
    document.close();
}
```

### Example 4: Add Text and Formatting
```java
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.kernel.colors.ColorConstants;

public void addFormattedText(Document document) {
    // Title
    Paragraph title = new Paragraph("Document Scanner")
        .setFontSize(20)
        .setBold()
        .setTextAlignment(TextAlignment.CENTER);
    document.add(title);
    
    // Subtitle with color
    Paragraph subtitle = new Paragraph("Scanned on: " + new Date())
        .setFontSize(12)
        .setFontColor(ColorConstants.GRAY)
        .setTextAlignment(TextAlignment.CENTER);
    document.add(subtitle);
}
```

---

## 📱 Android-Specific Implementation

### PdfGenerator.java (Helper Class)
```java
package com.example.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.property.TextAlignment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PdfGenerator {
    
    private static final String TAG = "PdfGenerator";
    
    /**
     * Generate PDF from list of image files
     */
    public static String generatePdfFromImages(Context context, 
                                              List<String> imagePaths,
                                              String outputDir) {
        try {
            // Generate output filename
            String timestamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US)
                    .format(new Date());
            String filename = "DOC_" + timestamp + ".pdf";
            File outputFile = new File(outputDir, filename);
            
            // Create PDF writer
            PdfWriter writer = new PdfWriter(outputFile);
            
            // Create PDF document
            PdfDocument pdf = new PdfDocument(writer);
            
            // Create document with A4 page size
            Document document = new Document(pdf, PageSize.A4);
            document.setMargins(0, 0, 0, 0);
            
            // Add title page (optional)
            addTitlePage(document);
            document.add(new AreaBreak());
            
            // Add images
            for (int i = 0; i < imagePaths.size(); i++) {
                String imagePath = imagePaths.get(i);
                
                // Load bitmap
                Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                if (bitmap == null) {
                    Log.w(TAG, "Failed to load image: " + imagePath);
                    continue;
                }
                
                // Convert bitmap to byte array
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
                byte[] imageBytes = stream.toByteArray();
                
                // Create iText image
                ImageData imageData = ImageDataFactory.create(imageBytes);
                Image img = new Image(imageData);
                
                // Scale to fit page
                float pageWidth = PageSize.A4.getWidth();
                float pageHeight = PageSize.A4.getHeight();
                img.scaleToFit(pageWidth, pageHeight);
                
                // Center image
                img.setHorizontalAlignment(com.itextpdf.layout.property.HorizontalAlignment.CENTER);
                
                // Add image
                document.add(img);
                
                // Add page break (except for last image)
                if (i < imagePaths.size() - 1) {
                    document.add(new AreaBreak());
                }
                
                // Clean up
                stream.close();
                bitmap.recycle();
            }
            
            // Close document
            document.close();
            
            Log.d(TAG, "PDF created: " + outputFile.getAbsolutePath());
            return outputFile.getAbsolutePath();
            
        } catch (Exception e) {
            Log.e(TAG, "Error generating PDF", e);
            return null;
        }
    }
    
    /**
     * Add title page to PDF
     */
    private static void addTitlePage(Document document) {
        // Add some spacing
        document.add(new Paragraph("\n\n\n\n"));
        
        // Title
        Paragraph title = new Paragraph("Document Scanner")
                .setFontSize(24)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER);
        document.add(title);
        
        // Date
        String dateStr = new SimpleDateFormat("MMMM dd, yyyy", Locale.US)
                .format(new Date());
        Paragraph date = new Paragraph("Generated on: " + dateStr)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(date);
    }
    
    /**
     * Generate PDF from single image
     */
    public static String generatePdfFromImage(Context context,
                                             String imagePath,
                                             String outputDir) {
        List<String> images = new ArrayList<>();
        images.add(imagePath);
        return generatePdfFromImages(context, images, outputDir);
    }
}
```

---

## 🔗 Integration with PreviewActivity

### Add to PreviewActivity.java:
```java
// In generatePdf() method
private void generatePdf() {
    showProgress(true, "Generating PDF...");
    
    new Thread(() -> {
        try {
            // Get current image path
            String imagePath = this.imagePath;
            
            // Generate PDF
            String pdfPath = PdfGenerator.generatePdfFromImage(
                this,
                imagePath,
                outputDirectory.getAbsolutePath()
            );
            
            if (pdfPath != null) {
                runOnUiThread(() -> {
                    showProgress(false, null);
                    
                    new MaterialAlertDialogBuilder(this)
                        .setTitle("PDF Generated")
                        .setMessage("PDF saved successfully!")
                        .setPositiveButton("Open", (d, w) -> {
                            openPdf(pdfPath);
                        })
                        .setNeutralButton("Share", (d, w) -> {
                            sharePdf(pdfPath);
                        })
                        .setNegativeButton("Done", null)
                        .show();
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
```

---

## 📊 Advanced Features

### 1. **Add Page Numbers**
```java
public class PageNumberHandler implements IEventHandler {
    @Override
    public void handleEvent(Event event) {
        PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
        PdfDocument pdfDoc = docEvent.getDocument();
        PdfPage page = docEvent.getPage();
        int pageNumber = pdfDoc.getPageNumber(page);
        
        Canvas canvas = new Canvas(page, page.getPageSize());
        canvas.showTextAligned(
            new Paragraph("Page " + pageNumber),
            page.getPageSize().getWidth() / 2,
            20,
            TextAlignment.CENTER
        );
        canvas.close();
    }
}

// Add to document
pdf.addEventHandler(PdfDocumentEvent.END_PAGE, new PageNumberHandler());
```

### 2. **Add Watermark**
```java
public void addWatermark(PdfDocument pdf, String text) {
    for (int i = 1; i <= pdf.getNumberOfPages(); i++) {
        PdfPage page = pdf.getPage(i);
        PdfCanvas canvas = new PdfCanvas(page);
        
        canvas.saveState();
        canvas.setFillColor(ColorConstants.LIGHT_GRAY);
        canvas.setFontAndSize(PdfFontFactory.createFont(), 60);
        canvas.beginText();
        canvas.setTextMatrix(100, 400);
        canvas.showText(text);
        canvas.endText();
        canvas.restoreState();
    }
}
```

### 3. **Compress PDF**
```java
PdfWriter writer = new PdfWriter(dest, 
    new WriterProperties().setCompressionLevel(CompressionConstants.BEST_COMPRESSION));
```

### 4. **Password Protection**
```java
WriterProperties properties = new WriterProperties()
    .setStandardEncryption(
        "user_password".getBytes(),
        "owner_password".getBytes(),
        EncryptionConstants.ALLOW_PRINTING,
        EncryptionConstants.ENCRYPTION_AES_128
    );

PdfWriter writer = new PdfWriter(dest, properties);
```

---

## 🧪 Testing Checklist

### Basic PDF Generation:
- [ ] Single image to PDF
- [ ] Multiple images to PDF
- [ ] PDF file created successfully
- [ ] PDF opens correctly
- [ ] Images display properly
- [ ] Page size correct (A4)

### Advanced Features:
- [ ] Title page generation
- [ ] Page numbering
- [ ] Text formatting
- [ ] Image scaling
- [ ] Multi-page documents
- [ ] File naming with timestamps

### Error Handling:
- [ ] Invalid image paths
- [ ] Insufficient storage
- [ ] Large image handling
- [ ] Memory management
- [ ] Corrupt images

---

## 🚀 Performance Tips

### 1. **Optimize Images Before PDF:**
```java
// Compress large images
Bitmap compressed = compressBitmap(original, 1024, 1024);
```

### 2. **Use Background Thread:**
```java
// Always generate PDF in background
ExecutorService executor = Executors.newSingleThreadExecutor();
executor.execute(() -> generatePdf());
```

### 3. **Reuse Resources:**
```java
// Reuse PdfWriter for multiple operations
// Close properly to avoid memory leaks
```

### 4. **Monitor Memory:**
```java
// Recycle bitmaps after use
bitmap.recycle();
```

---

## 📝 Required Permissions

### AndroidManifest.xml:
```xml
<!-- Already added in your manifest -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

---

## ✅ Summary

**iText PDF Library provides:**
- ✅ Professional PDF generation
- ✅ Image to PDF conversion
- ✅ Multi-page documents
- ✅ Text and formatting
- ✅ Tables and layouts
- ✅ Annotations and forms
- ✅ Compression and encryption
- ✅ Android compatibility

**Dependencies Added:**
```
✅ itext7-core:7.2.5
✅ layout:7.2.5
✅ kernel:7.2.5
✅ io:7.2.5
✅ ProGuard rules configured
```

**Ready to Use:**
- ✅ Import iText classes
- ✅ Create PDF documents
- ✅ Add images and text
- ✅ Generate from scanned documents
- ✅ Integrate with PreviewActivity

---

## 🎯 Next Steps

1. **Build project** - Sync Gradle dependencies
2. **Create PdfGenerator.java** - Helper class
3. **Update PreviewActivity** - Implement PDF generation
4. **Test PDF creation** - Generate sample PDFs
5. **Add sharing** - Share generated PDFs

---

📄✨ **iText PDF Library ready for document generation!** ✨📄

