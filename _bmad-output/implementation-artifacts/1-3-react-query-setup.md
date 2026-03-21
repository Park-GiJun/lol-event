# Story 1.3: React Query QueryClient 설정 & 공통 훅 레이어

Status: done

## Story

As a 개발자,
I want React Query QueryClient가 기본 캐시 정책으로 설정되고, 공통 데이터 훅이 생성되기를,
so that 모든 플랫폼에서 staleTime 5분, gcTime 30분 기준의 캐싱이 일관되게 적용된다.

## Acceptance Criteria

1. **Given** `main.tsx`에 QueryClientProvider가 감싸져 있을 때
   **When** 앱이 로드되면
   **Then** 기본 `staleTime: 5 * 60 * 1000`, `gcTime: 30 * 60 * 1000`이 적용된 QueryClient가 동작한다

2. **Given** `hooks/useLeaderboard.ts`가 생성되었을 때
   **When** `useLeaderboard()`를 호출하면
   **Then** `['leaderboard']` 쿼리 키로 React Query가 동작하고 `{ data, isLoading, error, refetch }`를 반환한다

3. **Given** 동일한 방식으로 `useChampions`, `usePlayers`, `useMatches`, `useBanRecommend` 훅이 생성되었을 때
   **When** 각 훅을 호출하면
   **Then** 정해진 Query Key 컨벤션(`['champions']`, `['players']`, `['matches']`, `['bans', 'recommend']`)으로 동작한다

4. **Given** `api.get<T>(endpoint)`를 `queryFn`으로 사용할 때
   **When** API 호출이 실패하면
   **Then** React Query의 `error` 상태가 설정되고 `data`는 undefined가 된다

## Tasks / Subtasks

