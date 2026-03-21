---
stepsCompleted: [1, 2, 3, 4, 5, 6, 7, 8]
lastStep: 8
status: 'complete'
completedAt: '2026-03-21'
inputDocuments:
  - '_bmad-output/planning-artifacts/prd.md'
  - '_bmad-output/planning-artifacts/ux-design-specification.md'
workflowType: 'architecture'
project_name: 'lol-event'
user_name: '박기준'
date: '2026-03-21'
---

# Architecture Decision Document

_This document builds collaboratively through step-by-step discovery. Sections are appended as we work through each architectural decision together._

## Project Context Analysis

### Requirements Overview

**Functional Requirements (28개 FRs):**

- **Dashboard 홈 & 탐색 (FR1-FR7):** 계층 구조(전체→챔피언→플레이어→매치) 탐색. EloLeaderboard, ChampionTierTable, 밴픽 트렌드를 홈에서 즉시 제공. Breadcrumb 기반 상위 계층 복귀. → 명확한 라우팅 계층과 계층별 컴포넌트 분리 필요.
- **Hover 통계 위젯 (FR8-FR9):** PlayerLink/ChampionLink — 기존 인터페이스 유지 필수, 스타일만 교체. 300ms 이내 응답 → React Query 캐싱 + Popover 컴포넌트.
- **데이터 시각화 (FR10-FR14):** 챔피언/플레이어 상세 페이지에만 Chart.js. 목록/홈은 순수 수치 카드. → 번들 분리(`React.lazy`) 아키텍처 결정 필요.
- **Electron 대기방 (FR15-FR20):** LCU API 이벤트 기반 자동 트리거 → 양팀 카드 2초 이내 렌더링. 클라이언트 미실행 시 graceful degradation.
- **모바일 웹 (FR21-FR25):** `/m/*` 전용 라우트. 공통 API 훅 재사용, UI 컴포넌트 별도 구현.
- **관리자 (FR26-FR28):** AdminPage, SyncPage, LcuPage, MonitoringPage 변경 없음. LCU 동기화 유지.

**Non-Functional Requirements (12개 NFRs):**

- **Performance (NFR1-NFR5):** Hover 300ms, Electron 2s, LCP 3s, Chart 2s, Chart.js 번들 제외(홈/목록)
- **Accessibility (NFR6-NFR8):** WCAG AA 4.5:1, 승/패 색+텍스트 병행, 모바일 터치 44px
- **Integration (NFR9-NFR10):** LCU graceful degradation, 클라이언트 패치 후 오류 로깅
- **Reliability (NFR11-NFR12):** 인라인 에러 + 재시도 버튼, Skeleton UI (CLS 방지)

**Scale & Complexity:**

- Primary domain: Full-stack Web + Desktop (Electron) + Mobile Web
- Complexity level: Medium (브라운필드, 소규모 사용자 50-60명, 내부 전용, 규제 없음)
- 플랫폼 수: 3 (Dashboard, Mobile `/m/*`, Electron)
- 예상 아키텍처 컴포넌트: ~15개 신규 커스텀 컴포넌트

### Technical Constraints & Dependencies

- **브라운필드 보존:** 기존 REST API 엔드포인트, 데이터 모델, LCU 동기화 로직 변경 불가
- **기존 코드 유지:** PlayerLink/ChampionLink(인터페이스), AdminPage, SyncPage, LcuPage, MonitoringPage
- **기술 스택 고정:** React + TypeScript + Tailwind CSS. shadcn/ui 추가 도입.
- **LCU API:** Electron 전용 로컬 Riot API — 클라이언트 실행 시에만 사용 가능
- **Chart.js:** 상세 페이지 한정 동적 임포트(`React.lazy`) — 번들 오염 방지
- **라우팅:** Dashboard(`/`, `/champions/*`, `/players/*`, `/matches/*`) vs 모바일(`/m/*`) 완전 분리

### Cross-Cutting Concerns Identified

