# Story 2.3: ChampionTierTable 컴포넌트 & 홈 배치

Status: done

## Story

As a 유저,
I want 홈 화면에서 내전 기반 챔피언 티어표(S/A/B/C)를 즉시 확인하기를,
So that 이번 리그에서 강한 챔피언이 무엇인지 OP.GG처럼 직관적으로 파악할 수 있다. (FR2)

## Acceptance Criteria

1. **Given** 유저가 홈에 접속했을 때
   **When** ChampionTierTable이 로드되면
   **Then** S/A/B/C 티어 섹션으로 분류된 챔피언 목록이 표시된다

2. **Given** 챔피언 티어표가 표시될 때
   **When** 각 챔피언 행을 확인하면
   **Then** 챔피언 아이콘, 이름, 승률, 픽률, 게임 수가 표시되고 수치는 `font-mono`로 정렬된다

3. **Given** 티어 섹션 헤더를 클릭했을 때
   **When** 접기/펼치기를 누르면
   **Then** 해당 티어의 챔피언 목록이 접히거나 펼쳐진다

4. **Given** 승률 50% 이상인 챔피언을 확인할 때
   **When** 승률 수치를 보면
   **Then** `var(--color-win)` (`#10B981`, 초록) 색상이 적용된다
   **And** 50% 미만이면 `var(--color-loss)` (`#EF4444`, 빨강) 색상이 적용된다

5. **Given** S티어 뱃지를 확인할 때
   **When** 티어 색상을 보면
   **Then** `TIER_COLORS` 상수에 정의된 색상(S: `#FFD700`)이 적용된다

## Tasks / Subtasks

