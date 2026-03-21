# Story 3.1: 챔피언 목록 페이지 & 상세 수치 카드

Status: done

## Story

As a 유저,
I want 챔피언 목록 페이지에서 챔피언을 클릭해 상세 수치를 확인하기를,
So that 특정 챔피언의 승률/픽률/KDA를 빠르게 파악할 수 있다. (FR5)

## Acceptance Criteria

1. **Given** 사이드바에서 "챔피언 목록" 클릭 시
   **When** `/champions` 페이지가 로드되면
   **Then** 픽률(picks/matchCount) 내림차순으로 챔피언 목록이 표시된다

2. **Given** 챔피언 목록이 표시될 때
   **When** 각 행을 확인하면
   **Then** 챔피언 아이콘(32×32), 한국어명, 승률(%), 픽률(%), 판수가 표시되고 수치는 `font-mono`로 정렬된다

3. **Given** 챔피언 목록에서 행 클릭 시
   **When** 챔피언 행을 클릭하면
   **Then** 1-click으로 `/stats/champion/:champion` 상세 페이지로 이동한다 (ChampionLink 활용)

4. **Given** 챔피언 상세 페이지(`/stats/champion/:champion`)가 로드될 때
   **When** 데이터 로드 완료 후
   **Then** 페이지 상단(기존 헤더 아래, 인기 아이템 위)에 수치 카드 4개가 표시된다:
   - 카드1: 승률 (`data.winRate`%) — 60% 이상: `var(--color-win)`, 50% 미만: `var(--color-loss)`, 그 외: primary
   - 카드2: 총 판수 (`data.totalGames`판)
   - 카드3: 전체 KDA — laneStats 기반 가중 평균 (`sum(lane.kda * lane.games) / sum(lane.games)`) — laneStats 없으면 "—"
   - 카드4: 픽률 — `useChampions()` 캐시에서 해당 챔피언 조회 (`topPickedChampions.find(c => c.champion === champion)`) → `(picks / matchCount * 100).toFixed(1)`% — 없으면 "—"

5. **Given** 챔피언 상세 페이지 로딩 중
   **When** 데이터 로드 전
   **Then** 수치 카드 영역에 Skeleton 카드 4개가 표시되어 레이아웃 시프트가 없다

6. **Given** 수치 카드의 숫자를 확인할 때
   **When** 표시된 수치를 보면
   **Then** 승률·픽률은 소수점 1자리, KDA는 소수점 2자리로 `font-mono` 클래스 적용

## Tasks / Subtasks

