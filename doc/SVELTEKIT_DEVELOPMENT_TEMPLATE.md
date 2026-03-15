# SvelteKit Frontend 개발 가이드 (범용 템플릿)

> 이 문서는 SvelteKit 기반 프론트엔드 프로젝트에 적용할 수 있는 범용 개발 지침입니다.

---

## 1. 프로젝트 개요

이 프로젝트는 **SvelteKit** 기반의 프론트엔드 애플리케이션입니다.

---

## 2. 기술 스택

- **Framework**: SvelteKit
- **Language**: TypeScript
- **Styling**: CSS (Global CSS Variables)
- **Package Manager**: pnpm
- **Linting**: ESLint
- **Formatting**: Prettier

---

## 3. 폴더 구조

- 가상 면접 사례로 배우는 대규모 시스템 설계 기초 2에 대한 프론트 엔드 구현
- 아래 항목에 대해서 각각 별도의 화면 구현 필요
- 복잡할 필요는 없음
  - 주변 친구
  - 구글 맵
  - 분산 메세지 큐
  - 지표 모니터링 및 경보 시스템
  - 광고 클릭 이벤트 집계
  - 호텔 예약 시스템
  - 분산 이메일 서비스
  - S3와 유사한 객체 저장소
  - 실시간 게임 순위표
  - 결제 시스템
  - 전자 지갑
  - 증권 거래서

```bash
frontend/
├── doc/                           # 개발 문서
├── src/
│   ├── lib/
│   │   ├── components/            # 공통 컴포넌트
│   │   │   ├── desktop/           # 데스크톱 전용 컴포넌트
│   │   │   ├── mobile/            # 모바일 전용 컴포넌트
│   │   │   ├── shared/            # 공유 컴포넌트
│   │   │   ├── Button.svelte
│   │   │   ├── Input.svelte
│   │   │   ├── Modal.svelte
│   │   │   ├── Grid.svelte
│   │   │   ├── Table.svelte
│   │   │   └── ErrorModal.svelte
│   │   ├── stores/                # Svelte Stores
│   │   ├── api/                   # API 통신
│   │   │   └── api.ts
│   │   ├── types/                 # TypeScript 인터페이스
│   │   │   ├── api.ts
│   │   │   ├── menu.ts
│   │   │   └── common.ts
│   │   └── utils/                 # 유틸리티 함수
│   │       └── device.ts          # 디바이스 감지 유틸리티
│   ├── routes/                    # 페이지 라우팅 (데스크톱)
│   │   ├── (desktop)/             # 데스크톱 레이아웃 그룹
│   │       └── (application)/     # 세부 어플리케이션 그룹
│   │           └── +layout.svelte
│   │   └── m/                     # 모바일 라우팅 (/m/*)
│   │       └── (application)/     # 세부 어플리케이션 그룹
│   │           └── +layout.svelte
│   ├── styles/                    # 전역 스타일
│   │   ├── global.css             # 테마 CSS 변수
│   │   └── responsive.css         # 반응형 CSS
│   └── app.html                   # HTML 템플릿
├── static/                        # 정적 파일
└── ...
```

---

## 4. 테마 설정 (Global CSS Variables)

> **중요**: 테마 색상은 반드시 CSS 변수로 관리하여 언제든 변경 가능하게 합니다.
> 다크모드/라이트모드를 지원해야합니다.

> [!CAUTION]
> **스타일 작성 규칙**: 모든 스타일은 반드시 **CSS 파일**에 작성합니다.
> 각 컴포넌트 파일 내에 `<style>` 태그 사용을 **금지**합니다.

### 4.0 CSS 파일 구조 및 규칙

#### CSS 파일 구조

```
src/styles/
├── global.css          # 전역 변수 및 유틸리티 클래스
├── responsive.css      # 반응형 유틸리티
├── components/         # 컴포넌트별 스타일
│   ├── alert.css       # 알림/토스트
│   ├── avatar.css      # 아바타/프로필 이미지
│   ├── badge.css       # 배지/태그
│   ├── button.css      # 버튼
│   ├── card.css        # 카드 레이아웃
│   ├── dropdown.css    # 드롭다운 메뉴
│   ├── form.css        # 폼 요소 (체크박스, 라디오, 스위치)
│   ├── input.css       # 입력 필드
│   ├── modal.css       # 모달
│   ├── skeleton.css    # 스켈레톤 로딩/스피너
│   ├── table.css       # 테이블
│   ├── tab.css         # 탭
│   └── tooltip.css     # 툴팁
├── layouts/            # 레이아웃 스타일
│   ├── sidebar.css
│   ├── header.css
│   ├── topnav.css
│   └── mobile-nav.css
└── pages/              # 페이지별 스타일
    └── ...
```

#### 스타일 작성 규칙

| 올바른 방법             | 잘못된 방법                     |
| ----------------------- | ------------------------------- |
| CSS 파일에 스타일 작성  | 컴포넌트 내 `<style>` 태그 사용 |
| CSS 클래스 사용         | 인라인 스타일 (`style=""`)      |
| CSS 변수 사용 (`var()`) | 하드코딩된 색상값               |

#### 올바른 예시

**CSS 파일** (`src/styles/components/button.css`)

```css
.btn {
	padding: var(--spacing-sm) var(--spacing-md);
	border-radius: var(--radius-md);
	font-size: var(--font-size-sm);
	cursor: pointer;
	transition: all 0.2s ease;
}

.btn-primary {
	background-color: var(--color-primary);
	color: var(--color-text-inverse);
	border: none;
}

.btn-primary:hover {
	background-color: var(--color-primary-hover);
}
```

**Svelte 컴포넌트** (`src/lib/components/Button.svelte`)

