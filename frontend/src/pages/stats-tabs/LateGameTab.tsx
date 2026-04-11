import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../../lib/api/api';
import type { LateGameResult, LateGamePlayerEntry } from '../../lib/types/stats';
import { LoadingCenter } from '../../components/common/Spinner';
import { RankBadge } from './shared';

function formatTime(seconds: number): string {
  const m = Math.floor(seconds / 60);
  const s = Math.floor(seconds % 60);
  return `${m}:${String(s).padStart(2, '0')}`;
}

export default function LateGameTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const [data, setData]       = useState<LateGameResult | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try { setData(await api.get<LateGameResult>(`/stats/late-game?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  return (
    <div>
      {data.lateGameKing && (
        <div style={{ marginBottom: 16, padding: '10px 14px', borderRadius: 8, background: 'rgba(167,139,250,0.1)', border: '1px solid rgba(167,139,250,0.25)', display: 'flex', alignItems: 'center', gap: 10 }}>
          <span style={{ fontSize: 22 }}>👑</span>
          <div>
            <div style={{ fontSize: 10, color: 'var(--color-text-secondary)', fontWeight: 700, letterSpacing: '0.05em', textTransform: 'uppercase' }}>후반 지배왕</div>
            <div style={{ fontWeight: 800, fontSize: 14, color: '#a78bfa' }}>{data.lateGameKing.split('#')[0]}</div>
          </div>
        </div>
      )}
      <p style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginBottom: 16 }}>
        후반 영향력 — 억제기 파괴, 생존 시간, 킬링 스프리 종합 지수
      </p>
      <div className="table-wrapper">
        <table className="table member-stats-table">
          <thead>
            <tr>
              <th style={{ width: 36 }}>#</th>
              <th>플레이어</th>
              <th className="table-number">판수</th>
              <th className="table-number">억제기 파괴</th>
              <th className="table-number">평균 생존</th>
              <th className="table-number">킬링 스프리</th>
              <th className="table-number">최장 스프리</th>
              <th className="table-number">후반 지수</th>
            </tr>
          </thead>
          <tbody>
            {data.rankings.map((p: LateGamePlayerEntry, i) => (
              <tr key={p.riotId} className="member-stats-row"
                onClick={() => navigate(`/player-stats/${encodeURIComponent(p.riotId)}`)}>
                <td><RankBadge rank={i + 1} /></td>
                <td>
                  <div style={{ fontWeight: 700, fontSize: 13 }}>{p.riotId.split('#')[0]}</div>
                  <div style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>
                    #{p.riotId.split('#')[1]}
                    {p.topChampion && <span> · {p.topChampion}</span>}
                  </div>
                </td>
                <td className="table-number">{p.games}</td>
                <td className="table-number">{p.avgInhibitorKills.toFixed(2)}</td>
                <td className="table-number">{formatTime(p.avgSurvivalSeconds)}</td>
                <td className="table-number">{p.avgKillingSpree.toFixed(2)}</td>
                <td className="table-number">{p.longestKillingSpree}</td>
                <td className="table-number" style={{ fontWeight: 700, color: '#a78bfa' }}>
                  {p.lateGameScore.toFixed(1)}
                </td>
              </tr>
            ))}
            {!data.rankings.length && (
              <tr><td colSpan={8} style={{ textAlign: 'center', padding: '40px 0', color: 'var(--color-text-secondary)' }}>데이터 없음</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