- [x] Task 1: `ChampionListPage.tsx` 신규 생성 (AC: #1, #2, #3)
  - [x] `frontend/src/pages/ChampionListPage.tsx` 신규 생성
  - [x] `useChampions()` 훅으로 데이터 페치 (`OverviewStats.topPickedChampions`)
  - [x] 클라이언트 정렬: `[...data.topPickedChampions].sort((a, b) => b.picks - a.picks)` (AC1 — 서버 순서 의존 금지)
  - [x] 픽률 계산: `(entry.picks / data.matchCount * 100).toFixed(1)` (matchCount > 0 guard)
  - [x] 로딩 상태: Skeleton (Table 구조 일치 — 아래 JSX 참고)
  - [x] 에러 상태: `InlineError` + `refetch` callback
  - [x] 데이터 없음 상태: "챔피언 데이터가 없습니다." 텍스트
  - [x] 각 행: `ChampionLink champion={entry.champion} championId={entry.championId}`
  - [x] `key={entry.championId}` (숫자 ID 사용, 문자열 금지)
  - [x] 챔피언 이미지: `useDragon()` → `dragonChampions.get(entry.championId)?.imageUrl`
  - [x] 챔피언명: `dragon?.nameKo ?? entry.champion`

- [x] Task 2: `App.tsx` 라우트 추가 (AC: #1)
  - [x] `import { ChampionListPage } from './pages/ChampionListPage';` 추가
  - [x] Desktop routes에 `<Route path="champions" element={<ChampionListPage />} />` 추가

- [x] Task 3: `Sidebar.tsx` 네비게이션 항목 추가 (AC: #1)
  - [x] `Trophy` 아이콘 import 추가 (`lucide-react`)
  - [x] NAV_ITEMS에 `{ to: '/champions', icon: Trophy, label: '챔피언 목록' }` 추가 (멤버 통계 아래)

- [x] Task 4: `ChampionStatsPage.tsx` 수치 카드 추가 (AC: #4, #6)
  - [x] `useChampions` import 추가 (`@/hooks/useChampions`)
  - [x] `const { data: overviewData } = useChampions();` 추가 (캐시 공유 — 추가 API 호출 없음)
  - [x] 전체 KDA 계산 (useMemo):
    ```ts
    const overallKda = useMemo(() => {
      if (!data || data.laneStats.length === 0) return null;
      const totalGamesLane = data.laneStats.reduce((s, l) => s + l.games, 0);
      if (totalGamesLane === 0) return null;
      return data.laneStats.reduce((s, l) => s + l.kda * l.games, 0) / totalGamesLane;
    }, [data]);
    ```
  - [x] pickRate 계산:
    ```ts
    const pickRate = useMemo(() => {
      if (!overviewData || !champion) return null;
      const champStat = overviewData.topPickedChampions.find(c => c.champion === champion);
      if (!champStat || overviewData.matchCount === 0) return null;
      return (champStat.picks / overviewData.matchCount * 100).toFixed(1);
    }, [overviewData, champion]);
    ```
  - [x] 수치 카드 섹션을 기존 헤더(`page-header`) 아래, `{loading ? <LoadingCenter /> : (...)}` 블록 내부 최상단에 삽입
  - [x] 로딩 중(`loading === true`) → Skeleton 카드 4개 (AC5)
  - [x] 로딩 완료 → 실제 수치 카드 4개

- [x] Task 5: TypeScript 검증
  - [x] `npx tsc --noEmit` 오류 없이 통과 확인

## Dev Notes

### 🚨 라우트 결정 — 기존 `/stats/champion/:champion` 유지

Epic 스펙에는 `/champions/:id` 라우트가 명시되어 있으나, 코드베이스 분석 결과:
- `ChampionLink.tsx`가 이미 `/stats/champion/${encodeURIComponent(champion)}`으로 라우팅
- `ChampionStatsPage`가 `/stats/champion/:champion`에서 완전 구현됨
- `ChampionTierTable`, `BanTrendCard` 등 모든 기존 컴포넌트가 ChampionLink 사용

따라서 **기존 라우트를 유지**하고 새 `/champions` 목록 페이지만 추가한다. 상세 페이지는 기존 ChampionStatsPage를 강화한다.

### Task 1 상세: ChampionListPage.tsx 전체 구조

```tsx
// frontend/src/pages/ChampionListPage.tsx
import { useMemo } from 'react';
import { useChampions } from '@/hooks/useChampions';
import { InlineError } from '@/components/common/InlineError';
import { Skeleton } from '@/components/common/Skeleton';
import { ChampionLink } from '@/components/common/ChampionLink';
import { useDragon } from '@/context/DragonContext';
import type { ChampionPickStat } from '@/lib/types/stats';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';

export function ChampionListPage() {
  const { data, isLoading, error, refetch } = useChampions();
  const { champions: dragonChampions } = useDragon();

  const sorted = useMemo(() => {
    if (!data) return [];
    return [...data.topPickedChampions].sort((a, b) => b.picks - a.picks);
  }, [data]);

  if (isLoading) {
    return (
      <div>
        <h1 className="page-title" style={{ marginBottom: 'var(--spacing-md)' }}>챔피언 목록</h1>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>챔피언</TableHead>
              <TableHead className="text-right font-mono">승률</TableHead>
              <TableHead className="text-right font-mono">픽률</TableHead>
              <TableHead className="text-right font-mono">판수</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {Array.from({ length: 8 }).map((_, i) => (
              <TableRow key={i}>
                <TableCell>
                  <div className="flex items-center gap-2">
                    <Skeleton className="h-8 w-8 rounded" />
                    <Skeleton className="h-4 w-24" />
                  </div>
                </TableCell>
                <TableCell className="text-right"><Skeleton className="h-4 w-12 ml-auto" /></TableCell>
                <TableCell className="text-right"><Skeleton className="h-4 w-12 ml-auto" /></TableCell>
                <TableCell className="text-right"><Skeleton className="h-4 w-8 ml-auto" /></TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>
    );
  }

  if (error) {
    return <InlineError message="챔피언 목록을 불러오지 못했습니다." onRetry={() => void refetch()} />;
  }

  if (!data || sorted.length === 0) {
    return (
      <div>
        <h1 className="page-title" style={{ marginBottom: 'var(--spacing-md)' }}>챔피언 목록</h1>
        <p className="text-sm p-4" style={{ color: 'var(--color-text-secondary)' }}>
          챔피언 데이터가 없습니다.
        </p>
      </div>
    );
  }

  return (
    <div>
      <h1 className="page-title" style={{ marginBottom: 'var(--spacing-md)' }}>챔피언 목록</h1>
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>챔피언</TableHead>
            <TableHead className="text-right font-mono">승률</TableHead>
            <TableHead className="text-right font-mono">픽률</TableHead>
            <TableHead className="text-right font-mono">판수</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {sorted.map((entry: ChampionPickStat) => {
            const dragon = dragonChampions.get(entry.championId);
            const displayName = dragon?.nameKo ?? entry.champion;
            const imgUrl = dragon?.imageUrl ?? null;
            const pickRate = data.matchCount > 0
              ? ((entry.picks / data.matchCount) * 100).toFixed(1)
              : '0.0';
            const wrColor = entry.winRate >= 50
              ? 'var(--color-win)'
              : 'var(--color-loss)';

            return (
              <TableRow key={entry.championId}>
                <TableCell>
                  <ChampionLink champion={entry.champion} championId={entry.championId}>
                    <div className="flex items-center gap-2">
                      {imgUrl ? (
                        <img
                          src={imgUrl}
                          alt={displayName}
                          width={32}
                          height={32}
                          style={{
                            borderRadius: 4,
                            border: '1px solid var(--color-border)',
                            objectFit: 'cover',
                          }}
                          onError={e => {
                            (e.currentTarget as HTMLImageElement).style.display = 'none';
                          }}
                        />
                      ) : (
                        <div style={{
                          width: 32, height: 32,
                          background: 'var(--color-bg-hover)',
                          borderRadius: 4, display: 'flex',
                          alignItems: 'center', justifyContent: 'center',
                          fontSize: 10, color: 'var(--color-text-secondary)',
                        }}>
                          {displayName.slice(0, 2)}
                        </div>
                      )}
                      <span style={{ fontWeight: 600, fontSize: 13 }}>
                        {displayName}
                      </span>
                    </div>
                  </ChampionLink>
                </TableCell>
                <TableCell
                  className="text-right font-mono"
                  style={{ fontWeight: 700, color: wrColor }}
                >
                  {entry.winRate.toFixed(1)}%
                </TableCell>
                <TableCell
                  className="text-right font-mono"
                  style={{ color: 'var(--color-text-secondary)' }}
                >
                  {pickRate}%
                </TableCell>
                <TableCell
                  className="text-right font-mono"
                  style={{ color: 'var(--color-text-secondary)' }}
                >
                  {entry.picks}
                </TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    </div>
  );
}
```

### Task 4 상세: 수치 카드 JSX

수치 카드는 `ChampionStatsPage`의 기존 `loading ? <LoadingCenter /> : (...)` 블록 안, 인기 아이템 섹션 직전에 삽입:

```tsx
// 수치 카드 Skeleton (loading === true일 때 표시)
<div style={{ display: 'flex', gap: 12, marginBottom: 16, flexWrap: 'wrap' }}>
  {Array.from({ length: 4 }).map((_, i) => (
    <div key={i} className="card" style={{ minWidth: 110, flex: '1 1 110px', padding: '12px 16px' }}>
      <Skeleton className="h-7 w-16 mb-1" />
      <Skeleton className="h-3 w-10" />
    </div>
  ))}
</div>

// 수치 카드 (데이터 로드 완료 후)
const wrCardColor = data.winRate >= 60
  ? 'var(--color-win)'
  : data.winRate < 50
    ? 'var(--color-loss)'
    : 'var(--color-primary)';

<div style={{ display: 'flex', gap: 12, marginBottom: 16, flexWrap: 'wrap' }}>
  {/* 승률 */}
  <div className="card" style={{ minWidth: 110, flex: '1 1 110px', padding: '12px 16px' }}>
    <div className="font-mono" style={{ fontSize: 22, fontWeight: 800, color: wrCardColor }}>
      {data.winRate.toFixed(1)}%
    </div>
    <div style={{ fontSize: 11, color: 'var(--color-text-secondary)', marginTop: 2 }}>승률</div>
  </div>

  {/* 총 판수 */}
  <div className="card" style={{ minWidth: 110, flex: '1 1 110px', padding: '12px 16px' }}>
    <div className="font-mono" style={{ fontSize: 22, fontWeight: 800 }}>
      {data.totalGames}
    </div>
    <div style={{ fontSize: 11, color: 'var(--color-text-secondary)', marginTop: 2 }}>총 판수</div>
  </div>

  {/* 전체 KDA */}
  <div className="card" style={{ minWidth: 110, flex: '1 1 110px', padding: '12px 16px' }}>
    <div className="font-mono" style={{ fontSize: 22, fontWeight: 800 }}>
      {overallKda !== null ? overallKda.toFixed(2) : '—'}
    </div>
    <div style={{ fontSize: 11, color: 'var(--color-text-secondary)', marginTop: 2 }}>전체 KDA</div>
  </div>

  {/* 픽률 */}
  <div className="card" style={{ minWidth: 110, flex: '1 1 110px', padding: '12px 16px' }}>
    <div className="font-mono" style={{ fontSize: 22, fontWeight: 800 }}>
      {pickRate !== null ? `${pickRate}%` : '—'}
    </div>
    <div style={{ fontSize: 11, color: 'var(--color-text-secondary)', marginTop: 2 }}>픽률</div>
  </div>
</div>
```

**Skeleton import 추가** (`ChampionStatsPage`에 아직 없음):
```tsx
import { Skeleton } from '@/components/common/Skeleton';
```

### 데이터 훅 선택

- `useChampions()` 재사용: `queryKey: ['champions']`, `ChampionTierTable` / `BanTrendCard`와 캐시 공유
  - ChampionListPage: 주요 데이터 소스 (topPickedChampions)
  - ChampionStatsPage: pickRate 계산용 (추가 API 호출 없음)
- ChampionStatsPage 기존 방식: `api.get<ChampionDetailStats>(...)` 유지 (직접 fetch)

### 코딩 컨벤션 (엄수)

- **Named export**: `export function ChampionListPage()` (default 금지)
- **`import type`**: 타입 전용은 반드시 `import type`
- **`key={entry.championId}`**: 안정적 숫자 키 (문자열 금지)
- **Skeleton Table 구조 일치**: Skeleton도 `<Table>` 안에 배치 (Story 2.2 P3, 2.3 P2 교훈)
- **클라이언트 정렬**: AC에 정렬 기준 명시 시 반드시 클라이언트 정렬 구현 (Story 2.4 P1 교훈)
- **CSS 변수**: 신규 변수 사용 전 `global.css` `:root`에서 존재 확인 (Epic 2 A2 액션)
  - 사용 가능: `--color-win`, `--color-loss`, `--color-primary`, `--color-text-secondary`, `--color-border`, `--color-bg-hover`

### ChampionLink 인터페이스

```tsx
// props: champion(string), championId(number), className?(string)
<ChampionLink champion={entry.champion} championId={entry.championId}>
  {children}
</ChampionLink>
// → /stats/champion/{encodeURIComponent(champion)} 로 이동 (기존 경로 유지)
```

### 파일 위치 규칙

```
frontend/src/
  pages/
    ChampionListPage.tsx         ← 신규 (Task 1)
    ChampionStatsPage.tsx        ← 수정 (Task 4: 수치 카드 추가)
  components/
    layout/
      Sidebar.tsx                ← 수정 (Task 3: 챔피언 목록 nav 추가)
  App.tsx                        ← 수정 (Task 2: /champions 라우트)
```

### References

- `useChampions`: `frontend/src/hooks/useChampions.ts`
- `ChampionPickStat` / `OverviewStats` / `ChampionDetailStats` / `ChampionLaneStat`: `frontend/src/lib/types/stats.ts`
- `ChampionLink`: `frontend/src/components/common/ChampionLink.tsx`
- shadcn Table: `frontend/src/components/ui/table.tsx`
- DragonContext 패턴: `frontend/src/context/DragonContext.tsx`
- 기존 ChampionStatsPage: `frontend/src/pages/ChampionStatsPage.tsx`
- ChampionTierTable (Skeleton 패턴 참고): `frontend/src/components/dashboard/ChampionTierTable.tsx`
- Sidebar nav: `frontend/src/components/layout/Sidebar.tsx`
- App.tsx 라우트: `frontend/src/App.tsx`

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

- `ChampionListPage` 신규 생성 — `useChampions()` 캐시 공유, 클라이언트 정렬(`b.picks - a.picks`), Skeleton Table 구조 일치
- `App.tsx` `/champions` 라우트 추가 — Desktop routes에 삽입
- `Sidebar.tsx` `Trophy` 아이콘 + `챔피언 목록` nav item 추가 (멤버 통계 아래)
- `ChampionStatsPage` 수치 카드 4개 추가: 승률(색상 분기), 총 판수, 전체 KDA(laneStats 가중 평균), 픽률(useChampions 캐시)
- 수치 카드 Skeleton: 로딩 중 레이아웃 시프트 없음 (AC5 충족)
- `useMemo` 적용: overallKda, pickRate 계산 최적화
- TypeScript 오류 없음 (`npx tsc --noEmit` 통과)

### File List

- frontend/src/pages/ChampionListPage.tsx (신규)
- frontend/src/App.tsx (수정)
- frontend/src/components/layout/Sidebar.tsx (수정)
- frontend/src/pages/ChampionStatsPage.tsx (수정)
