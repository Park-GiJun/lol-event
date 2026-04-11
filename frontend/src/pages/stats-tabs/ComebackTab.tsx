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
          <div className="card" style={{ padding: '18px', textAlign: 'center', background: 'linear-gradient(135deg, rgba(16,185,129,0.06) 0%, var(--glass-bg) 100%)', borderColor: 'rgba(16,185,129,0.2)', boxShadow: '0 0 20px rgba(16,185,129,0.06)' }}>
            <div style={{ fontSize: 28, marginBottom: 6, filter: 'drop-shadow(0 0 6px rgba(16,185,129,0.3))' }}>🔄</div>
            <div style={{ fontSize: 10, fontWeight: 700, color: 'var(--color-text-secondary)', letterSpacing: 'var(--tracking-wider)', textTransform: 'uppercase', marginBottom: 6 }}>컴백킹</div>
            <div style={{ fontWeight: 800, fontSize: 18, color: '#FFD700', textShadow: '0 0 12px rgba(255,215,0,0.3)' }}>
              {data.comebackKing ? data.comebackKing.split('#')[0] : kings[0]?.riotId.split('#')[0] ?? '-'}
            </div>
            {kings[0] && (
              <div style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginTop: 8, fontVariantNumeric: 'tabular-nums' }}>
                접전 승률 <span style={{ fontWeight: 700, color: 'var(--color-win)' }}>{kings[0].contestWinRate.toFixed(1)}%</span> &middot; 접전 {kings[0].contestGames}게임
              </div>
            )}
          </div>
        </div>
      )}

      <h3 style={{ fontSize: 13, fontWeight: 700, marginBottom: 10, paddingBottom: 8, borderBottom: '1px solid var(--glass-border)', color: 'var(--color-text-primary)' }}>접전 vs 압도 경기 승률</h3>
      <div className="table-wrapper">
        <table className="table member-stats-table" style={{ fontSize: 12 }}>
          <thead>
            <tr>
              <th style={{ padding: '8px 12px', textAlign: 'left' }}>플레이어</th>
              <th style={{ padding: '8px 12px', textAlign: 'center' }}>전체 승률</th>
              <th style={{ padding: '8px 12px', textAlign: 'center' }}>접전 승률<br/><span style={{ fontSize: 10, fontWeight: 400, color: 'var(--color-text-disabled)' }}>(항복경기 제외)</span></th>
              <th style={{ padding: '8px 12px', textAlign: 'center' }}>압도 승률<br/><span style={{ fontSize: 10, fontWeight: 400, color: 'var(--color-text-disabled)' }}>(항복경기)</span></th>
              <th style={{ padding: '8px 12px', textAlign: 'center' }}>컴백보너스</th>
            </tr>
          </thead>
          <tbody>
            {data.rankings.map((e: ComebackIndexEntry, i: number) => (
              <tr key={e.riotId} className="member-stats-row" style={{ background: e.isKing ? 'rgba(16,185,129,0.05)' : undefined }}>
                <td style={{ padding: '8px 12px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                    <RankBadge rank={i + 1} />
                    <PlayerLink riotId={e.riotId} mode={mode}>
                      <span style={{ fontWeight: 700 }}>{e.riotId.split('#')[0]}</span>
                    </PlayerLink>
                    {e.isKing && <span style={{ fontSize: 12 }}>👑</span>}
                  </div>
                  <div style={{ fontSize: 10, color: 'var(--color-text-disabled)', paddingLeft: 32, marginTop: 1 }}>{e.totalGames}게임</div>
                </td>
                <td style={{ padding: '8px 12px', textAlign: 'center', color: e.totalWinRate >= 50 ? 'var(--color-win)' : 'var(--color-loss)', fontWeight: 700, fontVariantNumeric: 'tabular-nums' }}>
                  {e.totalWinRate.toFixed(1)}%
                </td>
                <td style={{ padding: '8px 12px', textAlign: 'center' }}>
                  {e.contestGames > 0 ? (
                    <>
                      <span style={{ fontWeight: 700, color: e.contestWinRate >= 50 ? 'var(--color-win)' : 'var(--color-loss)', fontVariantNumeric: 'tabular-nums' }}>{e.contestWinRate.toFixed(1)}%</span>
                      <div style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>{e.contestGames}게임</div>
                    </>
                  ) : <span style={{ color: 'var(--color-text-disabled)' }}>-</span>}
                </td>
                <td style={{ padding: '8px 12px', textAlign: 'center' }}>
                  {e.surrenderGames > 0 ? (
                    <>
                      <span style={{ fontWeight: 700, color: e.surrenderWinRate >= 50 ? 'var(--color-win)' : 'var(--color-loss)', fontVariantNumeric: 'tabular-nums' }}>{e.surrenderWinRate.toFixed(1)}%</span>
                      <div style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>{e.surrenderGames}게임</div>
                    </>
                  ) : <span style={{ color: 'var(--color-text-disabled)' }}>-</span>}
                </td>
                <td style={{ padding: '8px 12px', textAlign: 'center', fontWeight: 800, color: e.comebackBonus > 0 ? 'var(--color-win)' : e.comebackBonus < 0 ? 'var(--color-loss)' : 'var(--color-text-secondary)', fontVariantNumeric: 'tabular-nums' }}>
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