```svelte
<script lang="ts">
	export let type: 'primary' | 'secondary' = 'primary';
	export let size: 'sm' | 'md' | 'lg' = 'md';
</script>

<button class="btn btn-{type} btn-{size}">
	<slot />
</button>

<!-- <style> 태그 사용 금지! -->
```

#### CSS 파일 임포트 (`src/app.css` 또는 `+layout.svelte`)

```css
/* src/app.css */
@import './styles/global.css';
@import './styles/responsive.css';

/* Components */
@import './styles/components/button.css';
@import './styles/components/input.css';
@import './styles/components/modal.css';
@import './styles/components/table.css';
@import './styles/components/tab.css';

/* Layouts */
@import './styles/layouts/sidebar.css';
@import './styles/layouts/header.css';
```

### 4.1 전역 CSS 변수 정의 (`src/styles/global.css`)

> 아래는 CSS 변수 구조입니다. 실제 색상값은 프로젝트에 맞게 설정하세요.

```css
:root {
	/* ===== Primary Theme Colors ===== */
	/* 프로젝트 메인 테마 색상을 설정하세요 */
	--color-primary: #YOUR_PRIMARY_COLOR;
	--color-primary-light: #YOUR_PRIMARY_LIGHT;
	--color-primary-dark: #YOUR_PRIMARY_DARK;
	--color-primary-hover: #YOUR_PRIMARY_HOVER;
	--color-primary-rgb: R, G, B; /* opacity 적용용 */

	/* ===== Secondary Colors ===== */
	--color-secondary: #9E9E9E;
	--color-secondary-light: #BDBDBD;
	--color-secondary-dark: #757575;

	/* ===== Status Colors ===== */
	--color-success: #81C784;
	--color-warning: #FFD54F;
	--color-error: #E57373;
	--color-info: #64B5F6;

	/* ===== Background Colors ===== */
	--color-bg-primary: #FAFAFA;
	--color-bg-secondary: #F5F5F5;
	--color-bg-tertiary: #EEEEEE;
	--color-bg-accent: #YOUR_ACCENT_BG; /* 테마 틴트 배경 */

	/* ===== Text Colors ===== */
	--color-text-primary: #424242;
	--color-text-secondary: #757575;
	--color-text-disabled: #BDBDBD;
	--color-text-inverse: #FFFFFF;

	/* ===== Border Colors ===== */
	--color-border: #E0E0E0;
	--color-border-light: #EEEEEE;
	--color-border-focus: var(--color-primary-dark);

	/* ===== Spacing ===== */
	--spacing-xs: 4px;
	--spacing-sm: 8px;
	--spacing-md: 16px;
	--spacing-lg: 24px;
	--spacing-xl: 32px;

	/* ===== Border Radius ===== */
	--radius-sm: 4px;
	--radius-md: 8px;
	--radius-lg: 12px;

	/* ===== Shadow ===== */
	--shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.1);
	--shadow-md: 0 4px 8px rgba(0, 0, 0, 0.1);
	--shadow-lg: 0 8px 16px rgba(0, 0, 0, 0.15);

	/* ===== Font ===== */
	--font-family: 'Pretendard', -apple-system, BlinkMacSystemFont, sans-serif;
	--font-size-xs: 12px;
	--font-size-sm: 14px;
	--font-size-md: 16px;
	--font-size-lg: 18px;
	--font-size-xl: 24px;

	/* ===== Tab Colors ===== */
	--tab-bg: #FFFFFF;
	--tab-bg-active: var(--color-primary);
	--tab-text: var(--color-text-primary);
	--tab-text-active: var(--color-text-inverse);
	--tab-border: var(--color-border);
}
```

### 4.2 테마 사용 예시

```css
.button-primary {
	background-color: var(--color-primary);
	color: var(--color-text-inverse);
}

.button-primary:hover {
	background-color: var(--color-primary-hover);
}
```

### 4.3 유틸리티 클래스 정리

> `global.css`에 정의된 재사용 가능한 유틸리티 클래스입니다.

#### Typography

| 클래스 | 설명 |
|-------|------|
| `.text-xs`, `.text-sm`, `.text-md`, `.text-lg`, `.text-xl` | 폰트 크기 |
| `.font-light`, `.font-medium`, `.font-semibold`, `.font-bold` | 폰트 무게 |
| `.text-primary`, `.text-secondary`, `.text-disabled` | 텍스트 색상 |
| `.text-success`, `.text-warning`, `.text-error`, `.text-info` | 상태 색상 |
| `.text-left`, `.text-center`, `.text-right` | 정렬 |
| `.truncate` | 한 줄 말줄임 |
| `.line-clamp-1`, `.line-clamp-2`, `.line-clamp-3` | 여러 줄 말줄임 |

#### Spacing

| 클래스 | 설명 |
|-------|------|
| `.m-xs/sm/md/lg/xl`, `.mt-`, `.mb-`, `.ml-`, `.mr-` | 마진 |
| `.p-xs/sm/md/lg/xl`, `.pt-`, `.pb-`, `.pl-`, `.pr-` | 패딩 |
| `.gap-xs/sm/md/lg/xl` | Flex/Grid 갭 |

#### Flexbox

| 클래스 | 설명 |
|-------|------|
| `.flex`, `.inline-flex` | 디스플레이 |
| `.flex-col`, `.flex-row`, `.flex-wrap` | 방향 |
| `.items-start/center/end/stretch` | 정렬 (수직) |
| `.justify-start/center/end/between/around` | 정렬 (수평) |
| `.flex-1`, `.flex-auto`, `.flex-none` | 크기 |

#### Layout

| 클래스 | 설명 |
|-------|------|
| `.w-full`, `.h-full`, `.h-screen`, `.min-h-screen` | 크기 |
| `.relative`, `.absolute`, `.fixed`, `.sticky` | 포지션 |
| `.z-0/10/20/30/40/50`, `.z-negative` | Z-Index |
| `.overflow-auto/hidden/scroll` | 오버플로우 |

