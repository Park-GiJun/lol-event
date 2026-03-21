# Story 1.4: 공통 피드백 컴포넌트 생성 (InlineError, Skeleton)

Status: done

## Story

As a 개발자,
I want InlineError와 Skeleton 공통 컴포넌트가 존재하기를,
so that 모든 데이터 페치 컴포넌트에서 일관된 로딩/에러 UI 패턴을 적용할 수 있다.

## Acceptance Criteria

1. **Given** `components/common/InlineError.tsx`가 생성되었을 때
   **When** `<InlineError message="데이터를 불러오지 못했습니다" onRetry={refetch} />`를 렌더링하면
   **Then** 에러 메시지 텍스트와 "다시 시도" 버튼이 표시된다
   **And** 버튼 클릭 시 `onRetry` 콜백이 호출된다 (NFR11)

2. **Given** `components/common/Skeleton.tsx`가 생성되었을 때
   **When** `<Skeleton className="h-8 w-full" />`을 렌더링하면
   **Then** pulse 애니메이션이 있는 회색 블록이 표시된다
   **And** 지정된 className의 높이/너비가 적용된다 (NFR12)

3. **Given** 데이터 로딩 중일 때
   **When** 컴포넌트가 Skeleton을 렌더링하면
   **Then** 레이아웃이 흔들리지 않는다 (CLS 방지, NFR12)

## Tasks / Subtasks

