import { useChampions } from '@/hooks/useChampions';
import { useSortTable } from '@/hooks/useSortTable';
import { SortableTh } from '@/components/common/SortableTh';
import { ChampionLink } from '@/components/common/ChampionLink';
import { useDragon } from '@/context/DragonContext';
import { Skeleton } from '@/components/common/Skeleton';
import { InlineError } from '@/components/common/InlineError';
import type { ChampionPickStat } from '@/lib/types/stats';

type SortCol = 'picks' | 'winRate' | 'pickRate' | 'wins' | 'losses' | 'kda' | 'avgDamage' | 'avgCs';

function ColGroup() {
  return (
    <colgroup>
      <col style={{ width: 36 }} />
      <col />
      <col style={{ width: 82 }} />
      <col style={{ width: 56 }} />
      <col style={{ width: 56 }} />
      <col style={{ width: 80 }} />
      <col style={{ width: 68 }} />
      <col style={{ width: 56 }} />
      <col style={{ width: 60 }} />
      <col style={{ width: 56 }} />
    </colgroup>
  );
}

export function ChampionPicksTab() {
  const { data, isLoading, error, refetch } = useChampions();
  const { champions: dragonChampions } = useDragon();
  const { sortKey, sortDir, handleSort, sorted } = useSortTable<SortCol>('picks');

  if (isLoading) {
    return (
      <div className="table-wrapper">
        <table className="table member-stats-table">
          <ColGroup />
          <thead>
            <tr>
              <th>#</th><th>챔피언</th>
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
                <td><Skeleton className="h-4 w-6" /></td>
                <td>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <Skeleton className="h-8 w-8 rounded" />
                    <Skeleton className="h-4 w-20" />
                  </div>
                </td>
                <td className="table-number"><Skeleton className="h-4 w-14 ml-auto" /></td>
                <td className="table-number"><Skeleton className="h-4 w-8 ml-auto" /></td>
                <td className="table-number"><Skeleton className="h-4 w-8 ml-auto" /></td>
                <td className="table-number"><Skeleton className="h-4 w-12 ml-auto" /></td>
                <td className="table-number"><Skeleton className="h-4 w-14 ml-auto" /></td>
                <td className="table-number"><Skeleton className="h-4 w-8 ml-auto" /></td>
                <td className="table-number"><Skeleton className="h-4 w-10 ml-auto" /></td>
                <td className="table-number"><Skeleton className="h-4 w-8 ml-auto" /></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  }

  if (error) {
    return <InlineError message="챔피언 데이터를 불러오지 못했습니다." onRetry={() => void refetch()} />;
  }

  if (!data || data.topPickedChampions.length === 0) {
    return <p style={{ color: 'var(--color-text-secondary)', fontSize: 13 }}>챔피언 데이터가 없습니다.</p>;
  }

  const enriched = data.topPickedChampions.map(c => ({
    ...c,
    losses: c.picks - c.wins,
    pickRate: data.matchCount > 0 ? (c.picks / data.matchCount) * 100 : 0,
  }));

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

  return (
    <div className="table-wrapper">
      <table className="table member-stats-table">
        <ColGroup />
        <thead>
          <tr>
            <th>#</th>
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
          {displayed.map((entry: ChampionPickStat & { losses: number; pickRate: number }, idx) => {
            const dragon = dragonChampions.get(entry.championId);
            const displayName = dragon?.nameKo ?? entry.champion;
            const imgUrl = dragon?.imageUrl ?? null;
            const wrColor = entry.winRate >= 60 ? 'var(--color-win)' : entry.winRate >= 50 ? 'var(--color-primary)' : 'var(--color-loss)';
            return (
              <tr key={entry.championId} className="member-stats-row">
                <td style={{ color: 'var(--color-text-disabled)', fontSize: 12 }}>{idx + 1}</td>
                <td>
                  <ChampionLink champion={entry.champion} championId={entry.championId}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      {imgUrl ? (
                        <img src={imgUrl} alt={displayName} width={32} height={32}
                          style={{ borderRadius: 4, border: '1px solid var(--color-border)', objectFit: 'cover' }}
                          onError={e => { (e.currentTarget as HTMLImageElement).style.display = 'none'; }} />
                      ) : (
                        <div style={{ width: 32, height: 32, background: 'var(--color-bg-hover)', borderRadius: 4, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 10, color: 'var(--color-text-secondary)' }}>
                          {displayName.slice(0, 2)}
                        </div>
                      )}
                      <span style={{ fontWeight: 600, fontSize: 13 }}>{displayName}</span>
                    </div>
                  </ChampionLink>
                </td>
                <td className="table-number">
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 2, alignItems: 'flex-end' }}>
                    <span style={{ fontWeight: 700, color: wrColor }}>{entry.winRate.toFixed(1)}%</span>
                    <div style={{ height: 3, width: 50, background: 'var(--color-bg-hover)', borderRadius: 2, overflow: 'hidden' }}>
                      <div style={{ width: `${entry.winRate}%`, height: '100%', background: wrColor, borderRadius: 2 }} />
                    </div>
                  </div>
                </td>
                <td className="table-number" style={{ color: 'var(--color-win)', fontWeight: 600 }}>{entry.wins}</td>
                <td className="table-number" style={{ color: 'var(--color-loss)', fontWeight: 600 }}>{entry.losses}</td>
                <td className="table-number">
                  {(() => {
                    const kda = entry.kda ?? 0;
                    const kdaColor = kda >= 5 ? 'var(--color-win)' : kda >= 3 ? 'var(--color-primary)' : 'var(--color-text-primary)';
                    return (
                      <div style={{ display: 'flex', flexDirection: 'column', gap: 1, alignItems: 'flex-end' }}>
                        <span style={{ fontWeight: 700, fontSize: 12, color: kdaColor }}>{kda.toFixed(2)}</span>
                        <span style={{ fontSize: 10, color: 'var(--color-text-secondary)' }}>
                          {(entry.avgKills ?? 0).toFixed(1)} / <span style={{ color: 'var(--color-error)' }}>{(entry.avgDeaths ?? 0).toFixed(1)}</span> / {(entry.avgAssists ?? 0).toFixed(1)}
                        </span>
                      </div>
                    );
                  })()}
                </td>
                <td className="table-number" style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>{(entry.avgDamage ?? 0).toLocaleString()}</td>
                <td className="table-number" style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>{(entry.avgCs ?? 0).toFixed(1)}</td>
                <td className="table-number" style={{ color: 'var(--color-text-secondary)' }}>{entry.pickRate.toFixed(1)}%</td>
                <td className="table-number" style={{ color: 'var(--color-text-secondary)' }}>{entry.picks}</td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
