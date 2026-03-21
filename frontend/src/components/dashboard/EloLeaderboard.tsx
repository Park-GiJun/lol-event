import { useLeaderboard } from '@/hooks/useLeaderboard';
import { InlineError } from '@/components/common/InlineError';
import { Skeleton } from '@/components/common/Skeleton';
import { PlayerLink } from '@/components/common/PlayerLink';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import type { EloRankEntry } from '@/lib/types/stats';

// ── 내부 유틸 ────────────────────────────────────────────────

function eloTier(elo: number): { label: string; color: string } {
  if (elo >= 1300) return { label: 'Challenger', color: '#FFD700' };
  if (elo >= 1200) return { label: 'Master',     color: '#AA47BC' };
  if (elo >= 1100) return { label: 'Diamond',    color: '#0BC4B4' };
  if (elo >= 1000) return { label: 'Platinum',   color: '#4A9EFF' };
  if (elo >= 900)  return { label: 'Gold',       color: '#C89B3C' };
  if (elo >= 800)  return { label: 'Silver',     color: '#A8A8A8' };
  return                  { label: 'Bronze',     color: '#CD7F32' };
}

const RANK_COLORS: Record<number, string> = { 1: '#FFD700', 2: '#C0C0C0', 3: '#CD7F32' };

function RankBadge({ rank }: { rank: number }) {
  const color = RANK_COLORS[rank];
  if (color) {
    return (
      <div style={{
        width: 26, height: 26, borderRadius: '50%',
        background: color, display: 'flex', alignItems: 'center',
        justifyContent: 'center', fontSize: 11, fontWeight: 800,
        color: '#111', flexShrink: 0,
      }}>
        {rank}
      </div>
    );
  }
  return (
    <span style={{
      color: 'var(--color-text-disabled)', fontSize: 12,
      width: 26, textAlign: 'center', display: 'inline-block',
    }}>
      {rank}
    </span>
  );
}

// ── Props ────────────────────────────────────────────────────

export interface EloLeaderboardProps {
  currentRiotId?: string;
}

// ── 컴포넌트 ─────────────────────────────────────────────────

export function EloLeaderboard({ currentRiotId }: EloLeaderboardProps) {
  const { data, isLoading, error, refetch } = useLeaderboard();

  if (isLoading) {
    return (
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead className="w-12">순위</TableHead>
            <TableHead>플레이어</TableHead>
            <TableHead>티어</TableHead>
            <TableHead className="text-right font-mono">Elo</TableHead>
            <TableHead className="text-right font-mono">게임</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {Array.from({ length: 5 }).map((_, i) => (
            <TableRow key={i}>
              <TableCell><Skeleton className="h-6 w-6 rounded-full" /></TableCell>
              <TableCell><Skeleton className="h-4 w-32" /></TableCell>
              <TableCell><Skeleton className="h-4 w-16" /></TableCell>
              <TableCell className="text-right"><Skeleton className="h-4 w-12 ml-auto" /></TableCell>
              <TableCell className="text-right"><Skeleton className="h-4 w-8 ml-auto" /></TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    );
  }

  if (error) {
    return (
      <InlineError
        message="리더보드를 불러오지 못했습니다."
        onRetry={() => void refetch()}
      />
    );
  }

  if (!data || data.players.length === 0) {
    return (
      <p className="text-sm p-4" style={{ color: 'var(--color-text-secondary)' }}>
        Elo 데이터가 없습니다. 어드민에서 재집계를 실행하세요.
      </p>
    );
  }

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead className="w-12">순위</TableHead>
          <TableHead>플레이어</TableHead>
          <TableHead>티어</TableHead>
          <TableHead className="text-right font-mono">Elo</TableHead>
          <TableHead className="text-right font-mono">게임</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {data.players.map((entry: EloRankEntry) => {
          const tier = eloTier(entry.elo);
          const isCurrentUser = currentRiotId !== undefined && entry.riotId === currentRiotId;
          return (
            <TableRow
              key={entry.riotId}
              className={isCurrentUser ? 'bg-teal-900/20' : undefined}
            >
              <TableCell>
                <RankBadge rank={entry.rank} />
              </TableCell>
              <TableCell>
                <PlayerLink riotId={entry.riotId} mode="all">
                  {(() => {
                    const [name, tag = ''] = entry.riotId.split('#');
                    return (
                      <>
                        <span style={{ fontWeight: 600 }}>{name}</span>
                        {tag && (
                          <span style={{ fontSize: 10, color: 'var(--color-text-secondary)', marginLeft: 4 }}>
                            #{tag}
                          </span>
                        )}
                      </>
                    );
                  })()}
                </PlayerLink>
              </TableCell>
              <TableCell>
                <span style={{
                  fontSize: 11, fontWeight: 700, color: tier.color,
                  background: tier.color + '22', borderRadius: 4, padding: '2px 7px',
                  border: `1px solid ${tier.color}44`,
                }}>
                  {tier.label}
                </span>
              </TableCell>
              <TableCell
                className="text-right font-mono"
                style={{ fontWeight: 700, color: tier.color }}
              >
                {entry.elo.toFixed(1)}
              </TableCell>
              <TableCell
                className="text-right font-mono"
                style={{ color: 'var(--color-text-secondary)' }}
              >
                {entry.games}
              </TableCell>
            </TableRow>
          );
        })}
      </TableBody>
    </Table>
  );
}