1. **React Query 캐싱 전략** — 3개 플랫폼 공통 API 훅, 캐시 키 설계 및 stale time 정책
2. **공유 API 훅 레이어** — Dashboard/모바일이 같은 훅 재사용, Electron은 LCU 훅 추가
3. **디자인 시스템 토큰** — Tailwind CSS 커스텀 토큰(색상, 타이포그래피) 3개 플랫폼 공통 적용
4. **에러/로딩 처리** — Skeleton UI + 인라인 에러 패턴 모든 데이터 페치 컴포넌트에 일관 적용
5. **플랫폼 라우팅 분기** — `/m/*` 진입 감지 및 모바일 컴포넌트 트리 분기
6. **번들 최적화** — Chart.js dynamic import 패턴을 일관된 규칙으로 관리

## Foundation: Existing Technical Stack

### Project Type

Brownfield — 기존 React + TypeScript + Tailwind CSS 코드베이스. 새 프로젝트 초기화 불필요.

### Established Technical Decisions

**Language & Runtime:**
- TypeScript (전체 코드베이스)
- React (UI 프레임워크, 기존 유지)

**Styling Solution:**
- Tailwind CSS (기존) — 커스텀 토큰 추가 (색상, 타이포그래피)
- shadcn/ui (신규 추가) — headless 컴포넌트 라이브러리
- Lucide React (아이콘 — shadcn/ui 기본)

**Build Tooling:**
- Vite (기존 React 빌드 도구)
- electron-builder (`electron-collector` 패키지)

**Data Fetching & State:**
- React Query — 서버 상태 + 클라이언트 캐싱 (기존 유지)
- 기존 REST API 엔드포인트 유지

**Visualization:**
- Chart.js — 동적 임포트(`React.lazy`) 패턴으로 상세 페이지 한정

**Routing:**
- React Router — 기존 라우트 구조 유지, `/m/*` 모바일 분기 추가

**Desktop:**
- Electron (기존 `electron-collector` 패키지) — LCU API 연동 로직 유지

**Note:** 기존 코드베이스 위에서 작업하므로 패키지 초기화 없이 shadcn/ui CLI를 통해 컴포넌트를 선택적으로 추가하는 것이 첫 구현 스토리가 됩니다.

## Core Architectural Decisions

### Decision Priority Analysis

**Critical Decisions (구현 전 필수):**
- React Query 캐시 전략 및 Query Key 컨벤션
- 모바일 라우팅 분기 방식 (`/m/*`)
- 컴포넌트 폴더 구조 확장 방향

**Important Decisions (아키텍처 형태 결정):**
- 에러/로딩 공통 패턴
- Chart.js 동적 임포트 적용 규칙

**Deferred (Post-MVP):**
- Electron 자동 업데이트 — 현재 수동 배포 방식이 충분

### Data Architecture

**React Query 캐시 전략:**
- `staleTime: 5분` — 통계 데이터는 게임 후 동기화 주기로 빈번한 재요청 불필요
- `gcTime: 30분` — 리더보드/챔피언/플레이어/매치 전체 동일 적용
- Electron 대기방 데이터: `staleTime: 0` (대기방 진입 시 항상 최신 데이터)

**Query Key 컨벤션:**

```ts
['players']                    // 플레이어 목록
['players', playerId]          // 플레이어 상세
['champions']                  // 챔피언 목록
['champions', championId]      // 챔피언 상세
['leaderboard']                // Elo 리더보드
['matches', matchId]           // 매치 상세
['lobby', 'current']           // Electron 현재 대기방
['bans', 'recommend']          // 밴 추천 데이터
```

### Authentication & Security

**결정:** 인증 없음 — 내부 전용 도구(50-60명 고정 멤버). 기존 구조 유지.
**관리자 기능:** 기존 AdminPage 접근 방식 유지.

### Frontend Architecture

**컴포넌트 폴더 구조 (기존 type-based 유지 + 확장):**

```
frontend/src/
  components/
    common/           # 기존 공통 컴포넌트
    layout/           # 기존 레이아웃
    dashboard/        # 신규: EloLeaderboard, ChampionTierTable, StatHoverWidget
    electron/         # 신규: PlayerCard, BanRecommendBadge
    mobile/           # 신규: MobileBottomNav, 모바일 전용 컴포넌트
  pages/
    mobile/           # 기존 존재 — 확장 (MobileHome, MobileChampions 등)
  hooks/
    useLeaderboard.ts
    useChampions.ts
    usePlayers.ts
    useMatches.ts
    useLobby.ts       # Electron 전용
  lib/
    api/              # 기존 REST API 클라이언트 유지
    types/            # 기존 타입 유지
```

