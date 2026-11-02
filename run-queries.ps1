# =====================================================================
# India Banking Management System - SQL Queries Runner (PowerShell)
# Runs queries from queries.sql against bank_system.db via JDBC Connection
# Usage:
#   .\run-queries.ps1          (runs all 11 queries)
#   .\run-queries.ps1 7        (runs query #7)
#   .\run-queries.ps1 -Query 11
# =====================================================================

param (
    [Parameter(Position = 0)]
    [string]$QueryNumber
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path "bin\com\bank\dao\SqlRunner.class")) {
    Write-Host "[*] Compiling Java source files..." -ForegroundColor Cyan
    New-Item -ItemType Directory -Force -Path "bin" | Out-Null
    $sourceFiles = Get-ChildItem -Recurse -Filter *.java src/main/java | Select-Object -ExpandProperty FullName
    javac -d bin -cp "lib/*" $sourceFiles
    Copy-Item -Path "src/main/resources/*" -Destination "bin" -Recurse -Force
}

$javaCmd = "java"
if (Get-Command "C:\Program Files\Java\jdk-21.0.12\bin\java.exe" -ErrorAction SilentlyContinue) {
    $javaCmd = "C:\Program Files\Java\jdk-21.0.12\bin\java.exe"
}

if ($QueryNumber) {
    & $javaCmd -cp "bin;lib/*" com.bank.dao.SqlRunner queries.sql $QueryNumber
} else {
    & $javaCmd -cp "bin;lib/*" com.bank.dao.SqlRunner queries.sql
}

