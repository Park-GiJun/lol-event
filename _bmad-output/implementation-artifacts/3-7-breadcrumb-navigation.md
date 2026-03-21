# Story 3.7: Breadcrumb 계층 탐색

Status: done

## Story

As a 유저,
I want 상세 페이지 상단의 Breadcrumb으로 상위 계층으로 돌아가기를,
So that 탐색 중 어디 있는지 파악하고 1클릭으로 이전 화면으로 복귀할 수 있다. (FR7)

## Acceptance Criteria

1. **Given** 유저가 챔피언 상세 페이지(`/stats/champion/:champion`)에 있을 때
   **When** Breadcrumb을 확인하면
   **Then** "홈 > 챔피언 > [챔피언명]" 형태로 상단에 표시된다

2. **Given** Breadcrumb의 "홈"을 클릭했을 때
   **When** 클릭이 발생하면
   **Then** 홈(`/`)으로 이동한다

3. **Given** Breadcrumb의 "챔피언"을 클릭했을 때
   **When** 클릭이 발생하면
   **Then** 챔피언 목록 페이지(`/champions`)로 이동한다

4. **Given** 플레이어 상세 페이지(`/player-stats/:riotId`)에도 Breadcrumb이 있을 때
   **When** 구조를 확인하면
   **Then** "홈 > 플레이어 > [플레이어명]" 형태로 표시되며, "플레이어" 클릭 시 `/player-stats`로 이동한다

## Tasks / Subtasks

