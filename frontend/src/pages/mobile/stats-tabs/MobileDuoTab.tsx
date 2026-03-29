import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../../lib/api/api';
import type { DuoStatsResult } from '../../../lib/types/stats';
import { LoadingCenter } from '../../../components/common/Spinner';

export default function MobileDuoTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-duo', mode],
    queryFn: () => api.get<DuoStatsResult>(`/stats/duo?mode=${mode}&minGames=2`),
  });

  if (isLoading) return <LoadingCenter />;
  if (!data) return <div className="m-empty">데이터 없음</div>;

  return (
    <div>
      {data.duos.map((d, i) => {
        const [name1] = d.player1.split('#');
        const [name2] = d.player2.split('#');
        return (
          <div key={i} className="m-synergy-card">
            <div style={{ flex: 1 }}>
              <div style={{ display: 'flex', gap: 6, alignItems: 'center', marginBottom: 4 }}>
                <button
                  onClick={() => navigate(`/m/player/${encodeURIComponent(d.player1)}`)}
                  style={{ fontWeight: 700, fontSize: 13, background: 'none', border: 'none', color: 'var(--color-text-primary)', cursor: 'pointer', padding: 0 }}
                >
                  {name1}
                </button>
                <span style={{ fontSize: 11, color: 'var(--color-text-disabled)' }}>+</span>
                <button
                  onClick={() => navigate(`/m/player/${encodeURIComponent(d.player2)}`)}
                  style={{ fontWeight: 700, fontSize: 13, background: 'none', border: 'none', color: 'var(--color-text-primary)', cursor: 'pointer', padding: 0 }}
                >
                  {name2}
                </button>
              </div>
              <div className="m-stat-chips">
                <span className="m-stat-chip">{d.games}게임</span>
                <span className="m-stat-chip" style={{ color: d.winRate >= 60 ? 'var(--color-win)' : 'inherit' }}>
                  {d.winRate.toFixed(1)}%
                </span>
                <span className="m-stat-chip">KDA {d.kda.toFixed(2)}</span>
              </div>
            </div>
          </div>
        );
      })}
      {data.duos.length === 0 && <div className="m-empty">듀오 데이터가 없습니다</div>}
    </div>
  );
}
