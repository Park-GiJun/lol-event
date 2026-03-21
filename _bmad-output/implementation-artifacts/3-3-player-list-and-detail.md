# Story 3.3: 플레이어 목록 & 상세 페이지 (수치 카드)

Status: done

## Story

As a 유저,
I want 플레이어 목록에서 플레이어를 클릭하여 개인 통계 수치 카드를 확인하기를,
So that 특정 플레이어의 전반적인 성적을 한눈에 파악할 수 있다. (FR5, FR12)

## Acceptance Criteria

1. **Given** 유저가 플레이어 목록 페이지에 있을 때
   **When** 특정 플레이어 행을 클릭하면
   **Then** 1클릭으로 플레이어 상세 페이지(`/player-stats/:riotId`)로 이동한다 (이미 구현됨 — 검증만)

2. **Given** 플레이어 상세 페이지에 진입했을 때
   **When** 페이지가 로드되면
   **Then** 승률, 게임 수, KDA, 평균 킬/데스/어시스트, **주력 챔피언** 수치 카드가 표시된다
   - **주력 챔피언**: `data.championStats[0]` (판수 기준 1위), 챔피언 이미지 + 한국어 이름 + 게임 수 + 승률 표시

3. **Given** 승률이 50% 이상일 때 (NFR7)
   **When** 수치를 확인하면
   **Then** `var(--color-win)` (#10B981) 색상이 적용되고 **"높음"** 텍스트 레이블도 병행 표시된다
   - 50% 미만: `var(--color-loss)` (#EF4444) + **"낮음"** 텍스트

4. **Given** 플레이어 목록 페이지 로딩 중일 때
   **When** API 응답 대기 중이면
   **Then** LoadingCenter 대신 Skeleton UI가 표시된다 (테이블 행 구조 유지)

## Tasks / Subtasks

- [x] Task 1: `PlayerStatsPage.tsx` 수정 — 주력 챔피언 카드 추가 (AC: #2)
  - [x] `player-summary-card` 내 `player-stat-cards` 영역 아래(또는 우측)에 주력 챔피언 섹션 추가
  - [x] `data.championStats[0]` 이 존재할 때만 렌더링 (없으면 null)
  - [x] `ChampImg` 컴포넌트(이미 파일 내 정의됨) 재사용 — size={36}
  - [x] `useDragon()` → `champions.get(stat.championId)?.nameKo ?? stat.champion` 로 한국어 이름 표시
  - [x] 표시 정보: 이미지, 챔피언명, `{games}판`, `{winRate.toFixed(1)}%` (승률 색상 적용)

- [x] Task 2: `PlayerStatsPage.tsx` 수정 — NFR7 텍스트 레이블 추가 (AC: #3)
  - [x] `player-summary-header` 내 WR ring 영역에서 `{data.winRate}%` 아래에 텍스트 추가
  - [x] 승률 ≥ 50%: "높음" (color: `var(--color-win)`)
  - [x] 승률 < 50%: "낮음" (color: `var(--color-loss)`)
  - [x] 폰트 사이즈: 10px

- [x] Task 3: `MemberStatsListPage.tsx` 수정 — Skeleton 로딩 개선 (AC: #4)
  - [x] `LoadingCenter` 대신 테이블 구조를 유지하는 Skeleton 추가
  - [x] `ChampionListPage.tsx` Skeleton 패턴 참조 (8행 × Skeleton 셀)
  - [x] 기존 테이블 컬럼 수(8열)에 맞게 구성: #, 플레이어, 판수, 승률, KDA, 평균 딜, CS, 주요 챔피언
  - [x] `import { Skeleton } from '../components/common/Skeleton'` 추가

- [x] Task 4: TypeScript 검증
  - [x] `npx tsc --noEmit` 오류 없이 통과 확인

## Dev Notes

### 🚨 CRITICAL: 기존 구현 분석 — 중복 작업 금지

**이미 완성된 항목 (수정 불필요):**
- `MemberStatsListPage.tsx`: 행 클릭 시 `navigate('/player-stats/${encodeURIComponent(s.riotId)}')` ✅
- `PlayerStatsPage.tsx`: WR ring(승률), 게임 수/승패, KDA, K/D/A 표시 ✅
- `PlayerStatsPage.tsx`: `StatCard` 컴포넌트로 평균 딜/CS/골드/시야 ✅
- `App.tsx`: `/player-stats/:riotId` 라우트 등록 ✅

**누락된 항목 (이번 스토리에서 추가):**
- `PlayerStatsPage`: 주력 챔피언 카드 (AC2 gap)
- `PlayerStatsPage`: NFR7 텍스트 레이블 "높음"/"낮음" (AC3 gap)
- `MemberStatsListPage`: Skeleton 로딩 (LoadingCenter → Skeleton 개선)

### Task 1 상세: 주력 챔피언 카드 구현

**현재 `PlayerStatsPage.tsx`의 `player-stat-cards` 구조:**
```tsx
<div className="player-stat-cards">
  <StatCard icon={<Sword size={14} />} label="평균 딜량" value={data.avgDamage.toLocaleString()} />
  <StatCard icon={<Shield size={14} />} label="평균 CS" value={data.avgCs.toFixed(1)} />
  <StatCard icon={<Coins size={14} />} label="평균 골드" value={data.avgGold.toLocaleString()} />
  <StatCard icon={<Eye size={14} />} label="평균 시야" value={data.avgVisionScore.toFixed(1)} />
</div>
```

**추가할 주력 챔피언 섹션 위치:** `player-stat-cards` div 바로 아래, `player-summary-card` 내부

```tsx
{data.championStats.length > 0 && (() => {
  const top = data.championStats[0];
  const topWrColor = top.winRate >= 60 ? 'var(--color-win)' : top.winRate >= 50 ? 'var(--color-primary)' : 'var(--color-loss)';
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 12, paddingTop: 12, borderTop: '1px solid var(--color-border)' }}>
      <ChampImg championId={top.championId} champion={top.champion} size={36} />
      <div>
        <div style={{ fontSize: 12, fontWeight: 700 }}>{champions.get(top.championId)?.nameKo ?? top.champion}</div>
        <div style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>{top.games}판</div>
      </div>
      <div style={{ marginLeft: 'auto', fontWeight: 700, color: topWrColor, fontSize: 14 }}>
        {top.winRate.toFixed(1)}%
      </div>
      <div style={{ fontSize: 10, color: 'var(--color-text-secondary)' }}>주력 챔피언</div>
    </div>
  );
})()}
```

**주의:** `champions`는 이미 `const { champions } = useDragon();` 로 선언되어 있음. 추가 import 불필요.

### Task 2 상세: NFR7 텍스트 레이블

**현재 WR ring 코드 (PlayerStatsPage.tsx, player-wr-ring 내):**
```tsx
<span style={{ fontSize: 18, fontWeight: 700, color: winColor(data.winRate) }}>{data.winRate}%</span>
<span style={{ fontSize: 10, color: 'var(--color-text-secondary)' }}>승률</span>
```

**수정 후:**
```tsx
<span style={{ fontSize: 18, fontWeight: 700, color: winColor(data.winRate) }}>{data.winRate}%</span>
<span style={{ fontSize: 10, color: 'var(--color-text-secondary)' }}>승률</span>
<span style={{ fontSize: 10, fontWeight: 700, color: data.winRate >= 50 ? 'var(--color-win)' : 'var(--color-loss)' }}>
  {data.winRate >= 50 ? '높음' : '낮음'}
</span>
```

### Task 3 상세: MemberStatsListPage Skeleton 패턴

`ChampionListPage.tsx`의 Skeleton 패턴을 참조하되, **기존 테이블 컬럼 구조(8열)** 에 맞게 적용:

```tsx
if (loading) {
  return (
    <div>
      <div className="page-header flex items-center justify-between">
        <div>
          <h1 className="page-title">멤버 통계</h1>
          <Skeleton className="h-4 w-32 mt-1" />
        </div>
        {/* 모드 버튼 영역 — 기존 코드 유지 */}
      </div>
      <div className="card">
        <div className="member-sort-tabs">
          {cols.map(c => (
            <button key={c.key} className="member-sort-tab">{c.label}</button>
          ))}
        </div>
        <div className="table-wrapper">
          <table className="table member-stats-table">
            <thead>
              <tr>
                <th style={{ width: 36 }}>#</th>
                <th>플레이어</th>
                <th>판수</th>
                <th style={{ minWidth: 120 }}>승률</th>
                <th>KDA</th>
                <th className="table-number">평균 딜</th>
                <th className="table-number">CS</th>
                <th>주요 챔피언</th>
              </tr>
            </thead>
            <tbody>
              {Array.from({ length: 8 }).map((_, i) => (
                <tr key={i}>
                  <td><Skeleton className="h-6 w-6 rounded-full" /></td>
                  <td><Skeleton className="h-4 w-28" /></td>
                  <td><Skeleton className="h-4 w-10" /></td>
                  <td><Skeleton className="h-4 w-20" /></td>
                  <td><Skeleton className="h-4 w-14" /></td>
                  <td><Skeleton className="h-4 w-16 ml-auto" /></td>
                  <td><Skeleton className="h-4 w-10 ml-auto" /></td>
                  <td><div className="flex gap-1">{Array.from({ length: 3 }).map((_, j) => <Skeleton key={j} className="h-6 w-6 rounded" />)}</div></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
```

**주의:** `Skeleton` import는 `'@/components/common/Skeleton'` 대신 실제 파일 구조에 맞게 `'../components/common/Skeleton'`으로 해야 함 (MemberStatsListPage는 `pages/` 디렉토리).

### 코딩 컨벤션 (엄수)

- **Named export**: `export function MemberStatsListPage()`, `export function PlayerStatsPage()` (이미 준수됨)
- **import type**: 타입 전용 import에 `import type` 사용
- **CSS 변수**: `var(--color-win)`, `var(--color-loss)`, `var(--color-primary)` — 하드코딩 금지
- **Tailwind**: 레이아웃/구조적 스타일에만, 색상은 CSS 변수 사용
- **Skeleton** 컴포넌트: `frontend/src/components/common/Skeleton.tsx` 에 있음

### 파일 위치 규칙

```
frontend/src/
  pages/
    MemberStatsListPage.tsx    ← 수정 (Task 3)
    PlayerStatsPage.tsx        ← 수정 (Task 1, 2)
```

신규 파일 생성 없음.

### Story 3.1/3.2 코드 리뷰 학습 사항

- **named export 필수** — default export 사용 금지 (기존 파일 이미 준수)
- **import type** — `import type { ... }` 타입 전용 import
- **Skeleton 높이 고정** — 레이아웃 시프트(CLS) 방지를 위해 Skeleton 크기를 실제 요소 크기에 맞게
- **클라이언트 정렬** — 목록에서 정렬 변경 시 재요청 없이 클라이언트 정렬 유지 (기존 구현 유지)

### References

- `PlayerDetailStats` 타입: `frontend/src/lib/types/stats.ts` (lines 77-95)
  - 핵심 필드: `riotId, games, wins, losses, winRate, avgKills, avgDeaths, avgAssists, kda, championStats, laneStats`
- `ChampionStat` 타입: `frontend/src/lib/types/stats.ts` (lines 27-42)
  - 핵심 필드: `champion, championId, games, wins, winRate, kda`
- `PlayerStatsPage`: `frontend/src/pages/PlayerStatsPage.tsx`
- `MemberStatsListPage`: `frontend/src/pages/MemberStatsListPage.tsx`
- `Skeleton` 컴포넌트: `frontend/src/components/common/Skeleton.tsx`
- `ChampionListPage.tsx`: `frontend/src/pages/ChampionListPage.tsx` — Skeleton 패턴 참조

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

- `PlayerStatsPage`: `const { champions } = useDragon()` 추가 → 주력 챔피언 한국어 이름 표시
- `PlayerStatsPage`: `player-stat-cards` 아래 주력 챔피언 섹션 추가 — `data.championStats[0]` 조건부 렌더링, `ChampImg` 재사용, 승률 색상 적용
- `PlayerStatsPage`: WR ring에 NFR7 "높음"/"낮음" 텍스트 레이블 추가 (≥50% 높음, <50% 낮음)
- `MemberStatsListPage`: `LoadingCenter` 제거, `Skeleton` 기반 8행 테이블 Skeleton 로딩 추가 (레이아웃 시프트 방지)
- TypeScript 오류 없음 (`npx tsc --noEmit` 통과)

### File List

- frontend/src/pages/PlayerStatsPage.tsx (수정)
- frontend/src/pages/MemberStatsListPage.tsx (수정)
