@echo off
REM =====================================================================
REM India Bank - Unified Web & API Server Launcher (Single Localhost)
REM =====================================================================

echo [1/3] Checking Frontend Production Build...
if not exist "frontend\dist\index.html" (
    cd frontend
    if not exist "node_modules" (
        echo Installing frontend dependencies...
        call npm.cmd install
    )
    echo Building React frontend production assets...
    call npm.cmd run build
    cd ..
) else (
    echo Frontend build verified at frontend\dist.
)

echo [2/3] Compiling Java Core & REST Server...
if not exist "bin" mkdir bin
dir /s /b src\main\java\*.java > sources.txt
javac -d bin -cp "lib/*" @sources.txt
del sources.txt

xcopy /s /y /q src\main\resources\* bin\ > nul

echo.
echo ======================================================================
echo   INDIA BANK - UNIFIED DASHBOARD RUNNING ON A SINGLE LOCALHOST
echo ======================================================================
echo   Dashboard URL : http://localhost:8080/
echo   Core REST API : http://localhost:8080/api
echo ======================================================================
echo Press Ctrl+C to stop the server.
echo.

java -cp "bin;lib/*" com.bank.api.BankingRestServer 8080
