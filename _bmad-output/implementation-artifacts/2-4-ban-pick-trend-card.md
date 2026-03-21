# Story 2.4: BanTrendCard 컴포넌트 & 홈 배치

Status: done

## Story

As a 유저,
I want 홈 화면에서 최근 많이 밴되는 챔피언 트렌드를 확인하기를,
So that 현재 메타에서 어떤 챔피언이 위협적으로 인식되는지 빠르게 파악할 수 있다. (FR3)

## Acceptance Criteria

1. **Given** 유저가 홈에 접속했을 때
   **When** BanTrendCard가 로드되면
   **Then** 밴률 상위 챔피언 목록이 밴률 내림차순으로 표시된다

2. **Given** 밴트렌드 목록이 표시될 때
   **When** 각 챔피언 행을 확인하면
   **Then** 챔피언 아이콘, 이름(한국어), 밴률(%), 밴 횟수가 표시되고 수치는 `font-mono`로 정렬된다

3. **Given** 밴률 수치를 확인할 때
   **When** 표시된 수치를 보면
   **Then** `(picks / matchCount) * 100`으로 계산된 밴률이 소수점 1자리로 표시된다

4. **Given** 홈 페이지 번들을 확인할 때
   **When** 번들 내용을 보면
   **Then** Chart.js import가 없다 (NFR5)

## Tasks / Subtasks

