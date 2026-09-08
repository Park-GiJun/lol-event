# lol-event Project Instructions

## 데스크탑 앱 구조

본체는 **Rust 단일 exe** 다. 설치 프로그램(MSI/NSIS)이 없다.

```
desktop-collector-rs/   ← 본체. 이걸 고친다.
  - Rust + egui. 산출물은 LoL-Collector.exe 하나뿐.
  - 첫 실행 때 스스로를 %LOCALAPPDATA%\LoL-Collector 로 복사하고 바로가기를 만든다.
  - 업데이트도 스스로 한다 (GitHub Releases 조회 → exe 교체 → 재시작).

desktop-launcher/       ← 최초 설치용 부트스트랩. 거의 변하지 않는다.
  - LoL-Collector-Launcher.msi (upgradeUuid: f7e8d9c0-1234-5678-9abc-def012345678)
  - 본체가 이미 깔려 있으면 그냥 실행하고 끝.
  - 없으면 GitHub Releases 에서 exe 를 받아 임시 폴더에서 한 번 실행한다.
    그 다음은 본체가 알아서 설치한다. 버전 비교 로직이 여기 없는 이유다.

desktop-collector/      ← 옛 Kotlin/Compose 본체. 더 이상 쓰지 않는다.
  - 참고용으로만 남겨 뒀다. 새 기능을 여기 넣지 않는다.
```

### 왜 설치 프로그램을 없앴나

예전에는 런처가 MSI 를 받아 `msiexec` 를 관리자 권한으로 돌렸다. 그래서 **업데이트할
때마다** UAC 승격 프롬프트와 "알 수 없는 게시자" 경고가 떴다. 지금은 exe 를 사용자
폴더에 두고 파일을 갈아 끼우기만 하므로 관리자 권한이 필요 없고, 경고가 뜰 일 자체가
없다. Program Files 아래에 설치하는 방식으로 되돌리면 이 문제가 그대로 돌아온다.

또 하나: 앱이 코드로 직접 내려받아 쓴 파일에는 Mark of the Web 이 붙지 않는다.
그래서 자동 업데이트로 받은 exe 는 SmartScreen 을 아예 거치지 않는다. 브라우저로
직접 받는 최초 1회만 SmartScreen 을 만난다.

---

## 본체 릴리즈

```powershell
cd desktop-collector-rs
.\release.ps1
```

`release.ps1` 이 하는 일:
1. patch 버전 자동 증가 (`Cargo.toml`)
2. git add / commit (`release(collector): v{NEW}`) / push origin master
3. `cargo test` → 실패하면 릴리즈 중단
4. `cargo build --release`
5. 자체 서명 인증서로 exe 서명 (인증서가 없으면 경고만 남기고 통과)
6. 같은 버전의 기존 GitHub Release/태그가 있으면 삭제
7. `gh release create desktop-v{NEW}` 로 exe 업로드 + `--latest`

옵션: `-NoBump` (버전 유지), `-SkipPublish` (빌드·서명까지만).

### 규칙

- 릴리즈는 반드시 `release.ps1` 로 한다. 수동 단계를 끼워넣지 않는다.
- 태그는 반드시 `desktop-v` 접두사, 자산명은 `LoL-Collector.exe`.
  둘 다 런처(`LauncherMain.kt`)와 자동 업데이터(`src/updater.rs`)가 의존하는 규약이다.
  한쪽만 바꾸면 업데이트가 조용히 멈춘다.
- 버전은 patch 단위로 오른다. major/minor 를 올리려면 `Cargo.toml` 을 먼저 손으로
  고치고 `.\release.ps1 -NoBump` 를 돌린다.
- 본체 릴리즈만 `--latest` 로 표시한다.

---

## 코드 서명

유료 인증서 대신 자체 서명 인증서를 쓴다. 폐쇄된 그룹 배포라 가능한 방식이다.

```powershell
cd desktop-collector-rs
.\scripts\setup-cert.ps1     # 최초 1회. 3년짜리 인증서를 만든다.
```

- 개인키(`codesign.pfx`)는 `%USERPROFILE%\.lol-collector\` 에 있다. **저장소에 넣지 않는다.**
- 공개 인증서와 `trust-cert.bat` 은 `cert\` 에 생기고, 릴리즈 자산으로 같이 올라간다.
  (`dist\` 는 루트 `.gitignore` 에 걸려 커밋되지 않으므로 쓰지 않는다.)
- 참가자는 `trust-cert.bat` 을 관리자 권한으로 **한 번만** 실행하면 게시자 경고가 사라진다.
- 인증서가 만료되면 스크립트를 다시 돌리고 참가자도 다시 등록해야 한다.

주의: 자체 서명은 "알 수 없는 게시자" 표시를 없애 줄 뿐, SmartScreen 평판까지
주지는 않는다. 평판이 필요하면 Azure Trusted Signing(월 $10 수준)이나 EV 인증서를
사야 한다.

---

## 런처 릴리즈

런처는 거의 변하지 않는다. 변경이 필요할 때만 실행한다.

```bat
cd desktop-launcher
release.bat
```

### 규칙

- 런처 태그는 반드시 `launcher-v` 접두사. **본체와 다른 접두사**여야 한다 —
  런처와 자동 업데이터가 `desktop-v` 접두사만 보기 때문에, 런처 릴리즈가
  본체 업데이트로 잘못 잡히지 않는다.
- 런처 릴리즈는 절대 `--latest` 로 표시하지 않는다. "최신"은 항상 본체다.
- 런처와 본체는 다른 `upgradeUuid` 를 쓴다.
- 런처에 버전 비교나 설치 로직을 다시 넣지 않는다. 업데이트 책임은 전적으로 본체에 있다.

---

## 백엔드 API

- 정본은 웹 프론트엔드(`frontend/src/hooks/*`, `frontend/src/lib/types/stats.ts`)다.
  데스크탑 모델(`desktop-collector-rs/src/models.rs`)은 거기에 맞춘다.
- 백엔드가 경로·필드를 정리하는 중이라, 이름이 바뀔 만한 자리에는
  `#[serde(alias = ...)]` 로 옛 이름도 같이 받아 둔다.
- 비율 필드는 0~1 과 0~100 이 섞여 있다. `models::as_percent` 를 거쳐 쓴다.
