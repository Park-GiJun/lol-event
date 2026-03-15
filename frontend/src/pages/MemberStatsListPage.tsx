import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../lib/api/api';
import type { StatsResponse, PlayerStats } from '../lib/types/stats';
import { LoadingCenter } from '../components/common/Spinner';
import { Button } from '../components/common/Button';
import { useDragon } from '../context/DragonContext';
import '../styles/pages/stats.css';

const MODES = [
  { value: 'normal', label: '5v5 내전' },
  { value: 'aram',   label: '칼바람' },
  { value: 'all',    label: '전체' },
];

const RANK_COLORS: Record<number, string> = {
  1: '#FFD700',
  2: '#C0C0C0',
  3: '#CD7F32',
};

function RankBadge({ rank }: { rank: number }) {
  const color = RANK_COLORS[rank];
  if (color) {
    return (
      <div style={{
        width: 24, height: 24, borderRadius: '50%',
        background: color, display: 'flex', alignItems: 'center', justifyContent: 'center',
        fontSize: 11, fontWeight: 800, color: '#111', flexShrink: 0,
      }}>{rank}</div>
    );
  }
  return <span style={{ color: 'var(--color-text-disabled)', fontSize: 12, width: 24, textAlign: 'center' }}>{rank}</span>;
}

function WinRatePill({ winRate, wins, losses }: { winRate: number; wins: number; losses: number }) {
  const color = winRate >= 60 ? 'var(--color-win)' : winRate >= 50 ? 'var(--color-primary)' : 'var(--color-loss)';
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 3, minWidth: 90 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
        <span style={{ fontWeight: 700, fontSize: 13, color, minWidth: 36 }}>{winRate}%</span>
        <span style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>
          <span style={{ color: 'var(--color-win)' }}>{wins}W</span>
          {' '}<span style={{ color: 'var(--color-loss)' }}>{losses}L</span>
        </span>
      </div>
      <div style={{ height: 4, background: 'var(--color-bg-hover)', borderRadius: 2, overflow: 'hidden' }}>
        <div style={{ width: `${winRate}%`, height: '100%', background: color, borderRadius: 2, transition: 'width 0.3s' }} />
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

export function MemberStatsListPage() {
  const [data, setData]         = useState<StatsResponse | null>(null);
  const [mode, setMode]         = useState('normal');
  const [loading, setLoading]   = useState(true);
  const [sort, setSort]         = useState<keyof PlayerStats>('winRate');
  const navigate = useNavigate();

  const load = useCallback(async () => {
    setLoading(true);
    try { setData(await api.get<StatsResponse>(`/stats?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode]);

  useEffect(() => { load(); }, [load]);

  const sorted = data?.stats
    ? [...data.stats].sort((a, b) => (b[sort] as number) - (a[sort] as number))
    : [];

  const cols: { key: keyof PlayerStats; label: string }[] = [
    { key: 'winRate',   label: '승률' },
    { key: 'kda',       label: 'KDA' },
    { key: 'avgKills',  label: '킬' },
    { key: 'avgDamage', label: '딜량' },
    { key: 'avgCs',     label: 'CS' },
    { key: 'games',     label: '판수' },
  ];

  return (
    <div>
      <div className="page-header flex items-center justify-between">
        <div>
          <h1 className="page-title">멤버 통계</h1>
          <p className="page-subtitle">총 {data?.matchCount ?? 0}경기 · {sorted.length}명</p>
        </div>
        <div className="flex gap-sm">
          {MODES.map(m => (
            <Button key={m.value} variant={mode === m.value ? 'primary' : 'secondary'}
              size="sm" onClick={() => setMode(m.value)}>{m.label}</Button>
          ))}
        </div>
      </div>

      {loading ? <LoadingCenter /> : (
        <div className="card">
          {/* 정렬 탭 */}
          <div className="member-sort-tabs">
            {cols.map(c => (
              <button key={c.key}
                className={`member-sort-tab ${sort === c.key ? 'active' : ''}`}
                onClick={() => setSort(c.key)}>
                {c.label}
              </button>
            ))}
          </div>

          <div className="table-wrapper">
            <table className="table member-stats-table">
              <thead>
                <tr>
                  <th style={{ width: 36 }}>#</th>
                  <th>플레이어</th>
                  <th>판수</th>
                  <th style={{ minWidth: 120 }}>승률</th>
                  <th>KDA</th>
                  <th className="table-number">평균 딜</th>
                  <th className="table-number">CS</th>
                  <th>주요 챔피언</th>
                </tr>
              </thead>
              <tbody>
                {sorted.map((s, i) => (
                  <tr key={s.riotId}
                    className="member-stats-row"
                    onClick={() => navigate(`/player-stats/${encodeURIComponent(s.riotId)}`)}>
                    <td><RankBadge rank={i + 1} /></td>
                    <td>
                      <div style={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                        <span style={{ fontWeight: 700, fontSize: 13 }}>{s.riotId.split('#')[0]}</span>
                        <span style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>#{s.riotId.split('#')[1]}</span>
                      </div>
                    </td>
                    <td>
                      <span style={{ fontWeight: 600, fontSize: 14 }}>{s.games}</span>
                      <span style={{ fontSize: 10, color: 'var(--color-text-disabled)', marginLeft: 2 }}>판</span>
                    </td>
                    <td><WinRatePill winRate={s.winRate} wins={s.wins} losses={s.losses} /></td>
                    <td><KdaDisplay kda={s.kda} kills={s.avgKills} deaths={s.avgDeaths} assists={s.avgAssists} /></td>
                    <td className="table-number" style={{ fontSize: 12 }}>{s.avgDamage.toLocaleString()}</td>
                    <td className="table-number" style={{ fontSize: 12 }}>{s.avgCs.toFixed(1)}</td>
                    <td><ChampBadges champions={s.topChampions} /></td>
                  </tr>
                ))}
                {!sorted.length && (
                  <tr>
                    <td colSpan={8} style={{ textAlign: 'center', padding: '48px 0', color: 'var(--color-text-secondary)' }}>
                      데이터 없음
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
