import { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { api } from '../lib/api/api';
import type { ChampionDetailStats, ChampionPlayerStat } from '../lib/types/stats';
import { LoadingCenter } from '../components/common/Spinner';
import { Button } from '../components/common/Button';
import { useDragon } from '../context/DragonContext';
import '../styles/pages/stats.css';

const MODES = [
  { value: 'normal', label: '5v5 내전' },
  { value: 'aram',   label: '칼바람' },
  { value: 'all',    label: '전체' },
];

const SORT_COLS: { key: keyof ChampionPlayerStat; label: string }[] = [
  { key: 'games',          label: '판수' },
  { key: 'winRate',        label: '승률' },
  { key: 'kda',            label: 'KDA' },
  { key: 'avgDamage',      label: '딜량' },
  { key: 'avgVisionScore', label: '시야' },
  { key: 'avgCs',          label: 'CS' },
];

const RANK_COLORS: Record<number, string> = { 1: '#FFD700', 2: '#C0C0C0', 3: '#CD7F32' };

function RankBadge({ rank }: { rank: number }) {
  const color = RANK_COLORS[rank];
  if (color) return (
    <div style={{
      width: 24, height: 24, borderRadius: '50%',
      background: color, display: 'flex', alignItems: 'center', justifyContent: 'center',
      fontSize: 11, fontWeight: 800, color: '#111', flexShrink: 0,
    }}>{rank}</div>
  );
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

export function ChampionStatsPage() {
  const { champion } = useParams<{ champion: string }>();
  const navigate = useNavigate();
  const { champions } = useDragon();

  const [data, setData]       = useState<ChampionDetailStats | null>(null);
  const [mode, setMode]       = useState('normal');
  const [loading, setLoading] = useState(true);
  const [sort, setSort]       = useState<keyof ChampionPlayerStat>('games');

  const load = useCallback(async () => {
    if (!champion) return;
    setLoading(true);
    try {
      setData(await api.get<ChampionDetailStats>(
        `/stats/champion/${encodeURIComponent(champion)}?mode=${mode}`
      ));
    } finally { setLoading(false); }
  }, [champion, mode]);

  useEffect(() => { load(); }, [load]);

  const sorted = data?.players
    ? [...data.players].sort((a, b) => (b[sort] as number) - (a[sort] as number))
    : [];

  const champImg = data ? champions.get(data.championId)?.imageUrl : null;

  return (
    <div>
      {/* 헤더 */}
      <div className="page-header flex items-center justify-between">
        <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          <button
            onClick={() => navigate(-1)}
            style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--color-text-secondary)', fontSize: 18, padding: '4px 8px' }}
          >←</button>
          {champImg && (
            <img
              src={champImg}
              alt={data?.champion}
              style={{ width: 52, height: 52, borderRadius: 8, border: '2px solid var(--color-border)', objectFit: 'cover' }}
              onError={e => { (e.target as HTMLImageElement).style.display = 'none'; }}
            />
          )}
          <div>
            <h1 className="page-title">{data?.champion ?? champion} 장인 랭킹</h1>
            <p className="page-subtitle">
              총 {data?.totalGames ?? 0}경기 · 승률 {data?.winRate ?? 0}% · {sorted.length}명
            </p>
          </div>
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
            {SORT_COLS.map(c => (
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
                  <th className="table-number">시야</th>
                  <th className="table-number">CS</th>
                  <th className="table-number">골드</th>
                </tr>
              </thead>
              <tbody>
                {sorted.map((p, i) => (
                  <tr key={p.riotId}
                    className="member-stats-row"
                    onClick={() => navigate(`/player-stats/${encodeURIComponent(p.riotId)}`)}>
                    <td><RankBadge rank={i + 1} /></td>
                    <td>
                      <div style={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                        <span style={{ fontWeight: 700, fontSize: 13 }}>{p.riotId.split('#')[0]}</span>
                        <span style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>#{p.riotId.split('#')[1]}</span>
                      </div>
                    </td>
                    <td>
                      <span style={{ fontWeight: 600, fontSize: 14 }}>{p.games}</span>
                      <span style={{ fontSize: 10, color: 'var(--color-text-disabled)', marginLeft: 2 }}>판</span>
                    </td>
                    <td><WinRatePill winRate={p.winRate} wins={p.wins} losses={p.games - p.wins} /></td>
                    <td><KdaDisplay kda={p.kda} kills={p.avgKills} deaths={p.avgDeaths} assists={p.avgAssists} /></td>
                    <td className="table-number" style={{ fontSize: 12 }}>{p.avgDamage.toLocaleString()}</td>
                    <td className="table-number" style={{ fontSize: 12 }}>{p.avgVisionScore.toFixed(1)}</td>
                    <td className="table-number" style={{ fontSize: 12 }}>{p.avgCs.toFixed(1)}</td>
                    <td className="table-number" style={{ fontSize: 12 }}>{p.avgGold.toLocaleString()}</td>
                  </tr>
                ))}
                {!sorted.length && (
                  <tr>
                    <td colSpan={9} style={{ textAlign: 'center', padding: '48px 0', color: 'var(--color-text-secondary)' }}>
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
