# Story 2.2: EloLeaderboard 컴포넌트 & 홈 배치

Status: done

## Story

As a 유저,
I want 홈 화면에서 전체 멤버 Elo 순위표를 즉시 확인하기를,
So that 내 순위가 어디쯤인지 추가 탐색 없이 파악할 수 있다. (FR1, FR14, FR26)

## Acceptance Criteria

1. **Given** 유저가 홈(`/`)에 접속했을 때
   **When** 페이지가 로드되면
   **Then** 3초 이내 Elo 리더보드가 표시된다 (NFR3)

2. **Given** 리더보드가 로딩 중일 때
   **When** 데이터가 아직 없으면
   **Then** Skeleton UI가 표시되고 레이아웃이 흔들리지 않는다 (NFR12)

3. **Given** 현재 유저의 riotId가 localStorage에 존재할 때
   **When** 리더보드를 확인하면
   **Then** 해당 행에 `bg-teal-900/20` 배경 하이라이트가 자동으로 적용된다 (FR14)

4. **Given** 리더보드 데이터가 표시될 때
   **When** 각 행을 확인하면
   **Then** 순위, 닉네임, 티어, Elo 수치, 최근 N게임이 표시되고 수치는 `font-mono`로 정렬된다

5. **Given** API 호출이 실패했을 때
   **When** 에러가 발생하면
   **Then** InlineError 컴포넌트가 표시되고 재시도 버튼이 동작한다 (NFR11)

## Tasks / Subtasks