**모바일 라우팅 분기:**

```tsx
<Route path="/m" element={<MobileLayout />}>
  <Route index element={<MobileLeaderboard />} />
  <Route path="champions" element={<MobileChampions />} />
  <Route path="players/:id" element={<MobilePlayerDetail />} />
</Route>
```

**에러/로딩 공통 패턴 (NFR11, NFR12):**

```tsx
if (isLoading) return <Skeleton />
if (error) return <InlineError message={...} onRetry={refetch} />
return <Component data={data} />
```

**Chart.js 동적 임포트 규칙:**
- 적용 대상: 챔피언 상세(`/champions/:id`), 플레이어 상세(`/players/:id`) 페이지만
- 방식: `React.lazy` + `Suspense` wrapping
- 홈/목록/모바일 페이지: Chart.js import 금지

### Infrastructure & Deployment

**Dashboard 웹:** 자체 서버 배포 (기존 방식 유지)

**Electron:** 수동 배포 방식 유지 (CLAUDE.md 절차: 버전업 → 빌드 → 커밋 → 푸시). `installer/lol-collector-setup.exe` 수동 배포.

### Decision Impact Analysis

**구현 순서:**
1. shadcn/ui 셋업 + Tailwind 커스텀 토큰 정의
2. 공통 훅 레이어 (React Query 캐시 설정 + Query Key 컨벤션)
3. Dashboard 레이아웃 (사이드바 + 라우팅)
4. 핵심 컴포넌트 (EloLeaderboard, ChampionTierTable)
5. Hover 위젯 스타일 업데이트 (PlayerLink/ChampionLink)
6. Electron 대기방 뷰 (PlayerCard, 밴 추천)
7. 모바일 레이아웃 + 하단 탭바 (`/m/*`)

**크로스 컴포넌트 의존성:**
- React Query 훅 → Dashboard + 모바일 + Electron 공통 재사용
- Tailwind 토큰 → 3개 플랫폼 공통 적용
- Chart.js → 챔피언/플레이어 상세 페이지만 의존

## Implementation Patterns & Consistency Rules

### 잠재적 충돌 포인트 (8개 식별)

1. API 응답 언래핑 방식
2. 컴포넌트 export 방식 (named vs default)
3. CSS 클래스 vs Tailwind 혼용 기준
4. 파일/컴포넌트 네이밍 컨벤션
5. 에러/로딩 처리 위치 (컴포넌트 내부 vs 훅)
6. Hover 위젯 캐시 방식 (기존 Map vs React Query)
7. React Router 라우트 정의 위치
8. Electron LCU 이벤트 수신 패턴

### Naming Patterns

**파일명 컨벤션:**

```
components/  → PascalCase.tsx          (예: EloLeaderboard.tsx)
pages/       → PascalCasePage.tsx      (예: ChampionDetailPage.tsx)
hooks/       → useCamelCase.ts         (예: useLeaderboard.ts)
lib/types/   → camelCase.ts            (예: stats.ts, member.ts)
lib/api/     → camelCase.ts            (예: api.ts)
```

**컴포넌트/함수 네이밍:**

```ts
// ✅ Named export 사용 (default export 금지)
export function EloLeaderboard() { ... }
export function useLeaderboard() { ... }
```

**React Query 훅 네이밍:**

```ts
useLeaderboard()       // 리더보드
useChampions()         // 챔피언 목록
useChampion(id)        // 챔피언 상세
usePlayers()           // 플레이어 목록
usePlayer(riotId)      // 플레이어 상세
useMatches()           // 매치 목록
useMatch(id)           // 매치 상세
useLobby()             // Electron 대기방
useBanRecommend()      // 밴 추천
```

### API Patterns

**응답 처리 — 기존 `api.ts` wrapper 유지:**

```ts
// ApiResponse<T> = { success, data, message, errorCode }
// api.get/post/delete 자동으로 data 언래핑하여 반환
// 신규 API 함수는 반드시 lib/api/api.ts에 추가

// ✅ 올바른 패턴
const players = await api.get<Player[]>('/stats/players');

// ❌ fetch 직접 사용 금지 (lcuApi 제외)
```

