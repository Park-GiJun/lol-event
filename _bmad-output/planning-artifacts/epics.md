---
stepsCompleted: [1, 2, 3, 4]
inputDocuments:
  - '_bmad-output/planning-artifacts/prd.md'
  - '_bmad-output/planning-artifacts/architecture.md'
  - '_bmad-output/planning-artifacts/ux-design-specification.md'
---

# lol-event - Epic Breakdown

## Overview

This document provides the complete epic and story breakdown for lol-event, decomposing the requirements from the PRD, UX Design, and Architecture into implementable stories.

## Requirements Inventory

### Functional Requirements

FR1: 유저는 홈 화면에서 전체 멤버 Elo 리더보드를 즉시 확인할 수 있다
FR2: 유저는 홈 화면에서 내전 기반 챔피언 티어표(S/A/B/C)를 즉시 확인할 수 있다
FR3: 유저는 홈 화면에서 챔피언 밴픽 트렌드를 확인할 수 있다
FR4: 유저는 챔피언 목록에서 챔피언을 선택하여 상세 페이지로 이동할 수 있다 (1클릭)
FR5: 유저는 플레이어 목록에서 플레이어를 선택하여 상세 페이지로 이동할 수 있다 (1클릭)
FR6: 유저는 매치 목록에서 매치를 선택하여 상세 내역을 확인할 수 있다
FR7: 유저는 Breadcrumb을 통해 상위 계층으로 돌아갈 수 있다
FR8: 유저는 플레이어 이름에 마우스를 올려 해당 플레이어의 핵심 통계를 즉시 확인할 수 있다 (PlayerLink)
FR9: 유저는 챔피언 이름에 마우스를 올려 해당 챔피언의 핵심 통계를 즉시 확인할 수 있다 (ChampionLink)
FR10: 유저는 챔피언 상세 페이지에서 승률/픽률/KDA 수치 카드를 확인할 수 있다
FR11: 유저는 챔피언 상세 페이지에서 Chart.js 기반 시계열 시각화(승률 추이, 게임별 퍼포먼스)를 확인할 수 있다
FR12: 유저는 플레이어 상세 페이지에서 개인 통계 수치 카드를 확인할 수 있다
FR13: 유저는 플레이어 상세 페이지에서 Chart.js 기반 Elo 추이 및 챔피언별 성적 시각화를 확인할 수 있다
FR14: 유저는 Elo 리더보드에서 자신의 순위가 자동으로 하이라이트된 상태로 확인할 수 있다
FR15: 시스템은 LoL 클라이언트 대기방 진입을 자동으로 감지할 수 있다
FR16: 시스템은 대기방 진입 감지 시 양팀 분석 화면을 자동으로 표시할 수 있다
FR17: 유저는 좌우 분할 카드로 우리팀과 상대팀 플레이어를 동시에 확인할 수 있다
FR18: 유저는 각 플레이어 카드에서 닉네임, Elo, 고승률 챔피언 TOP 3를 확인할 수 있다
FR19: 유저는 각 플레이어 카드에서 밴 추천 챔피언을 확인할 수 있다
FR20: 유저는 LoL 클라이언트 미실행 시 Electron 안내 메시지를 확인할 수 있다
FR21: 유저는 모바일 전용 경로(/m/*)로 접근하여 모바일 최적화 화면을 사용할 수 있다
FR22: 유저는 모바일 하단 탭바를 통해 홈(리더보드)/챔피언/플레이어 간 이동할 수 있다
FR23: 유저는 모바일에서 Elo 리더보드를 확인하고 자신의 순위를 파악할 수 있다
FR24: 유저는 모바일에서 챔피언 티어표를 확인할 수 있다
FR25: 유저는 모바일에서 개인 통계 페이지를 확인할 수 있다
FR26: 관리자는 멤버 Elo 리더보드를 기반으로 팀 구성에 필요한 데이터를 한 화면에서 비교할 수 있다
FR27: 관리자는 기존 관리 페이지(AdminPage, SyncPage, LcuPage, MonitoringPage)를 통해 리그를 운영할 수 있다
FR28: 시스템은 LCU 동기화를 통해 게임 데이터를 자동으로 수집할 수 있다

### NonFunctional Requirements

NFR1: Hover 위젯(PlayerLink/ChampionLink) — 마우스 진입 후 300ms 이내 통계 표시
NFR2: Electron 대기방 — 대기방 진입 감지 후 2초 이내 양팀 카드 렌더링
NFR3: Dashboard 페이지 초기 로딩 — 첫 화면 3초 이내 표시 (LCP 기준)
NFR4: Chart.js 시각화 — 상세 페이지 진입 후 2초 이내 차트 렌더링
NFR5: 홈/목록 페이지 — Chart.js 미포함 (동적 임포트로 번들 분리)
NFR6: 텍스트/배경 색상 대비 WCAG AA 기준 4.5:1 이상 준수
NFR7: 승/패 색상 표시 시 색상 + 텍스트 병행 (색맹 대응)
NFR8: 모바일 최소 터치 타겟 44px × 44px 이상
NFR9: LCU API — LoL 클라이언트 미실행 시 Electron graceful degradation (안내 메시지 표시)
NFR10: LCU 동기화 — 클라이언트 패치 후 API 변경 감지 및 오류 로깅
NFR11: API 응답 실패 시 인라인 에러 메시지 + 재시도 버튼 표시 (앱 전체 중단 없음)
NFR12: Skeleton UI로 로딩 중에도 레이아웃 구조 유지 (CLS 방지)

### Additional Requirements

- 커스텀 CSS 클래스 정의 (`.card`, `.table`, `.member-stats-table`, `.btn`, `.breadcrumb-nav`)
- React Query QueryClient 초기 설정 (staleTime: 5분, gcTime: 30분 기본값, 대기방 staleTime: 0)
- 공통 컴포넌트 신규 생성: InlineError.tsx (재시도 버튼 포함), Skeleton.tsx
- 신규 컴포넌트 폴더 생성: components/dashboard/, components/electron/, components/mobile/
- App.tsx 라우팅 재구성: /m/* 모바일 nested routes 추가
- React Query 훅 레이어 생성: hooks/useLeaderboard.ts, useChampions.ts, usePlayers.ts, useMatches.ts, useBanRecommend.ts
- styles/global.css Tailwind 커스텀 토큰 추가 (Teal #00B4D8, Navy #0A0E1A 등)
- Chart.js: React.lazy + Suspense 패턴으로 상세 페이지 한정 동적 임포트
- Electron renderer 내 대기방 API 유틸 추가 (플레이어/챔피언 통계 조회)

### UX Design Requirements

UX-DR1: styles/global.css에 Tailwind 커스텀 색상 토큰 정의 (--color-win: #10B981, --color-loss: #EF4444, --color-primary: #00B4D8, Deep Navy: #0A0E1A 등 전체 팔레트)
UX-DR2: Inter 폰트(기본) + JetBrains Mono 폰트(수치 표시) 설정
UX-DR3: TIER_COLORS 상수 정의 (S: #FFD700, A: #00B4D8, B: #8899BB, C: #4A5568)
UX-DR4: PlayerCard 커스텀 컴포넌트 구현 — 3상태 (default/expanded/loading), role="article", 닉네임 aria-label
UX-DR5: ChampionTierTable 커스텀 컴포넌트 구현 — 티어 섹션 접기/펼치기, 컬럼 정렬, `.table.member-stats-table` 커스텀 CSS
UX-DR6: EloLeaderboard 커스텀 컴포넌트 — 데스크톱(테이블)/모바일(카드 리스트) 변형, 자신의 행 `rgba(11, 196, 180, 0.08)` 하이라이트
UX-DR7: StatHoverWidget(PlayerLink/ChampionLink) 스타일 업데이트 — 기존 인터페이스(riotId, children, className, mode) 유지, CSS position으로 스타일 개선
UX-DR8: BanRecommendBadge 커스텀 컴포넌트 — 높은 위협도(#EF4444)/일반(#00B4D8), `<span>` 인라인 스타일 구현
UX-DR9: MobileBottomNav 컴포넌트 — fixed bottom-0, safe-area-inset-bottom, 4-5탭, 활성 탭 Teal 강조
UX-DR10: Dashboard Command Center 레이아웃 — 좌측 240px 고정 사이드바, 활성 항목 Teal 좌측 보더 강조
UX-DR11: 피드백 패턴 일관 적용 — Skeleton(pulse 애니메이션), InlineError(red + 재시도 버튼), 빈 상태(텍스트만, 이모지 없음)
UX-DR12: 데이터 표시 패턴 — 테이블 행 높이 48px, font-mono 수치 정렬, 승률 50% 기준 색상 분기
UX-DR13: Electron 대기방 좌우 grid-cols-2 분할 레이아웃 — 우리팀(좌)/상대팀(우)
UX-DR14: Breadcrumb 컴포넌트 상단 고정 — "전체 > 챔피언 > 야스오" 형태, 각 항목 클릭 가능, `<nav>` + `<ol>` 네이티브 구현
UX-DR15: 검색/필터 패턴 — 즉시 필터링(debounce 300ms), 기본 정렬 Elo 내림차순
UX-DR16: WCAG AA 색상 대비 4.5:1 이상 준수 (Teal on Navy 포함)
UX-DR17: 모바일 터치 타겟 최소 44px × 44px 적용
UX-DR18: 승/패 색상 표시 시 색상 + 텍스트 레이블 병행 (색맹 대응)

### FR Coverage Map

FR1: Epic 2 — 홈 Elo 리더보드
FR2: Epic 2 — 홈 챔피언 티어표
FR3: Epic 2 — 홈 밴픽 트렌드
FR4: Epic 3 — 챔피언 목록 → 상세
FR5: Epic 3 — 플레이어 목록 → 상세
FR6: Epic 3 — 매치 목록 → 상세
FR7: Epic 3 — Breadcrumb 탐색
FR8: Epic 3 — PlayerLink hover
FR9: Epic 3 — ChampionLink hover
FR10: Epic 3 — 챔피언 수치 카드
FR11: Epic 3 — 챔피언 Chart.js 시각화
FR12: Epic 3 — 플레이어 수치 카드
FR13: Epic 3 — 플레이어 Chart.js 시각화
FR14: Epic 2 — Elo 자신 행 하이라이트
FR15: Epic 4 — LCU 대기방 감지
FR16: Epic 4 — 자동 트리거 화면 표시
FR17: Epic 4 — 좌우 분할 카드
FR18: Epic 4 — 플레이어 카드 정보
FR19: Epic 4 — 밴 추천 표시
FR20: Epic 4 — LCU 미실행 안내
FR21: Epic 5 — 모바일 /m/* 라우트
FR22: Epic 5 — 하단 탭바
FR23: Epic 5 — 모바일 리더보드
FR24: Epic 5 — 모바일 챔피언 티어표
FR25: Epic 5 — 모바일 플레이어 통계
FR26: Epic 2 — 관리자 팀 구성 지원
FR27: Epic 1 — 관리자 페이지 유지 (KEEP)
FR28: Epic 1 — LCU 동기화 유지 (KEEP)

## Epic List

### Epic 1: Foundation — 디자인 시스템 & 공통 인프라
모든 플랫폼에서 일관된 디자인 토큰, 공통 컴포넌트(Skeleton, InlineError), React Query 캐싱이 동작하는 기반 환경을 완성한다.
**FRs covered:** FR27 (관리자 페이지 유지), FR28 (LCU 동기화 유지)
**Architecture requirements:** 커스텀 CSS 클래스 정의, React Query QueryClient 설정, 신규 폴더 구조, App.tsx 라우팅 재구성
**UX-DRs:** UX-DR1~3 (색상 토큰/폰트), UX-DR11 (Skeleton/InlineError), UX-DR16, UX-DR18

### Epic 2: Dashboard 홈 — 리그 현황 한눈에 보기
유저가 홈 접속 즉시 Elo 리더보드 + 챔피언 티어표 + 밴픽 트렌드를 확인하고, 관리자는 한 화면에서 팀 구성 데이터를 비교할 수 있다.
**FRs covered:** FR1, FR2, FR3, FR14, FR26
**UX-DRs:** UX-DR5 (ChampionTierTable), UX-DR6 (EloLeaderboard), UX-DR10 (Command Center 레이아웃), UX-DR12 (데이터 표시 패턴)
**NFRs:** NFR3 (LCP 3s), NFR5 (Chart.js 미포함)

### Epic 3: Dashboard 탐색 & 상세 분석
유저가 챔피언/플레이어/매치를 클릭 1회로 드릴다운하고, 상세 페이지에서 Chart.js 시각화와 Hover 위젯으로 깊이 있는 분석을 할 수 있다.
**FRs covered:** FR4, FR5, FR6, FR7, FR8, FR9, FR10, FR11, FR12, FR13
**UX-DRs:** UX-DR7 (StatHoverWidget 보강), UX-DR14 (Breadcrumb), UX-DR15 (검색/필터)
**NFRs:** NFR1 (hover 300ms), NFR4 (chart 2s)

### Epic 4: Electron 대기방 — 자동화된 밴픽 지원
유저가 LoL 대기방에 입장하면 자동으로 양팀 플레이어 카드와 밴 추천이 표시되어 별도 조작 없이 10초 안에 밴픽을 결정할 수 있다.
**FRs covered:** FR15, FR16, FR17, FR18, FR19, FR20
**UX-DRs:** UX-DR4 (PlayerCard), UX-DR8 (BanRecommendBadge), UX-DR13 (좌우 grid-cols-2)
**NFRs:** NFR2 (2s 렌더링), NFR9 (LCU graceful degradation)

### Epic 5: 모바일 웹 — 이동 중 리그 확인
유저가 스마트폰으로 /m/* 접속 시 하단 탭바로 리더보드/챔피언/개인 통계를 불편함 없이 탐색할 수 있다.
**FRs covered:** FR21, FR22, FR23, FR24, FR25
**UX-DRs:** UX-DR9 (MobileBottomNav), UX-DR17 (44px 터치), UX-DR12 (모바일 데이터 표시)
**NFRs:** NFR8 (터치 타겟 44px)

---

## Epic 1: Foundation — 디자인 시스템 & 공통 인프라

모든 플랫폼에서 일관된 디자인 토큰, 공통 컴포넌트(Skeleton, InlineError), React Query 캐싱이 동작하는 기반 환경을 완성한다. 이 Epic 완료 후 이후 모든 Epic의 구현이 가능하다.

### Story 1.1: 디자인 토큰 & 폰트 설정

As a 개발자,
I want Tailwind CSS 커스텀 색상 토큰과 폰트가 global.css에 정의되어 있기를,
So that 모든 컴포넌트에서 일관된 색상(Teal, Navy, Win/Loss)과 수치 전용 폰트(JetBrains Mono)를 사용할 수 있다.

**Acceptance Criteria:**

**Given** `styles/global.css`가 존재할 때
**When** 개발자가 CSS 변수를 참조하면
**Then** `--color-primary: #00B4D8`, `--color-win: #10B981`, `--color-loss: #EF4444`, `--bg-base: #0A0E1A`, `--bg-surface: #0F1629` 등 전체 팔레트가 정의되어 있다
**And** `TIER_COLORS` 상수(`S: #FFD700, A: #00B4D8, B: #8899BB, C: #4A5568`)가 TypeScript 파일에 export된다

**Given** Inter 폰트와 JetBrains Mono 폰트가 설정되었을 때
**When** 수치 표시 요소에 `font-mono` 클래스가 적용되면
**Then** JetBrains Mono 폰트로 렌더링된다

**Given** Teal(#00B4D8) 텍스트를 Navy(#0F1629) 배경 위에 표시할 때
**When** 색상 대비를 측정하면
**Then** WCAG AA 기준 4.5:1 이상이다 (NFR6)

---

### Story 1.2: 커스텀 CSS 컴포넌트 기반 설정

As a 개발자,
I want 공통 CSS 클래스와 Tailwind 유틸리티로 컴포넌트 스타일 기반이 정의되기를,
So that `.card`, `.table`, `.member-stats-table`, `.btn`, `BreadcrumbNav` 등을 즉시 사용할 수 있다.

**Acceptance Criteria:**

**Given** `styles/global.css` + `styles/components.css`가 설정되었을 때
**When** 공통 CSS 클래스를 사용하면
**Then** `.card`, `.table`, `.table-wrapper`, `.btn`, `.breadcrumb-nav` 클래스가 정상 동작한다

**Given** Lucide React가 설치되었을 때
**When** `import { ChevronRight } from 'lucide-react'`를 사용하면
**Then** 정상적으로 렌더링된다

---

### Story 1.3: React Query QueryClient 설정 & 공통 훅 레이어

As a 개발자,
I want React Query QueryClient가 기본 캐시 정책으로 설정되고, 공통 데이터 훅이 생성되기를,
So that 모든 플랫폼에서 staleTime 5분, gcTime 30분 기준의 캐싱이 일관되게 적용된다.

**Acceptance Criteria:**

**Given** `main.tsx`에 QueryClientProvider가 감싸져 있을 때
**When** 앱이 로드되면
**Then** 기본 `staleTime: 5 * 60 * 1000`, `gcTime: 30 * 60 * 1000`이 적용된 QueryClient가 동작한다

**Given** `hooks/useLeaderboard.ts`가 생성되었을 때
**When** `useLeaderboard()`를 호출하면
**Then** `['leaderboard']` 쿼리 키로 React Query가 동작하고 `{ data, isLoading, error, refetch }`를 반환한다

**Given** 동일한 방식으로 `useChampions`, `usePlayers`, `useMatches`, `useBanRecommend` 훅이 생성되었을 때
**When** 각 훅을 호출하면
**Then** 정해진 Query Key 컨벤션(`['champions']`, `['players']`, 등)으로 동작한다

**Given** `api.get<T>(endpoint)`를 `queryFn`으로 사용할 때
**When** API 호출이 실패하면
**Then** React Query의 `error` 상태가 설정되고 `data`는 undefined가 된다

---

### Story 1.4: 공통 피드백 컴포넌트 생성 (InlineError, Skeleton)

As a 개발자,
I want InlineError와 Skeleton 공통 컴포넌트가 존재하기를,
So that 모든 데이터 페치 컴포넌트에서 일관된 로딩/에러 UI 패턴을 적용할 수 있다.

**Acceptance Criteria:**

**Given** `components/common/InlineError.tsx`가 생성되었을 때
**When** `<InlineError message="데이터를 불러오지 못했습니다" onRetry={refetch} />`를 렌더링하면
**Then** 에러 메시지 텍스트와 "다시 시도" 버튼이 표시된다
**And** 버튼 클릭 시 `onRetry` 콜백이 호출된다 (NFR11)

**Given** `components/common/Skeleton.tsx`가 생성되었을 때
**When** `<Skeleton className="h-8 w-full" />`을 렌더링하면
**Then** pulse 애니메이션이 있는 회색 블록이 표시된다
**And** 지정된 className의 높이/너비가 적용된다 (NFR12)

**Given** 데이터 로딩 중일 때
**When** 컴포넌트가 Skeleton을 렌더링하면
**Then** 레이아웃이 흔들리지 않는다 (CLS 방지, NFR12)

---

### Story 1.5: App.tsx 라우팅 재구성 & 폴더 구조 정비

As a 개발자,
I want App.tsx 라우팅에 `/m/*` 모바일 nested routes가 추가되고, 신규 컴포넌트 폴더가 생성되기를,
So that Dashboard와 모바일 뷰가 완전히 분리된 라우팅으로 동작한다.

**Acceptance Criteria:**

**Given** `App.tsx`에 React Router 라우팅이 설정되었을 때
**When** 브라우저에서 `/m`으로 접근하면
**Then** `<MobileLayout />`이 렌더링된다

**Given** `/m` 경로에 nested routes가 설정되었을 때
**When** `/m/champions`로 접근하면
**Then** 모바일 챔피언 페이지가 렌더링되고 데스크톱 사이드바 레이아웃은 표시되지 않는다

**Given** 신규 폴더 구조가 생성되었을 때
**When** 개발자가 `components/dashboard/`, `components/electron/`, `components/mobile/` 폴더를 확인하면
**Then** 각 폴더가 존재한다

**Given** 기존 AdminPage, SyncPage, LcuPage, MonitoringPage 라우트가 있을 때
**When** App.tsx를 수정해도
**Then** 기존 관리자 페이지 라우트는 그대로 동작한다 (FR27)

---

## Epic 2: Dashboard 홈 — 리그 현황 한눈에 보기

유저가 홈 접속 즉시 Elo 리더보드 + 챔피언 티어표 + 밴픽 트렌드를 확인하고, 관리자는 한 화면에서 팀 구성 데이터를 비교할 수 있다.

### Story 2.1: Dashboard Command Center 레이아웃 재설계

As a 유저,
I want Dashboard 접속 시 좌측 240px 고정 사이드바와 콘텐츠 영역이 분리된 레이아웃을 보기를,
So that 어느 페이지에서든 사이드바로 즉시 원하는 섹션으로 이동할 수 있다.

**Acceptance Criteria:**

**Given** 유저가 Dashboard(`/`)에 접속했을 때
**When** 페이지가 로드되면
**Then** 좌측 240px 고정 사이드바와 우측 콘텐츠 영역이 분리되어 렌더링된다

**Given** 사이드바에 네비게이션 항목들이 있을 때
**When** 현재 활성 페이지의 항목을 확인하면
**Then** Teal 색상 좌측 보더와 배경 강조가 표시된다

**Given** 사이드바 네비게이션 항목에 Lucide 아이콘이 있을 때
**When** 아이콘을 확인하면
**Then** 컬러 이모지가 아닌 단색/라인 Lucide 아이콘이 사용된다

**Given** 브라우저 너비가 1024px 이상일 때
**When** Dashboard를 사용하면
**Then** 사이드바가 항상 표시된다

---

### Story 2.2: EloLeaderboard 컴포넌트 & 홈 배치

As a 유저,
I want 홈 화면에서 전체 멤버 Elo 순위표를 즉시 확인하기를,
So that 내 순위가 어디쯤인지 추가 탐색 없이 파악할 수 있다. (FR1, FR14, FR26)

**Acceptance Criteria:**

**Given** 유저가 홈(`/`)에 접속했을 때
**When** 페이지가 로드되면
**Then** 3초 이내 Elo 리더보드가 표시된다 (NFR3)

**Given** 리더보드가 로딩 중일 때
**When** 데이터가 아직 없으면
**Then** Skeleton UI가 표시되고 레이아웃이 흔들리지 않는다 (NFR12)

**Given** 현재 로그인한 유저의 riotId가 존재할 때
**When** 리더보드를 확인하면
**Then** 해당 행에 `bg-teal-900/20` 배경 하이라이트가 자동으로 적용된다 (FR14)

**Given** 리더보드 데이터가 표시될 때
**When** 각 행을 확인하면
**Then** 순위, 닉네임, Elo 수치, 최근 N게임이 표시되고 수치는 `font-mono`로 정렬된다

**Given** API 호출이 실패했을 때
**When** 에러가 발생하면
**Then** InlineError 컴포넌트가 표시되고 재시도 버튼이 동작한다 (NFR11)

---

### Story 2.3: ChampionTierTable 컴포넌트 & 홈 배치

As a 유저,
I want 홈 화면에서 내전 기반 챔피언 티어표(S/A/B/C)를 즉시 확인하기를,
So that 이번 리그에서 강한 챔피언이 무엇인지 OP.GG처럼 직관적으로 파악할 수 있다. (FR2)

**Acceptance Criteria:**

**Given** 유저가 홈에 접속했을 때
**When** ChampionTierTable이 로드되면
**Then** S/A/B/C 티어 섹션으로 분류된 챔피언 목록이 표시된다

**Given** 챔피언 티어표가 표시될 때
**When** 각 챔피언 행을 확인하면
**Then** 챔피언 아이콘, 이름, 승률, 픽률, 게임 수가 표시되고 수치는 `font-mono`로 정렬된다

**Given** 티어 섹션 헤더를 클릭했을 때
**When** 접기/펼치기를 누르면
**Then** 해당 티어의 챔피언 목록이 접히거나 펼쳐진다

**Given** 승률 50% 이상인 챔피언을 확인할 때
**When** 승률 수치를 보면
**Then** `#10B981`(초록) 색상이 적용된다
**And** 50% 미만이면 `#EF4444`(빨강) 색상이 적용된다

**Given** S티어 뱃지를 확인할 때
**When** 티어 색상을 보면
**Then** TIER_COLORS 상수에 정의된 색상(S: #FFD700)이 적용된다

---

### Story 2.4: 밴픽 트렌드 카드 & 홈 배치

As a 유저,
I want 홈 화면에서 최근 많이 밴되는 챔피언 트렌드를 확인하기를,
So that 현재 메타에서 어떤 챔피언이 위협적으로 인식되는지 빠르게 파악할 수 있다. (FR3)

**Acceptance Criteria:**

**Given** 유저가 홈에 접속했을 때
**When** 밴픽 트렌드 카드가 로드되면
**Then** 최근 N게임 기준 밴률 상위 챔피언 목록이 수치와 함께 표시된다

**Given** 밴픽 트렌드 데이터가 표시될 때
**When** 각 항목을 확인하면
**Then** 챔피언 이름, 밴률(%) 수치가 표시된다 (이모지 없음)

**Given** Chart.js가 홈 페이지에 포함되지 않아야 할 때
**When** 홈 페이지 번들을 확인하면
**Then** Chart.js import가 없다 (NFR5)

---

## Epic 3: Dashboard 탐색 & 상세 분석

유저가 챔피언/플레이어/매치를 클릭 1회로 드릴다운하고, 상세 페이지에서 Chart.js 시각화와 Hover 위젯으로 깊이 있는 분석을 할 수 있다.

### Story 3.1: 챔피언 목록 & 상세 페이지 (수치 카드)

As a 유저,
I want 챔피언 목록에서 챔피언을 클릭하여 상세 수치 카드를 확인하기를,
So that 특정 챔피언의 승률/픽률/KDA를 한눈에 파악할 수 있다. (FR4, FR10)

**Acceptance Criteria:**

**Given** 유저가 챔피언 목록 페이지에 있을 때
**When** 특정 챔피언 행을 클릭하면
**Then** 1클릭으로 챔피언 상세 페이지(`/champions/:id`)로 이동한다

**Given** 챔피언 상세 페이지에 진입했을 때
**When** 페이지가 로드되면
**Then** 상단에 승률, 픽률, KDA, 게임 수 수치 카드가 표시된다

**Given** 수치 카드가 표시될 때
**When** 각 수치를 확인하면
**Then** `font-mono` 폰트로 정렬되고 소수점 2자리가 통일된다

**Given** 데이터 로딩 중일 때
**When** 상세 페이지를 열면
**Then** Skeleton 카드가 표시되고 레이아웃이 흔들리지 않는다

---

### Story 3.2: 챔피언 상세 Chart.js 시각화

As a 유저,
I want 챔피언 상세 페이지에서 승률 추이와 게임별 퍼포먼스 차트를 확인하기를,
So that 해당 챔피언의 최근 메타 변화와 트렌드를 시각적으로 파악할 수 있다. (FR11)

**Acceptance Criteria:**

**Given** 유저가 챔피언 상세 페이지에 진입했을 때
**When** Chart.js 차트가 동적 로딩(React.lazy)으로 불러와지면
**Then** 2초 이내 승률 추이 라인 차트가 렌더링된다 (NFR4)

**Given** Chart.js가 동적 임포트로 적용되었을 때
**When** 홈/목록 페이지 번들을 확인하면
**Then** Chart.js가 포함되지 않는다 (NFR5)

**Given** Chart.js 차트가 로딩 중일 때
**When** `React.lazy` Suspense fallback이 표시되면
**Then** ChartSkeleton 컴포넌트가 표시된다

**Given** 차트가 렌더링되었을 때
**When** 데이터 포인트에 마우스를 올리면
**Then** 해당 시점의 수치가 툴팁으로 표시된다

---

### Story 3.3: 플레이어 목록 & 상세 페이지 (수치 카드)

As a 유저,
I want 플레이어 목록에서 플레이어를 클릭하여 개인 통계 수치 카드를 확인하기를,
So that 특정 플레이어의 전반적인 성적을 한눈에 파악할 수 있다. (FR5, FR12)

**Acceptance Criteria:**

**Given** 유저가 플레이어 목록 페이지에 있을 때
**When** 특정 플레이어 행을 클릭하면
**Then** 1클릭으로 플레이어 상세 페이지(`/player-stats/:riotId`)로 이동한다

**Given** 플레이어 상세 페이지에 진입했을 때
**When** 페이지가 로드되면
**Then** 승률, 게임 수, KDA, 평균 킬/데스/어시스트, 주력 챔피언 수치 카드가 표시된다

**Given** 승률이 50% 이상일 때
**When** 수치를 확인하면
**Then** `#10B981` 색상이 적용되고 텍스트 레이블도 병행 표시된다 (NFR7)

---

### Story 3.4: 플레이어 상세 Chart.js 시각화

As a 유저,
I want 플레이어 상세 페이지에서 Elo 추이와 챔피언별 성적 차트를 확인하기를,
So that 해당 플레이어의 성장 추이와 챔피언 풀을 시각적으로 분석할 수 있다. (FR13)

**Acceptance Criteria:**

**Given** 유저가 플레이어 상세 페이지에 진입했을 때
**When** Chart.js가 동적 로딩으로 불러와지면
**Then** 2초 이내 Elo 추이 차트가 렌더링된다 (NFR4)

**Given** Elo 추이 차트가 렌더링되었을 때
**When** Elo 상승 구간을 확인하면
**Then** `#10B981` 색상, 하락 구간은 `#EF4444` 색상으로 시각화된다

**Given** 챔피언별 성적 차트가 표시될 때
**When** 각 챔피언 바를 확인하면
**Then** 챔피언명, 게임 수, 승률이 표시된다

---

### Story 3.5: 매치 목록 & 매치 상세

As a 유저,
I want 매치 목록에서 특정 경기를 클릭하여 상세 내역을 확인하기를,
So that 과거 게임의 양팀 구성과 성적을 파악할 수 있다. (FR6)

**Acceptance Criteria:**

**Given** 유저가 매치 목록 페이지에 있을 때
**When** 특정 매치 행을 클릭하면
**Then** 매치 상세 페이지로 이동한다

**Given** 매치 상세 페이지에 진입했을 때
**When** 페이지가 로드되면
**Then** 양팀 플레이어 목록, KDA, 챔피언 정보가 표시된다

**Given** 승리팀과 패배팀이 표시될 때
**When** 팀 결과를 확인하면
**Then** 승리는 `#10B981` + "승리" 텍스트, 패배는 `#EF4444` + "패배" 텍스트가 병행 표시된다 (NFR7)

---

### Story 3.6: PlayerLink/ChampionLink Hover 위젯 스타일 업데이트

As a 유저,
I want 플레이어/챔피언 이름에 마우스를 올리면 핵심 통계가 즉시 표시되기를,
So that 상세 페이지 이동 없이 컨텍스트를 유지하며 빠르게 통계를 확인할 수 있다. (FR8, FR9)

**Acceptance Criteria:**

**Given** 유저가 플레이어 이름 위에 마우스를 올렸을 때
**When** 220ms 딜레이 후
**Then** 300ms 이내 PlayerLink Popover가 표시된다 (NFR1)

**Given** PlayerLink Popover가 표시될 때
**When** 내용을 확인하면
**Then** 승률, KDA, 주력 챔피언, Elo 등 핵심 수치가 `font-mono`로 표시된다

**Given** PlayerLink 스타일이 업데이트될 때
**When** 기존 인터페이스를 확인하면
**Then** `riotId`, `children`, `className`, `mode` props가 그대로 유지된다

**Given** 동일 플레이어를 두 번 hover할 때
**When** 두 번째 hover가 발생하면
**Then** 모듈 레벨 Map 캐시에서 즉시 표시된다 (추가 API 호출 없음)

**Given** ChampionLink가 챔피언 이름에 적용될 때
**When** 마우스를 올리면
**Then** 동일한 타이밍과 패턴으로 챔피언 통계 Popover가 표시된다 (FR9)

---

### Story 3.7: Breadcrumb 계층 탐색

As a 유저,
I want 상세 페이지 상단의 Breadcrumb으로 상위 계층으로 돌아가기를,
So that 탐색 중 어디 있는지 파악하고 1클릭으로 이전 화면으로 복귀할 수 있다. (FR7)

**Acceptance Criteria:**

**Given** 유저가 챔피언 상세 페이지(`/champions/:id`)에 있을 때
**When** Breadcrumb을 확인하면
**Then** "홈 > 챔피언 > [챔피언명]" 형태로 상단에 표시된다

**Given** Breadcrumb의 "홈"을 클릭했을 때
**When** 클릭이 발생하면
**Then** 홈(`/`)으로 이동한다

**Given** Breadcrumb의 "챔피언"을 클릭했을 때
**When** 클릭이 발생하면
**Then** 챔피언 목록 페이지로 이동한다

**Given** 플레이어 상세 페이지에도 Breadcrumb이 있을 때
**When** 구조를 확인하면
**Then** "홈 > 플레이어 > [플레이어명]" 형태로 표시된다

---

## Epic 4: Electron 대기방 — 자동화된 밴픽 지원

유저가 LoL 대기방에 입장하면 자동으로 양팀 플레이어 카드와 밴 추천이 표시되어 별도 조작 없이 10초 안에 밴픽을 결정할 수 있다.

### Story 4.1: PlayerCard 컴포넌트 구현

As a 유저,
I want 대기방에서 각 플레이어의 정보를 카드 형태로 확인하기를,
So that 닉네임, Elo, 고승률 챔피언 TOP 3를 한눈에 스캔할 수 있다. (FR17, FR18)

**Acceptance Criteria:**

**Given** PlayerCard 컴포넌트가 렌더링될 때
**When** 카드 내용을 확인하면
**Then** 닉네임, Elo 수치(`font-mono`), 고승률 챔피언 TOP 3(챔피언 아이콘 + 승률)가 표시된다

**Given** PlayerCard가 로딩 중일 때
**When** 데이터가 아직 없으면
**Then** Skeleton 상태로 카드 레이아웃이 유지된다

**Given** PlayerCard가 expanded 상태일 때
**When** 카드를 클릭하면
**Then** 챔피언 목록이 TOP 3에서 더 많이 확장된다

**Given** PlayerCard에 접근성이 적용되었을 때
**When** 구조를 확인하면
**Then** `role="article"`, 닉네임에 `aria-label`이 설정된다

---

### Story 4.2: BanRecommendBadge 컴포넌트 구현

As a 유저,
I want 각 플레이어 카드에서 밴 추천 챔피언을 뱃지로 확인하기를,
So that 어떤 챔피언을 밴해야 할지 직관적으로 판단할 수 있다. (FR19)

**Acceptance Criteria:**

**Given** BanRecommendBadge 컴포넌트가 렌더링될 때
**When** 뱃지 내용을 확인하면
**Then** 챔피언명이 표시된다

**Given** 높은 위협도(상위 밴 추천)인 챔피언의 뱃지일 때
**When** 뱃지 색상을 확인하면
**Then** `#EF4444`(빨강) 배경이 적용된다

**Given** 일반 위협도인 챔피언의 뱃지일 때
**When** 뱃지 색상을 확인하면
**Then** `#00B4D8`(Teal) 배경이 적용된다

**Given** `<span>` 인라인 스타일로 구현되었을 때
**When** 컴포넌트 구조를 확인하면
**Then** 위협도에 따라 색상이 분기된 뱃지 형태다

---

### Story 4.3: ChampSelectPage 대기방 뷰 재설계 & 자동 트리거

As a 유저,
I want LoL 대기방에 입장했을 때 Electron이 자동으로 양팀 분석 화면을 표시하기를,
So that 별도 조작 없이 2초 안에 상대팀 정보와 밴 추천을 확인할 수 있다. (FR15, FR16, FR20)

**Acceptance Criteria:**

**Given** LoL 클라이언트에서 대기방에 진입했을 때
**When** LCU API가 대기방 이벤트를 감지하면
**Then** 2초 이내 ChampSelectPage에 양팀 PlayerCard가 자동으로 렌더링된다 (NFR2)

**Given** ChampSelectPage가 렌더링될 때
**When** 레이아웃을 확인하면
**Then** `grid-cols-2` 좌우 분할로 우리팀(좌)/상대팀(우) PlayerCard가 각 5개 배치된다 (FR17)

**Given** LoL 클라이언트가 실행되지 않은 상태일 때
**When** ChampSelectPage를 확인하면
**Then** "LoL 클라이언트를 실행해주세요" 안내 메시지가 표시된다 (FR20, NFR9)

**Given** 대기방 데이터 로딩 중일 때
**When** 카드들이 아직 데이터를 받지 못했으면
**Then** 모든 PlayerCard가 Skeleton 상태로 표시된다

**Given** 기존 lcu.ts 파일이 있을 때
**When** ChampSelectPage 기능을 구현해도
**Then** lcu.ts의 LCU API 연동 로직은 변경되지 않는다

---

## Epic 5: 모바일 웹 — 이동 중 리그 확인

유저가 스마트폰으로 `/m/*` 접속 시 하단 탭바로 리더보드/챔피언/개인 통계를 불편함 없이 탐색할 수 있다.

### Story 5.1: MobileLayout & MobileBottomNav 컴포넌트

As a 모바일 유저,
I want `/m/*` 경로에서 하단 탭바가 있는 모바일 전용 레이아웃을 사용하기를,
So that 엄지로 쉽게 주요 화면 간 이동할 수 있다. (FR21, FR22)

**Acceptance Criteria:**

**Given** 유저가 모바일에서 `/m`으로 접근했을 때
**When** MobileLayout이 렌더링되면
**Then** 상단 콘텐츠 영역과 하단 고정 탭바(`fixed bottom-0`)가 표시된다

**Given** MobileBottomNav 탭바가 표시될 때
**When** 탭 항목들을 확인하면
**Then** 홈(리더보드), 챔피언, 플레이어 탭이 각각 Lucide 아이콘 + 텍스트로 표시된다

**Given** 현재 활성 탭을 확인할 때
**When** 해당 탭 아이콘과 텍스트를 보면
**Then** Teal(`#00B4D8`) 색상으로 강조된다

**Given** iOS Safari에서 하단 탭바가 표시될 때
**When** safe area를 확인하면
**Then** `safe-area-inset-bottom`이 적용되어 홈바와 겹치지 않는다

**Given** 탭바의 각 탭을 확인할 때
**When** 터치 영역을 측정하면
**Then** 44px × 44px 이상이다 (NFR8)

---

### Story 5.2: 모바일 홈 — Elo 리더보드

As a 모바일 유저,
I want 모바일 홈에서 Elo 리더보드를 카드 형태로 확인하기를,
So that 내 순위와 전체 멤버 Elo를 스마트폰에서 불편함 없이 파악할 수 있다. (FR23)

**Acceptance Criteria:**

**Given** 유저가 모바일 홈(`/m`)에 접속했을 때
**When** 페이지가 로드되면
**Then** Elo 리더보드가 카드 리스트 형태로 표시된다 (데스크톱 테이블과 다른 모바일 변형)

**Given** 자신의 riotId가 리더보드에 있을 때
**When** 리더보드를 확인하면
**Then** 자신의 행이 하이라이트된다

**Given** 리더보드 각 항목을 확인할 때
**When** 터치 영역을 측정하면
**Then** 행 높이가 44px 이상이다 (NFR8)

**Given** 로딩 중일 때
**When** 데이터가 없으면
**Then** Skeleton 카드가 표시된다

---

### Story 5.3: 모바일 챔피언 티어표

As a 모바일 유저,
I want 모바일에서 챔피언 탭을 눌러 S/A/B/C 티어표를 확인하기를,
So that 이번 리그 메타 챔피언을 스마트폰에서 쉽게 파악할 수 있다. (FR24)

**Acceptance Criteria:**

**Given** 유저가 모바일 챔피언 탭을 눌렀을 때
**When** MobileChampionListPage가 로드되면
**Then** 챔피언 티어표가 모바일 최적화 형태로 표시된다

**Given** 챔피언 목록이 표시될 때
**When** 각 항목의 터치 영역을 확인하면
**Then** 행 높이가 44px 이상이다 (NFR8)

**Given** 승률이 표시될 때
**When** 수치를 확인하면
**Then** 50% 이상은 `#10B981`, 미만은 `#EF4444` + 텍스트 레이블 병행 (NFR7)

---

### Story 5.4: 모바일 플레이어 통계 페이지

As a 모바일 유저,
I want 모바일에서 플레이어 탭을 눌러 개인 통계를 확인하기를,
So that 내 성적이나 특정 멤버의 통계를 스마트폰에서 확인할 수 있다. (FR25)

**Acceptance Criteria:**

**Given** 유저가 모바일 플레이어 탭을 눌렀을 때
**When** MobilePlayerListPage가 로드되면
**Then** 플레이어 목록이 표시된다

**Given** 특정 플레이어를 탭했을 때
**When** MobilePlayerDetailPage가 로드되면
**Then** 승률, KDA, 주력 챔피언 등 핵심 통계가 모바일 카드 형태로 표시된다

**Given** 모바일 플레이어 상세 페이지에서
**When** 수치들을 확인하면
**Then** `font-mono` 폰트로 정렬되고 44px 이상 터치 가능 영역이 확보된다 (NFR8)
