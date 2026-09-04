import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../../lib/api/api';
import type { EloLeaderboardResult, EloRankEntry } from '../../lib/types/stats';
import { LoadingCenter } from '../../components/common/Spinner';
import { PlayerLink } from '../../components/common/PlayerLink';
import { RankBadge } from './shared';

/**
 * Elo 구간 이름과 색.
 *
 * 원래 값은 다크 배경 기준이라 흰 바탕에서 대비가 무너진다
 * (#FFD700 골드는 흰색 위에서 1.4:1). 같은 계열을 유지하되 명도를 낮춰
 * 본문 대비 기준을 넘기도록 맞춘 값이다.
 */
function eloTier(elo: number): { label: string; color: string } {
  if (elo >= 1300) return { label: 'Challenger', color: '#B07A00' };
  if (elo >= 1200) return { label: 'Master',     color: '#8E24AA' };
  if (elo >= 1100) return { label: 'Diamond',    color: '#00796B' };
  if (elo >= 1000) return { label: 'Platinum',   color: '#1B64DA' };
  if (elo >= 900)  return { label: 'Gold',       color: '#96731F' };
  if (elo >= 800)  return { label: 'Silver',     color: '#6B7684' };
  return                  { label: 'Bronze',     color: '#8A5524' };
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
    <p style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-text-secondary)', padding: 'var(--spacing-lg) 0' }}>
      Elo 데이터가 없습니다. 어드민에서 재집계를 실행하세요.
    </p>
  );

  return (
    <div>
      <div className="section-head">
        <span className="icon-chip">🏆</span>
        <span className="section-head-title">Elo 랭킹</span>
      </div>
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
                      <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)', marginLeft: 'var(--spacing-xs)' }}>
                        #{entry.riotId.split('#')[1]}
                      </span>
                    </PlayerLink>
                  </td>
                  <td>
                    <span style={{ fontSize: 'var(--font-size-xs)', fontWeight: 700, color: tier.color,
                      background: tier.color + '22', borderRadius: 'var(--radius-sm)', padding: '2px 7px',
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
