import { useEffect, useState, useCallback } from 'react';
import { api } from '../../lib/api/api';
import type { ComebackIndexResult, ComebackIndexEntry } from '../../lib/types/stats';
import { LoadingCenter } from '../../components/common/Spinner';
import { PlayerLink } from '../../components/common/PlayerLink';
import { RankBadge } from './shared';

export default function ComebackTab({ mode }: { mode: string }) {
  const [data, setData] = useState<ComebackIndexResult | null>(null);
  const [loading, setLoading] = useState(false);

  const load = useCallback(async () => {
    if (data) return;
    setLoading(true);
    try { setData(await api.get<ComebackIndexResult>(`/stats/comeback?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode, data]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  const kings = data.rankings.filter((e: ComebackIndexEntry) => e.isKing);

  return (
    <div>
      {(data.comebackKing || kings.length > 0) && (
        <div style={{ marginBottom: 20 }}>
          <div className="card" style={{ padding: 14, textAlign: 'center', background: 'linear-gradient(135deg, var(--color-bg-secondary), var(--color-bg-hover))' }}>
            <div style={{ fontSize: 24, marginBottom: 4 }}>🔄</div>
            <div style={{ fontSize: 11, color: 'var(--color-text-disabled)', marginBottom: 4 }}>컴백킹</div>
            <div style={{ fontWeight: 700, fontSize: 16, color: '#FFD700' }}>
              {data.comebackKing ? data.comebackKing.split('#')[0] : kings[0]?.riotId.split('#')[0] ?? '-'}
            </div>
            {kings[0] && (
              <div style={{ fontSize: 11, color: 'var(--color-text-secondary)', marginTop: 6 }}>
                접전 승률 {kings[0].contestWinRate.toFixed(1)}% | 접전 {kings[0].contestGames}게임
              </div>
            )}
          </div>
        </div>
      )}

      <h3 style={{ fontSize: 13, fontWeight: 700, marginBottom: 10 }}>접전 vs 압도 경기 승률</h3>
      <div style={{ overflowX: 'auto' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
          <thead>
            <tr style={{ background: 'var(--color-bg-hover)' }}>
              <th style={{ padding: '7px 10px', textAlign: 'left' }}>플레이어</th>
              <th style={{ padding: '7px 10px', textAlign: 'center' }}>전체 승률</th>
              <th style={{ padding: '7px 10px', textAlign: 'center' }}>접전 승률<br/><span style={{ fontSize: 10, fontWeight: 400, color: 'var(--color-text-disabled)' }}>(항복경기 제외)</span></th>
              <th style={{ padding: '7px 10px', textAlign: 'center' }}>압도 승률<br/><span style={{ fontSize: 10, fontWeight: 400, color: 'var(--color-text-disabled)' }}>(항복경기)</span></th>
              <th style={{ padding: '7px 10px', textAlign: 'center' }}>컴백보너스</th>
            </tr>
          </thead>
          <tbody>
            {data.rankings.map((e: ComebackIndexEntry, i: number) => (
              <tr key={e.riotId} style={{ borderTop: '1px solid var(--color-border)', background: e.isKing ? 'var(--color-win)11' : undefined }}>
                <td style={{ padding: '7px 10px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                    <RankBadge rank={i + 1} />
                    <PlayerLink riotId={e.riotId} mode={mode}>
                      <span style={{ fontWeight: 600 }}>{e.riotId.split('#')[0]}</span>
                    </PlayerLink>
                    {e.isKing && <span style={{ fontSize: 10, color: '#FFD700' }}>👑</span>}
                  </div>
                  <div style={{ fontSize: 10, color: 'var(--color-text-disabled)', paddingLeft: 32 }}>{e.totalGames}게임</div>
                </td>
                <td style={{ padding: '7px 10px', textAlign: 'center', color: e.totalWinRate >= 50 ? 'var(--color-win)' : 'var(--color-loss)', fontWeight: 600 }}>
                  {e.totalWinRate.toFixed(1)}%
                </td>
                <td style={{ padding: '7px 10px', textAlign: 'center' }}>
                  {e.contestGames > 0 ? (
                    <>
                      <span style={{ fontWeight: 600, color: e.contestWinRate >= 50 ? 'var(--color-win)' : 'var(--color-loss)' }}>{e.contestWinRate.toFixed(1)}%</span>
                      <div style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>{e.contestGames}게임</div>
                    </>
                  ) : <span style={{ color: 'var(--color-text-disabled)' }}>-</span>}
                </td>
                <td style={{ padding: '7px 10px', textAlign: 'center' }}>
                  {e.surrenderGames > 0 ? (
                    <>
                      <span style={{ fontWeight: 600, color: e.surrenderWinRate >= 50 ? 'var(--color-win)' : 'var(--color-loss)' }}>{e.surrenderWinRate.toFixed(1)}%</span>
                      <div style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>{e.surrenderGames}게임</div>
                    </>
                  ) : <span style={{ color: 'var(--color-text-disabled)' }}>-</span>}
                </td>
                <td style={{ padding: '7px 10px', textAlign: 'center', fontWeight: 700, color: e.comebackBonus > 0 ? 'var(--color-win)' : e.comebackBonus < 0 ? 'var(--color-loss)' : undefined }}>
                  {e.comebackBonus > 0 ? '+' : ''}{e.comebackBonus.toFixed(1)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
