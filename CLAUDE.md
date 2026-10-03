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

- 정본은 웹 프론트엔드(`frontend/src/api/<도메인>/*Api.ts` — 함수와 응답 타입이 같은 파일에 있다)다.
  데스크탑 모델(`desktop-collector-rs/src/models.rs`)은 거기에 맞춘다.
- 백엔드가 경로·필드를 정리하는 중이라, 이름이 바뀔 만한 자리에는
  `#[serde(alias = ...)]` 로 옛 이름도 같이 받아 둔다.
- 비율 필드는 0~1 과 0~100 이 섞여 있다. `models::as_percent` 를 거쳐 쓴다.
- **경로와 JSON 필드 이름은 수집기가 기대는 계약이다.** 백엔드 구조를 바꿔도 이 둘은 그대로 둔다.
  응답 DTO 의 필드 이름은 `ResponseDtoContractTest` 가 지킨다.

---

## 백엔드 구조 (`backend/main-service`)

단일 모듈 헥사고날이다. 패키지 루트는 `com.gijun.main`.

```
domain/<집합>/{model,enums,service,exception}       순수 Kotlin. Spring·JPA 를 모른다.
application/
  port/in/            유즈케이스. 인터페이스 하나에 메서드 하나, 이름은 인터페이스와 같다.
                      파일은 주제별 복수형(`MatchUseCases.kt`).
  port/out/{persistence,cache,external,messaging,batch}
  handler/            `*QueryHandler` / `*CommandHandler`. 유즈케이스 여럿을 한 핸들러가 구현한다.
  dto/{command,query,result}   인자가 둘 이상인 조회는 Query 객체로 받는다.
infrastructure/
  adapter/in/web/<영역>/        `*WebAdapter` + `dto/`(`*Request.toCommand()`, `*Response.from()`)
  adapter/in/{messaging,runner,scheduler}
  adapter/out/persistence/<집합>/   `*JpaEntity` · `*JpaRepository` · `*PersistenceAdapter`
  adapter/out/{cache,external,messaging}   외부 HTTP 는 `*KtorPort` / `*KtorAdapter`
  batch/
shared/
  domain/{exception,vo}                  `DomainException`(sealed) · `ErrorCode` · `RiotId`/`MatchId`/`Puuid`
  infrastructure/{config,web/common}     `CommonApiResponse` · `GlobalExceptionHandler` · `TraceIdFilter`
```

- **예외**: `DomainException` 의 카테고리가 HTTP status 를 정하고, `ErrorCode` enum 이 코드의 정본이다.
  코드를 문자열로 쓰지 않는다. enum 상수는 `    NAME("설명"),` 형태를 지킨다 — 프론트 생성기가 읽는다.
- **응답**: 전부 `CommonApiResponse` 로 싼다. 웹 어댑터는 도메인 모델·Result 를 그대로 내보내지 않고
  `*Response.from()` 을 거친다.
- **입력 값**: 경기 모드는 `GameMode` enum(`GameModeConverter`), 식별자는 값 클래스로 받는다.
  모르는 값은 400 이다.
- **형식**: ktlint(`backend/.editorconfig`). 와일드카드 import 와 `!!` 를 쓰지 않는다.
  `./gradlew build` 가 ktlint · 테스트 · `checkErrorCodes` 를 다 돈다.
- 컨트롤러 클래스에는 `@RequestMapping(..., version = "1.0")` 를 붙인다. `X-API-Version` 헤더가
  없어도 닿는다 — 이미 배포된 수집기는 헤더를 보내지 않는다.

## 프론트 구조 (`frontend`)

패키지 매니저는 pnpm 이다. 게이트는 `pnpm lint`(prettier + eslint) · `pnpm test` · `pnpm build`(tsc 포함).

```
src/
  api/client.ts          axios. 봉투를 풀어 데이터만 돌려주고 실패는 ApiError 로 던진다.
  api/queryClient.ts     실패 토스트를 여기 한곳에서 낸다.
  api/<도메인>/*Api.ts    엔드포인트마다 함수 하나 + 그 응답 타입.
  app/                   AppShell 등 뼈대
  features/<도메인>/      화면
  components/common/     어디서나 쓰는 것
  components/<도메인>/    도메인에 묶인 공용 컴포넌트
  hooks/                 여러 화면이 같이 쓰는 Query 훅
  stores/                zustand
  lib/                   순수 함수 · 상수
  types/                 여러 도메인이 함께 쓰는 타입만(GameMode, SampleGrade)
  styles/                theme.css(토큰) → global.css(바탕) → components/*.css
```

