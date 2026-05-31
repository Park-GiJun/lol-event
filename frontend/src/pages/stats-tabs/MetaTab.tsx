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

  const renderChampList = (icon: string, title: string, list: MetaShiftChampion[], trendColor: string) => (
    <section style={{ marginBottom: 'var(--spacing-xl)' }}>
      <div className="section-head">
        <span className="icon-chip">{icon}</span>
        <span className="section-head-title">{title}</span>
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))', gap: 'var(--spacing-sm)' }}>
        {list.map((c: MetaShiftChampion) => {
          const nameKo = champions.get(c.championId)?.nameKo ?? c.champion;
          const trendSign = c.trend >= 0 ? '+' : '';
          return (
            <div key={c.champion} className="card" style={{ padding: 'var(--spacing-md)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--spacing-sm)', marginBottom: 'var(--spacing-sm)' }}>
                <ChampImg championId={c.championId} champion={c.champion} size={32} />
                <div>
                  <div style={{ fontWeight: 'var(--font-weight-bold)', fontSize: 'var(--font-size-sm)', color: 'var(--color-text-primary)' }}>{nameKo}</div>
                  <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-disabled)', marginTop: 1 }}>{c.metaTag}</div>
                </div>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: 'var(--font-size-sm)', fontVariantNumeric: 'tabular-nums' }}>
                <span style={{ color: 'var(--color-text-secondary)' }}>픽률 {(c.pickRate * 100).toFixed(1)}%</span>
                <span style={{ fontWeight: 'var(--font-weight-extrabold)', color: trendColor, fontSize: 'var(--font-size-sm)' }}>
                  {trendSign}{(c.trend * 100).toFixed(1)}%
                </span>
              </div>
              <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)', marginTop: 'var(--spacing-xs)', fontVariantNumeric: 'tabular-nums' }}>
                승률 <span style={{ fontWeight: 'var(--font-weight-bold)', color: c.winRate >= 55 ? 'var(--color-win)' : c.winRate >= 50 ? 'var(--color-primary)' : 'var(--color-loss)' }}>{c.winRate.toFixed(1)}%</span>
              </div>
            </div>
          );
        })}
        {!list.length && <p style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-text-disabled)' }}>데이터 없음</p>}
      </div>
    </section>
  );

  return (
    <div>
      <div className="hero-banner" style={{ marginBottom: 'var(--spacing-lg)' }}>
        <div className="hero-eyebrow">META SHIFT</div>
        <h2 className="hero-title">메타 변화</h2>
        <p className="hero-subtitle">
          총 {data.totalMatchesAnalyzed}경기 분석 — 최근 vs 이전 기간 픽률 변화
        </p>
      </div>
      {renderChampList('📈', '급상승 챔피언', data.risingChampions, 'var(--color-win)')}
      {renderChampList('📉', '급하락 챔피언', data.fallingChampions, 'var(--color-loss)')}
      {renderChampList('📊', '안정 메타 챔피언', data.stableTopChampions, 'var(--color-text-secondary)')}
    </div>
  );
}