**신규 API 엔드포인트 추가 위치:** `frontend/src/lib/api/api.ts`의 `api` 객체 확장

### CSS & Styling Patterns

**기준 원칙:**

```
신규 컴포넌트 (dashboard/, electron/, mobile/):
  → Tailwind CSS 유틸리티 클래스 우선 사용
  → 복잡한 상태/애니메이션만 CSS 변수로 처리

기존 컴포넌트 (PlayerLink, ChampionLink, popup-*):
  → 기존 커스텀 CSS 클래스 유지 (수정 금지)
  → 스타일 업데이트 시 기존 클래스명 보존
```

**색상 사용 규칙:**

```tsx
// CSS 변수 — 색상 의미가 있는 곳
style={{ color: 'var(--color-win)' }}     // 승리
style={{ color: 'var(--color-loss)' }}    // 패배
style={{ color: 'var(--color-primary)' }} // 강조(Teal)

// Tailwind — 레이아웃/구조적 스타일
className="flex items-center gap-2 p-4 rounded-lg"
```

**티어 색상 매핑:**

```ts
const TIER_COLORS = {
  S: '#FFD700',
  A: '#00B4D8',
  B: '#8899BB',
  C: '#4A5568',
} as const;
```

### Structure Patterns

**신규 컴포넌트 배치:**

```
components/dashboard/   → EloLeaderboard, ChampionTierTable, BanTrendCard, StatHoverWidget
components/electron/    → PlayerCard, BanRecommendBadge, LobbyView
components/mobile/      → MobileBottomNav, MobileStatCard
components/common/      → 3개 플랫폼 공통 재사용 컴포넌트만
```

**React Query 훅 배치:** `hooks/` 디렉토리, 파일 1개당 훅 1개

### Process Patterns

**에러/로딩 패턴 (모든 데이터 페치 컴포넌트):**

```tsx
// ✅ 표준 패턴 — 3단계 분기 필수
const { data, isLoading, error, refetch } = useLeaderboard();
if (isLoading) return <LeaderboardSkeleton />;
if (error) return <InlineError message="데이터를 불러오지 못했습니다" onRetry={refetch} />;
return <LeaderboardContent data={data} />;
```

**PlayerLink/ChampionLink 캐시 패턴 유지:**

```ts
// 기존 모듈 레벨 Map 캐시 유지 (React Query로 교체 금지)
// 220ms 딜레이 타이머 유지
const cache = new Map<string, PlayerDetailStats>();
```

**Chart.js 동적 임포트 패턴:**

```tsx
// ✅ 챔피언/플레이어 상세 페이지만
const ChampionChart = React.lazy(() => import('../components/dashboard/ChampionChart'));
<Suspense fallback={<ChartSkeleton />}>
  <ChampionChart data={data} />
</Suspense>
```

### Enforcement Guidelines

**모든 AI Agent MUST:**

1. `api.get/post/delete`를 통해서만 REST API 호출 (fetch 직접 사용 금지, lcuApi 제외)
2. Named export만 사용 (default export 금지)
3. 모든 데이터 페치 컴포넌트에 isLoading/error/data 3단계 분기 적용
4. 신규 컴포넌트는 `components/dashboard/`, `components/electron/`, `components/mobile/` 중 적절한 폴더에 배치
5. PlayerLink/ChampionLink 기존 인터페이스(`riotId`, `children`, `className`, `mode`) 변경 금지
6. Chart.js는 챔피언/플레이어 상세 페이지 외 import 금지

**Anti-Patterns:**

```tsx
export default function Component() {}   // ❌ default export
const res = await fetch('/api/...');     // ❌ fetch 직접 호출
if (!data) return null;                  // ❌ 로딩/에러 구분 없는 null 반환
import { Chart } from 'chart.js';       // ❌ 홈/목록 페이지에서 Chart.js import
```

## Project Structure & Boundaries

### Complete Project Directory Structure

