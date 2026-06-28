@echo off
REM Library Management System - Build and Run Script
REM This script builds the project with Maven and runs the Spring Boot application

setlocal enabledelayedexpansion

echo.
echo ========================================
echo Library Management System - Startup
echo ========================================
echo.

REM Set the project directory
set PROJECT_DIR=%~dp0
cd /d "%PROJECT_DIR%"

echo [1/4] Checking Java installation...
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java 21+ and add it to PATH
    pause
    exit /b 1
)
java -version
echo ✓ Java found
echo.

echo [2/4] Searching for Maven installation...
REM Try multiple Maven locations
set MAVEN_CMD=
for %%M in (mvn.cmd mvn.bat) do (
    where %%M >nul 2>nul && set MAVEN_CMD=%%M && goto :found_maven
)

REM Check NetBeans bundled Maven
if exist "C:\Program Files\NetBeans 8.2\java\bin\mvn.exe" (
    set MAVEN_CMD=C:\Program Files\NetBeans 8.2\java\bin\mvn.exe
    goto :found_maven
)

REM Check if MAVEN_HOME is set
if not "!MAVEN_HOME!"=="" (
    if exist "!MAVEN_HOME!\bin\mvn.cmd" (
        set MAVEN_CMD=!MAVEN_HOME!\bin\mvn.cmd
        goto :found_maven
    )
)

:found_maven
if "!MAVEN_CMD!"=="" (
    echo ERROR: Maven is not installed or not in PATH
    echo.
    echo SOLUTION: Please download and install Maven from https://maven.apache.org/download.cgi
    echo Instructions: https://maven.apache.org/install.html
    echo.
    echo After installation, either:
    echo   Option A: Add Maven\bin to your system PATH
    echo   Option B: Set MAVEN_HOME environment variable
    echo.
    pause
    exit /b 1
)

echo Maven found: !MAVEN_CMD!
echo ✓ Maven located
echo.

echo [3/4] Building project (this may take a few minutes)...
call !MAVEN_CMD! clean package -DskipTests
if %errorlevel% neq 0 (
    echo ERROR: Build failed
    pause
    exit /b 1
)
echo ✓ Build completed successfully
echo.

echo [4/4] Starting Spring Boot application...
echo.
echo ========================================
echo Application is starting...
echo Access it at: http://localhost:8080
echo Press Ctrl+C to stop the application
echo ========================================
echo.

call !MAVEN_CMD! spring-boot:run

pause
endlocal