#### Visual

| 클래스 | 설명 |
|-------|------|
| `.rounded-sm/md/lg/xl/full` | 테두리 라운드 |
| `.shadow-sm/md/lg/xl`, `.shadow-none` | 그림자 |
| `.border`, `.border-t/b/l/r`, `.border-none` | 테두리 |
| `.opacity-0/25/50/75/100` | 투명도 |
| `.blur-sm/md/lg`, `.grayscale` | 필터 |
| `.backdrop-blur-sm/md/lg` | 백드롭 필터 |

#### Transform

| 클래스 | 설명 |
|-------|------|
| `.rotate-45/90/180/270`, `.-rotate-45/90` | 회전 |
| `.scale-90/95/100/105/110/125` | 스케일 |
| `.translate-y-1/2`, `.-translate-y-1/2` | 이동 |

#### Animation

| 클래스 | 설명 |
|-------|------|
| `.transition-all`, `.transition-colors`, `.transition-transform` | 트랜지션 |
| `.animate-fade-in`, `.animate-slide-up`, `.animate-scale-in` | 애니메이션 |
| `.hover-lift`, `.hover-scale` | 호버 효과 |
| `.icon-spin` | 로딩 스피너 |

#### Utility

| 클래스 | 설명 |
|-------|------|
| `.hidden`, `.visible`, `.invisible` | 가시성 |
| `.pointer-events-none/auto` | 포인터 이벤트 |
| `.select-none/text/all` | 사용자 선택 |
| `.cursor-pointer/default/not-allowed` | 커서 |
| `.sr-only` | 스크린 리더 전용 |

### 4.4 로딩 컴포넌트 사용법

#### Spinner (기본 스피너)

```svelte
<script>
  import { Spinner } from '$lib/components';
</script>

<Spinner size="md" color="primary" />
<Spinner size="lg" color="white" type="dots" />
<Spinner type="pulse" />
```

#### PageLoader (페이지 전체 로딩)

```svelte
<script>
  import { PageLoader } from '$lib/components';
  let isLoading = true;
</script>

<PageLoader show={isLoading} text="페이지를 불러오는 중..." />
```

#### LoadingOverlay (비동기 데이터 로딩)

```svelte
<script>
  import { LoadingOverlay } from '$lib/components';
  let isLoading = true;
</script>

<div class="relative">
  <LoadingOverlay show={isLoading} text="데이터 로딩 중..." />
  <!-- 컨텐츠 영역 -->
</div>
```

#### Skeleton (컨텐츠 플레이스홀더)

```svelte
<script>
  import { Skeleton } from '$lib/components';
</script>

<Skeleton type="text" rows={3} />
<Skeleton type="circle" size="lg" />
<Skeleton type="image" />
<Skeleton type="button" width="100px" />
```

#### CSS 클래스로 직접 사용

```html
<!-- 스피너 -->
<span class="spinner spinner-md spinner-primary"></span>

<!-- 페이지 로더 -->
<div class="page-loader">
  <span class="spinner spinner-xl"></span>
  <p class="page-loader-text">Loading...</p>
</div>

<!-- 로딩 오버레이 -->
<div class="loading-overlay">
  <span class="spinner spinner-md"></span>
</div>

<!-- 스켈레톤 -->
<div class="skeleton skeleton-text"></div>
<div class="skeleton skeleton-circle skeleton-avatar-md"></div>
```

---

## 5. 모바일/데스크톱 분리 및 반응형 CSS

> **핵심 규칙**:
>
> - 모바일 페이지는 `/m` 경로로 분리
> - 데스크톱과 모바일 페이지 **모두 개발** 필수
> - 모든 CSS는 **반응형**으로 작성

### 5.1 라우팅 구조

| 플랫폼   | 경로 패턴     | 예시                              |
| -------- | ------------- | --------------------------------- |
| 데스크톱 | `/페이지명`   | `/dashboard`, `/settings`         |
| 모바일   | `/m/페이지명` | `/m/dashboard`, `/m/settings`     |

### 5.2 라우팅 폴더 구조

```
src/routes/
├── +layout.svelte              # 루트 레이아웃
├── +page.svelte                # 홈 (리다이렉트 처리)
├── (desktop)/                  # 데스크톱 그룹
│   ├── +layout.svelte          # 데스크톱 레이아웃 (사이드바, 탭 등)
│   └── [pages]/                # 각 페이지들
└── m/                          # 모바일 라우팅
    ├── +layout.svelte          # 모바일 레이아웃 (하단 네비게이션 등)
    └── [pages]/                # 각 페이지들
```

### 5.3 디바이스 감지 유틸리티 (`src/lib/utils/device.ts`)

```typescript
export interface DeviceInfo {
  isMobile: boolean;
  isTablet: boolean;
  isDesktop: boolean;
  screenWidth: number;
}

export const BREAKPOINTS = {
  mobile: 640,    // 0 ~ 640px
  tablet: 1024,   // 641 ~ 1024px
  desktop: 1025   // 1025px ~
} as const;

export function getDeviceInfo(): DeviceInfo {
  const screenWidth = typeof window !== 'undefined' ? window.innerWidth : 1920;

  return {
    isMobile: screenWidth <= BREAKPOINTS.mobile,
    isTablet: screenWidth > BREAKPOINTS.mobile && screenWidth <= BREAKPOINTS.tablet,
    isDesktop: screenWidth > BREAKPOINTS.tablet,
    screenWidth
  };
}

export function isMobilePath(path: string): boolean {
  return path.startsWith('/m/') || path === '/m';
}

// 데스크톱 경로를 모바일 경로로 변환
export function toMobilePath(path: string): string {
  if (isMobilePath(path)) return path;
  return `/m${path}`;
}

// 모바일 경로를 데스크톱 경로로 변환
export function toDesktopPath(path: string): string {
  if (!isMobilePath(path)) return path;
  return path.replace(/^\/m/, '') || '/';
}
```

