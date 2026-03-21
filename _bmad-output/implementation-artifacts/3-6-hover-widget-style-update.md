# Story 3.6: PlayerLink/ChampionLink Hover 위젯 스타일 업데이트

Status: done

## Story

As a 유저,
I want 플레이어/챔피언 이름에 마우스를 올리면 핵심 통계가 즉시 표시되기를,
So that 상세 페이지 이동 없이 컨텍스트를 유지하며 빠르게 통계를 확인할 수 있다. (FR8, FR9)

## Acceptance Criteria

1. **Given** 유저가 플레이어 이름 위에 마우스를 올렸을 때
   **When** 220ms 딜레이 후
   **Then** 300ms 이내 PlayerLink Popover가 표시된다 (NFR1) — **이미 구현됨, 검증만**

2. **Given** PlayerLink Popover가 표시될 때
   **When** 내용을 확인하면
   **Then** 승률, KDA, 주력 챔피언, **Elo** 등 핵심 수치가 `font-mono`로 표시된다

3. **Given** PlayerLink 스타일이 업데이트될 때
   **When** 기존 인터페이스를 확인하면
   **Then** `riotId`, `children`, `className`, `mode` props가 그대로 유지된다 — **이미 구현됨, 검증만**

4. **Given** 동일 플레이어를 두 번 hover할 때
   **When** 두 번째 hover가 발생하면
   **Then** 모듈 레벨 Map 캐시에서 즉시 표시된다 (추가 API 호출 없음) — **이미 구현됨, 검증만**

5. **Given** ChampionLink가 챔피언 이름에 적용될 때
   **When** 마우스를 올리면
   **Then** 동일한 타이밍과 패턴으로 챔피언 통계 Popover가 표시된다 (FR9) — **이미 구현됨, 검증만**

## Tasks / Subtasks

