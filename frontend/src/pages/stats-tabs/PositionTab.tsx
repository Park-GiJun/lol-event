import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../../lib/api/api';
import type { PositionBadgeResult, PositionBadgeEntry } from '../../lib/types/stats';
import { LoadingCenter } from '../../components/common/Spinner';
import { useDragon } from '../../context/DragonContext';
import { ChampImg } from './shared';

const POSITION_META: Record<string, { label: string; emoji: string }> = {
  TOP:     { label: '탑',   emoji: '🛡️' },
  JUNGLE:  { label: '정글', emoji: '🌲' },
  MID:     { label: '미드', emoji: '⚡' },
  BOTTOM:  { label: '원딜', emoji: '🏹' },
  SUPPORT: { label: '서폿', emoji: '💫' },
};

export default function PositionTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { champions } = useDragon();
  const [data, setData]       = useState<PositionBadgeResult | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try { setData(await api.get<PositionBadgeResult>(`/stats/position-badge?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  return (
    <div>
      <div className="section-head">
        <span className="icon-chip">🥇</span>
        <span className="section-head-title">포지션별 1위</span>
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(160px, 1fr))', gap: 'var(--spacing-md)', marginBottom: 'var(--spacing-2xl)' }}>
        {data.topPositions.map((entry: PositionBadgeEntry) => {
          const meta = POSITION_META[entry.position] ?? { label: entry.position, emoji: '📍' };
          const nameKo = entry.topChampionId ? (champions.get(entry.topChampionId)?.nameKo ?? entry.topChampion) : entry.topChampion;
          return (
            <div key={entry.position} className="card" style={{ padding: 'var(--spacing-md)', textAlign: 'center', cursor: 'pointer', transition: 'border-color var(--transition-fast), box-shadow var(--transition-fast), transform var(--transition-fast)' }}
              onClick={() => navigate(`/player-stats/${encodeURIComponent(entry.riotId)}`)}>
              <div style={{ fontSize: 28, marginBottom: 6, filter: 'drop-shadow(0 0 6px rgba(200, 170, 110, 0.35))' }}>{meta.emoji}</div>
              <div style={{ fontSize: 'var(--font-size-xs)', fontWeight: 700, color: 'var(--color-text-secondary)', letterSpacing: 'var(--tracking-wider)', textTransform: 'uppercase', marginBottom: 6 }}>{meta.label}</div>
              <div style={{ fontWeight: 800, fontSize: 'var(--font-size-md)', color: 'var(--color-text-primary)' }}>{entry.riotId.split('#')[0]}</div>
              <div style={{ fontSize: 'var(--font-size-lg)', fontWeight: 800, color: 'var(--color-primary)', marginTop: 4, fontVariantNumeric: 'tabular-nums' }}>
                {entry.positionScore.toFixed(1)}<span style={{ fontSize: 'var(--font-size-xs)', fontWeight: 600, color: 'var(--color-text-secondary)', marginLeft: 2 }}>점</span>
              </div>
              {nameKo && (
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 4, fontSize: 'var(--font-size-xs)', color: 'var(--color-text-disabled)', marginTop: 6 }}>
                  {entry.topChampionId && <ChampImg championId={entry.topChampionId} champion={entry.topChampion ?? ''} size={16} />}
                  <span>{nameKo}</span>
                </div>
              )}
              <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-disabled)', marginTop: 4 }}>{entry.games}판</div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
