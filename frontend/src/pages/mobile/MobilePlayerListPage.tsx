import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { usePlayers } from '@/hooks/usePlayers';
import { useDragon } from '@/context/DragonContext';
import { Skeleton } from '@/components/common/Skeleton';
import { InlineError } from '@/components/common/InlineError';
import { MODES } from '@/lib/lol';
import { MobilePlayerCard } from '@/components/mobile/MobilePlayerCard';
import type { PlayerStats } from '@/lib/types/stats';

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
  const { champions: dragonChampions } = useDragon();
  const [internalMode, setInternalMode] = useState('normal');
  const mode = externalMode ?? internalMode;
  const setMode = onModeChange ?? setInternalMode;

  const [sort, setSort] = useState<SortKey>('winRate');
  const [sortAsc, setSortAsc] = useState(false);

  const { data, isLoading, error, refetch } = usePlayers(mode);

  // Build key-lookup map once per dragon champions reference — avoids O(n) scan per player row
  const champKeyMap = useMemo(() => {
    const m = new Map<string, { imageUrl: string | null; nameKo: string }>();
    for (const d of dragonChampions.values()) {
      if (d.championKey) m.set(d.championKey, d);
      m.set(d.nameKo, d);
    }
    return m;
  }, [dragonChampions]);

  const handleSortClick = (key: SortKey) => {
    if (sort === key) setSortAsc(a => !a);
    else { setSort(key); setSortAsc(false); }
  };

  if (isLoading) {
    return (
      <div>
        {!externalMode && (
          <div className="m-mode-chips">
            {MODES.map(m => (
              <button key={m.value} className={`m-sort-chip${mode === m.value ? ' active' : ''}`} onClick={() => setMode(m.value)}>
                {m.label}
              </button>
            ))}
          </div>
        )}
        <div className="m-sort-chips">
          {SORT_OPTS.map(o => (
            <button key={o.key} className={`m-sort-chip${sort === o.key ? ' active' : ''}`} disabled aria-disabled="true">
              {o.label}{sort === o.key ? (sortAsc ? ' ↑' : ' ↓') : ''}
            </button>
          ))}
        </div>
        {Array.from({ length: 5 }).map((_, i) => (
          <div key={i} className="m-player-card">
            <div className="m-player-card-header">
              <Skeleton style={{ width: 28, height: 28, borderRadius: '50%', flexShrink: 0 }} />
              <div style={{ flex: 1 }}>
                <Skeleton style={{ height: 14, width: '50%', borderRadius: 4 }} />
              </div>
              <Skeleton style={{ width: 32, height: 14, borderRadius: 4 }} />
            </div>
          </div>
        ))}
      </div>
    );
  }

  if (error) {
    return <InlineError message="플레이어 통계를 불러오지 못했습니다." onRetry={() => void refetch()} />;
  }

  const sorted = (data?.stats ?? []).slice().sort((a: PlayerStats, b: PlayerStats) => {
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

      {sorted.length === 0 && <div className="m-empty">데이터가 없습니다</div>}

      {sorted.map((p: PlayerStats, i: number) => {
        const kdaVal = p.avgDeaths === 0 ? 'Perfect' : ((p.avgKills + p.avgAssists) / p.avgDeaths).toFixed(2);
        const dest = `/m/player/${encodeURIComponent(p.riotId)}`;
        return (
          <MobilePlayerCard
            key={p.riotId}
            riotId={p.riotId}
            rank={i + 1}
            rightSlot={<span className="m-player-games">{p.games}판</span>}
            winBar={{ winRate: p.winRate, wins: p.wins, losses: p.losses }}
            footer={p.topChampions.length > 0 ? (
              <div className="m-champ-icons">
                {p.topChampions.slice(0, 3).map(tc => {
                  const champInfo = champKeyMap.get(tc.champ);
                  return champInfo?.imageUrl ? (
                    <img key={tc.champ} src={champInfo.imageUrl} alt={champInfo.nameKo} className="m-champ-icon-sm"
                      loading="lazy"
                      title={`${champInfo.nameKo} (${tc.count}게임)`}
                      onError={e => { (e.currentTarget as HTMLImageElement).style.display = 'none'; }}
                    />
                  ) : (
                    <div key={tc.champ} className="m-champ-icon-sm-placeholder">{tc.champ.slice(0, 2)}</div>
                  );
                })}
              </div>
            ) : undefined}
            onClick={() => navigate(dest)}
          >
            <span className="m-stat-chip">KDA {kdaVal}</span>
            <span className="m-stat-chip">딜 {Math.round(p.avgDamage).toLocaleString()}</span>
            <span className="m-stat-chip">CS {p.avgCs.toFixed(1)}</span>
          </MobilePlayerCard>
        );
      })}
    </div>
  );
}
