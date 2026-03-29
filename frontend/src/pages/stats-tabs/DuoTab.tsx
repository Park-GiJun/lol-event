import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../../lib/api/api';
import type { DuoStatsResult, DuoStat } from '../../lib/types/stats';
import { LoadingCenter } from '../../components/common/Spinner';
import { PlayerLink } from '../../components/common/PlayerLink';
import { RankBadge, WinRateBar } from './shared';

export default function DuoTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const [data, setData]       = useState<DuoStatsResult | null>(null);
  const [loading, setLoading] = useState(true);
  const [minGames, setMinGames] = useState(2);

  const load = useCallback(async () => {
    setLoading(true);
    try { setData(await api.get<DuoStatsResult>(`/stats/duo?mode=${mode}&minGames=${minGames}`)); }
    finally { setLoading(false); }
  }, [mode, minGames]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
        <p style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>
          같은 팀에서 함께 플레이한 멤버 조합의 시너지
        </p>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginLeft: 'auto' }}>
          <span style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>최소 게임수</span>
          {[2, 3, 5].map(n => (
            <button key={n} className={`member-sort-tab ${minGames === n ? 'active' : ''}`}
              onClick={() => setMinGames(n)}>{n}+</button>
          ))}
        </div>
      </div>
      <div className="table-wrapper">
        <table className="table member-stats-table">
          <thead>
            <tr>
              <th style={{ width: 36 }}>#</th>
              <th style={{ minWidth: 220 }}>플레이어 조합</th>
              <th className="table-number">게임</th>
              <th style={{ minWidth: 110 }}>승률</th>
              <th className="table-number">합산 KDA</th>
              <th className="table-number">합산 K/D/A</th>
            </tr>
          </thead>
          <tbody>
            {data.duos.map((d: DuoStat, i) => (
              <tr key={`${d.player1}-${d.player2}`} className="member-stats-row">
                <td><RankBadge rank={i + 1} /></td>
                <td>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <PlayerLink riotId={d.player1} mode={mode}>
                      <button className="player-name-btn" onClick={e => { e.stopPropagation(); navigate(`/player-stats/${encodeURIComponent(d.player1)}`); }}>
                        {d.player1.split('#')[0]}
                      </button>
                    </PlayerLink>
                    <span style={{ color: 'var(--color-text-disabled)', fontSize: 12 }}>+</span>
                    <PlayerLink riotId={d.player2} mode={mode}>
                      <button className="player-name-btn" onClick={e => { e.stopPropagation(); navigate(`/player-stats/${encodeURIComponent(d.player2)}`); }}>
                        {d.player2.split('#')[0]}
                      </button>
                    </PlayerLink>
                  </div>
                </td>
                <td className="table-number">{d.games}</td>
                <td><WinRateBar winRate={d.winRate} wins={d.wins} losses={d.games - d.wins} /></td>
                <td className="table-number" style={{ fontWeight: 700, color: d.kda >= 5 ? 'var(--color-win)' : d.kda >= 3 ? 'var(--color-primary)' : 'var(--color-text-primary)' }}>
                  {d.kda.toFixed(2)}
                </td>
                <td className="table-number" style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>
                  {d.avgKills.toFixed(1)} / <span style={{ color: 'var(--color-error)' }}>{d.avgDeaths.toFixed(1)}</span> / {d.avgAssists.toFixed(1)}
                </td>
              </tr>
            ))}
            {!data.duos.length && (
              <tr><td colSpan={6} style={{ textAlign: 'center', padding: '40px 0', color: 'var(--color-text-secondary)' }}>
                데이터 없음 (최소 {minGames}게임 이상 조합 없음)
              </td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
