# 🧪 Testing the Optimized Build

## Quick Start

Since some dependencies were removed/changed, here's how to test the optimized build:

## Option 1: Test with Most Features (Recommended)

Keep these dependencies uncommented in `build.gradle.kts`:

```kotlin
// Keep AR (competitive advantage)
implementation("com.google.ar:core:1.42.0")

// Keep ML Kit Document Scanner (replaces OpenCV)
implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0-beta1")

// Keep Latin OCR only (covers 80% of users)
implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.0")

// Add ONE additional language if needed:
// implementation("com.google.mlkit:text-recognition-devanagari:16.0.0")
```

**Result:** ~30 MB per device

## Option 2: Minimal Build (Smallest APK)

Comment out AR in `build.gradle.kts`:

```kotlin
// OPTIONAL: Comment out to save 10 MB
// implementation("com.google.ar:core:1.42.0")
```

**Result:** ~20 MB per device

## Required Code Changes

### 1. Replace OpenCV Calls

**Before (OpenCV):**
```java
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

// In onCreate:
OpenCVLoader.initDebug();
```

**After (ML Kit Document Scanner):**
```java
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult;

// Use ML Kit Document Scanner
GmsDocumentScannerOptions options = new GmsDocumentScannerOptions.Builder()
    .setGalleryImportAllowed(true)
    .setPageLimit(10)
    .setResultFormats(
        GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
        GmsDocumentScannerOptions.RESULT_FORMAT_PDF
    )
    .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
    .build();

GmsDocumentScanner scanner = GmsDocumentScanning.getClient(options);
```

### 2. Replace iText PDF with PDFBox

**Before (iText):**
```java
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
```

**After (PDFBox):**
```java
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
```

**Or use Android's built-in PDF API:**
```java
import android.graphics.pdf.PdfDocument;

PdfDocument pdfDocument = new PdfDocument();
PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
PdfDocument.Page page = pdfDocument.startPage(pageInfo);
// Draw on page.getCanvas()
pdfDocument.finishPage(page);
```

### 3. Google Drive API (Use REST instead of SDK)

**Before (Heavy SDK):**
```java
import com.google.api.services.drive.Drive;
```

**After (Retrofit REST API):**
```java
// Define Retrofit interface
interface DriveApi {
    @POST("upload/drive/v3/files")
    Call<DriveFile> uploadFile(@Body RequestBody file);
    
    @GET("drive/v3/files")
    Call<FileList> listFiles();
}

// Use Retrofit
Retrofit retrofit = new Retrofit.Builder()
    .baseUrl("https://www.googleapis.com/")
    .addConverterFactory(GsonConverterFactory.create())
    .build();

DriveApi api = retrofit.create(DriveApi.class);
```

## Build Commands

### Build Debug APK (for testing)
```bash
./gradlew assembleDebug
```

### Build Release APK (optimized)
```bash
./gradlew assembleRelease
```

### Build App Bundle (for Play Store)
```bash
./gradlew bundleRelease
```

### Check APK Size
```bash
# Windows PowerShell
ls app\build\outputs\apk\release\ | Select-Object Name, Length

# Or in File Explorer:
# Navigate to app/build/outputs/apk/release/
# Check file sizes
```

## Testing Checklist

- [ ] App builds successfully
- [ ] Document scanning works (using ML Kit)
- [ ] OCR text extraction works
- [ ] PDF generation works (using PDFBox or PdfDocument)
- [ ] Camera preview displays correctly
- [ ] AR features work (if enabled)
- [ ] Cloud sync works (using Retrofit)
- [ ] Search functionality works
- [ ] All activities launch without crashes
- [ ] ProGuard doesn't break anything in release build

## Common Issues & Solutions

### Issue 1: OpenCV Classes Not Found
**Error:** `Cannot resolve symbol 'OpenCVLoader'`

**Solution:** Replace with ML Kit Document Scanner (see code changes above)

### Issue 2: iText Classes Not Found
**Error:** `Cannot resolve symbol 'PdfDocument' (iText)`

**Solution:** Use Android's PdfDocument or PDFBox (see code changes above)

### Issue 3: Google Drive SDK Classes Not Found
**Error:** `Cannot resolve symbol 'Drive'`

**Solution:** Use Retrofit with Drive REST API (see code changes above)

### Issue 4: ProGuard Breaks Release Build
**Error:** App crashes in release but works in debug

**Solution:** Add ProGuard rules to `proguard-rules.pro`:
```proguard
# Keep ML Kit classes
-keep class com.google.mlkit.** { *; }

# Keep PDFBox classes
-keep class com.tom_roush.pdfbox.** { *; }

# Keep ARCore classes
-keep class com.google.ar.core.** { *; }

# Keep Room entities
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
```

## Verification Steps

1. **Build succeeds without errors**
   ```bash
   ./gradlew assembleRelease
   # Should see: BUILD SUCCESSFUL
   ```

2. **APK size is reduced**
   ```bash
   # Before: ~120 MB
   # After: ~40-47 MB (debug)
   # After: ~35-40 MB (release with ProGuard)
   ```

3. **All features work**
   - Install APK on device
   - Test document scanning
   - Test OCR
   - Test PDF generation
   - Test search
   - Test cloud sync

4. **No crashes in release build**
   - ProGuard is working correctly
   - All reflection-based code has rules

## Rollback Plan

If you encounter issues, you can rollback:

1. **Restore OpenCV**
   ```kotlin
   implementation("org.opencv:opencv:4.9.0")
   ```

2. **Restore iText**
   ```kotlin
   implementation("com.itextpdf:itext7-core:7.2.5")
   ```

3. **Restore Google Drive SDK**
   ```kotlin
   implementation("com.google.apis:google-api-services-drive:v3-rev20220815-2.0.0")
   ```

4. **Disable ProGuard temporarily**
   ```kotlin
   release {
       isMinifyEnabled = false
       isShrinkResources = false
   }
   ```

## Gradual Migration Strategy

Instead of all at once, migrate features gradually:

### Phase 1: Remove unused dependencies (safe)
- Remove additional OCR languages
- Remove Compose (if not used)
- Remove RxJava

**Test:** Build and verify everything works

### Phase 2: Replace heavy libraries
- Replace OpenCV → ML Kit
- Replace iText → PDFBox

**Test:** Build and verify scanning/PDF works

### Phase 3: Optimize cloud sync
- Replace Drive SDK → REST API
- Simplify Dropbox integration

**Test:** Build and verify cloud sync works

### Phase 4: Enable optimizations
- Enable ProGuard
- Enable resource shrinking
- Enable APK splits

**Test:** Release build and verify no crashes

## Success Criteria

✅ Build completes without errors  
✅ APK size < 50 MB  
✅ All features functional  
✅ Release build doesn't crash  
✅ ProGuard mappings saved  
✅ Performance is same or better  

## Next Steps After Testing

1. **If everything works:**
   - Commit changes
   - Tag release
   - Build App Bundle
   - Upload to Play Store (internal test)
   - Monitor crash reports

2. **If issues found:**
   - Review error logs
   - Add ProGuard rules
   - Fix code compatibility
   - Re-test
   - Iterate

---

**Status:** Ready for testing  
**Risk Level:** Medium (removed major dependencies)  
**Rollback:** Easy (restore dependencies)  
**Recommended:** Test on multiple devices before production

