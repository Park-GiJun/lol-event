import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../../lib/api/api';
import type { LaneLeaderboardResult } from '../../../lib/types/stats';
import { LoadingCenter } from '../../../components/common/Spinner';

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
          {data?.players.map((p, i) => {
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
                  <span className="m-player-games">{p.games}판</span>
                </div>
                <div className="m-win-bar-wrap">
                  <div className="m-win-bar-label">
                    <span>{p.winRate.toFixed(1)}%</span>
                    <span>{p.wins}승 {p.games - p.wins}패</span>
                  </div>
                  <div className="m-win-bar">
                    <div className="m-win-bar-fill" style={{ width: `${p.winRate}%` }} />
                  </div>
                </div>
                <div className="m-stat-chips">
                  <span className="m-stat-chip">KDA {p.kda.toFixed(2)}</span>
                  <span className="m-stat-chip">딜 {Math.round(p.avgDamage).toLocaleString()}</span>
                  <span className="m-stat-chip">CS {p.avgCs.toFixed(1)}</span>
                </div>
              </div>
            );
          })}
        </>
      )}
    </div>
  );
}
