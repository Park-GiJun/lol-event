import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../../lib/api/api';
import type { EloLeaderboardResult, EloRankEntry } from '../../lib/types/stats';
import { LoadingCenter } from '../../components/common/Spinner';
import { PlayerLink } from '../../components/common/PlayerLink';
import { RankBadge } from './shared';

function eloTier(elo: number): { label: string; color: string } {
  if (elo >= 1300) return { label: 'Challenger', color: '#FFD700' };
  if (elo >= 1200) return { label: 'Master',     color: '#AA47BC' };
  if (elo >= 1100) return { label: 'Diamond',    color: '#0BC4B4' };
  if (elo >= 1000) return { label: 'Platinum',   color: '#4A9EFF' };
  if (elo >= 900)  return { label: 'Gold',       color: '#C89B3C' };
  if (elo >= 800)  return { label: 'Silver',     color: '#A8A8A8' };
  return                  { label: 'Bronze',     color: '#CD7F32' };
}

export default function EloTab() {
  const navigate = useNavigate();
  const [data, setData] = useState<EloLeaderboardResult | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.get<EloLeaderboardResult>('/stats/elo')
      .then(setData)
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <LoadingCenter />;
  if (!data || data.players.length === 0) return (
    <p style={{ fontSize: 13, color: 'var(--color-text-secondary)', padding: '24px 0' }}>
      Elo 데이터가 없습니다. 어드민에서 재집계를 실행하세요.
    </p>
  );

  return (
    <div>
      <div style={{ fontWeight: 700, marginBottom: 16, fontSize: 'var(--font-size-sm)', color: 'var(--color-text-secondary)', letterSpacing: 'var(--tracking-wide)', textTransform: 'uppercase' }}>Elo 랭킹</div>
      <div className="table-wrapper">
        <table className="table member-stats-table">
          <thead>
            <tr>
              <th style={{ width: 48 }}>순위</th>
              <th>플레이어</th>
              <th>티어</th>
              <th className="table-number">Elo</th>
              <th className="table-number">판수</th>
            </tr>
          </thead>
          <tbody>
            {data.players.map((entry: EloRankEntry) => {
              const tier = eloTier(entry.elo);
              return (
                <tr key={entry.riotId}
                  className="member-stats-row"
                  onClick={() => navigate(`/player-stats/${encodeURIComponent(entry.riotId)}`)}>
                  <td><RankBadge rank={entry.rank} /></td>
                  <td>
                    <PlayerLink riotId={entry.riotId} mode="all">
                      <span style={{ fontWeight: 600 }}>
                        {entry.riotId.split('#')[0]}
                      </span>
                      <span style={{ fontSize: 10, color: 'var(--color-text-secondary)', marginLeft: 4 }}>
                        #{entry.riotId.split('#')[1]}
                      </span>
                    </PlayerLink>
                  </td>
                  <td>
                    <span style={{ fontSize: 11, fontWeight: 700, color: tier.color,
                      background: tier.color + '22', borderRadius: 4, padding: '2px 7px',
                      border: `1px solid ${tier.color}44` }}>
                      {tier.label}
                    </span>
                  </td>
                  <td className="table-number" style={{ fontWeight: 700, color: tier.color }}>
                    {entry.elo.toFixed(1)}
                  </td>
                  <td className="table-number" style={{ color: 'var(--color-text-secondary)' }}>
                    {entry.games}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
