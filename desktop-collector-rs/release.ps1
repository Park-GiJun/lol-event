# 본체 릴리즈. 이거 하나만 돌리면 끝난다.
#
#   cd desktop-collector-rs
#   .\release.ps1
#
# 하는 일:
#   1. patch 버전 자동 증가 (Cargo.toml)
#   2. git add / commit / push origin master
#   3. cargo build --release
#   4. 자체 서명 인증서로 exe 서명 (인증서가 없으면 건너뜀)
#   5. 같은 버전의 기존 릴리즈/태그가 있으면 삭제
#   6. gh release create desktop-v{version} 로 exe 업로드 (--latest)
#
# MSI 도 msiexec 도 없다. 앱이 스스로 exe 를 갈아 끼우므로 업데이트 때
# UAC 승격이 일어나지 않고, 그래서 보안 경고도 뜨지 않는다.

param(
  # 버전을 올리지 않고 현재 버전 그대로 다시 올릴 때.
  [switch]$NoBump,
  # 빌드와 서명까지만 하고 GitHub 에는 올리지 않을 때.
  [switch]$SkipPublish
)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

$env:PATH = "$env:PATH;$env:USERPROFILE\.cargo\bin;C:\Program Files\GitHub CLI;C:\Program Files (x86)\GitHub CLI"

# 네이티브 명령(cargo · git · gh)을 돌리고 종료 코드로만 성공을 판단한다.
#
# Windows PowerShell 5.1 은 ErrorActionPreference=Stop 일 때 네이티브 명령이
# stderr 에 한 줄만 뱉어도 종료 코드와 무관하게 스크립트를 죽인다. cargo 는
# 링커 경고를 stderr 로 흘리므로 그대로 두면 정상 빌드에서도 릴리즈가 멈춘다.
function Invoke-Native {
  param(
    [Parameter(Mandatory = $true)][string]$What,
    [Parameter(Mandatory = $true)][scriptblock]$Body
  )
  $previous = $ErrorActionPreference
  $ErrorActionPreference = "Continue"
  try { & $Body } finally { $ErrorActionPreference = $previous }
  if ($LASTEXITCODE -ne 0) { throw "$What 실패 (exit $LASTEXITCODE)" }
}

# ── 1. 버전 ──────────────────────────────────────
$cargoToml = "Cargo.toml"
$content = Get-Content $cargoToml -Raw -Encoding UTF8
if ($content -notmatch '(?m)^version = "(\d+)\.(\d+)\.(\d+)"') {
  throw "Cargo.toml 에서 version 을 찾지 못했습니다"
}
$major = [int]$Matches[1]; $minor = [int]$Matches[2]; $patch = [int]$Matches[3]
$current = "$major.$minor.$patch"

if ($NoBump) {
  $version = $current
  Write-Host "버전 유지: $version" -ForegroundColor Cyan
} else {
  $version = "$major.$minor.$($patch + 1)"
  Write-Host "버전 증가: $current -> $version" -ForegroundColor Cyan
  # 첫 번째 version 줄만 바꾼다. [dependencies] 안의 version 은 건드리면 안 된다.
  $content = [regex]::Replace($content, '(?m)^version = "\d+\.\d+\.\d+"', "version = `"$version`"", 1)
  Set-Content -Path $cargoToml -Value $content -Encoding UTF8 -NoNewline
}

# ── 2. 커밋 ──────────────────────────────────────
if (-not $NoBump) {
  Push-Location ..
  # 프로젝트 폴더를 통째로 넣는다. Cargo.toml 만 넣으면 소스 변경이 빠진 채
  # 릴리즈가 나간다. target\ 은 .gitignore 가 막는다.
  try {
    Invoke-Native "git add" { git add desktop-collector-rs }
    Invoke-Native "git commit" { git commit -m "release(collector): v$version" }
    Invoke-Native "git push" { git push origin master }
  } finally {
    Pop-Location
  }
}

# ── 3. 빌드 ──────────────────────────────────────
Write-Host ""
Write-Host "=== 테스트 ===" -ForegroundColor Cyan
Invoke-Native "테스트 — 릴리즈를 중단합니다" { cargo test --quiet }

Write-Host ""
Write-Host "=== 릴리즈 빌드 ===" -ForegroundColor Cyan
Invoke-Native "빌드" { cargo build --release }

$exe = "target\release\LoL-Collector.exe"
if (-not (Test-Path $exe)) { throw "exe 가 없습니다: $exe" }
$sizeMb = [math]::Round((Get-Item $exe).Length / 1MB, 1)
Write-Host "빌드 완료: $exe (${sizeMb}MB)" -ForegroundColor Green

# ── 4. 서명 ──────────────────────────────────────
Write-Host ""
Write-Host "=== 서명 ===" -ForegroundColor Cyan
Invoke-Native "서명" { & "$PSScriptRoot\scripts\sign.ps1" -Path $exe }

if ($SkipPublish) {
  Write-Host ""
  Write-Host "게시 건너뜀 (-SkipPublish). 산출물: $exe" -ForegroundColor Yellow
  exit 0
}

# ── 5~6. GitHub 릴리즈 ───────────────────────────
Invoke-Native "gh CLI 확인" { gh --version *> $null }

$tag = "desktop-v$version"
Write-Host ""
Write-Host "=== GitHub 릴리즈 $tag ===" -ForegroundColor Cyan

# 같은 버전을 다시 올리는 경우를 위해 기존 것을 먼저 치운다.
# 없으면 실패하는 게 정상이므로 종료 코드를 보지 않는다.
$ErrorActionPreference = "Continue"
gh release delete $tag --yes *> $null
git tag -d $tag *> $null
git push origin ":refs/tags/$tag" *> $null
$ErrorActionPreference = "Stop"

$assets = @($exe)
# 참가자가 서명을 신뢰하려면 공개 인증서도 같이 받아야 한다.
$cer = "cert\LoL-Collector.cer"
if (Test-Path $cer) { $assets += $cer }
$trust = "cert\trust-cert.bat"
if (Test-Path $trust) { $assets += $trust }

$notes = @"
LoL 수집기 v$version

설치 프로그램이 없습니다. exe 하나만 받아 실행하면 스스로 설치되고,
그 뒤로는 알아서 업데이트합니다. 관리자 권한이 필요 없습니다.

처음 쓰는 경우 ``trust-cert.bat`` 을 관리자 권한으로 한 번 실행하면
게시자 경고가 뜨지 않습니다.
"@

Invoke-Native "릴리즈 업로드" {
  gh release create $tag @assets --title "LoL 수집기 v$version" --notes $notes --latest
}

Write-Host ""
Write-Host "=== 완료 ===" -ForegroundColor Green
Write-Host "태그: $tag"
Write-Host "기존 사용자는 앱을 다시 켜면 자동으로 v$version 으로 올라갑니다."
