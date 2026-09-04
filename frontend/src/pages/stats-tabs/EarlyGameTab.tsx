import { SwordsIcon } from '@/components/icons/LolIcons';
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
      <div className="grid-16" style={{ marginBottom: 'var(--spacing-lg)' }}>
        <div className="stat-card col-span-8" style={{ alignItems: 'center', textAlign: 'center' }}>
          <div style={{ fontSize: 28, marginBottom: 'var(--spacing-xs)', filter: 'drop-shadow(0 0 6px rgba(200,170,110,0.3))' }}></div>
          <div className="stat-card-label">퍼블킹</div>
          <div style={{ fontWeight: 800, fontSize: 'var(--font-size-md)', color: 'var(--color-primary)' }}>{data.firstBloodKing?.split('#')[0] ?? '-'}</div>
          <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)', marginTop: 'var(--spacing-xs)', fontVariantNumeric: 'tabular-nums' }}>
            전체 퍼블 승률: <span style={{ fontWeight: 700, color: 'var(--color-primary)' }}>{data.overallFirstBloodWinRate.toFixed(1)}%</span>
          </div>
        </div>
        <div className="stat-card col-span-8" style={{ alignItems: 'center', textAlign: 'center' }}>
          <div style={{ fontSize: 28, marginBottom: 'var(--spacing-xs)', filter: 'drop-shadow(0 0 6px rgba(200,170,110,0.3))' }}></div>
          <div className="stat-card-label">포탑 파괴자</div>
          <div style={{ fontWeight: 800, fontSize: 'var(--font-size-md)', color: 'var(--color-primary)' }}>{data.towerDestroyer?.split('#')[0] ?? '-'}</div>
          <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)', marginTop: 'var(--spacing-xs)', fontVariantNumeric: 'tabular-nums' }}>
            전체 퍼타 승률: <span style={{ fontWeight: 700, color: 'var(--color-primary)' }}>{data.overallFirstTowerWinRate.toFixed(1)}%</span>
          </div>
        </div>
      </div>

      <div className="section-head">
        <SwordsIcon size={16} />
        <span className="section-head-title">초반 지배력 랭킹</span>
      </div>
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
                <td className="table-number" style={{ padding: '8px 12px', textAlign: 'center', fontWeight: 800, color: 'var(--color-primary)' }}>
                  {e.earlyGameScore.toFixed(1)}
                </td>
                <td className="table-number" style={{ padding: '8px 12px', textAlign: 'center', fontWeight: e.firstBloodRate >= 0.3 ? 700 : 400, color: e.firstBloodRate >= 0.3 ? 'var(--color-win)' : 'var(--color-text-secondary)' }}>
                  {(e.firstBloodRate * 100).toFixed(1)}%
                </td>
                <td className="table-number" style={{ padding: '8px 12px', textAlign: 'center', fontWeight: e.firstTowerRate >= 0.3 ? 700 : 400, color: e.firstTowerRate >= 0.3 ? 'var(--color-win)' : 'var(--color-text-secondary)' }}>
                  {(e.firstTowerRate * 100).toFixed(1)}%
                </td>
                <td style={{ padding: '8px 12px' }}>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: 'var(--spacing-xs)' }}>
                    {e.badges.map((b: string) => (
                      <span key={b} className="badge badge-primary badge-sm">{b}</span>
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
