# Story 1.1: 디자인 토큰 & 폰트 설정

Status: done

## Story

As a 개발자,
I want Tailwind CSS 커스텀 색상 토큰과 폰트가 global.css에 정의되고 TIER_COLORS 상수가 TypeScript 파일에 있기를,
so that 모든 신규 컴포넌트에서 일관된 색상(Teal, Navy, Win/Loss)과 수치 전용 폰트(JetBrains Mono)를 사용할 수 있다.

## Acceptance Criteria

1. `frontend/src/styles/global.css`에 다음 CSS 변수가 정의된다:
   - `--color-primary: #00B4D8` (Teal)
   - `--color-win: #10B981`
   - `--color-loss: #EF4444`
   - `--bg-base: #0A0E1A`
   - `--bg-surface: #0F1629`
   - `--bg-elevated: #161D35`
   - `--border-color: #1E2A4A`
   - `--color-text-primary: #F0F4FF`
   - `--color-text-secondary: #8899BB`
   - `--color-text-muted: #4A5568`
   - `--color-warning: #F59E0B`
   - `--color-tier-s: #FFD700`
2. `frontend/src/lib/constants/theme.ts`에 `TIER_COLORS` 상수가 named export로 존재한다:
   `S: '#FFD700', A: '#00B4D8', B: '#8899BB', C: '#4A5568'`
3. Inter 폰트(기본 UI)와 JetBrains Mono 폰트(수치 표시)가 `frontend/index.html`에 설정된다
4. `html, body, #root`에 Inter 폰트가 적용되고, `.font-mono` 유틸리티 클래스로 JetBrains Mono가 적용된다
5. Teal(`#00B4D8`) on Deep Navy(`#0A0E1A`) 배경의 색상 대비가 WCAG AA 4.5:1 이상이다

## Tasks / Subtasks

