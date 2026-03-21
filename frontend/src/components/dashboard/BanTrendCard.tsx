import { useChampions } from '@/hooks/useChampions';
import { InlineError } from '@/components/common/InlineError';
import { Skeleton } from '@/components/common/Skeleton';
import { ChampionLink } from '@/components/common/ChampionLink';
import { useDragon } from '@/context/DragonContext';
import type { ChampionPickStat } from '@/lib/types/stats';

// ── 컴포넌트 ─────────────────────────────────────────────────

export function BanTrendCard() {
  const { data, isLoading, error, refetch } = useChampions();
  const { champions: dragonChampions } = useDragon();

  if (isLoading) {
    return (
      <div className="table-wrapper">
        <table className="table member-stats-table">
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
    return (
      <InlineError
        message="밴 트렌드를 불러오지 못했습니다."
        onRetry={() => void refetch()}
      />
    );
  }

  if (!data || data.topBannedChampions.length === 0) {
    return (
      <p className="text-sm p-4" style={{ color: 'var(--color-text-secondary)' }}>
        밴 데이터가 없습니다.
      </p>
    );
  }

  return (
    <div className="table-wrapper">
      <table className="table member-stats-table">
        <thead>
          <tr>
            <th>챔피언</th>
            <th className="table-number">밴률</th>
            <th className="table-number">밴 횟수</th>
          </tr>
        </thead>
        <tbody>
          {[...data.topBannedChampions]
            .sort((a, b) => b.picks - a.picks)
            .map((entry: ChampionPickStat) => {
            const dragon = dragonChampions.get(entry.championId);
            const displayName = dragon?.nameKo ?? entry.champion;
            const imgUrl = dragon?.imageUrl ?? null;
            const banRate = data.matchCount > 0
              ? ((entry.picks / data.matchCount) * 100).toFixed(1)
              : '0.0';

            return (
              <tr key={entry.championId}>
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
                <td className="table-number" style={{ fontWeight: 700, color: 'var(--color-text-primary)' }}>
                  {banRate}%
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
