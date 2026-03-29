import { useNavigate } from 'react-router-dom';
import { useLeaderboard } from '@/hooks/useLeaderboard';
import { Skeleton } from '@/components/common/Skeleton';
import { InlineError } from '@/components/common/InlineError';

const CURRENT_RIOT_ID_KEY = 'lol-event:currentRiotId';

function eloTier(elo: number): { label: string; color: string } {
  if (elo >= 1300) return { label: 'Challenger', color: '#FFD700' };
  if (elo >= 1200) return { label: 'Master',     color: '#AA47BC' };
  if (elo >= 1100) return { label: 'Diamond',    color: '#0BC4B4' };
  if (elo >= 1000) return { label: 'Platinum',   color: '#4A9EFF' };
  if (elo >= 900)  return { label: 'Gold',       color: '#C89B3C' };
  if (elo >= 800)  return { label: 'Silver',     color: '#A8A8A8' };
  return                  { label: 'Bronze',     color: '#CD7F32' };
}

export function MobileHomePage() {
  const navigate = useNavigate();
  const { data, isLoading, error, refetch } = useLeaderboard();
  const currentRiotId = localStorage.getItem(CURRENT_RIOT_ID_KEY) || undefined;

  if (isLoading) {
    return (
      <div>
        <p className="m-section-title">Elo 리더보드</p>
        {Array.from({ length: 5 }).map((_, i) => (
          <div key={i} className="m-player-card" style={{ minHeight: 60 }}>
            <div className="m-player-card-header">
              <Skeleton style={{ width: 28, height: 28, borderRadius: '50%', flexShrink: 0 }} />
              <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 6 }}>
                <Skeleton style={{ height: 14, width: '60%', borderRadius: 4 }} />
                <Skeleton style={{ height: 11, width: '30%', borderRadius: 4 }} />
              </div>
              <div style={{ textAlign: 'right', display: 'flex', flexDirection: 'column', gap: 4 }}>
                <Skeleton style={{ height: 18, width: 40, borderRadius: 4 }} />
                <Skeleton style={{ height: 11, width: 28, borderRadius: 4, marginLeft: 'auto' }} />
              </div>
            </div>
          </div>
        ))}
      </div>
    );
  }

  if (error) {
    return <InlineError message="리더보드를 불러오지 못했습니다." onRetry={() => void refetch()} />;
  }

  if (!data || data.players.length === 0) {
    return <div className="m-empty">Elo 데이터가 없습니다</div>;
  }

  return (
    <div>
      <p className="m-section-title">Elo 리더보드</p>
      {data.players.map((p) => {
        const tier = eloTier(p.elo);
        const [name, tag] = p.riotId.split('#');
        const isSelf = currentRiotId !== undefined && p.riotId === currentRiotId;
        return (
          <div
            key={p.riotId}
            className="m-player-card"
            style={isSelf ? {
              background: 'rgba(11, 196, 180, 0.08)',
              borderColor: 'var(--color-primary)',
            } : undefined}
            onClick={() => navigate(`/m/player/${encodeURIComponent(p.riotId)}`)}
          >
            <div className="m-player-card-header">
              <div className={`m-player-rank${p.rank <= 3 ? ` rank-${p.rank}` : ''}`}>{p.rank}</div>
              <div style={{ flex: 1 }}>
                <span className="m-player-name">{name}</span>
                {tag && <span className="m-player-tag"> #{tag}</span>}
                <div style={{ fontSize: 11, color: tier.color, marginTop: 1 }}>{tier.label}</div>
              </div>
              <div style={{ textAlign: 'right' }}>
                <div style={{ fontSize: 18, fontWeight: 800, color: tier.color }}>{p.elo.toFixed(1)}</div>
                <div style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>{p.games}게임</div>
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
}
