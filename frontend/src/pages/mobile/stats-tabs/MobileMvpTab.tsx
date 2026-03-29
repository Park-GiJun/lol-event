import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../../lib/api/api';
import type { MvpStatsResult } from '../../../lib/types/stats';
import { LoadingCenter } from '../../../components/common/Spinner';
import { MobilePlayerCard } from '../../../components/mobile/MobilePlayerCard';

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
      {data.rankings.map((p, i) => (
        <MobilePlayerCard
          key={p.riotId}
          riotId={p.riotId}
          rank={i + 1}
          rightSlot={<span className="m-player-games">{p.games}게임</span>}
          onClick={() => navigate(`/m/player/${encodeURIComponent(p.riotId)}`)}
        >
          <span className="m-stat-chip">MVP {p.mvpCount}회</span>
          <span className="m-stat-chip">ACE {p.aceCount}회</span>
          <span className="m-stat-chip">평균점수 {p.avgMvpScore.toFixed(1)}</span>
        </MobilePlayerCard>
      ))}
      {data.rankings.length === 0 && <div className="m-empty">MVP 데이터가 없습니다</div>}
    </div>
  );
}
