import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../lib/api/api';
import type { StatsResponse, PlayerStats } from '../lib/types/stats';
import { Skeleton } from '../components/common/Skeleton';
import { Button } from '../components/common/Button';
import { SortableTh } from '../components/common/SortableTh';
import { useSortTable } from '../hooks/useSortTable';
import { useDragon } from '../context/DragonContext';
import { PlayerLink } from '../components/common/PlayerLink';
import { RankBadge } from './stats-tabs/shared';
import { MODES } from '../lib/lol';
import '../styles/pages/stats.css';

function WinRatePill({ winRate, wins, losses }: { winRate: number; wins: number; losses: number }) {
  const color = winRate >= 60 ? 'var(--color-win)' : winRate >= 50 ? 'var(--color-primary)' : 'var(--color-loss)';
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 5 }}>
        <span style={{ fontWeight: 700, fontSize: 13, color, minWidth: 34 }}>{winRate}%</span>
        <span style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>
          <span style={{ color: 'var(--color-win)' }}>{wins}W</span>
          {' '}<span style={{ color: 'var(--color-loss)' }}>{losses}L</span>
        </span>
      </div>
      <div style={{ height: 3, background: 'var(--color-bg-hover)', borderRadius: 2, overflow: 'hidden', width: 80 }}>
        <div style={{ width: `${winRate}%`, height: '100%', background: color, borderRadius: 2 }} />
      </div>
    </div>
  );
}

function KdaDisplay({ kda, kills, deaths, assists }: { kda: number; kills: number; deaths: number; assists: number }) {
  const color = kda >= 5 ? 'var(--color-win)' : kda >= 3 ? 'var(--color-primary)' : 'var(--color-text-primary)';
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
      <span style={{ fontWeight: 700, fontSize: 13, color }}>{kda.toFixed(2)}</span>
      <span style={{ fontSize: 10, color: 'var(--color-text-secondary)' }}>
        {kills.toFixed(1)} / <span style={{ color: 'var(--color-error)' }}>{deaths.toFixed(1)}</span> / {assists.toFixed(1)}
      </span>
    </div>
  );
}

function ChampBadges({ champions }: { champions: PlayerStats['topChampions'] }) {
  const { champions: dragonChamps } = useDragon();
  return (
    <div style={{ display: 'flex', gap: 4, alignItems: 'center' }}>
      {champions.slice(0, 3).map(c => {
        const found = [...dragonChamps.values()].find(d => d.championKey === c.champ || d.nameKo === c.champ);
        return (
          <div key={c.champ} style={{ position: 'relative' }}>
            {found?.imageUrl
              ? <img src={found.imageUrl} alt={c.champ} width={26} height={26}
                  style={{ borderRadius: 4, border: '1px solid var(--color-border)', objectFit: 'cover' }}
                  onError={e => { (e.target as HTMLImageElement).style.display = 'none'; }} />
              : <div style={{ width: 26, height: 26, background: 'var(--color-bg-hover)', borderRadius: 4, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 9, color: 'var(--color-text-secondary)' }}>{c.champ.slice(0, 2)}</div>
            }
            <span style={{ position: 'absolute', bottom: -2, right: -2, background: 'var(--color-bg-secondary)', border: '1px solid var(--color-border)', borderRadius: 3, fontSize: 8, padding: '0 2px', lineHeight: '12px' }}>{c.count}</span>
          </div>
        );
      })}
    </div>
  );
}

type SortCol = 'games' | 'winRate' | 'kda' | 'avgKills' | 'avgDamage' | 'avgCs' | 'avgGold' | 'avgVisionScore';

function ColGroup() {
  return (
    <colgroup>
      <col style={{ width: 36 }} />
      <col />
      <col style={{ width: 52 }} />
      <col style={{ width: 110 }} />
      <col style={{ width: 90 }} />
      <col style={{ width: 80 }} />
      <col style={{ width: 60 }} />
      <col style={{ width: 72 }} />
      <col style={{ width: 68 }} />
      <col style={{ width: 90 }} />
    </colgroup>
  );
}

