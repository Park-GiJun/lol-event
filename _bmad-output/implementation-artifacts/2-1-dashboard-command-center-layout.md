# Story 2.1: Dashboard Command Center 레이아웃 재설계

Status: done

## Story

As a 유저,
I want Dashboard 접속 시 좌측 240px 고정 사이드바와 콘텐츠 영역이 분리된 레이아웃을 보기를,
So that 어느 페이지에서든 사이드바로 즉시 원하는 섹션으로 이동할 수 있다.

## Acceptance Criteria

1. **Given** 유저가 Dashboard(`/`)에 접속했을 때
   **When** 페이지가 로드되면
   **Then** 좌측 240px 고정 사이드바와 우측 콘텐츠 영역이 분리되어 렌더링된다

2. **Given** 사이드바에 네비게이션 항목들이 있을 때
   **When** 현재 활성 페이지의 항목을 확인하면
   **Then** Teal 색상(`#00B4D8`) 좌측 보더와 배경 강조가 표시된다

3. **Given** 사이드바 네비게이션 항목에 Lucide 아이콘이 있을 때
   **When** 아이콘을 확인하면
   **Then** 컬러 이모지가 아닌 단색/라인 Lucide 아이콘이 사용된다

4. **Given** 브라우저 너비가 1024px 이상일 때
   **When** Dashboard를 사용하면
   **Then** 사이드바가 항상 표시된다

## Tasks / Subtasks

