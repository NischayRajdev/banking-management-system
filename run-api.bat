@echo off
REM =====================================================================
REM Banking Management System - Unified Server Run Script (Batch/CMD)
REM =====================================================================

echo [1/3] Creating output directory...
if not exist "bin" mkdir bin

echo [2/3] Compiling Java source files...
dir /s /b src\main\java\*.java > sources.txt
javac -d bin -cp "lib/*" @sources.txt
del sources.txt

xcopy /s /y /q src\main\resources\* bin\ > nul

echo [3/3] Starting India Bank Unified Web & API Server on http://localhost:8080/...
java -cp "bin;lib/*" com.bank.api.BankingRestServer 8080
