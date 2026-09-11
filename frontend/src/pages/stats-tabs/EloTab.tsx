import { TrophyIcon } from '@/components/icons/LolIcons';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../../lib/api/api';
import type { EloLeaderboardResult, EloRankEntry } from '../../lib/types/stats';
import { LoadingCenter } from '../../components/common/Spinner';
import { PlayerLink } from '../../components/common/PlayerLink';
import { RankBadge } from './shared';
import { PlacementBadge } from '@/components/ds/PlacementBadge';

/**
 * Elo 구간 이름과 색.
 *
 * 원래 값은 다크 배경 기준이라 흰 바탕에서 대비가 무너진다
 * (#FFD700 골드는 흰색 위에서 1.4:1). 같은 계열을 유지하되 명도를 낮춰
 * 본문 대비 기준을 넘기도록 맞춘 값이다.
 *
 * 경계는 시작점이 1000 에서 1500 으로 바뀌면서 통째로 +500 옮겼다.
 * 기준점(Platinum 하단)이 시작점과 같아야 "아무것도 안 한 사람"이 한가운데에 선다.
 */
function eloTier(elo: number): { label: string; color: string } {
  if (elo >= 1800) return { label: 'Challenger', color: '#B07A00' };
  if (elo >= 1700) return { label: 'Master',     color: '#8E24AA' };
  if (elo >= 1600) return { label: 'Diamond',    color: '#00796B' };
  if (elo >= 1500) return { label: 'Platinum',   color: '#1B64DA' };
  if (elo >= 1400) return { label: 'Gold',       color: '#96731F' };
  if (elo >= 1300) return { label: 'Silver',     color: '#6B7684' };
  return                  { label: 'Bronze',     color: '#8A5524' };
}

/**
 * 편차에 색을 줄 기준. 이보다 작으면 노이즈라 회색으로 둔다.
 *
 * 한 경기의 팀 Elo 변동이 K=16 기준 최대 16점이라, 50 이면 서너 경기가 한 방향으로
 * 쏠려야 닿는 값이다. 그쯤 돼야 "편성자의 평가와 라인 실적이 어긋나 있다"고 말할 수 있다.
 */
const GAP_NOTABLE = 50;

/** 양수면 라인 실적에 비해 이기는 팀에 자주 들어갔다는 뜻, 음수면 그 반대다. */
function gapColor(gap: number): string {
  if (Math.abs(gap) < GAP_NOTABLE) return 'var(--color-text-secondary)';
  return gap > 0 ? 'var(--color-win)' : 'var(--color-loss)';
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
        <TrophyIcon size={16} />
        <span className="section-head-title">Elo 랭킹</span>
      </div>
      <p style={{
        fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)',
        margin: '0 0 var(--spacing-sm)', lineHeight: 1.6,
      }}>
        <b>실력</b>은 같은 자리 상대와의 라인 맞대결로만, <b>전적</b>은 팀 승패로만 움직입니다.
        둘을 합치지 않는 이유는 이 내전이 편성자가 팀을 직접 짜는 방식이라, 팀 승패에는 실력이 아니라
        편성자의 추정 오차가 남기 때문입니다. 순위는 <b>실력</b> 기준입니다.
      </p>
      <div className="table-wrapper">
        <table className="table member-stats-table">
          <thead>
            <tr>
              <th style={{ width: 48 }}>순위</th>
              <th>플레이어</th>
              <th>티어</th>
              <th className="table-number">실력 Elo</th>
              <th className="table-number">라인</th>
              <th className="table-number">전적 Elo</th>
              <th className="table-number">편차</th>
              <th className="table-number">판수</th>
            </tr>
          </thead>
          <tbody>
            {data.players.map((entry: EloRankEntry) => {
              // 표시·정렬 기준은 수축을 먹인 표시값이다. 원값은 표본이 적으면 과하게 튄다.
              const tier = eloTier(entry.laneEloDisplay);
              return (
                <tr
                  key={entry.riotId}
                  className="member-stats-row"
                  style={entry.placement ? { opacity: 0.65 } : undefined}
                  onClick={() => navigate(`/player-stats/${encodeURIComponent(entry.riotId)}`)}>
                  <td>
                    {entry.placement
                      ? <span style={{ color: 'var(--color-text-disabled)', fontSize: 12, width: 26, textAlign: 'center', display: 'inline-block' }}>–</span>
                      : <RankBadge rank={entry.rank} />}
                  </td>
                  <td>
                    <PlayerLink riotId={entry.riotId} mode="all">
                      <span style={{ fontWeight: 600 }}>
                        {entry.riotId.split('#')[0]}
                      </span>
                      <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)', marginLeft: 'var(--spacing-xs)' }}>
                        #{entry.riotId.split('#')[1]}
                      </span>
                    </PlayerLink>
                    {entry.placement && <PlacementBadge minDuels={data.minDuels} />}
                  </td>
                  <td>
                    <span style={{ fontSize: 'var(--font-size-xs)', fontWeight: 700, color: tier.color,
                      background: tier.color + '22', borderRadius: 'var(--radius-sm)', padding: '2px 7px',
                      border: `1px solid ${tier.color}44` }}>
                      {tier.label}
                    </span>
                  </td>
                  <td className="table-number" style={{ fontWeight: 700, color: tier.color }}>
                    {entry.laneEloDisplay.toFixed(1)}
                  </td>
                  <td
                    className="table-number"
                    style={{ color: 'var(--color-text-secondary)' }}
                    title={`라인 ${entry.laneWins}승 ${entry.laneLosses}패 · ${Math.round(entry.laneWinRate * 100)}%`}
                  >
                    {entry.laneDuels}
                  </td>
                  <td className="table-number" style={{ color: 'var(--color-text-secondary)' }}>
                    {entry.teamEloDisplay.toFixed(1)}
                  </td>
                  <td className="table-number" style={{ color: gapColor(entry.gap), fontWeight: Math.abs(entry.gap) >= GAP_NOTABLE ? 700 : 400 }}>
                    {entry.gap > 0 ? '+' : ''}{Math.round(entry.gap)}
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
