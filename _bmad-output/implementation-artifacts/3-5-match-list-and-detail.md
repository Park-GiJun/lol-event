# Story 3.5: 매치 목록 & 매치 상세 페이지

Status: done

## Story

As a 유저,
I want 매치 목록에서 특정 경기를 클릭하여 상세 내역을 확인하기를,
So that 과거 게임의 양팀 구성과 성적을 파악할 수 있다. (FR6)

## Acceptance Criteria

1. **Given** 유저가 매치 목록 페이지에 있을 때
   **When** 특정 매치 행을 클릭하면
   **Then** `/matches/:matchId` 경로의 매치 상세 페이지로 이동한다

2. **Given** 매치 상세 페이지에 진입했을 때
   **When** 페이지가 로드되면
   **Then** 양팀 플레이어 목록, KDA, 챔피언 정보가 표시된다

3. **Given** 승리팀과 패배팀이 표시될 때
   **When** 팀 결과를 확인하면
   **Then** 승리는 `#10B981` + "승리" 텍스트, 패배는 `#EF4444` + "패배" 텍스트가 병행 표시된다 (NFR7)

4. **Given** 매치 목록 페이지 로딩 중일 때
   **When** API 응답 대기 중이면
   **Then** LoadingCenter 대신 Skeleton UI가 표시된다 (테이블 구조 유지)

## Tasks / Subtasks

