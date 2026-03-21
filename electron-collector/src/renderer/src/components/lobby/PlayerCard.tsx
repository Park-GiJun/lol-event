import { useState } from 'react';

const CDN = 'https://raw.communitydragon.org/latest/plugins/rcp-be-lol-game-data/global/default/v1/champion-icons';

function champIconUrl(id: number): string {
  return `${CDN}/${id}.png`;
}

function ChampIcon({ id, size = 26 }: { id: number; size?: number }) {
  if (!id) return <div style={{ width: size, height: size, borderRadius: 4, background: 'var(--color-bg-hover)' }} />;
  return (
    <img
      src={champIconUrl(id)}
      alt=""
      width={size}
      height={size}
      style={{ borderRadius: 4, objectFit: 'cover' }}
      onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }}
    />
  );
}

export interface PlayerChampStat {
  champion: string;
  championId: number;
  games: number;
  wins: number;
  winRate: number;
}

export interface PlayerData {
  riotId: string;
  games: number;
  wins: number;
  winRate: number;
  elo?: number;
  championStats: PlayerChampStat[];
}

export interface PlayerCardProps {
  riotId: string;
  data: PlayerData | null;
  loading?: boolean;
}

export function PlayerCard({ riotId, data, loading = false }: PlayerCardProps) {
  const [expanded, setExpanded] = useState(false);

  const cardStyle: React.CSSProperties = {
    background: 'var(--color-bg-card)',
    border: '1px solid var(--color-border)',
    borderRadius: 'var(--radius-md)',
    padding: 'var(--spacing-sm)',
  };

  // 로딩 상태: Skeleton 레이아웃 유지
  if (loading || data === null) {
    return (
      <article
        role="article"
        aria-label={`플레이어 ${riotId} 로딩 중`}
        style={cardStyle}
      >
        <div style={{ height: 14, width: '70%', borderRadius: 3, background: 'var(--color-border)', marginBottom: 6 }} />
        <div style={{ height: 12, width: '40%', borderRadius: 3, background: 'var(--color-border)', marginBottom: 10 }} />
        <div style={{ display: 'flex', gap: 6 }}>
          {[0, 1, 2].map(i => (
            <div key={i} style={{ width: 26, height: 26, borderRadius: 4, background: 'var(--color-border)' }} />
          ))}
        </div>
      </article>
    );
  }

  const displayName = riotId?.split('#')[0] || riotId || '—';
  const eloVal = Number.isFinite(data.elo) ? Math.round(data.elo!) : null;
  const eloColor =
    eloVal === null
      ? 'var(--color-text-secondary)'
      : eloVal >= 1200
        ? 'var(--color-win)'
        : eloVal >= 1000
          ? 'var(--color-primary)'
          : 'var(--color-loss)';

  const showCount = expanded ? data.championStats.length : 3;
  const visibleChamps = data.championStats.slice(0, showCount);
  const hasMore = data.championStats.length > 3;

  return (
    <article
      role="article"
      aria-label={`플레이어 ${displayName}`}
      style={{
        ...cardStyle,
        cursor: hasMore ? 'pointer' : 'default',
      }}
      onClick={() => { if (hasMore) setExpanded(e => !e); }}
    >
      {/* 닉네임 */}
      <h3
        role="heading"
        aria-level={3}
        aria-label={`닉네임: ${displayName}`}
        style={{
          fontSize: 'var(--font-size-sm)',
          fontWeight: 600,
          color: 'var(--color-text-primary)',
          marginBottom: 2,
          overflow: 'hidden',
          textOverflow: 'ellipsis',
          whiteSpace: 'nowrap',
        }}
      >
        {displayName}
      </h3>

      {/* Elo */}
      <div
        style={{
          fontSize: 'var(--font-size-xs)',
          fontFamily: "'Consolas', 'D2Coding', monospace",
          color: eloColor,
          marginBottom: 8,
        }}
      >
        {eloVal !== null ? `Elo ${eloVal}` : 'Elo —'}
      </div>

      {/* 챔피언 아이콘 + 승률 */}
      <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', alignItems: 'flex-end' }}>
        {visibleChamps.map((c, i) => (
          <div key={`${c.championId}-${i}`} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2 }}>
            <ChampIcon id={c.championId} size={26} />
            <span
              style={{
                fontSize: 10,
                fontFamily: "'Consolas', 'D2Coding', monospace",
                color:
                  c.winRate >= 60
                    ? 'var(--color-loss)'
                    : c.winRate >= 50
                      ? 'var(--color-win)'
                      : 'var(--color-text-secondary)',
              }}
            >
              {c.winRate}%
            </span>
          </div>
        ))}
        {hasMore && (
          <div
            style={{
              fontSize: 10,
              color: 'var(--color-text-secondary)',
              alignSelf: 'center',
              userSelect: 'none',
            }}
          >
            {expanded ? '▲' : `+${data.championStats.length - 3}`}
          </div>
        )}
      </div>
    </article>
  );
}
