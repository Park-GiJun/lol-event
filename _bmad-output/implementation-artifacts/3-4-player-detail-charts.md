# Story 3.4: 플레이어 상세 Chart.js 시각화

Status: done

## Story

As a 유저,
I want 플레이어 상세 페이지에서 Elo 추이와 챔피언별 성적 차트를 확인하기를,
So that 해당 플레이어의 성장 추이와 챔피언 풀을 시각적으로 분석할 수 있다. (FR13)

## Acceptance Criteria

1. **Given** 유저가 플레이어 상세 페이지에 진입했을 때
   **When** Chart.js가 동적 로딩(`React.lazy`)으로 불러와지면
   **Then** 2초 이내 Elo 추이 라인 차트가 렌더링된다 (NFR4)

2. **Given** Elo 추이 라인 차트가 렌더링되었을 때
   **When** Elo 상승 구간을 확인하면
   **Then** `#10B981` (초록) 색상, 하락 구간은 `#EF4444` (빨강) 색상으로 시각화된다

3. **Given** 챔피언별 성적 바 차트가 표시될 때
   **When** 각 챔피언 바에 마우스를 올리면
   **Then** 툴팁에 챔피언명, 게임 수, 승률이 표시된다

4. **Given** Chart.js가 동적 임포트로 적용되었을 때
   **When** 홈/목록 페이지 번들을 확인하면
   **Then** Chart.js가 포함되지 않는다 (NFR5)

5. **Given** Chart.js 차트 컴포넌트가 로딩 중일 때
   **When** `React.Suspense` fallback이 표시되면
   **Then** `ChartSkeleton` 컴포넌트가 표시된다 (레이아웃 시프트 없음)

6. **Given** `eloHistory`가 null이거나 history가 비어 있을 때
   **When** Elo 차트 영역을 보면
   **Then** Elo 차트가 숨겨진다 (조건부 렌더링)

7. **Given** `championStats`가 비어 있을 때
   **When** 챔피언 차트 영역을 보면
   **Then** 챔피언 차트가 숨겨진다 (조건부 렌더링)

## Tasks / Subtasks

