import { useMemo } from 'react';
import { useChampions } from '@/hooks/useChampions';
import { InlineError } from '@/components/common/InlineError';
import { Skeleton } from '@/components/common/Skeleton';
import { ChampionLink } from '@/components/common/ChampionLink';
import { useDragon } from '@/context/DragonContext';
import type { ChampionPickStat } from '@/lib/types/stats';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';

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
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>챔피언</TableHead>
              <TableHead className="text-right font-mono">승률</TableHead>
              <TableHead className="text-right font-mono">픽률</TableHead>
              <TableHead className="text-right font-mono">판수</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {Array.from({ length: 8 }).map((_, i) => (
              <TableRow key={i}>
                <TableCell>
                  <div className="flex items-center gap-2">
                    <Skeleton className="h-8 w-8 rounded" />
                    <Skeleton className="h-4 w-24" />
                  </div>
                </TableCell>
                <TableCell className="text-right"><Skeleton className="h-4 w-12 ml-auto" /></TableCell>
                <TableCell className="text-right"><Skeleton className="h-4 w-12 ml-auto" /></TableCell>
                <TableCell className="text-right"><Skeleton className="h-4 w-8 ml-auto" /></TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
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
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>챔피언</TableHead>
            <TableHead className="text-right font-mono">승률</TableHead>
            <TableHead className="text-right font-mono">픽률</TableHead>
            <TableHead className="text-right font-mono">판수</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
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
              <TableRow key={entry.championId}>
                <TableCell>
                  <ChampionLink champion={entry.champion} championId={entry.championId}>
                    <div className="flex items-center gap-2">
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
                </TableCell>
                <TableCell
                  className="text-right font-mono"
                  style={{ fontWeight: 700, color: wrColor }}
                >
                  {entry.winRate.toFixed(1)}%
                </TableCell>
                <TableCell
                  className="text-right font-mono"
                  style={{ color: 'var(--color-text-secondary)' }}
                >
                  {pickRate}%
                </TableCell>
                <TableCell
                  className="text-right font-mono"
                  style={{ color: 'var(--color-text-secondary)' }}
                >
                  {entry.picks}
                </TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    </div>
  );
}
