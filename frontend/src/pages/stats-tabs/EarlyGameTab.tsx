import { useEffect, useState, useCallback } from 'react';
import { api } from '../../lib/api/api';
import type { EarlyGameDominanceResult, EarlyGameDominanceEntry } from '../../lib/types/stats';
import { LoadingCenter } from '../../components/common/Spinner';
import { PlayerLink } from '../../components/common/PlayerLink';
import { RankBadge } from './shared';

export default function EarlyGameTab({ mode }: { mode: string }) {
  const [data, setData] = useState<EarlyGameDominanceResult | null>(null);
  const [loading, setLoading] = useState(false);

  const load = useCallback(async () => {
    if (data) return;
    setLoading(true);
    try { setData(await api.get<EarlyGameDominanceResult>(`/stats/early-game?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode, data]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  return (
    <div>
      <div className="grid-16" style={{ marginBottom: 20 }}>
        <div className="card col-span-8" style={{ padding: '16px', textAlign: 'center', background: 'linear-gradient(135deg, rgba(255,215,0,0.06) 0%, var(--glass-bg) 100%)', borderColor: 'rgba(255,215,0,0.2)' }}>
          <div style={{ fontSize: 28, marginBottom: 6, filter: 'drop-shadow(0 0 6px rgba(255,215,0,0.3))' }}>🗡️</div>
          <div style={{ fontSize: 10, fontWeight: 700, color: 'var(--color-text-secondary)', letterSpacing: 'var(--tracking-wider)', textTransform: 'uppercase', marginBottom: 6 }}>퍼블킹</div>
          <div style={{ fontWeight: 800, fontSize: 16, color: '#FFD700' }}>{data.firstBloodKing?.split('#')[0] ?? '-'}</div>
          <div style={{ fontSize: 11, color: 'var(--color-text-secondary)', marginTop: 6, fontVariantNumeric: 'tabular-nums' }}>
            전체 퍼블 승률: <span style={{ fontWeight: 700, color: 'var(--color-primary)' }}>{data.overallFirstBloodWinRate.toFixed(1)}%</span>
          </div>
        </div>
        <div className="card col-span-8" style={{ padding: '16px', textAlign: 'center', background: 'linear-gradient(135deg, rgba(96,165,250,0.06) 0%, var(--glass-bg) 100%)', borderColor: 'rgba(96,165,250,0.2)' }}>
          <div style={{ fontSize: 28, marginBottom: 6, filter: 'drop-shadow(0 0 6px rgba(96,165,250,0.3))' }}>🏯</div>
          <div style={{ fontSize: 10, fontWeight: 700, color: 'var(--color-text-secondary)', letterSpacing: 'var(--tracking-wider)', textTransform: 'uppercase', marginBottom: 6 }}>포탑 파괴자</div>
          <div style={{ fontWeight: 800, fontSize: 16, color: '#60a5fa' }}>{data.towerDestroyer?.split('#')[0] ?? '-'}</div>
          <div style={{ fontSize: 11, color: 'var(--color-text-secondary)', marginTop: 6, fontVariantNumeric: 'tabular-nums' }}>
            전체 퍼타 승률: <span style={{ fontWeight: 700, color: '#60a5fa' }}>{data.overallFirstTowerWinRate.toFixed(1)}%</span>
          </div>
        </div>
      </div>

      <h3 style={{ fontSize: 13, fontWeight: 700, marginBottom: 10, paddingBottom: 8, borderBottom: '1px solid var(--glass-border)', color: 'var(--color-text-primary)' }}>초반 지배력 랭킹</h3>
      <div className="table-wrapper">
        <table className="table member-stats-table" style={{ fontSize: 12 }}>
          <thead>
            <tr>
              <th style={{ padding: '8px 12px', textAlign: 'left' }}>플레이어</th>
              <th style={{ padding: '8px 12px', textAlign: 'center' }}>초반점수</th>
              <th style={{ padding: '8px 12px', textAlign: 'center' }}>퍼블%</th>
              <th style={{ padding: '8px 12px', textAlign: 'center' }}>퍼타%</th>
              <th style={{ padding: '8px 12px', textAlign: 'left' }}>뱃지</th>
            </tr>
          </thead>
          <tbody>
            {data.rankings.map((e: EarlyGameDominanceEntry, i: number) => (
              <tr key={e.riotId} className="member-stats-row">
                <td style={{ padding: '8px 12px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                    <RankBadge rank={i + 1} />
                    <PlayerLink riotId={e.riotId} mode={mode}>
                      <span style={{ fontWeight: 700 }}>{e.riotId.split('#')[0]}</span>
                    </PlayerLink>
                  </div>
                  <div style={{ fontSize: 10, color: 'var(--color-text-disabled)', paddingLeft: 32, marginTop: 1 }}>{e.games}게임</div>
                </td>
                <td style={{ padding: '8px 12px', textAlign: 'center', fontWeight: 800, color: 'var(--color-primary)', fontVariantNumeric: 'tabular-nums' }}>
                  {e.earlyGameScore.toFixed(1)}
                </td>
                <td style={{ padding: '8px 12px', textAlign: 'center', fontWeight: e.firstBloodRate >= 0.3 ? 700 : 400, color: e.firstBloodRate >= 0.3 ? 'var(--color-win)' : 'var(--color-text-secondary)', fontVariantNumeric: 'tabular-nums' }}>
                  {(e.firstBloodRate * 100).toFixed(1)}%
                </td>
                <td style={{ padding: '8px 12px', textAlign: 'center', fontWeight: e.firstTowerRate >= 0.3 ? 700 : 400, color: e.firstTowerRate >= 0.3 ? 'var(--color-win)' : 'var(--color-text-secondary)', fontVariantNumeric: 'tabular-nums' }}>
                  {(e.firstTowerRate * 100).toFixed(1)}%
                </td>
                <td style={{ padding: '8px 12px' }}>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: 3 }}>
                    {e.badges.map((b: string) => (
                      <span key={b} style={{ fontSize: 10, padding: '2px 6px', borderRadius: 10, background: 'rgba(0,180,216,0.12)', color: 'var(--color-primary)', fontWeight: 600 }}>{b}</span>
                    ))}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