- [x] Task 1: `EloLeaderboard.tsx` 컴포넌트 구현 (AC: #1, #2, #3, #4, #5)
  - [x] `frontend/src/components/dashboard/EloLeaderboard.tsx` 신규 생성
  - [x] `useLeaderboard()` 훅으로 데이터 페치
  - [x] 로딩 상태: `Skeleton` 컴포넌트 5행 플레이스홀더 표시
  - [x] 에러 상태: `InlineError` + `refetch` callback 연결
  - [x] shadcn `Table` 컴포넌트로 테이블 구성 (순위/닉네임/티어/Elo/게임수)
  - [x] Elo/게임수 수치에 `font-mono` 클래스 적용
  - [x] `currentRiotId` prop 일치 행에 `bg-teal-900/20` 하이라이트
  - [x] `eloTier()` 인라인 함수 정의 (Challenger~Bronze)
  - [x] 순위 1/2/3 메달 배지 (금/은/동 원형), 그 이상 plain 텍스트
  - [x] `PlayerLink` wrapper로 닉네임 감싸기 (`mode="all"`)

- [x] Task 2: `HomePage.tsx` 신규 생성 (AC: #1)
  - [x] `frontend/src/pages/HomePage.tsx` 신규 생성
  - [x] localStorage `lol-event:currentRiotId` 키 읽어 `currentRiotId` 결정
  - [x] `EloLeaderboard` 컴포넌트 배치 (currentRiotId 전달)
  - [x] 추후 2.3/2.4 컴포넌트를 위한 레이아웃 구조 준비 (2컬럼 그리드 or 단일컬럼)

- [x] Task 3: `App.tsx` 라우팅 업데이트 (AC: #1)
  - [x] `frontend/src/App.tsx` 읽기 — 현재 `<Route index element={<StatsPage />}>` 확인
  - [x] `<Route index element={<HomePage />}>` 로 변경
  - [x] `HomePage` import 추가 (`import type` 금지 — 컴포넌트는 값 import)

- [x] Task 4: TypeScript 검증
  - [x] `npx tsc --noEmit` 오류 없이 통과 확인

## Dev Notes

### 🚨 CRITICAL: 기존 코드 활용 — 재발명 금지

**이미 구현된 것들 (수정 없이 사용):**
- `useLeaderboard()` 훅 → `frontend/src/hooks/useLeaderboard.ts` — `queryKey: ['leaderboard']`, 엔드포인트 `/stats/elo`, 반환타입 `EloLeaderboardResult`
- `InlineError` → `frontend/src/components/common/InlineError.tsx` — props: `{ message: string, onRetry: () => void, className?: string }`
- `Skeleton` → `frontend/src/components/common/Skeleton.tsx` — props: `React.HTMLAttributes<HTMLDivElement>` + className
- shadcn Table → `frontend/src/components/ui/table.tsx` — export: `Table, TableHeader, TableBody, TableRow, TableHead, TableCell`
- `PlayerLink` → `frontend/src/components/common/PlayerLink.tsx` — `mode="all"` 사용

### Task 1 상세: EloLeaderboard.tsx 전체 구조

```tsx
// frontend/src/components/dashboard/EloLeaderboard.tsx

import { useLeaderboard } from '@/hooks/useLeaderboard';
import { InlineError } from '@/components/common/InlineError';
import { Skeleton } from '@/components/common/Skeleton';
import { PlayerLink } from '@/components/common/PlayerLink';
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from '@/components/ui/table';
import type { EloRankEntry } from '@/lib/types/stats';

// ── 내부 유틸 ────────────────────────────────────────────────
function eloTier(elo: number): { label: string; color: string } {
  if (elo >= 1300) return { label: 'Challenger', color: '#FFD700' };
  if (elo >= 1200) return { label: 'Master',     color: '#AA47BC' };
  if (elo >= 1100) return { label: 'Diamond',    color: '#0BC4B4' };
  if (elo >= 1000) return { label: 'Platinum',   color: '#4A9EFF' };
  if (elo >= 900)  return { label: 'Gold',       color: '#C89B3C' };
  if (elo >= 800)  return { label: 'Silver',     color: '#A8A8A8' };
  return                  { label: 'Bronze',     color: '#CD7F32' };
}

const RANK_COLORS: Record<number, string> = { 1: '#FFD700', 2: '#C0C0C0', 3: '#CD7F32' };
function RankBadge({ rank }: { rank: number }) {
  const color = RANK_COLORS[rank];
  if (color) {
    return (
      <div style={{
        width: 26, height: 26, borderRadius: '50%',
        background: color, display: 'flex', alignItems: 'center',
        justifyContent: 'center', fontSize: 11, fontWeight: 800,
        color: '#111', flexShrink: 0,
      }}>{rank}</div>
    );
  }
  return (
    <span style={{
      color: 'var(--color-text-disabled)', fontSize: 12,
      width: 26, textAlign: 'center', display: 'inline-block',
    }}>{rank}</span>
  );
}

// ── props ────────────────────────────────────────────────────
export interface EloLeaderboardProps {
  currentRiotId?: string;
}

// ── 컴포넌트 ─────────────────────────────────────────────────
export function EloLeaderboard({ currentRiotId }: EloLeaderboardProps) {
  const { data, isLoading, error, refetch } = useLeaderboard();

  if (isLoading) {
    return (
      <div className="flex flex-col gap-2 p-4">
        {Array.from({ length: 5 }).map((_, i) => (
          <Skeleton key={i} className="h-10 w-full" />
        ))}
      </div>
    );
  }

  if (error) {
    return (
      <InlineError
        message="리더보드를 불러오지 못했습니다."
        onRetry={() => void refetch()}
      />
    );
  }

  if (!data || data.players.length === 0) {
    return (
      <p className="text-sm p-4" style={{ color: 'var(--color-text-secondary)' }}>
        Elo 데이터가 없습니다. 어드민에서 재집계를 실행하세요.
      </p>
    );
  }

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead className="w-12">순위</TableHead>
          <TableHead>플레이어</TableHead>
          <TableHead>티어</TableHead>
          <TableHead className="text-right font-mono">Elo</TableHead>
          <TableHead className="text-right font-mono">게임</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {data.players.map((entry: EloRankEntry) => {
          const tier = eloTier(entry.elo);
          const isCurrentUser = currentRiotId && entry.riotId === currentRiotId;
          return (
            <TableRow
              key={entry.riotId}
              className={isCurrentUser ? 'bg-teal-900/20' : undefined}
            >
              <TableCell>
                <RankBadge rank={entry.rank} />
              </TableCell>
              <TableCell>
                <PlayerLink riotId={entry.riotId} mode="all">
                  <span style={{ fontWeight: 600 }}>
                    {entry.riotId.split('#')[0]}
                  </span>
                  <span style={{ fontSize: 10, color: 'var(--color-text-secondary)', marginLeft: 4 }}>
                    #{entry.riotId.split('#')[1]}
                  </span>
                </PlayerLink>
              </TableCell>
              <TableCell>
                <span style={{
                  fontSize: 11, fontWeight: 700, color: tier.color,
                  background: tier.color + '22', borderRadius: 4, padding: '2px 7px',
                  border: `1px solid ${tier.color}44`,
                }}>
                  {tier.label}
                </span>
              </TableCell>
              <TableCell className="text-right font-mono" style={{ fontWeight: 700, color: tier.color }}>
                {entry.elo.toFixed(1)}
              </TableCell>
              <TableCell className="text-right font-mono" style={{ color: 'var(--color-text-secondary)' }}>
                {entry.games}
              </TableCell>
            </TableRow>
          );
        })}
      </TableBody>
    </Table>
  );
}
```

### Task 2 상세: HomePage.tsx 구조

```tsx
// frontend/src/pages/HomePage.tsx

import { EloLeaderboard } from '@/components/dashboard/EloLeaderboard';

// localStorage 키 (전체 앱 통일)
const CURRENT_RIOT_ID_KEY = 'lol-event:currentRiotId';

export function HomePage() {
  const currentRiotId = localStorage.getItem(CURRENT_RIOT_ID_KEY) ?? undefined;

  return (
    <div className="flex flex-col gap-6">
      <section>
        <h2 className="page-title" style={{ marginBottom: 'var(--spacing-md)' }}>
          Elo 리더보드
        </h2>
        <EloLeaderboard currentRiotId={currentRiotId} />
      </section>

      {/* Story 2.3: ChampionTierTable 추가 예정 */}
      {/* Story 2.4: BanTrendCard 추가 예정 */}
    </div>
  );
}
```

**참고:** `.page-title` 클래스는 `sidebar.css`에 정의됨 (`font-size: var(--font-size-xl); font-weight: 700; color: var(--color-text-primary)`)

### Task 3 상세: App.tsx 라우팅 변경

```tsx
// 현재:
<Route index element={<StatsPage />} />

// 변경:
<Route index element={<HomePage />} />
```

**주의사항:**
- `StatsPage` import는 `App.tsx`에 남겨둠 (다른 탭에서 StatsPage 사용 여부 확인)
- `import type` 금지 — 컴포넌트는 값이므로 `import { HomePage } from '@/pages/HomePage'`
- `App.tsx`에서 기존 `StatsPage` import 제거 여부: App.tsx를 먼저 읽어서 StatsPage가 다른 곳에도 쓰이는지 확인 후 결정

### EloRankEntry 타입 참조

```ts
// frontend/src/lib/types/stats.ts
export interface EloRankEntry {
  rank: number;
  riotId: string;
  elo: number;
  games: number;
}
export interface EloLeaderboardResult {
  players: EloRankEntry[];
}
```

### 코딩 컨벤션 (엄수)

- **Named export 필수** — `export function EloLeaderboard()` (default export 금지)
- **`import type`** — 타입 전용 import에 `import type` 사용 (`verbatimModuleSyntax: true`)
- **`@/` path alias** — 모든 내부 import에 사용
- **신규 컴포넌트 스타일링** — Tailwind CSS 클래스 우선 + CSS 변수(의미있는 색상)
- **Chart.js import 금지** — 홈/목록 페이지에서 Chart.js 절대 import 금지 (NFR5)

### PlayerLink 인터페이스 참고

```tsx
// PlayerLink props 요약 (정확한 인터페이스는 PlayerLink.tsx 읽어서 확인)
<PlayerLink riotId={entry.riotId} mode="all">
  {children}
</PlayerLink>
```
`mode="all"` — 전체 상세 페이지 링크 모드 사용

### useLeaderboard 반환 값

```ts
const { data, isLoading, error, refetch } = useLeaderboard();
// data: EloLeaderboardResult | undefined
// isLoading: boolean
// error: Error | null
// refetch: () => Promise<QueryObserverResult>
```

`refetch`는 Promise를 반환하므로 `onClick` 콜백에서 `() => void refetch()` 패턴 사용 (TypeScript unused promise warning 방지)

### 파일 위치 규칙

```
frontend/src/
  components/
    dashboard/
      EloLeaderboard.tsx    ← 신규 (Task 1)
  pages/
    HomePage.tsx             ← 신규 (Task 2)
  App.tsx                    ← 수정 (Task 3, 라우팅만)
```

### 🚨 Story 1.x 학습 사항

- **`verbatimModuleSyntax: true`** — 타입 임포트에 반드시 `import type` 사용
- **`noUnusedLocals`** — 미사용 변수 선언 시 컴파일 오류
- **shadcn CSS 변수** — Story 2.1에서 `--muted`, `--card`, `--border` 등 매핑 완료. `bg-muted`, `text-muted-foreground` 등 Tailwind 클래스 정상 동작
- **`bg-teal-900/20`** — Tailwind 색상. Teal 테마와 정합. `rgba(19, 78, 74, 0.2)` 렌더링

### 다음 스토리 연계

- **Story 2.3** (`ChampionTierTable`): `HomePage.tsx`의 `{/* Story 2.3 */}` 주석 위치에 추가
- **Story 2.4** (`BanTrendCard`): `HomePage.tsx`의 `{/* Story 2.4 */}` 주석 위치에 추가

### References

- AC 원본: `_bmad-output/planning-artifacts/epics.md` (Lines 314-342)
- 아키텍처: `_bmad-output/planning-artifacts/architecture.md` (Lines 141-187, 224-273)
- UX 컴포넌트 전략: `_bmad-output/planning-artifacts/ux-design-specification.md` (Lines 453-532)
- `useLeaderboard`: `frontend/src/hooks/useLeaderboard.ts`
- `EloRankEntry` 타입: `frontend/src/lib/types/stats.ts`
- 기존 EloTab 구현 참조: `frontend/src/pages/StatsPage.tsx` (Lines 314-395)
- shadcn Table: `frontend/src/components/ui/table.tsx`

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

- ✅ `frontend/src/components/dashboard/EloLeaderboard.tsx` — 신규 생성. useLeaderboard 훅 + shadcn Table + Skeleton(5행) + InlineError + eloTier 함수 + RankBadge + bg-teal-900/20 하이라이트 + font-mono 수치 정렬 + PlayerLink(mode="all")
- ✅ `frontend/src/pages/HomePage.tsx` — 신규 생성. localStorage `lol-event:currentRiotId` 읽어 EloLeaderboard에 전달. Story 2.3/2.4 주석 플레이스홀더 포함
- ✅ `frontend/src/App.tsx` — StatsPage import/route 제거, HomePage로 교체 (`/` 라우트). noUnusedLocals 대응
- ✅ `npx tsc --noEmit` 오류 없음

### File List

- `frontend/src/components/dashboard/EloLeaderboard.tsx` (신규)
- `frontend/src/pages/HomePage.tsx` (신규)
- `frontend/src/App.tsx` (수정 — index 라우트 StatsPage → HomePage)
