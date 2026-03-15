import { useEffect, useState, useCallback } from 'react';
import { api } from '../lib/api/api';
import type { StatsResponse, PlayerStats } from '../lib/types/stats';
import { LoadingCenter } from '../components/common/Spinner';
import { Button } from '../components/common/Button';

const MODES = [
  { value: 'normal', label: '5v5 내전' },
  { value: 'aram',   label: '칼바람' },
  { value: 'all',    label: '전체' },
];

export function StatsPage() {
  const [data, setData] = useState<StatsResponse | null>(null);
  const [mode, setMode] = useState('normal');
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await api.get<StatsResponse>(`/stats?mode=${mode}`);
      setData(res);
    } finally { setLoading(false); }
  }, [mode]);

  useEffect(() => { load(); }, [load]);

  return (
    <div>
      <div className="page-header flex items-center justify-between">
        <div>
          <h1 className="page-title">통계</h1>
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
                {data?.stats.map((s, i) => <StatsRow key={s.riotId} rank={i + 1} stats={s} />)}
                {!data?.stats.length && (
                  <tr><td colSpan={10} style={{ textAlign: 'center', color: 'var(--color-text-secondary)', padding: '32px' }}>데이터 없음</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}

function StatsRow({ rank, stats: s }: { rank: number; stats: PlayerStats }) {
  const winRateColor = s.winRate >= 60 ? 'var(--color-win)' : s.winRate >= 50 ? 'var(--color-primary)' : 'var(--color-loss)';
  return (
    <tr>
      <td style={{ color: 'var(--color-text-secondary)' }}>{rank}</td>
      <td className="font-semibold">{s.riotId}</td>
      <td className="table-number">{s.games}</td>
      <td className="table-number font-bold" style={{ color: winRateColor }}>{s.winRate}%</td>
      <td className="table-number">{s.kda.toFixed(2)}</td>
      <td className="table-number">{s.avgKills}</td>
      <td className="table-number">{s.avgDeaths}</td>
      <td className="table-number">{s.avgAssists}</td>
      <td className="table-number">{s.avgDamage.toLocaleString()}</td>
      <td><div className="flex gap-sm">{s.topChampions.map(c => (
        <span key={c.champ} className="badge badge-normal">{c.champ} {c.count}</span>
      ))}</div></td>
    </tr>
  );
}
