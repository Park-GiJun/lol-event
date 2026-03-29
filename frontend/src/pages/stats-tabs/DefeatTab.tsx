import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../../lib/api/api';
import type { DefeatContributionResult, DefeatContributionEntry } from '../../lib/types/stats';
import { LoadingCenter } from '../../components/common/Spinner';
import { RankBadge } from './shared';

export default function DefeatTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const [data, setData]       = useState<DefeatContributionResult | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try { setData(await api.get<DefeatContributionResult>(`/stats/defeat-contribution?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  return (
    <div>
      <p style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginBottom: 16 }}>
        패배 기여도 — 높을수록 팀을 더 힘들게 합니다
      </p>
      <div className="table-wrapper">
        <table className="table member-stats-table">
          <thead>
            <tr>
              <th style={{ width: 36 }}>#</th>
              <th>플레이어</th>
              <th className="table-number">판수</th>
              <th className="table-number">패배수</th>
              <th className="table-number">평균 Defeat Score</th>
              <th className="table-number">평균 사망</th>
            </tr>
          </thead>
          <tbody>
            {data.rankings.map((p: DefeatContributionEntry, i) => (
              <tr key={p.riotId} className="member-stats-row"
                onClick={() => navigate(`/player-stats/${encodeURIComponent(p.riotId)}`)}>
                <td><RankBadge rank={i + 1} /></td>
                <td>
                  <div style={{ fontWeight: 700, fontSize: 13 }}>{p.riotId.split('#')[0]}</div>
                  <div style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>#{p.riotId.split('#')[1]}</div>
                </td>
                <td className="table-number">{p.games}</td>
                <td className="table-number" style={{ color: 'var(--color-loss)', fontWeight: 700 }}>{p.losses}</td>
                <td className="table-number" style={{ fontWeight: 700, color: 'var(--color-error)' }}>
                  {p.avgDefeatScore.toFixed(1)}
                </td>
                <td className="table-number" style={{ color: 'var(--color-text-secondary)' }}>{p.avgDeaths.toFixed(1)}</td>
              </tr>
            ))}
            {!data.rankings.length && (
              <tr><td colSpan={6} style={{ textAlign: 'center', padding: '40px 0', color: 'var(--color-text-secondary)' }}>데이터 없음</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
