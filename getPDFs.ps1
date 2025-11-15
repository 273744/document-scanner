# Quick script to pull PDFs from emulator
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$remotePath = "/storage/emulated/0/Android/data/com.example.myapplication/files/DocumentScanner"
$localPath = "./GeneratedPDFs"

Write-Host "`n📄 Downloading PDFs from Emulator..." -ForegroundColor Cyan
Write-Host "====================================`n" -ForegroundColor Cyan

# Create local directory
New-Item -ItemType Directory -Force -Path $localPath | Out-Null

# Check if files exist
$fileCheck = & $adb shell "ls $remotePath/*.pdf 2>/dev/null | wc -l" 2>&1
$fileCount = $fileCheck.Trim()

if ($fileCount -gt 0) {
    Write-Host "✅ Found $fileCount PDF file(s)" -ForegroundColor Green

    # Pull all PDFs
    Write-Host "`n⬇️  Downloading..." -ForegroundColor Yellow
    & $adb pull "$remotePath/" "$localPath/" 2>&1

    # Show downloaded files
    Write-Host "`n✅ PDFs downloaded to: $localPath" -ForegroundColor Green
    Get-ChildItem $localPath -Filter *.pdf -ErrorAction SilentlyContinue | ForEach-Object {
        Write-Host "   📄 $($_.Name) ($([math]::Round($_.Length/1KB, 2)) KB)" -ForegroundColor White
    }

    # Open folder
    Write-Host "`n📂 Opening folder..." -ForegroundColor Cyan
    explorer $localPath

} else {
    Write-Host "⚠️  No PDFs found on emulator" -ForegroundColor Yellow
    Write-Host "`n📝 To generate a PDF:" -ForegroundColor Cyan
    Write-Host "   1. Open the app on emulator" -ForegroundColor White
    Write-Host "   2. Tap 'Capture Document'" -ForegroundColor White
    Write-Host "   3. Capture an image" -ForegroundColor White
    Write-Host "   4. Tap 'Enhance & Generate PDF'" -ForegroundColor White
    Write-Host "   5. Select a filter (optional)" -ForegroundColor White
    Write-Host "   6. Tap 'PDF' button" -ForegroundColor White
    Write-Host "   7. Run this script again!`n" -ForegroundColor White
}

