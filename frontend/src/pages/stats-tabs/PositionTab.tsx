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
      <h3 style={{ fontSize: 13, fontWeight: 700, marginBottom: 12 }}>🥇 포지션별 1위</h3>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(160px, 1fr))', gap: 12, marginBottom: 28 }}>
        {data.topPositions.map((entry: PositionBadgeEntry) => {
          const meta = POSITION_META[entry.position] ?? { label: entry.position, emoji: '📍' };
          const nameKo = entry.topChampionId ? (champions.get(entry.topChampionId)?.nameKo ?? entry.topChampion) : entry.topChampion;
          return (
            <div key={entry.position} className="card" style={{ padding: '14px', textAlign: 'center', cursor: 'pointer' }}
              onClick={() => navigate(`/player-stats/${encodeURIComponent(entry.riotId)}`)}>
              <div style={{ fontSize: 28, marginBottom: 4 }}>{meta.emoji}</div>
              <div style={{ fontSize: 11, color: 'var(--color-text-secondary)', marginBottom: 4 }}>{meta.label}</div>
              <div style={{ fontWeight: 700, fontSize: 14 }}>{entry.riotId.split('#')[0]}</div>
              <div style={{ fontSize: 12, color: 'var(--color-primary)', marginTop: 2 }}>
                {entry.positionScore.toFixed(1)}점
              </div>
              {nameKo && (
                <div style={{ fontSize: 11, color: 'var(--color-text-disabled)', marginTop: 4 }}>
                  {entry.topChampionId && <ChampImg championId={entry.topChampionId} champion={entry.topChampion ?? ''} size={16} />}
                  <span style={{ marginLeft: 4 }}>{nameKo}</span>
                </div>
              )}
              <div style={{ fontSize: 11, color: 'var(--color-text-disabled)', marginTop: 2 }}>{entry.games}판</div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
