# Story 1.5: App.tsx 라우팅 재구성 & 폴더 구조 정비

Status: done

## Story

As a 개발자,
I want App.tsx 라우팅에 `/m/*` 모바일 nested routes가 추가되고, 신규 컴포넌트 폴더가 생성되기를,
So that Dashboard와 모바일 뷰가 완전히 분리된 라우팅으로 동작한다.

## Acceptance Criteria

1. **Given** `App.tsx`에 React Router 라우팅이 설정되었을 때
   **When** 브라우저에서 `/m`으로 접근하면
   **Then** `<MobileLayout />`이 렌더링된다

2. **Given** `/m` 경로에 nested routes가 설정되었을 때
   **When** `/m/champions`로 접근하면
   **Then** 모바일 챔피언 페이지가 렌더링되고 데스크톱 사이드바 레이아웃은 표시되지 않는다

3. **Given** 신규 폴더 구조가 생성되었을 때
   **When** 개발자가 `components/dashboard/`, `components/electron/`, `components/mobile/` 폴더를 확인하면
   **Then** 각 폴더가 존재한다

4. **Given** 기존 AdminPage, SyncPage, LcuPage, MonitoringPage 라우트가 있을 때
   **When** App.tsx를 수정해도
   **Then** 기존 관리자 페이지 라우트는 그대로 동작한다 (FR27)

## Tasks / Subtasks

