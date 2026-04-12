@echo off
chcp 65001 >nul 2>&1
setlocal enabledelayedexpansion

set JAVA_HOME=C:\Users\tpgj9\.jdks\ms-25.0.2
set PATH=%PATH%;C:\Program Files\GitHub CLI;C:\Program Files (x86)\GitHub CLI

REM === Step 0: Auto bump patch version ===
for /f "tokens=3 delims= " %%v in ('findstr /C:"version = " build.gradle.kts') do set CUR_VER=%%~v
set CUR_VER=%CUR_VER:"=%

for /f "tokens=1,2,3 delims=." %%a in ("%CUR_VER%") do (
    set /a PATCH=%%c+1
    set NEW_VER=%%a.%%b.!PATCH!
)

echo Bumping version: %CUR_VER% -^> %NEW_VER%

REM Update build.gradle.kts (UTF8 encoding to preserve Korean)
powershell -NoProfile -Command "(Get-Content -Encoding UTF8 build.gradle.kts) -replace 'version = \"%CUR_VER%\"', 'version = \"%NEW_VER%\"' | Set-Content -Encoding UTF8 build.gradle.kts"

REM Update Main.kt (UTF8 encoding to preserve Korean)
powershell -NoProfile -Command "(Get-Content -Encoding UTF8 src\main\kotlin\net\gijun\collector\Main.kt) -replace 'APP_VERSION = \"%CUR_VER%\"', 'APP_VERSION = \"%NEW_VER%\"' | Set-Content -Encoding UTF8 src\main\kotlin\net\gijun\collector\Main.kt"

REM Git commit the version bump
cd ..
git add desktop-collector/build.gradle.kts desktop-collector/src/
git commit -m "release(desktop): v%NEW_VER%"
git push origin master
cd desktop-collector

echo.
echo === Step 1: Build MSI ===
call gradlew.bat packageMsi
if errorlevel 1 (
    echo BUILD FAILED
    exit /b 1
)

set MSI_FILE=
for %%f in (build\compose\binaries\main\msi\*.msi) do set MSI_FILE=%%f

if "%MSI_FILE%"=="" (
    echo No MSI file found
    exit /b 1
)

for /f "tokens=*" %%v in ('powershell -NoProfile -Command "if('%MSI_FILE%' -match '(\d+\.\d+\.\d+)'){$matches[1]}"') do set VERSION=%%v
if "%VERSION%"=="" set VERSION=%NEW_VER%

echo.
echo === Step 2: Upload to GitHub Releases ===
echo MSI: %MSI_FILE%
echo Version: v%VERSION%
echo.

gh --version >nul 2>&1
if errorlevel 1 (
    echo gh CLI not found in PATH.
    echo Restart terminal or: set PATH=%%PATH%%;C:\Program Files\GitHub CLI
    exit /b 1
)

gh release delete desktop-v%VERSION% --yes 2>nul
git tag -d desktop-v%VERSION% 2>nul
git push origin :refs/tags/desktop-v%VERSION% 2>nul

gh release create desktop-v%VERSION% "%MSI_FILE%" --title "Desktop Collector v%VERSION%" --notes "LoL Collector v%VERSION%" --latest

if errorlevel 1 (
    echo Upload FAILED
    exit /b 1
)

echo.
echo === DONE ===
echo Release: desktop-v%VERSION%
echo TeamCity will auto-download on next deploy.

endlocal
