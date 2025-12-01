@echo off
REM Home Library Application Launcher for Windows

REM Find Java executable
set JAVA_CMD=java
where java >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo Java not found in PATH. Please install Java 17 or later.
    pause
    exit /b 1
)

REM Check Java version
for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VERSION=%%g
)
echo Using Java: %JAVA_VERSION%

REM Set the path to the JAR file
set JAR=home-library-1.0.0.jar
set LIB_DIR=lib

REM Check if JAR exists
if not exist "%JAR%" (
    echo JAR file not found: %JAR%
    echo Please ensure you have extracted the distribution package.
    pause
    exit /b 1
)

REM Check if lib directory exists
if not exist "%LIB_DIR%" (
    echo Dependencies not found: %LIB_DIR%
    echo Please ensure you have extracted the distribution package.
    pause
    exit /b 1
)

REM Run the application
echo Starting Home Library Application...

"%JAVA_CMD%" ^
  --module-path "%LIB_DIR%" ^
  --add-modules javafx.controls,javafx.fxml ^
  -jar "%JAR%"

if %ERRORLEVEL% neq 0 (
    echo.
    echo ERROR: Application failed to start
    echo.
    pause
)
