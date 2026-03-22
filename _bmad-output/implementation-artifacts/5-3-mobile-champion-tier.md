# Story 5.3: 모바일 챔피언 티어표

Status: done

## Story

As a 모바일 유저,
I want 모바일에서 챔피언 탭을 눌러 S/A/B/C 티어표를 확인하기를,
So that 이번 리그 메타 챔피언을 스마트폰에서 쉽게 파악할 수 있다. (FR24)

## Acceptance Criteria

1. **Given** 유저가 모바일 챔피언 탭(`/m/champions`)을 눌렀을 때
   **When** MobileChampionListPage가 로드되면
   **Then** 챔피언 티어표가 S/A/B/C 섹션 구조로 모바일 최적화 형태로 표시된다

2. **Given** 챔피언 목록이 표시될 때
   **When** 각 항목의 터치 영역을 확인하면
   **Then** 행 높이가 44px 이상이다 (NFR8)

3. **Given** 승률이 표시될 때
   **When** 수치를 확인하면
   **Then** 50% 이상은 `#10B981`(win color), 미만은 `#EF4444`(loss color) + 텍스트 레이블 병행 표시 (NFR7)

## Tasks / Subtasks

- [x] Task 1: `MobileChampionListPage.tsx` 교체 — 기존 grid → tier table (AC: #1, #2, #3)
  - [x] `useChampions()` + `useDragon()` 두 훅 모두 사용
  - [x] S/A/B/C 섹션별 접기/펼치기 (기본값: 모두 펼침)
  - [x] Skeleton 로딩 상태 (각 섹션 3행 placeholder)
  - [x] InlineError + refetch 버튼 (에러 상태)
  - [x] 각 챔피언 row: 아이콘(32px) + 이름 + 승률 색상 표시
  - [x] 챔피언 클릭 → `navigate('/m/champion/' + encodeURIComponent(championKey))`
  - [x] `topPickedChampions` 빈 경우 "챔피언 데이터가 없습니다" 빈 상태 표시

- [x] Task 2: TypeScript 검증
  - [x] `npx tsc --noEmit` 오류 없음 확인

## Dev Notes

### 핵심 원칙 — 바퀴 재발명 금지

| 재발명 위험 요소 | 올바른 접근 |
|---|---|
| 티어 분류 로직 새로 작성 | 데스크톱 `ChampionTierTable.tsx`의 `getChampionTier` 로직 그대로 복사 |
| 챔피언 데이터 직접 API 호출 | **반드시** `useChampions()` 훅 사용 |
| 티어 색상 하드코딩 | `TIER_COLORS` 상수 import (`@/lib/constants/theme`) |
| 챔피언 이미지/이름 | `useDragon()` 훅 — `champions.get(championId)` |

---

### 기존 구현 현황 — 현재 MobileChampionListPage.tsx

현재 `MobileChampionListPage.tsx`는 **DataDragon 전체 챔피언 그리드**를 표시한다 (검색+4열 grid). 이것을 **통계 기반 티어표**로 완전히 교체해야 한다.

```
현재: useDragon() 전체 챔피언 그리드
교체: useChampions() 통계 + useDragon() 이미지/이름 → 티어 섹션별 리스트
```

> **주의**: 기존 검색 기능(`m-search`, `m-champ-grid`)은 제거해도 된다. 티어표가 이 페이지의 주목적이다.

---

### 데스크톱 ChampionTierTable.tsx 핵심 로직 참조

`frontend/src/components/dashboard/ChampionTierTable.tsx`가 데스크톱 티어표의 완성 구현체다.

**그대로 재사용할 로직:**

```ts
// TIER_ORDER, getChampionTier — 동일 임계값 사용
import { TIER_COLORS } from '@/lib/constants/theme';
import type { TierKey } from '@/lib/constants/theme';

const TIER_ORDER: TierKey[] = ['S', 'A', 'B', 'C'];

function getChampionTier(winRate: number): TierKey {
  if (winRate >= 60) return 'S';
  if (winRate >= 55) return 'A';
  if (winRate >= 50) return 'B';
  return 'C';
}
```

**TIER_COLORS 상수** (`@/lib/constants/theme`):
```ts
S: '#FFD700'  // 골드
A: '#00B4D8'  // Teal (= color-primary)
B: '#8899BB'  // 스틸 블루
C: '#4A5568'  // 그레이
```

**useChampions() 훅 반환 타입** (`OverviewStats`):
```ts
interface OverviewStats {
  matchCount: number;           // 전체 경기 수 (픽률 계산에 사용)
  topPickedChampions: ChampionPickStat[];  // 티어표 데이터
  ...
}

interface ChampionPickStat {
  champion: string;     // 챔피언 키 (예: "Yasuo")
  championId: number;   // Dragon ID (useDragon에서 이미지/이름 조회)
  picks: number;        // 픽 횟수
  wins: number;
  winRate: number;      // 0~100 범위
}
```

**useDragon()으로 이미지/이름 조회:**
```ts
const { champions: dragonChampions } = useDragon();
const dragon = dragonChampions.get(entry.championId);  // Map<number, ChampionData>
const displayName = dragon?.nameKo ?? entry.champion;
const imgUrl = dragon?.imageUrl ?? null;
const championKey = dragon?.championKey ?? entry.champion;  // navigate용
```

---

### MobileChampionListPage.tsx 구현 가이드

```tsx
import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useChampions } from '@/hooks/useChampions';
import { useDragon } from '@/context/DragonContext';
import { Skeleton } from '@/components/common/Skeleton';
import { InlineError } from '@/components/common/InlineError';
import { TIER_COLORS } from '@/lib/constants/theme';
import type { TierKey } from '@/lib/constants/theme';
import type { ChampionPickStat } from '@/lib/types/stats';

const TIER_ORDER: TierKey[] = ['S', 'A', 'B', 'C'];

function getChampionTier(winRate: number): TierKey {
  if (winRate >= 60) return 'S';
  if (winRate >= 55) return 'A';
  if (winRate >= 50) return 'B';
  return 'C';
}

export function MobileChampionListPage() {
  const navigate = useNavigate();
  const { data, isLoading, error, refetch } = useChampions();
  const { champions: dragonChampions } = useDragon();
  const [expanded, setExpanded] = useState<Record<TierKey, boolean>>(
    { S: true, A: true, B: true, C: true }
  );

  const tierGroups = useMemo(() => {
    if (!data) return [];
    return TIER_ORDER
      .map(tier => ({
        tier,
        champions: data.topPickedChampions.filter(
          (c: ChampionPickStat) => getChampionTier(c.winRate) === tier
        ),
      }))
      .filter(g => g.champions.length > 0);
  }, [data]);

  // 로딩
  if (isLoading) {
    return (
      <div>
        {['S', 'A'].map(tier => (
          <div key={tier} style={{ marginBottom: 16 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '8px 4px', marginBottom: 8 }}>
              <Skeleton style={{ width: 28, height: 22, borderRadius: 4 }} />
              <Skeleton style={{ width: 60, height: 16, borderRadius: 4 }} />
            </div>
            {Array.from({ length: 3 }).map((_, i) => (
              <div key={i} className="m-player-card" style={{ minHeight: 44 }}>
                <div className="m-player-card-header">
                  <Skeleton style={{ width: 32, height: 32, borderRadius: 4, flexShrink: 0 }} />
                  <Skeleton style={{ flex: 1, height: 14, borderRadius: 4, marginLeft: 10 }} />
                  <Skeleton style={{ width: 40, height: 14, borderRadius: 4 }} />
                </div>
              </div>
            ))}
          </div>
        ))}
      </div>
    );
  }

  // 에러
  if (error) {
    return <InlineError message="챔피언 티어표를 불러오지 못했습니다." onRetry={() => void refetch()} />;
  }

  // 빈 데이터
  if (!data || data.topPickedChampions.length === 0) {
    return <div className="m-empty">챔피언 데이터가 없습니다</div>;
  }

  return (
    <div>
      {tierGroups.map(({ tier, champions }) => {
        const tierColor = TIER_COLORS[tier];
        const isExpanded = expanded[tier];

        return (
          <div key={tier} style={{ marginBottom: 16 }}>
            {/* 티어 섹션 헤더 — 탭 가능 */}
            <button
              type="button"
              style={{
                display: 'flex', alignItems: 'center', gap: 8,
                width: '100%', textAlign: 'left',
                padding: '10px 4px', background: 'none', border: 'none', cursor: 'pointer',
                minHeight: 44,  // AC2 충족
              }}
              onClick={() => setExpanded(prev => ({ ...prev, [tier]: !prev[tier] }))}
            >
              <span style={{
                fontSize: 11, fontWeight: 700, color: tierColor,
                background: tierColor + '18', borderRadius: 4, padding: '2px 8px',
                border: `1px solid ${tierColor}66`,
              }}>
                {tier}
              </span>
              <span style={{ fontSize: 14, fontWeight: 600, color: tierColor }}>
                {tier} 티어
              </span>
              <span style={{ fontSize: 11, color: 'var(--color-text-secondary)', marginLeft: 4 }}>
                ({champions.length})
              </span>
              <span style={{ marginLeft: 'auto', color: 'var(--color-text-secondary)', fontSize: 12 }}>
                {isExpanded ? '▲' : '▼'}
              </span>
            </button>

            {/* 챔피언 목록 */}
            {isExpanded && champions.map((entry: ChampionPickStat) => {
              const dragon = dragonChampions.get(entry.championId);
              const displayName = dragon?.nameKo ?? entry.champion;
              const imgUrl = dragon?.imageUrl ?? null;
              const championKey = dragon?.championKey ?? entry.champion;
              const wrColor = entry.winRate >= 50 ? '#10B981' : '#EF4444';
              const wrLabel = entry.winRate >= 50 ? '승' : '패';  // NFR7 텍스트 레이블 병행

              return (
                <div
                  key={entry.championId}
                  className="m-player-card"
                  style={{ minHeight: 44 }}  // AC2
                  onClick={() => navigate(`/m/champion/${encodeURIComponent(championKey)}`)}
                >
                  <div className="m-player-card-header">
                    {/* 챔피언 이미지 */}
                    {imgUrl ? (
                      <img
                        src={imgUrl}
                        alt={displayName}
                        width={32}
                        height={32}
                        style={{ borderRadius: 4, border: '1px solid var(--color-border)', objectFit: 'cover', flexShrink: 0 }}
                        onError={e => { (e.currentTarget as HTMLImageElement).style.display = 'none'; }}
                      />
                    ) : (
                      <div style={{
                        width: 32, height: 32, borderRadius: 4,
                        background: 'var(--color-bg-hover)',
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                        fontSize: 10, color: 'var(--color-text-secondary)', flexShrink: 0,
                      }}>
                        {displayName.slice(0, 2)}
                      </div>
                    )}

                    {/* 챔피언 이름 */}
                    <span className="m-player-name" style={{ flex: 1, marginLeft: 10 }}>
                      {displayName}
                    </span>

                    {/* 승률 + 텍스트 레이블 (NFR7) */}
                    <div style={{ textAlign: 'right' }}>
                      <span style={{ fontSize: 14, fontWeight: 700, color: wrColor }}>
                        {entry.winRate.toFixed(1)}%
                      </span>
                      <span style={{ fontSize: 10, color: wrColor, marginLeft: 4 }}>
                        {wrLabel}
                      </span>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        );
      })}
    </div>
  );
}
```

---

### CSS 변수 & 클래스 참조

```css
/* 재사용 CSS 클래스 */
.m-player-card        /* 카드 컨테이너, padding:12px, min-height는 style로 추가 */
.m-player-card-header /* 카드 내부 flex row */
.m-player-name        /* 플레이어 이름 — 챔피언 이름에도 동일 적용 */
.m-empty              /* "데이터 없음" 텍스트 */

/* 승률 색상 */
--color-win: #10B981   (= AC3 조건: 50% 이상)
--color-loss: #EF4444  (= AC3 조건: 50% 미만)
/* CSS 변수 대신 하드코딩 가능: '#10B981' / '#EF4444' */
```

---

### 이전 스토리(5-2) 학습 사항

- `useChampions()` 훅 사용 필수 — 직접 `api.get` 금지
- Skeleton 컴포넌트 사용 필수 — `LoadingCenter` 금지
- `m-player-card` + `minHeight: 44` 로 44px 터치 영역 충족
- TypeScript `npx tsc --noEmit` 오류 없음 확인 후 완료

---

### 완료 판단 기준

- [ ] `/m/champions` 접속 시 S/A/B/C 섹션 티어표 표시
- [ ] 각 챔피언 row 높이 44px 이상
- [ ] 승률 ≥50% → 녹색(`#10B981`) + "승" 레이블, <50% → 빨간색(`#EF4444`) + "패" 레이블
- [ ] 로딩 중 Skeleton 표시 (LoadingCenter 아님)
- [ ] 에러 시 InlineError + 재시도
- [ ] 챔피언 클릭 → `/m/champion/:key` 이동
- [ ] TypeScript 컴파일 오류 없음

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

- `MobileChampionListPage.tsx` 완전 교체: 기존 DataDragon 전체 챔피언 그리드(검색+4열) → 통계 기반 S/A/B/C 티어 섹션 리스트
- `useChampions()` + `useDragon()` 두 훅 조합 사용 (`topPickedChampions` 통계 + Dragon 이미지/이름)
- `TIER_COLORS`, `TierKey` import → 데스크톱 `ChampionTierTable.tsx`와 동일 임계값·색상 사용
- `getChampionTier()`: S≥60%, A≥55%, B≥50%, C<50%
- 승률 색상 + 텍스트 레이블: ≥50% → `#10B981`+"승", <50% → `#EF4444`+"패" (NFR7 충족)
- 각 row `minHeight: 44` 명시적 설정 (NFR8 충족)
- Skeleton 로딩(S/A 두 섹션 × 3행 placeholder), InlineError, 빈 상태 처리
- `npx tsc --noEmit` 오류 없음
- [코드 리뷰 수정] `ChampionRow` 컴포넌트 추출 — per-row `imgError` state로 이미지 오류 시 fallback div 표시 (#4)
- [코드 리뷰 수정] 챔피언 row에 `role="button"`, `tabIndex={0}`, `onKeyDown` 추가 — 키보드/접근성 (#3)
- [코드 리뷰 수정] `championKey` 빈 문자열 방어: `|| String(entry.championId)` fallback 추가 (#5)
- [코드 리뷰 수정] Skeleton 섹션 헤더 `<div>`에 `minHeight: 44` 추가 (NFR8, #6)

### File List

- frontend/src/pages/mobile/MobileChampionListPage.tsx (수정 — 기존 grid → tier table)
