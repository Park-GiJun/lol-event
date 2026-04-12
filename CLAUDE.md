# lol-event Project Instructions

## 자동 업데이트 아키텍처

데스크탑은 **런처 패턴**을 사용한다. 두 개의 독립된 Gradle 프로젝트와 두 개의 MSI가 존재한다.

```
desktop-launcher/    ← 사용자가 실행하는 진입점 (바탕화면 바로가기)
  - LoL-Collector-Launcher.msi (upgradeUuid: f7e8d9c0-1234-5678-9abc-def012345678)
  - 시작 시 GitHub Releases 조회 → 본체 신버전 있으면 자동 다운로드 & msiexec /qn 설치 → 본체 실행
  - 자체 업데이트는 없음. 거의 변하지 않는다.

desktop-collector/   ← 실제 본체 앱
  - LoL-Collector-{version}.msi (upgradeUuid: a1b2c3d4-e5f6-7890-abcd-ef1234567890)
  - 본체 코드에는 자동 업데이트 로직이 일절 없다 (런처가 모두 처리).
```

**왜 두 개의 MSI인가:** 본체 MSI 재설치가 런처 파일을 건드리지 않아야 함. 같은 MSI라면 런처가 자기 자신을 교체하려 하다가 깨진다.

**업데이트 소스:** GitHub Releases
- 본체 릴리즈 태그는 `desktop-v{version}` 패턴 (예: `desktop-v1.0.6`).
- 런처는 `https://api.github.com/repos/Park-GiJun/lol-event/releases`에서 `desktop-v` 접두사를 가진 최신 release를 선택한다.
- MSI 자산명은 `LoL-Collector-{version}.msi`.

---

## 본체(desktop-collector) 릴리즈

본체 변경 후 배포할 때는 **`desktop-collector/release.bat` 한 번 실행하면 끝**이다.

```bat
cd desktop-collector
release.bat
```

`release.bat`이 자동으로 처리하는 것:
1. patch 버전 자동 증가 (`build.gradle.kts`의 `version`, `Main.kt`의 `APP_VERSION` 동시 갱신)
2. git add / commit (`release(desktop): v{NEW}`) / push origin master
3. `gradlew.bat packageMsi`로 MSI 빌드
4. 같은 버전의 기존 GitHub Release/태그가 있으면 삭제
5. `gh release create desktop-v{NEW}` 로 GitHub Release 생성 + MSI 업로드 + `--latest` 플래그

### 규칙

- 릴리즈는 반드시 `release.bat`로 한다. 수동 단계 끼워넣지 않는다.
- 빌드가 실패하면 스크립트가 자체적으로 멈춘다. 실패 메시지 그대로 사용자에게 보고한다.
- 버전은 반드시 patch 단위로 올라간다. major/minor를 올리려면 `build.gradle.kts`와 `Main.kt`를 수동으로 먼저 조정한 뒤 `release.bat`을 돌려야 한다 (스크립트는 patch만 자동 증가).
- 태그는 반드시 `desktop-v` 접두사. 자산명은 `LoL-Collector-{version}.msi`. 둘 다 런처가 의존하는 규약이라 바꾸면 안 된다.
- 본체에는 자동 업데이트 코드를 다시 추가하지 않는다. 업데이트 책임은 전적으로 런처에 있다.

---

## 런처(desktop-launcher) 릴리즈

런처는 거의 변하지 않는다. 변경이 필요할 때만 `desktop-launcher/release.bat`을 실행한다.

```bat
cd desktop-launcher
release.bat
```

`release.bat`이 자동으로 처리하는 것:
1. patch 버전 자동 증가 (`build.gradle.kts`의 `version`, `LauncherMain.kt`의 `LAUNCHER_VERSION` 동시 갱신)
2. git add / commit (`release(launcher): v{NEW}`) / push origin master
3. `gradlew.bat packageMsi`로 런처 MSI 빌드
4. 같은 버전의 기존 GitHub Release/태그가 있으면 삭제
5. `gh release create launcher-v{NEW}` 로 GitHub Release 생성 + MSI 업로드 (**`--latest` 플래그 없음**)

### 규칙

- 런처 태그는 반드시 `launcher-v` 접두사. **본체와 다른 접두사**여야 한다 — 런처의 자동 업데이트 로직은 `desktop-v` 접두사만 보기 때문에 런처 릴리즈가 본체 자동 업데이트에 잘못 잡히지 않는다.
- 런처 릴리즈는 절대 `--latest`로 표시하지 않는다. "최신" 릴리즈는 항상 본체(`desktop-v*`)여야 한다.
- 런처 MSI는 사용자가 직접 받는 진입점이다. 자동 배포되지 않는다.
- 런처와 본체는 반드시 다른 `upgradeUuid`를 사용한다 (build.gradle.kts에서 강제). 같으면 본체 MSI 재설치가 런처 파일을 건드려 런처가 깨진다.
