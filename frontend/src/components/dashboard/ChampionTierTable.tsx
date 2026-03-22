import { useMemo, useState } from 'react';
import { useChampions } from '@/hooks/useChampions';
import { useSortTable } from '@/hooks/useSortTable';
import { SortableTh } from '@/components/common/SortableTh';
import { InlineError } from '@/components/common/InlineError';
import { Skeleton } from '@/components/common/Skeleton';
import { ChampionLink } from '@/components/common/ChampionLink';
import { useDragon } from '@/context/DragonContext';
import { TIER_COLORS } from '@/lib/constants/theme';
import type { TierKey } from '@/lib/constants/theme';
import type { ChampionPickStat } from '@/lib/types/stats';

// ── 내부 유틸 ────────────────────────────────────────────────

const TIER_ORDER: TierKey[] = ['S', 'A', 'B', 'C'];

function getChampionTier(winRate: number): TierKey {
  if (winRate >= 60) return 'S';
  if (winRate >= 55) return 'A';
  if (winRate >= 50) return 'B';
  return 'C';
}

type SortCol = 'picks' | 'winRate' | 'pickRate';

function ColGroup() {
  return (
    <colgroup>
      <col />
      <col style={{ width: 80 }} />
      <col style={{ width: 74 }} />
      <col style={{ width: 60 }} />
    </colgroup>
  );
}

// ── 컴포넌트 ─────────────────────────────────────────────────

