import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../../lib/api/api';
import type { EloLeaderboardResult } from '../../../lib/types/stats';
import { LoadingCenter } from '../../../components/common/Spinner';

function eloTier(elo: number) {
  if (elo >= 1300) return { label: 'Challenger', color: '#FFD700' };
  if (elo >= 1200) return { label: 'Master',     color: '#AA47BC' };
  if (elo >= 1100) return { label: 'Diamond',    color: '#0BC4B4' };
  if (elo >= 1000) return { label: 'Platinum',   color: '#4A9EFF' };
  if (elo >= 900)  return { label: 'Gold',       color: '#C89B3C' };
  if (elo >= 800)  return { label: 'Silver',     color: '#A8A8A8' };
  return              { label: 'Bronze',     color: '#CD7F32' };
}

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
      {data.players.map((p, i) => {
        const tier = eloTier(p.elo);
        const [name, tag] = p.riotId.split('#');
        return (
          <div key={p.riotId} className="m-player-card"
            onClick={() => navigate(`/m/player/${encodeURIComponent(p.riotId)}`)}>
            <div className="m-player-card-header">
              <div className={`m-player-rank${i < 3 ? ` rank-${i + 1}` : ''}`}>{p.rank}</div>
              <div style={{ flex: 1 }}>
                <span className="m-player-name">{name}</span>
                {tag && <span className="m-player-tag"> #{tag}</span>}
                <div style={{ fontSize: 11, color: tier.color, marginTop: 1 }}>{tier.label}</div>
              </div>
              <div style={{ textAlign: 'right' }}>
                <div style={{ fontSize: 18, fontWeight: 800, color: tier.color }}>{p.elo.toFixed(1)}</div>
                <div style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>
                  {p.games > 0 ? `${p.winRate.toFixed(1)}% · ${p.wins}승 ${p.losses}패` : `${p.games}게임`}
                </div>
                {(p.winStreak >= 3 || p.lossStreak >= 3) && (
                  <div style={{ fontSize: 10, marginTop: 2 }}>
                    {p.winStreak >= 3
                      ? <span style={{ color: '#FF6B2B' }}>🔥 {p.winStreak}연승</span>
                      : <span style={{ color: '#6BAAFF' }}>🧊 {p.lossStreak}연패</span>
                    }
                  </div>
                )}
              </div>
            </div>
          </div>
        );
      })}
      {data.players.length === 0 && <div className="m-empty">Elo 데이터가 없습니다</div>}
    </div>
  );
}
