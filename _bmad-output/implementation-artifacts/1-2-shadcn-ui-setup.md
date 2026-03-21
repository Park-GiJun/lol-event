# Story 1.2: shadcn/ui 설치 & 기본 컴포넌트 추가

Status: done

## Story

As a 개발자,
I want shadcn/ui CLI로 필요한 컴포넌트가 설치되기를,
so that Card, Table, Badge, Popover, Breadcrumb, Skeleton, Tabs, Button 등 shadcn 컴포넌트를 신규 컴포넌트 구현 시 즉시 사용할 수 있다.

## Acceptance Criteria

1. `frontend/src/components/ui/` 폴더에 다음 파일들이 존재한다: `button.tsx`, `card.tsx`, `table.tsx`, `badge.tsx`, `popover.tsx`, `breadcrumb.tsx`, `skeleton.tsx`, `tabs.tsx`
2. shadcn/ui 컴포넌트를 import해도 기존 CSS 스타일(global.css)과 충돌 없이 렌더링된다
3. `import { ChevronRight } from 'lucide-react'` 사용 시 정상 렌더링된다 (이미 설치됨)
4. `frontend/src/lib/utils.ts`에 `cn()` 유틸리티 함수가 named export로 존재한다
5. TypeScript 타입 체크(`tsc --noEmit`)가 오류 없이 통과한다

## Tasks / Subtasks

