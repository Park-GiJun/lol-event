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

  const MULTIKILL_COLORS: Record<string, string> = {
    PENTA: '#FFD700',
    QUADRA: '#AA47BC',
    TRIPLE: '#4a9eff',
    DOUBLE: '#888',
  };

  return (
    <div>
      {data.pentaKillEvents.length > 0 && (
        <section style={{ marginBottom: 24 }}>
          <h3 style={{ fontSize: 13, fontWeight: 700, marginBottom: 10 }}>⭐ 펜타킬 명예의 전당</h3>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: 10 }}>
            {data.pentaKillEvents.map((ev: MultiKillEvent) => {
              const nameKo = ev.championId ? (champions.get(ev.championId)?.nameKo ?? ev.champion) : ev.champion;
              return (
                <div key={`${ev.matchId}-${ev.riotId}`} className="card" style={{ padding: '12px 14px', borderLeft: '3px solid #FFD700' }}>
                  <div style={{ fontWeight: 700, fontSize: 14, color: '#FFD700', marginBottom: 4 }}>펜타킬!</div>
                  <div style={{ fontWeight: 600, fontSize: 13 }}>{ev.riotId.split('#')[0]}</div>
                  <div style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginTop: 2 }}>
                    <ChampImg championId={ev.championId} champion={ev.champion} size={18} />
                    <span style={{ marginLeft: 4 }}>{nameKo}</span>
                  </div>
                </div>
              );
            })}
          </div>
        </section>
      )}

      <h3 style={{ fontSize: 13, fontWeight: 700, marginBottom: 10 }}>📊 플레이어 멀티킬 랭킹</h3>
      <div className="table-wrapper">
        <table className="table member-stats-table">
          <thead>
            <tr>
              <th style={{ width: 36 }}>#</th>
              <th>플레이어</th>
              <th className="table-number" style={{ color: '#FFD700' }}>펜타</th>
              <th className="table-number" style={{ color: '#AA47BC' }}>쿼드라</th>
              <th className="table-number" style={{ color: '#4a9eff' }}>트리플</th>
              <th className="table-number">더블</th>
            </tr>
          </thead>
          <tbody>
            {data.playerRankings.map((p: PlayerMultiKillStat, i) => (
              <tr key={p.riotId} className="member-stats-row">
                <td><RankBadge rank={i + 1} /></td>
                <td>
                  <div style={{ fontWeight: 700, fontSize: 13 }}>{p.riotId.split('#')[0]}</div>
                </td>
                <td className="table-number" style={{ fontWeight: 700, color: MULTIKILL_COLORS.PENTA }}>{p.pentaKills}</td>
                <td className="table-number" style={{ fontWeight: 700, color: MULTIKILL_COLORS.QUADRA }}>{p.quadraKills}</td>
                <td className="table-number" style={{ fontWeight: 700, color: MULTIKILL_COLORS.TRIPLE }}>{p.tripleKills}</td>
                <td className="table-number" style={{ color: 'var(--color-text-secondary)' }}>{p.doubleKills}</td>
              </tr>
            ))}
            {!data.playerRankings.length && (
              <tr><td colSpan={6} style={{ textAlign: 'center', padding: '40px 0', color: 'var(--color-text-secondary)' }}>데이터 없음</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