### 5.4 자동 리다이렉트 (선택사항)

```typescript
// src/routes/+layout.ts
import { redirect } from '@sveltejs/kit';
import { getDeviceInfo, isMobilePath, toMobilePath, toDesktopPath } from '$lib/utils/device';
import type { LayoutLoad } from './$types';

export const load: LayoutLoad = ({ url }) => {
  // 서버 사이드에서는 리다이렉트 하지 않음 (클라이언트 감지용)
  if (typeof window === 'undefined') return {};

  const device = getDeviceInfo();
  const currentPath = url.pathname;

  // 모바일 디바이스인데 데스크톱 경로에 접근한 경우
  if (device.isMobile && !isMobilePath(currentPath)) {
    throw redirect(302, toMobilePath(currentPath));
  }

  // 데스크톱 디바이스인데 모바일 경로에 접근한 경우
  if (device.isDesktop && isMobilePath(currentPath)) {
    throw redirect(302, toDesktopPath(currentPath));
  }

  return {};
};
```

### 5.5 반응형 CSS (`src/styles/responsive.css`)

```css
/* ===== Breakpoints ===== */
/*
  Mobile:  0 ~ 640px
  Tablet:  641px ~ 1024px
  Desktop: 1025px ~
*/

/* ===== Container ===== */
.container {
	width: 100%;
	margin: 0 auto;
	padding: 0 var(--spacing-md);
}

@media (min-width: 641px) {
	.container {
		max-width: 640px;
	}
}

@media (min-width: 1025px) {
	.container {
		max-width: 1200px;
	}
}

/* ===== Display Utilities ===== */
.hide-mobile {
	display: block;
}

.hide-desktop {
	display: none;
}

.show-mobile-only {
	display: none;
}

.show-desktop-only {
	display: block;
}

@media (max-width: 640px) {
	.hide-mobile {
		display: none !important;
	}

	.show-mobile-only {
		display: block !important;
	}
}

@media (min-width: 1025px) {
	.hide-desktop {
		display: none !important;
	}

	.show-desktop-only {
		display: block !important;
	}
}

/* ===== Grid System ===== */
.grid {
	display: grid;
	gap: var(--spacing-md);
}

.grid-cols-1 {
	grid-template-columns: repeat(1, 1fr);
}
.grid-cols-2 {
	grid-template-columns: repeat(2, 1fr);
}
.grid-cols-3 {
	grid-template-columns: repeat(3, 1fr);
}
.grid-cols-4 {
	grid-template-columns: repeat(4, 1fr);
}

/* Mobile: 1 column */
@media (max-width: 640px) {
	.grid-cols-2,
	.grid-cols-3,
	.grid-cols-4 {
		grid-template-columns: 1fr;
	}
}

/* Tablet: 2 columns max */
@media (min-width: 641px) and (max-width: 1024px) {
	.grid-cols-3,
	.grid-cols-4 {
		grid-template-columns: repeat(2, 1fr);
	}
}

/* ===== Flex Utilities ===== */
.flex-col-mobile {
	display: flex;
	flex-direction: row;
}

@media (max-width: 640px) {
	.flex-col-mobile {
		flex-direction: column;
	}
}

/* ===== Typography ===== */
.text-responsive {
	font-size: var(--font-size-md);
}

@media (max-width: 640px) {
	.text-responsive {
		font-size: var(--font-size-sm);
	}
}

/* ===== Spacing ===== */
.p-responsive {
	padding: var(--spacing-lg);
}

.m-responsive {
	margin: var(--spacing-lg);
}

@media (max-width: 640px) {
	.p-responsive {
		padding: var(--spacing-sm);
	}

	.m-responsive {
		margin: var(--spacing-sm);
	}
}
```

### 5.6 컴포넌트 개발 규칙

> **필수**: 페이지 개발 시 데스크톱과 모바일 버전을 **모두** 개발해야 합니다.

#### 방법 1: 분리된 페이지 (권장)

```
src/routes/
├── (desktop)/[page]/+page.svelte    # 데스크톱 페이지
└── m/[page]/+page.svelte            # 모바일 페이지
```

#### 방법 2: 반응형 단일 컴포넌트 (간단한 페이지용)

```svelte
<script lang="ts">
	import { getDeviceInfo } from '$lib/utils/device';

	let device = getDeviceInfo();
</script>

{#if device.isMobile}
	<!-- 모바일 레이아웃 -->
	<div class="mobile-layout">...</div>
{:else}
	<!-- 데스크톱 레이아웃 -->
	<div class="desktop-layout">...</div>
{/if}
```

### 5.7 레이아웃 차이점

| 요소       | 데스크톱                 | 모바일                     |
| ---------- | ------------------------ | -------------------------- |
| 네비게이션 | 좌측 사이드바            | 하단 탭바 또는 햄버거 메뉴 |
| 탭 관리    | 상단 탭 바 (우클릭 메뉴) | 단일 페이지 (뒤로가기)     |
| 테이블     | 전체 컬럼 표시           | 카드형 또는 축약 표시      |
| 폼         | 가로 배치 가능           | 세로 배치                  |
| 모달       | 중앙 팝업                | 풀스크린 또는 바텀시트     |

### 5.8 개발 체크리스트

- [ ] 데스크톱 페이지 (`/route`) 개발 완료
- [ ] 모바일 페이지 (`/m/route`) 개발 완료
- [ ] 반응형 CSS 적용 (breakpoint 기준 준수)
- [ ] 모바일 터치 인터랙션 고려
- [ ] 데스크톱/모바일 간 기능 동일성 확인

