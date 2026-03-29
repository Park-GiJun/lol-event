import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../../lib/api/api';
import type { SupportImpactResult, SupportImpactEntry } from '../../lib/types/stats';
import { LoadingCenter } from '../../components/common/Spinner';
import { useDragon } from '../../context/DragonContext';
import { RankBadge, ChampImg } from './shared';

export default function SupportTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { champions } = useDragon();
  const [data, setData]       = useState<SupportImpactResult | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try { setData(await api.get<SupportImpactResult>(`/stats/support-impact?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  return (
    <div>
      <div className="table-wrapper">
        <table className="table member-stats-table">
          <thead>
            <tr>
              <th style={{ width: 36 }}>#</th>
              <th>플레이어</th>
              <th>역할</th>
              <th className="table-number">판수</th>
              <th className="table-number">힐 기여</th>
              <th className="table-number">CC 기여</th>
              <th className="table-number">시야 기여</th>
              <th className="table-number">서폿 지수</th>
            </tr>
          </thead>
          <tbody>
            {data.rankings.map((p: SupportImpactEntry, i) => (
              <tr key={p.riotId} className="member-stats-row"
                onClick={() => navigate(`/player-stats/${encodeURIComponent(p.riotId)}`)}>
                <td><RankBadge rank={i + 1} /></td>
                <td>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                    <span style={{ fontWeight: 700, fontSize: 13 }}>{p.riotId.split('#')[0]}</span>
                    {p.topChampion && p.topChampionId && (
                      <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                        <ChampImg championId={p.topChampionId} champion={p.topChampion} size={16} />
                        <span style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>
                          {champions.get(p.topChampionId)?.nameKo ?? p.topChampion}
                        </span>
                      </div>
                    )}
                  </div>
                </td>
                <td>
                  <span style={{ fontSize: 11, background: 'var(--color-bg-hover)', borderRadius: 4, padding: '2px 6px' }}>
                    {p.roleTag}
                  </span>
                </td>
                <td className="table-number">{p.games}</td>
                <td className="table-number">{(p.avgHealShare * 100).toFixed(1)}%</td>
                <td className="table-number">{(p.avgCcShare * 100).toFixed(1)}%</td>
                <td className="table-number">{(p.avgVisionShare * 100).toFixed(1)}%</td>
                <td className="table-number" style={{ fontWeight: 700, color: '#4CAF50' }}>
                  {p.supportImpact.toFixed(1)}
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
