# Story 4.3: ChampSelectPage 대기방 뷰 재설계 & 자동 트리거

Status: done

## Story

As a 유저,
I want LoL 대기방에 입장했을 때 Electron이 자동으로 양팀 분석 화면을 표시하기를,
So that 별도 조작 없이 2초 안에 상대팀 정보와 밴 추천을 확인할 수 있다. (FR15, FR16, FR20)

## Acceptance Criteria

1. **Given** LoL 클라이언트에서 대기방에 진입했을 때
   **When** LCU API가 대기방 이벤트를 감지하면
   **Then** 2초 이내 ChampSelectPage에 양팀 PlayerCard가 자동으로 렌더링된다 (NFR2)

2. **Given** ChampSelectPage가 렌더링될 때
   **When** 레이아웃을 확인하면
   **Then** `grid-cols-2` 좌우 분할로 우리팀(좌)/상대팀(우) PlayerCard가 각 5개 배치된다 (FR17)

3. **Given** LoL 클라이언트가 실행되지 않은 상태일 때
   **When** ChampSelectPage를 확인하면
   **Then** "LoL 클라이언트를 실행해주세요" 안내 메시지가 표시된다 (FR20, NFR9)

4. **Given** 대기방 데이터 로딩 중일 때
   **When** 카드들이 아직 데이터를 받지 못했으면
   **Then** 모든 PlayerCard가 Skeleton 상태로 표시된다

5. **Given** 기존 lcu.ts 파일이 있을 때
   **When** ChampSelectPage 기능을 구현해도
   **Then** lcu.ts의 LCU API 연동 로직은 변경되지 않는다

## Tasks / Subtasks

