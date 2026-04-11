import { useQuery } from '@tanstack/react-query';
import { api } from '../../lib/api/api';
import type { BanAnalysisResult, BanEntry } from '../../lib/types/stats';
import { LoadingCenter } from '../../components/common/Spinner';
import { useDragon } from '../../context/DragonContext';
import { ChampionLink } from '../../components/common/ChampionLink';

export default function BanAnalysisTab({ mode }: { mode: string }) {
  const { champions } = useDragon();
  const { data, isLoading } = useQuery({
    queryKey: ['ban-analysis', mode],
    queryFn: () => api.get<BanAnalysisResult>(`/stats/ban-analysis?mode=${mode}`),
  });
  if (isLoading) return <LoadingCenter />;
  if (!data || data.topBanned.length === 0) return <div style={{ padding: 24, color: 'var(--color-text-secondary)' }}>밴 데이터가 없습니다</div>;

  return (
    <div>
      <div style={{ marginBottom: 12, fontSize: 12, color: 'var(--color-text-secondary)' }}>
        총 {data.totalGamesAnalyzed}게임 분석 · 가장 많이 밴된 챔피언: <span style={{ fontWeight: 700, color: 'var(--color-primary)' }}>{data.mostBannedChampion ?? '-'}</span>
      </div>
      <div className="table-wrapper">
        <table className="table member-stats-table" style={{ fontSize: 13 }}>
          <thead>
            <tr>
              <th style={{ textAlign: 'left', padding: '8px 12px', width: 40 }}>#</th>
              <th style={{ textAlign: 'left', padding: '8px 12px' }}>챔피언</th>
              <th style={{ textAlign: 'right', padding: '8px 12px' }}>밴 횟수</th>
              <th style={{ textAlign: 'right', padding: '8px 12px' }}>밴율</th>
            </tr>
          </thead>
          <tbody>
            {data.topBanned.map((e: BanEntry, i: number) => {
              const c = champions.get(e.championId);
              return (
                <tr key={e.champion} className="member-stats-row">
                  <td style={{ padding: '8px 12px', color: i < 3 ? 'var(--color-primary)' : 'var(--color-text-secondary)', fontSize: 12, fontWeight: i < 3 ? 700 : 400 }}>{i + 1}</td>
                  <td style={{ padding: '8px 12px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      {c?.imageUrl && <img src={c.imageUrl} alt={c.nameKo} width={28} height={28} style={{ borderRadius: 4, border: '1px solid var(--color-border)' }} />}
                      <ChampionLink champion={e.champion} championId={e.championId}><span style={{ fontWeight: 600 }}>{c?.nameKo ?? e.champion}</span></ChampionLink>
                    </div>
                  </td>
                  <td style={{ padding: '8px 12px', textAlign: 'right', fontWeight: 600, fontVariantNumeric: 'tabular-nums' }}>{e.banCount}회</td>
                  <td style={{ padding: '8px 12px', textAlign: 'right', fontWeight: 700, color: e.banRate >= 50 ? '#FF4757' : e.banRate >= 30 ? '#FF6B2B' : 'var(--color-text-secondary)', fontVariantNumeric: 'tabular-nums' }}>
                    {e.banRate.toFixed(1)}%
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
