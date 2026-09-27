@echo off
title Smart Network Monitoring System
echo Starting Smart Network Monitoring System...
echo ========================================
cd /d "%~dp0"

echo [1/3] Starting MySQL if not running...
sc query MySQL84 | find "RUNNING" >nul 2>&1
if %errorlevel% neq 0 (
    echo MySQL not running. Starting MySQL84 service...
    net start MySQL84 >nul 2>&1
    if %errorlevel% neq 0 (
        echo WARNING: Could not start MySQL84 service. Database features may not work.
    ) else (
        echo MySQL started successfully.
        timeout /t 3 /nobreak >nul
    )
) else (
    echo MySQL is already running.
)

echo [2/3] Compiling project...
javac -cp "lib/*" -d out *.java
if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b %errorlevel%
)

echo [3/3] Launching Application...
java -cp "out;lib/*" com.networkmonitor.main.MainApp
pause

