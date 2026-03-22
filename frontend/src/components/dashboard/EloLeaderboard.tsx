import { useLeaderboard } from '@/hooks/useLeaderboard';
import { useSortTable } from '@/hooks/useSortTable';
import { SortableTh } from '@/components/common/SortableTh';
import { InlineError } from '@/components/common/InlineError';
import { Skeleton } from '@/components/common/Skeleton';
import { PlayerLink } from '@/components/common/PlayerLink';
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

function StreakBadge({ winStreak, lossStreak }: { winStreak: number; lossStreak: number }) {
  if (winStreak >= 3) {
    return (
      <span style={{
        fontSize: 10, fontWeight: 700, color: '#FF6B2B',
        background: 'rgba(255,107,43,0.15)', borderRadius: 4,
        padding: '1px 5px', border: '1px solid rgba(255,107,43,0.3)',
        whiteSpace: 'nowrap',
      }}>
        🔥 {winStreak}연승
      </span>
    );
  }
  if (lossStreak >= 3) {
    return (
      <span style={{
        fontSize: 10, fontWeight: 700, color: '#6BAAFF',
        background: 'rgba(107,170,255,0.15)', borderRadius: 4,
        padding: '1px 5px', border: '1px solid rgba(107,170,255,0.3)',
        whiteSpace: 'nowrap',
      }}>
        🧊 {lossStreak}연패
      </span>
    );
  }
  return <span style={{ color: 'var(--color-text-disabled)', fontSize: 12 }}>—</span>;
}

type SortCol = 'elo' | 'winRate' | 'games';

function ColGroup() {
  return (
    <colgroup>
      <col style={{ width: 48 }} />
      <col />
      <col style={{ width: 88 }} />
      <col style={{ width: 80 }} />
      <col style={{ width: 80 }} />
      <col style={{ width: 72 }} />
      <col style={{ width: 80 }} />
      <col style={{ width: 52 }} />
    </colgroup>
  );
}

// ── Props ────────────────────────────────────────────────────

export interface EloLeaderboardProps {
  currentRiotId?: string;
}

// ── 컴포넌트 ─────────────────────────────────────────────────

export function EloLeaderboard({ currentRiotId }: EloLeaderboardProps) {
  const { data, isLoading, error, refetch } = useLeaderboard();
  const { sortKey, sortDir, handleSort, sorted } = useSortTable<SortCol>('elo');

  const getValue = (key: SortCol, row: EloRankEntry): number => {
    if (key === 'elo')     return row.elo;
    if (key === 'winRate') return row.winRate;
    if (key === 'games')   return row.games;
    return 0;
  };

  if (isLoading) {
    return (
      <div className="table-wrapper">
        <table className="table member-stats-table">
          <ColGroup />
          <thead>
            <tr>
              <th>순위</th><th>플레이어</th><th>티어</th>
              <th className="table-number">Elo</th>
              <th className="table-number">승/패</th>
              <th className="table-number">승률</th>
              <th>연속</th>
              <th className="table-number">게임</th>
            </tr>
          </thead>
          <tbody>
            {Array.from({ length: 5 }).map((_, i) => (
              <tr key={i}>
                <td><Skeleton className="h-6 w-6 rounded-full" /></td>
                <td><Skeleton className="h-4 w-28" /></td>
                <td><Skeleton className="h-4 w-16" /></td>
                <td className="table-number"><Skeleton className="h-4 w-12 ml-auto" /></td>
                <td className="table-number"><Skeleton className="h-4 w-12 ml-auto" /></td>
                <td className="table-number"><Skeleton className="h-4 w-10 ml-auto" /></td>
                <td><Skeleton className="h-4 w-14" /></td>
                <td className="table-number"><Skeleton className="h-4 w-8 ml-auto" /></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  }

  if (error) {
    return <InlineError message="리더보드를 불러오지 못했습니다." onRetry={() => void refetch()} />;
  }

  if (!data || data.players.length === 0) {
    return (
      <p className="text-sm p-4" style={{ color: 'var(--color-text-secondary)' }}>
        Elo 데이터가 없습니다. 어드민에서 재집계를 실행하세요.
      </p>
    );
  }

  const displayed = sorted(data.players, getValue);

  return (
    <div className="table-wrapper">
      <table className="table member-stats-table">
        <ColGroup />
        <thead>
          <tr>
            <th>순위</th>
            <th>플레이어</th>
            <th>티어</th>
            <SortableTh label="Elo"  col="elo"     sortKey={sortKey} sortDir={sortDir} onSort={handleSort} right />
            <th className="table-number">승/패</th>
            <SortableTh label="승률" col="winRate"  sortKey={sortKey} sortDir={sortDir} onSort={handleSort} right />
            <th>연속</th>
            <SortableTh label="게임" col="games"    sortKey={sortKey} sortDir={sortDir} onSort={handleSort} right />
          </tr>
        </thead>
        <tbody>
          {displayed.map((entry) => {
            const tier = eloTier(entry.elo);
            const isCurrentUser = currentRiotId !== undefined && entry.riotId === currentRiotId;
            const [name, tag = ''] = entry.riotId.split('#');
            const wrColor = entry.winRate >= 60 ? 'var(--color-win)' : entry.winRate >= 50 ? 'var(--color-primary)' : entry.winRate > 0 ? 'var(--color-loss)' : 'var(--color-text-disabled)';
            return (
              <tr
                key={entry.riotId}
                className="member-stats-row"
                style={isCurrentUser ? { background: 'rgba(11, 196, 180, 0.08)' } : undefined}
              >
                <td><RankBadge rank={entry.rank} /></td>
                <td>
                  <PlayerLink riotId={entry.riotId} mode="all">
                    <span style={{ fontWeight: 600 }}>{name}</span>
                    {tag && (
                      <span style={{ fontSize: 10, color: 'var(--color-text-secondary)', marginLeft: 4 }}>
                        #{tag}
                      </span>
                    )}
                  </PlayerLink>
                </td>
                <td>
                  <span style={{
                    fontSize: 11, fontWeight: 700, color: tier.color,
                    background: tier.color + '22', borderRadius: 4, padding: '2px 7px',
                    border: `1px solid ${tier.color}44`,
                  }}>
                    {tier.label}
                  </span>
                </td>
                <td className="table-number" style={{ fontWeight: 700, color: tier.color }}>
                  {entry.elo.toFixed(1)}
                </td>
                <td className="table-number" style={{ fontSize: 12 }}>
                  <span style={{ color: 'var(--color-win)', fontWeight: 600 }}>{entry.wins}승</span>
                  {' '}
                  <span style={{ color: 'var(--color-loss)', fontWeight: 600 }}>{entry.losses}패</span>
                </td>
                <td className="table-number" style={{ fontWeight: 600, color: wrColor }}>
                  {entry.games > 0 ? `${entry.winRate.toFixed(1)}%` : '—'}
                </td>
                <td>
                  <StreakBadge winStreak={entry.winStreak} lossStreak={entry.lossStreak} />
                </td>
                <td className="table-number" style={{ color: 'var(--color-text-secondary)' }}>
                  {entry.games}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
