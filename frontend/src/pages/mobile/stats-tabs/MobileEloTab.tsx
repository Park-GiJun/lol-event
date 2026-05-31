import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../../lib/api/api';
import type { EloLeaderboardResult } from '../../../lib/types/stats';
import { LoadingCenter } from '../../../components/common/Spinner';
import { eloTier } from '../../../lib/lol';
import { MobilePlayerCard } from '../../../components/mobile/MobilePlayerCard';

export default function MobileEloTab() {
  const navigate = useNavigate();
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-elo'],
    queryFn: () => api.get<EloLeaderboardResult>('/stats/elo'),
  });

  if (isLoading) return <LoadingCenter />;
  if (!data) return <div className="m-empty">데이터 없음</div>;

  return (
    <div>
      {data.players.map((p) => {
        const tier = eloTier(p.elo);
        return (
          <MobilePlayerCard
            key={p.riotId}
            riotId={p.riotId}
            rank={p.rank}
            subText={<span style={{ fontSize: 'var(--font-size-2xs)', color: tier.color }}>{tier.label}</span>}
            rightSlot={
              <div style={{ textAlign: 'right' }}>
                <div style={{ fontSize: 'var(--font-size-lg)', fontWeight: 800, color: tier.color }}>{p.elo.toFixed(1)}</div>
                <div style={{ fontSize: 'var(--font-size-2xs)', color: 'var(--color-text-secondary)' }}>
                  {p.games > 0 ? `${p.winRate.toFixed(1)}% · ${p.wins}승 ${p.losses}패` : `${p.games}게임`}
                </div>
                {(p.winStreak >= 3 || p.lossStreak >= 3) && (
                  <div style={{ fontSize: 'var(--font-size-2xs)', marginTop: 'var(--spacing-2xs)' }}>
                    {p.winStreak >= 3
                      ? <span style={{ color: 'var(--color-win)' }}>🔥 {p.winStreak}연승</span>
                      : <span style={{ color: 'var(--color-loss)' }}>🧊 {p.lossStreak}연패</span>
                    }
                  </div>
                )}
              </div>
            }
            onClick={() => navigate(`/m/player/${encodeURIComponent(p.riotId)}`)}
          />
        );
      })}
      {data.players.length === 0 && <div className="m-empty">Elo 데이터가 없습니다</div>}
    </div>
  );
}
