import { useEffect, useState, useCallback } from 'react';
import { api } from '../../lib/api/api';
import type { GameLengthTendencyResult, GameLengthTendencyEntry } from '../../lib/types/stats';
import { LoadingCenter } from '../../components/common/Spinner';
import { PlayerLink } from '../../components/common/PlayerLink';

export default function GameLengthTab({ mode }: { mode: string }) {
  const [data, setData] = useState<GameLengthTendencyResult | null>(null);
  const [loading, setLoading] = useState(false);

  const load = useCallback(async () => {
    if (data) return;
    setLoading(true);
    try { setData(await api.get<GameLengthTendencyResult>(`/stats/game-length-tendency?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode, data]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  return (
    <div>
      <h3 style={{ fontSize: 13, fontWeight: 700, marginBottom: 12, paddingBottom: 8, borderBottom: '1px solid var(--glass-border)', color: 'var(--color-text-primary)' }}>플레이어별 게임 길이 성향</h3>
      <div className="table-wrapper">
        <table className="table member-stats-table" style={{ fontSize: 12 }}>
          <thead>
            <tr>
              <th style={{ padding: '8px 12px', textAlign: 'left' }}>플레이어</th>
              <th style={{ padding: '8px 12px', textAlign: 'center' }}>성향</th>
              <th style={{ padding: '8px 12px', textAlign: 'center' }}>단기전<br/><span style={{ fontSize: 10, fontWeight: 400, color: 'var(--color-text-disabled)' }}>(~20분)</span></th>
              <th style={{ padding: '8px 12px', textAlign: 'center' }}>중기전<br/><span style={{ fontSize: 10, fontWeight: 400, color: 'var(--color-text-disabled)' }}>(20~35분)</span></th>
              <th style={{ padding: '8px 12px', textAlign: 'center' }}>장기전<br/><span style={{ fontSize: 10, fontWeight: 400, color: 'var(--color-text-disabled)' }}>(35분+)</span></th>
            </tr>
          </thead>
          <tbody>
            {data.players.map((p: GameLengthTendencyEntry) => {
              const wrStyle = (wr: number) => ({
                color: wr >= 60 ? 'var(--color-win)' : wr >= 50 ? 'var(--color-primary)' : 'var(--color-loss)',
                fontWeight: 700 as const,
                fontVariantNumeric: 'tabular-nums' as const,
              });
              return (
                <tr key={p.riotId} className="member-stats-row">
                  <td style={{ padding: '8px 12px' }}>
                    <PlayerLink riotId={p.riotId} mode={mode}>
                      <span style={{ fontWeight: 700 }}>{p.riotId.split('#')[0]}</span>
                    </PlayerLink>
                    <div style={{ fontSize: 10, color: 'var(--color-text-disabled)', marginTop: 1 }}>{p.totalGames}게임</div>
                  </td>
                  <td style={{ padding: '8px 12px', textAlign: 'center' }}>
                    <span style={{ fontSize: 11, padding: '2px 8px', borderRadius: 10, background: 'rgba(0,180,216,0.12)', color: 'var(--color-primary)', fontWeight: 700 }}>{p.tendency}</span>
                  </td>
                  <td style={{ padding: '8px 12px', textAlign: 'center' }}>
                    {p.shortGame.games > 0 ? <span style={wrStyle(p.shortGame.winRate)}>{p.shortGame.winRate.toFixed(1)}%</span> : <span style={{ color: 'var(--color-text-disabled)' }}>-</span>}
                    {p.shortGame.games > 0 && <div style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>{p.shortGame.games}게임</div>}
                  </td>
                  <td style={{ padding: '8px 12px', textAlign: 'center' }}>
                    {p.midGame.games > 0 ? <span style={wrStyle(p.midGame.winRate)}>{p.midGame.winRate.toFixed(1)}%</span> : <span style={{ color: 'var(--color-text-disabled)' }}>-</span>}
                    {p.midGame.games > 0 && <div style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>{p.midGame.games}게임</div>}
                  </td>
                  <td style={{ padding: '8px 12px', textAlign: 'center' }}>
                    {p.longGame.games > 0 ? <span style={wrStyle(p.longGame.winRate)}>{p.longGame.winRate.toFixed(1)}%</span> : <span style={{ color: 'var(--color-text-disabled)' }}>-</span>}
                    {p.longGame.games > 0 && <div style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>{p.longGame.games}게임</div>}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
