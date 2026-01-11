@echo off
setlocal enabledelayedexpansion

:: NeoGuard Startup Script
:: Automatically kills any process on port 8080 before starting

echo.
echo   Checking port 8080...

:: Find and kill any process on port 8080
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING') do (
    echo   Stopping process %%a on port 8080...
    taskkill /F /PID %%a >nul 2>&1
)

:: Small delay to ensure port is released
timeout /t 1 /nobreak >nul

echo   Starting NeoGuard...
echo.

:: Start the application
java -jar target/neo-guard-1.0.0.jar

endlocal