- [x] Task 1: `ChampSelectPage.tsx` 레이아웃 재설계 — PlayerCard 그리드 (AC: #1, #2, #4)
  - [x] 기존 탭(카운터픽/밴추천/팀구성) UI 제거 또는 PlayerCard 그리드 아래로 이동
  - [x] `grid-cols-2` 레이아웃: 좌측 우리팀(myTeam), 우측 상대팀(theirTeam)
  - [x] 각 팀 5개 슬롯에 `PlayerCard` 배치 (빈 슬롯도 Skeleton으로 표시)
  - [x] 각 플레이어 riotId가 있으면 API 호출 → `PlayerData` 수집 → PlayerCard에 전달
  - [x] riotId 없음 또는 로딩 중 → `loading={true}` prop 전달 (Skeleton)
  - [x] `PlayerCard` import: `../components/lobby/PlayerCard`

- [x] Task 2: LCU 미연결 상태 안내 (AC: #3)
  - [x] `window.lol.getChampSelectFull()` 예외 또는 null 반환 시 → 안내 메시지 표시
  - [x] 메시지: "LoL 클라이언트를 실행해주세요"
  - [x] 기존 "챔피언 선택 화면이 아닙니다" 메시지와 구분하여 상태에 따라 적절한 메시지 표시

- [x] Task 3: 자동 새로고침 주기 조정 (AC: #1)
  - [x] 현재 10초 → **2초** 주기로 변경 (`setInterval(load, 2_000)`)
  - [x] 대기방 진입 감지를 위한 polling 주기 단축

- [x] Task 4: 기존 버그 수정 — `var(--color-surface-2)` (AC: #2)
  - [x] `ChampSelectPage.tsx` line 48: `ChampIcon` fallback background `'var(--color-surface-2)'` → `'var(--color-bg-hover)'`
  - [x] `ChampSelectPage.tsx` line 135: `CounterSection` badge background `'var(--color-surface-2)'` → `'var(--color-bg-hover)'`
  - [x] `var(--color-surface-2)`는 Electron global.css에 미정의 변수

- [x] Task 5: TypeScript 검증 (AC: #5)
  - [x] `electron-collector`에서 `npx tsc --noEmit` 오류 없음 확인

## Dev Notes

### 현재 ChampSelectPage.tsx 구조 (수정 대상)

```
electron-collector/src/renderer/src/pages/ChampSelectPage.tsx (수정)
electron-collector/src/renderer/src/components/lobby/PlayerCard.tsx  ← 재사용 (변경 없음)
electron-collector/src/renderer/src/components/lobby/BanRecommendBadge.tsx ← 재사용 (변경 없음)
electron-collector/src/main/lcu.ts ← 변경 없음 (AC #5)
```

### 기존 데이터 흐름 (유지)

```
window.lol.getChampSelectFull()
  → ChampSelectFull { myTeam: ChampSlot[], theirTeam: ChampSlot[], bans, phase, timer }

ChampSlot { cellId, championId, assignedPosition, riotId, summonerName, isMe }

API 엔드포인트 (BanRecommendSection에서 이미 사용 중):
  GET https://api.gijun.net/api/stats/player/{riotId}?mode=all
  → { data: PlayerDetail { riotId, games, wins, winRate, elo?, championStats: PlayerChampStat[] } }
```

### PlayerCard 인터페이스 (4-1에서 정의, 재사용)

```typescript
// electron-collector/src/renderer/src/components/lobby/PlayerCard.tsx 에서 import
import { PlayerCard, PlayerData } from '../components/lobby/PlayerCard';

export interface PlayerData {
  riotId: string;
  games: number;
  wins: number;
  winRate: number;
  elo?: number;
  championStats: PlayerChampStat[];
}

export interface PlayerCardProps {
  riotId: string;
  data: PlayerData | null;
  loading?: boolean;
}
// loading=true → Skeleton
// data=null + loading=false → 빈 카드 (데이터 없음)
```

### 새 레이아웃 구조 (핵심 변경)

```tsx
// AC #2: grid-cols-2 좌우 분할
<div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--spacing-md)' }}>
  {/* 좌측: 우리팀 */}
  <div className="card">
    <div className="card-header">
      <span className="card-title" style={{ color: 'var(--color-info)' }}>우리팀</span>
    </div>
    <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--spacing-sm)' }}>
      {state.myTeam.map(slot => (
        <PlayerCard
          key={slot.cellId}
          riotId={slot.riotId || slot.summonerName || `Player ${slot.cellId}`}
          data={playerDetails[slot.riotId] ?? null}
          loading={loading || (!slot.riotId && !playerDetails[slot.riotId])}
        />
      ))}
    </div>
  </div>
  {/* 우측: 상대팀 */}
  <div className="card">
    <div className="card-header">
      <span className="card-title" style={{ color: 'var(--color-error)' }}>상대팀</span>
    </div>
    <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--spacing-sm)' }}>
      {state.theirTeam.map(slot => (
        <PlayerCard
          key={slot.cellId}
          riotId={slot.riotId || slot.summonerName || `Player ${slot.cellId}`}
          data={playerDetails[slot.riotId] ?? null}
          loading={loading || (!slot.riotId && !playerDetails[slot.riotId])}
        />
      ))}
    </div>
  </div>
</div>
```

### 플레이어 데이터 fetch 패턴 (BanRecommendSection에서 발췌)

```typescript
// ChampSelectFull에서 양팀 riotId 수집
const allPlayers = [...state.myTeam, ...state.theirTeam];
const riotIds = allPlayers.map(s => s.riotId).filter(Boolean);

// 이미 BanRecommendSection과 동일한 패턴 사용:
const [playerDetails, setPlayerDetails] = useState<Record<string, PlayerData>>({});

useEffect(() => {
  if (!riotIds.length) return;
  Promise.all(
    riotIds.map(async (riotId) => {
      try {
        const res = await fetch(`${API}/stats/player/${encodeURIComponent(riotId)}?mode=all`);
        if (!res.ok) return null;
        const json = await res.json() as { data: PlayerData };
        return [riotId, json.data] as [string, PlayerData];
      } catch { return null; }
    })
  ).then(results => {
    const map: Record<string, PlayerData> = {};
    for (const r of results) if (r) map[r[0]] = r[1];
    setPlayerDetails(map);
  });
}, [riotIds.join(',')]);
```

**주의**: `PlayerData`는 `PlayerCard.tsx`에서 export하므로 import하여 재사용. `ChampSelectPage` 내 `PlayerDetail` 인터페이스와 구조가 동일하므로 `PlayerData`로 통일 가능.

### 자동 새로고침 (AC #1 — 2초)

```typescript
// 기존 10초 → 2초로 변경
useEffect(() => {
  const t = setInterval(load, 2_000);  // 2_000 ms
  return () => clearInterval(t);
}, [load]);
```

### LCU 미연결 안내 메시지 (AC #3)

```tsx
// window.lol.getChampSelectFull() 에서:
// - null 반환 → 대기방 아님
// - throw → 클라이언트 미실행
const [lcuError, setLcuError] = useState(false);

const load = useCallback(async () => {
  setLoading(true);
  try {
    const data = await window.lol.getChampSelectFull();
    setLcuError(false);
    setState(data);
  } catch {
    setLcuError(true);
    setState(null);
  } finally {
    setLoading(false);
  }
}, []);

// 렌더링:
{lcuError && (
  <p style={{ color: 'var(--color-text-secondary)', fontSize: 'var(--font-size-sm)' }}>
    LoL 클라이언트를 실행해주세요
  </p>
)}
{!lcuError && !state && !loading && (
  <p style={{ color: 'var(--color-text-secondary)', fontSize: 'var(--font-size-sm)' }}>
    챔피언 선택 화면이 아닙니다
  </p>
)}
```

### CSS 변수 버그 수정 (Task 4)

```typescript
// ChampSelectPage.tsx의 ChampIcon 함수 (line ~48):
// ❌ background: 'var(--color-surface-2)'  → 미정의 변수
// ✅ background: 'var(--color-bg-hover)'

// CounterSection 내 badge (line ~135):
// ❌ background: 'var(--color-surface-2)'
// ✅ background: 'var(--color-bg-hover)'
```

### 기존 탭 구조 처리

현재 탭(카운터픽/밴추천/팀구성) 중 PlayerCard 그리드가 "팀구성" 탭을 대체한다. 카운터픽/밴추천 탭은 그리드 아래에 유지하거나 제거 가능. **최소 구현**: PlayerCard 그리드를 메인으로, 기존 CounterSection/BanRecommendSection 탭은 아래에 유지.

### CSS 변수 참조

```css
--color-info: #3B9EFF          /* 우리팀 헤더 색상 */
--color-error: #E84040         /* 상대팀 헤더 색상 */
--color-bg-hover: #152035      /* 빈 아이콘 placeholder */
--color-bg-card: #0D1B2E
--color-border: #1E2D40
--color-text-secondary: #A0A8B0
--color-text-disabled: #5B5A56
--spacing-sm: 8px
--spacing-md: 16px
--radius-md: 6px
```

### 코딩 컨벤션 (엄수)

- **Named export**: `export function ChampSelectPage` (유지)
- **CSS 변수**: 하드코딩 금지. `rgba()` 반투명값만 예외
- **lcu.ts 변경 없음**: LCU 로직은 main process 전용
- **TypeScript strict**: any 사용 금지, 타입 명시
- **PlayerData import**: `ChampSelectPage` 내 기존 `PlayerDetail` 인터페이스 대신 `PlayerCard.tsx`의 `PlayerData` 재사용 (동일 구조)
- **window.lol API**: `getChampSelectFull()` 만 사용 (기존 패턴 유지)

### 4-1 / 4-2 스토리 학습 사항

- **CDN 패턴**: `https://raw.communitydragon.org/latest/...` (ChampSelectPage 내 기존 `CDN` 상수 재사용)
- **ChampIcon**: 각 컴포넌트 내 인라인 구현 — 공유 유틸 없음. ChampSelectPage의 `ChampIcon`은 그대로 유지
- **BanRecommendBadge**: `key={c.championId}` 사용 (champion 이름 중복 가능성)
- **`var(--color-surface-2)` 존재하지 않음**: `var(--color-bg-hover)` 로 교체 필수
- **Promise.all 패턴**: 여러 플레이어 데이터 병렬 fetch (BanRecommendSection 패턴 동일하게 적용)

### 파일 위치 규칙

```
electron-collector/src/renderer/src/
  components/
    lobby/
      PlayerCard.tsx         ← 재사용 (변경 없음)
      BanRecommendBadge.tsx  ← 재사용 (변경 없음)
  pages/
    ChampSelectPage.tsx      ← [수정] 이번 스토리 핵심 대상
  main/
    lcu.ts                   ← 변경 없음
```

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

- `ChampSelectPage.tsx` 전면 재설계: grid-cols-2 PlayerCard 그리드 메인 레이아웃으로 교체
- `PlayerCard` import 추가 (`../components/lobby/PlayerCard`), `PlayerData` 타입 재사용으로 `PlayerDetail` 중복 제거
- 양팀 플레이어 riotId 기준 API 병렬 fetch → `playerDetails` 상태 관리 → PlayerCard에 전달
- riotId 있고 데이터 미도착 시 `loading={true}` → Skeleton 표시 (AC #4)
- `lcuError` state 추가: throw → "LoL 클라이언트를 실행해주세요", null → "챔피언 선택 화면이 아닙니다" 분기 (AC #3)
- 자동 새로고침 `setInterval` 10_000 → 2_000 ms (AC #1)
- 기존 버그 2곳 수정: `var(--color-surface-2)` → `var(--color-bg-hover)` (ChampIcon fallback, CounterSection badge)
- 기존 탭: 팀구성 탭 제거(PlayerCard 그리드로 대체), 밴추천/카운터픽 탭 그리드 아래 유지
- `BanRecommendSection` 내 `PlayerDetail` → `PlayerData` 타입으로 통일
- `BanRecommendBadge` key: `key={c.championId}` 유지 (4-2 패치 적용 상태)
- lcu.ts 변경 없음 (AC #5)
- TypeScript `npx tsc --noEmit` 오류 없음

### File List

- electron-collector/src/renderer/src/pages/ChampSelectPage.tsx (수정)

### Code Review Patches Applied

- [P-1] Race condition 수정: `cancelled` 플래그로 stale write 방지 (`playerDetails` useEffect)
- [P-2] 영구 Skeleton 수정: `fetchedRiotIds` Set 추가 — API 실패 후 loading 상태 해소
- [P-3] Double-fetch 제거: `BanRecommendSection` 자체 fetch 제거, `playerDetails` prop 주입
- [P-4] `isHighThreat={i === 0}` 인덱스 비교로 교체 (참조 동일성 대신)
- [P-5] `Promise.all` `.catch(() => {})` 추가
