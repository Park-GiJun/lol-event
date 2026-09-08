import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../../lib/api/api';
import type { JungleDominanceResult, JungleDominanceEntry } from '../../lib/types/stats';
import { LoadingCenter } from '../../components/common/Spinner';
import { useDragon } from '../../context/DragonContext';
import { RankBadge, ChampImg } from './shared';

export default function JungleTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { champions } = useDragon();
  const [data, setData]       = useState<JungleDominanceResult | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try { setData(await api.get<JungleDominanceResult>(`/stats/jungle-dominance?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  /**
   * 적 정글 침입은 match_participants.neutral_minions_killed_enemy_jungle 로 낸다.
   * 그런데 이 컬럼은 LCU 전적 응답에서 안 실려 오는 경우가 있고, 그러면 전원이 0.0% 로
   * 나란히 서서 아무 의미도 없는 칸이 된다. 값이 하나라도 있을 때만 보여준다.
   * 나중에 채워지기 시작하면 자동으로 다시 뜬다.
   */
  const hasInvadeData = data.rankings.some(p => p.avgInvadeRatio > 0);
  const columnCount = hasInvadeData ? 8 : 7;

  return (
    <div>
      <div className="table-wrapper">
        <table className="table member-stats-table">
          <thead>
            <tr>
              <th style={{ width: 36 }}>#</th>
              <th>플레이어</th>
              <th>스타일</th>
              <th className="table-number">판수</th>
              {hasInvadeData && <th className="table-number">적 정글 침입</th>}
              <th className="table-number">오브젝트 딜 비중</th>
              <th className="table-number">킬 관여</th>
              <th className="table-number">정글 장악 점수</th>
            </tr>
          </thead>
          <tbody>
            {data.rankings.map((p: JungleDominanceEntry, i) => (
              <tr key={p.riotId} className="member-stats-row"
                onClick={() => navigate(`/player-stats/${encodeURIComponent(p.riotId)}`)}>
                <td><RankBadge rank={i + 1} /></td>
                <td>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                    <span style={{ fontWeight: 'var(--font-weight-bold)', fontSize: 'var(--font-size-sm)' }}>{p.riotId.split('#')[0]}</span>
                    {p.topChampion && p.topChampionId && (
                      <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                        <ChampImg championId={p.topChampionId} champion={p.topChampion} size={16} />
                        <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-disabled)' }}>
                          {champions.get(p.topChampionId)?.nameKo ?? p.topChampion}
                        </span>
                      </div>
                    )}
                  </div>
                </td>
                <td>
                  <span className="badge badge-primary badge-sm">{p.playStyleTag}</span>
                </td>
                <td className="table-number">{p.games}</td>
                {hasInvadeData && <td className="table-number">{(p.avgInvadeRatio * 100).toFixed(1)}%</td>}
                <td className="table-number">{(p.avgObjShare * 100).toFixed(1)}%</td>
                <td className="table-number">{(p.avgKp * 100).toFixed(1)}%</td>
                <td className="table-number" style={{ fontWeight: 700, color: 'var(--color-primary)' }}>
                  {p.avgJungleDominance.toFixed(1)}
                </td>
              </tr>
            ))}
            {!data.rankings.length && (
              <tr><td colSpan={columnCount} style={{ textAlign: 'center', padding: 'var(--spacing-2xl) 0', color: 'var(--color-text-secondary)' }}>데이터 없음</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
