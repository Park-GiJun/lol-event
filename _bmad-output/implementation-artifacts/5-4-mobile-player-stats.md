# Story 5.4: 모바일 플레이어 통계

Status: review

## Story

As a 모바일 유저,
I want 모바일에서 플레이어 탭을 눌러 개인 통계를 확인하기를,
So that 내 성적이나 특정 멤버의 통계를 스마트폰에서 확인할 수 있다. (FR25)

## Acceptance Criteria

1. **Given** 유저가 모바일 플레이어 목록(`/m/players`)에 접속했을 때
   **When** MobilePlayerListPage가 로드되면
   **Then** Skeleton 카드 5개가 표시되고, 데이터 로드 후 플레이어 카드 목록이 표시된다

2. **Given** 플레이어 목록 로딩 중 에러가 발생했을 때
   **When** 에러 상태를 확인하면
   **Then** InlineError + 재시도 버튼이 표시된다

3. **Given** 특정 플레이어 카드를 탭했을 때
   **When** MobilePlayerDetailPage(`/m/player/:riotId`)가 로드되면
   **Then** Skeleton이 표시되고, 로드 후 판수/승률/KDA/평균딜/CS/시야 6개 지표 + Elo 카드 + 챔피언별 통계 + 최근 경기가 표시된다

4. **Given** 플레이어 목록 각 항목을 확인할 때
   **When** 터치 영역을 측정하면
   **Then** 카드 전체 높이가 44px 이상이다 (NFR8)

## Tasks / Subtasks

