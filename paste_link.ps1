Clear-Host
Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host "🚀 TapMarker - Auto Copy & Paste" -ForegroundColor Green
Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. Open Instagram Gold on your phone."
Write-Host "2. Drag the red dots onto the 'Copy' buttons."
Write-Host "3. Drag S (start) and E (end) for scrolling."
Write-Host "4. Press the ▶ button on your phone."
Write-Host "5. This script will automatically grab the coords and tap!"
Write-Host ""

$scriptPath = Join-Path $PSScriptRoot "instagram_auto.py"
if (-not (Test-Path $scriptPath)) {
    Write-Host "❌ ERROR: instagram_auto.py not found!" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

$output = python $scriptPath 2>&1 | Out-String
Write-Host $output -ForegroundColor Gray

if ($output -match "LINK_OUTPUT: (.*)") {
    $link = $matches[1].Trim()
    if ($link -and $link -ne "ERROR") {
        Write-Host ""
        Write-Host "✅ LINK CAPTURED: $link" -ForegroundColor Green
        $escapedLink = $link -replace "([{}()\[\]+^%~])", "{$1}"
        Write-Host "⌨️  Typing link in 2 seconds..." -ForegroundColor Yellow
        Start-Sleep -Seconds 2
        Add-Type -AssemblyName System.Windows.Forms
        [System.Windows.Forms.SendKeys]::SendWait($escapedLink)
        [System.Windows.Forms.SendKeys]::SendWait("{ENTER}")
        Write-Host "✅ DONE!" -ForegroundColor Green
    } else {
        Write-Host "❌ No valid link captured." -ForegroundColor Red
    }
} else {
    Write-Host "❌ Failed to get link." -ForegroundColor Red
}

Read-Host "Press Enter to close"
