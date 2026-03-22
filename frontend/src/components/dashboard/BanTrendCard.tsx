import { useChampions } from '@/hooks/useChampions';
import { useSortTable } from '@/hooks/useSortTable';
import { SortableTh } from '@/components/common/SortableTh';
import { InlineError } from '@/components/common/InlineError';
import { Skeleton } from '@/components/common/Skeleton';
import { ChampionLink } from '@/components/common/ChampionLink';
import { useDragon } from '@/context/DragonContext';
import type { ChampionPickStat } from '@/lib/types/stats';

type SortCol = 'picks' | 'banRate';

function ColGroup() {
  return (
    <colgroup>
      <col />
      <col style={{ width: 76 }} />
      <col style={{ width: 76 }} />
    </colgroup>
  );
}

export function BanTrendCard() {
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
              <th>챔피언</th>
              <th className="table-number">밴률</th>
              <th className="table-number">밴 횟수</th>
            </tr>
          </thead>
          <tbody>
            {Array.from({ length: 5 }).map((_, i) => (
              <tr key={i}>
                <td>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <Skeleton className="h-8 w-8 rounded" />
                    <Skeleton className="h-4 w-20" />
                  </div>
                </td>
                <td className="table-number"><Skeleton className="h-4 w-12 ml-auto" /></td>
                <td className="table-number"><Skeleton className="h-4 w-8 ml-auto" /></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  }

  if (error) {
    return <InlineError message="밴 트렌드를 불러오지 못했습니다." onRetry={() => void refetch()} />;
  }

  if (!data || data.topBannedChampions.length === 0) {
    return (
      <p className="text-sm p-4" style={{ color: 'var(--color-text-secondary)' }}>
        밴 데이터가 없습니다.
      </p>
    );
  }

  const enriched = data.topBannedChampions.map(c => ({
    ...c,
    banRate: data.matchCount > 0 ? (c.picks / data.matchCount) * 100 : 0,
  }));

  const getValue = (key: SortCol, c: typeof enriched[number]): number => {
    if (key === 'picks')   return c.picks;
    if (key === 'banRate') return c.banRate;
    return 0;
  };

  const displayed = sorted(enriched, getValue);

  return (
    <div className="table-wrapper">
      <table className="table member-stats-table">
        <ColGroup />
        <thead>
          <tr>
            <th>챔피언</th>
            <SortableTh label="밴률"   col="banRate" sortKey={sortKey} sortDir={sortDir} onSort={handleSort} right />
            <SortableTh label="밴 횟수" col="picks"   sortKey={sortKey} sortDir={sortDir} onSort={handleSort} right />
          </tr>
        </thead>
        <tbody>
          {displayed.map((entry: ChampionPickStat & { banRate: number }) => {
            const dragon = dragonChampions.get(entry.championId);
            const displayName = dragon?.nameKo ?? entry.champion;
            const imgUrl = dragon?.imageUrl ?? null;

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
                <td className="table-number" style={{ fontWeight: 700 }}>
                  {entry.banRate.toFixed(1)}%
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
  );
}