- [x] Task 1: App.tsx 라우팅 확인 (AC: #1, #2, #4)
  - [x] `frontend/src/App.tsx` 읽기 — `/m/*` 모바일 nested routes 이미 구현 확인
  - [x] `<Route path="m" element={<MobileLayout />}>` 및 `/m/champions` 라우트 존재 확인
  - [x] 기존 AdminPage, SyncPage, LcuPage 라우트 유지 확인
  - [x] 수정 불필요 시 해당 Task 완료 처리 (이미 구현됨)

- [x] Task 2: 신규 컴포넌트 폴더 생성 (AC: #3)
  - [x] `frontend/src/components/dashboard/.gitkeep` 생성
  - [x] `frontend/src/components/electron/.gitkeep` 생성
  - [x] `frontend/src/components/mobile/.gitkeep` 생성

- [x] Task 3: TypeScript 검증 (AC: #1, #2, #4)
  - [x] `npx tsc --noEmit` 오류 없이 통과 확인

## Dev Notes

### 🚨 CRITICAL: 라우팅은 이미 구현됨 — 재구현 금지

`App.tsx`는 commit `8eca530` ("feat: 모바일 전용 라우팅 시스템 구현 (/m/*)")에서 이미 완성됨.

**현재 `frontend/src/App.tsx` 구조 (수정 불필요):**

```tsx
// Desktop routes — <Route element={<Layout />}> 아래
<Route index element={<StatsPage />} />
<Route path="player-stats" element={<MemberStatsListPage />} />
<Route path="player-stats/:riotId" element={<PlayerStatsPage />} />
<Route path="stats/player/:riotId" element={<PlayerStatsPage />} />
<Route path="stats/champion/:champion" element={<ChampionStatsPage />} />
<Route path="members" element={<MembersPage />} />
<Route path="matches" element={<MatchesPage />} />
<Route path="lcu" element={<LcuPage />} />
<Route path="sync" element={<SyncPage />} />
<Route path="admin" element={<AdminPage />} />

// Mobile routes — 이미 nested routes로 구현됨
<Route path="m" element={<MobileLayout />}>
  <Route index element={<MobileStatsPage />} />
  <Route path="matches" element={<MobileMatchesPage />} />
  <Route path="players" element={<MobilePlayerListPage />} />
  <Route path="player/:riotId" element={<MobilePlayerDetailPage />} />
  <Route path="champions" element={<MobileChampionListPage />} />
  <Route path="champion/:champion" element={<MobileChampionDetailPage />} />
  <Route path="more" element={<MobileMorePage />} />
  <Route path="members" element={<MobileMembersPage />} />
  <Route path="admin" element={<MobileAdminPage />} />
  <Route path="sync" element={<MobileSyncPage />} />
  <Route path="lcu" element={<MobileLcuPage />} />
  <Route path="*" element={<Navigate to="/m" replace />} />
</Route>
```

**AC1, AC2, AC4 → App.tsx 현재 코드로 모두 충족. 수정 없이 Task 1 완료 처리.**

### Task 2 상세: 신규 폴더 `.gitkeep` 파일 생성

아키텍처 문서 기준 신규 컴포넌트 폴더 3개가 필요하다 (현재 미존재):

```
frontend/src/components/
  common/      ← 기존 존재 (Button, ErrorModal, InlineError, Skeleton)
  layout/      ← 기존 존재 (Layout, MobileLayout)
  ui/          ← 기존 존재 (shadcn 컴포넌트)
  dashboard/   ← 신규 생성 필요 (Epic 2에서 EloLeaderboard 등 추가 예정)
  electron/    ← 신규 생성 필요 (Epic 4에서 PlayerCard 등 추가 예정)
  mobile/      ← 신규 생성 필요 (Epic 5에서 MobileBottomNav 등 추가 예정)
```

각 폴더에 `.gitkeep` 파일만 생성하면 됨 — 컴포넌트 구현은 각 Epic에서 진행.

**주의:** `frontend/src/pages/mobile/` 폴더는 이미 존재하며 11개 Mobile 페이지가 있음. 이와 혼동 금지.
- `pages/mobile/` → 모바일 페이지 컴포넌트 (이미 존재)
- `components/mobile/` → 모바일 전용 UI 컴포넌트 (신규, MobileBottomNav 등)

### 현재 폴더 구조 현황

```
frontend/src/
  components/
    common/          ← 기존 (Button, ErrorModal, Modal, InlineError, Skeleton 등)
    layout/          ← 기존 (Layout.tsx, MobileLayout.tsx)
    ui/              ← 기존 (shadcn 8개 컴포넌트)
    dashboard/       ← ❌ 미존재 → Task 2에서 생성
    electron/        ← ❌ 미존재 → Task 2에서 생성
    mobile/          ← ❌ 미존재 → Task 2에서 생성
  pages/
    AdminPage.tsx    ← 기존
    StatsPage.tsx    ← 기존 (현재 홈, Epic 2에서 HomePage로 재설계)
    mobile/          ← 기존 (11개 페이지)
    ...
  hooks/
    useLeaderboard.ts, useChampions.ts, usePlayers.ts, useMatches.ts, useBanRecommend.ts
```

### 코딩 컨벤션 (이 스토리 범위에서는 관련 없으나 참고)

```
components/  → PascalCase.tsx          (예: EloLeaderboard.tsx)
pages/       → PascalCasePage.tsx      (예: ChampionDetailPage.tsx)
hooks/       → useCamelCase.ts         (예: useLeaderboard.ts)
```

### 다음 스토리 연계

- **Epic 2 Story 2.1**: `components/dashboard/` 폴더에 EloLeaderboard, ChampionTierTable, BanTrendCard 추가
- **Epic 4**: `components/electron/` 폴더에 PlayerCard, BanRecommendBadge 추가
- **Epic 5**: `components/mobile/` 폴더에 MobileBottomNav 추가
- `pages/HomePage.tsx` 신규 생성은 Story 2.1에서 진행 (이 스토리 범위 밖)

### References

- AC 원본: `_bmad-output/planning-artifacts/epics.md` (Lines 260-284)
- 아키텍처 폴더 구조: `_bmad-output/planning-artifacts/architecture.md` (Lines 380-420)
- 기존 App.tsx: `frontend/src/App.tsx`
- 기존 모바일 라우팅 commit: `8eca530`

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

- ✅ `frontend/src/App.tsx` — 라우팅 이미 완성 (commit 8eca530). `/m/*` nested routes, MobileLayout, AdminPage/SyncPage/LcuPage 모두 존재. 수정 불필요.
- ✅ `frontend/src/components/dashboard/.gitkeep` — 신규 폴더 생성
- ✅ `frontend/src/components/electron/.gitkeep` — 신규 폴더 생성
- ✅ `frontend/src/components/mobile/.gitkeep` — 신규 폴더 생성
- ✅ TypeScript `tsc --noEmit` 오류 없이 통과

### File List

- `frontend/src/components/dashboard/.gitkeep` (신규)
- `frontend/src/components/electron/.gitkeep` (신규)
- `frontend/src/components/mobile/.gitkeep` (신규)

