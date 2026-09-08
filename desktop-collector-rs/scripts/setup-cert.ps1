# 코드 서명용 자체 서명 인증서를 한 번 만든다.
#
# 유료 인증서(EV/OV)를 쓰지 않는 대신, 우리가 만든 인증서로 서명하고 참가자가
# 그 인증서를 한 번 신뢰 목록에 넣는 방식이다. 폐쇄된 그룹 배포에만 맞는 방법이다.
#
#   .\scripts\setup-cert.ps1
#
# 만들어지는 것:
#   %USERPROFILE%\.lol-collector\codesign.pfx   개인키. 저장소에 절대 넣지 않는다.
#   %USERPROFILE%\.lol-collector\password.txt   pfx 비밀번호.
#   cert\LoL-Collector.cer                      공개 인증서. 참가자에게 배포한다.
#   cert\trust-cert.bat                         참가자가 관리자 권한으로 1회 실행.
#
# 인증서는 3년짜리다. 만료되면 이 스크립트를 다시 돌리고 참가자도 다시 등록해야 한다.

$ErrorActionPreference = "Stop"

$Subject = "CN=gijun.net, O=gijun.net, C=KR"
$FriendlyName = "LoL Collector Code Signing"
$Years = 3

$secretDir = Join-Path $env:USERPROFILE ".lol-collector"
$pfxPath = Join-Path $secretDir "codesign.pfx"
$passPath = Join-Path $secretDir "password.txt"

$repoRoot = Split-Path -Parent $PSScriptRoot
$certDir = Join-Path $repoRoot "cert"
$cerPath = Join-Path $certDir "LoL-Collector.cer"
$trustBat = Join-Path $certDir "trust-cert.bat"

if (Test-Path $pfxPath) {
  Write-Host "이미 인증서가 있습니다: $pfxPath" -ForegroundColor Yellow
  Write-Host "새로 만들려면 먼저 지우세요. 단, 참가자 전원이 새 인증서를 다시 등록해야 합니다."
  exit 1
}

New-Item -ItemType Directory -Force -Path $secretDir | Out-Null
New-Item -ItemType Directory -Force -Path $certDir | Out-Null

Write-Host "인증서 생성 중..." -ForegroundColor Cyan
$cert = New-SelfSignedCertificate `
  -Type CodeSigningCert `
  -Subject $Subject `
  -FriendlyName $FriendlyName `
  -CertStoreLocation "Cert:\CurrentUser\My" `
  -KeyExportPolicy Exportable `
  -KeyLength 3072 `
  -KeyAlgorithm RSA `
  -HashAlgorithm SHA256 `
  -NotAfter (Get-Date).AddYears($Years)

# pfx 비밀번호는 무작위로 만들고 파일로만 남긴다. 사람이 외울 일이 없다.
$bytes = New-Object byte[] 24
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
$plainPassword = [Convert]::ToBase64String($bytes)
$securePassword = ConvertTo-SecureString -String $plainPassword -Force -AsPlainText

Export-PfxCertificate -Cert $cert -FilePath $pfxPath -Password $securePassword | Out-Null
Set-Content -Path $passPath -Value $plainPassword -Encoding UTF8 -NoNewline
Export-Certificate -Cert $cert -FilePath $cerPath | Out-Null

# 개인키가 든 파일은 나만 읽을 수 있게 잠근다.
icacls $pfxPath /inheritance:r /grant:r "$($env:USERNAME):(R,W)" | Out-Null
icacls $passPath /inheritance:r /grant:r "$($env:USERNAME):(R,W)" | Out-Null

$batContent = @"
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
"@
Set-Content -Path $trustBat -Value $batContent -Encoding UTF8

Write-Host ""
Write-Host "완료" -ForegroundColor Green
Write-Host "  개인키(비공개): $pfxPath"
Write-Host "  공개 인증서   : $cerPath"
Write-Host "  참가자 배포용 : $trustBat"
Write-Host ""
Write-Host "지문(SHA1): $($cert.Thumbprint)"
Write-Host ""
Write-Host "다음 단계: cert\ 의 두 파일을 참가자에게 주고, trust-cert.bat 을"
Write-Host "관리자 권한으로 한 번 실행하게 하세요. release.ps1 이 이제부터 exe 를 서명합니다."
