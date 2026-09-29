@echo off
title Smart Network Monitoring System
echo Starting Smart Network Monitoring System...
echo ========================================
cd /d "%~dp0"

echo [1/2] Compiling project...
javac -cp "lib/*" -d out *.java
if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b %errorlevel%
)

echo [2/2] Launching Application...
java -cp "out;lib/*" com.networkmonitor.main.MainApp
pause