```
lol-event/
├── frontend/                          # Dashboard 웹 앱 (React + Vite)
│   ├── src/
│   │   ├── App.tsx                    # [MODIFY] 라우팅 구조 재정의 (/m/* 분기 추가)
│   │   ├── main.tsx
│   │   ├── index.css
│   │   ├── components/
│   │   │   ├── common/
│   │   │   │   ├── PlayerLink.tsx     # [MODIFY] 스타일 업데이트 (인터페이스 유지)
│   │   │   │   ├── ChampionLink.tsx   # [MODIFY] 스타일 업데이트 (인터페이스 유지)
│   │   │   │   ├── Button.tsx         # [KEEP]
│   │   │   │   ├── Modal.tsx          # [KEEP]
│   │   │   │   ├── ErrorModal.tsx     # [KEEP]
│   │   │   │   ├── Input.tsx          # [KEEP]
│   │   │   │   ├── Spinner.tsx        # [KEEP]
│   │   │   │   ├── InlineError.tsx    # [NEW] 인라인 에러 + 재시도 버튼 (NFR11)
│   │   │   │   └── Skeleton.tsx       # [NEW] 공통 Skeleton UI (NFR12)
│   │   │   ├── layout/
│   │   │   │   ├── Layout.tsx         # [MODIFY] 사이드바 Command Center 재설계
│   │   │   │   ├── Sidebar.tsx        # [MODIFY] 계층 탐색 구조 반영
│   │   │   │   └── MobileLayout.tsx   # [MODIFY] 하단 탭바 통합
│   │   │   ├── dashboard/             # [NEW 폴더] Dashboard 전용 컴포넌트
│   │   │   │   ├── EloLeaderboard.tsx        # FR1, FR14
│   │   │   │   ├── ChampionTierTable.tsx      # FR2
│   │   │   │   ├── BanTrendCard.tsx           # FR3
│   │   │   │   ├── ChampionChart.tsx          # FR11 (React.lazy 대상)
│   │   │   │   └── PlayerChart.tsx            # FR13 (React.lazy 대상)
│   │   │   ├── mobile/                # [NEW 폴더] 모바일 전용 컴포넌트
│   │   │   │   └── MobileBottomNav.tsx        # FR22
│   │   │   └── electron/              # [NEW 폴더] Electron 전용 컴포넌트
│   │   │       ├── PlayerCard.tsx             # FR17, FR18
│   │   │       └── BanRecommendBadge.tsx      # FR19
│   │   ├── pages/
│   │   │   ├── HomePage.tsx           # [NEW] FR1-FR3: 홈 (EloLeaderboard + ChampionTierTable + BanTrend)
│   │   │   ├── ChampionStatsPage.tsx  # [MODIFY] FR10-FR11: 챔피언 상세 + Chart.js
│   │   │   ├── PlayerStatsPage.tsx    # [MODIFY] FR12-FR13: 플레이어 상세 + Chart.js
│   │   │   ├── MatchesPage.tsx        # [MODIFY] FR6: 매치 목록
│   │   │   ├── MemberStatsListPage.tsx # [KEEP/MODIFY] FR5: 플레이어 목록
│   │   │   ├── StatsPage.tsx          # [KEEP/MODIFY] FR4: 챔피언 목록
│   │   │   ├── AdminPage.tsx          # [KEEP] FR27
│   │   │   ├── SyncPage.tsx           # [KEEP] FR28
│   │   │   ├── LcuPage.tsx            # [KEEP] FR27
│   │   │   ├── MonitoringPage.tsx     # [KEEP] FR27
│   │   │   ├── AdvancedStatsPage.tsx  # [KEEP]
│   │   │   ├── MembersPage.tsx        # [KEEP]
│   │   │   └── mobile/
│   │   │       ├── MobileStatsPage.tsx          # [MODIFY] FR23: 모바일 리더보드
│   │   │       ├── MobileChampionListPage.tsx   # [MODIFY] FR24: 챔피언 티어표
│   │   │       ├── MobileChampionDetailPage.tsx # [KEEP/MODIFY]
│   │   │       ├── MobilePlayerListPage.tsx     # [MODIFY] FR25: 플레이어 목록
│   │   │       ├── MobilePlayerDetailPage.tsx   # [MODIFY] FR25
│   │   │       ├── MobileMatchesPage.tsx        # [KEEP]
│   │   │       ├── MobileAdminPage.tsx          # [KEEP]
│   │   │       ├── MobileLcuPage.tsx            # [KEEP]
│   │   │       ├── MobileSyncPage.tsx           # [KEEP]
│   │   │       ├── MobileMembersPage.tsx        # [KEEP]
│   │   │       └── MobileMorePage.tsx           # [KEEP]
│   │   ├── hooks/
│   │   │   ├── useMobile.ts           # [KEEP]
│   │   │   ├── useLeaderboard.ts      # [NEW] FR1, FR14, FR23
│   │   │   ├── useChampions.ts        # [NEW] FR2, FR4, FR10-FR11, FR24
│   │   │   ├── usePlayers.ts          # [NEW] FR5, FR12-FR13, FR25
│   │   │   ├── useMatches.ts          # [NEW] FR6
│   │   │   └── useBanRecommend.ts     # [NEW] FR3
│   │   ├── lib/
│   │   │   ├── api/
│   │   │   │   └── api.ts             # [MODIFY] 신규 엔드포인트 추가
│   │   │   └── types/
│   │   │       ├── api.ts             # [KEEP]
│   │   │       ├── stats.ts           # [KEEP/EXTEND]
│   │   │       ├── member.ts          # [KEEP]
│   │   │       ├── match.ts           # [KEEP]
│   │   │       └── dragon.ts          # [KEEP]
│   │   ├── context/
│   │   │   └── DragonContext.tsx      # [KEEP]
│   │   └── styles/
│   │       ├── global.css             # [MODIFY] Tailwind 커스텀 토큰 추가
│   │       ├── mobile.css             # [KEEP/MODIFY]
│   │       ├── components/            # [KEEP] popup.css 등 기존 유지
│   │       ├── layouts/               # [KEEP/MODIFY]
│   │       └── pages/                 # [KEEP]
│
├── electron-collector/                # Electron 대기방 앱
│   └── src/
│       ├── main/
│       │   ├── index.ts               # [KEEP] 메인 프로세스
│       │   ├── lcu.ts                 # [KEEP] LCU API 연동 (변경 없음)
│       │   └── collect.ts             # [KEEP]
│       ├── preload/
│       │   └── index.ts               # [KEEP]
│       └── renderer/src/
│           ├── App.tsx                # [KEEP]
│           ├── components/
│           │   ├── layout/            # [KEEP]
│           │   └── lobby/             # [NEW 폴더]
│           │       ├── PlayerCard.tsx          # FR17, FR18
│           │       └── BanRecommendBadge.tsx   # FR19
│           └── pages/
│               ├── ChampSelectPage.tsx  # [MODIFY] FR15-FR19: 대기방 뷰 재설계
│               ├── CollectPage.tsx      # [KEEP]
│               ├── CustomGamePage.tsx   # [KEEP]
│               ├── LiveGamePage.tsx     # [KEEP]
│               └── SummonerPage.tsx     # [KEEP]
│
├── backend/                           # 기존 REST API 서버 (변경 없음)
└── installer/                         # Electron 빌드 결과물
```

