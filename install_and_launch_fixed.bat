@echo off
echo ==========================================
echo Document Scanner - Install and Launch
echo ==========================================
echo.

set ADB=C:\Users\273744\AppData\Local\Android\Sdk\platform-tools\adb.exe
set APK=C:\Users\273744\AndroidStudioProjects\MyApplication\app\build\outputs\apk\debug\app-debug.apk

echo Step 1: Checking emulator connection...
"%ADB%" devices
echo.

echo Step 2: Uninstalling old version (if exists)...
"%ADB%" uninstall com.srikanth.docscanner >nul 2>&1
echo Done.
echo.

echo Step 3: Installing new APK...
"%ADB%" install -r "%APK%"
echo.

if %ERRORLEVEL% EQU 0 (
    echo Step 4: Launching app...
    "%ADB%" shell am start -n com.srikanth.docscanner/.MainActivity
    echo.
    echo ==========================================
    echo SUCCESS! App installed and launched!
    echo ==========================================
    echo.
    echo Check your emulator - the app should be opening now!
    echo App name: Document Scanner
    echo Package: com.srikanth.docscanner
) else (
    echo ==========================================
    echo INSTALLATION FAILED!
    echo ==========================================
    echo.
    echo Possible issues:
    echo 1. Emulator not running
    echo 2. APK not found or corrupted
    echo 3. USB debugging not enabled
    echo.
    echo Try:
    echo - Start emulator from Android Studio
    echo - Run: quick_build.bat first
)

echo.
echo Press any key to close...
pause >nul

