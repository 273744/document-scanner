@echo off
echo ============================================
echo Document Scanner App - Install and Launch
echo ============================================
cd /d "C:\Users\273744\AndroidStudioProjects\MyApplication"

echo.
echo [1/4] Checking for connected devices...
adb devices

echo.
echo [2/4] Uninstalling old version...
adb uninstall com.example.myapplication

echo.
echo [3/4] Installing new APK...
adb install -r "app\build\outputs\apk\debug\app-debug.apk"

echo.
echo [4/4] Launching app...
adb shell am start -n com.example.myapplication/.MainActivity

echo.
echo ============================================
echo Done! Check your emulator screen.
echo ============================================
pause

