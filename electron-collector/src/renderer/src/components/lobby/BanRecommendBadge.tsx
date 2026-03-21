import { Star } from 'lucide-react';

const CDN = 'https://raw.communitydragon.org/latest/plugins/rcp-be-lol-game-data/global/default/v1/champion-icons';

function champIconUrl(id: number): string {
  return `${CDN}/${id}.png`;
}

function ChampIcon({ id, size = 22 }: { id: number; size?: number }) {
  if (!id) return <div style={{ width: size, height: size, borderRadius: 4, background: 'var(--color-bg-hover)', flexShrink: 0 }} />;
  return (
    <img
      src={champIconUrl(id)}
      alt=""
      width={size}
      height={size}
      style={{ borderRadius: 4, objectFit: 'cover', flexShrink: 0 }}
      onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }}
    />
  );
}

export interface BanRecommendBadgeProps {
  champion: string;
  championId: number;
  isHighThreat: boolean;
  winRate?: number;
  games?: number;
}

export function BanRecommendBadge({
  champion,
  championId,
  isHighThreat,
  winRate,
  games,
}: BanRecommendBadgeProps) {
  const bg = isHighThreat ? 'rgba(232, 64, 64, 0.1)' : 'var(--color-bg-hover)';
  const border = isHighThreat ? '1px solid rgba(232, 64, 64, 0.3)' : '1px solid var(--color-border)';

  const wrColor =
    winRate === undefined
      ? 'var(--color-text-secondary)'
      : winRate >= 60
        ? 'var(--color-error)'
        : winRate >= 50
          ? 'var(--color-win)'
          : 'var(--color-text-secondary)';

  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 6,
      padding: '4px 8px', borderRadius: 'var(--radius-sm)',
      background: bg, border,
    }}>
      <ChampIcon id={championId} size={22} />
      <div>
        <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-primary)' }}>
          {champion}
        </div>
        {(winRate !== undefined || games !== undefined) && (
          <div style={{
            fontSize: 10,
            color: wrColor,
            fontFamily: "'Consolas', 'D2Coding', monospace",
          }}>
            {winRate !== undefined ? `${winRate}%` : ''}
            {winRate !== undefined && games !== undefined ? ' ' : ''}
            {games !== undefined ? `(${games}판)` : ''}
          </div>
        )}
      </div>
      {isHighThreat && (
        <Star size={10} style={{ color: 'var(--color-warning)', flexShrink: 0 }} />
      )}
    </div>
  );
}
