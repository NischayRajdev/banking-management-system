@echo off
REM =====================================================================
REM Banking Management System - Build & Run Script (Windows CMD)
REM =====================================================================

echo [1/3] Creating output directory...
if not exist "bin" mkdir "bin"

echo [2/3] Compiling Java source files...
javac -d bin -cp "lib/*" src/main/java/com/bank/model/*.java src/main/java/com/bank/exception/*.java src/main/java/com/bank/dao/*.java src/main/java/com/bank/service/*.java src/main/java/com/bank/main/*.java
if %ERRORLEVEL% NEQ 0 (
    echo Compilation failed with error code %ERRORLEVEL%.
    exit /b %ERRORLEVEL%
)

REM Copy resources
copy /y src\main\resources\* bin\ > nul

echo [3/3] Running Banking Application...
java -cp "bin;lib/*" com.bank.main.BankingApplication %*

