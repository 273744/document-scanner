# 📄 How to Access Generated PDFs in Emulator

## 🎯 Quick Access Methods

Your generated PDFs are stored in the app's private storage. Here are multiple ways to access them:

---

## Method 1: Using ADB Commands (Recommended)

### **List All PDFs:**
```powershell
# Open PowerShell and run:
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb shell "ls -lh /storage/emulated/0/Android/data/com.example.myapplication/files/DocumentScanner/*.pdf"
```

This will show you all PDF files with their sizes and timestamps.

### **Pull PDF to Your Computer:**
```powershell
# Pull a specific PDF
& $adb pull /storage/emulated/0/Android/data/com.example.myapplication/files/DocumentScanner/DOC_2025-11-15-14-30-45.pdf ./

# Or pull all PDFs
& $adb pull /storage/emulated/0/Android/data/com.example.myapplication/files/DocumentScanner/ ./DocumentScanner/
```

The PDFs will be downloaded to your current directory (or the DocumentScanner folder).

### **View PDF List with Details:**
```powershell
# See all files in the DocumentScanner directory
& $adb shell "ls -la /storage/emulated/0/Android/data/com.example.myapplication/files/DocumentScanner/"
```

---

## Method 2: Using Android Device File Explorer

### **Steps:**
1. **Open Android Studio**
2. **Click "Device File Explorer"** (bottom-right tab)
   - Or go to: View → Tool Windows → Device File Explorer
3. **Navigate to:**
   ```
   /storage/emulated/0/Android/data/com.example.myapplication/files/DocumentScanner/
   ```
4. **Find your PDFs:**
   - Files named: `DOC_2025-11-15-HH-mm-ss.pdf`
5. **Right-click on a PDF** → **Save As...**
6. **Choose location** on your computer
7. **Open with PDF viewer**

---

## Method 3: Using ADB Shell (Interactive)

### **Enter Shell:**
```powershell
& $adb shell
```

### **Navigate to PDFs:**
```bash
cd /storage/emulated/0/Android/data/com.example.myapplication/files/DocumentScanner/
ls -lh
```

### **View File Info:**
```bash
file DOC_*.pdf
stat DOC_*.pdf
```

### **Exit Shell:**
```bash
exit
```

---

## Method 4: Copy to Accessible Location

### **Copy PDF to Downloads (Visible in Emulator):**
```powershell
# Copy PDF to Downloads folder (accessible via Files app)
& $adb shell "cp /storage/emulated/0/Android/data/com.example.myapplication/files/DocumentScanner/DOC_*.pdf /storage/emulated/0/Download/"
```

Now you can access it from the emulator's **Files** or **Downloads** app!

---

## Method 5: View in Emulator's File Manager

### **Steps:**
1. **Open emulator**
2. **Open "Files" app** (built-in file manager)
3. **Tap "Browse"** at bottom
4. **Tap "Downloads"** or "Internal storage"
5. **Navigate to:**
   ```
   Android → data → com.example.myapplication → files → DocumentScanner
   ```
6. **Tap on PDF** to open with PDF viewer

**Note:** You may need to copy PDFs to Downloads first (see Method 4)

---

## 📋 Complete ADB Commands Reference

### **Check if Emulator is Connected:**
```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb devices
```

### **List All Generated Files:**
```powershell
# Images and PDFs
& $adb shell "ls -lh /storage/emulated/0/Android/data/com.example.myapplication/files/DocumentScanner/"
```

### **Count Files:**
```powershell
# Count PDFs
& $adb shell "ls /storage/emulated/0/Android/data/com.example.myapplication/files/DocumentScanner/*.pdf | wc -l"

# Count images
& $adb shell "ls /storage/emulated/0/Android/data/com.example.myapplication/files/DocumentScanner/*.jpg | wc -l"
```

### **Pull Latest PDF:**
```powershell
# Get the most recently created PDF
& $adb shell "ls -t /storage/emulated/0/Android/data/com.example.myapplication/files/DocumentScanner/*.pdf | head -1"

# Pull it
$latestPdf = & $adb shell "ls -t /storage/emulated/0/Android/data/com.example.myapplication/files/DocumentScanner/*.pdf | head -1"
& $adb pull $latestPdf.Trim() ./latest.pdf
```

### **Pull All PDFs at Once:**
```powershell
# Create local directory
New-Item -ItemType Directory -Force -Path "./GeneratedPDFs"

# Pull all PDFs
& $adb pull /storage/emulated/0/Android/data/com.example.myapplication/files/DocumentScanner/ ./GeneratedPDFs/
```

---

## 🔍 Finding Specific PDFs

### **By Date:**
```powershell
# Find PDFs from today
& $adb shell "ls -lh /storage/emulated/0/Android/data/com.example.myapplication/files/DocumentScanner/DOC_2025-11-15*.pdf"
```

### **By Time Range:**
```powershell
# Find PDFs created in the last hour
& $adb shell "find /storage/emulated/0/Android/data/com.example.myapplication/files/DocumentScanner/ -name '*.pdf' -mmin -60"
```

### **Latest 5 PDFs:**
```powershell
& $adb shell "ls -t /storage/emulated/0/Android/data/com.example.myapplication/files/DocumentScanner/*.pdf | head -5"
```

---

