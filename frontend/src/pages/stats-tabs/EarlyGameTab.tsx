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
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10, marginBottom: 20 }}>
        <div className="card" style={{ padding: 14, textAlign: 'center' }}>
          <div style={{ fontSize: 24, marginBottom: 4 }}>🗡️</div>
          <div style={{ fontSize: 11, color: 'var(--color-text-disabled)', marginBottom: 4 }}>퍼블킹</div>
          <div style={{ fontWeight: 700, fontSize: 15, color: '#FFD700' }}>{data.firstBloodKing?.split('#')[0] ?? '-'}</div>
          <div style={{ fontSize: 10, color: 'var(--color-text-secondary)', marginTop: 4 }}>
            전체 퍼블 승률: {data.overallFirstBloodWinRate.toFixed(1)}%
          </div>
        </div>
        <div className="card" style={{ padding: 14, textAlign: 'center' }}>
          <div style={{ fontSize: 24, marginBottom: 4 }}>🏯</div>
          <div style={{ fontSize: 11, color: 'var(--color-text-disabled)', marginBottom: 4 }}>포탑 파괴자</div>
          <div style={{ fontWeight: 700, fontSize: 15, color: '#60a5fa' }}>{data.towerDestroyer?.split('#')[0] ?? '-'}</div>
          <div style={{ fontSize: 10, color: 'var(--color-text-secondary)', marginTop: 4 }}>
            전체 퍼타 승률: {data.overallFirstTowerWinRate.toFixed(1)}%
          </div>
        </div>
      </div>

      <h3 style={{ fontSize: 13, fontWeight: 700, marginBottom: 10 }}>초반 지배력 랭킹</h3>
      <div style={{ overflowX: 'auto' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
          <thead>
            <tr style={{ background: 'var(--color-bg-hover)' }}>
              <th style={{ padding: '7px 10px', textAlign: 'left' }}>플레이어</th>
              <th style={{ padding: '7px 10px', textAlign: 'center' }}>초반점수</th>
              <th style={{ padding: '7px 10px', textAlign: 'center' }}>퍼블%</th>
              <th style={{ padding: '7px 10px', textAlign: 'center' }}>퍼타%</th>
              <th style={{ padding: '7px 10px', textAlign: 'left' }}>뱃지</th>
            </tr>
          </thead>
          <tbody>
            {data.rankings.map((e: EarlyGameDominanceEntry, i: number) => (
              <tr key={e.riotId} style={{ borderTop: '1px solid var(--color-border)' }}>
                <td style={{ padding: '7px 10px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                    <RankBadge rank={i + 1} />
                    <PlayerLink riotId={e.riotId} mode={mode}>
                      <span style={{ fontWeight: 600 }}>{e.riotId.split('#')[0]}</span>
                    </PlayerLink>
                  </div>
                  <div style={{ fontSize: 10, color: 'var(--color-text-disabled)', paddingLeft: 32 }}>{e.games}게임</div>
                </td>
                <td style={{ padding: '7px 10px', textAlign: 'center', fontWeight: 700, color: 'var(--color-primary)' }}>
                  {e.earlyGameScore.toFixed(1)}
                </td>
                <td style={{ padding: '7px 10px', textAlign: 'center', color: e.firstBloodRate >= 0.3 ? 'var(--color-win)' : undefined }}>
                  {(e.firstBloodRate * 100).toFixed(1)}%
                </td>
                <td style={{ padding: '7px 10px', textAlign: 'center', color: e.firstTowerRate >= 0.3 ? 'var(--color-win)' : undefined }}>
                  {(e.firstTowerRate * 100).toFixed(1)}%
                </td>
                <td style={{ padding: '7px 10px' }}>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: 3 }}>
                    {e.badges.map((b: string) => (
                      <span key={b} style={{ fontSize: 10, padding: '1px 5px', borderRadius: 8, background: 'var(--color-primary)22', color: 'var(--color-primary)' }}>{b}</span>
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
