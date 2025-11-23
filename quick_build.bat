@echo off
echo ==========================================
echo Building Project (Quick Build)
echo ==========================================
echo.
cd /d "C:\Users\273744\AndroidStudioProjects\MyApplication"
call gradlew.bat assembleDebug
echo.
echo ==========================================
if %ERRORLEVEL% EQU 0 (
    echo BUILD SUCCESSFUL!
    echo APK Location: app\build\outputs\apk\debug\app-debug.apk
) else (
    echo BUILD FAILED - Check errors above
)
echo ==========================================
pause