### Architectural Boundaries

**API 경계:**
- 모든 프론트엔드 → 백엔드 통신: `lib/api/api.ts`의 `api.get/post/delete` 경유
- LCU API: `lcuApi` 객체 전용 (Electron 렌더러에서 직접 사용)
- 백엔드 REST API: `http://localhost:9832/api` (기존 그대로)

**컴포넌트 경계:**

```
Dashboard (components/dashboard/, pages/)
  → React Query 훅 (hooks/)
  → api.ts → 백엔드 REST API

모바일 (pages/mobile/)
  → 동일 React Query 훅 재사용
  → UI만 분리 (MobileLayout + MobileBottomNav)

Electron (renderer/pages/ChampSelectPage)
  → lcuApi (LCU 이벤트 수신)
  → api.ts (플레이어/챔피언 통계 조회)

PlayerLink/ChampionLink
  → 모듈 레벨 Map 캐시 (React Query 미사용)
  → api.ts 직접 호출
```

### FR → 파일 매핑

| FR | 기능 | 주요 파일 |
|----|------|-----------|
| FR1-FR3 | Dashboard 홈 | `pages/HomePage.tsx`, `components/dashboard/EloLeaderboard.tsx`, `ChampionTierTable.tsx`, `BanTrendCard.tsx` |
| FR4-FR7 | 계층 탐색 + Breadcrumb | `pages/ChampionStatsPage.tsx`, `PlayerStatsPage.tsx`, `MatchesPage.tsx`, `Sidebar.tsx` |
| FR8-FR9 | Hover 위젯 | `components/common/PlayerLink.tsx`, `ChampionLink.tsx` |
| FR10-FR13 | Chart.js 시각화 | `components/dashboard/ChampionChart.tsx`, `PlayerChart.tsx` |
| FR14 | Elo 하이라이트 | `components/dashboard/EloLeaderboard.tsx` |
| FR15-FR20 | Electron 대기방 | `renderer/pages/ChampSelectPage.tsx`, `renderer/components/lobby/PlayerCard.tsx`, `BanRecommendBadge.tsx` |
| FR21-FR25 | 모바일 웹 | `components/layout/MobileLayout.tsx`, `components/mobile/MobileBottomNav.tsx`, `pages/mobile/*` |
| FR26 | 관리자 팀 구성 | `pages/HomePage.tsx` (EloLeaderboard + PlayerLink hover) |
| FR27-FR28 | 관리자/LCU 유지 | `pages/AdminPage.tsx`, `SyncPage.tsx`, `LcuPage.tsx`, `MonitoringPage.tsx` (변경 없음) |