---

## 6. 메뉴 및 라우팅

### 6.1 메뉴 구조

> - **최종**: API를 통해 동적으로 메뉴 목록을 가져옵니다.
> - **초기 테스트**: 백엔드 연결 전까지 정적 라우팅으로 처리합니다.

### 6.2 메뉴 타입 정의 (`src/lib/types/menu.ts`)

```typescript
export interface MenuItem {
  id: string;
  name: string;
  path: string;
  icon?: string;
  children?: MenuItem[];
  order: number;
  isVisible: boolean;
}

export interface MenuState {
  items: MenuItem[];
  isLoading: boolean;
  error: string | null;
}
```

### 6.3 정적 메뉴 (테스트용)

```typescript
// src/lib/config/staticMenu.ts
import type { MenuItem } from '$lib/types/menu';

export const STATIC_MENU: MenuItem[] = [
  {
    id: 'home',
    name: '홈',
    path: '/',
    icon: 'home',
    order: 1,
    isVisible: true
  },
  // 프로젝트에 맞게 메뉴 항목 추가
];
```

---

## 7. 공통 컴포넌트

> **필수 규칙**: Button, Input, Modal, Grid, Table 등은 반드시 공통 컴포넌트를 사용합니다.
> 직접 HTML 요소를 사용하지 않습니다.

### 7.1 컴포넌트 목록

| 컴포넌트         | 파일                    | 용도                       |
| ---------------- | ----------------------- | -------------------------- |
| `Button`         | `Button.svelte`         | 모든 버튼                  |
| `Input`          | `Input.svelte`          | 텍스트 입력                |
| `Modal`          | `Modal.svelte`          | 모달 다이얼로그            |
| `ErrorModal`     | `ErrorModal.svelte`     | 에러 모달 (HTTP 에러 포함) |
| `Grid`           | `Grid.svelte`           | 그리드 레이아웃            |
| `Table`          | `Table.svelte`          | 데이터 테이블              |
| `Select`         | `Select.svelte`         | 셀렉트 박스                |
| `Checkbox`       | `Checkbox.svelte`       | 체크박스                   |
| `Radio`          | `Radio.svelte`          | 라디오 버튼                |
| `Icon`           | `Icon.svelte`           | 아이콘 래퍼 컴포넌트       |
| `Spinner`        | `Spinner.svelte`        | 로딩 스피너                |
| `PageLoader`     | `PageLoader.svelte`     | 페이지 전체 로딩           |
| `LoadingOverlay` | `LoadingOverlay.svelte` | 비동기 데이터 로딩 오버레이|
| `Skeleton`       | `Skeleton.svelte`       | 컨텐츠 플레이스홀더        |

### 7.2 아이콘 사용 규칙

> **필수**: 아이콘은 반드시 **Heroicons** 또는 **Lucide** 라이브러리를 사용합니다.
> 커스텀 SVG 아이콘이나 이미지 아이콘 사용 금지.

#### 설치

```bash
# Lucide (권장)
pnpm add lucide-svelte

# 또는 Heroicons
pnpm add @heroicons/vue  # Vue/Svelte 호환
```

#### Lucide 사용 예시

```svelte
<script lang="ts">
	import { Home, Settings, User, Search, Menu, X, ChevronRight } from 'lucide-svelte';
</script>

<!-- 기본 사용 -->
<Home size={24} />

<!-- 색상 및 스타일 적용 -->
<Settings size={20} color="var(--color-primary)" strokeWidth={2} />

<!-- 버튼 내 아이콘 -->
<Button>
	<Search size={16} />
	검색
</Button>
```

#### 아이콘 래퍼 컴포넌트 (`src/lib/components/Icon.svelte`)

```svelte
<script lang="ts">
	import type { ComponentType } from 'svelte';

	export let icon: ComponentType;
	export let size: number = 24;
	export let color: string = 'currentColor';
	export let strokeWidth: number = 2;
</script>

<svelte:component this={icon} {size} {color} {strokeWidth} />
```

#### 자주 사용하는 아이콘 (Lucide)

| 용도        | 아이콘 이름                                               |
| ----------- | --------------------------------------------------------- |
| 홈/대시보드 | `Home`, `LayoutDashboard`                                 |
| 설정        | `Settings`, `Cog`                                         |
| 사용자      | `User`, `Users`                                           |
| 검색        | `Search`                                                  |
| 메뉴        | `Menu`, `MoreVertical`, `MoreHorizontal`                  |
| 닫기        | `X`, `XCircle`                                            |
| 추가        | `Plus`, `PlusCircle`                                      |
| 삭제        | `Trash2`, `Minus`                                         |
| 수정        | `Edit`, `Pencil`                                          |
| 저장        | `Save`, `Check`                                           |
| 화살표      | `ChevronLeft`, `ChevronRight`, `ChevronUp`, `ChevronDown` |
| 경고/정보   | `AlertCircle`, `AlertTriangle`, `Info`                    |
| 로딩        | `Loader2` (spin 애니메이션 적용)                          |

#### 아이콘 스타일 가이드라인

```css
/* 버튼 내 아이콘 */
.btn-icon {
	display: inline-flex;
	align-items: center;
	gap: var(--spacing-xs);
}

/* 로딩 스피너 */
.icon-spin {
	animation: spin 1s linear infinite;
}

@keyframes spin {
	from {
		transform: rotate(0deg);
	}
	to {
		transform: rotate(360deg);
	}
}
```

### 7.3 컴포넌트 Props 인터페이스 예시