- [x] Task 1: Tailwind CSS v4 설치 및 Vite 설정 (AC: #2)
  - [x]`npm install tailwindcss @tailwindcss/vite` 실행
  - [x]`npm install clsx tailwind-merge class-variance-authority` 실행
  - [x]`frontend/vite.config.ts`에 `@tailwindcss/vite` 플러그인 추가
  - [x]`frontend/src/index.css` 맨 첫 줄에 `@import "tailwindcss";` 추가
- [x] Task 2: `@/` 경로 별칭 설정 (shadcn/ui 필수 요건) (AC: #5)
  - [x]`frontend/tsconfig.app.json`에 `"paths": { "@/*": ["./src/*"] }` 추가
  - [x]`frontend/vite.config.ts`에 `resolve.alias: { "@": path.resolve(__dirname, "./src") }` 추가
  - [x]`import path from 'path'` import 추가 (vite.config.ts)
- [x] Task 3: `components.json` 수동 생성 (shadcn/ui 설정 파일) (AC: #1)
  - [x]`frontend/components.json` 파일 생성 (아래 Dev Notes의 내용 사용)
- [x] Task 4: `src/lib/utils.ts` 생성 (AC: #4)
  - [x]`frontend/src/lib/utils.ts` 파일 생성 — `cn()` named export
- [x] Task 5: shadcn/ui 컴포넌트 추가 (AC: #1)
  - [x]`frontend/` 디렉토리에서 실행: `npx shadcn@latest add button card table badge popover breadcrumb skeleton tabs`
  - [x]`frontend/src/components/ui/` 폴더에 8개 파일 모두 존재 확인
- [x] Task 6: 검증 (AC: #2, #3, #5)
  - [x]`npx tsc --noEmit` 오류 없이 통과 확인
  - [x]기존 CSS 변수(`--color-primary`, `--bg-base` 등) 변경 없음 확인

## Dev Notes

### 🚨 Story 1.1 학습 사항 (반드시 읽을 것)

- 프로젝트는 **Tailwind CSS가 설치되어 있지 않다** — 이 스토리에서 처음 설치
- **Vite 8.0.0** 사용 → Tailwind v3 방식(postcss) 대신 **Tailwind v4 Vite 플러그인** 방식 사용
- 기존 CSS는 `frontend/src/styles/global.css` CSS 변수 방식으로 작성됨 — 건드리지 않음
- `lucide-react@^0.469.0` **이미 설치됨** — 재설치 불필요 (AC #3 이미 충족)
- `@types/node` **이미 설치됨** — `path.resolve()` 사용 가능

### 현재 package.json dependencies

```json
{
  "dependencies": {
    "lucide-react": "^0.469.0",
    "react": "^19.2.4",
    "react-dom": "^19.2.4",
    "react-router-dom": "^6.30.3"
  },
  "devDependencies": {
    "@types/node": "^24.12.0",
    "@vitejs/plugin-react": "^6.0.0",
    "vite": "^8.0.0"
    // ...
  }
}
```

### Task 1 상세: vite.config.ts 최종 형태

```ts
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import path from 'path'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 8080,
  },
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
})
```

### Task 1 상세: index.css 수정

`@import "tailwindcss"` 를 **파일 첫 줄**에 추가. 기존 imports는 그대로 유지:

```css
@import "tailwindcss";
@import './styles/global.css';
@import './styles/components/button.css';
/* ... 나머지 기존 imports 유지 ... */
```

⚠️ **중요:** Tailwind v4의 preflight는 `box-sizing: border-box` 등 기본 리셋을 적용한다.
기존 `global.css`도 동일한 리셋을 정의하므로 충돌 없이 공존한다 (같은 값, 나중에 선언된 것이 우선).

### Task 2 상세: tsconfig.app.json 수정

`compilerOptions` 안에 `paths` 추가:

```json
{
  "compilerOptions": {
    // ... 기존 옵션 유지 ...
    "baseUrl": ".",
    "paths": {
      "@/*": ["./src/*"]
    }
  }
}
```

⚠️ `paths`를 사용하려면 `baseUrl`도 함께 설정해야 한다.

### Task 3 상세: components.json 내용

`frontend/components.json` (프로젝트 루트가 아닌 **frontend/ 폴더에** 생성):

```json
{
  "$schema": "https://ui.shadcn.com/schema.json",
  "style": "default",
  "rsc": false,
  "tsx": true,
  "tailwind": {
    "config": "",
    "css": "src/index.css",
    "baseColor": "neutral",
    "cssVariables": true,
    "prefix": ""
  },
  "aliases": {
    "components": "@/components",
    "utils": "@/lib/utils",
    "ui": "@/components/ui",
    "lib": "@/lib",
    "hooks": "@/hooks"
  },
  "iconLibrary": "lucide"
}
```

- `"config": ""` → Tailwind v4 방식 (tailwind.config.js 불필요)
- `"rsc": false` → React 19, 클라이언트 컴포넌트
- `"iconLibrary": "lucide"` → 이미 설치된 lucide-react 사용

### Task 4 상세: lib/utils.ts 내용

```ts
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
```

### Task 5 상세: shadcn/ui 컴포넌트 추가 명령어

`frontend/` 디렉토리에서 실행:

```bash
npx shadcn@latest add button card table badge popover breadcrumb skeleton tabs
```

- `-y` 플래그로 확인 프롬프트 건너뛰기 가능
- 컴포넌트는 `frontend/src/components/ui/` 폴더에 생성됨
- shadcn이 `class-variance-authority`, `@radix-ui/*` 등 추가 deps를 자동 설치할 수 있음 — 허용

**예상 추가 의존성 (shadcn이 자동 설치):**
- `@radix-ui/react-popover`
- `@radix-ui/react-slot`
- `@radix-ui/react-tabs`

### shadcn/ui CSS 변수 vs 기존 CSS 변수 공존

shadcn/ui는 자체 CSS 변수를 `:root`에 추가한다:
```css
--background, --foreground, --primary, --card, --border, --input, --ring 등
```

이 변수들은 우리 기존 변수들(`--color-primary`, `--bg-base`, `--color-border` 등)과 **이름이 다르므로 충돌하지 않는다**.

단, shadcn의 `--border` 와 우리의 `--color-border` / `--border-color`는 다른 이름이므로 문제없다.

shadcn이 index.css에 CSS 변수를 추가하는 경우, **기존 global.css import 이후에 위치**하도록 확인.

### 코딩 컨벤션 준수

```ts
// ✅ Named export (default export 금지)
export function cn(...) { ... }

// ❌ 금지 — shadcn 자동 생성 파일은 예외 (ui/ 컴포넌트는 default export 허용)
// 이유: shadcn 생성 파일은 외부 도구 산출물로 컨벤션 예외
```

⚠️ **단, `src/components/ui/` 폴더 외부에서 작성하는 코드는 여전히 named export만 사용.**

### tsconfig strict 모드 주의사항

`tsconfig.app.json`에 `"noUnusedLocals": true`, `"noUnusedParameters": true` 설정됨.
shadcn 컴포넌트 파일들은 사용되지 않아도 TypeScript 오류가 발생하지 않음 (모듈 파일은 exports만으로 충분).

### 파일 위치 규칙

```
frontend/
  components.json          ← shadcn/ui 설정 (신규)
  src/
    index.css              ← @import "tailwindcss" 추가
    lib/
      utils.ts             ← cn() 유틸리티 (신규)
      constants/
        theme.ts           ← Story 1.1에서 생성됨 (수정 없음)
    components/
      ui/                  ← shadcn 컴포넌트들 (신규 폴더)
        button.tsx
        card.tsx
        table.tsx
        badge.tsx
        popover.tsx
        breadcrumb.tsx
        skeleton.tsx
        tabs.tsx
  vite.config.ts           ← tailwind 플러그인 + path alias 추가
  tsconfig.app.json        ← paths 추가
```

### References

- Tailwind v4 Vite 설치: Architecture doc — "Tailwind CSS + shadcn/ui"
- 컴포넌트 목록: `_bmad-output/planning-artifacts/epics.md#Story 1.2`
- 코딩 컨벤션: `_bmad-output/planning-artifacts/architecture.md#Naming Patterns`
- Story 1.1 구현 결과: `_bmad-output/implementation-artifacts/1-1-design-tokens-and-fonts.md`

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

- shadcn CLI가 `@/components/ui/`를 `frontend/@/components/ui/`로 리터럴 생성 → 수동으로 `frontend/src/components/ui/`로 이동 후 `@` 디렉토리 삭제

### Completion Notes List

- ✅ `tailwindcss`, `@tailwindcss/vite` 설치 완료 (Vite 8.0.0 호환 v4 방식)
- ✅ `clsx`, `tailwind-merge`, `class-variance-authority` 설치 완료
- ✅ `frontend/vite.config.ts` — tailwindcss 플러그인 + `@/` path alias 추가
- ✅ `frontend/src/index.css` — 첫 줄에 `@import "tailwindcss"` 추가 (기존 imports 유지)
- ✅ `frontend/tsconfig.app.json` — `baseUrl: "."` + `paths: { "@/*": ["./src/*"] }` 추가
- ✅ `frontend/components.json` — Tailwind v4 방식 (`"config": ""`) 수동 생성
- ✅ `frontend/src/lib/utils.ts` — `cn()` named export 생성
- ✅ `npx shadcn@latest add` 로 8개 컴포넌트 설치 후 `src/components/ui/`로 이동
- ✅ TypeScript `tsc --noEmit` 오류 없이 통과
- ✅ 기존 CSS 변수(`--color-primary`, `--bg-base` 등) 변경 없음 확인
- ℹ️ shadcn이 index.css에 CSS 변수를 추가하지 않음 — Tailwind v4 감지 결과로 정상
- ✅ 코드 리뷰 후 수정: `skeleton.tsx`에 `import * as React from "react"` 추가 (React.HTMLAttributes 명시적 import)
- ✅ 코드 리뷰 후 수정: `breadcrumb.tsx` displayName 오타 수정 (`"BreadcrumbElipssis"` → `"BreadcrumbEllipsis"`)
- ✅ 코드 리뷰 후 수정: `tailwindcss-animate` 설치 + `index.css`에 `@plugin "tailwindcss-animate"` 등록 (Popover 애니메이션 활성화)

### File List

- `frontend/vite.config.ts` (수정)
- `frontend/tsconfig.app.json` (수정)
- `frontend/src/index.css` (수정)
- `frontend/components.json` (신규)
- `frontend/src/lib/utils.ts` (신규)
- `frontend/src/components/ui/button.tsx` (신규)
- `frontend/src/components/ui/card.tsx` (신규)
- `frontend/src/components/ui/table.tsx` (신규)
- `frontend/src/components/ui/badge.tsx` (신규)
- `frontend/src/components/ui/popover.tsx` (신규)
- `frontend/src/components/ui/breadcrumb.tsx` (신규)
- `frontend/src/components/ui/skeleton.tsx` (신규)
- `frontend/src/components/ui/tabs.tsx` (신규)
