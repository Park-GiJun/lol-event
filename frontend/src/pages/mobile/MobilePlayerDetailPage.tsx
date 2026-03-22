import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { usePlayerDetail } from '@/hooks/usePlayerDetail';
import { usePlayerEloHistory } from '@/hooks/usePlayerEloHistory';
import { useDragon } from '@/context/DragonContext';
import { Skeleton } from '@/components/common/Skeleton';
import { InlineError } from '@/components/common/InlineError';

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
  const [champSort, setChampSort] = useState<ChampSort>('games');

  // P1: useParams already decodes — no additional decodeURIComponent
  const decoded = riotId ?? '';

  const { data, isLoading: statsLoading, error: statsError, refetch } = usePlayerDetail(decoded);
  const { data: eloHistory, isLoading: eloLoading, error: eloError } = usePlayerEloHistory(decoded);

  // P2: check error before isLoading so statsError is never hidden by eloLoading
  if (statsError || (!statsLoading && !data)) {
    return <InlineError message="플레이어 정보를 불러오지 못했습니다." onRetry={() => void refetch()} />;
  }

  if (statsLoading || eloLoading) {
    return (
      <div>
        {/* Header skeleton */}
        <div className="m-card" style={{ marginBottom: 12 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <Skeleton style={{ height: 24, width: 120, borderRadius: 4, marginBottom: 6 }} />
              <Skeleton style={{ height: 14, width: 60, borderRadius: 4 }} />
            </div>
            <div style={{ textAlign: 'right' }}>
              <Skeleton style={{ height: 16, width: 64, borderRadius: 4, marginBottom: 6 }} />
              <Skeleton style={{ height: 24, width: 48, borderRadius: 4 }} />
            </div>
          </div>
        </div>
        {/* Overview grid skeleton */}
        <div className="m-overview-grid" style={{ marginBottom: 12 }}>
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="m-overview-stat">
              <Skeleton style={{ height: 22, width: 48, borderRadius: 4, marginBottom: 4 }} />
              <Skeleton style={{ height: 12, width: 32, borderRadius: 4 }} />
            </div>
          ))}
        </div>
        {/* Elo card skeleton */}
        <div className="m-elo-card" style={{ marginBottom: 12 }}>
          <Skeleton style={{ height: 32, width: 80, borderRadius: 4 }} />
          <Skeleton style={{ height: 28, width: 120, borderRadius: 4 }} />
        </div>
      </div>
    );
  }

  // data is guaranteed non-null here
  const tier = eloTier(data!.elo);

  // P5: safe split for riotIds that may contain multiple '#'
  const hashIdx = decoded.indexOf('#');
  const name = hashIdx >= 0 ? decoded.slice(0, hashIdx) : decoded;
  const tag  = hashIdx >= 0 ? decoded.slice(hashIdx + 1) : '';

  // P3: NaN-safe sort
  const sortedChamps = data!.championStats.slice().sort((a, b) => {
    if (champSort === 'kda') {
      const av = a.kda ?? 0;
      const bv = b.kda ?? 0;
      return bv - av;
    }
    const av = (a[champSort] as number) ?? 0;
    const bv = (b[champSort] as number) ?? 0;
    return bv - av;
  });

  const recent5 = eloHistory?.history.slice(0, 5) ?? [];
  const eloSum5 = recent5.reduce((s, h) => s + h.delta, 0);

  // P4: use data.elo as single source of truth for tier display
  const displayElo = data!.elo;

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
            <div style={{ fontSize: 18, fontWeight: 800, color: 'var(--color-primary)' }}>{displayElo}</div>
            {data!.eloRank && <div style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>#{data!.eloRank}</div>}
          </div>
        </div>
      </div>

      {/* Summary grid */}
      <div className="m-overview-grid" style={{ marginBottom: 12 }}>
        {[
          { label: '판수', value: data!.games },
          { label: '승률', value: `${data!.winRate.toFixed(1)}%` },
          { label: 'KDA', value: data!.kda.toFixed(2) },
          { label: '평균딜', value: Math.round(data!.avgDamage).toLocaleString() },
          { label: 'CS', value: data!.avgCs.toFixed(1) },
          { label: '시야', value: data!.avgVisionScore.toFixed(1) },
        ].map(({ label, value }) => (
          <div key={label} className="m-overview-stat">
            <div className="m-overview-stat-value" style={{ fontSize: typeof value === 'string' && value.length > 5 ? 16 : 22 }}>{value}</div>
            <div className="m-overview-stat-label">{label}</div>
          </div>
        ))}
      </div>

      {/* Elo card — P7: show error fallback if elo history failed */}
      {eloError ? (
        <div className="m-card" style={{ marginBottom: 12, fontSize: 12, color: 'var(--color-text-secondary)' }}>
          Elo 이력을 불러오지 못했습니다.
        </div>
      ) : eloHistory && (
        <div className="m-elo-card" style={{ marginBottom: 12 }}>
          <div>
            <div style={{ fontSize: 11, color: 'var(--color-text-secondary)', marginBottom: 2 }}>현재 Elo</div>
            {/* P4: single source — use displayElo (data.elo) */}
            <div style={{ fontSize: 24, fontWeight: 800, color: tier.color }}>{displayElo}</div>
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
            {/* P6: 0 is neutral, not win */}
            <div style={{ fontSize: 11, marginTop: 4, color: eloSum5 > 0 ? 'var(--color-win)' : eloSum5 < 0 ? 'var(--color-loss)' : 'var(--color-text-secondary)' }}>
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
      {data!.recentMatches.slice(0, 15).map(rm => {
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
