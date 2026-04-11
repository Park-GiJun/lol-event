import { useEffect, useState, useCallback } from 'react';
import { api } from '../../lib/api/api';
import type { ChaosMatchResult, ChaosMatchEntry } from '../../lib/types/stats';
import { LoadingCenter } from '../../components/common/Spinner';

const CHAOS_TAG_COLORS: Record<string, string> = {
  '혈전':   '#e74c3c',
  '학살':   '#e67e22',
  '운영 접전': '#4a9eff',
};

function chaosTagColor(tag: string): string {
  return CHAOS_TAG_COLORS[tag] ?? '#888';
}

function formatDuration(min: number): string {
  const m = Math.floor(min);
  const s = Math.round((min - m) * 60);
  return `${m}:${s.toString().padStart(2, '0')}`;
}

export default function ChaosTab({ mode }: { mode: string }) {
  const [data, setData]       = useState<ChaosMatchResult | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try { setData(await api.get<ChaosMatchResult>(`/stats/chaos-match?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  const renderCard = (entry: ChaosMatchEntry) => {
    const tagColor = chaosTagColor(entry.gameTypeTag);
    return (
      <div key={entry.matchId} className="card" style={{ padding: '14px 16px', borderLeft: `3px solid ${tagColor}88`, transition: 'border-color var(--transition-fast), box-shadow var(--transition-fast), transform var(--transition-fast)' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 8 }}>
          <span style={{ fontWeight: 800, fontSize: 22, color: tagColor, fontVariantNumeric: 'tabular-nums', textShadow: `0 0 12px ${tagColor}44` }}>
            {entry.chaosIndex.toFixed(1)}
          </span>
          <span style={{ fontSize: 11, fontWeight: 700, color: tagColor,
            background: tagColor + '22', border: `1px solid ${tagColor}44`,
            borderRadius: 4, padding: '2px 8px' }}>
            {entry.gameTypeTag}
          </span>
        </div>
        <div style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginBottom: 4 }}>
          킬 {entry.totalKills} · {formatDuration(entry.gameDurationMin)}
        </div>
        <div style={{ fontSize: 11, color: 'var(--color-text-disabled)' }}>
          {entry.participants.slice(0, 5).map(p => p.split('#')[0]).join(', ')}
        </div>
      </div>
    );
  };

  return (
    <div>
      <p style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginBottom: 16 }}>
        평균 혼돈 지수: <span style={{ fontWeight: 700, color: 'var(--color-primary)', fontVariantNumeric: 'tabular-nums' }}>{data.avgChaosIndex.toFixed(1)}</span>
      </p>
      <h3 style={{ fontSize: 13, fontWeight: 700, marginBottom: 8, color: 'var(--color-text-primary)' }}>🔥 최고 혼돈 경기 TOP 10</h3>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: 10, marginBottom: 20 }}>
        {data.topChaosMatches.map(renderCard)}
      </div>
    </div>
  );
}
