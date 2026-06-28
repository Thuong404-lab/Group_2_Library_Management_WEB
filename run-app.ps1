#!/usr/bin/env pwsh
# Library Management System - Build and Run Script (PowerShell)

$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Library Management System - Startup" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$projectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $projectDir

# Step 1: Check Java
Write-Host "[1/4] Checking Java installation..." -ForegroundColor Yellow
try {
    $javaVersion = java -version 2>&1
    Write-Host "✓ Java found:" -ForegroundColor Green
    Write-Host $javaVersion[0]
} catch {
    Write-Host "ERROR: Java is not installed or not in PATH" -ForegroundColor Red
    Write-Host "Please install Java 21+ and add it to PATH"
    exit 1
}
Write-Host ""

# Step 2: Find Maven
Write-Host "[2/4] Searching for Maven installation..." -ForegroundColor Yellow

$mavenCmd = $null

# Check if mvn is in PATH
$mavenCmd = (Get-Command mvn -ErrorAction SilentlyContinue).Source
if ($mavenCmd) {
    Write-Host "✓ Maven found in PATH: $mavenCmd" -ForegroundColor Green
}

# Check MAVEN_HOME
if (-not $mavenCmd -and $env:MAVEN_HOME) {
    $mavenPath = Join-Path $env:MAVEN_HOME "bin" "mvn.cmd"
    if (Test-Path $mavenPath) {
        $mavenCmd = $mavenPath
        Write-Host "✓ Maven found via MAVEN_HOME: $mavenCmd" -ForegroundColor Green
    }
}

# Check NetBeans bundled Maven
if (-not $mavenCmd) {
    $netbeansMaven = "C:\Program Files\NetBeans 8.2\java\bin\mvn.exe"
    if (Test-Path $netbeansMaven) {
        $mavenCmd = $netbeansMaven
        Write-Host "✓ Maven found (NetBeans bundled): $mavenCmd" -ForegroundColor Green
    }
}

if (-not $mavenCmd) {
    Write-Host "ERROR: Maven is not installed or not in PATH" -ForegroundColor Red
    Write-Host ""
    Write-Host "SOLUTION: Download Apache Maven from https://maven.apache.org/download.cgi" -ForegroundColor Yellow
    Write-Host "Instructions: https://maven.apache.org/install.html" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "After installation, either:" -ForegroundColor Cyan
    Write-Host "  Option A: Add Maven\bin to your system PATH" -ForegroundColor Cyan
    Write-Host "  Option B: Set MAVEN_HOME environment variable" -ForegroundColor Cyan
    exit 1
}

Write-Host ""

# Step 3: Build Project
Write-Host "[3/4] Building project (this may take a few minutes)..." -ForegroundColor Yellow
Write-Host ""

& $mavenCmd clean package -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Build failed" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "✓ Build completed successfully" -ForegroundColor Green
Write-Host ""

# Step 4: Run Application
Write-Host "[4/4] Starting Spring Boot application..." -ForegroundColor Yellow
Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "Application is starting..." -ForegroundColor Green
Write-Host "Access it at: http://localhost:8080" -ForegroundColor Green
Write-Host "Press Ctrl+C to stop the application" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""

& $mavenCmd spring-boot:run
