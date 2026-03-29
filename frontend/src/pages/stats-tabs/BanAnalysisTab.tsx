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
      <div style={{ marginBottom: 8, fontSize: 12, color: 'var(--color-text-secondary)' }}>
        총 {data.totalGamesAnalyzed}게임 분석 · 가장 많이 밴된 챔피언: {data.mostBannedChampion ?? '-'}
      </div>
      <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
        <thead>
          <tr style={{ borderBottom: '1px solid var(--color-border)', color: 'var(--color-text-secondary)', fontSize: 11 }}>
            <th style={{ textAlign: 'left', padding: '6px 8px' }}>#</th>
            <th style={{ textAlign: 'left', padding: '6px 8px' }}>챔피언</th>
            <th style={{ textAlign: 'right', padding: '6px 8px' }}>밴 횟수</th>
            <th style={{ textAlign: 'right', padding: '6px 8px' }}>밴율</th>
          </tr>
        </thead>
        <tbody>
          {data.topBanned.map((e: BanEntry, i: number) => {
            const c = champions.get(e.championId);
            return (
              <tr key={e.champion} style={{ borderBottom: '1px solid var(--color-border)' }}>
                <td style={{ padding: '8px', color: 'var(--color-text-secondary)', fontSize: 11 }}>{i + 1}</td>
                <td style={{ padding: '8px', display: 'flex', alignItems: 'center', gap: 8 }}>
                  {c?.imageUrl && <img src={c.imageUrl} alt={c.nameKo} width={28} height={28} style={{ borderRadius: 4 }} />}
                  <ChampionLink champion={e.champion} championId={e.championId}>{c?.nameKo ?? e.champion}</ChampionLink>
                </td>
                <td style={{ padding: '8px', textAlign: 'right', fontWeight: 600 }}>{e.banCount}회</td>
                <td style={{ padding: '8px', textAlign: 'right', color: e.banRate >= 50 ? '#FF4757' : e.banRate >= 30 ? '#FF6B2B' : 'inherit' }}>
                  {e.banRate.toFixed(1)}%
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
