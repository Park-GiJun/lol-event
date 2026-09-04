import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../../lib/api/api';
import type { PositionBadgeResult, PositionBadgeEntry } from '../../lib/types/stats';
import { LoadingCenter } from '../../components/common/Spinner';
import { useDragon } from '../../context/DragonContext';
import { ChampImg } from './shared';
import { positionLabel, type Position } from '@/lib/position';
import { POSITION_ICON } from '@/components/icons/positionIcon';

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
        <span className="icon-chip"></span>
        <span className="section-head-title">포지션별 1위</span>
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(160px, 1fr))', gap: 'var(--spacing-md)', marginBottom: 'var(--spacing-2xl)' }}>
        {data.topPositions.map((entry: PositionBadgeEntry) => {
          const Icon = POSITION_ICON[entry.position as Position];
          const nameKo = entry.topChampionId ? (champions.get(entry.topChampionId)?.nameKo ?? entry.topChampion) : entry.topChampion;
          return (
            <div key={entry.position} className="card" style={{ padding: 'var(--spacing-md)', textAlign: 'center', cursor: 'pointer', transition: 'border-color var(--transition-fast), box-shadow var(--transition-fast), transform var(--transition-fast)' }}
              onClick={() => navigate(`/player-stats/${encodeURIComponent(entry.riotId)}`)}>
              <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 6, color: 'var(--color-primary)' }}>
                {Icon && <Icon size={26} />}
              </div>
              <div style={{ fontSize: 'var(--font-size-xs)', fontWeight: 700, color: 'var(--color-text-secondary)', letterSpacing: 'var(--tracking-wider)', textTransform: 'uppercase', marginBottom: 6 }}>{positionLabel(entry.position)}</div>
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
