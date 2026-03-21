import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../../lib/api/api';
import type { StatsResponse, PlayerStats } from '../../lib/types/stats';
import { useDragon } from '../../context/DragonContext';
import { LoadingCenter } from '../../components/common/Spinner';

const MODES = [
  { value: 'normal', label: '5v5' },
  { value: 'aram', label: '칼바람' },
  { value: 'all', label: '전체' },
];

type SortKey = 'winRate' | 'kda' | 'avgKills' | 'avgDamage' | 'avgCs' | 'games';
const SORT_OPTS: { key: SortKey; label: string }[] = [
  { key: 'winRate', label: '승률' },
  { key: 'kda', label: 'KDA' },
  { key: 'avgKills', label: '킬' },
  { key: 'avgDamage', label: '딜량' },
  { key: 'avgCs', label: 'CS' },
  { key: 'games', label: '판수' },
];

interface Props {
  mode?: string;
  onModeChange?: (m: string) => void;
}

export function MobilePlayerListPage({ mode: externalMode, onModeChange }: Props = {}) {
  const navigate = useNavigate();
  const { champions } = useDragon();
  const [internalMode, setInternalMode] = useState('normal');
  const mode = externalMode ?? internalMode;
  const setMode = onModeChange ?? setInternalMode;

  const [stats, setStats] = useState<PlayerStats[]>([]);
  const [loading, setLoading] = useState(true);
  const [sort, setSort] = useState<SortKey>('winRate');
  const [sortAsc, setSortAsc] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await api.get<StatsResponse>(`/stats?mode=${mode}`);
      setStats(res.stats);
    } finally {
      setLoading(false);
    }
  }, [mode]);

  useEffect(() => { load(); }, [load]);

  const handleSortClick = (key: SortKey) => {
    if (sort === key) setSortAsc(a => !a);
    else { setSort(key); setSortAsc(false); }
  };

  const sorted = stats.slice().sort((a, b) => {
    const diff = (b[sort] as number) - (a[sort] as number);
    return sortAsc ? -diff : diff;
  });

  return (
    <div>
      {/* Mode chips */}
      {!externalMode && (
        <div className="m-mode-chips">
          {MODES.map(m => (
            <button key={m.value} className={`m-sort-chip${mode === m.value ? ' active' : ''}`} onClick={() => setMode(m.value)}>
              {m.label}
            </button>
          ))}
        </div>
      )}

      {/* Sort chips */}
      <div className="m-sort-chips">
        {SORT_OPTS.map(o => (
          <button key={o.key} className={`m-sort-chip${sort === o.key ? ' active' : ''}`} onClick={() => handleSortClick(o.key)}>
            {o.label}{sort === o.key ? (sortAsc ? ' ↑' : ' ↓') : ''}
          </button>
        ))}
      </div>

      {loading ? <LoadingCenter /> : (
        <>
          {sorted.length === 0 && <div className="m-empty">데이터가 없습니다</div>}
          {sorted.map((p, i) => {
            const [name, tag] = p.riotId.split('#');
            const kdaVal = p.avgDeaths === 0 ? 'Perfect' : ((p.avgKills + p.avgAssists) / p.avgDeaths).toFixed(2);
            return (
              <div
                key={p.riotId}
                className="m-player-card"
                onClick={() => navigate(`/m/player/${encodeURIComponent(p.riotId)}`)}
              >
                <div className="m-player-card-header">
                  <div className={`m-player-rank${i < 3 ? ` rank-${i + 1}` : ''}`}>{i + 1}</div>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ display: 'flex', alignItems: 'baseline', gap: 4 }}>
                      <span className="m-player-name">{name}</span>
                      {tag && <span className="m-player-tag">#{tag}</span>}
                    </div>
                  </div>
                  <span className="m-player-games">{p.games}판</span>
                </div>

                {/* Win bar */}
                <div className="m-win-bar-wrap">
                  <div className="m-win-bar-label">
                    <span>{p.winRate.toFixed(1)}%</span>
                    <span>{p.wins}승 {p.losses}패</span>
                  </div>
                  <div className="m-win-bar">
                    <div className="m-win-bar-fill" style={{ width: `${p.winRate}%` }} />
                  </div>
                </div>

                {/* Stat chips */}
                <div className="m-stat-chips">
                  <span className="m-stat-chip">KDA {kdaVal}</span>
                  <span className="m-stat-chip">딜 {Math.round(p.avgDamage).toLocaleString()}</span>
                  <span className="m-stat-chip">CS {p.avgCs.toFixed(1)}</span>
                </div>

                {/* Top champion icons */}
                {p.topChampions.length > 0 && (
                  <div className="m-champ-icons">
                    {p.topChampions.slice(0, 3).map(tc => {
                      const champInfo = Array.from(champions.values()).find(c => c.championKey === tc.champ || c.nameKo === tc.champ);
                      return champInfo?.imageUrl ? (
                        <img key={tc.champ} src={champInfo.imageUrl} alt={champInfo.nameKo} className="m-champ-icon-sm"
                          title={`${champInfo.nameKo} (${tc.count}게임)`}
                          onError={e => { (e.currentTarget as HTMLImageElement).style.display = 'none'; }}
                        />
                      ) : (
                        <div key={tc.champ} className="m-champ-icon-sm-placeholder">{tc.champ.slice(0, 2)}</div>
                      );
                    })}
                  </div>
                )}
              </div>
            );
          })}
        </>
      )}
    </div>
  );
}
