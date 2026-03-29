import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../../lib/api/api';
import type { SurvivalIndexResult, SurvivalIndexEntry } from '../../lib/types/stats';
import { LoadingCenter } from '../../components/common/Spinner';
import { RankBadge } from './shared';

export default function SurvivalTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const [data, setData]       = useState<SurvivalIndexResult | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try { setData(await api.get<SurvivalIndexResult>(`/stats/survival-index?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  return (
    <div>
      <p style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginBottom: 16 }}>
        생존력 & 탱킹 지수 — 높을수록 팀의 방패
      </p>
      <div className="table-wrapper">
        <table className="table member-stats-table">
          <thead>
            <tr>
              <th style={{ width: 36 }}>#</th>
              <th>플레이어</th>
              <th className="table-number">판수</th>
              <th className="table-number">탱킹 기여(%)</th>
              <th className="table-number">피해 감소율(%)</th>
              <th className="table-number">생존 지수</th>
            </tr>
          </thead>
          <tbody>
            {data.rankings.map((p: SurvivalIndexEntry, i) => (
              <tr key={p.riotId} className="member-stats-row"
                onClick={() => navigate(`/player-stats/${encodeURIComponent(p.riotId)}`)}>
                <td><RankBadge rank={i + 1} /></td>
                <td>
                  <div style={{ fontWeight: 700, fontSize: 13 }}>{p.riotId.split('#')[0]}</div>
                  <div style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>#{p.riotId.split('#')[1]}</div>
                </td>
                <td className="table-number">{p.games}</td>
                <td className="table-number">{(p.avgTankShare * 100).toFixed(1)}%</td>
                <td className="table-number">{(p.avgMitigationRatio * 100).toFixed(1)}%</td>
                <td className="table-number" style={{ fontWeight: 700, color: 'var(--color-primary)' }}>
                  {p.survivalIndex.toFixed(1)}
                </td>
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
