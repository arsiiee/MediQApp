# run-all.ps1 — Start all MediQ services at once
# Save this at repo root, then run:  .\run-all.ps1

$ErrorActionPreference = "Stop"
$repoRoot = (Get-Location).Path

Write-Host "=== MediQ: Starting all services ===" -ForegroundColor Cyan

# ---- 1. Verify emulator is connected ----
Write-Host "`n[1/5] Checking emulator..." -ForegroundColor Yellow
$devices = & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
if ($devices -notmatch "emulator-5554") {
    Write-Warning "No emulator found. Start an emulator first, then re-run."
    exit 1
}
Write-Host "Emulator connected." -ForegroundColor Green

# ---- 2. Set up adb reverse (emulator -> host port 8099) ----
Write-Host "`n[2/5] Setting up adb reverse tcp:8099..." -ForegroundColor Yellow
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" reverse tcp:8099 tcp:8099
if ($LASTEXITCODE -ne 0) {
    Write-Warning "adb reverse failed."
} else {
    Write-Host "Port forwarding active." -ForegroundColor Green
}

# ---- 3. Start Ktor server (8099) in background ----
Write-Host "`n[3/5] Starting Ktor server on 8099..." -ForegroundColor Yellow
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
Start-Process powershell -ArgumentList "-NoExit", "-Command", `
    "& ""$repoRoot/server/gradlew.bat"" run" -WorkingDirectory "$repoRoot/server"
Start-Sleep -Seconds 5

# ---- 4. Start Hono BFF (3000) in background ----
Write-Host "`n[4/5] Starting Hono BFF on 3000..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", `
    "`$env:SUPABASE_URL=`'https://wdoyhtjpjhmhsegdllvd.supabase.co`'; `" + `
    "`$env:SUPABASE_PUBLISHABLE_KEY=`'REDACTED_SUPABASE_KEY`'; `" + `
    "node --env-file .env src/index.js" -WorkingDirectory "$repoRoot/mediqweb/mediqapp-api"
Start-Sleep -Seconds 3

# ---- 5. Install + launch MediQ app on emulator ----
Write-Host "`n[5/5] Installing + launching MediQ app on emulator..." -ForegroundColor Yellow
$apk = "$repoRoot/app/build/outputs/apk/debug/app-debug.apk"
if (!(Test-Path $apk)) {
    Write-Warning "APK not found. Build it first: ./gradlew.bat :app:assembleDebug"
} else {
    & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r $apk
    & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell am start -n "com.example.mediq/.MainActivity" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER
    Write-Host "App launched on emulator." -ForegroundColor Green
}

# ---- Verification ----
Write-Host "`n=== Verification ===" -ForegroundColor Cyan
Write-Host "Ktor  : curl.exe http://localhost:8099/health"
Write-Host "Hono  : curl.exe http://localhost:3000/health"
Write-Host "Sign-in: curl.exe -X POST http://localhost:3000/auth/sign-in -H `"Content-Type: application/json`" -d `"{""username"":""demo_patient"",""password"":""demo12345""}"`"
Write-Host "Web FE: npm start  (in mediqweb/mediqweb-fe, serves on 4200)"
Write-Host "`nAll services started. Leave these terminal windows open." -ForegroundColor Green