### 스타일 아키텍처

```
styles/global.css         ← Tailwind 커스텀 토큰 추가 (--color-win, --color-primary 등)
styles/components/        ← 기존 커스텀 CSS 유지 (popup.css 등)
신규 컴포넌트              ← Tailwind 유틸리티 클래스 인라인 사용
```

## Architecture Validation Results

### Coherence Validation ✅

**Decision Compatibility:**
- TypeScript + React + Tailwind CSS + shadcn/ui: 완전 호환. 기존 Tailwind 코드베이스에 shadcn/ui headless 컴포넌트 추가는 충돌 없음.
- React Query + 기존 `api.ts` wrapper: `api.get/post/delete`가 `ApiResponse<T>`를 자동 언래핑하므로 React Query의 `queryFn`에 바로 사용 가능.
- React Router nested routes + `/m/*` 분기: 기존 라우팅 구조 유지하면서 `<Route path="/m">` 추가 — 충돌 없음.
- Chart.js + `React.lazy`: Vite 기반 코드 스플리팅 완전 지원.

**Pattern Consistency:**
- Named export 규칙이 기존 코드(`export function PlayerLink`)와 일치.
- `PascalCasePage.tsx` 네이밍이 기존 파일(`AdminPage.tsx`, `ChampionStatsPage.tsx`)과 일치.
- CSS 변수(`var(--color-win)`) + Tailwind 혼용 패턴이 기존 코드와 일치.

**Structure Alignment:**
- 신규 폴더(`components/dashboard/`, `components/electron/`, `components/mobile/`)가 기존 type-based 구조와 일관성 유지.
- Electron renderer는 자체 독립 패키지로 frontend `api.ts` 미공유 — 기존 방식 그대로 유지.

### Requirements Coverage Validation ✅

**Functional Requirements (FR1-FR28):**
- FR1-FR7: `HomePage.tsx` + Dashboard 컴포넌트 + `Sidebar.tsx` 재설계로 커버
- FR8-FR9: `PlayerLink.tsx` + `ChampionLink.tsx` 스타일 업데이트 (인터페이스 유지)로 커버
- FR10-FR14: `ChampionChart.tsx` + `PlayerChart.tsx` (React.lazy) + 상세 페이지 수정으로 커버
- FR15-FR20: `ChampSelectPage.tsx` 재설계 + `lobby/PlayerCard.tsx` + `BanRecommendBadge.tsx`로 커버
- FR21-FR25: `MobileLayout.tsx` + `MobileBottomNav.tsx` + 모바일 페이지 수정으로 커버
- FR26-FR28: 기존 Admin/Sync/Lcu/MonitoringPage 유지로 커버

**Non-Functional Requirements (NFR1-NFR12):**
- NFR1 (300ms hover): 기존 Map 캐시 + 220ms 타이머 유지 ✅
- NFR2 (Electron 2s): `useLobby` staleTime: 0 ✅
- NFR3-NFR5 (번들/LCP): Chart.js `React.lazy` 적용 규칙 ✅
- NFR6-NFR8 (접근성): TIER_COLORS 상수 + CSS 변수 색상 기준 정의 ✅
- NFR9-NFR10 (LCU): `lcu.ts` 유지 + FR20 graceful degradation ✅
- NFR11-NFR12 (신뢰성): `InlineError.tsx` + `Skeleton.tsx` [NEW] ✅