## 📱 Open PDF in Emulator

### **Option 1: Copy to Downloads & Open:**
```powershell
# Copy latest PDF to Downloads
$latestPdf = & $adb shell "ls -t /storage/emulated/0/Android/data/com.example.myapplication/files/DocumentScanner/*.pdf | head -1"
& $adb shell "cp $($latestPdf.Trim()) /storage/emulated/0/Download/"

# Open Files app
& $adb shell am start -a android.intent.action.VIEW -t "application/pdf"
```

### **Option 2: Open with Intent:**
```powershell
# Open PDF directly
$pdfPath = "/storage/emulated/0/Android/data/com.example.myapplication/files/DocumentScanner/DOC_2025-11-15-14-30-45.pdf"
& $adb shell am start -a android.intent.action.VIEW -d "file://$pdfPath" -t "application/pdf"
```

---

## 🔧 Troubleshooting

### **Issue: "Permission denied"**
**Solution:** The app storage is private. Use `adb` with proper permissions:
```powershell
& $adb shell "run-as com.example.myapplication ls files/DocumentScanner/"
```

### **Issue: "No such file or directory"**
**Solution:** Check if PDFs were actually generated:
```powershell
# Check logcat for PDF generation
& $adb logcat -s "PdfGenerator:*" -v brief | Select-Object -Last 20
```

### **Issue: Can't find adb**
**Solution:** ADB is in Android SDK platform-tools:
```powershell
# Verify path
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
Test-Path $adb
```

---

## 💡 Pro Tips

### **1. Create a Quick Access Script:**

Save this as `getPDFs.ps1`:
```powershell
# getPDFs.ps1
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$remotePath = "/storage/emulated/0/Android/data/com.example.myapplication/files/DocumentScanner"
$localPath = "./MyPDFs"

Write-Host "📄 Fetching PDFs from emulator..." -ForegroundColor Cyan

# Create local directory
New-Item -ItemType Directory -Force -Path $localPath | Out-Null

# Pull all PDFs
& $adb pull "$remotePath/" "$localPath/"

# List downloaded files
Write-Host "`n✅ PDFs downloaded to: $localPath" -ForegroundColor Green
Get-ChildItem $localPath -Filter *.pdf | ForEach-Object {
    Write-Host "   📄 $($_.Name) ($([math]::Round($_.Length/1KB, 2)) KB)"
}
```

Run with: `.\getPDFs.ps1`

### **2. Auto-Open Latest PDF:**
```powershell
# Pull and open latest PDF
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$latestPdf = & $adb shell "ls -t /storage/emulated/0/Android/data/com.example.myapplication/files/DocumentScanner/*.pdf | head -1"
& $adb pull $latestPdf.Trim() ./latest.pdf
Start-Process ./latest.pdf
```

### **3. Batch Download All Files:**
```powershell
# Download everything (images + PDFs)
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb pull /storage/emulated/0/Android/data/com.example.myapplication/files/DocumentScanner/ ./AllDocuments/
```

---

## 📊 Verify PDF Generation

### **Check if PDFs Exist:**
```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$pdfCount = & $adb shell "ls /storage/emulated/0/Android/data/com.example.myapplication/files/DocumentScanner/*.pdf 2>/dev/null | wc -l"
if ($pdfCount -gt 0) {
    Write-Host "✅ Found $pdfCount PDF(s)" -ForegroundColor Green
} else {
    Write-Host "❌ No PDFs found. Generate one first!" -ForegroundColor Red
}
```

### **View PDF Details:**
```powershell
& $adb shell "ls -lh /storage/emulated/0/Android/data/com.example.myapplication/files/DocumentScanner/*.pdf" | ForEach-Object {
    Write-Host $_ -ForegroundColor Cyan
}
```

---

## 🎯 Quick Start Guide

### **To Access Your First PDF:**

1. **Generate a PDF in the app first:**
   - Capture an image
   - Tap "Enhance & Generate PDF"
   - Apply a filter
   - Tap "PDF" button
   - Wait for success message

2. **Open PowerShell and run:**
   ```powershell
   $adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
   
   # List PDFs
   & $adb shell "ls -lh /storage/emulated/0/Android/data/com.example.myapplication/files/DocumentScanner/*.pdf"
   
   # Pull to current directory
   & $adb pull /storage/emulated/0/Android/data/com.example.myapplication/files/DocumentScanner/ ./MyPDFs/
   
   # Open the folder
   explorer ./MyPDFs/
   ```

3. **Open the PDF** with your favorite PDF viewer!

---

## ✅ Summary

**Storage Location:**
```
/storage/emulated/0/Android/data/com.example.myapplication/files/DocumentScanner/
```

**File Format:**
- PDFs: `DOC_2025-11-15-HH-mm-ss.pdf`
- Images: `DOC_2025-11-15-HH-mm-ss.jpg`
- Enhanced: `ENHANCED_2025-11-15-HH-mm-ss.jpg`
- Cropped: `CROPPED_2025-11-15-HH-mm-ss.jpg`

**Best Method:**
1. Use **ADB pull** to download to computer
2. Or use **Device File Explorer** in Android Studio
3. Or copy to **Downloads** folder in emulator

---

📄✨ **Now you can easily access all your generated PDFs!** ✨📄

