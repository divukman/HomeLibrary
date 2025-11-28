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
set JAR_FILE=target\home-library-1.0.0.jar
set LIB_DIR=target\lib

REM Check if JAR exists, if not try to build
if not exist "%JAR_FILE%" (
    echo JAR file not found. Attempting to build...
    where mvn >nul 2>&1
    if %ERRORLEVEL% neq 0 (
        echo Maven not found. Please run: mvn clean package
        pause
        exit /b 1
    )
    call mvn clean package -DskipTests
    if %ERRORLEVEL% neq 0 (
        echo Build failed!
        pause
        exit /b 1
    )
)

REM Check if lib directory exists
if not exist "%LIB_DIR%" (
    echo Dependencies not found: %LIB_DIR%
    echo Please run: mvn clean package
    pause
    exit /b 1
)

REM Build module path for JavaFX
set MODULE_PATH=%LIB_DIR%\javafx-controls-21.0.1.jar;%LIB_DIR%\javafx-graphics-21.0.1.jar;%LIB_DIR%\javafx-base-21.0.1.jar;%LIB_DIR%\javafx-fxml-21.0.1.jar

REM Run the application
echo Starting Home Library Application...
"%JAVA_CMD%" --module-path "%MODULE_PATH%" --add-modules javafx.controls,javafx.fxml -jar "%JAR_FILE%"

if %ERRORLEVEL% neq 0 (
    echo Application exited with error code: %ERRORLEVEL%
    pause
)
