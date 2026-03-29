import { useEffect, useState, useCallback } from 'react';
import { api } from '../../lib/api/api';
import type { ChampionSynergyResult, ChampionSynergy } from '../../lib/types/stats';
import { LoadingCenter } from '../../components/common/Spinner';
import { useDragon } from '../../context/DragonContext';
import { ChampionLink } from '../../components/common/ChampionLink';
import { RankBadge, ChampImg, WinRateBar } from './shared';

export default function SynergyTab({ mode }: { mode: string }) {
  const { champions } = useDragon();
  const [data, setData]       = useState<ChampionSynergyResult | null>(null);
  const [loading, setLoading] = useState(true);
  const [minGames, setMinGames] = useState(3);

  const load = useCallback(async () => {
    setLoading(true);
    try { setData(await api.get<ChampionSynergyResult>(`/stats/synergy?mode=${mode}&minGames=${minGames}`)); }
    finally { setLoading(false); }
  }, [mode, minGames]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
        <p style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>
          총 {data.totalGames}경기 · 같은 팀에 함께 픽된 챔피언 조합의 승률
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
              <th style={{ minWidth: 200 }}>챔피언 조합</th>
              <th className="table-number">게임</th>
              <th style={{ minWidth: 110 }}>승률</th>
              <th className="table-number">합산 킬/경기</th>
            </tr>
          </thead>
          <tbody>
            {data.synergies.map((s: ChampionSynergy, i) => {
              const ko1 = champions.get(s.champion1Id)?.nameKo || s.champion1;
              const ko2 = champions.get(s.champion2Id)?.nameKo || s.champion2;
              return (
              <tr key={`${s.champion1}-${s.champion2}`} className="member-stats-row">
                <td><RankBadge rank={i + 1} /></td>
                <td>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <ChampImg championId={s.champion1Id} champion={s.champion1} size={28} />
                    <ChampionLink champion={s.champion1} championId={s.champion1Id} mode={mode}>
                      <span style={{ fontWeight: 700, fontSize: 12 }}>{ko1}</span>
                    </ChampionLink>
                    <span style={{ color: 'var(--color-text-disabled)', fontSize: 16, fontWeight: 300 }}>+</span>
                    <ChampImg championId={s.champion2Id} champion={s.champion2} size={28} />
                    <ChampionLink champion={s.champion2} championId={s.champion2Id} mode={mode}>
                      <span style={{ fontWeight: 700, fontSize: 12 }}>{ko2}</span>
                    </ChampionLink>
                  </div>
                </td>
                <td className="table-number">{s.games}</td>
                <td><WinRateBar winRate={s.winRate} wins={s.wins} losses={s.games - s.wins} /></td>
                <td className="table-number" style={{ fontWeight: 600 }}>{s.avgCombinedKills.toFixed(1)}</td>
              </tr>
              );
            })}
            {!data.synergies.length && (
              <tr><td colSpan={5} style={{ textAlign: 'center', padding: '40px 0', color: 'var(--color-text-secondary)' }}>
                데이터 없음 (최소 {minGames}게임 이상 조합 없음)
              </td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
