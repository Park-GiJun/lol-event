# lol-event Project Instructions

## Electron 앱 릴리즈 워크플로우

electron-collector 프로젝트에 코드 변경 후 빌드/배포가 필요할 때, **반드시 아래 순서를 정확히 따라야 한다.**

### 릴리즈 절차 (순서 엄수)

**1단계: 버전 올리기**

`electron-collector/package.json`의 `version` 필드를 patch 단위로 올린다 (예: `1.0.3` → `1.0.4`).

```bash
# 현재 버전 확인
cat electron-collector/package.json | grep '"version"'
```

Edit 도구로 package.json의 version을 직접 수정한다. `npm version` 명령은 사용하지 않는다.

**2단계: 빌드**

```bash
cd electron-collector && npm run dist:win
```

빌드 결과물은 `installer/` 디렉토리에 생성된다 (`lol-collector-setup.exe`).

빌드 실패 시 중단하고 오류를 사용자에게 보고한다. 빌드 성공 확인 후에만 다음 단계로 진행한다.

**3단계: 커밋**

변경된 파일을 스테이징하고 커밋한다:

```bash
git add electron-collector/package.json installer/
git commit -m "release(electron): v{NEW_VERSION}"
```

커밋 메시지 형식: `release(electron): v1.0.4`

**4단계: 푸시**

```bash
git push origin master
```

### 규칙

- 버전 올리기 → 빌드 → 커밋 → 푸시 순서를 절대 바꾸지 않는다.
- 빌드가 성공해야만 커밋/푸시를 진행한다.
- 빌드 없이 커밋/푸시만 하지 않는다.
- 버전은 반드시 patch 단위(세 번째 숫자)로 올린다. major/minor 변경은 사용자가 명시적으로 요청할 때만 한다.
- `installer/` 디렉토리의 빌드 결과물도 항상 커밋에 포함한다.
