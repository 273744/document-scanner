# 📱 Install APK - Quick Guide

## ✅ Installation Scripts Created

I've created **TWO scripts** to install the APK on your emulator:

### Option 1: Batch File (Recommended)
**File:** `install_app.bat`

**How to use:**
1. Make sure your **emulator is running**
2. Double-click `install_app.bat` in the project folder
3. Watch the installation progress
4. App will launch automatically when done

---

### Option 2: PowerShell Script
**File:** `install_app.ps1`

**How to use:**
1. Right-click `install_app.ps1`
2. Select "Run with PowerShell"
3. Or from PowerShell:
   ```powershell
   cd C:\Users\273744\AndroidStudioProjects\MyApplication
   .\install_app.ps1
   ```

---

## 📍 Where Are These Files?

Both scripts are located in:
```
C:\Users\273744\AndroidStudioProjects\MyApplication\
├── install_app.bat  ← Double-click this!
├── install_app.ps1
├── build_app.bat
└── app\build\outputs\apk\debug\app-debug.apk
```

---

## 🚀 What Each Script Does

1. ✅ Checks if ADB is available
2. ✅ Checks if APK file exists
3. ✅ Lists connected devices (emulator)
4. ✅ Installs APK with `-r` flag (replaces existing)
5. ✅ Launches the app automatically
6. ✅ Shows success/failure message

---

## 📱 Before Installing - Start Your Emulator

### If emulator is not running:

**Option A: From Android Studio**
1. Click **Tools → Device Manager**
2. Click the ▶️ play button on your emulator
3. Wait for it to fully boot (home screen visible)

**Option B: From Command Line**
```bash
emulator -avd Medium_Phone_API_36.1
```

---

## 🧪 Manual Installation (If Scripts Fail)

### Step 1: Check Devices
```bash
C:\Users\273744\AppData\Local\Android\Sdk\platform-tools\adb.exe devices
```

You should see something like:
```
List of devices attached
emulator-5554    device
```

### Step 2: Install APK
```bash
C:\Users\273744\AppData\Local\Android\Sdk\platform-tools\adb.exe install -r "C:\Users\273744\AndroidStudioProjects\MyApplication\app\build\outputs\apk\debug\app-debug.apk"
```

### Step 3: Launch App
```bash
C:\Users\273744\AppData\Local\Android\Sdk\platform-tools\adb.exe shell am start -n com.srikanth.docscanner/.MainActivity
```

---

## ✅ Verify Installation

After installation, check:

1. **App icon appears** on emulator home screen or app drawer
2. **App name:** "Document Scanner"
3. **Package:** com.srikanth.docscanner

---

## 🐛 Troubleshooting

### "No devices/emulators found"
**Solution:** Start your emulator first
```bash
# List available emulators
emulator -list-avds

# Start emulator
emulator -avd Medium_Phone_API_36.1
```

### "INSTALL_FAILED_UPDATE_INCOMPATIBLE"
**Solution:** Uninstall old version first
```bash
adb uninstall com.srikanth.docscanner
# Then install again
```

### "APK not found"
**Solution:** Build the project first
- Run `build_app.bat` or
- Build in Android Studio

### "adb not found"
**Solution:** Use full path or add to PATH
```bash
# Full path (always works)
C:\Users\273744\AppData\Local\Android\Sdk\platform-tools\adb.exe

# Or add to PATH environment variable
```

---

## 📊 Installation Output

**Successful installation looks like:**
```
Performing Streamed Install
Success
```

**Then app launches with:**
```
Starting: Intent { cmp=com.srikanth.docscanner/.MainActivity }
```

---

## 🎯 Quick Start After Installation

1. ✅ App is installed and launched
2. ✅ OpenCV initialized (check logcat)
3. ✅ Tap "Capture Document"
4. ✅ Take a photo
5. ✅ Auto-detection runs
6. ✅ Corners detected
7. ✅ Crop and save!

---

## 📝 Next Steps

After successful installation:

1. **Test the capture fix:**
   - Tap "Capture Document"
   - Take a photo
   - Verify auto-detection works
   - Check quality score badge

2. **Test edit functionality:**
   - Go to Gallery
   - Open a document
   - Tap Edit
   - Choose "Crop & Adjust" or "Enhance & Filter"

3. **Test multi-page:**
   - Capture first page
   - Tap "Add More Pages"
   - Capture additional pages
   - Generate PDF

---

## ✅ Summary

**To install the APK:**

### Easiest Way:
1. Start emulator
2. Double-click `install_app.bat`
3. Wait for success message
4. App launches automatically

**That's it!** 🎉

---

**Files Created:**
- ✅ `install_app.bat` - Batch file for installation
- ✅ `install_app.ps1` - PowerShell script for installation
- ✅ `INSTALL_GUIDE.md` - This guide

**Location:**
```
C:\Users\273744\AndroidStudioProjects\MyApplication\
```

**Status:** ✅ Ready to install!

