@echo off
echo ============================================
echo   MediQ: Stopping all services
echo ============================================

echo.
echo Stopping Ktor Server on port 8099...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8099" ^| findstr "LISTENING"') do (
    echo Found PID %%a listening on port 8099. Killing...
    taskkill /PID %%a /F >nul 2>&1
)

echo.
echo Stopping Hono BFF on port 3000...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":3000" ^| findstr "LISTENING"') do (
    echo Found PID %%a listening on port 3000. Killing...
    taskkill /PID %%a /F >nul 2>&1
)

set ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe
if exist "%ADB%" (
    echo.
    echo Removing adb reverse tcp:8099...
    "%ADB%" reverse --remove tcp:8099 >nul 2>&1
    echo   done.
) else (
    echo.
    echo WARNING: adb not found at %ADB%, skipping port-forward cleanup.
)

if exist "%ADB%" (
    echo.
    echo Force-stopping MediQ app on emulator...
    "%ADB%" shell am force-stop com.example.mediq >nul 2>&1
    echo   done.
)

echo.
echo ============================================
echo   All MediQ services stopped.
echo ============================================
