import { useState } from 'react';
import { api } from '../../lib/api/api';
import type { PlayerComparisonResult, PlayerStatSnapshot } from '../../lib/types/stats';
import { LoadingCenter } from '../../components/common/Spinner';
import { Button } from '../../components/common/Button';

export default function CompareTab({ mode }: { mode: string }) {
  const [p1Input, setP1Input] = useState('');
  const [p2Input, setP2Input] = useState('');
  const [data, setData] = useState<PlayerComparisonResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = async () => {
    if (!p1Input.trim() || !p2Input.trim()) return;
    setLoading(true);
    setError(null);
    try {
      const result = await api.get<PlayerComparisonResult>(
        `/stats/compare?player1=${encodeURIComponent(p1Input.trim())}&player2=${encodeURIComponent(p2Input.trim())}&mode=${mode}`
      );
      setData(result);
    } catch {
      setError('데이터를 불러오지 못했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const StatRow = ({ label, v1, v2, higher }: { label: string; v1: string | number; v2: string | number; higher?: 'p1' | 'p2' | 'none' }) => (
    <tr>
      <td style={{ fontWeight: higher === 'p1' ? 700 : 400, color: higher === 'p1' ? 'var(--color-win)' : undefined, textAlign: 'center', padding: '5px 8px', fontSize: 12 }}>{v1}</td>
      <td style={{ textAlign: 'center', padding: '5px 8px', fontSize: 11, color: 'var(--color-text-secondary)', whiteSpace: 'nowrap' }}>{label}</td>
      <td style={{ fontWeight: higher === 'p2' ? 700 : 400, color: higher === 'p2' ? 'var(--color-win)' : undefined, textAlign: 'center', padding: '5px 8px', fontSize: 12 }}>{v2}</td>
    </tr>
  );

  const compareSnap = (s1: PlayerStatSnapshot | null, s2: PlayerStatSnapshot | null, title: string) => {
    if (!s1 && !s2) return null;
    const safe = (v: number | undefined) => v?.toFixed(2) ?? '-';
    const hi = (a: number | undefined, b: number | undefined): 'p1' | 'p2' | 'none' =>
      a == null || b == null ? 'none' : a > b ? 'p1' : a < b ? 'p2' : 'none';
    return (
      <div style={{ marginBottom: 20 }}>
        <h4 style={{ fontSize: 13, fontWeight: 700, marginBottom: 8 }}>{title}</h4>
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr>
              <th style={{ textAlign: 'center', padding: '4px 8px', fontSize: 12, color: 'var(--color-primary)' }}>{data?.player1.split('#')[0]}</th>
              <th style={{ textAlign: 'center', padding: '4px 8px', fontSize: 11, color: 'var(--color-text-disabled)' }}>지표</th>
              <th style={{ textAlign: 'center', padding: '4px 8px', fontSize: 12, color: 'var(--color-primary)' }}>{data?.player2.split('#')[0]}</th>
            </tr>
          </thead>
          <tbody>
            <StatRow label="경기수" v1={s1?.games ?? '-'} v2={s2?.games ?? '-'} higher={hi(s1?.games, s2?.games)} />
            <StatRow label="승률%" v1={s1 ? s1.winRate.toFixed(1) : '-'} v2={s2 ? s2.winRate.toFixed(1) : '-'} higher={hi(s1?.winRate, s2?.winRate)} />
            <StatRow label="KDA" v1={s1 ? safe(s1.kda) : '-'} v2={s2 ? safe(s2.kda) : '-'} higher={hi(s1?.kda, s2?.kda)} />
            <StatRow label="평균킬" v1={s1 ? safe(s1.avgKills) : '-'} v2={s2 ? safe(s2.avgKills) : '-'} higher={hi(s1?.avgKills, s2?.avgKills)} />
            <StatRow label="평균데스" v1={s1 ? safe(s1.avgDeaths) : '-'} v2={s2 ? safe(s2.avgDeaths) : '-'} higher={hi(s2?.avgDeaths, s1?.avgDeaths)} />
            <StatRow label="평균딜" v1={s1 ? Math.round(s1.avgDamage).toLocaleString() : '-'} v2={s2 ? Math.round(s2.avgDamage).toLocaleString() : '-'} higher={hi(s1?.avgDamage, s2?.avgDamage)} />
            <StatRow label="평균CS" v1={s1 ? safe(s1.avgCs) : '-'} v2={s2 ? safe(s2.avgCs) : '-'} higher={hi(s1?.avgCs, s2?.avgCs)} />
            <StatRow label="시야" v1={s1 ? safe(s1.avgVisionScore) : '-'} v2={s2 ? safe(s2.avgVisionScore) : '-'} higher={hi(s1?.avgVisionScore, s2?.avgVisionScore)} />
          </tbody>
        </table>
      </div>
    );
  };

  return (
    <div>
      <div className="grid-16" style={{ marginBottom: 16 }}>
        <input
          value={p1Input} onChange={e => setP1Input(e.target.value)}
          placeholder="플레이어1 (RiotID#태그)"
          className="col-span-6"
          style={{ minWidth: 140, padding: '7px 10px', borderRadius: 6, border: '1px solid var(--color-border)', background: 'var(--color-bg-secondary)', color: 'var(--color-text-primary)', fontSize: 13 }}
        />
        <input
          value={p2Input} onChange={e => setP2Input(e.target.value)}
          placeholder="플레이어2 (RiotID#태그)"
          className="col-span-6"
          style={{ minWidth: 140, padding: '7px 10px', borderRadius: 6, border: '1px solid var(--color-border)', background: 'var(--color-bg-secondary)', color: 'var(--color-text-primary)', fontSize: 13 }}
        />
        <Button className="col-span-4" variant="primary" size="sm" onClick={load} disabled={loading}>
          {loading ? '로딩...' : '비교'}
        </Button>
      </div>

      {error && <p style={{ color: 'var(--color-loss)', fontSize: 13 }}>{error}</p>}
      {loading && <LoadingCenter />}

      {data && !loading && (
        <div>
          <div className="grid-16" style={{ marginBottom: 16 }}>
            <div className="card col-span-8" style={{ padding: 12, textAlign: 'center' }}>
              <div style={{ fontSize: 13, fontWeight: 700, color: 'var(--color-primary)' }}>{data.player1.split('#')[0]}</div>
              <div style={{ fontSize: 11, color: 'var(--color-text-disabled)' }}>{data.player1}</div>
            </div>
            <div className="card col-span-8" style={{ padding: 12, textAlign: 'center' }}>
              <div style={{ fontSize: 13, fontWeight: 700, color: 'var(--color-primary)' }}>{data.player2.split('#')[0]}</div>
              <div style={{ fontSize: 11, color: 'var(--color-text-disabled)' }}>{data.player2}</div>
            </div>
          </div>

          {data.togetherGames > 0 && (
            <div className="card" style={{ padding: 12, marginBottom: 12 }}>
              <div style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginBottom: 8 }}>
                함께한 경기: <strong>{data.togetherGames}판</strong> | 승률: <strong style={{ color: data.togetherWinRate >= 50 ? 'var(--color-win)' : 'var(--color-loss)' }}>{data.togetherWinRate.toFixed(1)}%</strong>
              </div>
              {compareSnap(data.p1TogetherStats, data.p2TogetherStats, '🤝 함께 플레이')}
            </div>
          )}

          {data.versusGames > 0 && (
            <div className="card" style={{ padding: 12, marginBottom: 12 }}>
              <div style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginBottom: 8 }}>
                맞대결: <strong>{data.versusGames}판</strong> | {data.player1.split('#')[0]} 승률: <strong style={{ color: data.player1VsWinRate >= 50 ? 'var(--color-win)' : 'var(--color-loss)' }}>{data.player1VsWinRate.toFixed(1)}%</strong>
              </div>
              {compareSnap(data.p1VersusStats, data.p2VersusStats, '⚔️ 맞대결')}
            </div>
          )}

          <div className="card" style={{ padding: 12 }}>
            {compareSnap(data.overallP1Stats, data.overallP2Stats, '📊 전체 통계')}
          </div>
        </div>
      )}
    </div>
  );
}