- [x] Task 1: `@tanstack/react-query` 설치 (AC: #1)
  - [x]`frontend/` 디렉토리에서: `npm install @tanstack/react-query` 실행
  - [x]package.json에 `@tanstack/react-query` 추가 확인

- [x] Task 2: QueryClient 설정 파일 생성 (AC: #1)
  - [x]`frontend/src/lib/query-client.ts` 파일 생성
  - [x]`staleTime: 5 * 60 * 1000`, `gcTime: 30 * 60 * 1000` 기본값으로 QueryClient 생성
  - [x]named export로 `queryClient` 인스턴스 export

- [x] Task 3: `main.tsx`에 QueryClientProvider 래핑 (AC: #1)
  - [x]`@tanstack/react-query`에서 `QueryClientProvider` import
  - [x]`query-client.ts`에서 `queryClient` import
  - [x]`<App />`을 `<QueryClientProvider client={queryClient}>` 로 감싸기

- [x] Task 4: `useLeaderboard.ts` 훅 생성 (AC: #2)
  - [x]`frontend/src/hooks/useLeaderboard.ts` 파일 생성
  - [x]queryKey: `['leaderboard']`, queryFn: `api.get<EloLeaderboardResult>('/stats/elo')`
  - [x]`{ data, isLoading, error, refetch }` 반환

- [x] Task 5: 나머지 4개 훅 생성 (AC: #3)
  - [x]`frontend/src/hooks/useChampions.ts` — queryKey: `['champions']`
  - [x]`frontend/src/hooks/usePlayers.ts` — queryKey: `['players']`
  - [x]`frontend/src/hooks/useMatches.ts` — queryKey: `['matches']`
  - [x]`frontend/src/hooks/useBanRecommend.ts` — queryKey: `['bans', 'recommend']`

- [x] Task 6: TypeScript 검증 (AC: #4)
  - [x]`npx tsc --noEmit` 오류 없이 통과 확인
  - [x]기존 컴포넌트에서 regression 없음 확인 (dev build 실행)

## Dev Notes

### 🚨 Story 1.1, 1.2 학습 사항 (반드시 읽을 것)

- **Tailwind CSS v4** `@tailwindcss/vite` 방식으로 설치됨 (postcss 아님)
- **`@/` path alias** 설정 완료: `tsconfig.app.json`의 `paths` + `vite.config.ts`의 `resolve.alias`
  - 훅 파일 import: `import { useLeaderboard } from '@/hooks/useLeaderboard'`
- **Named export 규칙** 엄수 — 모든 훅, 함수는 named export (default export 금지)
- **TypeScript strict mode**: `noUnusedLocals: true`, `noUnusedParameters: true` — 사용 안 되는 import/변수 제거

### Task 1 상세: 패키지 설치

```bash
cd frontend && npm install @tanstack/react-query
```

React Query DevTools는 이 스토리 범위 밖 — 설치 불필요.

### Task 2 상세: query-client.ts 최종 형태

```ts
// frontend/src/lib/query-client.ts
import { QueryClient } from '@tanstack/react-query';

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000,   // 5분 — 통계 데이터는 게임 싱크 후 빈번한 재요청 불필요
      gcTime: 30 * 60 * 1000,     // 30분 — 리더보드/챔피언/플레이어/매치 전체 동일 적용
    },
  },
});
```

### Task 3 상세: main.tsx 최종 형태

```tsx
// frontend/src/main.tsx
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClientProvider } from '@tanstack/react-query'
import { queryClient } from './lib/query-client'
import './index.css'
import App from './App.tsx'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <App />
    </QueryClientProvider>
  </StrictMode>,
)
```

⚠️ `App`은 default import — 기존 패턴 유지 (App.tsx는 코딩 컨벤션의 default export 예외 파일).

### Task 4 상세: useLeaderboard.ts 최종 형태

```ts
// frontend/src/hooks/useLeaderboard.ts
import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api/api';
import type { EloLeaderboardResult } from '@/lib/types/stats';

export function useLeaderboard() {
  return useQuery({
    queryKey: ['leaderboard'],
    queryFn: () => api.get<EloLeaderboardResult>('/stats/elo'),
  });
}
```

**반환값**: `useQuery`의 반환 객체 그대로 사용 — `{ data, isLoading, error, refetch, ... }` 포함.
**staleTime/gcTime**: QueryClient 전역 defaultOptions에서 적용되므로 각 훅에 재정의 불필요.

### Task 5 상세: 나머지 훅 엔드포인트 및 타입 매핑

| 훅 파일 | queryKey | queryFn 엔드포인트 | 반환 타입 | import 경로 |
|---|---|---|---|---|
| `useChampions.ts` | `['champions']` | `api.get<OverviewStats>('/stats/overview')` | `OverviewStats` | `@/lib/types/stats` |
| `usePlayers.ts` | `['players']` | `api.get<StatsResponse>('/stats')` | `StatsResponse` | `@/lib/types/stats` |
| `useMatches.ts` | `['matches']` | `api.get<Match[]>('/matches')` | `Match[]` | `@/lib/types/match` |
| `useBanRecommend.ts` | `['bans', 'recommend']` | `api.get<OverviewStats>('/stats/overview')` | `OverviewStats` | `@/lib/types/stats` |

> ℹ️ **useChampions / useBanRecommend 엔드포인트 노트:**
> - `useChampions`는 현재 `/stats/overview` 사용 — Epic 2 구현 시 전용 챔피언 통계 엔드포인트로 교체 가능.
> - `useBanRecommend`는 현재 overview 데이터 재활용 — Epic 4(Electron 밴픽) 구현 시 전용 LCU 기반 엔드포인트로 교체 예정.
> - 이 스토리 목표는 **Query Key 컨벤션과 훅 인터페이스 확립**이며, 정확한 엔드포인트는 각 Epic에서 세부 조정.

```ts
// 예시: usePlayers.ts
import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api/api';
import type { StatsResponse } from '@/lib/types/stats';

export function usePlayers() {
  return useQuery({
    queryKey: ['players'],
    queryFn: () => api.get<StatsResponse>('/stats'),
  });
}

// 예시: useMatches.ts
import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api/api';
import type { Match } from '@/lib/types/match';

export function useMatches() {
  return useQuery({
    queryKey: ['matches'],
    queryFn: () => api.get<Match[]>('/matches'),
  });
}
```

### 기존 API 레이어 (수정 불필요)

```
frontend/src/lib/api/api.ts — 이미 존재, 수정 불필요
```

`api.get<T>(endpoint)` 는 이미 `ApiResponse<T>` 를 자동 언래핑하여 `T` 를 반환함.
실패 시 `Error` throw → React Query가 자동으로 `error` 상태로 전환 (AC #4 자동 충족).

### 기존 타입 파일 (import만 하면 됨)

```
frontend/src/lib/types/stats.ts — EloLeaderboardResult, OverviewStats, StatsResponse, ChampionStat
frontend/src/lib/types/match.ts — Match
```

**주의**: `Match` 타입은 `lib/types/match.ts` 에 있음 (stats.ts 아님).

### 코딩 컨벤션

```ts
// ✅ Named export (default export 금지)
export function useLeaderboard() { ... }
export function usePlayers() { ... }

// ❌ 금지
export default function useLeaderboard() { ... }
```

```ts
// ✅ 올바른 api 사용 (타입 추론 포함)
queryFn: () => api.get<EloLeaderboardResult>('/stats/elo')

// ❌ 금지 — 직접 fetch 사용
queryFn: () => fetch('/api/stats/elo').then(r => r.json())

// ❌ 금지 — 타입 생략
queryFn: () => api.get('/stats/elo')
```

### 파일 위치 규칙

```
frontend/src/
  main.tsx                          ← QueryClientProvider 래핑 (수정)
  lib/
    query-client.ts                 ← QueryClient 인스턴스 (신규)
    api/
      api.ts                        ← 수정 없음
    types/
      stats.ts                      ← 수정 없음
      match.ts                      ← 수정 없음
  hooks/                            ← 신규 폴더
    useLeaderboard.ts               ← 신규
    useChampions.ts                 ← 신규
    usePlayers.ts                   ← 신규
    useMatches.ts                   ← 신규
    useBanRecommend.ts              ← 신규
```

### tsconfig strict 모드 주의사항

- 훅 파일들은 export만 해도 TypeScript 오류 없음 (import되지 않아도 됨)
- `verbatimModuleSyntax: true` — 타입 전용 import는 `import type` 사용 필수

```ts
// ✅ 타입 import
import type { EloLeaderboardResult } from '@/lib/types/stats';

// ❌ 런타임 import로 타입만 사용 (verbatimModuleSyntax 위반 가능)
import { EloLeaderboardResult } from '@/lib/types/stats';
```

### 다음 스토리 연계

- **Story 1.4**: `InlineError`, `Skeleton` 공통 컴포넌트 생성 — 이 스토리의 `{ isLoading, error, refetch }` 패턴을 활용
- **Story 2.x**: 이 훅들을 실제 컴포넌트에서 사용 시작
- **Epic 4**: `useBanRecommend` 엔드포인트를 LCU 기반으로 교체

### References

- React Query QueryClient 설정: `_bmad-output/planning-artifacts/architecture.md` (Lines 118-134)
- 훅 네이밍 컨벤션: `_bmad-output/planning-artifacts/architecture.md` (Lines 244-256)
- 에러 핸들링 패턴: `_bmad-output/planning-artifacts/architecture.md` (Lines 327-335)
- 기존 API 클라이언트: `frontend/src/lib/api/api.ts`
- AC 원본: `_bmad-output/planning-artifacts/epics.md` (Story 1.3, Lines 210-233)

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

- ✅ `@tanstack/react-query` 설치 완료 (2개 패키지 추가)
- ✅ `frontend/src/lib/query-client.ts` — QueryClient (staleTime 5분, gcTime 30분) 생성
- ✅ `frontend/src/main.tsx` — QueryClientProvider 래핑 완료
- ✅ `frontend/src/hooks/useLeaderboard.ts` — queryKey `['leaderboard']`, `/stats/elo`
- ✅ `frontend/src/hooks/useChampions.ts` — queryKey `['champions']`, `/stats/overview`
- ✅ `frontend/src/hooks/usePlayers.ts` — queryKey `['players']`, `/stats`
- ✅ `frontend/src/hooks/useMatches.ts` — queryKey `['matches']`, `/matches`
- ✅ `frontend/src/hooks/useBanRecommend.ts` — queryKey `['bans', 'recommend']`, `/stats/overview`
- ✅ TypeScript `tsc --noEmit` 오류 없이 통과
- ✅ 모든 훅 named export 규칙 준수
- ✅ 모든 타입 import에 `import type` 사용 (verbatimModuleSyntax 준수)

### File List

- `frontend/src/main.tsx` (수정)
- `frontend/src/lib/query-client.ts` (신규)
- `frontend/src/hooks/useLeaderboard.ts` (신규)
- `frontend/src/hooks/useChampions.ts` (신규)
- `frontend/src/hooks/usePlayers.ts` (신규)
- `frontend/src/hooks/useMatches.ts` (신규)
- `frontend/src/hooks/useBanRecommend.ts` (신규)
