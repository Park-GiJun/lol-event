import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../../lib/api/api';
import type { VisionDominanceResult, VisionPlayerEntry } from '../../lib/types/stats';
import { LoadingCenter } from '../../components/common/Spinner';
import { RankBadge } from './shared';

export default function VisionDominanceTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const [data, setData]       = useState<VisionDominanceResult | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try { setData(await api.get<VisionDominanceResult>(`/stats/vision-dominance?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  return (
    <div>
      {data.visionKing && (
        <div className="card-glass" style={{ marginBottom: 'var(--spacing-md)', padding: '10px 14px', display: 'flex', alignItems: 'center', gap: 'var(--spacing-sm)' }}>
          <span className="icon-chip">👁️</span>
          <div>
            <div className="hero-eyebrow" style={{ marginBottom: 2 }}>시야 지배왕</div>
            <div style={{ fontWeight: 'var(--font-weight-extrabold)', fontSize: 'var(--font-size-md)', color: 'var(--color-primary)' }}>{data.visionKing.split('#')[0]}</div>
          </div>
        </div>
      )}
      <p style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)', marginBottom: 'var(--spacing-md)' }}>
        시야 지수 = 평균 시야 점수 / 게임 수 기준 정규화
      </p>
      <div className="table-wrapper">
        <table className="table member-stats-table">
          <thead>
            <tr>
              <th style={{ width: 36 }}>#</th>
              <th>플레이어</th>
              <th className="table-number">판수</th>
              <th className="table-number">시야 점수</th>
              <th className="table-number">와드 설치</th>
              <th className="table-number">와드 제거</th>
              <th className="table-number">제어 와드</th>
              <th className="table-number">시야 지수</th>
            </tr>
          </thead>
          <tbody>
            {data.rankings.map((p: VisionPlayerEntry, i) => (
              <tr key={p.riotId} className="member-stats-row"
                onClick={() => navigate(`/player-stats/${encodeURIComponent(p.riotId)}`)}>
                <td><RankBadge rank={i + 1} /></td>
                <td>
                  <div style={{ fontWeight: 'var(--font-weight-bold)', fontSize: 'var(--font-size-sm)', color: 'var(--color-text-primary)' }}>{p.riotId.split('#')[0]}</div>
                  <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-disabled)' }}>#{p.riotId.split('#')[1]}</div>
                </td>
                <td className="table-number">{p.games}</td>
                <td className="table-number">{p.avgVisionScore.toFixed(1)}</td>
                <td className="table-number">{p.avgWardsPlaced.toFixed(1)}</td>
                <td className="table-number">{p.avgWardsKilled.toFixed(1)}</td>
                <td className="table-number">{p.avgControlWardsBought.toFixed(1)}</td>
                <td className="table-number" style={{ fontWeight: 'var(--font-weight-bold)', color: 'var(--color-primary)' }}>
                  {p.visionIndex.toFixed(2)}
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
