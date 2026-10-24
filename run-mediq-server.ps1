# -------------------------------------------------
# run-mediq-server.ps1
# -------------------------------------------------
# This script launches the MediQ Ktor server (port 8099) and
# ensures adb reverse is set up for Android emulator/devices.
# It runs the server as a background process and redirects logs
# to server.log / server-error.log in the repository root.

# Resolve repo root (the directory containing this script)
$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$serverDir = Join-Path $repoRoot "server"

# -------------------------------------------------
# 1️⃣ Set up ADB reverse (required for Android emulator)
# -------------------------------------------------
# Silently ignore errors if adb is not installed or the device is not connected.
try {
    adb reverse tcp:8099 tcp:8099 2>$null
} catch { }

# -------------------------------------------------
# 2️⃣ Start the Ktor server in the background
# -------------------------------------------------
Push-Location $serverDir
# Start-Process returns immediately; the server keeps running.
Start-Process -FilePath "cmd.exe" `
    -ArgumentList "/c", "gradlew.bat", "run" `
    -WorkingDirectory $serverDir `
    -RedirectStandardOutput "$repoRoot\server.log" `
    -RedirectStandardError "$repoRoot\server-error.log" `
    -WindowStyle Hidden
Pop-Location

Write-Host "✅ MediQ Ktor server launched (logs → server.log)."