# Story 5.2: 모바일 홈 — Elo 리더보드

Status: done

## Story

As a 모바일 유저,
I want 모바일 홈에서 Elo 리더보드를 카드 형태로 확인하기를,
So that 내 순위와 전체 멤버 Elo를 스마트폰에서 불편함 없이 파악할 수 있다. (FR23)

## Acceptance Criteria

1. **Given** 유저가 모바일 홈(`/m`)에 접속했을 때
   **When** 페이지가 로드되면
   **Then** Elo 리더보드가 카드 리스트 형태로 표시된다 (데스크톱 테이블과 다른 모바일 변형)

2. **Given** 자신의 riotId가 리더보드에 있을 때
   **When** 리더보드를 확인하면
   **Then** 자신의 카드가 `rgba(11, 196, 180, 0.08)` 배경 + `var(--color-primary)` 테두리로 하이라이트된다

3. **Given** 리더보드 각 항목을 확인할 때
   **When** 터치 영역을 측정하면
   **Then** 카드 전체 높이가 44px 이상이다 (NFR8 — 기존 `.m-player-card` padding 12px으로 자연 충족)

4. **Given** 로딩 중일 때
   **When** 데이터가 없으면
   **Then** Skeleton 카드 5개가 표시된다 (`Skeleton` 컴포넌트 사용, `LoadingCenter` 금지)

## Tasks / Subtasks

