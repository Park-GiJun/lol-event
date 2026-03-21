# Story 5.1: MobileLayout & MobileBottomNav 컴포넌트

Status: done

## Story

As a 모바일 유저,
I want `/m/*` 경로에서 하단 탭바가 있는 모바일 전용 레이아웃을 사용하기를,
So that 엄지로 쉽게 주요 화면 간 이동할 수 있다. (FR21, FR22)

## Acceptance Criteria

1. **Given** 유저가 모바일에서 `/m`으로 접근했을 때
   **When** MobileLayout이 렌더링되면
   **Then** 상단 콘텐츠 영역과 하단 고정 탭바(`fixed bottom-0`)가 표시된다

2. **Given** MobileBottomNav 탭바가 표시될 때
   **When** 탭 항목들을 확인하면
   **Then** 홈(리더보드), 챔피언, 플레이어 탭이 각각 Lucide 아이콘 + 텍스트로 표시된다

3. **Given** 현재 활성 탭을 확인할 때
   **When** 해당 탭 아이콘과 텍스트를 보면
   **Then** Teal(`#00B4D8`, `var(--color-primary)`) 색상으로 강조된다

4. **Given** iOS Safari에서 하단 탭바가 표시될 때
   **When** safe area를 확인하면
   **Then** `safe-area-inset-bottom`이 적용되어 홈바와 겹치지 않는다

5. **Given** 탭바의 각 탭을 확인할 때
   **When** 터치 영역을 측정하면
   **Then** 44px × 44px 이상이다 (NFR8)

## Tasks / Subtasks

- [x] Task 1: `safe-area-inset-bottom` 적용 (AC: #4)
  - [x] `frontend/src/styles/layouts/mobile-layout.css` 수정
  - [x] `.m-bottom-nav`에 `padding-bottom: env(safe-area-inset-bottom, 0px)` 추가, `height: calc(60px + env(safe-area-inset-bottom, 0px))`
  - [x] `.m-content` padding 하단값 보정: `calc(68px + 12px + env(safe-area-inset-bottom, 0px))`
  - [x] `frontend/index.html` viewport 메타태그에 `viewport-fit=cover` 추가

- [x] Task 2: 기존 구현 AC 검증 (AC: #1, #2, #3, #5)
  - [x] AC1: `MobileLayout.tsx`의 `.m-header`(fixed top 52px) + `.m-content` + `.m-bottom-nav`(fixed bottom-0) 구조 확인
  - [x] AC2: 5개 탭(통계/경기/플레이어/챔피언/더보기) + Lucide 아이콘 + 텍스트 존재 확인
  - [x] AC3: `.m-bottom-nav-item.active { color: var(--color-primary) }` — `#00B4D8` 확인
  - [x] AC5: `.m-bottom-nav` 높이 60px(+safe area), 아이템당 60px 터치 영역 (44px 초과) 확인

## Dev Notes

### 현황 — 대부분 이미 구현됨 (`8eca530` 커밋)

```
frontend/src/
  components/layout/
    MobileLayout.tsx          ← 이미 존재 (변경 불필요)
    Layout.tsx                ← 데스크톱 레이아웃 (변경 없음)
  styles/layouts/
    mobile-layout.css         ← [수정] safe-area-inset-bottom Task 1
  pages/mobile/
    (11개 파일 모두 존재)     ← 변경 없음
```

### MobileLayout.tsx 현재 구조 (변경 없음)

```tsx
// frontend/src/components/layout/MobileLayout.tsx
const TABS = [
  { to: '/m',           icon: BarChart2,      label: '통계',    end: true },
  { to: '/m/matches',   icon: List,           label: '경기',    end: false },
  { to: '/m/players',   icon: UserRound,      label: '플레이어', end: false },
  { to: '/m/champions', icon: Swords,         label: '챔피언',  end: false },
  { to: '/m/more',      icon: MoreHorizontal, label: '더보기',  end: false },
];

export function MobileLayout() {
  return (
    <div className="m-layout">
      <header className="m-header">...</header>
      <main className="m-content"><Outlet /></main>
      <nav className="m-bottom-nav">
        {TABS.map(({ to, icon: Icon, label, end }) => (
          <NavLink className={({ isActive }) => `m-bottom-nav-item${isActive ? ' active' : ''}`}>
            <Icon size={22} />
            <span>{label}</span>
          </NavLink>
        ))}
      </nav>
    </div>
  );
}
```

### Task 1: safe-area-inset-bottom CSS 수정

**현재 코드 (`mobile-layout.css`):**
```css
.m-bottom-nav {
  position: fixed;
  bottom: 0;
  height: 60px;
  /* ← safe area 없음 */
}

.m-content {
  padding: 12px 12px calc(68px + 12px);
  /* ← safe area 미반영 */
}
```

**변경 후:**
```css
.m-bottom-nav {
  position: fixed;
  bottom: 0;
  height: calc(60px + env(safe-area-inset-bottom));
  padding-bottom: env(safe-area-inset-bottom);
}

.m-content {
  padding: 12px 12px calc(68px + 12px + env(safe-area-inset-bottom));
}
```

### viewport-fit=cover 확인

`safe-area-inset-bottom`이 동작하려면 `index.html`에 다음이 필요:
```html
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover" />
```

파일 위치: `frontend/index.html`

### 라우팅 (변경 없음)

```tsx
// App.tsx — 이미 설정됨
<Route path="m" element={<MobileLayout />}>
  <Route index element={<MobileStatsPage />} />
  <Route path="players" element={<MobilePlayerListPage />} />
  <Route path="champions" element={<MobileChampionListPage />} />
  ...
</Route>
```

### CSS 변수 (frontend — Electron과 다름)

```css
--color-primary: #00B4D8     /* Teal — active 탭 색상 (AC3) */
--color-bg-primary: #0A1428
--color-bg-secondary: #0D1B2E
--color-bg-card: #112240
--color-bg-hover: #152035
--color-border: #1E3A5F
--color-text-primary: #E2E8F0
--color-text-secondary: #94A3B8
--color-text-disabled: #475569
--color-win: #10B981
--color-loss: #EF4444
```

### 코딩 컨벤션

- CSS 클래스는 `m-` 접두사 사용 (기존 패턴 유지)
- `MobileLayout.tsx` 변경 없음 — CSS만 수정
- `env()` fallback: `env(safe-area-inset-bottom, 0px)` — 지원하지 않는 브라우저 대비

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

- `frontend/index.html`: viewport 메타태그에 `viewport-fit=cover` 추가 — `env(safe-area-inset-bottom)` 동작 전제조건
- `mobile-layout.css` `.m-bottom-nav`: `height: calc(60px + env(safe-area-inset-bottom, 0px))` + `padding-bottom: env(safe-area-inset-bottom, 0px)` 추가 (AC #4)
- `mobile-layout.css` `.m-content`: padding 하단 `calc(68px + 12px + env(safe-area-inset-bottom, 0px))`로 보정 — 콘텐츠가 탭바에 가려지지 않음
- `fallback 0px`: `env()` 미지원 브라우저(desktop)에서 기존 동작 유지
- 기존 AC #1~#3, #5: `MobileLayout.tsx`와 CSS에 이미 완전 구현되어 있음 — 변경 없음
- TypeScript `npx tsc --noEmit` 오류 없음

### File List

- frontend/index.html (수정)
- frontend/src/styles/layouts/mobile-layout.css (수정)
