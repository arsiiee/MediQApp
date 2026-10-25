# -------------------------------------------------
# run-mediq-server.ps1
# -------------------------------------------------
# Launches all three MediQ servers, each in its own window:
#   Ktor server        -> port 8099
#   Hono BFF           -> port 3000
#   Angular frontend   -> port 4200
#
# Run via: run-mediq-server.bat  (or .\run-mediq-server.ps1 from an open PowerShell)
# Press Ctrl-C in a child window to stop that server.
# This window stays open until you press Enter.
# -------------------------------------------------

$ErrorActionPreference = "Continue"
$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$serverDir = Join-Path $repoRoot "server"
$apiDir    = Join-Path $repoRoot "mediqweb\mediqapp-api"
$feDir     = Join-Path $repoRoot "mediqweb\mediqweb-fe"
$adb       = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  MediQ: Starting all servers" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan

# -------------------------------------------------
# Helper: check if a port is already in use
# -------------------------------------------------
function Test-PortFree {
    param([int]$Port, [string]$Name)
    $inUse = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    if ($inUse) {
        $owningPid = $inUse[0].OwningProcess
        Write-Host "  ERROR: Port $Port ($Name) is already in use (PID $owningPid)." -ForegroundColor Red
        Write-Host "         Run stop-all.bat first, or free port $Port, then re-run." -ForegroundColor Red
        return $false
    }
    return $true
}

# -------------------------------------------------
# 0️⃣ Prerequisite checks
# -------------------------------------------------
Write-Host "`n[0/5] Checking prerequisites..." -ForegroundColor Yellow

# Java for Ktor
$javaHome = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { "C:\Program Files\Android\Android Studio\jbr" }
if (!(Test-Path "$javaHome\bin\java.exe")) {
    Write-Host "  ERROR: java.exe not found at $javaHome\bin\java.exe" -ForegroundColor Red
    Write-Host "         Set JAVA_HOME and re-run." -ForegroundColor Red
    Read-Host "Press Enter to close this window..."
    exit 1
}
Write-Host "   Java: $javaHome" -ForegroundColor Green

# node + npm for Hono + Angular
if (!(Get-Command node -ErrorAction SilentlyContinue)) {
    Write-Host "  ERROR: node is not on PATH. Install Node.js and re-run." -ForegroundColor Red
    Read-Host "Press Enter to close this window..."
    exit 1
}
if (!(Get-Command npm -ErrorAction SilentlyContinue)) {
    Write-Host "  ERROR: npm is not on PATH. Install Node.js and re-run." -ForegroundColor Red
    Read-Host "Press Enter to close this window..."
    exit 1
}
Write-Host "   node: $(node --version)" -ForegroundColor Green
Write-Host "   npm : $(npm --version)" -ForegroundColor Green

# gradlew.bat for Ktor
if (!(Test-Path "$serverDir\gradlew.bat")) {
    Write-Host "  ERROR: gradlew.bat not found in $serverDir" -ForegroundColor Red
    Read-Host "Press Enter to close this window..."
    exit 1
}
Write-Host "   gradlew.bat: found" -ForegroundColor Green

# Ports free?
$portsOk = $true
$portsOk = $portsOk -and (Test-PortFree -Port 8099 -Name "Ktor")
$portsOk = $portsOk -and (Test-PortFree -Port 3000 -Name "Hono BFF")
$portsOk = $portsOk -and (Test-PortFree -Port 4200 -Name "Angular")
if (-not $portsOk) {
    Read-Host "Press Enter to close this window..."
    exit 1
}

# -------------------------------------------------
# 1️⃣ ADB reverse (emulator -> host port 8099)
# -------------------------------------------------
Write-Host "`n[1/5] Setting up adb reverse tcp:8099..." -ForegroundColor Yellow
if (Test-Path $adb) {
    & $adb reverse tcp:8099 tcp:8099 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "   Port forwarding active." -ForegroundColor Green
    } else {
        Write-Warning "   adb reverse failed (emulator may not be connected)."
    }
} else {
    Write-Warning "   adb not found at $adb, skipping port-forward."
}

# -------------------------------------------------
# 2️⃣ Start Ktor server (8099) in its own window
# -------------------------------------------------
Write-Host "`n[2/5] Starting Ktor server on 8099..." -ForegroundColor Yellow
try {
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd `"$serverDir`"; `"$serverDir\gradlew.bat`" run" `
        -WorkingDirectory $serverDir `
        -WindowStyle Normal
    Start-Sleep -Seconds 5
    Write-Host "   Ktor server window launched." -ForegroundColor Green
} catch {
    Write-Host "  ERROR launching Ktor: $($_.Exception.Message)" -ForegroundColor Red
}

# -------------------------------------------------
# 3️⃣ Start Hono BFF (3000) in its own window
# -------------------------------------------------
Write-Host "`n[3/5] Starting Hono BFF on 3000..." -ForegroundColor Yellow
try {
    $apiCmd = '$env:PORT=''3000''; $env:KTOR_BASE_URL=''http://localhost:8099''; $env:SUPABASE_URL=''https://wdoyhtjpjhmhsegdllvd.supabase.co''; $env:SUPABASE_PUBLISHABLE_KEY=''REDACTED_SUPABASE_KEY''; $env:COOKIE_SECRET=''mediq-dev-cookie-secret-change-in-production''; cd "' + $apiDir + '"; npm start'
    Start-Process powershell -ArgumentList "-NoExit", "-Command", $apiCmd -WorkingDirectory $apiDir -WindowStyle Normal
    Start-Sleep -Seconds 3
    Write-Host "   Hono BFF window launched." -ForegroundColor Green
} catch {
    Write-Host "  ERROR launching Hono BFF: $($_.Exception.Message)" -ForegroundColor Red
}

# -------------------------------------------------
# 4️⃣ Start Angular frontend (4200) in its own window
# -------------------------------------------------
Write-Host "`n[4/5] Starting Angular frontend on 4200..." -ForegroundColor Yellow
try {
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd `"$feDir`"; npm start" `
        -WorkingDirectory $feDir `
        -WindowStyle Normal
    Start-Sleep -Seconds 3
    Write-Host "   Angular frontend window launched." -ForegroundColor Green
} catch {
    Write-Host "  ERROR launching Angular: $($_.Exception.Message)" -ForegroundColor Red
}

# -------------------------------------------------
# 5️⃣ Verification
# -------------------------------------------------
Write-Host "`n[5/5] Verification" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "Ktor  : curl.exe http://localhost:8099/health" -ForegroundColor Gray
Write-Host "Hono  : curl.exe http://localhost:3000/health" -ForegroundColor Gray
Write-Host "Sign-in: curl.exe -X POST http://localhost:3000/auth/sign-in -H `"Content-Type: application/json`" -d `{""username"":""demo_patient"",""password"":""demo12345""}`"" -ForegroundColor Gray
Write-Host "Web FE: http://localhost:4200" -ForegroundColor Gray
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "All servers launched. Keep these windows open." -ForegroundColor Green
Write-Host "Press Ctrl-C in a child window to stop that server." -ForegroundColor Green
Write-Host ""

# -------------------------------------------------
# Keep THIS window open until the user presses Enter
# -------------------------------------------------
Read-Host "Press Enter to close this window..."
