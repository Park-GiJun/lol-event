@echo off
chcp 65001 >nul 2>&1
REM LoL 수집기 서명 인증서를 신뢰 목록에 등록한다.
REM 관리자 권한으로 한 번만 실행하면 된다.
REM 이걸 하면 앱을 실행할 때 "알 수 없는 게시자" 경고가 뜨지 않는다.

net session >nul 2>&1
if errorlevel 1 (
  echo 관리자 권한이 필요합니다.
  echo 이 파일을 마우스 오른쪽 클릭 - "관리자 권한으로 실행" 으로 다시 열어주세요.
  pause
  exit /b 1
)

certutil -addstore -f Root "%~dp0LoL-Collector.cer"
if errorlevel 1 goto fail
certutil -addstore -f TrustedPublisher "%~dp0LoL-Collector.cer"
if errorlevel 1 goto fail

echo.
echo 등록 완료. 이제 LoL 수집기를 실행해도 게시자 경고가 뜨지 않습니다.
pause
exit /b 0

:fail
echo.
echo 등록 실패. 위 메시지를 확인해주세요.
pause
exit /b 1