- [x] Task 1: `PlayerChartsSection.tsx` 신규 생성 (AC: #1, #2, #3, #6, #7)
  - [x] `frontend/src/components/dashboard/PlayerChartsSection.tsx` 신규 생성
  - [x] Chart.js 등록: `ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, BarElement, Tooltip, Legend, Filler)` — 모듈 최상단
  - [x] **차트 1 — Elo 추이 라인 차트**: `eloHistory.history`를 역순(오래된 것 먼저)으로 표시
    - X축: `gameCreation` 기반 날짜 (`MM/DD` 형식)
    - Y축: `eloAfter` 값
    - 상승 구간: `borderColor: '#10B981'`, 하락 구간: `borderColor: '#EF4444'` (`segment` 콜백 사용)
    - `eloHistory === null` 또는 `history.length === 0` → 차트 숨김
  - [x] **차트 2 — 챔피언별 성적 바 차트**: `championStats` 상위 8개
    - X축: 챔피언 한국어 이름 (`useDragon().champions.get(c.championId)?.nameKo ?? c.champion`)
    - Y축: 승률(%)
    - 바 색상: 승률 ≥50% `rgba(34,197,94,0.7)`, <50% `rgba(239,68,68,0.7)`
    - 툴팁 `afterLabel`: `${c.games}판`
    - `championStats.length === 0` → 차트 숨김
  - [x] props: `eloHistory: PlayerEloHistoryResult | null`, `championStats: ChampionStat[]`
  - [x] 두 차트 모두 없으면 `null` 반환 (컴포넌트 전체 조건부)
  - [x] `key={riotId}` prop 없이 — 부모에서 `key={riotId-mode}` 전달로 리마운트 처리

- [x] Task 2: `PlayerStatsPage.tsx` 수정 — React.lazy + Suspense + ChartErrorBoundary 적용 (AC: #4, #5)
  - [x] `lazy`, `Suspense` import 추가 (`import { ..., lazy, Suspense } from 'react'`)
  - [x] `ChartSkeleton` import 추가 (`import { ChartSkeleton } from '../components/common/ChartSkeleton'`)
  - [x] `PlayerChartsSection` lazy 동적 임포트 (컴포넌트 외부, 파일 최상단 레벨)
  - [x] `ChartErrorBoundary` 클래스 신규 정의 (PlayerStatsPage.tsx 파일 최상단 레벨)
  - [x] 배치 위치: 챔피언 테이블(`ChampionTable`) 아래, 최근 경기(`recentMatches`) 위

- [x] Task 3: TypeScript 검증
  - [x] `npx tsc --noEmit` 오류 없이 통과 확인

## Dev Notes

### 🚨 CRITICAL: Story 3.2 학습 사항 — 반드시 준수

이 스토리는 Story 3.2 (챔피언 상세 Chart.js)와 동일한 패턴. 아래 사항은 3.2 코드 리뷰에서 도출된 필수 규칙:

1. **`height: '220px'` (string)** — `height: 220` (number)로 쓰면 Canvas 0px 렌더링
2. **`ChartOptions<'bar'>` 타입** — `as const` 금지 (TypeScript 타입 불일치)
3. **`ChartErrorBoundary`** 로 Suspense 감싸기 — lazy 로드 실패 시 페이지 크래시 방지
4. **Named export** + `.then(m => ({ default: m.PlayerChartsSection }))` 패턴 — React.lazy는 default export만 지원
5. **`ChartJS.register()`는 파일 최상단(컴포넌트 함수 바깥)** 에서 호출

### Task 1 상세: PlayerChartsSection.tsx 전체 구조

```tsx
// frontend/src/components/dashboard/PlayerChartsSection.tsx
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  Tooltip,
  Legend,
  Filler,
} from 'chart.js';
import type { ChartOptions } from 'chart.js';
import { Line, Bar } from 'react-chartjs-2';
import { useDragon } from '@/context/DragonContext';
import type { PlayerEloHistoryResult, ChampionStat } from '@/lib/types/stats';

ChartJS.register(
  CategoryScale, LinearScale, PointElement, LineElement,
  BarElement, Tooltip, Legend, Filler,
);

interface Props {
  eloHistory: PlayerEloHistoryResult | null;
  championStats: ChampionStat[];
}

export function PlayerChartsSection({ eloHistory, championStats }: Props) {
  const { champions } = useDragon();

  const hasElo = eloHistory !== null && eloHistory.history.length > 0;
  const hasChamp = championStats.length > 0;
  if (!hasElo && !hasChamp) return null;

  // Elo 차트 데이터 — history는 최신이 앞에 있으므로 역순 처리
  const reversed = hasElo ? [...eloHistory!.history].reverse() : [];
  const eloLabels = reversed.map(h => {
    const d = new Date(h.gameCreation);
    return `${d.getMonth() + 1}/${d.getDate()}`;
  });
  const eloValues = reversed.map(h => h.eloAfter);

  const eloOptions: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: { enabled: true },
    },
    scales: {
      y: { beginAtZero: false },
    },
    elements: {
      line: {
        tension: 0.3,
      },
    },
  };

  const eloData = {
    labels: eloLabels,
    datasets: [{
      label: 'Elo',
      data: eloValues,
      borderWidth: 2,
      pointRadius: 3,
      fill: false,
      segment: {
        borderColor: (ctx: { p0: { raw: number }; p1: { raw: number } }) =>
          ctx.p1.raw >= ctx.p0.raw ? '#10B981' : '#EF4444',
      },
      pointBackgroundColor: eloValues.map((v, i) =>
        i === 0 ? '#10B981' : v >= eloValues[i - 1] ? '#10B981' : '#EF4444'
      ),
    }],
  };

  // 챔피언 차트 데이터 — 판수 기준 상위 8개
  const topChamps = [...championStats].sort((a, b) => b.games - a.games).slice(0, 8);
  const champLabels = topChamps.map(c => champions.get(c.championId)?.nameKo ?? c.champion);

  const champOptions: ChartOptions<'bar'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: {
        enabled: true,
        callbacks: {
          afterLabel: (ctx) => `${topChamps[ctx.dataIndex]?.games ?? 0}판`,
        },
      },
    },
    scales: {
      y: { beginAtZero: true, max: 100 },
    },
  };

  const champData = {
    labels: champLabels,
    datasets: [{
      label: '승률 (%)',
      data: topChamps.map(c => c.winRate),
      backgroundColor: topChamps.map(c =>
        c.winRate >= 50 ? 'rgba(34,197,94,0.7)' : 'rgba(239,68,68,0.7)'
      ),
      borderRadius: 4,
    }],
  };

  return (
    <div className="card" style={{ marginBottom: 20 }}>
      <div style={{ fontWeight: 700, fontSize: 'var(--font-size-sm)', marginBottom: 16 }}>
        통계 차트
      </div>
      <div style={{ display: 'flex', gap: 24, flexWrap: 'wrap' }}>
        {hasElo && (
          <div style={{ flex: '1 1 280px', height: '220px' }}>
            <div style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginBottom: 8 }}>Elo 추이</div>
            <Line data={eloData} options={eloOptions} />
          </div>
        )}
        {hasChamp && (
          <div style={{ flex: '1 1 280px', height: '220px' }}>
            <div style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginBottom: 8 }}>챔피언별 승률</div>
            <Bar data={champData} options={champOptions} />
          </div>
        )}
      </div>
    </div>
  );
}
```

### Task 2 상세: PlayerStatsPage 수정 포인트

**기존 import 라인 수정:**
```tsx
// 기존:
import { useEffect, useState, useCallback } from 'react';
// 변경:
import { useEffect, useState, useCallback, lazy, Suspense } from 'react';
```

**파일 최상단 레벨 (import 아래, 컴포넌트 정의 위):**
```tsx
const PlayerChartsSection = lazy(
  () => import('../components/dashboard/PlayerChartsSection')
    .then(m => ({ default: m.PlayerChartsSection }))
);
```

**`ChartSkeleton` import 추가:**
```tsx
import { ChartSkeleton } from '../components/common/ChartSkeleton';
```

**`ChartErrorBoundary`**: 이미 `PlayerStatsPage.tsx` 내에 정의됨 (Story 3.3 때 추가됨) — 추가 불필요.

**배치 — `ChampionTable` 아래, `recentMatches` 위:**
```tsx
{/* 챔피언 통계 */}
{data.championStats.length > 0 && <ChampionTable stats={data.championStats} />}

{/* 통계 차트 (Story 3.4) */}
{(data.championStats.length > 0 || (eloHistory && eloHistory.history.length > 0)) && (
  <ChartErrorBoundary>
    <Suspense fallback={<ChartSkeleton />}>
      <PlayerChartsSection
        key={`${riotId}-${mode}`}
        eloHistory={eloHistory}
        championStats={data.championStats}
      />
    </Suspense>
  </ChartErrorBoundary>
)}

{/* 최근 경기 */}
{data.recentMatches.length > 0 && (
```

### Elo 추이 차트 `segment` 콜백 TypeScript 처리

Chart.js 4.x에서 `segment` 콜백 타입이 `ScriptableLineSegment`이다.
TypeScript가 추론하지 못할 경우 `any` 캐스팅 대신 아래 타입 선언 사용:

```tsx
import type { ScriptableLineSegment } from 'chart.js';

segment: {
  borderColor: (ctx: ScriptableLineSegment) =>
    (ctx.p1.parsed.y ?? 0) >= (ctx.p0.parsed.y ?? 0) ? '#10B981' : '#EF4444',
},
```

`p0.raw`/`p1.raw`가 아닌 `p0.parsed.y`/`p1.parsed.y` 사용에 주의.

### `Filler` 등록 필요 이유

`Filler` 플러그인은 Line 차트의 `fill` 옵션 지원을 위해 필요. 현재 `fill: false`이지만 등록하지 않으면 경고 발생.

### Chart.js react-chartjs-2 버전 확인

- `chart.js`: 4.x (이미 `package.json`에 설치됨 — Story 3.2 때 추가)
- `react-chartjs-2`: 5.x (이미 설치됨)
- **추가 npm install 불필요**

### 코딩 컨벤션 (엄수)

- **Named export**: `export function PlayerChartsSection()` (default 금지)
- **`import type`**: `import type { ChartOptions }`, `import type { ... }` — 타입 전용 import
- **lazy named export 패턴**: `.then(m => ({ default: m.PlayerChartsSection }))` 필수
- **CSS 변수**: `var(--color-text-secondary)`, `var(--font-size-sm)` — 하드코딩 금지
- **Canvas 높이**: `height: '220px'` (문자열), `maintainAspectRatio: false`

### 파일 위치 규칙

```
frontend/src/
  components/
    dashboard/
      ChampionChartsSection.tsx   ← 기존 (Story 3.2)
      PlayerChartsSection.tsx     ← 신규 (Task 1)
  pages/
    PlayerStatsPage.tsx           ← 수정 (Task 2)
```

### References

- `EloHistoryEntry` 타입: `frontend/src/lib/types/stats.ts` (lines 98-105)
- `PlayerEloHistoryResult` 타입: `frontend/src/lib/types/stats.ts` (lines 107-112)
- `ChampionStat` 타입: `frontend/src/lib/types/stats.ts` (lines 26-39)
- `ChampionChartsSection.tsx`: `frontend/src/components/dashboard/ChampionChartsSection.tsx` — 참조용 패턴
- `ChartSkeleton.tsx`: `frontend/src/components/common/ChartSkeleton.tsx`
- `PlayerStatsPage.tsx`: `frontend/src/pages/PlayerStatsPage.tsx`

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

- `PlayerChartsSection.tsx` 신규 생성 — Elo 라인 차트(segment 콜백, ScriptableLineSegment 타입) + 챔피언 바 차트(상위 8개, 승률 색상)
- `ChartJS.register()` 모듈 최상단에서 호출 (Filler 포함)
- `PlayerStatsPage.tsx` — `lazy`, `Suspense`, `Component` import 추가, `ChartSkeleton` import 추가
- `PlayerChartsSection` lazy 동적 임포트 (named export `.then(m => ({ default: m.PlayerChartsSection }))` 패턴)
- `ChartErrorBoundary` 클래스 신규 정의 (파일 최상단 레벨)
- 배치: ChampionTable 아래, recentMatches 위, `key={riotId-mode}`로 모드 전환 시 리마운트
- TypeScript 오류 없음 (`npx tsc --noEmit` 통과)

### File List

- frontend/src/components/dashboard/PlayerChartsSection.tsx (신규)
- frontend/src/pages/PlayerStatsPage.tsx (수정)
