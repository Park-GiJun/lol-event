# Story 4.1: PlayerCard 컴포넌트 구현

Status: done

## Story

As a 유저,
I want 대기방에서 각 플레이어의 정보를 카드 형태로 확인하기를,
So that 닉네임, Elo, 고승률 챔피언 TOP 3를 한눈에 스캔할 수 있다. (FR17, FR18)

## Acceptance Criteria

1. **Given** PlayerCard 컴포넌트가 렌더링될 때
   **When** 카드 내용을 확인하면
   **Then** 닉네임, Elo 수치(`font-mono`), 고승률 챔피언 TOP 3(챔피언 아이콘 + 승률)가 표시된다

2. **Given** PlayerCard가 로딩 중일 때
   **When** 데이터가 아직 없으면
   **Then** Skeleton 상태로 카드 레이아웃이 유지된다

3. **Given** PlayerCard가 expanded 상태일 때
   **When** 카드를 클릭하면
   **Then** 챔피언 목록이 TOP 3에서 더 많이 확장된다

4. **Given** PlayerCard에 접근성이 적용되었을 때
   **When** 구조를 확인하면
   **Then** `role="article"`, 닉네임에 `aria-label`이 설정된다

## Tasks / Subtasks

- [x] Task 1: `PlayerCard.tsx` 신규 생성 (AC: #1, #2, #3, #4)
  - [x] `electron-collector/src/renderer/src/components/lobby/` 폴더 신규 생성
  - [x] `PlayerCard.tsx` 신규 생성
  - [x] `PlayerData` 인터페이스 정의 (riotId, elo, championStats)
  - [x] `PlayerCardProps` 정의: `{ riotId: string; data: PlayerData | null; loading?: boolean }`
  - [x] loading 상태: Skeleton 렌더링
  - [x] default 상태: 닉네임 + Elo + TOP 3 챔피언
  - [x] expanded 상태: 클릭 시 전체 챔피언 목록 확장
  - [x] `role="article"`, 닉네임 `aria-label` 적용
  - [x] Named export: `export function PlayerCard`

- [x] Task 2: 타입 확장 — `PlayerDetail`에 `elo` 필드 추가 (AC: #1)
  - [x] `ChampSelectPage.tsx`의 `PlayerDetail` 인터페이스에 `elo?: number` 추가
  - [x] `BanRecommendSection`에서 이미 `fetch`로 가져오는 응답에 elo가 포함되므로 타입만 업데이트

- [x] Task 3: `ChampIcon` 유틸 추출 (옵션 — 기존 ChampSelectPage의 `ChampIcon` 재사용)
  - [x] `ChampSelectPage.tsx`에 이미 `ChampIcon` 함수가 있음 — `PlayerCard`에서 직접 구현하거나 별도 파일로 추출
  - [x] 별도 추출 없이 `PlayerCard.tsx` 내부에 동일 패턴으로 인라인 구현

- [x] Task 4: TypeScript 검증
  - [x] `electron-collector`에서 TypeScript 오류 없음 확인

## Dev Notes

### 프로젝트 구조 — Electron Renderer

```
electron-collector/src/renderer/src/
  components/
    layout/           ← 기존 (Layout.tsx, Sidebar.tsx, Titlebar.tsx)
    lobby/            ← [신규 폴더] Story 4.1에서 생성
      PlayerCard.tsx  ← [신규] 이번 스토리 대상
  pages/
    ChampSelectPage.tsx   ← 기존 (Task 2에서 타입만 수정)
    CollectPage.tsx       ← 유지
    CustomGamePage.tsx    ← 유지
    SummonerPage.tsx      ← 유지
  styles/
    global.css        ← CSS 변수 정의 (아래 참고)
    components.css    ← .card, .btn 클래스
    layout.css        ← 레이아웃 스타일
```

### CSS 변수 — Electron Renderer (frontend와 다름!)

```css
--color-primary: #C89B3C        /* 금색 (대시보드의 #00B4D8과 다름!) */
--color-primary-light: #D4AF5A
--color-win: #0BC4B4             /* 틸 (대시보드의 #10B981과 다름!) */
--color-loss: #E84040
--color-info: #3B9EFF
--color-warning: #FFD166
--color-bg-card: #0D1B2E
--color-bg-hover: #152035
--color-text-primary: #F0E6D3
--color-text-secondary: #A0A8B0
--color-text-disabled: #5B5A56
--color-border: #1E2D40
--font-size-xs: 11px
--font-size-sm: 13px
--font-size-md: 15px
--font-size-lg: 18px
--spacing-xs: 4px
--spacing-sm: 8px
--spacing-md: 16px
--spacing-lg: 24px
--radius-sm: 4px
--radius-md: 6px
--radius-lg: 10px
```

**주의**: `--font-family-mono` 토큰이 없음 → Elo 수치 font-mono는 인라인 `fontFamily: "'Consolas', 'D2Coding', monospace"` 사용

### API 패턴 — Electron Renderer

**중요**: Electron renderer는 `frontend/`와 별도 패키지 — `lib/api/api.ts` 미공유
→ 직접 `fetch()` 사용

```typescript
const API = 'https://api.gijun.net/api';

// 플레이어 상세 통계 (Elo 포함)
const res = await fetch(`${API}/stats/player/${encodeURIComponent(riotId)}?mode=all`);
const json = await res.json() as { data: PlayerDetail };
```

API 응답 구조 (`PlayerDetail`):
```typescript
interface PlayerChampStat {
  champion: string;     // 영문 챔피언명
  championId: number;
  games: number;
  wins: number;
  winRate: number;      // 0~100
}

interface PlayerDetail {
  riotId: string;
  games: number;
  wins: number;
  winRate: number;
  elo?: number;          // ← Task 2에서 추가할 필드
  championStats: PlayerChampStat[];
}
```

### 챔피언 아이콘 URL

```typescript
const CDN = 'https://raw.communitydragon.org/latest/plugins/rcp-be-lol-game-data/global/default/v1/champion-icons';

function champIconUrl(id: number) {
  return `${CDN}/${id}.png`;
}
```

`ChampSelectPage.tsx`에 동일 패턴 존재 — PlayerCard 내부에서 동일하게 구현.

### Task 1: PlayerCard 전체 구현

```tsx
// electron-collector/src/renderer/src/components/lobby/PlayerCard.tsx

interface PlayerChampStat {
  champion: string;
  championId: number;
  games: number;
  wins: number;
  winRate: number;
}

interface PlayerData {
  riotId: string;
  games: number;
  wins: number;
  winRate: number;
  elo?: number;
  championStats: PlayerChampStat[];
}

interface PlayerCardProps {
  riotId: string;
  data: PlayerData | null;
  loading?: boolean;
}

const CDN = 'https://raw.communitydragon.org/latest/plugins/rcp-be-lol-game-data/global/default/v1/champion-icons';

function champIconUrl(id: number) {
  return `${CDN}/${id}.png`;
}

function ChampIcon({ id, size = 26 }: { id: number; size?: number }) {
  if (!id) return <div style={{ width: size, height: size, borderRadius: 4, background: 'var(--color-bg-hover)' }} />;
  return (
    <img
      src={champIconUrl(id)}
      alt=""
      width={size} height={size}
      style={{ borderRadius: 4, objectFit: 'cover' }}
      onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }}
    />
  );
}

export function PlayerCard({ riotId, data, loading = false }: PlayerCardProps) {
  const [expanded, setExpanded] = useState(false);

  // 로딩 상태: Skeleton 레이아웃 유지
  if (loading || data === null) {
    return (
      <article
        role="article"
        aria-label={`플레이어 ${riotId} 로딩 중`}
        style={{
          background: 'var(--color-bg-card)',
          border: '1px solid var(--color-border)',
          borderRadius: 'var(--radius-md)',
          padding: 'var(--spacing-sm)',
        }}
      >
        {/* Skeleton 닉네임 */}
        <div style={{ height: 14, width: '70%', borderRadius: 3, background: 'var(--color-border)', marginBottom: 6 }} />
        {/* Skeleton Elo */}
        <div style={{ height: 12, width: '40%', borderRadius: 3, background: 'var(--color-border)', marginBottom: 10 }} />
        {/* Skeleton 챔피언 3개 */}
        <div style={{ display: 'flex', gap: 6 }}>
          {[0, 1, 2].map(i => (
            <div key={i} style={{ width: 26, height: 26, borderRadius: 4, background: 'var(--color-border)' }} />
          ))}
        </div>
      </article>
    );
  }

  const displayName = riotId.split('#')[0];
  const eloVal = Number.isFinite(data.elo) ? Math.round(data.elo!) : null;
  const eloColor = (eloVal ?? 0) >= 1200 ? 'var(--color-win)' : (eloVal ?? 0) >= 1000 ? 'var(--color-primary)' : 'var(--color-loss)';

  const showCount = expanded ? data.championStats.length : 3;
  const visibleChamps = data.championStats.slice(0, showCount);

  return (
    <article
      role="article"
      aria-label={`플레이어 ${displayName}`}
      style={{
        background: 'var(--color-bg-card)',
        border: '1px solid var(--color-border)',
        borderRadius: 'var(--radius-md)',
        padding: 'var(--spacing-sm)',
        cursor: data.championStats.length > 3 ? 'pointer' : 'default',
      }}
      onClick={() => { if (data.championStats.length > 3) setExpanded(e => !e); }}
    >
      {/* 닉네임 */}
      <div style={{ fontSize: 'var(--font-size-sm)', fontWeight: 600, color: 'var(--color-text-primary)', marginBottom: 2 }}
        aria-label={`닉네임: ${displayName}`}>
        {displayName}
      </div>

      {/* Elo */}
      <div style={{
        fontSize: 'var(--font-size-xs)',
        fontFamily: "'Consolas', 'D2Coding', monospace",
        color: eloColor,
        marginBottom: 8,
      }}>
        {eloVal !== null ? `Elo ${eloVal}` : 'Elo —'}
      </div>

      {/* 챔피언 아이콘 + 승률 */}
      <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
        {visibleChamps.map(c => (
          <div key={c.championId} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2 }}>
            <ChampIcon id={c.championId} size={26} />
            <span style={{
              fontSize: 10,
              color: c.winRate >= 60 ? 'var(--color-loss)' : c.winRate >= 50 ? 'var(--color-win)' : 'var(--color-text-secondary)',
              fontFamily: "'Consolas', 'D2Coding', monospace",
            }}>
              {c.winRate}%
            </span>
          </div>
        ))}
        {data.championStats.length > 3 && (
          <div style={{
            fontSize: 10, color: 'var(--color-text-secondary)',
            alignSelf: 'center', userSelect: 'none',
          }}>
            {expanded ? '▲' : `+${data.championStats.length - 3}`}
          </div>
        )}
      </div>
    </article>
  );
}
```

### Task 2: PlayerDetail 타입 확장

`ChampSelectPage.tsx:29`에 있는 `PlayerDetail` 인터페이스에 `elo?: number` 추가:

```typescript
// 수정 전
interface PlayerDetail { riotId: string; games: number; wins: number; winRate: number; championStats: PlayerChampStat[]; }

// 수정 후
interface PlayerDetail { riotId: string; games: number; wins: number; winRate: number; elo?: number; championStats: PlayerChampStat[]; }
```

### 코딩 컨벤션 (엄수)

- **Named export**: `export function PlayerCard` (default export 금지)
- **CSS 변수**: 하드코딩 금지, `var(--color-*)` 사용
- **font-mono**: `--font-family-mono` 없음 → `fontFamily: "'Consolas', 'D2Coding', monospace"` 인라인
- **API 직접 fetch**: Electron renderer는 `lib/api/api.ts` 없음 → `fetch()` 직접 사용
- **Elo guard**: `Number.isFinite()` 체크 후 `Math.round()` — undefined/NaN/Infinity 방어
- **`import { useState } from 'react'`** — 클릭 expanded 상태 관리 필요
- **lcu.ts 변경 없음**: 이번 스토리는 UI 컴포넌트만 — LCU 로직 무변경

### 파일 위치 규칙

```
electron-collector/src/renderer/src/
  components/
    lobby/                         ← [신규 폴더]
      PlayerCard.tsx               ← [신규] 이번 스토리
  pages/
    ChampSelectPage.tsx            ← [수정] Task 2: PlayerDetail 타입만
```

### References

- `ChampSelectPage.tsx`: `electron-collector/src/renderer/src/pages/ChampSelectPage.tsx`
  - 기존 `ChampIcon`, `PlayerDetail`, `winRateColor`, `positionLabel` 패턴 참고
  - `PlayerDetail` 인터페이스 (line 29): `elo?: number` 추가
- `global.css`: `electron-collector/src/renderer/src/styles/global.css` — CSS 변수
- `components.css`: `.card`, `.btn` 클래스 (PlayerCard는 카드 클래스 사용 안 하고 인라인 스타일 사용)

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

- `PlayerCard.tsx` 신규 생성: `components/lobby/` 폴더 신규 생성, 3상태(loading/default/expanded) 구현
- loading: 닉네임·Elo·챔피언 3개 Skeleton 블록으로 레이아웃 유지
- default: 닉네임(font-sm bold) + Elo(font-mono, 색상 연동) + TOP 3 챔피언 아이콘+승률
- expanded: 클릭 시 전체 championStats 확장, `▲` / `+N` 표시
- `role="article"`, 닉네임 div에 `aria-label` 적용
- `Number.isFinite(data.elo)` guard + `Math.round()` 적용 (undefined/NaN 방어)
- `ChampSelectPage.tsx`: `PlayerDetail` 인터페이스에 `elo?: number` 추가
- TypeScript 오류 없음 (`npx tsc --noEmit` 통과)

### File List

- electron-collector/src/renderer/src/components/lobby/PlayerCard.tsx (신규)
- electron-collector/src/renderer/src/pages/ChampSelectPage.tsx (수정)
