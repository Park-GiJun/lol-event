@echo off
setlocal

REM ──────────────────────────────────────────────
REM  Desktop Collector 릴리즈 스크립트
REM  빌드 → GitHub Release 업로드 → 자동 배포
REM ──────────────────────────────────────────────

set JAVA_HOME=C:\Users\tpgj9\.jdks\ms-25.0.2

echo === 1단계: MSI 빌드 ===
call gradlew.bat packageMsi
if errorlevel 1 (
    echo 빌드 실패!
    exit /b 1
)

REM 버전 추출
for %%f in (build\compose\binaries\main\msi\*.msi) do set MSI_FILE=%%f
for /f "tokens=2 delims=-" %%v in ("%MSI_FILE%") do set VERSION=%%v
set VERSION=%VERSION:.msi=%

echo.
echo === 2단계: GitHub Release 업로드 ===
echo MSI: %MSI_FILE%
echo 버전: v%VERSION%
echo.

REM gh CLI 확인
where gh >nul 2>&1
if errorlevel 1 (
    echo gh CLI가 설치되지 않았습니다.
    echo   winget install GitHub.cli
    echo 설치 후 다시 실행하세요.
    exit /b 1
)

REM 기존 릴리즈 삭제 (같은 태그가 있으면)
gh release delete desktop-v%VERSION% --yes 2>nul
git tag -d desktop-v%VERSION% 2>nul
git push origin :refs/tags/desktop-v%VERSION% 2>nul

REM 릴리즈 생성 + MSI 업로드
gh release create desktop-v%VERSION% "%MSI_FILE%" ^
    --title "Desktop Collector v%VERSION%" ^
    --notes "LoL 내전 수집기 v%VERSION% (Compose Desktop)" ^
    --latest

if errorlevel 1 (
    echo GitHub Release 업로드 실패!
    exit /b 1
)

echo.
echo === 완료 ===
echo GitHub Release: desktop-v%VERSION%
echo TeamCity 배포 시 자동으로 다운로드됩니다.
echo.

endlocal
