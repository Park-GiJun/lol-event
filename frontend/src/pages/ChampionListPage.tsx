import { useMemo } from 'react';
import { useChampions } from '@/hooks/useChampions';
import { useSortTable } from '@/hooks/useSortTable';
import { SortableTh } from '@/components/common/SortableTh';
import { InlineError } from '@/components/common/InlineError';
import { ChampionLink } from '@/components/common/ChampionLink';
import { useDragon } from '@/context/DragonContext';
import type { ChampionPickStat } from '@/lib/types/stats';

type SortCol = 'picks' | 'winRate' | 'pickRate' | 'wins' | 'losses' | 'kda' | 'avgDamage' | 'avgCs';

function ColGroup() {
  return (
    <colgroup>
      <col />
      <col style={{ width: 80 }} />
      <col style={{ width: 68 }} />
      <col style={{ width: 68 }} />
      <col style={{ width: 80 }} />
      <col style={{ width: 68 }} />
      <col style={{ width: 60 }} />
      <col style={{ width: 56 }} />
      <col style={{ width: 56 }} />
    </colgroup>
  );
}

function WrBar({ winRate }: { winRate: number }) {
  const color = winRate >= 60 ? 'var(--color-win)' : winRate >= 50 ? 'var(--color-primary)' : 'var(--color-loss)';
  const fillClass = winRate >= 60 ? 'wr-bar-fill--high' : winRate >= 50 ? 'wr-bar-fill--mid' : 'wr-bar-fill--low';
  return (
    <div className="tabular-nums" style={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
      <span style={{ fontWeight: 700, fontSize: 13, color }}>{winRate.toFixed(1)}%</span>
      <div className="wr-bar-track" style={{ width: 60 }}>
        <div className={`wr-bar-fill ${fillClass}`} style={{ width: `${winRate}%` }} />
      </div>
    </div>
  );
}

export function ChampionListPage() {
  const { data, isLoading, error, refetch } = useChampions();
  const { champions: dragonChampions } = useDragon();
  const { sortKey, sortDir, handleSort, sorted } = useSortTable<SortCol>('picks');

  const enriched = useMemo(() => {
    if (!data) return [];
    return data.topPickedChampions.map(c => ({
      ...c,
      wins: c.wins,
      losses: c.picks - c.wins,
      pickRate: data.matchCount > 0 ? (c.picks / data.matchCount) * 100 : 0,
    }));
  }, [data]);

  const getValue = (key: SortCol, c: typeof enriched[number]): number => {
    if (key === 'picks')     return c.picks;
    if (key === 'winRate')   return c.winRate;
    if (key === 'pickRate')  return c.pickRate;
    if (key === 'wins')      return c.wins;
    if (key === 'losses')    return c.losses;
    if (key === 'kda')       return c.kda ?? 0;
    if (key === 'avgDamage') return c.avgDamage ?? 0;
    if (key === 'avgCs')     return c.avgCs ?? 0;
    return 0;
  };

  const displayed = sorted(enriched, getValue);

  if (isLoading) {
    return (
      <div>
        <h1 className="page-title" style={{ marginBottom: 'var(--spacing-md)' }}>챔피언 목록</h1>
        <div className="card">
          <div className="table-wrapper">
            <table className="table member-stats-table">
              <ColGroup />
              <thead>
                <tr>
                  <th>챔피언</th>
                  <th className="table-number">승률</th>
                  <th className="table-number">승</th>
                  <th className="table-number">패</th>
                  <th className="table-number">KDA</th>
                  <th className="table-number">평균 딜</th>
                  <th className="table-number">CS</th>
                  <th className="table-number">픽률</th>
                  <th className="table-number">게임</th>
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
                    <td className="table-number"><div style={{ width: 50, height: 13, borderRadius: 3, background: 'var(--color-bg-hover)', marginLeft: 'auto' }} /></td>
                    <td className="table-number"><div style={{ width: 28, height: 13, borderRadius: 3, background: 'var(--color-bg-hover)', marginLeft: 'auto' }} /></td>
                    <td className="table-number"><div style={{ width: 28, height: 13, borderRadius: 3, background: 'var(--color-bg-hover)', marginLeft: 'auto' }} /></td>
                    <td className="table-number"><div style={{ width: 36, height: 13, borderRadius: 3, background: 'var(--color-bg-hover)', marginLeft: 'auto' }} /></td>
                    <td className="table-number"><div style={{ width: 50, height: 13, borderRadius: 3, background: 'var(--color-bg-hover)', marginLeft: 'auto' }} /></td>
                    <td className="table-number"><div style={{ width: 28, height: 13, borderRadius: 3, background: 'var(--color-bg-hover)', marginLeft: 'auto' }} /></td>
                    <td className="table-number"><div style={{ width: 36, height: 13, borderRadius: 3, background: 'var(--color-bg-hover)', marginLeft: 'auto' }} /></td>
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

  if (!data || displayed.length === 0) {
    return (
      <div>
        <h1 className="page-title" style={{ marginBottom: 'var(--spacing-md)' }}>챔피언 목록</h1>
        <p className="text-sm p-4" style={{ color: 'var(--color-text-secondary)' }}>챔피언 데이터가 없습니다.</p>
      </div>
    );
  }

  return (
    <div>
      <div className="page-header flex items-center justify-between">
        <div>
          <h1 className="page-title">챔피언 목록</h1>
          <p className="page-subtitle">총 {data.matchCount}경기 · {displayed.length}개 챔피언</p>
        </div>
      </div>
      <div className="card">
        <div className="table-wrapper">
          <table className="table member-stats-table">
            <ColGroup />
            <thead>
              <tr>
                <th>챔피언</th>
                <SortableTh label="승률"   col="winRate"   sortKey={sortKey} sortDir={sortDir} onSort={handleSort} right />
                <SortableTh label="승"     col="wins"      sortKey={sortKey} sortDir={sortDir} onSort={handleSort} right />
                <SortableTh label="패"     col="losses"    sortKey={sortKey} sortDir={sortDir} onSort={handleSort} right />
                <SortableTh label="KDA"    col="kda"       sortKey={sortKey} sortDir={sortDir} onSort={handleSort} right />
                <SortableTh label="평균 딜" col="avgDamage" sortKey={sortKey} sortDir={sortDir} onSort={handleSort} right />
                <SortableTh label="CS"     col="avgCs"     sortKey={sortKey} sortDir={sortDir} onSort={handleSort} right />
                <SortableTh label="픽률"   col="pickRate"  sortKey={sortKey} sortDir={sortDir} onSort={handleSort} right />
                <SortableTh label="게임"   col="picks"     sortKey={sortKey} sortDir={sortDir} onSort={handleSort} right />
              </tr>
            </thead>
            <tbody>
              {displayed.map((entry: ChampionPickStat & { wins: number; losses: number; pickRate: number }) => {
                const dragon = dragonChampions.get(entry.championId);
                const displayName = dragon?.nameKo ?? entry.champion;
                const imgUrl = dragon?.imageUrl ?? null;

                return (
                  <tr key={entry.championId} className="member-stats-row">
                    <td>
                      <ChampionLink champion={entry.champion} championId={entry.championId}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                          {imgUrl ? (
                            <img src={imgUrl} alt={displayName} width={32} height={32}
                              className="champ-list-img"
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
                    <td className="table-number"><WrBar winRate={entry.winRate} /></td>
                    <td className="table-number" style={{ color: 'var(--color-win)', fontWeight: 600 }}>
                      {entry.wins}
                    </td>
                    <td className="table-number" style={{ color: 'var(--color-loss)', fontWeight: 600 }}>
                      {entry.losses}
                    </td>
                    <td className="table-number">
                      {(() => {
                        const kda = entry.kda ?? 0;
                        const color = kda >= 5 ? 'var(--color-win)' : kda >= 3 ? 'var(--color-primary)' : 'var(--color-text-primary)';
                        return (
                          <div className="tabular-nums" style={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                            <span style={{ fontWeight: 700, fontSize: 12, color }}>{kda.toFixed(2)}</span>
                            <span style={{ fontSize: 10, color: 'var(--color-text-secondary)' }}>
                              {(entry.avgKills ?? 0).toFixed(1)} / <span style={{ color: 'var(--color-error)' }}>{(entry.avgDeaths ?? 0).toFixed(1)}</span> / {(entry.avgAssists ?? 0).toFixed(1)}
                            </span>
                          </div>
                        );
                      })()}
                    </td>
                    <td className="table-number" style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>
                      {(entry.avgDamage ?? 0).toLocaleString()}
                    </td>
                    <td className="table-number" style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>
                      {(entry.avgCs ?? 0).toFixed(1)}
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
      </div>
    </div>
  );
}
