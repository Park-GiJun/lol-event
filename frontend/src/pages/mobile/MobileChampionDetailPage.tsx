import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { api } from '../../lib/api/api';
import type { ChampionDetailStats, ChampionPlayerStat } from '../../lib/types/stats';
import { useDragon } from '../../context/DragonContext';
import { LoadingCenter } from '../../components/common/Spinner';
import { MODES_WITH_ALL } from '../../lib/lol';
import { MobilePlayerCard } from '../../components/mobile/MobilePlayerCard';

const MODES = MODES_WITH_ALL;

const POS_LABELS: Record<string, string> = {
  ALL: '전체', TOP: 'TOP', JUNGLE: 'JGL', MID: 'MID', BOTTOM: 'BOT', SUPPORT: 'SUP',
};

type SortKey = 'games' | 'winRate' | 'kda' | 'avgDamage';
const SORT_OPTS: { key: SortKey; label: string }[] = [
  { key: 'games', label: '판수' },
  { key: 'winRate', label: '승률' },
  { key: 'kda', label: 'KDA' },
  { key: 'avgDamage', label: '딜량' },
];

export function MobileChampionDetailPage() {
  const { champion } = useParams<{ champion: string }>();
  const navigate = useNavigate();
  const { champions, items } = useDragon();
  const [mode, setMode] = useState('all');
  const [pos, setPos] = useState('ALL');
  const [sort, setSort] = useState<SortKey>('games');
  const [data, setData] = useState<ChampionDetailStats | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!champion) return;
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setLoading(true);
    api.get<ChampionDetailStats>(`/stats/champion/${champion}?mode=${mode}`)
      .then(setData)
      .finally(() => setLoading(false));
  }, [champion, mode]);

  if (loading) return <LoadingCenter />;
  if (!data) return <div className="m-empty">데이터를 불러올 수 없습니다</div>;

  const champInfo = Array.from(champions.values()).find(c => c.championKey === champion);

  const players = (pos === 'ALL'
    ? data.players
    : data.players.filter(() => true) // position filtering not available at this level
  )
    .slice()
    .sort((a, b) => (b[sort] ?? 0) - (a[sort] ?? 0));

  const kda = (p: ChampionPlayerStat) =>
    p.avgDeaths === 0 ? 'Perfect' : ((p.avgKills + p.avgAssists) / p.avgDeaths).toFixed(2);

  return (
    <div>
      {/* Champion header */}
      <div className="m-card" style={{ display: 'flex', alignItems: 'center', gap: 14, marginBottom: 12 }}>
        {champInfo?.imageUrl ? (
          <img src={champInfo.imageUrl} alt={champInfo.nameKo} width={64} height={64} style={{ borderRadius: 12, objectFit: 'cover', flexShrink: 0 }} />
        ) : (
          <div style={{ width: 64, height: 64, borderRadius: 12, background: 'rgba(255,255,255,0.05)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 18, flexShrink: 0 }}>
            {data.champion.slice(0, 2)}
          </div>
        )}
        <div>
          <div style={{ fontWeight: 800, fontSize: 20 }}>{champInfo?.nameKo ?? data.champion}</div>
          <div style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginTop: 2 }}>
            {data.totalGames}게임 · 승률 {data.winRate.toFixed(1)}%
          </div>
        </div>
      </div>

      {/* Mode chips */}
      <div className="m-sort-chips">
        {MODES.map(m => (
          <button key={m.value} className={`m-sort-chip${mode === m.value ? ' active' : ''}`} onClick={() => setMode(m.value)}>
            {m.label}
          </button>
        ))}
      </div>

      {/* Summary */}
      <div className="m-overview-grid" style={{ marginBottom: 12 }}>
        <div className="m-overview-stat">
          <div className="m-overview-stat-value">{data.totalGames}</div>
          <div className="m-overview-stat-label">총 게임</div>
        </div>
        <div className="m-overview-stat">
          <div className="m-overview-stat-value">{data.winRate.toFixed(1)}%</div>
          <div className="m-overview-stat-label">승률</div>
        </div>
      </div>

      {/* Position tabs (from laneStats) */}
      {data.laneStats.length > 0 && (
        <>
          <div className="m-tab-bar">
            <button className={`m-lane-tab${pos === 'ALL' ? ' active' : ''}`} onClick={() => setPos('ALL')}>전체</button>
            {data.laneStats.map(ls => (
              <button key={ls.position} className={`m-lane-tab${pos === ls.position ? ' active' : ''}`} onClick={() => setPos(ls.position)}>
                {POS_LABELS[ls.position] ?? ls.position}
              </button>
            ))}
          </div>

          {pos !== 'ALL' && (() => {
            const ls = data.laneStats.find(l => l.position === pos);
            if (!ls) return null;
            return (
              <div className="m-card" style={{ marginBottom: 12 }}>
                <div className="m-stat-chips">
                  <span className="m-stat-chip">{ls.games}게임</span>
                  <span className="m-stat-chip">승률 {ls.winRate.toFixed(1)}%</span>
                  <span className="m-stat-chip">KDA {ls.kda.toFixed(2)}</span>
                  <span className="m-stat-chip">딜 {Math.round(ls.avgDamage).toLocaleString()}</span>
                  <span className="m-stat-chip">CS {ls.avgCs.toFixed(1)}</span>
                </div>
              </div>
            );
          })()}
        </>
      )}

      {/* Player ranking */}
      <p className="m-section-title">플레이어 순위</p>
      <div className="m-sort-chips">
        {SORT_OPTS.map(o => (
          <button key={o.key} className={`m-sort-chip${sort === o.key ? ' active' : ''}`} onClick={() => setSort(o.key)}>
            {o.label}
          </button>
        ))}
      </div>

      {players.map((p, i) => (
        <MobilePlayerCard
          key={p.riotId}
          riotId={p.riotId}
          rank={i + 1}
          rightSlot={<span className="m-player-games">{p.games}게임</span>}
          winBar={{ winRate: p.winRate, wins: p.wins, losses: p.games - p.wins }}
          onClick={() => navigate(`/m/player/${encodeURIComponent(p.riotId)}`)}
        >
          <span className="m-stat-chip">KDA {kda(p)}</span>
          <span className="m-stat-chip">딜 {Math.round(p.avgDamage).toLocaleString()}</span>
          <span className="m-stat-chip">CS {p.avgCs.toFixed(1)}</span>
        </MobilePlayerCard>
      ))}

      {/* Item stats */}
      {data.itemStats.length > 0 && (
        <>
          <p className="m-section-title" style={{ marginTop: 8 }}>아이템 통계</p>
          <div className="grid-16">
            {data.itemStats.slice(0, 12).map(it => {
              const itemInfo = items.get(it.itemId);
              return (
                <div key={it.itemId} className="col-span-2" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4 }}>
                  {itemInfo?.imageUrl ? (
                    <img src={itemInfo.imageUrl} alt={itemInfo.nameKo} width={44} height={44} style={{ borderRadius: 8 }} title={itemInfo.nameKo} />
                  ) : (
                    <div style={{ width: 44, height: 44, borderRadius: 8, background: 'rgba(255,255,255,0.05)' }} />
                  )}
                  <div style={{ fontSize: 10, color: 'var(--color-text-secondary)' }}>{it.picks}회</div>
                  <div style={{ fontSize: 10, color: 'var(--color-win)' }}>{it.winRate.toFixed(0)}%</div>
                </div>
              );
            })}
          </div>
        </>
      )}
    </div>
  );
}