- [x] Task 1: `InlineError.tsx` 생성 (AC: #1, NFR11)
  - [x] `frontend/src/components/common/InlineError.tsx` 파일 생성
  - [x] `AlertCircle` 아이콘 + 메시지 텍스트 + "다시 시도" 버튼 구현
  - [x] `onRetry` 콜백을 버튼 클릭 핸들러로 연결
  - [x] named export `InlineErrorProps` 인터페이스와 `InlineError` 함수

- [x] Task 2: `Skeleton.tsx` 생성 (AC: #2, #3, NFR12)
  - [x] `frontend/src/components/common/Skeleton.tsx` 파일 생성
  - [x] shadcn `Skeleton`(`@/components/ui/skeleton`) 래핑 또는 동일 구현
  - [x] `animate-pulse` + `className` prop 병합 지원
  - [x] named export `Skeleton` 함수

- [x] Task 3: TypeScript 검증 (AC: #1, #2)
  - [x] `npx tsc --noEmit` 오류 없이 통과 확인

## Dev Notes

### 🚨 Stories 1.1~1.3 학습 사항 (반드시 읽을 것)

- **Tailwind CSS v4** 설치 완료 (`@tailwindcss/vite`) — `animate-pulse`, `h-8`, `w-full`, `rounded-md` 등 Tailwind 클래스 사용 가능
- **shadcn Skeleton 이미 존재** — `frontend/src/components/ui/skeleton.tsx` (Story 1.2 설치), 이를 래핑하거나 동일 구현
- **`@/` path alias** 설정 완료 — `import { cn } from '@/lib/utils'` 형태 사용
- **Named export 규칙** — 모든 함수/인터페이스 named export (default export 금지)
- **`import type`** — 타입 전용 import에 `import type` 필수 (`verbatimModuleSyntax: true`)
- **기존 Button 컴포넌트** — `frontend/src/components/common/Button.tsx` 재사용 가능

### 기존 컴포넌트 패턴 (반드시 참고)

`components/common/`의 기존 패턴:

```tsx
// ✅ 기존 ErrorModal.tsx 패턴 (참고용)
import { AlertCircle } from 'lucide-react';
import { Modal } from './Modal';
import { Button } from './Button';

interface ErrorModalProps {
  isOpen: boolean; title: string; message: string; onClose: () => void;
}

export function ErrorModal({ isOpen, title, message, onClose }: ErrorModalProps) {
  return (
    <Modal isOpen={isOpen} onClose={onClose} title={title} size="sm"
      footer={<Button onClick={onClose}>확인</Button>}>
      <div className="flex items-center gap-md" style={{ color: 'var(--color-error)' }}>
        <AlertCircle size={20} />
        <span>{message}</span>
      </div>
    </Modal>
  );
}

// ✅ 기존 Button.tsx 시그니처 (재사용)
// variant?: 'primary' | 'secondary' | 'danger' | 'ghost'
// size?: 'sm' | 'md' | 'lg'
```

**InlineError는 Modal이 아닌 인라인 표시** — `ErrorModal`과 달리 페이지를 막지 않음.

### Task 1 상세: InlineError.tsx 최종 형태

```tsx
// frontend/src/components/common/InlineError.tsx
import { AlertCircle } from 'lucide-react';
import { Button } from './Button';

export interface InlineErrorProps {
  message: string;
  onRetry: () => void;
  className?: string;
}

export function InlineError({ message, onRetry, className = '' }: InlineErrorProps) {
  return (
    <div
      className={`flex items-center gap-sm ${className}`}
      style={{ color: 'var(--color-error)', padding: 'var(--spacing-md)' }}
    >
      <AlertCircle size={16} />
      <span className="text-sm">{message}</span>
      <Button variant="secondary" size="sm" onClick={onRetry}>
        다시 시도
      </Button>
    </div>
  );
}
```

**포인트:**
- `AlertCircle` icon — `lucide-react` (이미 설치, ErrorModal 동일 패턴)
- `Button` 컴포넌트 재사용 — `variant="secondary"` + `size="sm"`
- 색상: `var(--color-error)` (#EF4444) — 기존 ErrorModal과 동일
- 간격: CSS 변수 기반 (`--spacing-md`, `--spacing-sm` via `gap-sm` 기존 utility)
- `className` prop 지원 — 호출자가 레이아웃 조절 가능

### Task 2 상세: Skeleton.tsx 최종 형태

**옵션 A — shadcn Skeleton 래핑 (권장):**

```tsx
// frontend/src/components/common/Skeleton.tsx
import { Skeleton as ShadcnSkeleton } from '@/components/ui/skeleton';
import type { ComponentPropsWithoutRef } from 'react';

export function Skeleton(props: ComponentPropsWithoutRef<typeof ShadcnSkeleton>) {
  return <ShadcnSkeleton {...props} />;
}
```

**옵션 B — 직접 구현 (shadcn과 동일 로직):**

```tsx
// frontend/src/components/common/Skeleton.tsx
import * as React from 'react';
import { cn } from '@/lib/utils';

export function Skeleton({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn('animate-pulse rounded-md bg-muted', className)}
      {...props}
    />
  );
}
```

**권장: 옵션 B (직접 구현)** — `components/common/`는 `components/ui/`를 내부적으로 사용하지 않는 독립 레이어로 유지. shadcn의 `bg-muted`가 이 프로젝트의 dark theme에서 렌더링되지 않을 경우 `bg-[#1E2A4A]` 같은 프로젝트 CSS 변수로 대체 가능.

> ℹ️ `bg-muted` 동작 확인: Tailwind v4에서 `bg-muted` 클래스가 시각적으로 동작하지 않으면 `style={{ backgroundColor: 'var(--color-border)' }}` (#1E2A4A) 를 fallback으로 사용.

**사용 예시 (검증 기준):**
```tsx
<Skeleton className="h-8 w-full" />          // → 높이 32px, 전체 너비 pulse 블록
<Skeleton className="h-4 w-48" />            // → 높이 16px, 너비 192px
<Skeleton className="h-10 w-10 rounded-full" /> // → 원형 아바타 skeleton
```

### CLS 방지 패턴 (AC #3, NFR12)

Skeleton으로 CLS를 방지하려면 Skeleton이 실제 콘텐츠와 동일한 크기를 차지해야 함:

```tsx
// ✅ CLS 방지 — Skeleton이 콘텐츠 공간을 미리 점유
function LeaderboardCard() {
  const { data, isLoading, error, refetch } = useLeaderboard();
  if (isLoading) return <Skeleton className="h-40 w-full" />;
  if (error) return <InlineError message="데이터를 불러오지 못했습니다" onRetry={refetch} />;
  return <div className="h-40 w-full">...</div>;  // 동일 크기
}

// ❌ CLS 유발 — 로딩 중 높이가 0이다가 갑자기 늘어남
function BadCard() {
  const { data, isLoading } = useLeaderboard();
  if (isLoading) return null;  // 레이아웃 점프 발생
  return <div>...</div>;
}
```

### 코딩 컨벤션

```tsx
// ✅ Named export (인터페이스도 named export)
export interface InlineErrorProps { ... }
export function InlineError({ ... }: InlineErrorProps) { ... }
export function Skeleton({ ... }: ...) { ... }

// ❌ Default export 금지
export default InlineError;

// ✅ 타입 전용 import에 import type 사용
import type { ComponentPropsWithoutRef } from 'react';

// ✅ CSS는 CSS 변수 + Tailwind 클래스 혼합 사용 (이 프로젝트 패턴)
// style={{ color: 'var(--color-error)' }}  — CSS 변수 (기존 패턴 유지)
// className="flex items-center gap-sm"     — Tailwind + global.css utility 혼합
```

### 파일 위치 규칙

```
frontend/src/
  components/
    common/
      Button.tsx          ← 기존, 재사용
      ErrorModal.tsx      ← 기존, 참고용
      InlineError.tsx     ← 신규 (이 스토리)
      Skeleton.tsx        ← 신규 (이 스토리)
    ui/
      skeleton.tsx        ← shadcn 설치본, 독립적으로 유지
```

**`components/ui/`의 shadcn 컴포넌트와 `components/common/`은 별도 레이어.** 필요 시 common이 ui를 참조할 수 있으나 ui → common 방향은 없음.

### 다음 스토리 연계

이 스토리의 컴포넌트들은 Epic 2부터 모든 데이터 페치 컴포넌트에서 다음 패턴으로 사용:

```tsx
import { InlineError } from '@/components/common/InlineError';
import { Skeleton } from '@/components/common/Skeleton';
import { useLeaderboard } from '@/hooks/useLeaderboard';

function LeaderboardSection() {
  const { data, isLoading, error, refetch } = useLeaderboard();
  if (isLoading) return <Skeleton className="h-64 w-full" />;
  if (error) return <InlineError message="데이터를 불러오지 못했습니다" onRetry={refetch} />;
  return <div>...</div>;
}
```

### References

- AC 원본: `_bmad-output/planning-artifacts/epics.md` (Lines 236-257)
- NFR11/12: `_bmad-output/planning-artifacts/architecture.md` (Lines 176-182)
- 에러 처리 패턴: `_bmad-output/planning-artifacts/architecture.md` (Lines 327-335)
- 기존 Button: `frontend/src/components/common/Button.tsx`
- 기존 ErrorModal: `frontend/src/components/common/ErrorModal.tsx`
- shadcn Skeleton: `frontend/src/components/ui/skeleton.tsx`

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

- ✅ `frontend/src/components/common/InlineError.tsx` — AlertCircle + 메시지 + "다시 시도" Button, `var(--color-error)`, named export
- ✅ `frontend/src/components/common/Skeleton.tsx` — `animate-pulse rounded-md bg-muted` 직접 구현 (옵션 B), `cn()` 으로 className 병합, named export
- ✅ TypeScript `tsc --noEmit` 오류 없이 통과

### File List

- `frontend/src/components/common/InlineError.tsx` (신규)
- `frontend/src/components/common/Skeleton.tsx` (신규)