- [x] Task 1: `popup.css` — 수치 클래스에 `font-mono` 적용 (AC: #2)
  - [x] `.popup-stat-value`에 `font-family: var(--font-family-mono)` 추가
  - [x] `.popup-wr-value`에 `font-family: var(--font-family-mono)` 추가
  - [x] `.popup-master-wr`에 `font-family: var(--font-family-mono)` 추가
  - [x] `.popup-champ-wr`에 `font-family: var(--font-family-mono)` 추가
  - [x] `.popup-wl`에 `font-family: var(--font-family-mono)` 추가

- [x] Task 2: `PlayerLink.tsx` — PlayerPopupContent에 Elo 수치 추가 (AC: #2)
  - [x] `popup-stats-row` 내에 Elo 항목 추가: `data.elo` (정수) + label `"Elo"`
  - [x] Elo 색상: `data.elo >= 1200 ? 'var(--color-win)' : data.elo >= 1000 ? 'var(--color-primary)' : 'var(--color-loss)'`
  - [x] `data.elo`가 `PlayerDetailStats`에 있음 — 별도 타입 수정 불필요

- [x] Task 3: TypeScript 검증
  - [x] `npx tsc --noEmit` 오류 없이 통과 확인

## Dev Notes

### 🚨 CRITICAL: 기능은 이미 완전 구현됨 — 절대 재구현 금지

**PlayerLink.tsx와 ChampionLink.tsx는 이미 완전 동작 상태**:
- 220ms 딜레이 후 팝업 표시 ✅
- 모듈 레벨 캐시 (`Map<string, PlayerDetailStats>()`) ✅
- 180ms 숨김 딜레이 ✅
- 팝업 위치 자동 조정 (flipY, 화면 경계 체크) ✅
- Portal로 `document.body`에 렌더링 ✅
- `popup-trigger` 클래스 hover 스타일 ✅

**이 스토리에서 실제로 추가할 것:**
1. popup.css 수치 클래스에 font-family 1줄씩 추가
2. PlayerPopupContent의 popup-stats-row에 Elo 항목 추가

### Task 1: font-mono 적용 위치 상세

`frontend/src/styles/components/popup.css`에서 수정할 클래스:

```css
/* 현재 */
.popup-stat-value {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-text-primary);
}

/* 수정 후 */
.popup-stat-value {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-text-primary);
  font-family: var(--font-family-mono);
}
```

동일 패턴으로 수정할 클래스들:
- `.popup-wr-value` (승률 %)
- `.popup-master-wr` (장인 랭킹 승률)
- `.popup-champ-wr` (주요 챔피언 승률)
- `.popup-wl` (승/패 수치 W/L)

**참조**: `global.css:98` — `--font-family-mono: 'JetBrains Mono', 'Courier New', monospace`

### Task 2: PlayerPopupContent Elo 추가

`frontend/src/components/common/PlayerLink.tsx`의 `PlayerPopupContent` 내 `popup-stats-row`:

```tsx
/* 현재 popup-stats-row (6개 stat: KDA, 킬, 데스, 딜량, CS) */
<div className="popup-stats-row">
  <div className="popup-stat">
    <div className="popup-stat-value" style={{ color: kdaColor }}>{data.kda.toFixed(2)}</div>
    <div className="popup-stat-label">KDA</div>
  </div>
  {/* ... 킬, 데스, 딜량, CS */}
</div>

/* 수정 후: Elo 항목 추가 */
// 상단에 eloColor 계산 추가:
const eloColor = data.elo >= 1200 ? 'var(--color-win)' : data.elo >= 1000 ? 'var(--color-primary)' : 'var(--color-loss)';

// popup-stats-row에 항목 추가:
<div className="popup-stat">
  <div className="popup-stat-value" style={{ color: eloColor }}>{data.elo}</div>
  <div className="popup-stat-label">Elo</div>
</div>
```

`PlayerDetailStats` 타입 (`frontend/src/lib/types/stats.ts:91`):
```ts
elo: number;
eloRank: number | null;
```
둘 다 이미 있음 — 타입 수정 불필요.

### popup-stats-row 레이아웃 고려

현재 `popup-stats-row`에 5개 stat (KDA, 킬, 데스, 딜량, CS).
Elo 추가 시 6개. `flex: 1`로 균등 분배되므로 각 항목이 조금 좁아지지만 수용 가능.
`popup-panel` 너비는 280px (PlayerLink:144).

### 파일 위치 규칙

```
frontend/src/
  components/common/
    PlayerLink.tsx          ← 수정 (Task 2)
  styles/components/
    popup.css               ← 수정 (Task 1)
```

**수정 불필요:**
- `ChampionLink.tsx` — ChampionPopupContent는 Elo 없음 (챔피언 팝업은 장인/아이템 정보), AC에도 언급 없음
- `App.tsx`, `MatchesPage.tsx` — 변경 없음
- `global.css` — `font-mono` 유틸 이미 존재, CSS 변수 이미 정의됨

### 코딩 컨벤션 (엄수)

- **CSS 변수**: `var(--font-family-mono)` — 하드코딩 금지 (`'JetBrains Mono'` 직접 쓰기 금지)
- **Named export**: 기존 `export function PlayerLink` 유지
- **import type**: 타입 전용 import 유지

### References

- `PlayerDetailStats` 타입: `frontend/src/lib/types/stats.ts:77-96`
- `popup.css`: `frontend/src/styles/components/popup.css`
- `global.css` 디자인 토큰: `frontend/src/styles/global.css:97-99` (`--font-family-mono`)
- `PlayerLink.tsx`: `frontend/src/components/common/PlayerLink.tsx`
- `ChampionLink.tsx`: `frontend/src/components/common/ChampionLink.tsx` (참조용, 수정 불필요)

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

- `popup.css`: `.popup-stat-value`, `.popup-wr-value`, `.popup-wl`, `.popup-master-wr`, `.popup-champ-wr` 5개 클래스에 `font-family: var(--font-family-mono)` 추가 — JetBrains Mono 수치 표시
- `PlayerLink.tsx`: `PlayerPopupContent` 내 `eloColor` 계산 추가(1200 이상 win, 1000 이상 primary, 미만 loss); `popup-stats-row`에 Elo 항목(6번째) 추가 — `data.elo` 표시
- TypeScript 오류 없음 (`npx tsc --noEmit` 통과)
- 코드 리뷰 P1 수정: `eloVal = Number.isFinite(data.elo) ? data.elo : null` guard 추가; 렌더링 `Math.round(eloVal) : '-'`로 NaN/undefined/Infinity/float 방어

### File List

- frontend/src/styles/components/popup.css (수정)
- frontend/src/components/common/PlayerLink.tsx (수정)