- [x] Task 1: `BanTrendCard.tsx` 컴포넌트 구현 (AC: #1, #2, #3, #4)
  - [x] `frontend/src/components/dashboard/BanTrendCard.tsx` 신규 생성
  - [x] `useChampions()` 훅으로 데이터 페치 (`OverviewStats.topBannedChampions`)
  - [x] 로딩 상태: Table 구조 Skeleton (ChampionTierTable 패턴 동일)
  - [x] 에러 상태: `InlineError` + `refetch` callback
  - [x] 밴률 계산: `(entry.picks / data.matchCount) * 100` (소수점 1자리)
  - [x] `useDragon()` 훅으로 챔피언 아이콘(imageUrl) + 한국어 이름(nameKo) 표시
  - [x] `ChampionLink`으로 챔피언 행 클릭 가능
  - [x] 수치(밴률/밴횟수)에 `font-mono` 클래스 적용
  - [x] `key={entry.championId}` 사용 (문자열 키 금지)
  - [x] Chart.js import 절대 금지 (NFR5)

- [x] Task 2: `HomePage.tsx` 업데이트 — BanTrendCard 배치 (AC: #1)
  - [x] `{/* Story 2.4: BanTrendCard 추가 예정 */}` 주석 제거
  - [x] `BanTrendCard` import 및 섹션 추가

- [x] Task 3: TypeScript 검증
  - [x] `npx tsc --noEmit` 오류 없이 통과 확인

## Dev Notes

### 🚨 CRITICAL: 타입 주의 — topBannedChampions는 ChampionPickStat[] 타입

`topBannedChampions`는 별도 BanStat 타입이 아닌 **ChampionPickStat[]** 그대로 사용:

```ts
// frontend/src/lib/types/stats.ts
export interface ChampionPickStat {
  champion: string;   // 챔피언 영문명 (e.g., "Yasuo")
  championId: number; // DragonContext 조회 키
  picks: number;      // ← 밴 컨텍스트에서는 "밴 횟수"
  wins: number;       // 밴 컨텍스트에서는 무의미 (사용 안 함)
  winRate: number;    // 밴 컨텍스트에서는 무의미 (사용 안 함)
}

export interface OverviewStats {
  matchCount: number;                         // 밴률 계산 분모
  topBannedChampions: ChampionPickStat[];     // ← 이 필드 사용
  topPickedChampions: ChampionPickStat[];     // Story 2.3에서 사용
  // ...
}
```

**밴률 계산:**
```ts
// 밴률 = 전체 경기 중 밴된 비율
const banRate = data.matchCount > 0
  ? ((entry.picks / data.matchCount) * 100).toFixed(1)
  : '0.0';
```

### Task 1 상세: BanTrendCard.tsx 전체 구조

```tsx
// frontend/src/components/dashboard/BanTrendCard.tsx

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

export function BanTrendCard() {
  const { data, isLoading, error, refetch } = useChampions();
  const { champions: dragonChampions } = useDragon();

  if (isLoading) {
    return (
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>챔피언</TableHead>
            <TableHead className="text-right font-mono">밴률</TableHead>
            <TableHead className="text-right font-mono">밴 횟수</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {Array.from({ length: 5 }).map((_, i) => (
            <TableRow key={i}>
              <TableCell>
                <div className="flex items-center gap-2">
                  <Skeleton className="h-8 w-8 rounded" />
                  <Skeleton className="h-4 w-20" />
                </div>
              </TableCell>
              <TableCell className="text-right"><Skeleton className="h-4 w-12 ml-auto" /></TableCell>
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
        message="밴 트렌드를 불러오지 못했습니다."
        onRetry={() => void refetch()}
      />
    );
  }

  if (!data || data.topBannedChampions.length === 0) {
    return (
      <p className="text-sm p-4" style={{ color: 'var(--color-text-secondary)' }}>
        밴 데이터가 없습니다.
      </p>
    );
  }

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>챔피언</TableHead>
          <TableHead className="text-right font-mono">밴률</TableHead>
          <TableHead className="text-right font-mono">밴 횟수</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {data.topBannedChampions.map((entry: ChampionPickStat) => {
          const dragon = dragonChampions.get(entry.championId);
          const displayName = dragon?.nameKo ?? entry.champion;
          const imgUrl = dragon?.imageUrl ?? null;
          const banRate = data.matchCount > 0
            ? ((entry.picks / data.matchCount) * 100).toFixed(1)
            : '0.0';

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
                style={{ fontWeight: 700, color: 'var(--color-text-primary)' }}
              >
                {banRate}%
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
  );
}
```

### Task 2 상세: HomePage.tsx 업데이트

```tsx
// 추가할 import
import { BanTrendCard } from '@/components/dashboard/BanTrendCard';

// {/* Story 2.4: BanTrendCard 추가 예정 */} 주석을 아래로 교체:
<section>
  <h2 className="page-title" style={{ marginBottom: 'var(--spacing-md)' }}>
    밴픽 트렌드
  </h2>
  <BanTrendCard />
</section>
```

### 데이터 훅 선택

`useChampions()`를 재사용한다. `useBanRecommend()` 훅도 동일한 엔드포인트를 호출하지만 queryKey가 달라 캐시가 분리된다. BanTrendCard는 홈 화면 컴포넌트이므로 `useChampions()` 재사용이 적합 (ChampionTierTable과 캐시 공유).

### DragonContext 사용 패턴

```tsx
const { champions: dragonChampions } = useDragon();
const dragon = dragonChampions.get(entry.championId);
// dragon?.imageUrl — 챔피언 이미지 URL
// dragon?.nameKo  — 한국어 이름 (없으면 entry.champion 폴백)
```

### ChampionLink 인터페이스

```tsx
// frontend/src/components/common/ChampionLink.tsx
// props: champion(string), championId(number), className?(string)
<ChampionLink champion={entry.champion} championId={entry.championId}>
  {children}
</ChampionLink>
```

### 코딩 컨벤션 (엄수)

- **Named export**: `export function BanTrendCard()` (default 금지)
- **`import type`**: 타입 전용은 반드시 `import type`
- **신규 컴포넌트 스타일링**: Tailwind 우선 + CSS 변수(의미있는 색상)
- **Chart.js import 절대 금지**: 홈 페이지에 Chart.js 없음 (NFR5)
- **`key={entry.championId}`**: 안정적 고유 키 사용 (Story 2.3 P1 패치 교훈)

### 🚨 Story 2.2~2.3 학습 사항

- **`key` prop**: 문자열 챔피언 이름 대신 반드시 `entry.championId` 사용 (Story 2.3 P1)
- **Skeleton loading: Table 구조 일치**: Skeleton도 `<Table>` 안에 배치 (Story 2.2 P3, 2.3 P2)
- **`|| undefined` 패턴**: falsy 값(빈 문자열 포함) 제거 시 `?? undefined` 대신 `|| undefined`
- **Skeleton 근사치**: 단일 flat Table Skeleton은 실제 구조와 일치시킨다

### 파일 위치 규칙

```
frontend/src/
  components/
    dashboard/
      EloLeaderboard.tsx     ← 기존 (수정 없음)
      ChampionTierTable.tsx  ← 기존 (수정 없음)
      BanTrendCard.tsx       ← 신규 (Task 1)
  pages/
    HomePage.tsx             ← 수정 (Task 2, 2.4 주석 → 실제 컴포넌트)
```

### 다음 스토리 연계

- **Epic 2 완료 후**: `epic-2-retrospective` (optional) 실행 가능
- **Story 3.1** (`ChampionListPage`): 챔피언 상세 페이지 (Chart.js 동적 로드 허용)

### References

- AC 원본: `_bmad-output/planning-artifacts/epics.md` (Epic 2 Story 2.4)
- `useChampions`: `frontend/src/hooks/useChampions.ts`
- `ChampionPickStat` / `OverviewStats` 타입: `frontend/src/lib/types/stats.ts`
- shadcn Table: `frontend/src/components/ui/table.tsx`
- DragonContext 패턴: `frontend/src/context/DragonContext.tsx`
- ChampionLink: `frontend/src/components/common/ChampionLink.tsx`
- Story 2.3 완료 노트 (key 패치, Skeleton 구조): `_bmad-output/implementation-artifacts/2-3-champion-tier-table.md`

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

- P1: `[...data.topBannedChampions].sort((a, b) => b.picks - a.picks)` 추가 — AC1 밴률 내림차순 보장 (서버 순서에 묵시적 의존 제거)
- `useChampions()` 훅 재사용 (queryKey `['champions']`, ChampionTierTable과 캐시 공유)
- `topBannedChampions: ChampionPickStat[]` — `picks` 필드가 밴 횟수, `winRate`는 사용 안 함
- 밴률 = `(entry.picks / data.matchCount) * 100`
- Story 2.3 교훈 적용: `key={entry.championId}`, Skeleton Table 구조 일치
- TypeScript 오류 없음 (`npx tsc --noEmit` 통과)

### File List

- frontend/src/components/dashboard/BanTrendCard.tsx (신규)
- frontend/src/pages/HomePage.tsx (수정)
