import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../../lib/api/api';
import type { LaneLeaderboardResult, PlayerLaneStat } from '../../lib/types/stats';
import { LoadingCenter } from '../../components/common/Spinner';
import { useDragon } from '../../context/DragonContext';
import { PlayerLink } from '../../components/common/PlayerLink';
import { ChampionLink } from '../../components/common/ChampionLink';
import { RankBadge, ChampImg, WinRateBar } from './shared';

const LANE_META: Record<string, { label: string; emoji: string; keyCol: { key: keyof PlayerLaneStat; label: string; format: (v: number) => string } }> = {
  TOP:     { label: '탑',   emoji: '🛡️', keyCol: { key: 'avgDamageTaken',    label: '평균 받은딜',  format: v => v.toLocaleString() } },
  JUNGLE:  { label: '정글', emoji: '🌲', keyCol: { key: 'avgNeutralMinions', label: '중립 몬스터', format: v => v.toFixed(1) } },
  MID:     { label: '미드', emoji: '⚡', keyCol: { key: 'avgDamage',         label: '평균 딜량',   format: v => v.toLocaleString() } },
  BOTTOM:  { label: '원딜', emoji: '🏹', keyCol: { key: 'avgCs',             label: '평균 CS',    format: v => v.toFixed(1) } },
  SUPPORT: { label: '서폿', emoji: '💫', keyCol: { key: 'avgWardsPlaced',    label: '평균 와드',  format: v => v.toFixed(1) } },
};
const LANES = ['TOP', 'JUNGLE', 'MID', 'BOTTOM', 'SUPPORT'] as const;

export default function LaneTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { champions } = useDragon();
  const [selectedLane, setSelectedLane] = useState<string>('TOP');
  const [data, setData] = useState<LaneLeaderboardResult | null>(null);
  const [loading, setLoading] = useState(false);

  const load = useCallback(async (lane: string) => {
    setLoading(true);
    setData(null);
    try { setData(await api.get<LaneLeaderboardResult>(`/stats/lane?lane=${lane}&mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode]);

  useEffect(() => { load(selectedLane); }, [load, selectedLane]);

  const meta = LANE_META[selectedLane];

  return (
    <div>
      <div className="grid-16" style={{ marginBottom: 16 }}>
        {LANES.map(lane => {
          const m = LANE_META[lane];
          return (
            <button key={lane}
              className={`member-sort-tab col-span-3 ${selectedLane === lane ? 'active' : ''}`}
              onClick={() => setSelectedLane(lane)}
              style={{ display: 'flex', alignItems: 'center', gap: 4, padding: '6px 14px' }}>
              <span>{m.emoji}</span>
              <span>{m.label}</span>
            </button>
          );
        })}
      </div>

      {loading ? <LoadingCenter /> : !data ? null : (
        <div className="table-wrapper">
          <table className="table member-stats-table">
            <thead>
              <tr>
                <th style={{ width: 36 }}>#</th>
                <th>플레이어</th>
                <th>대표 챔피언</th>
                <th className="table-number">판수</th>
                <th style={{ minWidth: 110 }}>승률</th>
                <th className="table-number">KDA</th>
                <th className="table-number">K/D/A</th>
                <th className="table-number">평균 딜량</th>
                <th className="table-number">{meta.keyCol.label}</th>
              </tr>
            </thead>
            <tbody>
              {data.players.map((p: PlayerLaneStat, i) => {
                const champName = p.topChampion ?? '';
                const nameKo = p.topChampionId ? (champions.get(p.topChampionId)?.nameKo ?? champName) : champName;
                return (
                  <tr key={p.riotId} className="member-stats-row"
                    onClick={() => navigate(`/player-stats/${encodeURIComponent(p.riotId)}`)}>
                    <td><RankBadge rank={i + 1} /></td>
                    <td>
                      <PlayerLink riotId={p.riotId} mode={mode}>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                          <span style={{ fontWeight: 700, fontSize: 13 }}>{p.riotId.split('#')[0]}</span>
                          <span style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>#{p.riotId.split('#')[1]}</span>
                        </div>
                      </PlayerLink>
                    </td>
                    <td>
                      {p.topChampionId ? (
                        <ChampionLink champion={champName} championId={p.topChampionId} mode={mode}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                            <ChampImg championId={p.topChampionId} champion={champName} size={24} />
                            <span style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>{nameKo}</span>
                          </div>
                        </ChampionLink>
                      ) : <span style={{ color: 'var(--color-text-disabled)' }}>—</span>}
                    </td>
                    <td className="table-number">{p.games}</td>
                    <td><WinRateBar winRate={p.winRate} wins={p.wins} losses={p.games - p.wins} /></td>
                    <td className="table-number" style={{ fontWeight: 700, color: p.kda >= 5 ? 'var(--color-win)' : p.kda >= 3 ? 'var(--color-primary)' : undefined }}>
                      {p.kda.toFixed(2)}
                    </td>
                    <td className="table-number" style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>
                      {p.avgKills.toFixed(1)} / <span style={{ color: 'var(--color-error)' }}>{p.avgDeaths.toFixed(1)}</span> / {p.avgAssists.toFixed(1)}
                    </td>
                    <td className="table-number">{p.avgDamage.toLocaleString()}</td>
                    <td className="table-number">{meta.keyCol.format(p[meta.keyCol.key] as number)}</td>
                  </tr>
                );
              })}
              {data.players.length === 0 && (
                <tr><td colSpan={9} style={{ textAlign: 'center', padding: '40px 0', color: 'var(--color-text-secondary)' }}>데이터 없음</td></tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