- [x] Task 1: `MobileHomePage.tsx` 신규 생성 (AC: #1, #2, #3, #4)
  - [x] `frontend/src/pages/mobile/MobileHomePage.tsx` 생성
  - [x] `useLeaderboard()` 훅으로 데이터 로딩 (직접 `api.get` 금지)
  - [x] 로딩 시 Skeleton 카드 5개 표시 (`Skeleton` 컴포넌트 사용)
  - [x] 에러 시 `InlineError` + 재시도 버튼
  - [x] 카드 클릭 → `navigate('/m/player/' + encodeURIComponent(riotId))`
  - [x] `localStorage.getItem('lol-event:currentRiotId')` 로 자신 판별 후 하이라이트 적용

- [x] Task 2: 라우팅 변경 (AC: #1)
  - [x] `App.tsx` → `/m` index 라우트를 `MobileHomePage`로 교체
  - [x] `MobileStatsPage`를 `/m/stats` 라우트로 이동
  - [x] `MobileLayout.tsx` TABS에서 '통계' 탭 경로를 `/m/stats`로 변경, '홈' 탭 경로를 `/m`으로 변경 (아이콘: Trophy 사용)

- [x] Task 3: TypeScript 검증
  - [x] `npx tsc --noEmit` 오류 없음 확인

## Dev Notes

### 핵심 원칙 — 바퀴 재발명 금지

| 재발명 위험 요소 | 올바른 접근 |
|---|---|
| `api.get('/stats/elo')` 직접 호출 | **반드시** `useLeaderboard()` 훅 사용 |
| 자체 로딩 스피너 구현 | **반드시** `Skeleton` 컴포넌트 사용 |
| `eloTier()` 함수 새로 작성 | `MobileStatsPage.tsx`의 `eloTier` 패턴 복사 (같은 임계값) |
| 새로운 CSS 클래스 발명 | 기존 `.m-player-card`, `.m-player-rank`, `.m-player-name` 그대로 사용 |

---

### 기존 코드 분석 — 이미 완성된 EloTab 패턴

`MobileStatsPage.tsx`에 `EloTab()` 함수가 **이미 동작하는** Elo 카드 리스트 구현이 있다. 이 컴포넌트를 기반으로 **아래 3가지를 추가**하는 것이 이 스토리의 전부다:

```tsx
// frontend/src/pages/mobile/MobileStatsPage.tsx — 기존 EloTab (참조용)
function EloTab() {
  const navigate = useNavigate();
  const [data, setData] = useState<EloLeaderboardResult | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    api.get<EloLeaderboardResult>('/stats/elo')  // ← 1) useLeaderboard로 교체
      .then(setData)
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <LoadingCenter />;  // ← 2) Skeleton으로 교체
  // ← 3) 자신 하이라이트 없음 → 추가 필요
  ...
}
```

---

### MobileHomePage.tsx 구현 가이드

```tsx
// frontend/src/pages/mobile/MobileHomePage.tsx

import { useNavigate } from 'react-router-dom';
import { useLeaderboard } from '@/hooks/useLeaderboard';
import { Skeleton } from '@/components/common/Skeleton';
import { InlineError } from '@/components/common/InlineError';

const CURRENT_RIOT_ID_KEY = 'lol-event:currentRiotId';  // HomePage.tsx와 동일 키

function eloTier(elo: number): { label: string; color: string } {
  if (elo >= 1300) return { label: 'Challenger', color: '#FFD700' };
  if (elo >= 1200) return { label: 'Master',     color: '#AA47BC' };
  if (elo >= 1100) return { label: 'Diamond',    color: '#0BC4B4' };
  if (elo >= 1000) return { label: 'Platinum',   color: '#4A9EFF' };
  if (elo >= 900)  return { label: 'Gold',       color: '#C89B3C' };
  if (elo >= 800)  return { label: 'Silver',     color: '#A8A8A8' };
  return                  { label: 'Bronze',     color: '#CD7F32' };
}

export function MobileHomePage() {
  const navigate = useNavigate();
  const { data, isLoading, error, refetch } = useLeaderboard();
  const currentRiotId = localStorage.getItem(CURRENT_RIOT_ID_KEY) || undefined;

  // 로딩: Skeleton 5개
  if (isLoading) {
    return (
      <div>
        <p className="m-section-title">Elo 리더보드</p>
        {Array.from({ length: 5 }).map((_, i) => (
          <div key={i} className="m-player-card" style={{ minHeight: 60 }}>
            <div className="m-player-card-header">
              <Skeleton style={{ width: 28, height: 28, borderRadius: '50%', flexShrink: 0 }} />
              <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 6 }}>
                <Skeleton style={{ height: 14, width: '60%', borderRadius: 4 }} />
                <Skeleton style={{ height: 11, width: '30%', borderRadius: 4 }} />
              </div>
              <div style={{ textAlign: 'right', display: 'flex', flexDirection: 'column', gap: 4 }}>
                <Skeleton style={{ height: 18, width: 40, borderRadius: 4 }} />
                <Skeleton style={{ height: 11, width: 28, borderRadius: 4, marginLeft: 'auto' }} />
              </div>
            </div>
          </div>
        ))}
      </div>
    );
  }

  // 에러: InlineError
  if (error) {
    return <InlineError message="리더보드를 불러오지 못했습니다." onRetry={() => void refetch()} />;
  }

  // 빈 데이터
  if (!data || data.players.length === 0) {
    return <div className="m-empty">Elo 데이터가 없습니다</div>;
  }

  return (
    <div>
      <p className="m-section-title">Elo 리더보드</p>
      {data.players.map((p, i) => {
        const tier = eloTier(p.elo);
        const [name, tag] = p.riotId.split('#');
        const isSelf = currentRiotId !== undefined && p.riotId === currentRiotId;
        return (
          <div
            key={p.riotId}
            className="m-player-card"
            style={isSelf ? {
              background: 'rgba(11, 196, 180, 0.08)',
              borderColor: 'var(--color-primary)',
            } : undefined}
            onClick={() => navigate(`/m/player/${encodeURIComponent(p.riotId)}`)}
          >
            <div className="m-player-card-header">
              <div className={`m-player-rank${i < 3 ? ` rank-${i + 1}` : ''}`}>{p.rank}</div>
              <div style={{ flex: 1 }}>
                <span className="m-player-name">{name}</span>
                {tag && <span className="m-player-tag"> #{tag}</span>}
                <div style={{ fontSize: 11, color: tier.color, marginTop: 1 }}>{tier.label}</div>
              </div>
              <div style={{ textAlign: 'right' }}>
                <div style={{ fontSize: 18, fontWeight: 800, color: tier.color }}>{p.elo.toFixed(1)}</div>
                <div style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>{p.games}게임</div>
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
}
```

---

### App.tsx 변경 — 라우팅

```tsx
// 변경 전:
import { MobileStatsPage } from './pages/mobile/MobileStatsPage';
// <Route index element={<MobileStatsPage />} />

// 변경 후 (추가):
import { MobileHomePage } from './pages/mobile/MobileHomePage';
// <Route index element={<MobileHomePage />} />
// <Route path="stats" element={<MobileStatsPage />} />
```

---

### MobileLayout.tsx TABS 변경

```tsx
// 변경 전:
{ to: '/m', icon: BarChart2, label: '통계', end: true }

// 변경 후:
{ to: '/m',       icon: Trophy,    label: '홈',   end: true }
{ to: '/m/stats', icon: BarChart2, label: '통계', end: false }
```

**아이콘**: Lucide `Trophy` 또는 `Home` 중 선택 — 프로젝트 전체에서 이미 사용 중인 아이콘 우선.
`lucide-react`에서 `import { Trophy, BarChart2 } from 'lucide-react'` 또는 다른 적절한 아이콘 사용.

> **주의**: TABS는 현재 5개 (`grid-template-columns: repeat(5, 1fr)`). 탭 개수가 바뀌면 CSS도 함께 수정해야 함. 탭 수를 유지하려면 기존 탭 중 하나를 교체하는 방식으로 구현.

---

### 기존 EloTab 유지 — 수정 금지

`MobileStatsPage.tsx`의 `EloTab`은 **수정하지 않는다**. 이 스토리는 홈 화면을 교체하는 것이지, 기존 stats 페이지를 변경하는 것이 아니다.

---

### CSS 변수 참조

```css
--color-primary: #00B4D8      /* Teal — 하이라이트 테두리, 활성 탭 */
--color-bg-primary: #0A1428
--color-bg-secondary: #0D1B2E
--color-bg-card: #112240
--color-bg-hover: #152035
--color-border: #1E3A5F
--color-text-primary: #E2E8F0
--color-text-secondary: #94A3B8
--color-text-disabled: #475569
```

---

### 타입 참조

```ts
// frontend/src/lib/types/stats.ts
export interface EloLeaderboardResult {
  players: EloRankEntry[];
}

// EloRankEntry 필드 (backend에서 내려주는 값):
// rank: number, riotId: string, elo: number, games: number
```

`EloRankEntry` 타입은 `@/lib/types/stats`에서 import해서 사용한다.

---

### 이전 스토리(5-1) 학습 사항

- CSS 클래스는 반드시 `m-` 접두사 사용
- iOS Safe Area: `MobileLayout.tsx`의 `.m-content`에 이미 `env(safe-area-inset-bottom)` 적용됨 — 신규 페이지에서 별도 처리 불필요
- `TypeScript npx tsc --noEmit` 오류 없음 확인 후 완료 처리

---

### 완료 판단 기준

- [ ] `/m` 접속 시 Elo 리더보드 카드 리스트 즉시 표시
- [ ] localStorage에 `lol-event:currentRiotId` 있으면 해당 카드 하이라이트 표시
- [ ] 로딩 중 Skeleton 카드 5개 표시 (`LoadingCenter` 사용 X)
- [ ] 에러 시 InlineError + 재시도 버튼 표시
- [ ] 카드 클릭 → `/m/player/:riotId` 이동
- [ ] TypeScript 컴파일 오류 없음

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

- `MobileHomePage.tsx` 신규 생성: `useLeaderboard()` 훅 사용, Skeleton 5개 로딩 상태, InlineError 에러 상태, 자신 하이라이트(`rgba(11, 196, 180, 0.08)` + `var(--color-primary)` 테두리), 카드 클릭 시 `/m/player/:riotId` 네비게이션
- `App.tsx`: `/m` index → `MobileHomePage`, `/m/stats` → `MobileStatsPage` 라우트 추가
- `MobileLayout.tsx`: TABS 변경 — `/m` 탭 label '홈' + Trophy 아이콘, `/m/stats` 탭 '통계' + BarChart2 아이콘으로 교체. TITLES에 `/m` → 'Elo 리더보드', `/m/stats` → '통계' 추가
- `npx tsc --noEmit` 오류 없음 확인
- [코드 리뷰 수정] `MobileHomePage.tsx` rank CSS 버그 수정: `i < 3 ? rank-${i+1}` → `p.rank <= 3 ? rank-${p.rank}` (배열 인덱스 대신 실제 rank 값 사용)
- [코드 리뷰 수정] `MobileMorePage.tsx`에 플레이어 항목 추가: `/m/players`가 바텀탭에서 제거되었으므로 더보기 메뉴에서 접근 가능하도록 UserRound + '플레이어' 항목 추가

### File List

- frontend/src/pages/mobile/MobileHomePage.tsx (신규)
- frontend/src/App.tsx (수정 — 라우팅 변경)
- frontend/src/components/layout/MobileLayout.tsx (수정 — TABS 변경)
- frontend/src/pages/mobile/MobileMorePage.tsx (수정 — 플레이어 항목 추가)
