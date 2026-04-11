import { useEffect, useState, useCallback } from 'react';
import { api } from '../../lib/api/api';
import type { MetaShiftResult, MetaShiftChampion } from '../../lib/types/stats';
import { LoadingCenter } from '../../components/common/Spinner';
import { useDragon } from '../../context/DragonContext';
import { ChampImg } from './shared';

export default function MetaTab({ mode }: { mode: string }) {
  const { champions } = useDragon();
  const [data, setData]       = useState<MetaShiftResult | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try { setData(await api.get<MetaShiftResult>(`/stats/meta-shift?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  const renderChampList = (title: string, list: MetaShiftChampion[], trendColor: string) => (
    <section style={{ marginBottom: 24 }}>
      <h3 style={{ fontSize: 13, fontWeight: 700, marginBottom: 10, paddingBottom: 8, borderBottom: '1px solid var(--glass-border)', color: 'var(--color-text-primary)' }}>{title}</h3>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))', gap: 10 }}>
        {list.map((c: MetaShiftChampion) => {
          const nameKo = champions.get(c.championId)?.nameKo ?? c.champion;
          const trendSign = c.trend >= 0 ? '+' : '';
          return (
            <div key={c.champion} className="card" style={{ padding: '12px 14px', transition: 'border-color var(--transition-fast), box-shadow var(--transition-fast), transform var(--transition-fast)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
                <ChampImg championId={c.championId} champion={c.champion} size={32} />
                <div>
                  <div style={{ fontWeight: 700, fontSize: 13 }}>{nameKo}</div>
                  <div style={{ fontSize: 10, color: 'var(--color-text-disabled)', marginTop: 1 }}>{c.metaTag}</div>
                </div>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: 12, fontVariantNumeric: 'tabular-nums' }}>
                <span style={{ color: 'var(--color-text-secondary)' }}>픽률 {(c.pickRate * 100).toFixed(1)}%</span>
                <span style={{ fontWeight: 800, color: trendColor, fontSize: 13 }}>
                  {trendSign}{(c.trend * 100).toFixed(1)}%
                </span>
              </div>
              <div style={{ fontSize: 11, color: 'var(--color-text-secondary)', marginTop: 4, fontVariantNumeric: 'tabular-nums' }}>
                승률 <span style={{ fontWeight: 700, color: c.winRate >= 55 ? 'var(--color-win)' : c.winRate >= 50 ? 'var(--color-primary)' : 'var(--color-loss)' }}>{c.winRate.toFixed(1)}%</span>
              </div>
            </div>
          );
        })}
        {!list.length && <p style={{ fontSize: 12, color: 'var(--color-text-disabled)' }}>데이터 없음</p>}
      </div>
    </section>
  );

  return (
    <div>
      <p style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginBottom: 16 }}>
        총 {data.totalMatchesAnalyzed}경기 분석 — 최근 vs 이전 기간 픽률 변화
      </p>
      {renderChampList('📈 급상승 챔피언', data.risingChampions, 'var(--color-win)')}
      {renderChampList('📉 급하락 챔피언', data.fallingChampions, 'var(--color-loss)')}
      {renderChampList('📊 안정 메타 챔피언', data.stableTopChampions, 'var(--color-text-secondary)')}
    </div>
  );
}