export function MemberStatsListPage() {
  const [data, setData]       = useState<StatsResponse | null>(null);
  const [mode, setMode]       = useState('normal');
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();
  const { sortKey, sortDir, handleSort, sorted } = useSortTable<SortCol>('winRate');

  const load = useCallback(async () => {
    setLoading(true);
    try { setData(await api.get<StatsResponse>(`/stats?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode]);

  useEffect(() => { load(); }, [load]);

  const getValue = (key: SortCol, s: PlayerStats): number => {
    if (key === 'games')      return s.games;
    if (key === 'winRate')    return s.winRate;
    if (key === 'kda')        return s.kda;
    if (key === 'avgKills')   return s.avgKills;
    if (key === 'avgDamage')       return s.avgDamage;
    if (key === 'avgCs')           return s.avgCs;
    if (key === 'avgGold')         return s.avgGold;
    if (key === 'avgVisionScore')  return s.avgVisionScore;
    return 0;
  };

  const displayed = data?.stats ? sorted(data.stats, getValue) : [];

  return (
    <div>
      <div className="page-header flex items-center justify-between">
        <div>
          <h1 className="page-title">멤버 통계</h1>
          <p className="page-subtitle">총 {data?.matchCount ?? 0}경기 · {displayed.length}명</p>
        </div>
        <div className="flex gap-sm">
          {MODES.map(m => (
            <Button key={m.value} variant={mode === m.value ? 'primary' : 'secondary'}
              size="sm" onClick={() => setMode(m.value)}>{m.label}</Button>
          ))}
        </div>
      </div>

      <div className="card">
        <div className="table-wrapper">
          <table className="table member-stats-table">
            <ColGroup />
            <thead>
              <tr>
                <th>#</th>
                <th>플레이어</th>
                <SortableTh label="판수"     col="games"          sortKey={sortKey} sortDir={sortDir} onSort={handleSort} />
                <SortableTh label="승률"     col="winRate"        sortKey={sortKey} sortDir={sortDir} onSort={handleSort} />
                <SortableTh label="KDA"      col="kda"            sortKey={sortKey} sortDir={sortDir} onSort={handleSort} />
                <SortableTh label="평균 딜"  col="avgDamage"      sortKey={sortKey} sortDir={sortDir} onSort={handleSort} right />
                <SortableTh label="CS"       col="avgCs"          sortKey={sortKey} sortDir={sortDir} onSort={handleSort} right />
                <SortableTh label="골드"     col="avgGold"        sortKey={sortKey} sortDir={sortDir} onSort={handleSort} right />
                <SortableTh label="비전"     col="avgVisionScore" sortKey={sortKey} sortDir={sortDir} onSort={handleSort} right />
                <th>주요 챔피언</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                Array.from({ length: 8 }).map((_, i) => (
                  <tr key={i}>
                    <td><Skeleton className="h-6 w-6 rounded-full" /></td>
                    <td><Skeleton className="h-4 w-28" /></td>
                    <td><Skeleton className="h-4 w-10" /></td>
                    <td><Skeleton className="h-4 w-20" /></td>
                    <td><Skeleton className="h-4 w-14" /></td>
                    <td><Skeleton className="h-4 w-16 ml-auto" /></td>
                    <td><Skeleton className="h-4 w-10 ml-auto" /></td>
                    <td><Skeleton className="h-4 w-14 ml-auto" /></td>
                    <td><Skeleton className="h-4 w-10 ml-auto" /></td>
                    <td>
                      <div className="flex gap-1">
                        {Array.from({ length: 3 }).map((_, j) => <Skeleton key={j} className="h-6 w-6 rounded" />)}
                      </div>
                    </td>
                  </tr>
                ))
              ) : displayed.length === 0 ? (
                <tr>
                  <td colSpan={8} style={{ textAlign: 'center', padding: '48px 0', color: 'var(--color-text-secondary)' }}>
                    데이터 없음
                  </td>
                </tr>
              ) : (
                displayed.map((s, i) => (
                  <tr key={s.riotId}
                    className="member-stats-row"
                    onClick={() => navigate(`/player-stats/${encodeURIComponent(s.riotId)}`)}>
                    <td><RankBadge rank={i + 1} /></td>
                    <td>
                      <PlayerLink riotId={s.riotId} mode={mode}>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                          <span style={{ fontWeight: 700, fontSize: 13 }}>{s.riotId.split('#')[0]}</span>
                          <span style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>#{s.riotId.split('#')[1]}</span>
                        </div>
                      </PlayerLink>
                    </td>
                    <td>
                      <span style={{ fontWeight: 600, fontSize: 14 }}>{s.games}</span>
                      <span style={{ fontSize: 10, color: 'var(--color-text-disabled)', marginLeft: 2 }}>판</span>
                    </td>
                    <td><WinRatePill winRate={s.winRate} wins={s.wins} losses={s.losses} /></td>
                    <td><KdaDisplay kda={s.kda} kills={s.avgKills} deaths={s.avgDeaths} assists={s.avgAssists} /></td>
                    <td className="table-number" style={{ fontSize: 12 }}>{s.avgDamage.toLocaleString()}</td>
                    <td className="table-number" style={{ fontSize: 12 }}>{s.avgCs.toFixed(1)}</td>
                    <td className="table-number" style={{ fontSize: 12 }}>{(s.avgGold ?? 0).toLocaleString()}</td>
                    <td className="table-number" style={{ fontSize: 12 }}>{(s.avgVisionScore ?? 0).toFixed(1)}</td>
                    <td><ChampBadges champions={s.topChampions} /></td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
