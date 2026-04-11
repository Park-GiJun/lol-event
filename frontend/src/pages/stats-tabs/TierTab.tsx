import { useEffect, useState, useCallback } from 'react';
import { api } from '../../lib/api/api';
import type { ChampionTierResult, ChampionTierEntry } from '../../lib/types/stats';
import { LoadingCenter } from '../../components/common/Spinner';
import { useDragon } from '../../context/DragonContext';
import { ChampionLink } from '../../components/common/ChampionLink';
import { ChampImg } from './shared';

const TIER_COLORS: Record<string, string> = { S: '#FFD700', A: '#4ade80', B: '#60a5fa', C: '#9ca3af', D: '#f87171' };

export default function TierTab({ mode }: { mode: string }) {
  const [data, setData] = useState<ChampionTierResult | null>(null);
  const [loading, setLoading] = useState(false);
  const { champions } = useDragon();

  const load = useCallback(async () => {
    if (data) return;
    setLoading(true);
    try { setData(await api.get<ChampionTierResult>(`/stats/champion-tier?mode=${mode}&minGames=3`)); }
    finally { setLoading(false); }
  }, [mode, data]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  const tiers = ['S', 'A', 'B', 'C', 'D'];

  return (
    <div>
      <p style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginBottom: 12 }}>
        총 {data.totalMatches}경기 기준 (최소 3게임)
      </p>
      {tiers.map(tier => {
        const list: ChampionTierEntry[] = data.byTier[tier] ?? [];
        if (!list.length) return null;
        const color = TIER_COLORS[tier] ?? '#888';
        return (
          <div key={tier} style={{ marginBottom: 20 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
              <div style={{ width: 28, height: 28, borderRadius: 6, background: color, display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 800, fontSize: 14, color: '#111' }}>{tier}</div>
              <span style={{ fontWeight: 700, fontSize: 13, color }}>{tier} 티어</span>
              <span style={{ fontSize: 11, color: 'var(--color-text-disabled)' }}>({list.length})</span>
            </div>
            <div className="grid-16">
              {list.map((c: ChampionTierEntry) => {
                const nameKo = champions.get(c.championId)?.nameKo ?? c.champion;
                return (
                  <ChampionLink key={c.champion} champion={c.champion} championId={c.championId} className="popup-trigger--card col-span-2">
                    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4, padding: '8px 10px', background: 'var(--color-bg-secondary)', borderRadius: 8, border: `1px solid ${color}44`, cursor: 'pointer' }}>
                      <ChampImg championId={c.championId} champion={c.champion} size={36} />
                      <div style={{ fontSize: 11, fontWeight: 600, textAlign: 'center', lineHeight: 1.2 }}>{nameKo}</div>
                      <div style={{ fontSize: 10, color: c.winRate >= 60 ? 'var(--color-win)' : c.winRate >= 50 ? 'var(--color-primary)' : 'var(--color-loss)' }}>
                        {c.winRate.toFixed(1)}%
                      </div>
                      <div style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>{c.games}픽</div>
                    </div>
                  </ChampionLink>
                );
              })}
            </div>
          </div>
        );
      })}
    </div>
  );
}