- [x] Task 1: `sidebar.css` active 배경색 수정 (AC: #2)
  - [x] `.sidebar-item.active` 배경색을 `rgba(200, 155, 60, 0.08)` (gold) → `rgba(var(--color-primary-rgb), 0.08)` (Teal)으로 수정
  - [x] `frontend/src/styles/layouts/sidebar.css` 파일 수정

- [x] Task 2: `global.css`에 shadcn CSS 변수 매핑 추가 (Epic 1 회고 액션 A1)
  - [x] `:root`에 shadcn 호환 CSS 변수들을 프로젝트 토큰에 매핑 추가
  - [x] `--background`, `--foreground`, `--muted`, `--muted-foreground`, `--card`, `--card-foreground` 등 정의

- [x] Task 3: 레이아웃 AC 검증 (AC: #1, #3, #4)
  - [x] `Layout.tsx` + `Sidebar.tsx` 읽어서 AC1(240px 고정), AC3(Lucide 아이콘), AC4(1024px+ 상시 표시) 이미 충족 확인
  - [x] 수정 불필요 시 완료 처리

- [x] Task 4: TypeScript 검증
  - [x] `npx tsc --noEmit` 오류 없이 통과 확인

## Dev Notes

### 🚨 CRITICAL: 레이아웃 구조는 이미 구현됨 — AC1/AC3/AC4 재구현 금지

현재 `frontend/src/components/layout/Layout.tsx` + `Sidebar.tsx` + `sidebar.css` 구조 분석:

**AC1 (240px 고정 사이드바) — 이미 충족:**
```css
/* sidebar.css */
.layout { display: flex; height: 100vh; overflow: hidden; }
.sidebar { width: var(--sidebar-width); /* = 240px */ flex-shrink: 0; }
.main-content { flex: 1; }
```

**AC3 (Lucide 아이콘) — 이미 충족:**
```tsx
// Sidebar.tsx
import { Swords, Users, BarChart2, List, Radio, RefreshCw, Shield, UserRound } from 'lucide-react';
const NAV_ITEMS = [
  { to: '/',             icon: BarChart2,  label: '전체 통계' },
  { to: '/player-stats', icon: UserRound,  label: '멤버 통계' },
  // ... 모두 Lucide 아이콘 사용
];
```

**AC4 (1024px+ 상시 표시) — 이미 충족:**
```css
/* 데스크톱 기본: sidebar 항상 표시 */
/* 모바일 < 768px 에서만 drawer mode */
@media (max-width: 768px) {
  .sidebar { position: fixed; transform: translateX(-100%); }
}
/* → 768px 이상에서 항상 표시, 1024px 요건 충족 */
```

### Task 1 상세: active 배경색 수정 (유일한 CSS 수정)

**문제:** `.sidebar-item.active`의 배경색이 구 Gold 색상으로 남아있음

```css
/* sidebar.css:57-61 — 현재 (수정 전) */
.sidebar-item.active {
  color: var(--color-primary);
  border-left-color: var(--color-primary);
  background: rgba(200, 155, 60, 0.08);  /* ❌ Gold — 구 디자인 잔재 */
}
```

```css
/* 수정 후 */
.sidebar-item.active {
  color: var(--color-primary);
  border-left-color: var(--color-primary);
  background: rgba(var(--color-primary-rgb), 0.08);  /* ✅ Teal rgba */
}
```

`--color-primary-rgb: 0, 180, 216` → `rgba(0, 180, 216, 0.08)` = `#00B4D8` 10% 투명도

### Task 2 상세: shadcn CSS 변수 매핑

Epic 1 회고 액션 아이템 A1 해결. shadcn 컴포넌트들이 `bg-muted`, `bg-card`, `text-foreground` 등을 사용하므로 프로젝트 디자인 토큰에 매핑 필요.

**`frontend/src/styles/global.css`의 `:root` 섹션 끝에 추가:**

```css
/* ================================================================
 * SHADCN CSS 변수 매핑 (프로젝트 토큰 → shadcn 호환)
 * ================================================================ */
/* Tailwind v4 + shadcn: bg-background, text-foreground 등 사용 */
--background: var(--color-bg-primary);         /* #0A0E1A */
--foreground: var(--color-text-primary);       /* #F0F4FF */
--card: var(--color-bg-secondary);             /* #0F1629 */
--card-foreground: var(--color-text-primary);  /* #F0F4FF */
--popover: var(--color-bg-tertiary);           /* #161D35 */
--popover-foreground: var(--color-text-primary);
--primary: var(--color-primary);               /* #00B4D8 */
--primary-foreground: var(--color-text-inverse); /* #0A0E1A */
--secondary: var(--color-bg-tertiary);         /* #161D35 */
--secondary-foreground: var(--color-text-primary);
--muted: var(--color-border);                  /* #1E2A4A — skeleton 배경 */
--muted-foreground: var(--color-text-secondary); /* #8899BB */
--accent: var(--color-bg-hover);               /* #1A2540 */
--accent-foreground: var(--color-text-primary);
--destructive: var(--color-error);             /* #EF4444 */
--destructive-foreground: var(--color-text-primary);
--border: var(--color-border);                 /* #1E2A4A */
--input: var(--color-border);                  /* #1E2A4A */
--ring: var(--color-primary);                  /* #00B4D8 */
--radius: var(--radius-md);                    /* 6px */
```

**효과:**
- `bg-muted` → `#1E2A4A` (Skeleton 배경 해결 — Epic 1 회고 D1)
- `bg-card` → `#0F1629` (Card 컴포넌트 배경)
- `text-foreground` → `#F0F4FF` (텍스트)
- `ring-ring` → `#00B4D8` (focus ring Teal)

### 파일 위치 규칙

```
frontend/src/
  styles/
    global.css          ← shadcn CSS 변수 매핑 추가 (Task 2)
    layouts/
      sidebar.css       ← active 배경색 수정 (Task 1)
  components/
    layout/
      Layout.tsx        ← 수정 없음 (AC1/AC4 이미 충족)
      Sidebar.tsx       ← 수정 없음 (AC3 이미 충족)
```

### 🚨 Stories 1.1~1.5 학습 사항

- **Tailwind CSS v4** — `@tailwindcss/vite` 방식. CSS 변수 기반으로 shadcn 토큰 정의 시 Tailwind v4의 CSS variable 해석 방식 활용
- **`@/` path alias** 설정 완료
- **Named export 규칙** — 모든 함수 named export (default export 금지, App.tsx 예외)
- **`import type`** — 타입 전용 import에 필수 (`verbatimModuleSyntax: true`)
- **shadcn CSS 변수 추가 시 CSS 파일만 수정** — TypeScript 변경 없음

### 다음 스토리 연계

- **Story 2.2**: `EloLeaderboard.tsx` — `useLeaderboard` 훅 + `InlineError` + `Skeleton` 사용. `bg-muted` Skeleton 배경은 이 스토리에서 해결됨
- **Story 2.3**: `ChampionTierTable.tsx` — `TIER_COLORS` 상수 + shadcn Badge 사용. `bg-muted` 해결됨
- **Story 2.4**: `BanTrendCard.tsx` — `bg-card` 사용 시 이 스토리에서 정의한 `--card` 변수 활용

### References

- AC 원본: `_bmad-output/planning-artifacts/epics.md` (Lines 290-313)
- 아키텍처 레이아웃: `_bmad-output/planning-artifacts/architecture.md` (Lines 380-400)
- Epic 1 회고 액션 A1: `_bmad-output/implementation-artifacts/epic-1-retro-2026-03-22.md`
- 기존 Sidebar: `frontend/src/components/layout/Sidebar.tsx`
- 기존 CSS: `frontend/src/styles/layouts/sidebar.css`
- 기존 global.css: `frontend/src/styles/global.css`

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

- Code Review 2026-03-22: Patch P1 — `--popover: var(--color-bg-elevated)` → `var(--color-bg-tertiary)` (bad_spec: `--color-bg-elevated` 미정의 변수 참조 수정)

### Completion Notes List

- ✅ Task 1: `sidebar.css` `.sidebar-item.active` background `rgba(200, 155, 60, 0.08)` (Gold) → `rgba(var(--color-primary-rgb), 0.08)` (Teal) 수정
- ✅ Task 2: `global.css` `:root`에 shadcn 호환 CSS 변수 24개 매핑 추가 (`--background`, `--foreground`, `--card`, `--muted`, `--primary`, `--border`, `--ring`, `--radius` 등). Epic 1 회고 액션 A1 완료.
- ✅ Task 3: AC1(240px 고정 사이드바), AC3(Lucide 아이콘), AC4(768px+ 상시 표시) 기존 코드로 이미 충족 — 수정 없음
- ✅ Task 4: `npx tsc --noEmit` 오류 없이 통과

### File List

- `frontend/src/styles/layouts/sidebar.css` (수정 — active 배경색 gold → Teal)
- `frontend/src/styles/global.css` (수정 — shadcn CSS 변수 매핑 추가)

