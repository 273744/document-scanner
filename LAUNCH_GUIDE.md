# Quick App Launcher - Document Scanner

## ✅ App Successfully Launched!

**Status:** Running on emulator  
**Date:** November 14, 2025  
**Activity:** MainActivity  

---

## 🚀 Quick Launch Commands

### PowerShell (Recommended):
```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
cd C:\Users\273744\AndroidStudioProjects\MyApplication

# Install and launch
& $adb install -r app\build\outputs\apk\debug\app-debug.apk
& $adb shell am start -n com.example.myapplication/.MainActivity
```

### One-Line Quick Launch:
```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r "C:\Users\273744\AndroidStudioProjects\MyApplication\app\build\outputs\apk\debug\app-debug.apk" ; & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell am start -n com.example.myapplication/.MainActivity
```

---

## 📱 What's Running

Your Document Scanner app is now running on the emulator with:

### ✅ Main Screen Features:
- **Title:** "Document Scanner"
- **Preview Card:** Empty state with placeholder
- **Capture Button:** Opens camera (with permission request)
- **Gallery Button:** Opens gallery view

### ✅ Available Actions:
1. **Capture Document** - Request camera permission → Show toast
2. **View Gallery** - Navigate to gallery screen
3. **Back Navigation** - Return from gallery to main

---

## 🔧 Useful Commands

### Check if app is running:
```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell "ps | grep myapplication"
```

### View app logs:
```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" logcat -s "myapplication:*" "AndroidRuntime:E"
```

### Stop the app:
```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell am force-stop com.example.myapplication
```

### Uninstall the app:
```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" uninstall com.example.myapplication
```

### Restart the app:
```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell am start -n com.example.myapplication/.MainActivity
```

---

## 🎯 Testing the App

### 1. Test Capture Button:
- Tap "Capture Document"
- Permission dialog should appear
- Grant camera permission
- Toast message: "Opening camera to capture document..."

### 2. Test Gallery Button:
- Tap "View Gallery"
- Should navigate to gallery screen
- Shows: "No documents captured yet"
- Back arrow returns to main screen

### 3. Test Permissions:
- Deny camera permission
- Should show "Permission denied" dialog
- Request again should show rationale

---

## 📦 Quick Batch File

I've created `launch_app.bat` in your project folder.  
Just double-click it to install and launch the app!

**Location:** `C:\Users\273744\AndroidStudioProjects\MyApplication\launch_app.bat`

---

## 🐛 Troubleshooting

### App crashes on launch?
```powershell
# View crash logs
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" logcat -d *:E | Select-Object -Last 50
```

### Need to rebuild?
```powershell
cd C:\Users\273744\AndroidStudioProjects\MyApplication
.\gradlew assembleDebug -x lint
```

### Clear app data:
```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell pm clear com.example.myapplication
```

---

## ✅ Success!

Your Document Scanner app is:
- ✅ Built successfully
- ✅ Installed on emulator
- ✅ Launched and running
- ✅ Ready for testing

**Check your emulator screen to interact with the app!** 📱✨


