import { useQuery } from '@tanstack/react-query';
import { api } from '../../../lib/api/api';
import type { ChampionSynergyResult } from '../../../lib/types/stats';
import { useDragon } from '../../../context/DragonContext';
import { LoadingCenter } from '../../../components/common/Spinner';
import { ChampImg } from '../../stats-tabs/shared';

export default function MobileSynergyTab({ mode }: { mode: string }) {
  const { champions } = useDragon();
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-synergy', mode],
    queryFn: () => api.get<ChampionSynergyResult>(`/stats/synergy?mode=${mode}&minGames=3`),
  });

  if (isLoading) return <LoadingCenter />;
  if (!data) return <div className="m-empty">데이터 없음</div>;

  return (
    <div>
      {data.synergies.map((s, i) => {
        const c1 = champions.get(s.champion1Id);
        const c2 = champions.get(s.champion2Id);
        return (
          <div key={i} className="m-synergy-card">
            <div style={{ display: 'flex', alignItems: 'center', gap: 4, flexShrink: 0 }}>
              <ChampImg championId={s.champion1Id} champion={s.champion1} size={36} style={{ borderRadius: 6, border: 'none' }} />
              <span style={{ fontSize: 11, color: 'var(--color-text-disabled)' }}>+</span>
              <ChampImg championId={s.champion2Id} champion={s.champion2} size={36} style={{ borderRadius: 6, border: 'none' }} />
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 12, fontWeight: 600 }}>
                {c1?.nameKo ?? s.champion1} + {c2?.nameKo ?? s.champion2}
              </div>
              <div className="m-stat-chips" style={{ marginTop: 4 }}>
                <span className="m-stat-chip">{s.games}게임</span>
                <span className="m-stat-chip" style={{ color: s.winRate >= 60 ? 'var(--color-win)' : 'inherit' }}>
                  {s.winRate.toFixed(1)}%
                </span>
              </div>
            </div>
          </div>
        );
      })}
      {data.synergies.length === 0 && <div className="m-empty">시너지 데이터가 없습니다</div>}
    </div>
  );
}
