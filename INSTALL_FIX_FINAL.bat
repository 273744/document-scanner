@echo off
SETLOCAL EnableDelayedExpansion

echo.
echo ================================================
echo   Document Scanner - Fix Installation
echo ================================================
echo.

set ADB=C:\Users\273744\AppData\Local\Android\Sdk\platform-tools\adb.exe
set APK=C:\Users\273744\AndroidStudioProjects\MyApplication\app\build\outputs\apk\debug\app-debug.apk

REM Step 1: Kill and restart ADB
echo [1/5] Restarting ADB server...
"%ADB%" kill-server >nul 2>&1
timeout /t 2 /nobreak >nul
"%ADB%" start-server >nul 2>&1
timeout /t 3 /nobreak >nul
echo       Done.
echo.

REM Step 2: Check devices
echo [2/5] Checking emulator connection...
"%ADB%" devices
echo.

REM Step 3: Uninstall old version
echo [3/5] Removing old version...
"%ADB%" uninstall com.srikanth.docscanner >nul 2>&1
echo       Done.
echo.

REM Step 4: Install new APK
echo [4/5] Installing APK...
"%ADB%" install -r "%APK%"
set INSTALL_RESULT=%ERRORLEVEL%
echo.

REM Step 5: Launch app
if %INSTALL_RESULT% EQU 0 (
    echo [5/5] Launching app...
    "%ADB%" shell am start -n com.srikanth.docscanner/.MainActivity
    echo.
    echo ================================================
    echo   SUCCESS!
    echo ================================================
    echo.
    echo The app should now be visible on your emulator!
    echo.
    echo App details:
    echo   Name: Document Scanner
    echo   Package: com.srikanth.docscanner
    echo.
    echo If you don't see it on home screen:
    echo   1. Swipe UP from bottom of emulator
    echo   2. Look for "Document Scanner" in app list
    echo   3. Tap the icon to open
    echo.
) else (
    echo ================================================
    echo   INSTALLATION FAILED
    echo ================================================
    echo.
    echo Possible solutions:
    echo   1. Make sure emulator is running
    echo   2. Try closing and reopening emulator
    echo   3. Run this script again
    echo   4. Check if APK exists: %APK%
    echo.
)

echo Press any key to exit...
pause >nul