- **서버 데이터는 Query/Mutation 으로만 받는다.** `useEffect` 안에서 조회하지 않는다.
  `queryFn` 은 `signal` 을 API 함수에 넘긴다.
- **화면은 실패 문구를 적지 않는다.** `queryClient.ts` 가 토스트로 낸다. 문구를 바꾸려면
  `meta.errorMessage`, 화면이 직접 말하려면 `meta: SILENT_META`, 저장 성공은 `meta.successMessage`.
- **주소는 `/api/...` 상대경로다.** dev 는 vite proxy, 운영은 nginx 가 넘긴다. 붙일 백엔드는
  `.env.local` 의 `VITE_DEV_BE` 로 바꾼다.
- **에러 코드**는 `errorCodes.generated.ts` 를 고치지 않는다. 백엔드에서
  `./gradlew generateErrorCodes` 로 다시 만들고, 문구는 `errorMessages.ts` 에 채운다
  (빠뜨리면 타입이 안 맞는다).
- import 는 `@/` 별칭으로 쓴다. CSS 는 `index.css` 에서만 불러온다.

---

## AI (RAG · 챗봇 · 팀 짜기)

LLM 은 집 안 장비(BC-250)의 llama.cpp 서버 둘이다 — 채팅 `:8080`(모델 `qwen`), 임베딩 `:8081`(bge-m3, 1024 차원).
Koog 로 붙는다. **스위치는 없다 — 항상 켜져 있다.** 그 장비가 꺼져 있어도 서비스는 그대로 뜨고
(클라이언트는 부를 때 연결한다), AI 엔드포인트만 409 `RAG_UNAVAILABLE` 을 낸다.

### 원칙 — 숫자는 모델이 만들지 않는다

- **검색 문서**(`lol_event.rag_documents`)의 글은 `RagDocumentWriter` 가 통계 유즈케이스의 결과로 찍어낸다.
  LLM 으로 글을 만들지 않는다.
- **챗봇**은 전적·승률·Elo 를 tool(`LolAgentTools`)로만 읽는다. tool 은 저장된 문서가 아니라 지금 통계로 글을 쓴다.
- **팀 편성**은 `TeamBalancer` 가 계산한다(팀 평균 라인 Elo 를 맞춘다). LLM 은 확정된 편성의 해설만 쓴다.
  개인 지표나 시너지를 편성 점수에 섞지 않는다 — `RatingValidationResult` 의 검증에서 전부 기준선보다 나빴다.

### 흐름

| 일 | 입구 | 하는 곳 |
|---|---|---|
| 경기가 저장되면 그 경기·사람·챔피언 문서를 다시 쓴다 | Kafka `lol.rag.index` → `RagIndexConsumer` | `RagIndexCommandHandler` |
| 전체를 다시 쓴다(뒤에서 돈다) | `POST /api/admin/rag/reindex`, `GET /api/admin/rag/status` | `RagIndexCommandHandler` |
| 질문에 답한다 | `POST /api/rag/chat` | `RagChatCommandHandler` → `KoogLlmChatAdapter` |
| 팀을 짠다 | `GET /api/team-build/candidates`, `POST /api/team-build` | `TeamBuildCommandHandler` |

글이 그대로인 문서는 임베딩을 건너뛴다. 그래서 전체 색인을 여러 번 돌려도 바뀐 문서만 임베딩 서버에 간다.

### 고칠 때

- **tool 을 더하거나 설명을 고치면** `RAG_SMOKE=1 ./gradlew :main-service:test --tests '*KoogAgentSmokeTest' -i`
  로 실제 모델이 맞는 tool 을 부르는지 본다(집 안 망에서만 돈다). tool 설명은 모델이 읽는 글이다 —
  한국어 예시까지 적는다.
- **임베딩 모델을 바꾸면** `rag.embedding.dimensions` 와 `rag_documents.embedding` 의 차원을 같이 바꾸고
  전체를 다시 색인한다. 다른 모델의 벡터끼리는 거리가 뜻이 없다.
- 응용 계층은 Koog 를 직접 받지 않고 포트(`TextEmbeddingPort`, `LlmChatPort`, `LlmCompletionPort`)만 받는다.
- Postgres 는 `pgvector/pgvector` 이미지여야 한다. 확장이 없으면 `V21` 에서 마이그레이션이 실패해 서비스가 뜨지 않는다.
  벡터 컬럼과 연산자는 `public.vector`, `OPERATOR(public.<=>)` 로 스키마를 붙여 쓴다.
