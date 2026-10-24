@echo off
:: run-all.bat — Start all MediQ services at once
:: Save this at repo root, then run:  run-all.bat
:: Each service opens in its own command window so you can see its output.

setlocal enabledelayedexpansion

set REPO_ROOT=%~dp0
set ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr

echo ============================================
echo   MediQ: Starting all services
echo ============================================

:: ---- 1. Verify emulator is connected ----
echo.
echo [1/5] Checking emulator...
"%ADB%" devices | findstr "emulator-5554" >nul
if errorlevel 1 (
    echo ERROR: No emulator found. Start an emulator first, then re-run.
    pause
    exit /b 1
)
echo Emulator connected.

:: ---- 2. Set up adb reverse (emulator -> host port 8099) ----
echo.
echo [2/5] Setting up adb reverse tcp:8099...
"%ADB%" reverse tcp:8099 tcp:8099
if errorlevel 1 (
    echo WARNING: adb reverse failed.
) else (
    echo Port forwarding active.
)

:: ---- 3. Start Ktor server (8099) in a new window ----
echo.
echo [3/5] Starting Ktor server on 8099...
start "Ktor Server" cmd /k "cd /d "%REPO_ROOT%server" & gradlew.bat run"
timeout /t 6 >nul

:: ---- 4. Start Hono BFF (3000) in a new window ----
echo.
echo [4/5] Starting Hono BFF on 3000...
start "Hono BFF" cmd /k "set SUPABASE_URL=https://wdoyhtjpjhmhsegdllvd.supabase.co & set SUPABASE_PUBLISHABLE_KEY=REDACTED_SUPABASE_KEY & cd /d "%REPO_ROOT%mediqweb\mediqapp-api" & node --env-file .env src/index.js"
timeout /t 3 >nul

:: ---- 5. Install + launch MediQ app on emulator ----
echo.
echo [5/5] Installing + launching MediQ app on emulator...
set APK="%REPO_ROOT%app\build\outputs\apk\debug\app-debug.apk"
if not exist %APK% (
    echo WARNING: APK not found. Build it first: gradlew.bat :app:assembleDebug
) else (
    "%ADB%" install -r %APK%
    "%ADB%" shell am start -n com.example.mediq/.MainActivity -a android.intent.action.MAIN -c android.intent.category.LAUNCHER
    echo App launched on emulator.
)

:: ---- Verification ----
echo.
echo ============================================
echo   Verification
echo ============================================
echo Ktor  : curl.exe http://localhost:8099/health
echo Hono  : curl.exe http://localhost:3000/health
echo Sign-in: curl.exe -X POST http://localhost:3000/auth/sign-in -H "Content-Type: application/json" -d "{\"username\":\"demo_patient\",\"password\":\"demo12345\"}"
echo Web FE: cd mediqweb\mediqweb-fe & npm start  (serves on 4200)
echo.
echo All services started. Keep these windows open.
echo ============================================
pause
