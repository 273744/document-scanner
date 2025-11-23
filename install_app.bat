@echo off
echo ===================================
echo Installing APK to Emulator
echo ===================================
echo.

set ADB=C:\Users\273744\AppData\Local\Android\Sdk\platform-tools\adb.exe
set APK=C:\Users\273744\AndroidStudioProjects\MyApplication\app\build\outputs\apk\debug\app-debug.apk

echo Checking connected devices...
"%ADB%" devices
echo.

echo Installing APK...
"%ADB%" install -r "%APK%"
echo.

if %ERRORLEVEL% EQU 0 (
    echo ===================================
    echo Installation Successful!
    echo ===================================
    echo.

    echo Launching app...
    "%ADB%" shell am start -n com.srikanth.docscanner/.MainActivity
    echo.

    echo Done! Check your emulator.
) else (
    echo ===================================
    echo Installation Failed!
    echo ===================================
    echo Check if emulator is running
)

echo.
pause

