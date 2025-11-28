@echo off
REM Home Library Manager - Run Script for Windows

echo Starting Home Library Manager...

REM Check if Maven is installed
where mvn >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Error: Maven is not installed or not in PATH
    echo Please install Maven from https://maven.apache.org/
    pause
    exit /b 1
)

REM Check if Java is installed
where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Error: Java is not installed or not in PATH
    echo Please install Java 17 or higher
    pause
    exit /b 1
)

REM Build if necessary
if not exist "target\home-library-1.0.0.jar" (
    echo Building application...
    call mvn clean package -DskipTests
)

REM Run the application
echo Launching application...
call mvn javafx:run

pause
