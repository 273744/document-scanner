# Deploy Document Scanner to Chromebook
param(
    [string]$ChromebookIP = "192.168.1.100",
    [switch]$Help
)

if ($Help) {
    Write-Host @"
Deploy Document Scanner to Chromebook

USAGE:
    .\deploy-to-chromebook.ps1 -ChromebookIP <IP_ADDRESS>

EXAMPLES:
    .\deploy-to-chromebook.ps1 -ChromebookIP "192.168.1.100"
    .\deploy-to-chromebook.ps1 -ChromebookIP "192.168.0.50"

PREREQUISITES:
    1. Enable "Develop Android apps" in Chromebook Settings
    2. Enable "ADB debugging"
    3. Note your Chromebook's IP address
    4. Ensure both devices on same network

"@
    exit
}

$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$apk = "app\build\outputs\apk\debug\app-debug.apk"

Write-Host "`n================================================" -ForegroundColor Cyan
Write-Host "  📱 Deploy to Chromebook" -ForegroundColor Cyan
Write-Host "================================================`n" -ForegroundColor Cyan

# Check if ADB exists
if (-not (Test-Path $adb)) {
    Write-Host "❌ ADB not found at: $adb" -ForegroundColor Red
    Write-Host "   Make sure Android SDK is installed" -ForegroundColor Yellow
    exit 1
}

# Build latest APK
Write-Host "[1/5] Building latest APK..." -ForegroundColor Yellow
.\gradlew assembleDebug -x lint 2>&1 | Out-Null
if (Test-Path $apk) {
    $apkSize = [math]::Round((Get-Item $apk).Length / 1MB, 2)
    Write-Host "✅ Build complete ($apkSize MB)" -ForegroundColor Green
} else {
    Write-Host "❌ Build failed" -ForegroundColor Red
    exit 1
}

# Connect to Chromebook
Write-Host "`n[2/5] Connecting to Chromebook ($ChromebookIP)..." -ForegroundColor Yellow
$connectResult = & $adb connect "${ChromebookIP}:5555" 2>&1
Start-Sleep -Seconds 2

$devices = & $adb devices 2>&1
if ($devices -match $ChromebookIP) {
    Write-Host "✅ Connected to Chromebook" -ForegroundColor Green
} else {
    Write-Host "❌ Connection failed" -ForegroundColor Red
    Write-Host "   Make sure:" -ForegroundColor Yellow
    Write-Host "   • Chromebook ADB debugging is enabled" -ForegroundColor Gray
    Write-Host "   • Both devices on same network" -ForegroundColor Gray
    Write-Host "   • IP address is correct: $ChromebookIP" -ForegroundColor Gray
    exit 1
}

# Uninstall old version
Write-Host "`n[3/5] Removing old version..." -ForegroundColor Yellow
& $adb uninstall com.example.myapplication 2>&1 | Out-Null
Write-Host "✅ Old version removed" -ForegroundColor Green

# Install new version
Write-Host "`n[4/5] Installing app..." -ForegroundColor Yellow
$result = & $adb install -r $apk 2>&1
if ($result -match "Success") {
    Write-Host "✅ Installation successful!" -ForegroundColor Green
} else {
    Write-Host "❌ Installation failed" -ForegroundColor Red
    Write-Host $result
    exit 1
}

# Launch app
Write-Host "`n[5/5] Launching app..." -ForegroundColor Yellow
& $adb shell am start -n com.example.myapplication/.MainActivity 2>&1 | Out-Null
Write-Host "✅ App launched on Chromebook!" -ForegroundColor Green

Write-Host "`n================================================" -ForegroundColor Green
Write-Host "  🎉 Deployment Complete!" -ForegroundColor Green
Write-Host "================================================`n" -ForegroundColor Green

Write-Host "📱 The app is now running on your Chromebook!" -ForegroundColor Cyan
Write-Host "   Check your Chromebook screen to test it.`n" -ForegroundColor White

Write-Host "💡 Useful Commands:" -ForegroundColor Yellow
Write-Host "   View logs:  & '$adb' logcat -s 'DocumentScanner:*'" -ForegroundColor Gray
Write-Host "   Stop app:   & '$adb' shell am force-stop com.example.myapplication" -ForegroundColor Gray
Write-Host "   Disconnect: & '$adb' disconnect`n" -ForegroundColor Gray

