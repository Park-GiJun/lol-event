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

  const players = data.players ?? [];
  const eligible = players.filter(p => p.games > 0);
  // 백엔드가 별도 펀카드 값을 주지 않으므로 players 에서 파생
  const surrenderKing = eligible.reduce<SurrenderPlayerEntry | null>(
    (best, p) => (!best || p.surrenderRate > best.surrenderRate ? p : best), null);
  const neverGiveUpKing = eligible.reduce<SurrenderPlayerEntry | null>(
    (best, p) => (!best || p.surrenderRate < best.surrenderRate ? p : best), null);

  const funCards = [
    { emoji: '', label: '서렌더왕', name: surrenderKing?.riotId, color: 'var(--color-loss)' },
    { emoji: '', label: '끝까지 안포기왕', name: neverGiveUpKing?.riotId, color: 'var(--color-win)' },
  ];

  return (
    <div>
      <div className="grid-16" style={{ marginBottom: 'var(--spacing-lg)' }}>
        <div className="stat-card col-span-8" style={{ textAlign: 'center' }}>
          <div className="stat-card-label">전체 서렌더율</div>
          <div className="stat-card-value" style={{ color: 'var(--color-loss)' }}>
            {(data.overallSurrenderRate * 100).toFixed(1)}%
          </div>
        </div>
        <div className="stat-card col-span-8" style={{ textAlign: 'center' }}>
          <div className="stat-card-label">조기 서렌더율</div>
          <div className="stat-card-value" style={{ color: 'var(--color-warning)' }}>
            {(data.overallEarlySurrenderRate * 100).toFixed(1)}%
          </div>
        </div>
        {funCards.map(c => (
          <div key={c.label} className="stat-card col-span-8" style={{ textAlign: 'center', alignItems: 'center' }}>
            <div className="icon-chip" style={{ marginBottom: 'var(--spacing-xs)' }}>{c.emoji}</div>
            <div className="stat-card-label">{c.label}</div>
            <div className="stat-card-value" style={{ fontSize: 'var(--font-size-md)', color: c.color }}>
              {c.name?.split('#')[0] ?? '-'}
            </div>
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
              <th className="table-number">조기 서렌더 유발</th>
            </tr>
          </thead>
          <tbody>
            {players.map((p: SurrenderPlayerEntry, i) => (
              <tr key={p.riotId} className="member-stats-row"
                onClick={() => navigate(`/player-stats/${encodeURIComponent(p.riotId)}`)}>
                <td><RankBadge rank={i + 1} /></td>
                <td>
                  <div style={{ fontWeight: 'var(--font-weight-bold)', fontSize: 'var(--font-size-sm)' }}>{p.riotId.split('#')[0]}</div>
                  <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-disabled)' }}>#{p.riotId.split('#')[1]}</div>
                </td>
                <td className="table-number">{p.games}</td>
                <td className="table-number">{p.surrenderGames}</td>
                <td className="table-number" style={{ color: p.surrenderRate > 0.5 ? 'var(--color-loss)' : 'inherit', fontWeight: p.surrenderRate > 0.5 ? 'var(--font-weight-bold)' : 'var(--font-weight-normal)' }}>
                  {(p.surrenderRate * 100).toFixed(1)}%
                </td>
                <td className="table-number">{p.earlySurrenderGames}</td>
                <td className="table-number" style={{ color: p.earlySurrenderRate > 0.3 ? 'var(--color-warning)' : 'inherit' }}>
                  {(p.earlySurrenderRate * 100).toFixed(1)}%
                </td>
                <td className="table-number">{p.causedEarlySurrenderGames}</td>
              </tr>
            ))}
            {!players.length && (
              <tr><td colSpan={8} style={{ textAlign: 'center', padding: 'var(--spacing-2xl) 0', color: 'var(--color-text-secondary)' }}>데이터 없음</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