### Gap Analysis Results

**Important (해결됨):**
- Electron renderer API 레이어: 별도 패키지이므로 frontend `api.ts` 미공유. `lcu.ts`의 기존 fetch 방식 + 필요 시 Electron renderer 내 별도 api 유틸 파일 추가. 기존 방식 그대로 유지.

**Minor (해결됨):**
- `hooks/useLobby.ts` → FR15-FR20 (Electron 대기방) 매핑 추가
- Tailwind 커스텀 토큰: `styles/global.css`에서 기존 CSS 변수 유지 + 신규 Teal(`#00B4D8`), Navy(`#0A0E1A`) 토큰 추가

### Architecture Completeness Checklist

**✅ Requirements Analysis**
- [x] 프로젝트 컨텍스트 분석 완료 (28 FRs, 12 NFRs)
- [x] 규모/복잡도 평가 (Medium, 50-60명, 내부 전용)
- [x] 기술 제약 사항 식별 (브라운필드, LCU API, 기존 코드 유지)
- [x] Cross-cutting 관심사 매핑 (React Query, 디자인 시스템, 번들 최적화)

**✅ Architectural Decisions**
- [x] 기술 스택 확정 (TypeScript + React + Tailwind + shadcn/ui + React Query)
- [x] 플랫폼별 아키텍처 결정 (Dashboard / Mobile / Electron)
- [x] 데이터 레이어 전략 (React Query + 기존 REST API 유지)
- [x] 번들 최적화 전략 (Chart.js React.lazy)
- [x] 배포 전략 (자체 서버 + Electron 수동 배포)

**✅ Implementation Patterns**
- [x] 파일/컴포넌트 네이밍 컨벤션
- [x] API 호출 패턴 (api.ts 경유)
- [x] CSS/Tailwind 혼용 기준
- [x] 에러/로딩 3단계 패턴
- [x] Chart.js 동적 임포트 규칙
- [x] Anti-pattern 명시

**✅ Project Structure**
- [x] 완전한 디렉토리 구조 (기존 파일 KEEP/MODIFY/NEW 표시)
- [x] FR → 파일 매핑 테이블
- [x] 컴포넌트 경계 및 데이터 흐름 정의
- [x] 스타일 아키텍처 정의

### Architecture Readiness Assessment

**Overall Status: READY FOR IMPLEMENTATION**

**Confidence Level: High**

**Key Strengths:**
- 브라운필드 특성을 정확히 반영 — 기존 동작 코드 보존 범위 명확
- 3개 플랫폼 경계가 명확하게 분리되어 AI 에이전트 간 충돌 위험 최소화
- 기존 코드 패턴(named export, api.ts, Map 캐시)을 아키텍처 패턴으로 공식화
- FR → 파일 매핑으로 구현 시작점 명확

**Areas for Future Enhancement:**
- Phase 2: 밴 가치 점수 고도화 시 별도 아키텍처 섹션 추가
- Phase 3: 명예의 전당 구현 시 새로운 데이터 타입/훅 추가

### Implementation Handoff

**AI Agent Guidelines:**
1. 이 문서의 모든 아키텍처 결정을 정확히 따를 것
2. [KEEP] 표시 파일은 절대 수정하지 않을 것 (AdminPage, SyncPage, LcuPage, MonitoringPage, lcu.ts)
3. [MODIFY] 파일은 기존 인터페이스를 유지하면서 스타일/구조만 업데이트
4. 신규 컴포넌트는 반드시 지정된 폴더에 배치

**First Implementation Priority:**
1. `shadcn/ui` CLI 설치 + `styles/global.css` Tailwind 커스텀 토큰 추가
2. `components/common/InlineError.tsx` + `Skeleton.tsx` 신규 생성
3. `hooks/useLeaderboard.ts` + React Query 캐시 설정
4. `components/dashboard/EloLeaderboard.tsx` + `ChampionTierTable.tsx`
5. `pages/HomePage.tsx` (Dashboard 홈 재설계)

