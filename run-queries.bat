@echo off
REM =====================================================================
REM India Banking Management System - SQL Queries Runner (Windows CMD)
REM Runs queries from queries.sql against bank_system.db via JDBC Connection
REM =====================================================================

setlocal enabledelayedexpansion

set "JAVA_BIN=java"
if exist "C:\Program Files\Java\jdk-21.0.12\bin\java.exe" (
    set "JAVA_BIN=C:\Program Files\Java\jdk-21.0.12\bin\java.exe"
) else if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_BIN=%JAVA_HOME%\bin\java.exe"
)

if not exist "bin" mkdir "bin"

if not exist "bin\com\bank\dao\SqlRunner.class" (
    echo [*] Compiling Java source files...
    javac -d bin -cp "lib/*" src/main/java/com/bank/model/*.java src/main/java/com/bank/exception/*.java src/main/java/com/bank/dao/*.java src/main/java/com/bank/service/*.java src/main/java/com/bank/main/*.java src/main/java/com/bank/api/*.java
    if %ERRORLEVEL% NEQ 0 (
        echo Compilation failed.
        exit /b %ERRORLEVEL%
    )
    copy /y src\main\resources\* bin\ > nul
)

if "%~1"=="" (
    "%JAVA_BIN%" -cp "bin;lib/*" com.bank.dao.SqlRunner queries.sql
) else (
    "%JAVA_BIN%" -cp "bin;lib/*" com.bank.dao.SqlRunner queries.sql %1
)
