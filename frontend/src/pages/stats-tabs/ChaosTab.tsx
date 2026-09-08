import { FlameIcon } from '@/components/icons/LolIcons';
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
      <div
        key={entry.matchId}
        className="card"
        style={{
          padding: 'var(--spacing-md)',
          borderLeft: `3px solid ${tagColor}88`,
        }}
      >
        <div style={{
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          marginBottom: 'var(--spacing-sm)',
        }}>
          <span style={{
            fontWeight: 'var(--font-weight-extrabold)', fontSize: 'var(--font-size-2xl)',
            color: tagColor, fontVariantNumeric: 'tabular-nums', textShadow: `0 0 12px ${tagColor}44`,
          }}>
            {entry.chaosIndex.toFixed(1)}
          </span>
          <span
            className="badge badge-sm"
            style={{ color: tagColor, background: tagColor + '22', borderColor: tagColor + '44' }}
          >
            {entry.gameTypeTag}
          </span>
        </div>
        <div style={{
          fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)',
          marginBottom: 4,
        }}>
          킬 {entry.totalKills} · {formatDuration(entry.gameDurationMin)}
        </div>
        <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-disabled)' }}>
          {entry.participants.slice(0, 5).map(p => p.split('#')[0]).join(', ')}
        </div>
      </div>
    );
  };

  return (
    <div>
      <div className="stat-card" style={{ marginBottom: 'var(--spacing-lg)', maxWidth: 220 }}>
        <span className="stat-card-label">평균 난장판 점수</span>
        <span className="stat-card-value">{data.avgChaosIndex.toFixed(1)}</span>
      </div>

      <div className="section-head">
        <FlameIcon size={16} />
        <span className="section-head-title">가장 난장판이었던 경기 10</span>
      </div>

      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))',
        gap: 'var(--spacing-sm)',
        marginBottom: 'var(--spacing-lg)',
      }}>
        {data.topChaosMatches.map(renderCard)}
      </div>
    </div>
  );
}
