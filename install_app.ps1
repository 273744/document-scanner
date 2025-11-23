$adb = "C:\Users\273744\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$apk = "C:\Users\273744\AndroidStudioProjects\MyApplication\app\build\outputs\apk\debug\app-debug.apk"

Write-Host "==================================="
Write-Host "Installing APK to Emulator"
Write-Host "==================================="
Write-Host ""

# Check if ADB exists
if (-not (Test-Path $adb)) {
    Write-Host "ERROR: ADB not found at: $adb" -ForegroundColor Red
    pause
    exit
}

# Check if APK exists
if (-not (Test-Path $apk)) {
    Write-Host "ERROR: APK not found at: $apk" -ForegroundColor Red
    pause
    exit
}

Write-Host "Checking connected devices..." -ForegroundColor Yellow
& $adb devices
Write-Host ""

Write-Host "Installing APK..." -ForegroundColor Yellow
& $adb install -r $apk
Write-Host ""

Write-Host "==================================="
Write-Host "Installation Complete!"
Write-Host "==================================="
Write-Host ""

Write-Host "Launching app..." -ForegroundColor Yellow
& $adb shell am start -n com.srikanth.docscanner/.MainActivity
Write-Host ""

Write-Host "Done! Check your emulator." -ForegroundColor Green
pause

