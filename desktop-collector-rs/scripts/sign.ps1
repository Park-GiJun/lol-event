# 파일 하나에 Authenticode 서명을 붙인다.
#
#   .\scripts\sign.ps1 -Path target\release\LoL-Collector.exe
#
# 인증서가 없으면 경고만 남기고 통과한다. 서명은 있으면 좋은 것이지, 없다고
# 릴리즈를 막을 일은 아니다 (설치 프로그램이 없으니 UAC 도 안 뜬다).
# signtool 대신 PowerShell 기본 기능을 쓴다 — Windows SDK 설치가 필요 없다.

param(
  [Parameter(Mandatory = $true)][string]$Path
)

$ErrorActionPreference = "Stop"

$secretDir = Join-Path $env:USERPROFILE ".lol-collector"
$pfxPath = Join-Path $secretDir "codesign.pfx"
$passPath = Join-Path $secretDir "password.txt"

if (-not (Test-Path $pfxPath)) {
  Write-Host "서명 건너뜀 — 인증서가 없습니다 (scripts\setup-cert.ps1 로 만들 수 있습니다)" -ForegroundColor Yellow
  exit 0
}
if (-not (Test-Path $Path)) {
  throw "서명할 파일이 없습니다: $Path"
}

# Windows PowerShell 5.1 의 Get-PfxCertificate 에는 -Password 가 없다.
# X509Certificate2 를 직접 만들어 개인키까지 들고 온다.
$password = (Get-Content $passPath -Raw).Trim()
$cert = New-Object System.Security.Cryptography.X509Certificates.X509Certificate2 `
  @($pfxPath, $password, "Exportable,PersistKeySet")
if (-not $cert.HasPrivateKey) {
  throw "pfx 에 개인키가 없습니다: $pfxPath"
}

# 타임스탬프를 박아 두면 인증서가 만료된 뒤에도 서명이 유효하게 남는다.
$result = Set-AuthenticodeSignature `
  -FilePath $Path `
  -Certificate $cert `
  -HashAlgorithm SHA256 `
  -TimestampServer "http://timestamp.digicert.com"

if ($result.Status -ne "Valid" -and $result.Status -ne "UnknownError") {
  Write-Host "서명 실패: $($result.Status) - $($result.StatusMessage)" -ForegroundColor Red
  exit 1
}

Write-Host "서명 완료: $Path ($($result.SignerCertificate.Subject))" -ForegroundColor Green
