import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../../lib/api/api';
import type { MvpStatsResult, MvpPlayerStat } from '../../lib/types/stats';
import { LoadingCenter } from '../../components/common/Spinner';
import { useDragon } from '../../context/DragonContext';
import { PlayerLink } from '../../components/common/PlayerLink';
import { ChampionLink } from '../../components/common/ChampionLink';
import { RankBadge, ChampImg } from './shared';

export default function MvpTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { champions } = useDragon();
  const [data, setData] = useState<MvpStatsResult | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try { setData(await api.get<MvpStatsResult>(`/stats/mvp?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  return (
    <div>
      <p style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginBottom: 16 }}>
        총 {data.totalGames}경기 · MVP 점수 = KDA기여 + 팀데미지기여(최대40) + 시야/분 + CS/분 + 승리보너스(+20)
      </p>
      <div className="table-wrapper">
        <table className="table member-stats-table">
          <thead>
            <tr>
              <th style={{ width: 36 }}>#</th>
              <th>플레이어</th>
              <th className="table-number">판수</th>
              <th className="table-number">MVP 횟수</th>
              <th className="table-number">ACE 횟수</th>
              <th style={{ minWidth: 100 }}>MVP 달성률</th>
              <th className="table-number">평균 점수</th>
              <th>MVP 챔피언</th>
            </tr>
          </thead>
          <tbody>
            {data.rankings.map((p: MvpPlayerStat, i) => (
              <tr key={p.riotId} className="member-stats-row" onClick={() => navigate(`/player-stats/${encodeURIComponent(p.riotId)}`)}>
                <td><RankBadge rank={i + 1} /></td>
                <td>
                  <PlayerLink riotId={p.riotId} mode={mode}>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                      <span style={{ fontWeight: 700, fontSize: 13 }}>{p.riotId.split('#')[0]}</span>
                      <span style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>#{p.riotId.split('#')[1]}</span>
                    </div>
                  </PlayerLink>
                </td>
                <td className="table-number">{p.games}</td>
                <td className="table-number">
                  <span style={{ fontWeight: 700, color: 'var(--color-primary)', fontSize: 14 }}>{p.mvpCount}</span>
                  <span style={{ fontSize: 10, color: 'var(--color-text-disabled)', marginLeft: 2 }}>회</span>
                </td>
                <td className="table-number">
                  <span style={{ fontWeight: 700, color: 'var(--color-win)', fontSize: 14 }}>{p.aceCount}</span>
                  <span style={{ fontSize: 10, color: 'var(--color-text-disabled)', marginLeft: 2 }}>회</span>
                </td>
                <td>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
                    <span style={{ fontWeight: 700, fontSize: 13, color: p.mvpRate >= 50 ? 'var(--color-win)' : 'var(--color-primary)' }}>{p.mvpRate}%</span>
                    <div style={{ height: 4, background: 'var(--color-bg-hover)', borderRadius: 2, overflow: 'hidden', minWidth: 80 }}>
                      <div style={{ width: `${Math.min(p.mvpRate, 100)}%`, height: '100%', background: p.mvpRate >= 50 ? 'var(--color-win)' : 'var(--color-primary)', borderRadius: 2 }} />
                    </div>
                  </div>
                </td>
                <td className="table-number" style={{ fontWeight: 700 }}>{p.avgMvpScore.toFixed(2)}</td>
                <td>
                  {p.topChampion && p.topChampionId ? (
                    <ChampionLink champion={p.topChampion} championId={p.topChampionId} mode={mode}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                        <ChampImg championId={p.topChampionId} champion={p.topChampion} size={24} />
                        <span style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>
                          {champions.get(p.topChampionId)?.nameKo ?? p.topChampion}
                        </span>
                      </div>
                    </ChampionLink>
                  ) : <span style={{ color: 'var(--color-text-disabled)' }}>—</span>}
                </td>
              </tr>
            ))}
            {!data.rankings.length && (
              <tr><td colSpan={8} style={{ textAlign: 'center', padding: '40px 0', color: 'var(--color-text-secondary)' }}>데이터 없음</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
