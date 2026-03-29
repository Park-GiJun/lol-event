import { useNavigate } from 'react-router-dom';
import { useLeaderboard } from '@/hooks/useLeaderboard';
import { Skeleton } from '@/components/common/Skeleton';
import { InlineError } from '@/components/common/InlineError';
import { eloTier } from '@/lib/lol';
import { MobilePlayerCard } from '@/components/mobile/MobilePlayerCard';

const CURRENT_RIOT_ID_KEY = 'lol-event:currentRiotId';

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
        const isSelf = currentRiotId !== undefined && p.riotId === currentRiotId;
        return (
          <MobilePlayerCard
            key={p.riotId}
            riotId={p.riotId}
            rank={p.rank}
            subText={<span style={{ fontSize: 11, color: tier.color }}>{tier.label}</span>}
            rightSlot={
              <div style={{ textAlign: 'right' }}>
                <div style={{ fontSize: 18, fontWeight: 800, color: tier.color }}>{p.elo.toFixed(1)}</div>
                <div style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>{p.games}게임</div>
              </div>
            }
            highlight={isSelf}
            onClick={() => navigate(`/m/player/${encodeURIComponent(p.riotId)}`)}
          />
        );
      })}
    </div>
  );
}