- [x] Task 1: 훅 업데이트 (AC: #1, #2, #3)
  - [x] `usePlayers(mode?)` — mode 파라미터 추가: `queryKey: ['players', mode]`, `queryFn: () => api.get<StatsResponse>(\`/stats?mode=${mode ?? 'normal'}\`)`
  - [x] `usePlayerDetail(riotId)` 신규 훅 작성 — `queryKey: ['players', riotId, 'detail']`, React Query `useQuery`로 `/stats/player/:riotId?mode=all` 호출
  - [x] `usePlayerEloHistory(riotId)` 신규 훅 작성 — `queryKey: ['players', riotId, 'elo-history']`, `/stats/player/:riotId/elo-history?limit=20` 호출

- [x] Task 2: `MobilePlayerListPage.tsx` 리팩터링 (AC: #1, #2, #4)
  - [x] `api.get` + `useEffect/useState` → `usePlayers(mode)` 훅으로 교체
  - [x] `LoadingCenter` → Skeleton 카드 5개로 교체 (`Skeleton` 컴포넌트 사용)
  - [x] 에러 상태 → `InlineError` + `onRetry={() => void refetch()}` 추가
  - [x] 플레이어 카드 `<div>`에 `role="button"`, `tabIndex={0}`, `onKeyDown={e => { if (e.key === 'Enter' || e.key === ' ') navigate(...) }}` 추가
  - [x] `import` 경로를 `../../lib/...` → `@/lib/...` alias로 통일

- [x] Task 3: `MobilePlayerDetailPage.tsx` 리팩터링 (AC: #3)
  - [x] `api.get` + `useEffect/useState` → `usePlayerDetail(riotId)` + `usePlayerEloHistory(riotId)` 훅으로 교체
  - [x] `LoadingCenter` → Skeleton으로 교체 (헤더 카드 + overview grid + Elo 카드 placeholder)
  - [x] 에러 상태 → `InlineError` + refetch 버튼 추가
  - [x] `import` 경로 alias 통일

- [x] Task 4: TypeScript 검증
  - [x] `npx tsc --noEmit` 오류 없음 확인

## Dev Notes

### 핵심 원칙 — 바퀴 재발명 금지

| 재발명 위험 요소 | 올바른 접근 |
|---|---|
| `api.get` 직접 호출 | **반드시** `usePlayers()` / `usePlayerDetail()` 훅 사용 |
| `LoadingCenter` 사용 | **반드시** `Skeleton` 컴포넌트 사용 |
| 에러 상태 무처리 | **반드시** `InlineError` + `onRetry` 패턴 사용 |
| div onClick만 | `role="button"` + `tabIndex={0}` + `onKeyDown` 추가 필수 |
| `../../lib/...` 상대경로 | `@/lib/...` alias 사용 |

---

### 현재 구현 현황 — 이미 존재하는 파일들

두 페이지 파일이 **이미 존재**하나 anti-pattern 사용 중. 이 스토리는 **리팩터링**이다.

**`MobilePlayerListPage.tsx` 현재 문제:**
- `api.get<StatsResponse>('/stats?mode=${mode}')` 직접 호출 + `useEffect/useState`
- `LoadingCenter` 사용 (Skeleton 금지 위반)
- 에러 처리 없음 (try/finally만 있고 InlineError 없음)
- `div` onClick에 `role`/`tabIndex`/`onKeyDown` 없음
- `import` 경로가 `../../lib/...` (alias 미사용)

**`MobilePlayerDetailPage.tsx` 현재 문제:**
- `api.get` + `Promise.all` + `useEffect/useState`
- `LoadingCenter` 사용
- 에러 처리 없음
- `import` 경로가 `../../lib/...`

**`usePlayers.ts` 현재 상태:**
- mode 파라미터 없음 (`/stats` 고정)
- `usePlayerDetail`, `usePlayerEloHistory` 훅 없음

---

### 훅 구현 가이드

**`frontend/src/hooks/usePlayers.ts` — 수정:**

```ts
import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api/api';
import type { StatsResponse } from '@/lib/types/stats';

export function usePlayers(mode = 'normal') {
  return useQuery({
    queryKey: ['players', mode],
    queryFn: () => api.get<StatsResponse>(`/stats?mode=${mode}`),
  });
}
```

**`frontend/src/hooks/usePlayerDetail.ts` — 신규:**

```ts
import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api/api';
import type { PlayerDetailStats } from '@/lib/types/stats';

export function usePlayerDetail(riotId: string) {
  return useQuery({
    queryKey: ['players', riotId, 'detail'],
    queryFn: () => api.get<PlayerDetailStats>(`/stats/player/${encodeURIComponent(riotId)}?mode=all`),
    enabled: !!riotId,
  });
}
```

**`frontend/src/hooks/usePlayerEloHistory.ts` — 신규:**

```ts
import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api/api';
import type { PlayerEloHistoryResult } from '@/lib/types/stats';

export function usePlayerEloHistory(riotId: string) {
  return useQuery({
    queryKey: ['players', riotId, 'elo-history'],
    queryFn: () => api.get<PlayerEloHistoryResult>(`/stats/player/${encodeURIComponent(riotId)}/elo-history?limit=20`),
    enabled: !!riotId,
  });
}
```

---

### MobilePlayerListPage 리팩터링 가이드

**변경 전 → 변경 후 핵심 diff:**

```tsx
// BEFORE
import { useEffect, useState, useCallback } from 'react';
import { api } from '../../lib/api/api';
import { LoadingCenter } from '../../components/common/Spinner';
// ...
const [stats, setStats] = useState<PlayerStats[]>([]);
const [loading, setLoading] = useState(true);
useEffect(() => { load(); }, [load]);
if (loading) return <LoadingCenter />;

// AFTER
import { usePlayers } from '@/hooks/usePlayers';
import { Skeleton } from '@/components/common/Skeleton';
import { InlineError } from '@/components/common/InlineError';
// ...
const { data, isLoading, error, refetch } = usePlayers(mode);
if (isLoading) return <SkeletonList />;    // Skeleton 5개
if (error) return <InlineError message="플레이어 통계를 불러오지 못했습니다." onRetry={() => void refetch()} />;
const sorted = (data?.stats ?? []).slice().sort(...);
```

**Skeleton 5개 패턴 (MobileHomePage.tsx 참조):**

```tsx
// isLoading 시
<div>
  {Array.from({ length: 5 }).map((_, i) => (
    <div key={i} className="m-player-card">
      <div className="m-player-card-header">
        <Skeleton style={{ width: 32, height: 32, borderRadius: '50%', flexShrink: 0 }} />
        <div style={{ flex: 1 }}>
          <Skeleton style={{ height: 14, width: '50%', borderRadius: 4 }} />
        </div>
        <Skeleton style={{ width: 32, height: 14, borderRadius: 4 }} />
      </div>
    </div>
  ))}
</div>
```

**플레이어 카드 접근성 패턴 (MobileChampionListPage의 ChampionRow 참조):**

```tsx
<div
  role="button"
  tabIndex={0}
  className="m-player-card"
  onClick={() => navigate(`/m/player/${encodeURIComponent(p.riotId)}`)}
  onKeyDown={e => { if (e.key === 'Enter' || e.key === ' ') navigate(`/m/player/${encodeURIComponent(p.riotId)}`); }}
>
```

---

### MobilePlayerDetailPage 리팩터링 가이드

**변경 전 → 변경 후 핵심 diff:**

```tsx
// BEFORE
import { useEffect, useState } from 'react';
import { api } from '../../lib/api/api';
import { LoadingCenter } from '../../components/common/Spinner';
// ...
const [data, setData] = useState<PlayerDetailStats | null>(null);
const [loading, setLoading] = useState(true);
Promise.all([api.get(...), api.get(...)]).then(...).finally(() => setLoading(false));
if (loading) return <LoadingCenter />;

// AFTER
import { usePlayerDetail } from '@/hooks/usePlayerDetail';
import { usePlayerEloHistory } from '@/hooks/usePlayerEloHistory';
import { Skeleton } from '@/components/common/Skeleton';
import { InlineError } from '@/components/common/InlineError';
// ...
const decoded = riotId ? decodeURIComponent(riotId) : '';
const { data, isLoading: statsLoading, error: statsError, refetch } = usePlayerDetail(decoded);
const { data: eloHistory, isLoading: eloLoading } = usePlayerEloHistory(decoded);
const isLoading = statsLoading || eloLoading;
if (isLoading) return <PlayerDetailSkeleton />;
if (statsError || !data) return <InlineError message="플레이어 정보를 불러오지 못했습니다." onRetry={() => void refetch()} />;
```

**Skeleton 패턴 (상세 페이지):**

```tsx
// 헤더 카드 + 6칸 grid + Elo 카드
<div>
  <div className="m-card" style={{ marginBottom: 12 }}>
    <Skeleton style={{ height: 24, width: '40%', borderRadius: 4, marginBottom: 6 }} />
    <Skeleton style={{ height: 18, width: '25%', borderRadius: 4 }} />
  </div>
  <div className="m-overview-grid" style={{ marginBottom: 12 }}>
    {Array.from({ length: 6 }).map((_, i) => (
      <div key={i} className="m-overview-stat">
        <Skeleton style={{ height: 22, width: 48, borderRadius: 4, marginBottom: 4 }} />
        <Skeleton style={{ height: 12, width: 32, borderRadius: 4 }} />
      </div>
    ))}
  </div>
</div>
```

---

### 타입 참조 (`@/lib/types/stats`)

```ts
// 플레이어 목록
export interface StatsResponse {
  stats: PlayerStats[];
  matchCount: number;
}
export interface PlayerStats {
  riotId: string;
  games: number; wins: number; losses: number;
  winRate: number;
  avgKills: number; avgDeaths: number; avgAssists: number;
  kda: number; avgDamage: number; avgCs: number;
  topChampions: ChampionCount[];
}

// 플레이어 상세
export interface PlayerDetailStats {
  riotId: string;
  games: number; wins: number; losses: number;
  winRate: number; avgKills: number; avgDeaths: number; avgAssists: number;
  kda: number; avgDamage: number; avgCs: number;
  avgGold: number; avgVisionScore: number;
  elo: number; eloRank: number | null;
  championStats: ChampionStat[];
  recentMatches: RecentMatchStat[];
  laneStats: LaneStat[];
}

// Elo 히스토리
export interface PlayerEloHistoryResult {
  riotId: string;
  currentElo: number;
  eloRank: number | null;
  history: EloHistoryEntry[];
}
export interface EloHistoryEntry {
  delta: number;
  win: boolean;
  // ... 추가 필드
}
```

---

### 이전 스토리 학습 사항

**5.2에서 확립된 패턴 (반드시 준수):**
- `useLeaderboard()` 훅 사용 — 직접 `api.get` 금지
- `Skeleton` 컴포넌트 사용 — `LoadingCenter` 금지
- `InlineError` + `onRetry={() => void refetch()}` 에러 처리 필수
- `m-player-card` + 자연스러운 padding으로 44px 터치 영역 충족

**5.3에서 추가된 패턴:**
- clickable `<div>`에 `role="button"` + `tabIndex={0}` + `onKeyDown` 필수 (코드 리뷰 수정)
- `ChampionRow` 같은 per-item 컴포넌트 추출로 로컬 state 관리

**CSS 클래스 참조:**
```css
.m-player-card        /* 카드 컨테이너 — padding:12px, 클릭 가능 */
.m-player-card-header /* flex row */
.m-player-name        /* 닉네임 */
.m-player-tag         /* #태그 */
.m-player-rank        /* 순위 뱃지 */
.rank-1, .rank-2, .rank-3  /* 상위 3위 색상 */
.m-win-bar-wrap       /* 승률 바 컨테이너 */
.m-win-bar            /* 승률 바 배경 */
.m-win-bar-fill       /* 승률 바 채움 */
.m-stat-chips         /* 통계 칩 영역 */
.m-stat-chip          /* 통계 칩 */
.m-champ-icons        /* 챔피언 아이콘 영역 */
.m-sort-chip          /* 정렬 칩, .active */
.m-overview-grid      /* 6칸 통계 그리드 */
.m-overview-stat      /* 개별 통계 셀 */
.m-overview-stat-value /* 수치 */
.m-overview-stat-label /* 레이블 */
.m-elo-card           /* Elo 카드 */
.m-section-title      /* 섹션 헤더 */
.m-win-badge.win / .loss  /* 승/패 뱃지 */
.m-empty              /* 빈 데이터 */
```

---

### 완료 판단 기준

- [ ] `/m/players` 접속 시 Skeleton → 플레이어 카드 목록 표시
- [ ] 로딩 중 `Skeleton` 5개 표시 (`LoadingCenter` 사용 X)
- [ ] 에러 시 `InlineError` + 재시도 버튼
- [ ] 플레이어 카드 클릭 → `/m/player/:riotId` 이동 (키보드 포함)
- [ ] `/m/player/:riotId` 접속 시 Skeleton → 판수/승률/KDA/딜/CS/시야 + Elo + 챔피언 통계 + 최근 경기
- [ ] TypeScript 컴파일 오류 없음

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

- `usePlayers(mode?)`: mode 파라미터 추가, queryKey `['players', mode]`으로 캐시 분리
- `usePlayerDetail(riotId)`: 신규 — `/stats/player/:riotId?mode=all`, enabled guard 포함
- `usePlayerEloHistory(riotId)`: 신규 — `/stats/player/:riotId/elo-history?limit=20`, enabled guard 포함
- `MobilePlayerListPage`: `api.get` + `useEffect/useState` → `usePlayers(mode)` 훅, `LoadingCenter` → Skeleton 5개, `InlineError` 에러 처리, `role="button"` + `tabIndex` + `onKeyDown` 접근성, `@/` alias 통일
- `MobilePlayerDetailPage`: `api.get` + `Promise.all` → `usePlayerDetail` + `usePlayerEloHistory`, `LoadingCenter` → 구조화된 Skeleton, `InlineError` 에러 처리, `@/` alias 통일
- `npx tsc --noEmit` 오류 없음

### File List

- frontend/src/hooks/usePlayers.ts (수정 — mode 파라미터 추가)
- frontend/src/hooks/usePlayerDetail.ts (신규)
- frontend/src/hooks/usePlayerEloHistory.ts (신규)
- frontend/src/pages/mobile/MobilePlayerListPage.tsx (수정 — 훅/Skeleton/InlineError 리팩터링)
- frontend/src/pages/mobile/MobilePlayerDetailPage.tsx (수정 — 훅/Skeleton/InlineError 리팩터링)
