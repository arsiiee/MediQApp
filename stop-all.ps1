# stop-all.ps1 — Stop all MediQ services started by run-all.bat / run-all.ps1
# Kills processes listening on ports 8099 (Ktor) and 3000 (Hono BFF),
# removes adb reverse port forwarding, and force-stops the app on the emulator.

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  MediQ: Stopping all services" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan

# Helper function to kill process on a port
function Stop-Port {
    param([int]$Port, [string]$Name)
    Write-Host ""
    Write-Host "Stopping $Name on port $Port..."
    $processes = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    if ($processes) {
        $processes | ForEach-Object {
            $pid = $_.OwningProcess
            Write-Host "Found PID $pid listening on port $Port. Killing..."
            try {
                Stop-Process -Id $pid -Force -ErrorAction Stop
                Write-Host "  Killed PID $pid." -ForegroundColor Green
            } catch {
                Write-Host "  (already stopped or access denied)" -ForegroundColor Yellow
            }
        }
    } else {
        Write-Host "  No process found on port $Port." -ForegroundColor Gray
    }
}

# Stop Ktor server (8099)
Stop-Port -Port 8099 -Name "Ktor Server"

# Stop Hono BFF (3000)
Stop-Port -Port 3000 -Name "Hono BFF"

# Remove adb reverse port forwarding
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
if (Test-Path $adb) {
    Write-Host ""
    Write-Host "Removing adb reverse tcp:8099..."
    & $adb reverse --remove tcp:8099 $null 2>$null
    Write-Host "  done." -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "WARNING: adb not found at $adb, skipping port-forward cleanup." -ForegroundColor Yellow
}

# Force-stop the MediQ app on the emulator
if (Test-Path $adb) {
    Write-Host ""
    Write-Host "Force-stopping MediQ app on emulator..."
    & $adb shell am force-stop com.example.mediq $null 2>$null
    Write-Host "  done." -ForegroundColor Green
}

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  All MediQ services stopped." -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
