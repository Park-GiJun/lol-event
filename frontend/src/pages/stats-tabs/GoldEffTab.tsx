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
    { emoji: '💥', label: '딜 효율왕', name: data.dmgEfficiencyKing },
    { emoji: '👁️', label: '시야 효율왕', name: data.visionEfficiencyKing },
    { emoji: '🌾', label: 'CS 효율왕', name: data.csEfficiencyKing },
    { emoji: '🏰', label: '오브젝트 효율왕', name: data.objEfficiencyKing },
  ];

  return (
    <div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 8, marginBottom: 20 }}>
        {kings.map(k => (
          <div key={k.label} className="card" style={{ padding: 12, textAlign: 'center' }}>
            <div style={{ fontSize: 22, marginBottom: 4 }}>{k.emoji}</div>
            <div style={{ fontSize: 10, color: 'var(--color-text-disabled)', marginBottom: 3 }}>{k.label}</div>
            <div style={{ fontWeight: 700, fontSize: 14, color: '#FFD700' }}>{k.name?.split('#')[0] ?? '-'}</div>
          </div>
        ))}
      </div>

      <h3 style={{ fontSize: 13, fontWeight: 700, marginBottom: 10 }}>골드 효율 랭킹</h3>
      <div style={{ overflowX: 'auto' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
          <thead>
            <tr style={{ background: 'var(--color-bg-hover)' }}>
              <th style={{ padding: '7px 10px', textAlign: 'left' }}>플레이어</th>
              <th style={{ padding: '7px 10px', textAlign: 'center' }}>효율점수</th>
              <th style={{ padding: '7px 10px', textAlign: 'center' }}>딜/골드</th>
              <th style={{ padding: '7px 10px', textAlign: 'center' }}>시야/골드</th>
              <th style={{ padding: '7px 10px', textAlign: 'center' }}>CS/골드</th>
              <th style={{ padding: '7px 10px', textAlign: 'left' }}>태그</th>
            </tr>
          </thead>
          <tbody>
            {data.rankings.map((e: GoldEfficiencyEntry, i: number) => (
              <tr key={e.riotId} style={{ borderTop: '1px solid var(--color-border)' }}>
                <td style={{ padding: '7px 10px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                    <RankBadge rank={i + 1} />
                    <PlayerLink riotId={e.riotId} mode={mode}>
                      <span style={{ fontWeight: 600 }}>{e.riotId.split('#')[0]}</span>
                    </PlayerLink>
                  </div>
                  <div style={{ fontSize: 10, color: 'var(--color-text-disabled)', paddingLeft: 32 }}>{e.games}게임</div>
                </td>
                <td style={{ padding: '7px 10px', textAlign: 'center', fontWeight: 700, color: 'var(--color-primary)' }}>
                  {e.goldEfficiencyScore.toFixed(2)}
                </td>
                <td style={{ padding: '7px 10px', textAlign: 'center' }}>{e.avgDmgPerGold.toFixed(3)}</td>
                <td style={{ padding: '7px 10px', textAlign: 'center' }}>{e.avgVisionPerGold.toFixed(4)}</td>
                <td style={{ padding: '7px 10px', textAlign: 'center' }}>{e.avgCsPerGold.toFixed(4)}</td>
                <td style={{ padding: '7px 10px' }}>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: 3 }}>
                    {e.tags.map((t: string) => (
                      <span key={t} style={{ fontSize: 10, padding: '1px 5px', borderRadius: 8, background: 'var(--color-primary)22', color: 'var(--color-primary)' }}>{t}</span>
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