export function ChampionTierTable() {
  const { data, isLoading, error, refetch } = useChampions();
  const { champions: dragonChampions } = useDragon();
  const { sortKey, sortDir, handleSort, sorted } = useSortTable<SortCol>('winRate');

  // 기본: 모두 닫힘
  const [expanded, setExpanded] = useState<Record<TierKey, boolean>>({
    S: false, A: false, B: false, C: false,
  });

  const tierGroups = useMemo(() => {
    if (!data) return [];
    return TIER_ORDER
      .map(tier => ({
        tier,
        champions: data.topPickedChampions.filter(
          (c: ChampionPickStat) => getChampionTier(c.winRate) === tier,
        ),
      }))
      .filter(g => g.champions.length > 0);
  }, [data]);

  if (isLoading) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        {[3, 2].map((rowCount, si) => (
          <div key={si}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '8px 4px' }}>
              <Skeleton className="h-5 w-7 rounded-sm" />
              <Skeleton className="h-4 w-16" />
            </div>
            <div className="table-wrapper">
              <table className="table member-stats-table">
                <ColGroup />
                <thead>
                  <tr>
                    <th>챔피언</th>
                    <th className="table-number">승률</th>
                    <th className="table-number">픽률</th>
                    <th className="table-number">게임</th>
                  </tr>
                </thead>
                <tbody>
                  {Array.from({ length: rowCount }).map((_, i) => (
                    <tr key={i}>
                      <td>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                          <Skeleton className="h-8 w-8 rounded" />
                          <Skeleton className="h-4 w-20" />
                        </div>
                      </td>
                      <td className="table-number"><Skeleton className="h-4 w-12 ml-auto" /></td>
                      <td className="table-number"><Skeleton className="h-4 w-10 ml-auto" /></td>
                      <td className="table-number"><Skeleton className="h-4 w-8 ml-auto" /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        ))}
      </div>
    );
  }

  if (error) {
    return <InlineError message="챔피언 티어표를 불러오지 못했습니다." onRetry={() => void refetch()} />;
  }

  if (!data || data.topPickedChampions.length === 0) {
    return (
      <p className="text-sm p-4" style={{ color: 'var(--color-text-secondary)' }}>
        챔피언 데이터가 없습니다.
      </p>
    );
  }

  const getValue = (key: SortCol, c: ChampionPickStat & { pickRate: number }): number => {
    if (key === 'winRate')  return c.winRate;
    if (key === 'picks')    return c.picks;
    if (key === 'pickRate') return c.pickRate;
    return 0;
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
      {tierGroups.map(({ tier, champions }) => {
        const tierColor = TIER_COLORS[tier];
        const isExpanded = expanded[tier];

        const enriched = champions.map(c => ({
          ...c,
          pickRate: data.matchCount > 0 ? (c.picks / data.matchCount) * 100 : 0,
        }));
        const displayChamps = sorted(enriched, getValue);

        return (
          <div key={tier}>
            <button
              type="button"
              style={{
                display: 'flex', alignItems: 'center', gap: 8,
                width: '100%', textAlign: 'left',
                padding: '8px 12px', background: tierColor + '0D',
                border: `1px solid ${tierColor}33`, borderRadius: isExpanded ? '6px 6px 0 0' : 6,
                cursor: 'pointer',
              }}
              onClick={() => setExpanded(prev => ({ ...prev, [tier]: !prev[tier] }))}
            >
              <span style={{
                fontSize: 11, fontWeight: 700, color: tierColor,
                background: tierColor + '28', borderRadius: 4, padding: '2px 7px',
                border: `1px solid ${tierColor}66`,
              }}>
                {tier}
              </span>
              <span style={{ fontSize: 13, fontWeight: 600, color: tierColor }}>
                {tier} 티어
              </span>
              <span style={{ fontSize: 11, color: 'var(--color-text-secondary)', marginLeft: 4 }}>
                {champions.length}개
              </span>
              <span style={{ marginLeft: 'auto', color: 'var(--color-text-secondary)', fontSize: 12 }}>
                {isExpanded ? '▲' : '▼'}
              </span>
            </button>

            {isExpanded && (
              <div className="table-wrapper" style={{ borderRadius: '0 0 6px 6px', border: `1px solid ${tierColor}22`, borderTop: 'none' }}>
                <table className="table member-stats-table">
                  <ColGroup />
                  <thead>
                    <tr>
                      <th>챔피언</th>
                      <SortableTh label="승률"  col="winRate"  sortKey={sortKey} sortDir={sortDir} onSort={handleSort} right />
                      <SortableTh label="픽률"  col="pickRate" sortKey={sortKey} sortDir={sortDir} onSort={handleSort} right />
                      <SortableTh label="게임"  col="picks"    sortKey={sortKey} sortDir={sortDir} onSort={handleSort} right />
                    </tr>
                  </thead>
                  <tbody>
                    {displayChamps.map((entry) => {
                      const dragon = dragonChampions.get(entry.championId);
                      const displayName = dragon?.nameKo ?? entry.champion;
                      const imgUrl = dragon?.imageUrl ?? null;
                      const wrColor = entry.winRate >= 50 ? 'var(--color-win)' : 'var(--color-loss)';

                      return (
                        <tr key={entry.championId}>
                          <td>
                            <ChampionLink champion={entry.champion} championId={entry.championId}>
                              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                                {imgUrl ? (
                                  <img src={imgUrl} alt={displayName} width={32} height={32}
                                    style={{ borderRadius: 4, border: '1px solid var(--color-border)', objectFit: 'cover' }}
                                    onError={e => { (e.currentTarget as HTMLImageElement).style.display = 'none'; }} />
                                ) : (
                                  <div style={{
                                    width: 32, height: 32, background: 'var(--color-bg-hover)',
                                    borderRadius: 4, display: 'flex', alignItems: 'center',
                                    justifyContent: 'center', fontSize: 10, color: 'var(--color-text-secondary)',
                                  }}>
                                    {displayName.slice(0, 2)}
                                  </div>
                                )}
                                <span style={{ fontWeight: 600, fontSize: 13 }}>{displayName}</span>
                              </div>
                            </ChampionLink>
                          </td>
                          <td className="table-number" style={{ fontWeight: 700, color: wrColor }}>
                            {entry.winRate.toFixed(1)}%
                          </td>
                          <td className="table-number" style={{ color: 'var(--color-text-secondary)' }}>
                            {entry.pickRate.toFixed(1)}%
                          </td>
                          <td className="table-number" style={{ color: 'var(--color-text-secondary)' }}>
                            {entry.picks}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
}
