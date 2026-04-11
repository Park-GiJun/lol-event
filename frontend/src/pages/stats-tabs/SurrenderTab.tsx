import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../../lib/api/api';
import type { SurrenderAnalysisResult, SurrenderPlayerEntry } from '../../lib/types/stats';
import { LoadingCenter } from '../../components/common/Spinner';
import { RankBadge } from './shared';

export default function SurrenderTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const [data, setData]       = useState<SurrenderAnalysisResult | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try { setData(await api.get<SurrenderAnalysisResult>(`/stats/surrender-analysis?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  const funCards = [
    { emoji: '🏳️', label: '서렌더 유발왕', name: data.surrenderTrigger, color: '#f87171' },
    { emoji: '💪', label: '끝까지 안포기왕', name: data.neverGiveUpKing, color: '#4ade80' },
  ];

  return (
    <div>
      <div className="grid-16" style={{ marginBottom: 20 }}>
        <div className="card col-span-8" style={{ padding: '14px 12px', textAlign: 'center' }}>
          <div style={{ fontSize: 11, color: 'var(--color-text-secondary)', fontWeight: 700, marginBottom: 4 }}>전체 서렌더율</div>
          <div style={{ fontSize: 24, fontWeight: 800, color: '#f87171' }}>{(data.overallSurrenderRate * 100).toFixed(1)}%</div>
        </div>
        <div className="card col-span-8" style={{ padding: '14px 12px', textAlign: 'center' }}>
          <div style={{ fontSize: 11, color: 'var(--color-text-secondary)', fontWeight: 700, marginBottom: 4 }}>조기 서렌더율</div>
          <div style={{ fontSize: 24, fontWeight: 800, color: '#fb923c' }}>{(data.earlyOverallSurrenderRate * 100).toFixed(1)}%</div>
        </div>
        {funCards.map(c => (
          <div key={c.label} className="card col-span-8" style={{ padding: '14px 12px', textAlign: 'center' }}>
            <div style={{ fontSize: 22, marginBottom: 4 }}>{c.emoji}</div>
            <div style={{ fontSize: 10, color: 'var(--color-text-secondary)', fontWeight: 700, marginBottom: 4 }}>{c.label}</div>
            <div style={{ fontWeight: 800, fontSize: 14, color: c.color }}>{c.name?.split('#')[0] ?? '-'}</div>
          </div>
        ))}
      </div>

      <div className="table-wrapper">
        <table className="table member-stats-table">
          <thead>
            <tr>
              <th style={{ width: 36 }}>#</th>
              <th>플레이어</th>
              <th className="table-number">판수</th>
              <th className="table-number">서렌더 횟수</th>
              <th className="table-number">서렌더율</th>
              <th className="table-number">조기 서렌더</th>
              <th className="table-number">조기 서렌더율</th>
              <th className="table-number">서렌더 후 승률</th>
            </tr>
          </thead>
          <tbody>
            {data.rankings.map((p: SurrenderPlayerEntry, i) => (
              <tr key={p.riotId} className="member-stats-row"
                onClick={() => navigate(`/player-stats/${encodeURIComponent(p.riotId)}`)}>
                <td><RankBadge rank={i + 1} /></td>
                <td>
                  <div style={{ fontWeight: 700, fontSize: 13 }}>{p.riotId.split('#')[0]}</div>
                  <div style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>#{p.riotId.split('#')[1]}</div>
                </td>
                <td className="table-number">{p.games}</td>
                <td className="table-number">{p.surrenderCount}</td>
                <td className="table-number" style={{ color: p.surrenderRate > 0.5 ? '#f87171' : 'inherit', fontWeight: p.surrenderRate > 0.5 ? 700 : 400 }}>
                  {(p.surrenderRate * 100).toFixed(1)}%
                </td>
                <td className="table-number">{p.earlySurrenderCount}</td>
                <td className="table-number" style={{ color: p.earlySurrenderRate > 0.3 ? '#fb923c' : 'inherit' }}>
                  {(p.earlySurrenderRate * 100).toFixed(1)}%
                </td>
                <td className="table-number">{(p.surrenderWinRate * 100).toFixed(1)}%</td>
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
