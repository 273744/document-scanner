Write-Host "============================================" -ForegroundColor Cyan
Write-Host "Document Scanner App - Install and Launch" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

Set-Location "C:\Users\273744\AndroidStudioProjects\MyApplication"

Write-Host "[1/4] Checking for connected devices..." -ForegroundColor Yellow
adb devices
Write-Host ""

Write-Host "[2/4] Uninstalling old version (if exists)..." -ForegroundColor Yellow
adb uninstall com.example.myapplication 2>$null
Write-Host ""

Write-Host "[3/4] Installing new APK..." -ForegroundColor Yellow
$installResult = adb install -r "app\build\outputs\apk\debug\app-debug.apk"
Write-Host $installResult
Write-Host ""

if ($installResult -match "Success") {
    Write-Host "[4/4] Launching app..." -ForegroundColor Yellow
    adb shell am start -n com.example.myapplication/.MainActivity
    Write-Host ""
    Write-Host "============================================" -ForegroundColor Green
    Write-Host "✅ SUCCESS! App launched on emulator." -ForegroundColor Green
    Write-Host "============================================" -ForegroundColor Green
} else {
    Write-Host "============================================" -ForegroundColor Red
    Write-Host "❌ Installation failed. Check the output above." -ForegroundColor Red
    Write-Host "============================================" -ForegroundColor Red
}

Write-Host ""
Write-Host "Press any key to close..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")

