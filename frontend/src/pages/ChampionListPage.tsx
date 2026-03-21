import { useMemo } from 'react';
import { useChampions } from '@/hooks/useChampions';
import { InlineError } from '@/components/common/InlineError';
import { ChampionLink } from '@/components/common/ChampionLink';
import { useDragon } from '@/context/DragonContext';
import type { ChampionPickStat } from '@/lib/types/stats';

export function ChampionListPage() {
  const { data, isLoading, error, refetch } = useChampions();
  const { champions: dragonChampions } = useDragon();

  const sorted = useMemo(() => {
    if (!data) return [];
    return [...data.topPickedChampions].sort((a, b) => b.picks - a.picks);
  }, [data]);

  if (isLoading) {
    return (
      <div>
        <h1 className="page-title" style={{ marginBottom: 'var(--spacing-md)' }}>챔피언 목록</h1>
        <div className="card">
          <div className="table-wrapper">
            <table className="table member-stats-table">
              <thead>
                <tr>
                  <th>챔피언</th>
                  <th className="table-number">승률</th>
                  <th className="table-number">픽률</th>
                  <th className="table-number">판수</th>
                </tr>
              </thead>
              <tbody>
                {Array.from({ length: 8 }).map((_, i) => (
                  <tr key={i}>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                        <div style={{ width: 32, height: 32, borderRadius: 4, background: 'var(--color-bg-hover)' }} />
                        <div style={{ width: 80, height: 13, borderRadius: 3, background: 'var(--color-bg-hover)' }} />
                      </div>
                    </td>
                    <td className="table-number"><div style={{ width: 40, height: 13, borderRadius: 3, background: 'var(--color-bg-hover)', marginLeft: 'auto' }} /></td>
                    <td className="table-number"><div style={{ width: 40, height: 13, borderRadius: 3, background: 'var(--color-bg-hover)', marginLeft: 'auto' }} /></td>
                    <td className="table-number"><div style={{ width: 28, height: 13, borderRadius: 3, background: 'var(--color-bg-hover)', marginLeft: 'auto' }} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return <InlineError message="챔피언 목록을 불러오지 못했습니다." onRetry={() => void refetch()} />;
  }

  if (!data || sorted.length === 0) {
    return (
      <div>
        <h1 className="page-title" style={{ marginBottom: 'var(--spacing-md)' }}>챔피언 목록</h1>
        <p className="text-sm p-4" style={{ color: 'var(--color-text-secondary)' }}>
          챔피언 데이터가 없습니다.
        </p>
      </div>
    );
  }

  return (
    <div>
      <h1 className="page-title" style={{ marginBottom: 'var(--spacing-md)' }}>챔피언 목록</h1>
      <div className="card">
        <div className="table-wrapper">
          <table className="table member-stats-table">
            <thead>
              <tr>
                <th>챔피언</th>
                <th className="table-number">승률</th>
                <th className="table-number">픽률</th>
                <th className="table-number">판수</th>
              </tr>
            </thead>
            <tbody>
              {sorted.map((entry: ChampionPickStat) => {
                const dragon = dragonChampions.get(entry.championId);
                const displayName = dragon?.nameKo ?? entry.champion;
                const imgUrl = dragon?.imageUrl ?? null;
                const pickRate = data.matchCount > 0
                  ? ((entry.picks / data.matchCount) * 100).toFixed(1)
                  : '0.0';
                const wrColor = entry.winRate >= 50
                  ? 'var(--color-win)'
                  : 'var(--color-loss)';

                return (
                  <tr key={entry.championId} className="member-stats-row">
                    <td>
                      <ChampionLink champion={entry.champion} championId={entry.championId}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                          {imgUrl ? (
                            <img
                              src={imgUrl}
                              alt={displayName}
                              width={32}
                              height={32}
                              style={{
                                borderRadius: 4,
                                border: '1px solid var(--color-border)',
                                objectFit: 'cover',
                              }}
                              onError={e => {
                                (e.currentTarget as HTMLImageElement).style.display = 'none';
                              }}
                            />
                          ) : (
                            <div style={{
                              width: 32, height: 32,
                              background: 'var(--color-bg-hover)',
                              borderRadius: 4, display: 'flex',
                              alignItems: 'center', justifyContent: 'center',
                              fontSize: 10, color: 'var(--color-text-secondary)',
                            }}>
                              {displayName.slice(0, 2)}
                            </div>
                          )}
                          <span style={{ fontWeight: 600, fontSize: 13 }}>
                            {displayName}
                          </span>
                        </div>
                      </ChampionLink>
                    </td>
                    <td className="table-number" style={{ fontWeight: 700, color: wrColor }}>
                      {entry.winRate.toFixed(1)}%
                    </td>
                    <td className="table-number" style={{ color: 'var(--color-text-secondary)' }}>
                      {pickRate}%
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
      </div>
    </div>
  );
}
