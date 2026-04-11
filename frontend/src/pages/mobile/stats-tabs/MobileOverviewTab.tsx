import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../../lib/api/api';
import type { OverviewStats } from '../../../lib/types/stats';
import { useDragon } from '../../../context/DragonContext';
import { LoadingCenter } from '../../../components/common/Spinner';

export default function MobileOverviewTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { champions } = useDragon();
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-overview', mode],
    queryFn: () => api.get<OverviewStats>(`/stats/overview?mode=${mode}`),
  });

  if (isLoading) return <LoadingCenter />;
  if (!data) return <div className="m-empty">데이터 없음</div>;

  const leaders = [
    { label: '승률왕', stat: data.winRateLeader },
    { label: 'KDA왕', stat: data.kdaLeader },
    { label: '킬왕', stat: data.killsLeader },
    { label: '딜왕', stat: data.damageLeader },
  ].filter(l => l.stat);

  return (
    <div>
      <div className="m-overview-grid" style={{ marginBottom: 12 }}>
        <div className="m-overview-stat">
          <div className="m-overview-stat-value">{data.matchCount}</div>
          <div className="m-overview-stat-label">총 경기</div>
        </div>
        <div className="m-overview-stat">
          <div className="m-overview-stat-value">{data.avgGameMinutes.toFixed(1)}<span style={{ fontSize: 12 }}>분</span></div>
          <div className="m-overview-stat-label">평균 시간</div>
        </div>
      </div>

      {data.topPickedChampions.length > 0 && (
        <>
          <p className="m-section-title">인기 챔피언</p>
          <div style={{ display: 'flex', gap: 10, overflowX: 'auto', paddingBottom: 8, marginBottom: 12, scrollbarWidth: 'none' }}>
            {data.topPickedChampions.slice(0, 10).map(cp => {
              const c = Array.from(champions.values()).find(ch => ch.championKey === cp.champion || ch.nameKo === cp.champion);
              return (
                <div key={cp.champion}
                  style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4, cursor: 'pointer', flexShrink: 0 }}
                  onClick={() => navigate(`/m/champion/${encodeURIComponent(c?.championKey ?? cp.champion)}`)}
                >
                  {c?.imageUrl ? (
                    <img src={c.imageUrl} alt={c.nameKo} width={48} height={48} style={{ borderRadius: 8, objectFit: 'cover' }} />
                  ) : (
                    <div style={{ width: 48, height: 48, borderRadius: 8, background: 'var(--color-bg-hover)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 11 }}>
                      {cp.champion.slice(0, 2)}
                    </div>
                  )}
                  <span style={{ fontSize: 10, color: 'var(--color-text-secondary)' }}>{cp.picks}픽</span>
                </div>
              );
            })}
          </div>
        </>
      )}

      {leaders.length > 0 && (
        <>
          <p className="m-section-title">명예의 전당</p>
          <div className="m-card">
            {leaders.map(({ label, stat }) => stat && (
              <div key={label} className="m-leader-row"
                onClick={() => navigate(`/m/player/${encodeURIComponent(stat.riotId)}`)}>
                <span style={{ fontSize: 12, color: 'var(--color-text-secondary)', width: 60 }}>{label}</span>
                <span style={{ fontWeight: 700, flex: 1 }}>{stat.riotId.split('#')[0]}</span>
                <span style={{ fontSize: 13, fontWeight: 700, color: 'var(--color-primary)' }}>{stat.displayValue}</span>
              </div>
            ))}
          </div>
        </>
      )}

      <p className="m-section-title" style={{ marginTop: 12 }}>오브젝트</p>
      <div className="grid-16">
        {[
          { label: '바론', value: data.totalBaronKills },
          { label: '드래곤', value: data.totalDragonKills },
          { label: '포탑', value: data.totalTowerKills },
          { label: '전령', value: data.totalRiftHeraldKills },
          { label: '억제기', value: data.totalInhibitorKills },
          { label: '퍼블', value: data.totalFirstBloods },
        ].map(({ label, value }) => (
          <div key={label} className="m-overview-stat col-span-5">
            <div className="m-overview-stat-value" style={{ fontSize: 18 }}>{value.toLocaleString()}</div>
            <div className="m-overview-stat-label">{label}</div>
          </div>
        ))}
      </div>
    </div>
  );
}