```typescript
// src/lib/types/components.ts
export interface ButtonProps {
  type?: 'primary' | 'secondary' | 'danger' | 'ghost';
  size?: 'sm' | 'md' | 'lg';
  disabled?: boolean;
  loading?: boolean;
  fullWidth?: boolean;
}

export interface InputProps {
  type?: 'text' | 'password' | 'email' | 'number';
  placeholder?: string;
  value?: string;
  disabled?: boolean;
  error?: string;
  required?: boolean;
}

export interface ModalProps {
  isOpen: boolean;
  title: string;
  size?: 'sm' | 'md' | 'lg' | 'xl';
  closable?: boolean;
}
```

---

## 8. API 통신

### 8.1 백엔드 응답 구조

> **중요**: 백엔드에서 넘어오는 모든 응답은 아래 구조를 따릅니다.

```typescript
// src/lib/types/api.ts
export interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  message: string | null;
  errorCode: string | null;
}
```

### 8.2 API 클라이언트 구현 (`src/lib/api/api.ts`)

```typescript
import type { ApiResponse } from '$lib/types/api';
import { showErrorModal } from '$lib/stores/errorStore';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

async function request<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const url = `${BASE_URL}${endpoint}`;

  const defaultHeaders: HeadersInit = {
    'Content-Type': 'application/json',
  };

  const config: RequestInit = {
    ...options,
    headers: {
      ...defaultHeaders,
      ...options.headers,
    },
  };

  try {
    const response = await fetch(url, config);

    // HTTP 에러 처리
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      handleHttpError(response.status, errorData);
      throw new Error(errorData.message || `HTTP ${response.status}`);
    }

    const result: ApiResponse<T> = await response.json();

    // API 응답 에러 처리
    if (!result.success) {
      showErrorModal({
        title: '오류',
        message: result.message || '알 수 없는 오류가 발생했습니다.',
        errorCode: result.errorCode || undefined
      });
      throw new Error(result.message || 'API Error');
    }

    return result.data as T;
  } catch (error) {
    if (error instanceof TypeError) {
      showErrorModal({
        title: '네트워크 오류',
        message: '서버와 연결할 수 없습니다. 네트워크 상태를 확인해주세요.',
      });
    }
    throw error;
  }
}

function handleHttpError(status: number, errorData: any) {
  const errorMessages: Record<number, string> = {
    400: '잘못된 요청입니다.',
    401: '인증이 필요합니다. 다시 로그인해주세요.',
    403: '접근 권한이 없습니다.',
    404: '요청한 리소스를 찾을 수 없습니다.',
    500: '서버 내부 오류가 발생했습니다.',
    502: '서버가 응답하지 않습니다.',
    503: '서비스를 일시적으로 사용할 수 없습니다.',
  };

  showErrorModal({
    title: `HTTP ${status} 오류`,
    message: errorData.message || errorMessages[status] || '알 수 없는 오류가 발생했습니다.',
    errorCode: errorData.errorCode || `HTTP_${status}`
  });
}

// API 메서드
export const api = {
  get: <T>(endpoint: string) => request<T>(endpoint, { method: 'GET' }),

  post: <T>(endpoint: string, data: unknown) =>
    request<T>(endpoint, { method: 'POST', body: JSON.stringify(data) }),

  put: <T>(endpoint: string, data: unknown) =>
    request<T>(endpoint, { method: 'PUT', body: JSON.stringify(data) }),

  patch: <T>(endpoint: string, data: unknown) =>
    request<T>(endpoint, { method: 'PATCH', body: JSON.stringify(data) }),

  delete: <T>(endpoint: string) => request<T>(endpoint, { method: 'DELETE' }),
};

export default api;
```

### 8.3 더미 데이터 (백엔드 연결 전)

> **규칙**: 백엔드 연결 전까지 모든 데이터는 **더미 데이터**로 처리합니다.
> API 연결 시 더미 데이터 코드를 쉽게 교체할 수 있도록 구조화합니다.

#### 더미 데이터 폴더 구조

```
src/lib/
├── api/
│   └── api.ts              # 실제 API 클라이언트
├── mock/                   # 더미 데이터
│   ├── index.ts            # 더미 데이터 익스포트
│   └── [entity].ts         # 엔티티별 더미 데이터
└── services/               # 서비스 레이어
    └── [entity]Service.ts
```

#### 더미 데이터 작성 예시

```typescript
// src/lib/mock/example.ts
import type { Entity } from '$lib/types/entity';

export const MOCK_DATA: Entity[] = [
  {
    id: 1,
    name: '샘플 데이터 1',
    createdAt: '2024-01-01T00:00:00Z'
  },
  // ... 더 많은 샘플 데이터
];

// 더미 API 응답 생성 헬퍼
export function createMockResponse<T>(data: T, delay: number = 300): Promise<T> {
  return new Promise((resolve) => {
    setTimeout(() => resolve(data), delay);
  });
}
```

#### 서비스 레이어 패턴

```typescript
// src/lib/services/entityService.ts
import type { Entity } from '$lib/types/entity';
import { MOCK_DATA, createMockResponse } from '$lib/mock/example';
import api from '$lib/api/api';

// 환경 변수로 더미 모드 제어
const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true';

export const entityService = {
  async getAll(): Promise<Entity[]> {
    if (USE_MOCK) {
      return createMockResponse(MOCK_DATA);
    }
    return api.get<Entity[]>('/entities');
  },

  async getById(id: number): Promise<Entity | null> {
    if (USE_MOCK) {
      const entity = MOCK_DATA.find(e => e.id === id) || null;
      return createMockResponse(entity);
    }
    return api.get<Entity>(`/entities/${id}`);
  },

  // ... 기타 CRUD 메서드
};
```

#### 환경 변수 설정 (`.env`)

```bash
# 개발 환경 (.env.development)
VITE_USE_MOCK=true
VITE_API_BASE_URL=http://localhost:8080/api

# 운영 환경 (.env.production)
VITE_USE_MOCK=false
VITE_API_BASE_URL=https://api.example.com
```

