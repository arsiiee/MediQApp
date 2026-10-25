@echo off
:: run-all.bat — Start all MediQ services at once
:: Save this at repo root, then run:  run-all.bat
::
:: Delegates to run-mediq-server.ps1, which launches each server in its
:: own window: Ktor (8099), Hono BFF (3000), Angular frontend (4200).

setlocal enabledelayedexpansion
set REPO_ROOT=%~dp0

echo ============================================
echo   MediQ: Starting all services
echo ============================================
echo.

powershell -NoProfile -ExecutionPolicy Bypass -File "%REPO_ROOT%run-mediq-server.ps1"
if errorlevel 1 (
    echo.
    echo run-mediq-server.ps1 exited with an error.
    pause
    exit /b 1
)
