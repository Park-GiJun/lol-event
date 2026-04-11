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
        <div style={{ marginBottom: 16, padding: '10px 14px', borderRadius: 8, background: 'rgba(96,165,250,0.1)', border: '1px solid rgba(96,165,250,0.25)', display: 'flex', alignItems: 'center', gap: 10 }}>
          <span style={{ fontSize: 22 }}>👁️</span>
          <div>
            <div style={{ fontSize: 10, color: 'var(--color-text-secondary)', fontWeight: 700, letterSpacing: '0.05em', textTransform: 'uppercase' }}>시야 지배왕</div>
            <div style={{ fontWeight: 800, fontSize: 14, color: '#60a5fa' }}>{data.visionKing.split('#')[0]}</div>
          </div>
        </div>
      )}
      <p style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginBottom: 16 }}>
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
                  <div style={{ fontWeight: 700, fontSize: 13 }}>{p.riotId.split('#')[0]}</div>
                  <div style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>#{p.riotId.split('#')[1]}</div>
                </td>
                <td className="table-number">{p.games}</td>
                <td className="table-number">{p.avgVisionScore.toFixed(1)}</td>
                <td className="table-number">{p.avgWardsPlaced.toFixed(1)}</td>
                <td className="table-number">{p.avgWardsKilled.toFixed(1)}</td>
                <td className="table-number">{p.avgControlWardsBought.toFixed(1)}</td>
                <td className="table-number" style={{ fontWeight: 700, color: '#60a5fa' }}>
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
