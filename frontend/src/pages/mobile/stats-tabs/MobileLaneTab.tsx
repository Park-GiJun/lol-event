import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../../lib/api/api';
import type { LaneLeaderboardResult } from '../../../lib/types/stats';
import { LoadingCenter } from '../../../components/common/Spinner';
import { MobilePlayerCard } from '../../../components/mobile/MobilePlayerCard';

const LANES = ['TOP', 'JUNGLE', 'MID', 'BOTTOM', 'SUPPORT'] as const;
const LANE_LABELS: Record<string, string> = {
  TOP: 'TOP', JUNGLE: 'JGL', MID: 'MID', BOTTOM: 'BOT', SUPPORT: 'SUP',
};

export default function MobileLaneTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const [lane, setLane] = useState('TOP');
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-lane', lane, mode],
    queryFn: () => api.get<LaneLeaderboardResult>(`/stats/lane?lane=${lane}&mode=${mode}`),
  });

  return (
    <div>
      <div style={{ display: 'flex', gap: 6, marginBottom: 12, flexWrap: 'wrap' }}>
        {LANES.map(l => (
          <button key={l} className={`m-lane-tab${lane === l ? ' active' : ''}`} onClick={() => setLane(l)}>
            {LANE_LABELS[l]}
          </button>
        ))}
      </div>

      {isLoading ? <LoadingCenter /> : (
        <>
          {(!data || data.players.length === 0) && <div className="m-empty">데이터가 없습니다</div>}
          {data?.players.map((p, i) => (
            <MobilePlayerCard
              key={p.riotId}
              riotId={p.riotId}
              rank={i + 1}
              rightSlot={<span className="m-player-games">{p.games}판</span>}
              winBar={{ winRate: p.winRate, wins: p.wins, losses: p.games - p.wins }}
              onClick={() => navigate(`/m/player/${encodeURIComponent(p.riotId)}`)}
            >
              <span className="m-stat-chip">KDA {p.kda.toFixed(2)}</span>
              <span className="m-stat-chip">딜 {Math.round(p.avgDamage).toLocaleString()}</span>
              <span className="m-stat-chip">CS {p.avgCs.toFixed(1)}</span>
            </MobilePlayerCard>
          ))}
        </>
      )}
    </div>
  );
}
