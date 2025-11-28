@echo off
REM Home Library - Release Package Creator for Windows
REM Creates a distributable package with JAR, dependencies, and launcher scripts

setlocal enabledelayedexpansion

set VERSION=1.0.0
set RELEASE_NAME=home-library-v%VERSION%
set RELEASE_DIR=release

echo =========================================
echo Home Library Release Package Creator
echo Version: %VERSION%
echo =========================================
echo.

REM Step 1: Check Maven
echo [1/5] Checking Maven...
where mvn >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo Error: Maven is required to create releases
    echo Please install Maven from https://maven.apache.org/
    pause
    exit /b 1
)
echo Done: Maven found
echo.

REM Step 2: Build project
echo [2/5] Building project...
call mvn clean package -DskipTests
if %ERRORLEVEL% neq 0 (
    echo Error: Build failed!
    pause
    exit /b 1
)
echo Done: Build successful
echo.

REM Step 3: Create release directory
echo [3/5] Creating release directory...
if exist "%RELEASE_DIR%" (
    rmdir /s /q "%RELEASE_DIR%"
)
mkdir "%RELEASE_DIR%\%RELEASE_NAME%"
echo Done: Release directory created
echo.

REM Step 4: Copy files
echo [4/5] Copying files...

REM Copy JAR
copy "target\home-library-%VERSION%.jar" "%RELEASE_DIR%\%RELEASE_NAME%\" >nul
echo   Done: Copied JAR file

REM Copy dependencies
xcopy "target\lib" "%RELEASE_DIR%\%RELEASE_NAME%\lib\" /E /I /Q >nul
echo   Done: Copied dependencies

REM Copy launcher scripts
copy "run.sh" "%RELEASE_DIR%\%RELEASE_NAME%\" >nul
copy "run.bat" "%RELEASE_DIR%\%RELEASE_NAME%\" >nul
echo   Done: Copied launcher scripts

REM Copy README
copy "README.md" "%RELEASE_DIR%\%RELEASE_NAME%\" >nul
echo   Done: Copied README

REM Create INSTALL.txt
(
echo Home Library - Installation Instructions
echo =========================================
echo.
echo REQUIREMENTS:
echo - Java 17 or higher
echo.
echo QUICK START:
echo.
echo   Linux/Mac:
echo     1. Extract this archive
echo     2. Open terminal in the extracted folder
echo     3. Run: ./run.sh
echo.
echo   Windows:
echo     1. Extract this archive
echo     2. Open Command Prompt in the extracted folder
echo     3. Run: run.bat
echo.
echo The application will start automatically!
echo.
echo DATABASE:
echo - The application creates a 'library.db' file in the same directory
echo - Cover images are stored in the 'covers/' directory
echo.
echo TROUBLESHOOTING:
echo - If you get "Java not found", install Java 17+ from:
echo   https://adoptium.net/
echo.
echo For more information, see README.md
) > "%RELEASE_DIR%\%RELEASE_NAME%\INSTALL.txt"

echo   Done: Created INSTALL.txt
echo.

REM Step 5: Create ZIP archive
echo [5/5] Creating release archive...

REM Check if PowerShell is available for creating ZIP
where powershell >nul 2>&1
if %ERRORLEVEL% equ 0 (
    powershell -Command "Compress-Archive -Path '%RELEASE_DIR%\%RELEASE_NAME%' -DestinationPath '%RELEASE_DIR%\%RELEASE_NAME%.zip' -Force"
    echo   Done: Created %RELEASE_NAME%.zip
) else (
    echo   Warning: PowerShell not found, cannot create ZIP automatically
    echo   Please manually compress the folder: %RELEASE_DIR%\%RELEASE_NAME%
)
echo.

REM Summary
echo =========================================
echo Release package created in: %RELEASE_DIR%\
echo.
echo Contents:
echo   - home-library-%VERSION%.jar
echo   - lib\ (dependencies^)
echo   - run.sh (Linux/Mac launcher^)
echo   - run.bat (Windows launcher^)
echo   - README.md
echo   - INSTALL.txt
echo.

if exist "%RELEASE_DIR%\%RELEASE_NAME%.zip" (
    echo Done: Release ready for distribution!
    echo.
    echo Files created:
    dir "%RELEASE_DIR%\*.zip" | find ".zip"
    echo.
    echo Next steps:
    echo   1. Test the release in: %RELEASE_DIR%\%RELEASE_NAME%
    echo   2. Upload ZIP file to GitHub releases
) else (
    echo Done: Release folder created!
    echo.
    echo Next steps:
    echo   1. Manually create ZIP from: %RELEASE_DIR%\%RELEASE_NAME%
    echo   2. Upload to GitHub releases
)

echo =========================================
echo.
pause
