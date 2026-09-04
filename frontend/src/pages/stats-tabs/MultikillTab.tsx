import { BarChartIcon, StarIcon } from '@/components/icons/LolIcons';
import { useEffect, useState, useCallback } from 'react';
import { api } from '../../lib/api/api';
import type { MultiKillHighlightsResult, MultiKillEvent, PlayerMultiKillStat } from '../../lib/types/stats';
import { LoadingCenter } from '../../components/common/Spinner';
import { useDragon } from '../../context/DragonContext';
import { RankBadge, ChampImg } from './shared';

export default function MultikillTab({ mode }: { mode: string }) {
  const { champions } = useDragon();
  const [data, setData]       = useState<MultiKillHighlightsResult | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try { setData(await api.get<MultiKillHighlightsResult>(`/stats/multikill-highlights?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  // 멀티킬 등급 색상 — 희귀도 스케일(의미있는 등급 색이라 유지). 펜타는 골드 액센트.
  const MULTIKILL_COLORS: Record<string, string> = {
    PENTA: 'var(--color-primary)',
    QUADRA: '#AA47BC',
    TRIPLE: '#4a9eff',
    DOUBLE: 'var(--color-text-disabled)',
  };

  return (
    <div>
      {data.pentaKillEvents.length > 0 && (
        <section style={{ marginBottom: 'var(--spacing-lg)' }}>
          <div className="section-head">
            <StarIcon size={16} />
            <span className="section-head-title">펜타킬 명예의 전당</span>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: 'var(--spacing-sm)' }}>
            {data.pentaKillEvents.map((ev: MultiKillEvent) => {
              const nameKo = ev.championId ? (champions.get(ev.championId)?.nameKo ?? ev.champion) : ev.champion;
              return (
                <div
                  key={`${ev.matchId}-${ev.riotId}`}
                  className="card"
                  style={{ padding: '12px 14px', borderLeft: '3px solid var(--color-primary)' }}
                >
                  <div className="badge badge-gold badge-sm" style={{ marginBottom: 'var(--spacing-xs)', letterSpacing: 'var(--tracking-wide)' }}>PENTA KILL</div>
                  <div style={{ fontWeight: 'var(--font-weight-bold)', fontSize: 'var(--font-size-md)', color: 'var(--color-text-primary)' }}>{ev.riotId.split('#')[0]}</div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)', marginTop: 4 }}>
                    <ChampImg championId={ev.championId} champion={ev.champion} size={18} />
                    <span>{nameKo}</span>
                  </div>
                </div>
              );
            })}
          </div>
        </section>
      )}

      <div className="section-head">
        <BarChartIcon size={16} />
        <span className="section-head-title">플레이어 멀티킬 랭킹</span>
      </div>
      <div className="table-wrapper">
        <table className="table member-stats-table">
          <thead>
            <tr>
              <th style={{ width: 36 }}>#</th>
              <th>플레이어</th>
              <th className="table-number" style={{ color: MULTIKILL_COLORS.PENTA }}>펜타</th>
              <th className="table-number" style={{ color: MULTIKILL_COLORS.QUADRA }}>쿼드라</th>
              <th className="table-number" style={{ color: MULTIKILL_COLORS.TRIPLE }}>트리플</th>
              <th className="table-number">더블</th>
            </tr>
          </thead>
          <tbody>
            {data.playerRankings.map((p: PlayerMultiKillStat, i) => (
              <tr key={p.riotId} className="member-stats-row">
                <td><RankBadge rank={i + 1} /></td>
                <td>
                  <div style={{ fontWeight: 'var(--font-weight-bold)', fontSize: 'var(--font-size-sm)', color: 'var(--color-text-primary)' }}>{p.riotId.split('#')[0]}</div>
                </td>
                <td className="table-number" style={{ fontWeight: 'var(--font-weight-bold)', color: MULTIKILL_COLORS.PENTA }}>{p.pentaKills}</td>
                <td className="table-number" style={{ fontWeight: 'var(--font-weight-bold)', color: MULTIKILL_COLORS.QUADRA }}>{p.quadraKills}</td>
                <td className="table-number" style={{ fontWeight: 'var(--font-weight-bold)', color: MULTIKILL_COLORS.TRIPLE }}>{p.tripleKills}</td>
                <td className="table-number" style={{ color: 'var(--color-text-secondary)' }}>{p.doubleKills}</td>
              </tr>
            ))}
            {!data.playerRankings.length && (
              <tr><td colSpan={6} style={{ textAlign: 'center', padding: 'var(--spacing-2xl) 0', color: 'var(--color-text-secondary)' }}>데이터 없음</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
