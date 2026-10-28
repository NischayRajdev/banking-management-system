# =====================================================================
# Banking Management System - Build & Run Script (PowerShell)
# =====================================================================

$ErrorActionPreference = "Stop"

Write-Host "[1/3] Creating output directory..." -ForegroundColor Cyan
New-Item -ItemType Directory -Force -Path "bin" | Out-Null

Write-Host "[2/3] Compiling Java source files..." -ForegroundColor Cyan
$sourceFiles = Get-ChildItem -Recurse -Filter *.java src/main/java | Select-Object -ExpandProperty FullName
javac -d bin -cp "lib/*" $sourceFiles

Copy-Item -Path "src/main/resources/*" -Destination "bin" -Recurse -Force

Write-Host "[3/3] Running Banking Application..." -ForegroundColor Cyan
java -cp "bin;lib/*" com.bank.main.BankingApplication $args