- [x] Task 1: `global.css` 디자인 토큰 업데이트 (AC: #1, #4)
  - [x] 기존 `--color-primary: #C89B3C` → `#00B4D8`으로 업데이트
  - [x] 기존 `--color-win: #0BC4B4` → `#10B981`으로 업데이트
  - [x] 기존 `--color-loss: #E84040` → `#EF4444`으로 업데이트
  - [x] 기존 배경색 변수에 새 시맨틱 변수 추가: `--bg-base`, `--bg-surface`, `--bg-elevated`
  - [x] 새 변수 추가: `--border-color`, `--color-text-muted`, `--color-tier-s`
  - [x] `--color-text-primary` → `#F0F4FF`으로 업데이트
  - [x] `--color-text-secondary` → `#8899BB`으로 업데이트
  - [x] `.font-mono` 유틸리티 클래스 추가
  - [x] 기존 `.text-gold` 유틸리티는 그대로 유지 (backward compat)
- [x] Task 2: 폰트 설정 (AC: #3, #4)
  - [x] `frontend/index.html` `<head>`에 Google Fonts CDN 링크 추가 (Inter, JetBrains Mono)
  - [x] `global.css`의 `--font-family` → Inter 우선으로 업데이트
  - [x] `--font-family-mono` 변수 추가 (JetBrains Mono)
  - [x] `html, body, #root`에 Inter 폰트 적용 확인
- [x] Task 3: TIER_COLORS 상수 파일 생성 (AC: #2)
  - [x] `frontend/src/lib/constants/` 디렉토리 생성
  - [x] `frontend/src/lib/constants/theme.ts` 파일 생성
  - [x] `TIER_COLORS` named export 작성

## Dev Notes

### 🚨 Critical: 프로젝트는 Tailwind CSS를 사용하지 않는다

현재 코드베이스는 **Tailwind CSS가 설치되어 있지 않다**. 오직 vanilla CSS + CSS 변수만 사용한다.
아키텍처 문서는 신규 컴포넌트에 Tailwind 사용을 명시하지만, Tailwind 설치는 Story 1.2(shadcn/ui 설치)의 범위다.
이 스토리(1.1)는 CSS 변수 + 폰트 + TypeScript 상수만 다룬다.

**Tailwind 유틸리티 클래스(`flex`, `grid-cols-2` 등)는 아직 사용하지 말 것** — Story 1.2 이후부터 사용 가능.

### 현재 코드베이스 상태 (기존 글로벌 CSS 파악)

**`frontend/src/styles/global.css` 현재 상태:**
```css
:root {
  --color-primary: #C89B3C;   /* ← #00B4D8 (Teal)으로 교체 */
  --color-secondary: #5B5A56;
  --color-win: #0BC4B4;       /* ← #10B981으로 교체 */
  --color-loss: #E84040;      /* ← #EF4444으로 교체 */
  --color-bg-primary: #0A1428;
  --color-text-primary: #F0E6D3; /* ← #F0F4FF으로 교체 */
  --color-text-secondary: #A0A8B0; /* ← #8899BB으로 교체 */
  --font-family: 'Pretendard', ...;  /* ← Inter 우선으로 교체 */
}
```

**⚠️ `--color-primary` 변경 영향 분석:**
`--color-primary`를 gold → teal로 변경하면 기존 컴포넌트의 강조색이 바뀐다.
이는 **의도된 UI 리디자인**이다. 전체 UI가 gold 테마 → teal/navy 테마로 전환된다.
- `.text-gold { color: var(--color-primary); }` → teal 색상을 가리키게 됨 — 클래스명은 misleading하지만 유지
- 기존 컴포넌트들은 점진적으로 업데이트될 예정 (각 Epic의 스토리에서 처리)

**`--color-bg-primary` vs `--bg-base` 충돌 방지:**
두 변수를 모두 정의하여 기존 코드(--color-bg-primary)와 신규 코드(--bg-base)가 공존:
```css
--color-bg-primary: #0A0E1A;  /* 기존 변수 — 값을 새 색상으로 업데이트 */
--bg-base: #0A0E1A;            /* 신규 변수 — 새 컴포넌트용 */
```

### 파일 위치 규칙

```
frontend/src/styles/global.css          ← CSS 변수 및 유틸리티 클래스
frontend/src/index.css                  ← @import들 (수정 불필요)
frontend/index.html                     ← Google Fonts CDN 링크 추가
frontend/src/lib/constants/theme.ts     ← TIER_COLORS 신규 파일 (이 디렉토리 신규 생성)
```

**`lib/` 구조:**
```
frontend/src/lib/
  api/          (기존)
  types/        (기존)
  constants/    (신규 생성 — 이 스토리에서)
    theme.ts    (신규 생성)
```

### 코딩 컨벤션 (아키텍처 문서 기준)

```ts
// ✅ Named export 사용 (default export 금지)
export const TIER_COLORS = { ... } as const;

// ❌ 금지
export default TIER_COLORS;
```

### Google Fonts CDN 링크 (index.html에 추가)

```html
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap" rel="stylesheet">
```

### global.css 최종 결과물 기대 형태

추가/변경되는 핵심 부분:
```css
:root {
  /* === 신규 디자인 시스템 토큰 === */

  /* Primary accent — Teal (기존 gold에서 교체) */
  --color-primary: #00B4D8;
  --color-primary-hover: #48CAE4;

  /* Semantic win/loss */
  --color-win: #10B981;
  --color-loss: #EF4444;
  --color-warning: #F59E0B;

  /* Backgrounds (기존 변수 업데이트 + 새 변수 추가) */
  --color-bg-primary: #0A0E1A;
  --bg-base: #0A0E1A;
  --bg-surface: #0F1629;
  --bg-elevated: #161D35;

  /* Borders */
  --color-border: #1E2A4A;
  --border-color: #1E2A4A;

  /* Text */
  --color-text-primary: #F0F4FF;
  --color-text-secondary: #8899BB;
  --color-text-muted: #4A5568;

  /* Tier colors */
  --color-tier-s: #FFD700;

  /* Fonts */
  --font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Malgun Gothic', sans-serif;
  --font-family-mono: 'JetBrains Mono', 'Courier New', monospace;
}

/* 수치 표시 유틸리티 */
.font-mono {
  font-family: var(--font-family-mono);
}
```

### theme.ts 최종 결과물

```ts
export const TIER_COLORS = {
  S: '#FFD700',
  A: '#00B4D8',
  B: '#8899BB',
  C: '#4A5568',
} as const;

export type TierKey = keyof typeof TIER_COLORS;
```

### Project Structure Notes

- 신규 `constants/` 디렉토리는 `frontend/src/lib/` 하위에 생성 (기존 `api/`, `types/`와 동급)
- `global.css`에서 CSS 변수를 변경할 때 기존 변수명 유지 — 기존 컴포넌트 코드 수정 불필요
- 폰트는 npm 패키지(`@fontsource/*`)가 아닌 Google Fonts CDN 사용 — package.json 변경 불필요

### References

- 색상 팔레트 정의: `_bmad-output/planning-artifacts/ux-design-specification.md#Design System Foundation`
- TIER_COLORS 상수 위치: `_bmad-output/planning-artifacts/architecture.md#CSS & Styling Patterns`
- 코딩 컨벤션(Named export): `_bmad-output/planning-artifacts/architecture.md#Naming Patterns`
- AC 원본: `_bmad-output/planning-artifacts/epics.md#Story 1.1`

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

없음 — 모든 작업이 단일 패스로 완료됨.

### Completion Notes List

- ✅ 코드 리뷰 후 수정: `.text-gold` → `var(--color-tier-s)` (#FFD700 gold 유지)
- ✅ 코드 리뷰 후 수정: googleapis preconnect에 `crossorigin` 속성 추가
- ✅ `global.css` 전체 재작성: gold 테마(#C89B3C) → teal/navy 테마(#00B4D8/#0A0E1A)
- ✅ 신규 시맨틱 변수 추가: `--bg-base`, `--bg-surface`, `--bg-elevated`, `--border-color`, `--color-text-muted`, `--color-tier-s`, `--font-family-mono`
- ✅ 승/패 색상 업데이트: win `#0BC4B4` → `#10B981`, loss `#E84040` → `#EF4444`
- ✅ `.font-mono` 유틸리티 클래스 추가 (JetBrains Mono 수치 정렬용)
- ✅ `.text-gold` backward compat 유지 (이제 teal을 가리킴)
- ✅ `index.html` Google Fonts CDN 링크 추가 (Inter + JetBrains Mono)
- ✅ `frontend/src/lib/constants/theme.ts` 신규 파일 생성
- ✅ `TIER_COLORS` & `TierKey` named export 작성
- ✅ TypeScript 타입 체크 통과 (오류 없음)
- ⚠️ 테스트 프레임워크 미설치 (Story 1.x 범위 외) — 빌드 검증 + TypeScript 컴파일로 대체
- ℹ️ `--sidebar-width` 220px → 240px 업데이트 (아키텍처 문서 기준)

### File List

- `frontend/src/styles/global.css` (수정)
- `frontend/index.html` (수정)
- `frontend/src/lib/constants/theme.ts` (신규)
