# Story 3.2: 챔피언 상세 Chart.js 시각화

Status: done

## Story

As a 유저,
I want 챔피언 상세 페이지에서 포지션별 승률과 KDA 차트를 확인하기를,
So that 해당 챔피언의 포지션 메타와 퍼포먼스 트렌드를 시각적으로 파악할 수 있다. (FR11)

## Acceptance Criteria

1. **Given** 유저가 챔피언 상세 페이지에 진입했을 때
   **When** Chart.js 차트가 `React.lazy` 동적 로딩으로 불러와지면
   **Then** 포지션별 승률 바 차트가 렌더링된다 (NFR4: 2초 이내)

2. **Given** Chart.js가 동적 임포트로 적용되었을 때
   **When** 홈/목록 페이지 번들을 확인하면
   **Then** Chart.js가 포함되지 않는다 (NFR5)

3. **Given** Chart.js 차트 컴포넌트가 로딩 중일 때
   **When** `React.Suspense` fallback이 표시되면
   **Then** `ChartSkeleton` 컴포넌트가 표시되어 레이아웃 시프트가 없다

4. **Given** 차트가 렌더링되었을 때
   **When** 데이터 포인트(바 또는 라인)에 마우스를 올리면
   **Then** 해당 포지션의 수치(승률%, 판수 등)가 툴팁으로 표시된다

5. **Given** laneStats 데이터가 없을 때
   **When** 차트가 표시될 자리를 보면
   **Then** 차트 섹션 자체가 숨겨진다 (`laneStats.length === 0` 조건부 렌더링)

## Tasks / Subtasks