#### 더미 데이터 작성 가이드라인

- [ ] 실제 데이터와 동일한 타입/구조 사용
- [ ] 충분한 양의 샘플 데이터 생성 (최소 5~10개)
- [ ] 다양한 케이스 포함 (정상, 빈 값, 긴 텍스트 등)
- [ ] 한글 데이터로 작성 (실제 사용 환경과 동일하게)
- [ ] 지연 시간 추가 (`setTimeout`)로 실제 네트워크 환경 시뮬레이션

---

## 9. 에러 처리 (Modal)

> **규칙**: 모든 HTTP 에러 및 API 에러는 모달로 표시합니다.

### 9.1 에러 타입 정의

```typescript
// src/lib/types/error.ts
export interface ErrorModalState {
  isOpen: boolean;
  title: string;
  message: string;
  errorCode?: string;
}
```

### 9.2 에러 Store

```typescript
// src/lib/stores/errorStore.ts
import { writable } from 'svelte/store';
import type { ErrorModalState } from '$lib/types/error';

const initialState: ErrorModalState = {
  isOpen: false,
  title: '',
  message: '',
  errorCode: undefined
};

export const errorModal = writable<ErrorModalState>(initialState);

export function showErrorModal(error: Omit<ErrorModalState, 'isOpen'>) {
  errorModal.set({
    ...error,
    isOpen: true
  });
}

export function closeErrorModal() {
  errorModal.set(initialState);
}
```

---

## 10. 탭 기능 (Store)

> **핵심 기능**:
>
> - 탭 전환 시 데이터 유지
> - 탭 우클릭 컨텍스트 메뉴 지원

### 10.1 탭 타입 정의

```typescript
// src/lib/types/tab.ts
export interface Tab {
  id: string;
  title: string;
  path: string;
  closable: boolean;
  data?: Record<string, unknown>;  // 탭별 데이터 저장
}

export interface TabState {
  tabs: Tab[];
  activeTabId: string | null;
}

export interface TabContextMenuAction {
  type: 'close' | 'closeAll' | 'closeOthers' | 'closeLeft' | 'closeRight';
  tabId: string;
}
```

### 10.2 탭 Store (`src/lib/stores/tabStore.ts`)

```typescript
import { writable, derived, get } from 'svelte/store';
import type { Tab, TabState, TabContextMenuAction } from '$lib/types/tab';

const initialState: TabState = {
  tabs: [],
  activeTabId: null
};

function createTabStore() {
  const { subscribe, set, update } = writable<TabState>(initialState);

  return {
    subscribe,

    // 탭 추가 또는 활성화
    openTab: (tab: Omit<Tab, 'data'>) => {
      update(state => {
        const existingTab = state.tabs.find(t => t.id === tab.id);
        if (existingTab) {
          return { ...state, activeTabId: tab.id };
        }
        return {
          tabs: [...state.tabs, { ...tab, data: {} }],
          activeTabId: tab.id
        };
      });
    },

    // 탭 닫기
    closeTab: (tabId: string) => {
      update(state => {
        const tabIndex = state.tabs.findIndex(t => t.id === tabId);
        const newTabs = state.tabs.filter(t => t.id !== tabId);

        let newActiveId = state.activeTabId;
        if (state.activeTabId === tabId && newTabs.length > 0) {
          newActiveId = newTabs[Math.min(tabIndex, newTabs.length - 1)]?.id || null;
        } else if (newTabs.length === 0) {
          newActiveId = null;
        }

        return { tabs: newTabs, activeTabId: newActiveId };
      });
    },

    // 탭 활성화
    setActiveTab: (tabId: string) => {
      update(state => ({ ...state, activeTabId: tabId }));
    },

    // 탭 데이터 저장 (탭 전환 시 데이터 유지용)
    setTabData: (tabId: string, data: Record<string, unknown>) => {
      update(state => ({
        ...state,
        tabs: state.tabs.map(tab =>
          tab.id === tabId ? { ...tab, data: { ...tab.data, ...data } } : tab
        )
      }));
    },

    // 탭 데이터 가져오기
    getTabData: (tabId: string): Record<string, unknown> | undefined => {
      const state = get({ subscribe });
      return state.tabs.find(t => t.id === tabId)?.data;
    },

    // === 컨텍스트 메뉴 액션 ===

    // 전체 닫기
    closeAllTabs: () => {
      update(state => ({
        tabs: state.tabs.filter(t => !t.closable),
        activeTabId: state.tabs.find(t => !t.closable)?.id || null
      }));
    },

    // 해당 탭 제외하고 전체 닫기
    closeOtherTabs: (tabId: string) => {
      update(state => ({
        tabs: state.tabs.filter(t => t.id === tabId || !t.closable),
        activeTabId: tabId
      }));
    },

    // 좌측 전체 닫기
    closeLeftTabs: (tabId: string) => {
      update(state => {
        const tabIndex = state.tabs.findIndex(t => t.id === tabId);
        const newTabs = state.tabs.filter((t, i) => i >= tabIndex || !t.closable);
        return { tabs: newTabs, activeTabId: tabId };
      });
    },

    // 우측 전체 닫기
    closeRightTabs: (tabId: string) => {
      update(state => {
        const tabIndex = state.tabs.findIndex(t => t.id === tabId);
        const newTabs = state.tabs.filter((t, i) => i <= tabIndex || !t.closable);
        return { tabs: newTabs, activeTabId: tabId };
      });
    },

    // 컨텍스트 메뉴 액션 처리
    handleContextAction: (action: TabContextMenuAction) => {
      const actions = {
        close: () => tabStore.closeTab(action.tabId),
        closeAll: () => tabStore.closeAllTabs(),
        closeOthers: () => tabStore.closeOtherTabs(action.tabId),
        closeLeft: () => tabStore.closeLeftTabs(action.tabId),
        closeRight: () => tabStore.closeRightTabs(action.tabId)
      };
      actions[action.type]?.();
    },

    // 초기화
    reset: () => set(initialState)
  };
}

export const tabStore = createTabStore();

// 파생 스토어: 현재 활성 탭
export const activeTab = derived(tabStore, $state =>
  $state.tabs.find(t => t.id === $state.activeTabId)
);
```

