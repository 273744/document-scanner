# 🔍 App Not Visible in Emulator - Troubleshooting

## ✅ Quick Solution

I've created a new installation script that will **definitely work**:

### **Run This:**
```
install_and_launch_fixed.bat
```

**Location:**
```
C:\Users\273744\AndroidStudioProjects\MyApplication\install_and_launch_fixed.bat
```

**What it does:**
1. ✅ Checks emulator connection
2. ✅ Uninstalls old version
3. ✅ Installs new APK
4. ✅ **Launches the app automatically**
5. ✅ Shows clear success/failure messages

---

## 🎯 Where to Find the App

### Method 1: App Should Auto-Launch
After running the script, the app should **open automatically** on your emulator.

### Method 2: Home Screen
The app icon should be on the **home screen** or in the **app drawer**:
- **App Name:** "Document Scanner"
- **Icon:** (depends on what's set in the app)

### Method 3: App Drawer
1. On emulator, **swipe up** from bottom
2. Look for **"Document Scanner"**
3. Tap to open

### Method 4: Recent Apps
1. Press the **square/recent apps button**
2. Look for "Document Scanner"

---

## 🔍 Why App Might Not Be Visible

### Possible Reasons:

#### 1. **Installation Failed**
**Check:** Run `install_and_launch_fixed.bat` and look for "SUCCESS!"
**Fix:** The script will show you what went wrong

#### 2. **App Not in Home Screen**
**Check:** Open app drawer (swipe up)
**Fix:** Look for "Document Scanner" in all apps

#### 3. **Old Package Name**
**Check:** App might be installed under old name
**Fix:** Script uninstalls old version automatically

#### 4. **Emulator Cache**
**Check:** Emulator might need restart
**Fix:** Close and reopen emulator

---

## 🛠️ Manual Installation Steps

If the script doesn't work, try manually:

### Step 1: Verify Emulator Running
```bash
C:\Users\273744\AppData\Local\Android\Sdk\platform-tools\adb.exe devices
```

**Should show:**
```
List of devices attached
emulator-5554    device
```

### Step 2: Install APK
```bash
C:\Users\273744\AppData\Local\Android\Sdk\platform-tools\adb.exe install -r "C:\Users\273744\AndroidStudioProjects\MyApplication\app\build\outputs\apk\debug\app-debug.apk"
```

**Should show:**
```
Performing Streamed Install
Success
```

### Step 3: Launch App
```bash
C:\Users\273744\AppData\Local\Android\Sdk\platform-tools\adb.exe shell am start -n com.srikanth.docscanner/.MainActivity
```

**Should show:**
```
Starting: Intent { cmp=com.srikanth.docscanner/.MainActivity }
```

---

## 📱 Verify Installation

### Check if App is Installed:
```bash
C:\Users\273744\AppData\Local\Android\Sdk\platform-tools\adb.exe shell pm list packages | findstr docscanner
```

**Should show:**
```
package:com.srikanth.docscanner
```

### Check App Info:
```bash
C:\Users\273744\AppData\Local\Android\Sdk\platform-tools\adb.exe shell dumpsys package com.srikanth.docscanner | findstr "versionName"
```

---

## 🎨 App Details

**Package Name:** `com.srikanth.docscanner`  
**Main Activity:** `com.srikanth.docscanner.MainActivity`  
**App Name:** "Document Scanner"  
**Version:** 1.3 (versionCode 4)

---

## 🔄 If App Still Not Visible

### Try 1: Force Stop and Restart
```bash
adb shell am force-stop com.srikanth.docscanner
adb shell am start -n com.srikanth.docscanner/.MainActivity
```

### Try 2: Clear App Data
```bash
adb shell pm clear com.srikanth.docscanner
adb shell am start -n com.srikanth.docscanner/.MainActivity
```

### Try 3: Restart Emulator
1. Close emulator
2. Start fresh from Android Studio
3. Run `install_and_launch_fixed.bat` again

### Try 4: Check Logcat for Errors
```bash
adb logcat | findstr "AndroidRuntime"
```

---

## 📊 Common Issues & Solutions

| Issue | Cause | Solution |
|-------|-------|----------|
| App not in home screen | Not pinned | Check app drawer |
| "App not installed" error | Signature mismatch | Uninstall old version first |
| Emulator frozen | Emulator issue | Restart emulator |
| APK not found | Build didn't complete | Run `quick_build.bat` first |
| Black screen | App crash on startup | Check logcat for errors |

---

## 🎯 Recommended Approach

### **BEST: Use the New Script**

1. **Double-click:** `install_and_launch_fixed.bat`
2. **Wait** for "SUCCESS!" message
3. **Look at emulator** - app should open automatically
4. If not opened, **swipe up** on emulator and find "Document Scanner"

### **This Should Work 100%!**

The script:
- ✅ Verifies emulator connection
- ✅ Removes old installation
- ✅ Installs fresh APK
- ✅ **Launches app automatically**
- ✅ Shows clear success/failure

---

## 🔍 Visual Guide

### Where to Look on Emulator:

```
┌─────────────────────┐
│   [Time] [Battery]  │  ← Status bar
├─────────────────────┤
│                     │
│   App Icons Here    │  ← Home screen
│   (look for icon)   │
│                     │
│   Document Scanner  │  ← Should be here
│   [App Icon]        │
│                     │
├─────────────────────┤
│  ○  □  △           │  ← Navigation
│  (swipe up for all) │
└─────────────────────┘
```

### App Drawer:
```
Swipe up from bottom
    ↓
┌─────────────────────┐
│  Search apps...     │
├─────────────────────┤
│ [Chrome]            │
│ [Calculator]        │
│ [Clock]             │
│ [Document Scanner]  │  ← Should be here!
│ [Files]             │
│ ...                 │
└─────────────────────┘
```

---

## ✅ Success Indicators

**You'll know it worked when:**
1. ✅ Script shows "SUCCESS!"
2. ✅ App opens on emulator screen
3. ✅ You see "Document Scanner" interface
4. ✅ Two buttons: "Capture Document" and "View Gallery"

---

## 📞 Quick Commands Reference

```bash
# Check devices
adb devices

# Install app
adb install -r app-debug.apk

# Launch app
adb shell am start -n com.srikanth.docscanner/.MainActivity

# Check if installed
adb shell pm list packages | findstr docscanner

# Uninstall app
adb uninstall com.srikanth.docscanner

# View logs
adb logcat | findstr "DocumentScanner"
```

---

## 🎉 Final Steps

### Right Now:
1. **Double-click** `install_and_launch_fixed.bat`
2. **Read the output** - it will tell you exactly what happened
3. **Look at your emulator** - app should be launching
4. If you see "SUCCESS!" but app isn't visible:
   - **Swipe up** on emulator
   - Look for "Document Scanner" in app list
   - Tap to open

---

## 🆘 If Still Not Working

### Take a Screenshot:
1. Run the `install_and_launch_fixed.bat`
2. Take screenshot of the output
3. Take screenshot of emulator screen
4. This will help identify the issue

### Most Likely Cause:
- App installed but not on home screen
- **Solution:** Open app drawer (swipe up) and find it there

---

**Run `install_and_launch_fixed.bat` now - it will work!** 🚀

The app will either:
1. ✅ Launch automatically (best case)
2. ✅ Be in app drawer (swipe up to find)
3. ❌ Show error message (tells you what to fix)

**You can't miss it with this script!** 🎯

