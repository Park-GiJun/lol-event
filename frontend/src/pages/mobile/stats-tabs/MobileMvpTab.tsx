import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../../lib/api/api';
import type { MvpStatsResult } from '../../../lib/types/stats';
import { LoadingCenter } from '../../../components/common/Spinner';

export default function MobileMvpTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-mvp', mode],
    queryFn: () => api.get<MvpStatsResult>(`/stats/mvp?mode=${mode}`),
  });

  if (isLoading) return <LoadingCenter />;
  if (!data) return <div className="m-empty">데이터 없음</div>;

  return (
    <div>
      {data.rankings.map((p, i) => {
        const [name, tag] = p.riotId.split('#');
        return (
          <div key={p.riotId} className="m-player-card"
            onClick={() => navigate(`/m/player/${encodeURIComponent(p.riotId)}`)}>
            <div className="m-player-card-header">
              <div className={`m-player-rank${i < 3 ? ` rank-${i + 1}` : ''}`}>{i + 1}</div>
              <div style={{ flex: 1 }}>
                <span className="m-player-name">{name}</span>
                {tag && <span className="m-player-tag"> #{tag}</span>}
              </div>
              <span className="m-player-games">{p.games}게임</span>
            </div>
            <div className="m-stat-chips">
              <span className="m-stat-chip">MVP {p.mvpCount}회</span>
              <span className="m-stat-chip">ACE {p.aceCount}회</span>
              <span className="m-stat-chip">평균점수 {p.avgMvpScore.toFixed(1)}</span>
            </div>
          </div>
        );
      })}
      {data.rankings.length === 0 && <div className="m-empty">MVP 데이터가 없습니다</div>}
    </div>
  );
}