- [x] Task 1: `BreadcrumbNav.tsx` 신규 생성 (AC: #1, #2, #3, #4)
  - [x] `frontend/src/components/common/BreadcrumbNav.tsx` 신규 생성
  - [x] `{ label: string; path?: string }[]` props 타입 정의 — `path` 없으면 현재 페이지(클릭 불가)
  - [x] shadcn `Breadcrumb`, `BreadcrumbList`, `BreadcrumbItem`, `BreadcrumbLink`, `BreadcrumbPage`, `BreadcrumbSeparator` import
  - [x] `useNavigate` + `asChild` 패턴으로 각 링크 클릭 시 navigate 호출
  - [x] Named export: `export function BreadcrumbNav`

- [x] Task 2: `ChampionStatsPage.tsx` 수정 — breadcrumb 추가 & 기존 `←` 버튼 제거 (AC: #1, #2, #3)
  - [x] `BreadcrumbNav` import 추가
  - [x] `page-header` 위에 `<BreadcrumbNav>` 삽입
  - [x] 챔피언명 breadcrumb item: `nameKo` 우선, fallback `champion` (URL param)
  - [x] 기존 인라인 `←` 버튼 제거 (inline `button` 스타일 블록 삭제)

- [x] Task 3: `PlayerStatsPage.tsx` 수정 — breadcrumb 추가 & 기존 back-btn 제거 (AC: #4)
  - [x] `BreadcrumbNav` import 추가
  - [x] `page-header` 위에 `<BreadcrumbNav>` 삽입
  - [x] 플레이어명 breadcrumb item: `riotId.split('#')[0]` 표시
  - [x] 기존 `<button className="back-btn">` 제거, `ChevronLeft`·`useNavigate` import 정리

- [x] Task 4: CSS 추가 — `stats.css`에 breadcrumb 스타일 (AC: #1, #4)
  - [x] `.breadcrumb-nav` 컨테이너 스타일 (margin-bottom)
  - [x] `.breadcrumb-nav ol` 색상 오버라이드 (`var(--color-text-secondary)`)
  - [x] `.breadcrumb-btn` hover → `var(--color-primary)`
  - [x] `.breadcrumb-nav [aria-current="page"]` → `var(--color-text-primary)` + font-weight 500

- [x] Task 5: TypeScript 검증
  - [x] `npx tsc --noEmit` 오류 없이 통과 확인

## Dev Notes

### Task 1: BreadcrumbNav 전체 구조

**shadcn breadcrumb.tsx 위치**: `frontend/src/components/ui/breadcrumb.tsx`
- 이미 설치됨 — `Breadcrumb`, `BreadcrumbList`, `BreadcrumbItem`, `BreadcrumbLink`, `BreadcrumbPage`, `BreadcrumbSeparator` export
- `BreadcrumbLink`는 `asChild?: boolean` prop 지원 — Slot 패턴으로 button 등 임의 요소를 link처럼 사용 가능
- `BreadcrumbSeparator`는 기본으로 `<ChevronRight />` 렌더링

```tsx
// frontend/src/components/common/BreadcrumbNav.tsx
import { useNavigate } from 'react-router-dom';
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from '../ui/breadcrumb';

interface BreadcrumbNavItem {
  label: string;
  path?: string;  // undefined → 현재 페이지 (BreadcrumbPage, 클릭 불가)
}

export function BreadcrumbNav({ items }: { items: BreadcrumbNavItem[] }) {
  const navigate = useNavigate();
  return (
    <Breadcrumb className="breadcrumb-nav">
      <BreadcrumbList>
        {items.map((item, i) => (
          <BreadcrumbItem key={i}>
            {item.path !== undefined ? (
              <BreadcrumbLink asChild>
                <button onClick={() => navigate(item.path!)}>{item.label}</button>
              </BreadcrumbLink>
            ) : (
              <BreadcrumbPage>{item.label}</BreadcrumbPage>
            )}
            {i < items.length - 1 && <BreadcrumbSeparator />}
          </BreadcrumbItem>
        ))}
      </BreadcrumbList>
    </Breadcrumb>
  );
}
```

**주의**: `<BreadcrumbSeparator />`를 `<BreadcrumbItem>` 안에 넣으면 li > li 중첩 문제가 생길 수 있음.
shadcn 패턴대로 `BreadcrumbItem`과 `BreadcrumbSeparator`를 **형제 요소**로 나열해야 함:

```tsx
// 올바른 패턴 (shadcn 공식):
<BreadcrumbList>
  <BreadcrumbItem>
    <BreadcrumbLink asChild><button onClick={...}>홈</button></BreadcrumbLink>
  </BreadcrumbItem>
  <BreadcrumbSeparator />           {/* BreadcrumbItem의 형제 */}
  <BreadcrumbItem>
    <BreadcrumbLink asChild><button onClick={...}>챔피언</button></BreadcrumbLink>
  </BreadcrumbItem>
  <BreadcrumbSeparator />
  <BreadcrumbItem>
    <BreadcrumbPage>야스오</BreadcrumbPage>
  </BreadcrumbItem>
</BreadcrumbList>
```

**올바른 BreadcrumbNav 구현** (shadcn 공식 패턴 준수):

```tsx
export function BreadcrumbNav({ items }: { items: BreadcrumbNavItem[] }) {
  const navigate = useNavigate();
  return (
    <Breadcrumb className="breadcrumb-nav">
      <BreadcrumbList>
        {items.map((item, i) => (
          <React.Fragment key={i}>
            <BreadcrumbItem>
              {item.path !== undefined ? (
                <BreadcrumbLink asChild>
                  <button className="breadcrumb-btn" onClick={() => navigate(item.path!)}>
                    {item.label}
                  </button>
                </BreadcrumbLink>
              ) : (
                <BreadcrumbPage>{item.label}</BreadcrumbPage>
              )}
            </BreadcrumbItem>
            {i < items.length - 1 && <BreadcrumbSeparator />}
          </React.Fragment>
        ))}
      </BreadcrumbList>
    </Breadcrumb>
  );
}
```
`React.Fragment`를 사용해야 `key` prop을 줄 수 있음. `import React from 'react'` 또는 `import { Fragment } from 'react'` 필요.

### Task 2: ChampionStatsPage 수정

**현재 구조** (`ChampionStatsPage.tsx:206-238`):
```tsx
<div>
  {/* 헤더 */}
  <div className="page-header flex items-center justify-between">
    <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
      <button
        onClick={() => navigate(-1)}
        style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--color-text-secondary)', fontSize: 18, padding: '4px 8px' }}
      >←</button>
      {champImg && <img ... />}
      <div>
        <h1 className="page-title">...</h1>
        ...
      </div>
    </div>
    ...
  </div>
```

**변경 후:**
```tsx
<div>
  {/* Breadcrumb */}
  <BreadcrumbNav items={[
    { label: '홈', path: '/' },
    { label: '챔피언', path: '/champions' },
    { label: (data?.championId && champions.get(data.championId)?.nameKo) || champion },
  ]} />
  {/* 헤더 */}
  <div className="page-header flex items-center justify-between">
    <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
      {/* ← button 제거 */}
      {champImg && <img ... />}
      <div>
        <h1 className="page-title">...</h1>
        ...
      </div>
    </div>
    ...
  </div>
```

챔피언명:
- `data`가 로딩 중이면 `champion` (URL param, 영문)
- `data` 로드 후엔 `champions.get(data.championId)?.nameKo || champion`

`champion` 변수는 이미 `const { champion } = useParams<{ champion: string }>()` 으로 선언되어 있음 (파일 확인 필요).

### Task 3: PlayerStatsPage 수정

**현재 구조** (`PlayerStatsPage.tsx:347-361`):
```tsx
<div>
  <div className="page-header flex items-center justify-between">
    <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
      <button className="back-btn" onClick={() => navigate('/player-stats')}>
        <ChevronLeft size={18} />
      </button>
      <div>
        <h1 className="page-title">{riotId.split('#')[0]}</h1>
        ...
      </div>
    </div>
    ...
  </div>
```

**변경 후:**
```tsx
<div>
  {/* Breadcrumb */}
  <BreadcrumbNav items={[
    { label: '홈', path: '/' },
    { label: '플레이어', path: '/player-stats' },
    { label: riotId.split('#')[0] },
  ]} />
  <div className="page-header flex items-center justify-between">
    <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
      {/* back-btn 제거 */}
      <div>
        <h1 className="page-title">{riotId.split('#')[0]}</h1>
        ...
      </div>
    </div>
    ...
  </div>
```

`back-btn` 제거 후 `ChevronLeft` import 정리 필요:
- `ChevronLeft`가 다른 곳에서 사용되는지 확인 후 미사용이면 import 제거

### Task 4: CSS 스타일

`frontend/src/styles/pages/stats.css` 하단에 추가:

```css
/* ── Breadcrumb Navigation ─────────────────────────────────────── */
.breadcrumb-nav {
  margin-bottom: 12px;
}

/* shadcn BreadcrumbList의 기본 text-muted-foreground 오버라이드 */
.breadcrumb-nav ol {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);  /* 13px */
}

/* BreadcrumbLink 내부 button 스타일 초기화 */
.breadcrumb-btn {
  background: none;
  border: none;
  padding: 0;
  cursor: pointer;
  color: inherit;
  font-size: inherit;
  transition: color 0.15s;
}
.breadcrumb-btn:hover {
  color: var(--color-primary);
}

/* 현재 페이지 (BreadcrumbPage) */
.breadcrumb-nav [aria-current="page"] {
  color: var(--color-text-primary);
  font-weight: 500;
}
```

### 코딩 컨벤션 (엄수)

- **Named export**: `export function BreadcrumbNav` (default 금지)
- **shadcn import 경로**: `../ui/breadcrumb` (BreadcrumbNav는 `common/` 위치)
- **CSS 변수**: 하드코딩 금지, 기존 토큰 활용
- **`ChevronLeft` 정리**: PlayerStatsPage에서 `back-btn` 제거 시 `ChevronLeft` import 불필요하면 제거
- **`navigate` import 불필요 제거**: `ChampionStatsPage`에서 `←` 버튼 제거 후 `navigate`가 다른 곳에서 사용되는지 확인 (모드 변경 등 다른 네비게이션 있으면 유지)

### 파일 위치 규칙

```
frontend/src/
  components/common/
    BreadcrumbNav.tsx          ← 신규
  pages/
    ChampionStatsPage.tsx      ← 수정 (Task 2)
    PlayerStatsPage.tsx        ← 수정 (Task 3)
  styles/pages/
    stats.css                  ← 수정 (Task 4)
```

### References

- `breadcrumb.tsx` (shadcn): `frontend/src/components/ui/breadcrumb.tsx`
- `ChampionStatsPage.tsx`: `frontend/src/pages/ChampionStatsPage.tsx` (line 206-238 헤더)
- `PlayerStatsPage.tsx`: `frontend/src/pages/PlayerStatsPage.tsx` (line 347-361 헤더)
- `stats.css`: `frontend/src/styles/pages/stats.css`
- 참고 패턴: `MatchDetailPage.tsx` — `back-btn` + `navigate('/matches')` (breadcrumb 대상 아님)

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

- `BreadcrumbNav.tsx` 신규: shadcn Breadcrumb 기반, `Fragment` + `BreadcrumbSeparator` 형제 패턴, `asChild`+`button.breadcrumb-btn`으로 SPA navigate 호출
- `ChampionStatsPage.tsx`: `BreadcrumbNav` import 추가; 인라인 `←` 버튼 제거; page-header 위에 `[홈, 챔피언, nameKo||champion]` breadcrumb 삽입; `useNavigate` 유지 (플레이어 링크 클릭에서 사용)
- `PlayerStatsPage.tsx`: `BreadcrumbNav` import 추가; `back-btn` 제거; `ChevronLeft`·`useNavigate` import 정리; page-header 위에 `[홈, 플레이어, riotId.split('#')[0]]` breadcrumb 삽입
- `stats.css`: `.breadcrumb-nav`, `.breadcrumb-btn`, hover/active 스타일 추가
- TypeScript 오류 없음 (`npx tsc --noEmit` 통과)

### File List

- frontend/src/components/common/BreadcrumbNav.tsx (신규)
- frontend/src/pages/ChampionStatsPage.tsx (수정)
- frontend/src/pages/PlayerStatsPage.tsx (수정)
- frontend/src/styles/pages/stats.css (수정)
