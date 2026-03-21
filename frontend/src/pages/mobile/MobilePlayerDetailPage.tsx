import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { api } from '../../lib/api/api';
import type { PlayerDetailStats, EloHistoryEntry, PlayerEloHistoryResult } from '../../lib/types/stats';
import { useDragon } from '../../context/DragonContext';
import { LoadingCenter } from '../../components/common/Spinner';

function eloTier(elo: number) {
  if (elo >= 1300) return { label: 'Challenger', color: '#FFD700' };
  if (elo >= 1200) return { label: 'Master',     color: '#AA47BC' };
  if (elo >= 1100) return { label: 'Diamond',    color: '#0BC4B4' };
  if (elo >= 1000) return { label: 'Platinum',   color: '#4A9EFF' };
  if (elo >= 900)  return { label: 'Gold',       color: '#C89B3C' };
  if (elo >= 800)  return { label: 'Silver',     color: '#A8A8A8' };
  return              { label: 'Bronze',     color: '#CD7F32' };
}

type ChampSort = 'games' | 'winRate' | 'kda';
const CHAMP_SORTS: { key: ChampSort; label: string }[] = [
  { key: 'games', label: '판수' },
  { key: 'winRate', label: '승률' },
  { key: 'kda', label: 'KDA' },
];

export function MobilePlayerDetailPage() {
  const { riotId } = useParams<{ riotId: string }>();
  const { champions } = useDragon();
  const [data, setData] = useState<PlayerDetailStats | null>(null);
  const [eloHistory, setEloHistory] = useState<PlayerEloHistoryResult | null>(null);
  const [loading, setLoading] = useState(true);
  const [champSort, setChampSort] = useState<ChampSort>('games');

  const decoded = riotId ? decodeURIComponent(riotId) : '';

  useEffect(() => {
    if (!decoded) return;
    setLoading(true);
    Promise.all([
      api.get<PlayerDetailStats>(`/stats/player/${encodeURIComponent(decoded)}?mode=all`),
      api.get<PlayerEloHistoryResult>(`/stats/player/${encodeURIComponent(decoded)}/elo-history?limit=20`),
    ])
      .then(([d, e]) => { setData(d); setEloHistory(e); })
      .finally(() => setLoading(false));
  }, [decoded]);

  if (loading) return <LoadingCenter />;
  if (!data) return <div className="m-empty">플레이어 데이터를 찾을 수 없습니다</div>;

  const tier = eloTier(data.elo);
  const [name, tag] = decoded.split('#');

  const sortedChamps = data.championStats.slice().sort((a, b) => {
    if (champSort === 'kda') return b.kda - a.kda;
    return (b[champSort] as number) - (a[champSort] as number);
  });

  const recent5 = eloHistory?.history.slice(0, 5) ?? [];
  const eloSum5 = recent5.reduce((s, h) => s + h.delta, 0);

  return (
    <div>
      {/* Player header */}
      <div className="m-card" style={{ marginBottom: 12 }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div>
            <div style={{ fontWeight: 800, fontSize: 20 }}>{name}</div>
            {tag && <div style={{ fontSize: 11, color: 'var(--color-text-disabled)' }}>#{tag}</div>}
          </div>
          <div style={{ textAlign: 'right' }}>
            <div style={{ fontSize: 14, fontWeight: 700, color: tier.color }}>{tier.label}</div>
            <div style={{ fontSize: 18, fontWeight: 800, color: 'var(--color-primary)' }}>{data.elo}</div>
            {data.eloRank && <div style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>#{data.eloRank}</div>}
          </div>
        </div>
      </div>

      {/* Summary grid */}
      <div className="m-overview-grid" style={{ marginBottom: 12 }}>
        {[
          { label: '판수', value: data.games },
          { label: '승률', value: `${data.winRate.toFixed(1)}%` },
          { label: 'KDA', value: data.kda.toFixed(2) },
          { label: '평균딜', value: Math.round(data.avgDamage).toLocaleString() },
          { label: 'CS', value: data.avgCs.toFixed(1) },
          { label: '시야', value: data.avgVisionScore.toFixed(1) },
        ].map(({ label, value }) => (
          <div key={label} className="m-overview-stat">
            <div className="m-overview-stat-value" style={{ fontSize: typeof value === 'string' && value.length > 5 ? 16 : 22 }}>{value}</div>
            <div className="m-overview-stat-label">{label}</div>
          </div>
        ))}
      </div>

      {/* Elo card */}
      {eloHistory && (
        <div className="m-elo-card" style={{ marginBottom: 12 }}>
          <div>
            <div style={{ fontSize: 11, color: 'var(--color-text-secondary)', marginBottom: 2 }}>현재 Elo</div>
            <div style={{ fontSize: 24, fontWeight: 800, color: tier.color }}>{eloHistory.currentElo}</div>
            <div style={{ fontSize: 12, color: tier.color }}>{tier.label}</div>
          </div>
          <div style={{ textAlign: 'right' }}>
            <div style={{ fontSize: 11, color: 'var(--color-text-secondary)', marginBottom: 4 }}>최근 5경기</div>
            <div style={{ display: 'flex', gap: 4 }}>
              {recent5.map((h, i) => (
                <div key={i} style={{
                  width: 28, height: 28, borderRadius: 6, display: 'flex', alignItems: 'center', justifyContent: 'center',
                  fontSize: 10, fontWeight: 700,
                  background: h.win ? 'rgba(11,196,180,0.15)' : 'rgba(232,64,64,0.15)',
                  color: h.win ? 'var(--color-win)' : 'var(--color-loss)',
                }}>
                  {h.delta > 0 ? `+${h.delta}` : h.delta}
                </div>
              ))}
            </div>
            <div style={{ fontSize: 11, marginTop: 4, color: eloSum5 >= 0 ? 'var(--color-win)' : 'var(--color-loss)' }}>
              합계 {eloSum5 > 0 ? `+${eloSum5}` : eloSum5}
            </div>
          </div>
        </div>
      )}

      {/* Champion stats */}
      <p className="m-section-title">챔피언별 통계</p>
      <div className="m-sort-chips">
        {CHAMP_SORTS.map(o => (
          <button key={o.key} className={`m-sort-chip${champSort === o.key ? ' active' : ''}`} onClick={() => setChampSort(o.key)}>
            {o.label}
          </button>
        ))}
      </div>

      {sortedChamps.slice(0, 10).map(cs => {
        const champInfo = Array.from(champions.values()).find(c => c.championKey === cs.champion || c.nameKo === cs.champion);
        const kdaVal = cs.avgDeaths === 0 ? 'Perfect' : ((cs.avgKills + cs.avgAssists) / cs.avgDeaths).toFixed(2);
        return (
          <div key={cs.champion} className="m-player-card" style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            {champInfo?.imageUrl ? (
              <img src={champInfo.imageUrl} alt={champInfo.nameKo} width={44} height={44} style={{ borderRadius: 8, objectFit: 'cover', flexShrink: 0 }} />
            ) : (
              <div style={{ width: 44, height: 44, borderRadius: 8, background: 'var(--color-bg-hover)', flexShrink: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 12 }}>
                {cs.champion.slice(0, 2)}
              </div>
            )}
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontWeight: 700, fontSize: 13 }}>{champInfo?.nameKo ?? cs.champion}</div>
              <div className="m-stat-chips" style={{ marginTop: 4 }}>
                <span className="m-stat-chip">{cs.games}게임</span>
                <span className="m-stat-chip">{cs.winRate.toFixed(1)}%</span>
                <span className="m-stat-chip">KDA {kdaVal}</span>
              </div>
            </div>
          </div>
        );
      })}

      {/* Recent matches */}
      <p className="m-section-title" style={{ marginTop: 8 }}>최근 경기</p>
      {data.recentMatches.slice(0, 15).map(rm => {
        const champInfo = Array.from(champions.values()).find(c => c.championKey === rm.champion || c.nameKo === rm.champion);
        return (
          <div key={rm.matchId} className="m-player-card" style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            {champInfo?.imageUrl ? (
              <img src={champInfo.imageUrl} alt={rm.champion} width={40} height={40} style={{ borderRadius: 8, objectFit: 'cover', flexShrink: 0 }} />
            ) : (
              <div style={{ width: 40, height: 40, borderRadius: 8, background: 'var(--color-bg-hover)', flexShrink: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 11 }}>
                {rm.champion.slice(0, 2)}
              </div>
            )}
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                <span style={{ fontWeight: 700, fontSize: 13 }}>{champInfo?.nameKo ?? rm.champion}</span>
                <span className={`m-win-badge ${rm.win ? 'win' : 'loss'}`} style={{ fontSize: 10, padding: '2px 6px' }}>{rm.win ? '승' : '패'}</span>
              </div>
              <div style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginTop: 2 }}>
                {rm.kills}/{rm.deaths}/{rm.assists} · 딜 {rm.damage.toLocaleString()}
              </div>
            </div>
            <div style={{ fontSize: 11, color: 'var(--color-text-secondary)', textAlign: 'right', flexShrink: 0 }}>
              {new Date(rm.gameCreation).toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' })}
            </div>
          </div>
        );
      })}
    </div>
  );
}