- [x] Task 1: `MatchDetailPage.tsx` 신규 생성 (AC: #1, #2, #3)
  - [x] `frontend/src/pages/MatchDetailPage.tsx` 신규 생성
  - [x] URL 파라미터 `matchId` 추출: `useParams<{ matchId: string }>()`
  - [x] API 호출: `api.get<Match>(`/matches/${encodeURIComponent(matchId)}`)`
  - [x] loading/error/data 3단계 분기 처리
  - [x] 페이지 헤더: 뒤로가기 버튼(`<- 목록으로`) + 매치 ID 표시
  - [x] `Scoreboard`, `StatsTab`, `TeamInfoTab` named export import 재사용
  - [x] `<LoadingCenter />` fallback 사용

- [x] Task 2: `MatchesPage.tsx` 수정 — 클릭 동작 변경 (AC: #1)
  - [x] `useNavigate` import 추가
  - [x] `MatchCard`의 `onOpen` 콜백을 `navigate(/matches/:matchId)` 로 변경
  - [x] `MatchesPage` 내 `detail` state 및 `MatchDetailModal` 렌더링 제거
  - [x] `Scoreboard`, `StatsTab`, `TeamInfoTab`, `Tab` named export 추가
  - [x] `MatchDetailModal` export 유지 (PlayerStatsPage에서 사용)

- [x] Task 3: `App.tsx` 수정 — 라우트 추가 (AC: #1)
  - [x] `MatchDetailPage` import 추가
  - [x] `<Route path="matches/:matchId" element={<MatchDetailPage />} />` 추가

- [x] Task 4: `MatchesPage.tsx` 수정 — Skeleton 로딩 개선 (AC: #4)
  - [x] `Skeleton` import 추가, `LoadingCenter` import 제거
  - [x] 3개 Skeleton 그룹 렌더링 (헤더 + 카드: 헤더 + 팀 2열 × 5행)

- [x] Task 5: TypeScript 검증
  - [x] `npx tsc --noEmit` 오류 없이 통과 확인

## Dev Notes

### 🚨 CRITICAL: MatchesPage 기존 구현 현황 — 절대 재구현 금지

**MatchesPage.tsx는 이미 완전 구현된 상태** — 이 파일 전체를 먼저 읽어서 기존 구조를 완전히 파악하라.

이미 구현된 항목 (절대 재구현 금지):
- `MatchDetailModal` — 6개 탭 전체 구현 완료 (`요약`, `딜/피해`, `경제`, `시야/오브젝트`, `멀티킬`, `팀 정보`)
- `Scoreboard` 컴포넌트 — 양팀 플레이어 목록, KDA, 아이템, MVP 표시
- `StatsTab` 컴포넌트 — 딜/피해/경제/시야 탭
- `TeamInfoTab` 컴포넌트 — 팀 오브젝트 정보
- `MatchCard` 컴포넌트 — 개별 경기 카드 (팀 구성, 승패, 시간 표시)
- `MatchGroup` 컴포넌트 — 날짜별 그룹핑
- 모드 필터 (`5v5 내전`, `칼바람`, `전체`)
- 경기 삭제 기능
- CSS: `frontend/src/styles/pages/matches.css` 전체

### Task 2 핵심: MatchesPage 수정 접근법

**현재 구조:**
```tsx
// MatchesPage 내부 (현재)
const [detail, setDetail] = useState<Match | null>(null);
// ...
<MatchCard ... onOpen={() => setDetail(match)} />
// ...
{detail && <MatchDetailModal match={detail} onClose={() => setDetail(null)} />}
```

**변경 후:**
```tsx
// MatchesPage 내부 (변경)
const navigate = useNavigate(); // 이미 import 여부 확인 필요
// detail state 제거
// ...
<MatchCard ... onOpen={() => navigate(`/matches/${encodeURIComponent(match.matchId)}`)} />
// MatchDetailModal 렌더링 부분 제거
```

**내보내야 할 컴포넌트 (named export 추가):**
```tsx
// MatchesPage.tsx 내 함수 앞에 export 키워드 추가
export function Scoreboard(...) { ... }
export function StatsTab(...) { ... }
export function TeamInfoTab(...) { ... }
```

### Task 1 핵심: MatchDetailPage 전체 구조

```tsx
// frontend/src/pages/MatchDetailPage.tsx
import { useParams, useNavigate } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { ChevronLeft } from 'lucide-react';
import { api } from '../lib/api/api';
import type { Match } from '../lib/types/match';
import { LoadingCenter } from '../components/common/Spinner';
import { Scoreboard, StatsTab, TeamInfoTab } from './MatchesPage';
import '../styles/pages/matches.css';

const TABS = ['요약', '딜/피해', '경제', '시야/오브젝트', '멀티킬', '팀 정보'] as const;
type Tab = typeof TABS[number];

const QUEUE_LABEL: Record<number, string> = { 0: '커스텀', 3130: '5v5 내전', 3270: '칼바람' };

function fmt(secs: number) {
  const m = Math.floor(secs / 60);
  const s = secs % 60;
  return `${m}:${String(s).padStart(2, '0')}`;
}

export function MatchDetailPage() {
  const { matchId: encodedId } = useParams<{ matchId: string }>();
  const matchId = decodeURIComponent(encodedId ?? '');
  const navigate = useNavigate();
  const [data, setData] = useState<Match | null>(null);
  const [loading, setLoading] = useState(true);
  const [tab, setTab] = useState<Tab>('요약');

  useEffect(() => {
    setLoading(true);
    api.get<Match>(`/matches/${encodeURIComponent(matchId)}`)
      .then(setData)
      .finally(() => setLoading(false));
  }, [matchId]);

  return (
    <div>
      <div className="page-header flex items-center justify-between">
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <button className="back-btn" onClick={() => navigate('/matches')}>
            <ChevronLeft size={18} />
          </button>
          <div>
            <h1 className="page-title">
              {data ? (QUEUE_LABEL[data.queueId] ?? data.queueId) : '경기 상세'}
            </h1>
            {data && (
              <p className="page-subtitle">
                {fmt(data.gameDuration)} · {new Date(data.gameCreation).toLocaleDateString('ko-KR')}
              </p>
            )}
          </div>
        </div>
      </div>

      {loading ? (
        <LoadingCenter />
      ) : !data ? (
        <div style={{ textAlign: 'center', padding: '60px 0', color: 'var(--color-text-secondary)' }}>
          경기 데이터를 불러올 수 없습니다.
        </div>
      ) : (
        <div className="card">
          <div className="match-tabs">
            {TABS.map(t => (
              <button key={t} className={`match-tab ${tab === t ? 'active' : ''}`} onClick={() => setTab(t)}>
                {t}
              </button>
            ))}
          </div>
          {tab === '요약'     && <Scoreboard match={data} />}
          {tab === '팀 정보' && <TeamInfoTab teams={data.teams ?? []} />}
          {(tab === '딜/피해' || tab === '경제' || tab === '시야/오브젝트' || tab === '멀티킬') && (
            <StatsTab match={data} tab={tab} />
          )}
        </div>
      )}
    </div>
  );
}
```

### Task 3: App.tsx 라우트 추가

**현재 App.tsx 구조 확인 필요.** `matches` 라우트 아래에 추가:
```tsx
import { MatchDetailPage } from './pages/MatchDetailPage';
// ...
<Route path="matches" element={<MatchesPage />} />
<Route path="matches/:matchId" element={<MatchDetailPage />} />
```

### Task 4: Skeleton 로딩 구조

매치 카드 3개 + 헤더 Skeleton:
```tsx
{loading && (
  <div>
    {Array.from({ length: 3 }).map((_, i) => (
      <div key={i} className="card" style={{ marginBottom: 16 }}>
        <div className="match-card-header">
          <Skeleton className="h-4 w-32" />
          <Skeleton className="h-4 w-16" />
        </div>
        <div className="match-teams">
          <div className="match-team">
            {Array.from({ length: 5 }).map((_, j) => (
              <div key={j} className="match-player-row">
                <Skeleton className="h-6 w-6 rounded" />
                <Skeleton className="h-4 w-24" />
              </div>
            ))}
          </div>
          <div style={{ display: 'flex', alignItems: 'center', padding: '0 8px' }}>
            <Skeleton className="h-8 w-8 rounded-full" />
          </div>
          <div className="match-team">
            {Array.from({ length: 5 }).map((_, j) => (
              <div key={j} className="match-player-row">
                <Skeleton className="h-6 w-6 rounded" />
                <Skeleton className="h-4 w-24" />
              </div>
            ))}
          </div>
        </div>
      </div>
    ))}
  </div>
)}
```

### 승패 색상 확인 (NFR7)

CSS 변수 `var(--color-win)` = `#10B981`, `var(--color-loss)` = `#EF4444`.
기존 `matches.css`의 `.win` / `.loss` 클래스가 이 값을 사용하는지 확인. 사용하지 않는다면 CSS 변수 방식으로 수정:
```css
/* matches.css 확인 사항 */
.sb-result.win { color: var(--color-win); }
.sb-result.loss { color: var(--color-loss); }
```

### StatsTab의 Tab 타입 공유

`StatsTab`은 현재 `MatchesPage.tsx` 내부에서 정의된 `Tab` 타입을 props로 받는다. `export function StatsTab` 시 props 타입도 같이 export하거나, `tab` prop 타입을 인라인으로 정의:

```tsx
// StatsTab props 타입 (named export 시)
export function StatsTab({ match, tab }: {
  match: Match;
  tab: '딜/피해' | '경제' | '시야/오브젝트' | '멀티킬';
}) { ... }
```

### 코딩 컨벤션 (엄수)

- **Named export**: `export function MatchDetailPage()` (default 금지)
- **import type**: `import type { Match }` — 타입 전용 import
- **CSS 변수**: `var(--color-win)`, `var(--color-loss)` — 하드코딩 금지
- **matches.css 재사용**: 신규 CSS 클래스 추가 최소화, 기존 클래스 활용
- **`encodeURIComponent`**: matchId URL 파라미터 encode/decode 일관성 유지

### 기존 패턴 참조

- `PlayerStatsPage.tsx` — `useParams` + `useNavigate` + back button 패턴
- `ChampionStatsPage.tsx` — lazy import 패턴 (차트 없으므로 불필요)
- `MatchDetailModal` — **이미 완전 구현**, Tab 상태/Scoreboard/StatsTab 패턴 그대로 복사하지 말고 재사용

### 파일 위치 규칙

```
frontend/src/
  pages/
    MatchesPage.tsx          ← 수정 (Task 2, 4)
    MatchDetailPage.tsx      ← 신규 (Task 1)
  App.tsx                    ← 수정 (Task 3)
```

### References

- `Match` 타입: `frontend/src/lib/types/match.ts`
- `MatchDetailModal`, `Scoreboard`, `StatsTab`, `TeamInfoTab`: `frontend/src/pages/MatchesPage.tsx`
- `PlayerStatsPage.tsx`: `MatchDetailModal` 재사용 사례 (수정 불필요)
- `Skeleton` 컴포넌트: `frontend/src/components/common/Skeleton.tsx`
- `matches.css`: `frontend/src/styles/pages/matches.css`
- `App.tsx`: `frontend/src/App.tsx`

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

- `MatchesPage.tsx`: `Tab`, `Scoreboard`, `StatsTab`, `TeamInfoTab` named export 추가; `useNavigate` import; `detail` state 제거; 클릭 → `navigate('/matches/:matchId')`로 전환; `LoadingCenter` → Skeleton 3-카드 구조로 교체
- `MatchDetailPage.tsx` 신규 생성: `useParams(matchId)` + API 호출 + 탭 렌더링; `Scoreboard`/`StatsTab`/`TeamInfoTab` import 재사용; 뒤로가기 버튼(`/matches`)
- `App.tsx`: `matches/:matchId` 라우트 추가
- `MatchDetailModal` export 유지 — PlayerStatsPage 재사용 보호
- TypeScript 오류 없음 (`npx tsc --noEmit` 통과)

### File List

- frontend/src/pages/MatchDetailPage.tsx (신규)
- frontend/src/pages/MatchesPage.tsx (수정)
- frontend/src/App.tsx (수정)