- [ ] Task 1: `ChampionTierTable.tsx` 컴포넌트 구현 (AC: #1, #2, #3, #4, #5)
  - [ ] `frontend/src/components/dashboard/ChampionTierTable.tsx` 신규 생성
  - [ ] `useChampions()` 훅으로 데이터 페치
  - [ ] 로딩 상태: Table 구조 Skeleton (EloLeaderboard 패턴 동일)
  - [ ] 에러 상태: `InlineError` + `refetch` callback
  - [ ] `getChampionTier(winRate)` 내부 함수: 60+→S, 55+→A, 50+→B, 미만→C
  - [ ] `useMemo`로 tier별 챔피언 그룹핑
  - [ ] `useState<Record<TierKey, boolean>>`로 각 티어 접기/펼치기 상태 관리 (기본: 모두 펼침)
  - [ ] `TIER_COLORS` 임포트 (`@/lib/constants/theme`)
  - [ ] shadcn `Badge` (outline variant + inline style)로 티어 뱃지 렌더링
  - [ ] `useDragon()` 훅으로 챔피언 아이콘(imageUrl) + 한국어 이름(nameKo) 표시
  - [ ] `ChampionLink`으로 챔피언 행 클릭 가능
  - [ ] 승률 색상: ≥50% → `var(--color-win)`, <50% → `var(--color-loss)`
  - [ ] pickRate 계산: `(picks / data.matchCount) * 100` (소수점 1자리)
  - [ ] 수치(승률/픽률/게임수)에 `font-mono` 클래스 적용

- [ ] Task 2: `HomePage.tsx` 업데이트 — ChampionTierTable 배치 (AC: #1)
  - [ ] `{/* Story 2.3: ChampionTierTable 추가 예정 */}` 주석 제거
  - [ ] `ChampionTierTable` import 및 섹션 추가

- [ ] Task 3: TypeScript 검증
  - [ ] `npx tsc --noEmit` 오류 없이 통과 확인

## Dev Notes

### 🚨 CRITICAL: 데이터 타입 분석 — 티어 필드 없음, winRate 기반 분류 필요

**`useChampions()` 반환 타입:**

```ts
// frontend/src/hooks/useChampions.ts
export function useChampions() {
  return useQuery({
    queryKey: ['champions'],
    queryFn: () => api.get<OverviewStats>('/stats/overview'),
  });
}
```

**`OverviewStats`에서 사용할 필드 (frontend/src/lib/types/stats.ts):**

```ts
interface ChampionPickStat {
  champion: string;       // 챔피언 이름 (영문, e.g. "Yasuo")
  championId: number;     // DragonContext 조회 키
  picks: number;          // 픽 횟수 (픽률 계산에 사용)
  wins: number;
  winRate: number;        // 승률 (%, e.g. 53.2)
}

interface OverviewStats {
  matchCount: number;           // 전체 경기 수 (픽률 계산 분모)
  topPickedChampions: ChampionPickStat[];  // ← 사용할 필드
  // topWinRateChampions, topBannedChampions 등 존재하지만 이 스토리에서 사용 안 함
}
```

**⚠️ 주의:** `topPickedChampions`는 픽 수 기준 상위 챔피언만 포함. 전체 챔피언 목록이 아님.

### Task 1 상세: ChampionTierTable.tsx 전체 구조

```tsx
// frontend/src/components/dashboard/ChampionTierTable.tsx

import { useMemo, useState } from 'react';
import { useChampions } from '@/hooks/useChampions';
import { InlineError } from '@/components/common/InlineError';
import { Skeleton } from '@/components/common/Skeleton';
import { ChampionLink } from '@/components/common/ChampionLink';
import { Badge } from '@/components/ui/badge';
import { useDragon } from '@/context/DragonContext';
import { TIER_COLORS } from '@/lib/constants/theme';
import type { TierKey } from '@/lib/constants/theme';
import type { ChampionPickStat } from '@/lib/types/stats';
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from '@/components/ui/table';

// ── 내부 유틸 ────────────────────────────────────────────────

const TIER_ORDER: TierKey[] = ['S', 'A', 'B', 'C'];

function getChampionTier(winRate: number): TierKey {
  if (winRate >= 60) return 'S';
  if (winRate >= 55) return 'A';
  if (winRate >= 50) return 'B';
  return 'C';
}

// ── 컴포넌트 ─────────────────────────────────────────────────

export function ChampionTierTable() {
  const { data, isLoading, error, refetch } = useChampions();
  const { champions: dragonChampions } = useDragon();

  // 각 티어 펼침 상태 (기본: 모두 펼침)
  const [expanded, setExpanded] = useState<Record<TierKey, boolean>>({
    S: true, A: true, B: true, C: true,
  });

  const tierGroups = useMemo(() => {
    if (!data) return [];
    return TIER_ORDER
      .map(tier => ({
        tier,
        champions: data.topPickedChampions.filter(
          (c: ChampionPickStat) => getChampionTier(c.winRate) === tier,
        ),
      }))
      .filter(g => g.champions.length > 0);
  }, [data]);

  if (isLoading) {
    return (
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead className="w-8">티어</TableHead>
            <TableHead>챔피언</TableHead>
            <TableHead className="text-right font-mono">승률</TableHead>
            <TableHead className="text-right font-mono">픽률</TableHead>
            <TableHead className="text-right font-mono">게임</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {Array.from({ length: 5 }).map((_, i) => (
            <TableRow key={i}>
              <TableCell><Skeleton className="h-5 w-5 rounded-full" /></TableCell>
              <TableCell>
                <div className="flex items-center gap-2">
                  <Skeleton className="h-8 w-8 rounded" />
                  <Skeleton className="h-4 w-20" />
                </div>
              </TableCell>
              <TableCell className="text-right"><Skeleton className="h-4 w-12 ml-auto" /></TableCell>
              <TableCell className="text-right"><Skeleton className="h-4 w-10 ml-auto" /></TableCell>
              <TableCell className="text-right"><Skeleton className="h-4 w-8 ml-auto" /></TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    );
  }

  if (error) {
    return (
      <InlineError
        message="챔피언 티어표를 불러오지 못했습니다."
        onRetry={() => void refetch()}
      />
    );
  }

  if (!data || data.topPickedChampions.length === 0) {
    return (
      <p className="text-sm p-4" style={{ color: 'var(--color-text-secondary)' }}>
        챔피언 데이터가 없습니다.
      </p>
    );
  }

  return (
    <div className="flex flex-col gap-4">
      {tierGroups.map(({ tier, champions }) => {
        const tierColor = TIER_COLORS[tier];
        const isExpanded = expanded[tier];

        return (
          <div key={tier}>
            {/* 티어 섹션 헤더 (클릭으로 접기/펼치기) */}
            <button
              type="button"
              className="flex items-center gap-2 w-full text-left py-2 px-1"
              style={{ background: 'none', border: 'none', cursor: 'pointer' }}
              onClick={() => setExpanded(prev => ({ ...prev, [tier]: !prev[tier] }))}
            >
              <Badge
                variant="outline"
                style={{ color: tierColor, borderColor: tierColor + '66', background: tierColor + '18' }}
              >
                {tier}
              </Badge>
              <span style={{ fontSize: 13, fontWeight: 600, color: tierColor }}>
                {tier} 티어
              </span>
              <span style={{ fontSize: 11, color: 'var(--color-text-secondary)', marginLeft: 4 }}>
                ({champions.length}명)
              </span>
              <span style={{ marginLeft: 'auto', color: 'var(--color-text-secondary)', fontSize: 12 }}>
                {isExpanded ? '▲' : '▼'}
              </span>
            </button>

            {/* 챔피언 테이블 */}
            {isExpanded && (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>챔피언</TableHead>
                    <TableHead className="text-right font-mono">승률</TableHead>
                    <TableHead className="text-right font-mono">픽률</TableHead>
                    <TableHead className="text-right font-mono">게임</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {champions.map((entry: ChampionPickStat) => {
                    const dragon = dragonChampions.get(entry.championId);
                    const displayName = dragon?.nameKo ?? entry.champion;
                    const imgUrl = dragon?.imageUrl ?? null;
                    const wrColor = entry.winRate >= 50
                      ? 'var(--color-win)'
                      : 'var(--color-loss)';
                    const pickRate = data.matchCount > 0
                      ? ((entry.picks / data.matchCount) * 100).toFixed(1)
                      : '0.0';

                    return (
                      <TableRow key={entry.champion}>
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
            )}
          </div>
        );
      })}
    </div>
  );
}
```

### Task 2 상세: HomePage.tsx 업데이트

```tsx
// 추가할 내용
import { ChampionTierTable } from '@/components/dashboard/ChampionTierTable';

// {/* Story 2.3: ChampionTierTable 추가 예정 */} 주석을 아래로 교체:
<section>
  <h2 className="page-title" style={{ marginBottom: 'var(--spacing-md)' }}>
    챔피언 티어표
  </h2>
  <ChampionTierTable />
</section>
```

### ChampionLink 인터페이스

```tsx
// frontend/src/components/common/ChampionLink.tsx
// props: champion(string), championId(number), className?(string)
<ChampionLink champion={entry.champion} championId={entry.championId}>
  {children}
</ChampionLink>
```

### DragonContext 사용 패턴

```tsx
const { champions: dragonChampions } = useDragon();
const dragon = dragonChampions.get(entry.championId);
// dragon?.imageUrl — 챔피언 이미지 URL
// dragon?.nameKo  — 한국어 이름 (없으면 entry.champion 폴백)
```

### TIER_COLORS & TierKey 임포트

```ts
// frontend/src/lib/constants/theme.ts (이미 존재)
export const TIER_COLORS = {
  S: '#FFD700',
  A: '#00B4D8',
  B: '#8899BB',
  C: '#4A5568',
} as const;
export type TierKey = keyof typeof TIER_COLORS;
```

**import 방법:**
```tsx
import { TIER_COLORS } from '@/lib/constants/theme';
import type { TierKey } from '@/lib/constants/theme';
```

### 티어 분류 기준

| 티어 | winRate 조건 | 색상 |
|------|-------------|------|
| S | ≥ 60% | #FFD700 (Gold) |
| A | ≥ 55% | #00B4D8 (Teal) |
| B | ≥ 50% | #8899BB (Blue Gray) |
| C | < 50% | #4A5568 (Muted) |

### 픽률 계산

```ts
// matchCount: 전체 경기 수
// picks: 해당 챔피언이 픽된 횟수 (양팀 합산)
// 픽률 = 전체 경기 중 등장 비율
const pickRate = data.matchCount > 0
  ? ((entry.picks / data.matchCount) * 100).toFixed(1)
  : '0.0';
```

### shadcn Badge 사용법

```tsx
import { Badge } from '@/components/ui/badge';

// outline variant + inline style로 티어 색상 적용
<Badge
  variant="outline"
  style={{
    color: tierColor,
    borderColor: tierColor + '66',
    background: tierColor + '18',
  }}
>
  {tier}
</Badge>
```

shadcn Badge는 `outline` variant를 지원하며 `text-foreground` 색상을 사용하지만, inline `style`로 override 가능.

### 코딩 컨벤션 (엄수)

- **Named export**: `export function ChampionTierTable()` (default 금지)
- **`import type`**: 타입 전용은 반드시 `import type`
- **신규 컴포넌트 스타일링**: Tailwind 우선 + CSS 변수(의미있는 색상)
- **Chart.js import 절대 금지**: 홈 페이지에 Chart.js 없음 (NFR5)
- **`useMemo`**: 티어 그룹핑은 렌더링 시 반복 계산 방지를 위해 `useMemo` 사용

### 🚨 Story 2.1~2.2 학습 사항

- **shadcn CSS 변수**: Story 2.1에서 매핑 완료 — `bg-muted`, `text-muted-foreground` 정상 동작
- **Skeleton loading: Table 구조 일치**: Story 2.2 리뷰에서 발견된 layout shift 방지 패턴 — Skeleton도 `<Table>` 안에 배치
- **`|| undefined` 패턴**: falsy 값(빈 문자열 포함) 제거 시 `?? undefined` 대신 `|| undefined`
- **`riotId.split('#')` 패턴**: destructure + 폴백 패턴 활용 (Story 2.2 P1)

### 파일 위치 규칙

```
frontend/src/
  components/
    dashboard/
      EloLeaderboard.tsx     ← 기존 (수정 없음)
      ChampionTierTable.tsx  ← 신규 (Task 1)
  pages/
    HomePage.tsx             ← 수정 (Task 2, 2.3 주석 → 실제 컴포넌트)
```

### 다음 스토리 연계

- **Story 2.4** (`BanTrendCard`): `HomePage.tsx`의 `{/* Story 2.4: BanTrendCard 추가 예정 */}` 주석 위치에 추가

### References

- AC 원본: `_bmad-output/planning-artifacts/epics.md` (Lines 343-370)
- `useChampions`: `frontend/src/hooks/useChampions.ts`
- `ChampionPickStat` / `OverviewStats` 타입: `frontend/src/lib/types/stats.ts`
- `TIER_COLORS`: `frontend/src/lib/constants/theme.ts`
- shadcn Badge: `frontend/src/components/ui/badge.tsx`
- DragonContext 패턴: `frontend/src/context/DragonContext.tsx`
- ChampionLink: `frontend/src/components/common/ChampionLink.tsx`
- Story 2.2 완료 노트 (Skeleton Table 패턴): `_bmad-output/implementation-artifacts/2-2-elo-leaderboard.md`

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

- P1: `key={entry.champion}` → `key={entry.championId}` 수정 (안정적인 고유 키 사용)
- P2: Skeleton 구조를 tier-grouped 레이아웃 근사치로 교체 (layout shift 방지, NFR12)

### File List

- frontend/src/components/dashboard/ChampionTierTable.tsx (신규)
- frontend/src/pages/HomePage.tsx (수정)
