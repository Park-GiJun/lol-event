import { CoinsIcon } from '@/components/icons/LolIcons';
import { useEffect, useState, useCallback } from 'react';
import { api } from '../../lib/api/api';
import type { GoldEfficiencyResult, GoldEfficiencyEntry } from '../../lib/types/stats';
import { LoadingCenter } from '../../components/common/Spinner';
import { PlayerLink } from '../../components/common/PlayerLink';
import { RankBadge } from './shared';

export default function GoldEffTab({ mode }: { mode: string }) {
  const [data, setData] = useState<GoldEfficiencyResult | null>(null);
  const [loading, setLoading] = useState(false);

  const load = useCallback(async () => {
    if (data) return;
    setLoading(true);
    try { setData(await api.get<GoldEfficiencyResult>(`/stats/gold-efficiency?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode, data]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  const kings = [
    { emoji: '', label: '딜 효율왕', name: data.dmgEfficiencyKing },
    { emoji: '', label: '시야 효율왕', name: data.visionEfficiencyKing },
    { emoji: '', label: 'CS 효율왕', name: data.csEfficiencyKing },
    { emoji: '', label: '오브젝트 효율왕', name: data.objEfficiencyKing },
  ];

  return (
    <div>
      <div className="grid-16" style={{ marginBottom: 'var(--spacing-lg)' }}>
        {kings.map(k => (
          <div key={k.label} className="stat-card col-span-8" style={{ textAlign: 'center' }}>
            <div style={{ fontSize: 24, marginBottom: 'var(--spacing-xs)', filter: 'drop-shadow(0 0 6px rgba(200, 170, 110, 0.35))' }}>{k.emoji}</div>
            <div className="stat-card-label" style={{ marginBottom: 5 }}>{k.label}</div>
            <div className="stat-card-value" style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-primary)', textShadow: '0 0 8px rgba(200, 170, 110, 0.3)' }}>{k.name?.split('#')[0] ?? '-'}</div>
          </div>
        ))}
      </div>

      <div className="section-head">
        <CoinsIcon size={16} />
        <span className="section-head-title">골드 효율 랭킹</span>
      </div>
      <div className="table-wrapper">
        <table className="table member-stats-table" style={{ fontSize: 12 }}>
          <thead>
            <tr>
              <th style={{ padding: '8px 12px', textAlign: 'left' }}>플레이어</th>
              <th style={{ padding: '8px 12px', textAlign: 'center' }}>효율점수</th>
              <th style={{ padding: '8px 12px', textAlign: 'center' }}>딜/골드</th>
              <th style={{ padding: '8px 12px', textAlign: 'center' }}>시야/골드</th>
              <th style={{ padding: '8px 12px', textAlign: 'center' }}>CS/골드</th>
              <th style={{ padding: '8px 12px', textAlign: 'left' }}>태그</th>
            </tr>
          </thead>
          <tbody>
            {data.rankings.map((e: GoldEfficiencyEntry, i: number) => (
              <tr key={e.riotId} className="member-stats-row">
                <td style={{ padding: '8px 12px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                    <RankBadge rank={i + 1} />
                    <PlayerLink riotId={e.riotId} mode={mode}>
                      <span style={{ fontWeight: 700 }}>{e.riotId.split('#')[0]}</span>
                    </PlayerLink>
                  </div>
                  <div style={{ fontSize: 10, color: 'var(--color-text-disabled)', paddingLeft: 32, marginTop: 1 }}>{e.games}게임</div>
                </td>
                <td style={{ padding: '8px 12px', textAlign: 'center', fontWeight: 800, color: 'var(--color-primary)', fontVariantNumeric: 'tabular-nums' }}>
                  {e.goldEfficiencyScore.toFixed(2)}
                </td>
                <td style={{ padding: '8px 12px', textAlign: 'center', color: 'var(--color-text-secondary)', fontVariantNumeric: 'tabular-nums' }}>{e.avgDmgPerGold.toFixed(3)}</td>
                <td style={{ padding: '8px 12px', textAlign: 'center', color: 'var(--color-text-secondary)', fontVariantNumeric: 'tabular-nums' }}>{e.avgVisionPerGold.toFixed(4)}</td>
                <td style={{ padding: '8px 12px', textAlign: 'center', color: 'var(--color-text-secondary)', fontVariantNumeric: 'tabular-nums' }}>{e.avgCsPerGold.toFixed(4)}</td>
                <td style={{ padding: '8px 12px' }}>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: 3 }}>
                    {e.tags.map((t: string) => (
                      <span key={t} className="badge badge-primary badge-sm">{t}</span>
                    ))}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
