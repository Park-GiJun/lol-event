# lol-event Project Instructions

## Desktop 앱 릴리즈 워크플로우

desktop-collector (Compose Desktop) 프로젝트에 코드 변경 후 빌드/배포가 필요할 때, **반드시 아래 순서를 정확히 따라야 한다.**

### 릴리즈 절차 (순서 엄수)

**1단계: 버전 올리기**

`desktop-collector/build.gradle.kts`의 `version` 필드를 patch 단위로 올린다 (예: `1.0.0` → `1.0.1`).

```bash
# 현재 버전 확인
grep 'version =' desktop-collector/build.gradle.kts
```

Edit 도구로 build.gradle.kts의 version과 Main.kt의 APP_VERSION을 함께 수정한다.

**2단계: 빌드**

```bash
cd desktop-collector && JAVA_HOME="$HOME/.jdks/ms-21.0.10" ./gradlew packageMsi
```

빌드 결과물은 `build/compose/binaries/main/msi/` 디렉토리에 생성된다.

빌드 실패 시 중단하고 오류를 사용자에게 보고한다. 빌드 성공 확인 후에만 다음 단계로 진행한다.

**3단계: 커밋**

변경된 파일을 스테이징하고 커밋한다:

```bash
git add desktop-collector/build.gradle.kts desktop-collector/src/
git commit -m "release(desktop): v{NEW_VERSION}"
```

커밋 메시지 형식: `release(desktop): v1.0.1`

**4단계: 푸시**

```bash
git push origin master
```

### 규칙

- 버전 올리기 → 빌드 → 커밋 → 푸시 순서를 절대 바꾸지 않는다.
- 빌드가 성공해야만 커밋/푸시를 진행한다.
- 빌드 없이 커밋/푸시만 하지 않는다.
- 버전은 반드시 patch 단위(세 번째 숫자)로 올린다. major/minor 변경은 사용자가 명시적으로 요청할 때만 한다.
- JAVA_HOME은 JDK 21을 사용한다.
