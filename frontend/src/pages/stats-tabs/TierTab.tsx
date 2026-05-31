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
      <p style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)', marginBottom: 'var(--spacing-md)' }}>
        총 {data.totalMatches}경기 기준 (최소 3게임)
      </p>
      {tiers.map(tier => {
        const list: ChampionTierEntry[] = data.byTier[tier] ?? [];
        if (!list.length) return null;
        const color = TIER_COLORS[tier] ?? '#888';
        return (
          <div key={tier} style={{ marginBottom: 'var(--spacing-xl)' }}>
            <div className="section-head" style={{ paddingBottom: 'var(--spacing-sm)', borderBottom: `1px solid ${color}33` }}>
              <span className="icon-chip" style={{ background: color, border: `1px solid ${color}`, color: '#111', fontWeight: 800, fontSize: 15, boxShadow: `0 0 12px ${color}66` }}>{tier}</span>
              <span className="section-head-title" style={{ color }}>{tier} 티어</span>
              <span className="badge badge-normal badge-sm section-head-action">{list.length}</span>
            </div>
            <div className="grid-16">
              {list.map((c: ChampionTierEntry) => {
                const nameKo = champions.get(c.championId)?.nameKo ?? c.champion;
                return (
                  <ChampionLink key={c.champion} champion={c.champion} championId={c.championId} className="popup-trigger--card col-span-2">
                    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 'var(--spacing-xs)', padding: 'var(--spacing-sm)', borderRadius: 'var(--radius-md)', border: `1px solid ${color}44`, cursor: 'pointer', transition: 'border-color 0.15s, box-shadow 0.15s, transform 0.15s', boxShadow: `0 0 8px ${color}11` }}>
                      <ChampImg championId={c.championId} champion={c.champion} size={36} />
                      <div style={{ fontSize: 'var(--font-size-xs)', fontWeight: 600, textAlign: 'center', lineHeight: 1.2, color: 'var(--color-text-primary)' }}>{nameKo}</div>
                      <div style={{ fontSize: 'var(--font-size-xs)', fontWeight: 700, color: c.winRate >= 60 ? 'var(--color-win)' : c.winRate >= 50 ? 'var(--color-primary)' : 'var(--color-loss)', fontVariantNumeric: 'tabular-nums' }}>
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
