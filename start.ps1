# =====================================================================
# India Bank - Unified Web & API Server Launcher (Single Localhost)
# =====================================================================

$ErrorActionPreference = "Stop"

Write-Host "[1/3] Checking Frontend Production Build..." -ForegroundColor Cyan
if (-not (Test-Path "frontend\dist\index.html")) {
    Push-Location frontend
    if (-not (Test-Path "node_modules")) {
        Write-Host "Installing frontend dependencies..." -ForegroundColor Yellow
        npm.cmd install
    }
    Write-Host "Building React frontend production assets..." -ForegroundColor Yellow
    npm.cmd run build
    Pop-Location
} else {
    Write-Host "Frontend build verified at frontend\dist." -ForegroundColor Green
}

Write-Host "[2/3] Compiling Java Core & REST Server..." -ForegroundColor Cyan
New-Item -ItemType Directory -Force -Path "bin" | Out-Null
$sourceFiles = Get-ChildItem -Recurse -Filter *.java src/main/java | Select-Object -ExpandProperty FullName
javac -d bin -cp "lib/*" $sourceFiles
Copy-Item -Path "src/main/resources/*" -Destination "bin" -Recurse -Force

Write-Host "`n======================================================================" -ForegroundColor Green
Write-Host "  INDIA BANK - UNIFIED DASHBOARD RUNNING ON A SINGLE LOCALHOST" -ForegroundColor Green
Write-Host "======================================================================" -ForegroundColor Green
Write-Host "  Dashboard URL : http://localhost:8080/" -ForegroundColor Yellow
Write-Host "  Core REST API : http://localhost:8080/api" -ForegroundColor Cyan
Write-Host "======================================================================" -ForegroundColor Green
Write-Host "Press Ctrl+C to stop the server.`n" -ForegroundColor Gray

java -cp "bin;lib/*" com.bank.api.BankingRestServer 8080
