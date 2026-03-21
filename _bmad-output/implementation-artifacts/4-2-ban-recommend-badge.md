# Story 4.2: BanRecommendBadge 컴포넌트 구현

Status: done

## Story

As a 유저,
I want 각 플레이어 카드에서 밴 추천 챔피언을 뱃지로 확인하기를,
So that 어떤 챔피언을 밴해야 할지 직관적으로 판단할 수 있다. (FR19)

## Acceptance Criteria

1. **Given** BanRecommendBadge 컴포넌트가 렌더링될 때
   **When** 뱃지 내용을 확인하면
   **Then** 챔피언 아이콘 + 챔피언명 + 승률/판수가 표시된다

2. **Given** 높은 위협도(상위 밴 추천, `isHighThreat: true`)인 뱃지일 때
   **When** 뱃지 스타일을 확인하면
   **Then** `var(--color-error)` (#E84040) 기반 반투명 배경 + 별 아이콘이 적용된다

3. **Given** 일반 위협도(`isHighThreat: false`)인 뱃지일 때
   **When** 뱃지 스타일을 확인하면
   **Then** `var(--color-bg-hover)` 배경 + `var(--color-border)` 테두리가 적용된다

4. **Given** `<span>` 인라인 스타일로 구현되었을 때
   **When** 컴포넌트 구조를 확인하면
   **Then** 위협도에 따라 색상이 분기된 뱃지 형태다

5. **Given** BanRecommendSection에서 BanRecommendBadge가 사용될 때
   **When** ChampSelectPage를 확인하면
   **Then** 기존 인라인 badge 렌더링이 BanRecommendBadge로 교체된다

## Tasks / Subtasks

- [x] Task 1: `BanRecommendBadge.tsx` 신규 생성 (AC: #1, #2, #3, #4)
  - [x] `electron-collector/src/renderer/src/components/lobby/BanRecommendBadge.tsx` 신규 생성
  - [x] `BanRecommendBadgeProps` 인터페이스 정의
  - [x] `isHighThreat: true` → `var(--color-error)` 기반 스타일 + Star 아이콘
  - [x] `isHighThreat: false` → `var(--color-bg-hover)` 기반 스타일
  - [x] ChampIcon 패턴 동일하게 인라인 구현 (PlayerCard.tsx 패턴 참고)
  - [x] Named export: `export function BanRecommendBadge`

- [x] Task 2: `ChampSelectPage.tsx` — BanRecommendSection 리팩터 (AC: #5)
  - [x] `BanRecommendBadge` import 추가
  - [x] BanRecommendSection 내 `top3.map` 의 인라인 badge div → `<BanRecommendBadge>` 교체
  - [x] 기존 인라인 스타일 블록 제거

- [x] Task 3: TypeScript 검증
  - [x] `electron-collector`에서 `npx tsc --noEmit` 오류 없음 확인

## Dev Notes

### 프로젝트 구조

```
electron-collector/src/renderer/src/
  components/
    lobby/
      PlayerCard.tsx       ← 기존 (4-1에서 신규 생성)
      BanRecommendBadge.tsx ← [신규] 이번 스토리 대상
  pages/
    ChampSelectPage.tsx    ← [수정] BanRecommendSection 리팩터
```

### CSS 변수 — Electron Renderer

```css
--color-error: #E84040        /* 빨강 (고위협 배경 기반) */
--color-bg-hover: #152035     /* 일반 배경 */
--color-bg-card: #0D1B2E
--color-border: #1E2D40
--color-warning: #FFD166      /* Star 아이콘 색상 */
--color-text-primary: #F0E6D3
--color-text-secondary: #A0A8B0
--font-size-xs: 11px
--font-size-sm: 13px
--spacing-xs: 4px
--spacing-sm: 8px
--radius-sm: 4px
```

**주의**: 에픽 명세의 `#EF4444`는 CSS 변수 `--color-error: #E84040`으로 대체. `#00B4D8` 일반색은 Electron에 없으므로 일반 배경은 `var(--color-bg-hover)`로 구현.

### BanRecommendBadgeProps 정의

```typescript
export interface BanRecommendBadgeProps {
  champion: string;       // 챔피언 영문명 (표시용)
  championId: number;     // 아이콘 URL 생성용
  isHighThreat: boolean;  // true = --color-error 기반 / false = bg-hover
  winRate?: number;       // 0~100, 있으면 표시
  games?: number;         // 있으면 표시
}
```

### Task 1: BanRecommendBadge 전체 구현

```tsx
// electron-collector/src/renderer/src/components/lobby/BanRecommendBadge.tsx

import { Star } from 'lucide-react';

const CDN = 'https://raw.communitydragon.org/latest/plugins/rcp-be-lol-game-data/global/default/v1/champion-icons';

function champIconUrl(id: number) {
  return `${CDN}/${id}.png`;
}

function ChampIcon({ id, size = 22 }: { id: number; size?: number }) {
  if (!id) return <div style={{ width: size, height: size, borderRadius: 4, background: 'var(--color-bg-hover)' }} />;
  return (
    <img
      src={champIconUrl(id)}
      alt=""
      width={size} height={size}
      style={{ borderRadius: 4, objectFit: 'cover', flexShrink: 0 }}
      onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }}
    />
  );
}

export interface BanRecommendBadgeProps {
  champion: string;
  championId: number;
  isHighThreat: boolean;
  winRate?: number;
  games?: number;
}

export function BanRecommendBadge({
  champion,
  championId,
  isHighThreat,
  winRate,
  games,
}: BanRecommendBadgeProps) {
  const bg = isHighThreat ? 'rgba(232, 64, 64, 0.1)' : 'var(--color-bg-hover)';
  const border = isHighThreat ? '1px solid rgba(232, 64, 64, 0.3)' : '1px solid var(--color-border)';

  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 6,
      padding: '4px 8px', borderRadius: 'var(--radius-sm)',
      background: bg, border,
    }}>
      <ChampIcon id={championId} size={22} />
      <div>
        <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-primary)' }}>
          {champion}
        </div>
        {(winRate !== undefined || games !== undefined) && (
          <div style={{
            fontSize: 10,
            color: winRate !== undefined && winRate >= 60
              ? 'var(--color-error)'
              : winRate !== undefined && winRate >= 50
                ? 'var(--color-win)'
                : 'var(--color-text-secondary)',
            fontFamily: "'Consolas', 'D2Coding', monospace",
          }}>
            {winRate !== undefined ? `${winRate}%` : ''}
            {winRate !== undefined && games !== undefined ? ' ' : ''}
            {games !== undefined ? `(${games}판)` : ''}
          </div>
        )}
      </div>
      {isHighThreat && (
        <Star size={10} style={{ color: 'var(--color-warning)', flexShrink: 0 }} />
      )}
    </div>
  );
}
```

### Task 2: ChampSelectPage.tsx — BanRecommendSection 수정

**현재 코드 (BanRecommendSection 내부, top3.map 부분):**

```tsx
{top3.map((c, idx) => (
  <div key={c.champion} style={{
    display: 'flex', alignItems: 'center', gap: 6,
    padding: '4px 8px', borderRadius: 'var(--radius-sm)',
    background: idx === 0 ? 'rgba(232,64,64,0.1)' : 'var(--color-surface-2)',
    border: `1px solid ${idx === 0 ? 'rgba(232,64,64,0.3)' : 'var(--color-border)'}`,
  }}>
    <ChampIcon id={c.championId} size={22} />
    <div>
      <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-primary)' }}>{c.champion}</div>
      <div style={{ fontSize: 10, color: winRateColor(c.winRate) }}>{c.winRate}% ({c.games}판)</div>
    </div>
    {idx === 0 && <Star size={10} style={{ color: 'var(--color-warning)' }} />}
  </div>
))}
```

**변경 후:**

```tsx
import { BanRecommendBadge } from '../components/lobby/BanRecommendBadge';

// top3.map 교체:
{top3.map((c, idx) => (
  <BanRecommendBadge
    key={c.champion}
    champion={c.champion}
    championId={c.championId}
    isHighThreat={idx === 0}
    winRate={c.winRate}
    games={c.games}
  />
))}
```

**주의**: `BanRecommendBadge` 내부에서 `ChampIcon`을 자체 구현하므로 `ChampSelectPage.tsx`의 `ChampIcon`은 삭제하지 않는다 (BanRecommendSection 외 다른 곳에서 사용 중임을 확인 후 판단).

### 코딩 컨벤션 (엄수)

- **Named export**: `export function BanRecommendBadge` (default export 금지)
- **CSS 변수**: 하드코딩 금지. `rgba(232,64,64,0.1)` 형태의 반투명값은 예외적으로 허용 (CSS 변수로 표현 불가)
- **`--font-family-mono` 없음** → `fontFamily: "'Consolas', 'D2Coding', monospace"` 인라인
- **lucide-react**: `Star` 아이콘 사용. `import { Star } from 'lucide-react'`
- **CDN 패턴**: `PlayerCard.tsx`와 동일한 CommunityDragon CDN URL 사용
- **lcu.ts 변경 없음**: UI 컴포넌트만 — LCU 로직 무변경
- **API 직접 fetch**: 이 컴포넌트는 fetch 없음 (props로만 데이터 수신)

### 파일 위치 규칙

```
electron-collector/src/renderer/src/
  components/
    lobby/
      PlayerCard.tsx               ← 기존 (4-1, 변경 없음)
      BanRecommendBadge.tsx        ← [신규] 이번 스토리
  pages/
    ChampSelectPage.tsx            ← [수정] BanRecommendSection 리팩터
```

### Import 경로 주의

`ChampSelectPage.tsx` 기준 상대 경로:
```typescript
import { BanRecommendBadge } from '../components/lobby/BanRecommendBadge';
```

### 4-1 스토리 학습 사항 (반드시 적용)

- **CDN 패턴**: `https://raw.communitydragon.org/latest/...` (4-1과 동일 — 복사하지 말고 동일 패턴 사용)
- **ChampIcon**: 각 컴포넌트에 인라인 구현 (공유 유틸 없음, 4-1 패턴 그대로)
- **`Number.isFinite()`**: 수치 guard — `winRate`가 `undefined`일 수 있으므로 옵셔널 체이닝 처리
- **빈 riotId 방어**: 이 컴포넌트는 props 수신이므로 해당 없음
- **TypeScript strict**: export 인터페이스 필수, any 사용 금지

### References

- `PlayerCard.tsx`: `electron-collector/src/renderer/src/components/lobby/PlayerCard.tsx`
  - ChampIcon 인라인 구현 패턴, CDN 상수, CSS 변수 사용 방식 참고
- `ChampSelectPage.tsx`: `electron-collector/src/renderer/src/pages/ChampSelectPage.tsx`
  - BanRecommendSection (line ~170~230): 현재 인라인 badge 렌더링 코드
  - `winRateColor` 함수 — BanRecommendBadge에서 승률 색상 분기 시 참고
  - `Star` 아이콘 import 이미 있음 — 중복 import 방지
- `global.css`: `electron-collector/src/renderer/src/styles/global.css` — CSS 변수

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

- `BanRecommendBadge.tsx` 신규 생성: `components/lobby/` 폴더에 PlayerCard와 동일 패턴으로 구현
- `isHighThreat: true` → `rgba(232,64,64,0.1)` 배경 + `rgba(232,64,64,0.3)` 테두리 + Star(10px) 아이콘
- `isHighThreat: false` → `var(--color-bg-hover)` 배경 + `var(--color-border)` 테두리
- 승률 색상: 60%↑ `var(--color-error)`, 50%↑ `var(--color-win)`, 나머지 `var(--color-text-secondary)`
- ChampIcon 인라인 구현 (PlayerCard.tsx와 동일 CDN 패턴)
- `ChampSelectPage.tsx`: `Star` import 제거, `BanRecommendBadge` import 추가
- BanRecommendSection `top3.map` 인라인 div → `<BanRecommendBadge>` 교체 (인라인 스타일 4줄 제거)
- 기존 `var(--color-surface-2)` (존재하지 않는 CSS 변수) → `var(--color-bg-hover)` 로 암묵적 수정
- TypeScript 오류 없음 (`npx tsc --noEmit` 통과)

### File List

- electron-collector/src/renderer/src/components/lobby/BanRecommendBadge.tsx (신규)
- electron-collector/src/renderer/src/pages/ChampSelectPage.tsx (수정)