- [x] Task 1: Chart.js 패키지 설치 (AC: #2)
  - [x] `cd frontend && npm install chart.js react-chartjs-2`
  - [x] `npx tsc --noEmit` 오류 없음 확인 (설치 후)

- [x] Task 2: `ChartSkeleton.tsx` 신규 생성 (AC: #3)
  - [x] `frontend/src/components/common/ChartSkeleton.tsx` 신규 생성
  - [x] 차트 영역 크기와 동일한 Skeleton 블록 (height: 220px)
  - [x] `<Skeleton className="h-[220px] w-full rounded-lg" />` 구조

- [x] Task 3: `ChampionChartsSection.tsx` 신규 생성 (AC: #1, #4, #5)
  - [x] `frontend/src/components/dashboard/ChampionChartsSection.tsx` 신규 생성
  - [x] Chart.js 등록: `Chart.register(...)` — 아래 Dev Notes 참고
  - [x] **차트 1**: 포지션별 승률 바 차트 (`laneStats` → position/winRate)
  - [x] **차트 2**: 포지션별 KDA 바 차트 (`laneStats` → position/kda)
  - [x] 두 차트 모두 툴팁 활성화 (Chart.js 기본 tooltip 사용)
  - [x] laneStats 없으면 `null` 반환 (AC5)
  - [x] `key={champion}` prop으로 모드 변경 시 차트 리셋
  - [x] props: `champion: string`, `laneStats: ChampionLaneStat[]`

- [x] Task 4: `ChampionStatsPage.tsx` 수정 — React.lazy + Suspense 적용 (AC: #1, #2, #3)
  - [x] `React.lazy`로 `ChampionChartsSection` 동적 임포트:
    ```ts
    const ChampionChartsSection = React.lazy(
      () => import('@/components/dashboard/ChampionChartsSection')
        .then(m => ({ default: m.ChampionChartsSection }))
    );
    ```
  - [x] `import { ChartSkeleton } from '@/components/common/ChartSkeleton'` 추가
  - [x] 기존 `ChampionLaneStats` 섹션 아래에 Suspense + 차트 섹션 배치:
    ```tsx
    {data && data.laneStats?.length > 0 && (
      <Suspense fallback={<ChartSkeleton />}>
        <ChampionChartsSection
          key={`${champion}-${mode}`}
          champion={champion ?? ''}
          laneStats={data.laneStats}
        />
      </Suspense>
    )}
    ```
  - [x] `import React, { ..., Suspense } from 'react'` — Suspense 추가

- [x] Task 5: TypeScript 검증
  - [x] `npx tsc --noEmit` 오류 없이 통과 확인

## Dev Notes

### 🚨 CRITICAL: NFR5 — Chart.js 동적 로딩 필수

Chart.js는 번들 크기가 크므로(~200KB) **반드시 `React.lazy` + 동적 import**로 로드해야 한다.
절대 `ChampionStatsPage.tsx` 최상단에서 직접 `import { Bar } from 'react-chartjs-2'` 하지 말 것.
홈 페이지(`/`), 목록 페이지(`/champions`) 번들에 Chart.js가 포함되어서는 안 된다.

### Task 1 상세: 패키지 설치

```bash
cd frontend
npm install chart.js react-chartjs-2
```

- `chart.js`: 4.x (현재 최신 안정 버전)
- `react-chartjs-2`: 5.x (Chart.js 4 지원)

### Task 2 상세: ChartSkeleton.tsx 전체 구조

```tsx
// frontend/src/components/common/ChartSkeleton.tsx
import { Skeleton } from '@/components/common/Skeleton';

export function ChartSkeleton() {
  return (
    <div style={{ marginBottom: 16 }}>
      <Skeleton className="h-[220px] w-full rounded-lg" />
    </div>
  );
}
```

### Task 3 상세: ChampionChartsSection.tsx 전체 구조

```tsx
// frontend/src/components/dashboard/ChampionChartsSection.tsx
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  PointElement,
  LineElement,
  Tooltip,
  Legend,
} from 'chart.js';
import { Bar } from 'react-chartjs-2';
import type { ChampionLaneStat } from '@/lib/types/stats';

ChartJS.register(
  CategoryScale,
  LinearScale,
  BarElement,
  PointElement,
  LineElement,
  Tooltip,
  Legend,
);

const LANE_LABELS: Record<string, string> = {
  TOP:     '탑',
  JUNGLE:  '정글',
  MID:     '미드',
  BOTTOM:  '원딜',
  SUPPORT: '서폿',
};

interface Props {
  champion: string;
  laneStats: ChampionLaneStat[];
}

const CHART_OPTIONS = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: false },
    tooltip: { enabled: true },
  },
  scales: {
    y: { beginAtZero: true },
  },
} as const;

export function ChampionChartsSection({ laneStats }: Props) {
  if (laneStats.length === 0) return null;

  const labels = laneStats.map(l => LANE_LABELS[l.position] ?? l.position);

  const winRateData = {
    labels,
    datasets: [{
      label: '승률 (%)',
      data: laneStats.map(l => l.winRate),
      backgroundColor: laneStats.map(l =>
        l.winRate >= 60 ? 'rgba(34,197,94,0.7)'
          : l.winRate >= 50 ? 'rgba(99,102,241,0.7)'
          : 'rgba(239,68,68,0.7)'
      ),
      borderRadius: 4,
    }],
  };

  const kdaData = {
    labels,
    datasets: [{
      label: 'KDA',
      data: laneStats.map(l => parseFloat(l.kda.toFixed(2))),
      backgroundColor: 'rgba(99,102,241,0.6)',
      borderRadius: 4,
    }],
  };

  return (
    <div className="card" style={{ marginBottom: 16 }}>
      <div style={{ fontWeight: 700, fontSize: 'var(--font-size-sm)', marginBottom: 16 }}>
        포지션별 통계 차트
      </div>
      <div style={{ display: 'flex', gap: 24, flexWrap: 'wrap' }}>
        <div style={{ flex: '1 1 240px', height: 220 }}>
          <div style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginBottom: 8 }}>승률</div>
          <Bar data={winRateData} options={CHART_OPTIONS} />
        </div>
        <div style={{ flex: '1 1 240px', height: 220 }}>
          <div style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginBottom: 8 }}>KDA</div>
          <Bar data={kdaData} options={CHART_OPTIONS} />
        </div>
      </div>
    </div>
  );
}
```

### Task 4 상세: ChampionStatsPage 수정 포인트

**Suspense import 추가:**
```tsx
import React, { useEffect, useState, useCallback, useMemo, Suspense } from 'react';
// 또는 별도로:
import { Suspense } from 'react';
```

**lazy 동적 임포트 (컴포넌트 외부, 파일 최상단 레벨):**
```tsx
const ChampionChartsSection = React.lazy(
  () => import('../components/dashboard/ChampionChartsSection')
    .then(m => ({ default: m.ChampionChartsSection }))
);
```

**기존 ChampionLaneStats 섹션 바로 아래 추가:**
```tsx
{/* 기존 코드 */}
{data && data.laneStats?.length > 0 && (
  <ChampionLaneStats laneStats={data.laneStats} />
)}

{/* Story 3.2: Chart 추가 */}
{data && data.laneStats?.length > 0 && (
  <Suspense fallback={<ChartSkeleton />}>
    <ChampionChartsSection
      key={`${champion}-${mode}`}
      champion={champion ?? ''}
      laneStats={data.laneStats}
    />
  </Suspense>
)}
```

**`key={`${champion}-${mode}`}` 사유**: 모드 전환 시 차트 컴포넌트가 완전히 리마운트되어 이전 Chart.js 인스턴스가 정리된다. Chart.js는 canvas를 직접 제어하므로 리마운트 없이 데이터만 교체하면 canvas 상태가 오염될 수 있다.

### Chart.js 등록 관련 주의사항

`ChampionChartsSection.tsx` 내부에서 `ChartJS.register(...)` 호출.
이 파일이 lazy-loaded이므로 Chart.js 등록도 lazy하게 일어난다 — NFR5 준수.

단, `ChartJS.register()`는 전역 상태를 변경하므로 **컴포넌트 함수 바깥, 파일 최상단 레벨**에서 호출한다.

필수 등록 요소 (Bar 차트용):
- `CategoryScale` — X축 카테고리 스케일
- `LinearScale` — Y축 선형 스케일
- `BarElement` — 바 요소
- `Tooltip` — 툴팁 플러그인

### CHART_OPTIONS 타입 처리

`as const` assertion을 사용하여 TypeScript가 options를 읽기 전용 리터럴로 추론.
`responsive: true` + `maintainAspectRatio: false` + 부모 div에 `height` 지정 필요.

### 코딩 컨벤션 (엄수)

- **Named export**: `export function ChampionChartsSection()`, `export function ChartSkeleton()` (default 금지)
- **`import type`**: `import type { ChampionLaneStat }` — Chart.js 타입 포함
- **lazy named export 패턴**: `.then(m => ({ default: m.ChampionChartsSection }))` 패턴 필수
  - React.lazy는 default export만 지원하므로, named export를 가진 컴포넌트는 위 패턴으로 래핑
- **CSS 변수**: 신규 색상 대신 Chart.js rgba()로 직접 색상 지정 (canvas에 CSS 변수 적용 불가)
- **Chart canvas 높이**: 부모 `div`에 `height: 220px` 지정 후 `maintainAspectRatio: false` 옵션 필수

### 파일 위치 규칙

```
frontend/src/
  components/
    common/
      ChartSkeleton.tsx            ← 신규 (Task 2)
    dashboard/
      ChampionChartsSection.tsx    ← 신규 (Task 3)
  pages/
    ChampionStatsPage.tsx          ← 수정 (Task 4)
```

### Story 3.1 코드 리뷰 학습 사항 (B1 bad_spec)

- 픽률 카드(`useChampions()`)는 모드 비의존 전체 통계를 표시 — 이번 스토리에서 차트는 `data.laneStats` (모드별) 기반이므로 모드 전환 시 자동 업데이트됨. `key` prop으로 명시적 리마운트 보장.

### References

- `ChampionLaneStat` 타입: `frontend/src/lib/types/stats.ts` (lines 147-159)
- `ChampionStatsPage`: `frontend/src/pages/ChampionStatsPage.tsx`
- `Skeleton` 컴포넌트: `frontend/src/components/common/Skeleton.tsx`
- react-chartjs-2 문서: https://react-chartjs-2.js.org/
- Chart.js 4 등록 패턴: `Chart.register(CategoryScale, LinearScale, BarElement, Tooltip)`

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

- `chart.js@4.x` + `react-chartjs-2@5.x` 설치 — TypeScript 오류 없음
- `ChartSkeleton` 신규 생성 — 220px 고정 높이 Skeleton (레이아웃 시프트 방지, AC3)
- `ChampionChartsSection` 신규 생성 — `ChartJS.register()` 파일 최상단 레벨, `Bar` 차트 2개(승률/KDA)
- Named export + `lazy().then(m => ({ default: m.ChampionChartsSection }))` 패턴으로 NFR5 준수
- `ChampionStatsPage` 수정: `lazy`, `Suspense` import 추가 + `ChartSkeleton` + lazy ChampionChartsSection 삽입
- `key={champion-mode}` prop으로 모드 전환 시 Chart.js canvas 오염 방지
- TypeScript 오류 없음 (`npx tsc --noEmit` 통과)

### File List

- frontend/src/components/common/ChartSkeleton.tsx (신규)
- frontend/src/components/dashboard/ChampionChartsSection.tsx (신규)
- frontend/src/pages/ChampionStatsPage.tsx (수정)
- frontend/package.json (수정 — chart.js, react-chartjs-2 추가)