### 10.3 탭 컨텍스트 메뉴

```typescript
// src/lib/types/contextMenu.ts
export interface ContextMenuItem {
  id: string;
  label: string;
  action: () => void;
  disabled?: boolean;
  divider?: boolean;
}

export interface ContextMenuState {
  isOpen: boolean;
  x: number;
  y: number;
  items: ContextMenuItem[];
}
```

### 10.4 컨텍스트 메뉴 옵션

| 메뉴 항목                  | 동작                            |
| -------------------------- | ------------------------------- |
| 닫기                       | 해당 탭 닫기                    |
| 전체 닫기                  | 모든 탭 닫기                    |
| 해당 탭 제외하고 전체 닫기 | 선택한 탭만 남기고 닫기         |
| 좌측 전체 닫기             | 선택한 탭 기준 왼쪽 탭들 닫기   |
| 우측 전체 닫기             | 선택한 탭 기준 오른쪽 탭들 닫기 |

---

## 11. TypeScript 규칙

> **필수**: 모든 코드는 TypeScript로 작성하고 interface를 통해 타입을 정의하여 type 에러를 방지합니다.

### 11.1 타입 정의 규칙

- 모든 API 응답에 대해 interface 정의
- 컴포넌트 Props는 반드시 interface로 정의
- `any` 타입 사용 금지
- `unknown` 타입 사용 후 타입 가드 적용

### 11.2 타입 파일 구조

```
src/lib/types/
├── api.ts          # API 관련 타입
├── menu.ts         # 메뉴 관련 타입
├── tab.ts          # 탭 관련 타입
├── error.ts        # 에러 관련 타입
├── components.ts   # 컴포넌트 Props 타입
└── common.ts       # 공통 타입
```

### 11.3 타입 정의 예시

```typescript
// 잘못된 예시
let data: any = {};
function handleData(input) { ... }

// 올바른 예시
interface UserData {
  id: number;
  name: string;
  email: string;
}

let data: UserData | null = null;
function handleData(input: UserData): void { ... }
```

---

## 12. 코딩 컨벤션

### 12.1 파일 명명 규칙

- **컴포넌트**: PascalCase (예: `MyComponent.svelte`)
- **유틸리티/헬퍼**: camelCase (예: `formatDate.ts`)
- **타입 파일**: camelCase (예: `api.ts`)
- **Store 파일**: camelCase + Store 접미사 (예: `tabStore.ts`)

### 12.2 컴포넌트 작성 순서

```svelte
<script lang="ts">
	// 1. Import 문
	import { onMount } from 'svelte';
	import type { ButtonProps } from '$lib/types/components';

	// 2. Props 선언 (interface 포함)
	export let type: ButtonProps['type'] = 'primary';
	export let disabled: boolean = false;

	// 3. 상태 변수
	let isLoading = false;

	// 4. 반응형 선언
	$: buttonClass = `btn btn-${type}`;

	// 5. 함수 정의
	function handleClick(): void {
		// 로직
	}

	// 6. 라이프사이클
	onMount(() => {
		// 초기화 로직
	});
</script>

<!-- 마크업 -->
<button class={buttonClass} {disabled} on:click={handleClick}>
	<slot />
</button>

<!-- 스타일은 별도 CSS 파일에 작성 -->
```

---

## 13. 개발 명령어

```bash
# 의존성 설치
pnpm install

# 개발 서버 실행
pnpm dev

# 프로덕션 빌드
pnpm build

# 린트 검사
pnpm lint

# 코드 포맷팅
pnpm format

# 타입 체크
pnpm check
```

---

## 14. 체크리스트

### 개발 시 확인사항

- [ ] 테마 색상은 CSS 변수 사용 (`var(--color-xxx)`)
- [ ] **스타일은 CSS 파일에만 작성** (컴포넌트 내 `<style>` 태그 금지)
- [ ] Button, Input, Modal 등은 공통 컴포넌트 사용
- [ ] 아이콘은 Lucide/Heroicons 라이브러리 사용
- [ ] API 호출 시 `api.ts`의 메서드 사용
- [ ] 백엔드 연결 전까지 더미 데이터 사용
- [ ] 모든 타입은 interface로 정의
- [ ] 에러는 ErrorModal로 표시
- [ ] 탭 데이터는 tabStore에 저장
- [ ] 데스크톱 페이지 개발 완료 (`/route`)
- [ ] 모바일 페이지 개발 완료 (`/m/route`)
- [ ] 반응형 CSS 적용
- [ ] 대화 진행 후 frontend/doc/work에 진행내역 기록

### 코드 리뷰 체크리스트

- [ ] TypeScript 타입이 올바르게 정의되었는가?
- [ ] `any` 타입을 사용하지 않았는가?
- [ ] 공통 컴포넌트를 사용했는가?
- [ ] **CSS가 별도 파일에 작성되었는가?** (인라인 스타일/`<style>` 태그 없음)
- [ ] CSS 변수를 사용했는가?
- [ ] API 응답 구조를 준수하는가?
- [ ] 데스크톱/모바일 버전 모두 개발되었는가?

---

## 15. 참고 자료

- [Svelte 공식 문서](https://svelte.dev/docs)
- [SvelteKit 공식 문서](https://kit.svelte.dev/docs)
- [TypeScript 핸드북](https://www.typescriptlang.org/docs/)
- [Lucide Icons](https://lucide.dev/icons/)
