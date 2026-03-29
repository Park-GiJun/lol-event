import { parseRiotId } from '@/lib/lol';

interface WinBarProps {
  winRate: number;
  wins: number;
  losses: number;
}

interface MobilePlayerCardProps {
  riotId: string;
  rank?: number;
  /** Right-side slot in the header row (games count, elo, etc.) */
  rightSlot?: React.ReactNode;
  /** Optional subtext under the name (tier label, etc.) */
  subText?: React.ReactNode;
  winBar?: WinBarProps;
  /** Chips row — pass <span className="m-stat-chip"> elements */
  children?: React.ReactNode;
  /** Extra content rendered after the chips row (e.g. champion icon strip) */
  footer?: React.ReactNode;
  onClick?: () => void;
  highlight?: boolean;
  style?: React.CSSProperties;
}

/**
 * Shared mobile player card shell.
 *
 * Renders the standard `m-player-card` layout:
 *   [rank] [name #tag / subText]  [rightSlot]
 *   (optional win bar)
 *   (optional children = stat chips)
 */
export function MobilePlayerCard({
  riotId,
  rank,
  rightSlot,
  subText,
  winBar,
  children,
  footer,
  onClick,
  highlight,
  style,
}: MobilePlayerCardProps) {
  const { name, tag } = parseRiotId(riotId);

  return (
    <div
      role={onClick ? 'button' : undefined}
      tabIndex={onClick ? 0 : undefined}
      className="m-player-card"
      style={{
        minHeight: 44,
        ...(highlight ? { background: 'rgba(11,196,180,0.08)', borderColor: 'var(--color-primary)' } : {}),
        ...style,
      }}
      onClick={onClick}
      onKeyDown={onClick ? e => { if (e.key === 'Enter' || e.key === ' ') onClick(); } : undefined}
    >
      <div className="m-player-card-header">
        {rank !== undefined && (
          <div className={`m-player-rank${rank <= 3 ? ` rank-${rank}` : ''}`}>{rank}</div>
        )}
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: 4 }}>
            <span className="m-player-name">{name}</span>
            {tag && <span className="m-player-tag">#{tag}</span>}
          </div>
          {subText && <div style={{ marginTop: 1 }}>{subText}</div>}
        </div>
        {rightSlot !== undefined && <div style={{ flexShrink: 0 }}>{rightSlot}</div>}
      </div>

      {winBar && (
        <div className="m-win-bar-wrap">
          <div className="m-win-bar-label">
            <span>{winBar.winRate.toFixed(1)}%</span>
            <span>{winBar.wins}승 {winBar.losses}패</span>
          </div>
          <div className="m-win-bar">
            <div className="m-win-bar-fill" style={{ width: `${winBar.winRate}%` }} />
          </div>
        </div>
      )}

      {children && (
        <div className="m-stat-chips">{children}</div>
      )}
      {footer}
    </div>
  );
}
