import { useDragon } from '../../context/DragonContext';
import { ChampionLink } from '../../components/common/ChampionLink';
import type { ChampionPickStat, PlayerLeaderStat } from '../../lib/types/stats';
import { useNavigate } from 'react-router-dom';
import { PlayerLink } from '../../components/common/PlayerLink';

// eslint-disable-next-line react-refresh/only-export-components
export function champImgUrl(championId: number, champions: ReturnType<typeof useDragon>['champions']): string | null {
  return champions.get(championId)?.imageUrl ?? null;
}

export function WinRateBar({ winRate, wins, losses }: { winRate: number; wins: number; losses: number }) {
  const color = winRate >= 60 ? 'var(--color-win)' : winRate >= 50 ? 'var(--color-primary)' : 'var(--color-loss)';
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 3, minWidth: 90 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
        <span style={{ fontWeight: 700, fontSize: 13, color, minWidth: 36 }}>{winRate}%</span>
        <span style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>
          <span style={{ color: 'var(--color-win)' }}>{wins}W</span>
          {' '}<span style={{ color: 'var(--color-loss)' }}>{losses}L</span>
        </span>
      </div>
      <div style={{ height: 4, background: 'var(--color-bg-hover)', borderRadius: 2, overflow: 'hidden' }}>
        <div style={{ width: `${winRate}%`, height: '100%', background: color, borderRadius: 2, transition: 'width 0.3s' }} />
      </div>
    </div>
  );
}

export function ChampImg({ championId, champion, size }: { championId: number; champion: string; size: number }) {
  const { champions } = useDragon();
  const data = champions.get(championId);
  if (data?.imageUrl) return (
    <img src={data.imageUrl} alt={champion} width={size} height={size}
      style={{ borderRadius: 4, border: '1px solid var(--color-border)', objectFit: 'cover' }}
      onError={e => { (e.currentTarget as HTMLImageElement).style.display = 'none'; }} />
  );
  return <div style={{ width: size, height: size, background: 'var(--color-bg-hover)', borderRadius: 4, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 9, color: 'var(--color-text-secondary)' }}>{champion.slice(0, 2)}</div>;
}

// eslint-disable-next-line react-refresh/only-export-components
export const RANK_COLORS: Record<number, string> = { 1: '#FFD700', 2: '#C0C0C0', 3: '#CD7F32' };
export function RankBadge({ rank }: { rank: number }) {
  const color = RANK_COLORS[rank];
  if (color) return (
    <div style={{ width: 26, height: 26, borderRadius: '50%', background: color, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 11, fontWeight: 800, color: '#111', flexShrink: 0 }}>{rank}</div>
  );
  return <span style={{ color: 'var(--color-text-disabled)', fontSize: 12, width: 26, textAlign: 'center', display: 'inline-block' }}>{rank}</span>;
}

export function ChampPickCard({ stat, champions, onClick, countLabel, imgStyle }: {
  stat: ChampionPickStat;
  champions: ReturnType<typeof useDragon>['champions'];
  onClick?: () => void;
  countLabel?: string;
  imgStyle?: React.CSSProperties;
}) {
  const imgUrl = champImgUrl(stat.championId, champions);
  const nameKo = champions.get(stat.championId)?.nameKo || stat.champion;
  const wrColor = stat.winRate >= 60 ? 'var(--color-win)' : stat.winRate >= 50 ? 'var(--color-primary)' : 'var(--color-loss)';
  return (
    <ChampionLink champion={stat.champion} championId={stat.championId} className="popup-trigger--card">
      <div className="champ-pick-card" onClick={onClick} style={{ cursor: 'pointer' }}>
        <div className="champ-pick-img-wrap">
          {imgUrl
            ? <img src={imgUrl} alt={nameKo} className="champ-pick-img" style={imgStyle} onError={e => { (e.target as HTMLImageElement).style.display = 'none'; }} />
            : <div className="champ-pick-img-fallback">{nameKo.slice(0, 2)}</div>
          }
        </div>
        <div className="champ-pick-name">{nameKo}</div>
        <div className="champ-pick-meta">
          <span className="champ-pick-count">{countLabel ?? `${stat.picks}픽`}</span>
          {!countLabel && stat.winRate > 0 && <span className="champ-pick-wr" style={{ color: wrColor }}>{stat.winRate}%</span>}
        </div>
      </div>
    </ChampionLink>
  );
}

export function BasisBadge({ basis }: { basis: 'per-min' | 'per-match' | 'total' }) {
  const map = { 'per-min': { text: '/분', color: '#7c6af7' }, 'per-match': { text: '/경기', color: '#4a9eff' }, 'total': { text: '누적', color: '#888' } };
  const { text, color } = map[basis];
  return <span style={{ fontSize: 9, fontWeight: 700, background: color + '22', color, borderRadius: 4, padding: '1px 5px', border: `1px solid ${color}44` }}>{text}</span>;
}

export function HallCard({ emoji, label, stat, basis, mode }: { emoji: string; label: string; stat: PlayerLeaderStat | null; basis: 'per-min' | 'per-match' | 'total'; mode?: string }) {
  const navigate = useNavigate();
  if (!stat) return (
    <div className="hall-card hall-card--empty">
      <div className="hall-emoji">{emoji}</div>
      <div className="hall-label">{label}</div>
      <div className="hall-empty-text">데이터 없음</div>
    </div>
  );
  return (
    <div className="hall-card" onClick={() => navigate(`/player-stats/${encodeURIComponent(stat.riotId)}`)}>
      <div className="hall-emoji">{emoji}</div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 4, justifyContent: 'center' }}>
        <div className="hall-label">{label}</div>
        <BasisBadge basis={basis} />
      </div>
      <PlayerLink riotId={stat.riotId} mode={mode}>
        <div className="hall-player">{stat.riotId.split('#')[0]}</div>
      </PlayerLink>
      <div className="hall-value">{stat.displayValue}</div>
      <div className="hall-games">{stat.games}판</div>
    </div>
  );
}
