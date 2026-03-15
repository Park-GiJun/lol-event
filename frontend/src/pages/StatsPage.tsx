import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../lib/api/api';
import type { StatsResponse, PlayerStats } from '../lib/types/stats';
import { LoadingCenter } from '../components/common/Spinner';
import { Button } from '../components/common/Button';
import '../styles/pages/stats.css';

const MODES = [
  { value: 'normal', label: '5v5 내전' },
  { value: 'aram',   label: '칼바람' },
  { value: 'all',    label: '전체' },
];

const MIN_GAMES = 3;

function HighlightCard({ label, player, value, sub }: {
  label: string; player: string; value: string | number; sub?: string;
}) {
  const navigate = useNavigate();
  return (
    <div className="highlight-card" onClick={() => navigate(`/stats/player/${encodeURIComponent(player)}`)}>
      <div className="highlight-label">{label}</div>
      <div className="highlight-player">{player.split('#')[0]}</div>
      <div className="highlight-value">{value}</div>
      {sub && <div className="highlight-sub">{sub}</div>}
    </div>
  );
}

function Highlights({ stats }: { stats: PlayerStats[] }) {
  const qualified = stats.filter(s => s.games >= MIN_GAMES);
  if (qualified.length === 0) return null;

  const best = (fn: (s: PlayerStats) => number) =>
    qualified.reduce((a, b) => fn(a) >= fn(b) ? a : b);

  const cards = [
    { label: '👑 승률 최고', s: best(s => s.winRate), val: (s: PlayerStats) => `${s.winRate}%`, sub: (s: PlayerStats) => `${s.games}판` },
    { label: '⚔️ KDA 장인', s: best(s => s.kda), val: (s: PlayerStats) => s.kda.toFixed(2), sub: () => 'KDA' },
    { label: '🗡️ 킬 장인',  s: best(s => s.avgKills), val: (s: PlayerStats) => s.avgKills.toFixed(1), sub: () => '평균 킬' },
    { label: '💥 딜 장인',  s: best(s => s.avgDamage), val: (s: PlayerStats) => s.avgDamage.toLocaleString(), sub: () => '평균 딜량' },
    { label: '🌾 CS 장인',  s: best(s => s.avgCs), val: (s: PlayerStats) => s.avgCs.toFixed(1), sub: () => '평균 CS' },
    { label: '🎮 판수왕',   s: best(s => s.games), val: (s: PlayerStats) => `${s.games}판`, sub: () => '최다 경기' },
  ];

  return (
    <div className="highlights-row">
      {cards.map(({ label, s, val, sub }) => (
        <HighlightCard key={label} label={label} player={s.riotId} value={val(s)} sub={sub(s)} />
      ))}
    </div>
  );
}

export function StatsPage() {
  const [data, setData]     = useState<StatsResponse | null>(null);
  const [mode, setMode]     = useState('normal');
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  const load = useCallback(async () => {
    setLoading(true);
    try { setData(await api.get<StatsResponse>(`/stats?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode]);

  useEffect(() => { load(); }, [load]);

  const winColor = (r: number) =>
    r >= 60 ? 'var(--color-win)' : r >= 50 ? 'var(--color-primary)' : 'var(--color-loss)';

  return (
    <div>
      <div className="page-header flex items-center justify-between">
        <div>
          <h1 className="page-title">전체 통계</h1>
          <p className="page-subtitle">경기 수: {data?.matchCount ?? 0}건</p>
        </div>
        <div className="flex gap-sm">
          {MODES.map(m => (
            <Button key={m.value} variant={mode === m.value ? 'primary' : 'secondary'}
              size="sm" onClick={() => setMode(m.value)}>{m.label}</Button>
          ))}
        </div>
      </div>

      {loading ? <LoadingCenter /> : (
        <>
          {data && <Highlights stats={data.stats} />}

          <div className="card">
            <div className="table-wrapper">
              <table className="table">
                <thead>
                  <tr>
                    <th>#</th>
                    <th>플레이어</th>
                    <th className="table-number">판수</th>
                    <th className="table-number">승률</th>
                    <th className="table-number">KDA</th>
                    <th className="table-number">평균 킬</th>
                    <th className="table-number">평균 데스</th>
                    <th className="table-number">평균 어시</th>
                    <th className="table-number">평균 딜량</th>
                    <th>주요 챔피언</th>
                  </tr>
                </thead>
                <tbody>
                  {data?.stats.map((s, i) => (
                    <tr key={s.riotId}>
                      <td style={{ color: 'var(--color-text-secondary)' }}>{i + 1}</td>
                      <td>
                        <button className="player-name-btn"
                          onClick={() => navigate(`/stats/player/${encodeURIComponent(s.riotId)}`)}>
                          {s.riotId}
                        </button>
                      </td>
                      <td className="table-number">{s.games}</td>
                      <td className="table-number font-bold" style={{ color: winColor(s.winRate) }}>{s.winRate}%</td>
                      <td className="table-number">{s.kda.toFixed(2)}</td>
                      <td className="table-number">{s.avgKills}</td>
                      <td className="table-number">{s.avgDeaths}</td>
                      <td className="table-number">{s.avgAssists}</td>
                      <td className="table-number">{s.avgDamage.toLocaleString()}</td>
                      <td><div className="flex gap-sm">{s.topChampions.map(c => (
                        <span key={c.champ} className="badge badge-normal">{c.champ} {c.count}</span>
                      ))}</div></td>
                    </tr>
                  ))}
                  {!data?.stats.length && (
                    <tr><td colSpan={10} style={{ textAlign: 'center', color: 'var(--color-text-secondary)', padding: '32px' }}>데이터 없음</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
