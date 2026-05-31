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
  if (!data || data.topBanned.length === 0) return <div style={{ padding: 'var(--spacing-lg)', color: 'var(--color-text-secondary)' }}>밴 데이터가 없습니다</div>;

  return (
    <div>
      <div className="section-head" style={{ marginBottom: 'var(--spacing-md)' }}>
        <span className="icon-chip">🚫</span>
        <span className="section-head-title">밴 분석</span>
        <span className="section-head-action" style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)' }}>
          총 {data.totalGamesAnalyzed}게임 분석 · 최다 밴
          <span style={{ marginLeft: 'var(--spacing-xs)', fontWeight: 700, color: 'var(--color-primary)' }}>{data.mostBannedChampion ?? '-'}</span>
        </span>
      </div>
      <div className="table-wrapper">
        <table className="table member-stats-table" style={{ fontSize: 'var(--font-size-sm)' }}>
          <thead>
            <tr>
              <th style={{ textAlign: 'left', width: 40 }}>#</th>
              <th style={{ textAlign: 'left' }}>챔피언</th>
              <th style={{ textAlign: 'right' }}>밴 횟수</th>
              <th style={{ textAlign: 'right' }}>밴율</th>
            </tr>
          </thead>
          <tbody>
            {data.topBanned.map((e: BanEntry, i: number) => {
              const c = champions.get(e.championId);
              return (
                <tr key={e.champion} className="member-stats-row">
                  <td style={{ color: i < 3 ? 'var(--color-primary)' : 'var(--color-text-disabled)', fontSize: 'var(--font-size-xs)', fontWeight: i < 3 ? 700 : 400 }}>{i + 1}</td>
                  <td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--spacing-sm)' }}>
                      {c?.imageUrl && <img src={c.imageUrl} alt={c.nameKo} width={28} height={28} style={{ borderRadius: 'var(--radius-xs)', border: '1px solid var(--color-border)' }} />}
                      <ChampionLink champion={e.champion} championId={e.championId}><span style={{ fontWeight: 600 }}>{c?.nameKo ?? e.champion}</span></ChampionLink>
                    </div>
                  </td>
                  <td className="table-number" style={{ textAlign: 'right', fontWeight: 600 }}>{e.banCount}회</td>
                  <td className="table-number" style={{ textAlign: 'right', fontWeight: 700, color: e.banRate >= 50 ? 'var(--color-error)' : e.banRate >= 30 ? 'var(--color-warning)' : 'var(--color-text-secondary)' }}>
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
