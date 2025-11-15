@echo off
cls
echo ================================================
echo   Document Scanner - App Launcher
echo ================================================
echo.

cd /d "C:\Users\273744\AndroidStudioProjects\MyApplication"

echo [Step 1/4] Checking connected devices...
adb devices
echo.

echo [Step 2/4] Uninstalling old version...
adb uninstall com.example.myapplication >nul 2>&1
echo Done.
echo.

echo [Step 3/4] Installing new APK...
adb install -r "app\build\outputs\apk\debug\app-debug.apk"
echo.

echo [Step 4/4] Launching Document Scanner app...
adb shell am start -n com.example.myapplication/.MainActivity
echo.

echo ================================================
echo   ✅ App launched successfully!
echo   Check your emulator screen.
echo ================================================
echo.

timeout /t 3 >nul

