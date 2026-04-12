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

echo Bumping launcher version: %CUR_VER% -^> %NEW_VER%

REM Update build.gradle.kts (UTF8)
powershell -NoProfile -Command "(Get-Content -Encoding UTF8 build.gradle.kts) -replace 'version = \"%CUR_VER%\"', 'version = \"%NEW_VER%\"' | Set-Content -Encoding UTF8 build.gradle.kts"

REM Update LauncherMain.kt LAUNCHER_VERSION (UTF8)
powershell -NoProfile -Command "(Get-Content -Encoding UTF8 src\main\kotlin\net\gijun\launcher\LauncherMain.kt) -replace 'LAUNCHER_VERSION = \"%CUR_VER%\"', 'LAUNCHER_VERSION = \"%NEW_VER%\"' | Set-Content -Encoding UTF8 src\main\kotlin\net\gijun\launcher\LauncherMain.kt"

REM Git commit the version bump
cd ..
git add desktop-launcher/build.gradle.kts desktop-launcher/src/
git commit -m "release(launcher): v%NEW_VER%"
git push origin master
cd desktop-launcher

echo.
echo === Step 1: Build Launcher MSI ===
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

gh release delete launcher-v%VERSION% --yes 2>nul
git tag -d launcher-v%VERSION% 2>nul
git push origin :refs/tags/launcher-v%VERSION% 2>nul

REM Launcher releases must NOT use --latest. The "latest" release must always
REM be a desktop-v* tag because the launcher's auto-update logic only looks
REM at desktop-v* tags. A launcher release marked latest would break body updates.
gh release create launcher-v%VERSION% "%MSI_FILE%" --title "LoL Collector Launcher v%VERSION%" --notes "Launcher v%VERSION% - manual download only, not auto-updated"

if errorlevel 1 (
    echo Upload FAILED
    exit /b 1
)

echo.
echo === DONE ===
echo Release: launcher-v%VERSION%
echo NOTE: Launcher MSI must be downloaded manually by users. It is not auto-updated.

endlocal
