@echo off
chcp 65001 >nul 2>&1
setlocal

set JAVA_HOME=C:\Users\tpgj9\.jdks\ms-25.0.2

echo === Step 1: Build MSI ===
call gradlew.bat packageMsi
if errorlevel 1 (
    echo BUILD FAILED
    exit /b 1
)

for %%f in (build\compose\binaries\main\msi\*.msi) do set MSI_FILE=%%f
for /f "tokens=2 delims=-" %%v in ("%MSI_FILE%") do set RAW_VER=%%v
set VERSION=%RAW_VER:.msi=%

echo.
echo === Step 2: Upload to GitHub Releases ===
echo MSI: %MSI_FILE%
echo Version: v%VERSION%
echo.

where gh >nul 2>&1
if errorlevel 1 (
    echo gh CLI not found. Install: winget install GitHub.cli
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
