# Launch Document Scanner App on Emulator

## 🚀 Quick Launch Instructions

### Method 1: Using the Batch Script (Easiest)
I've created a batch script for you. Just double-click this file:
```
C:\Users\273744\AndroidStudioProjects\MyApplication\install_and_launch.bat
```

It will:
1. Check for connected devices
2. Install the APK
3. Launch the app automatically

---

### Method 2: Manual Commands

Open **Command Prompt** or **PowerShell** and run these commands:

```batch
cd C:\Users\273744\AndroidStudioProjects\MyApplication

# Check connected devices (should show emulator-5554)
adb devices

# Install the app
adb install -r app\build\outputs\apk\debug\app-debug.apk

# Launch the app
adb shell am start -n com.example.myapplication/.MainActivity
```

---

### Method 3: Using Gradle (If emulator API level is detected)

```powershell
cd C:\Users\273744\AndroidStudioProjects\MyApplication
.\gradlew installDebug

# Then launch manually from emulator or use:
adb shell am start -n com.example.myapplication/.MainActivity
```

---

### Method 4: Using Android Studio (Most Reliable)

1. Open Android Studio
2. Make sure the emulator is running (emulator-5554 appears connected)
3. Click the **Run** button (▶️) in the toolbar
4. Select **Medium_Phone_API_36.1** from the device list
5. The app will install and launch automatically

---

## 🔍 Troubleshooting

### Issue: "Unknown API Level" or "Could not find build of variant"

The emulator (emulator-5554) seems to have an API level detection issue. Here's how to fix it:

**Option A: Use ADB directly**
```batch
# Force install regardless of API level check
adb -s emulator-5554 install -r -d app\build\outputs\apk\debug\app-debug.apk

# Launch the app
adb -s emulator-5554 shell am start -n com.example.myapplication/.MainActivity
```

**Option B: Restart the emulator**
1. Close the current emulator
2. Launch "Medium_Phone_API_36.1" from AVD Manager
3. Wait for it to fully boot
4. Try installing again

**Option C: Verify emulator setup**
```batch
# Check emulator API level
adb -s emulator-5554 shell getprop ro.build.version.sdk

# Should return: 36 (for API 36)
```

---

## 📱 What You Should See

Once the app launches on the emulator:

### Main Screen:
✅ **Title:** "Document Scanner" at the top  
✅ **Preview Area:** Large card with "No document captured yet" text  
✅ **Capture Button:** Blue button with camera icon saying "Capture Document"  
✅ **Gallery Button:** Purple/tonal button with gallery icon saying "View Gallery"  

### Testing the App:
1. **Click "Capture Document"**
   - Permission dialog appears
   - Grant camera permission
   - Toast message: "Opening camera to capture document..."

2. **Click "View Gallery"**
   - Toast message: "Opening gallery..."
   - Navigates to gallery screen
   - Shows: "No documents captured yet. Start by capturing your first document!"
   - Back arrow in toolbar returns to main screen

---

## 🎯 Alternative: Run from APK File

If all else fails, you can manually install the APK:

1. **Locate the APK:**
   ```
   C:\Users\273744\AndroidStudioProjects\MyApplication\app\build\outputs\apk\debug\app-debug.apk
   ```

2. **Drag and drop** the APK file onto the running emulator window

3. **Or install via ADB:**
   ```batch
   adb install app\build\outputs\apk\debug\app-debug.apk
   ```

4. **Find the app:**
   - Open app drawer in emulator
   - Look for "Document Scanner" icon
   - Tap to launch

---

## 📋 Current Status

**Emulator Detected:** ✅ emulator-5554  
**APK Built:** ✅ app-debug.apk (19.97 MB)  
**Issue:** Gradle cannot detect emulator API level properly  
**Solution:** Use ADB commands directly or Android Studio Run button  

---

## 🔧 Recommended Solution

**Since you have the emulator running, the easiest way is:**

1. **Open a new Command Prompt window** (not the IDE terminal)

2. **Run these exact commands:**
   ```batch
   cd C:\Users\273744\AndroidStudioProjects\MyApplication
   adb install -r app\build\outputs\apk\debug\app-debug.apk
   adb shell am start -n com.example.myapplication/.MainActivity
   ```

3. **Check your emulator screen** - the app should launch!

---

## 💡 Quick Tips

- The emulator window might be minimized - check your taskbar
- If the emulator is slow, give it a minute to fully boot
- You can also click the app icon in the emulator's app drawer
- Grant camera permission when prompted to test the full flow

---

**The app is ready to run! Just use one of the methods above.** 🎉

The batch script I created (`install_and_launch.bat`) should work if you double-click it from Windows Explorer.

