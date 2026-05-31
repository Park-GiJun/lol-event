import { useQuery } from '@tanstack/react-query';
import { api } from '../../lib/api/api';
import type { KillParticipationResult, KillParticipationEntry } from '../../lib/types/stats';
import { LoadingCenter } from '../../components/common/Spinner';
import { PlayerLink } from '../../components/common/PlayerLink';

export default function KillParticipationTab({ mode }: { mode: string }) {
  const { data, isLoading } = useQuery({
    queryKey: ['kp-ranking', mode],
    queryFn: () => api.get<KillParticipationResult>(`/stats/kill-participation?mode=${mode}`),
  });
  if (isLoading) return <LoadingCenter />;
  if (!data || data.rankings.length === 0) return <div style={{ padding: 'var(--spacing-lg)', color: 'var(--color-text-secondary)' }}>데이터 없음</div>;

  return (
    <div>
      {data.kpKing && (
        <div className="card-glass" style={{ display: 'flex', alignItems: 'center', gap: 'var(--spacing-sm)', padding: 'var(--spacing-sm) var(--spacing-md)', marginBottom: 'var(--spacing-md)', fontSize: 'var(--font-size-sm)' }}>
          <span className="icon-chip">⚡</span>
          <span style={{ color: 'var(--color-text-secondary)' }}>
            KP왕 <strong style={{ color: 'var(--color-primary)' }}><PlayerLink riotId={data.kpKing}>{data.kpKing.split('#')[0]}</PlayerLink></strong>
          </span>
        </div>
      )}
      <div className="table-wrapper">
        <table className="table member-stats-table" style={{ fontSize: 'var(--font-size-sm)' }}>
          <thead>
            <tr>
              <th style={{ textAlign: 'left', padding: '8px 12px', width: 40 }}>#</th>
              <th style={{ textAlign: 'left', padding: '8px 12px' }}>플레이어</th>
              <th style={{ textAlign: 'right', padding: '8px 12px' }}>게임</th>
              <th style={{ textAlign: 'right', padding: '8px 12px' }}>평균 KP</th>
              <th style={{ textAlign: 'right', padding: '8px 12px' }}>승리 KP</th>
              <th style={{ textAlign: 'right', padding: '8px 12px' }}>패배 KP</th>
              <th style={{ textAlign: 'right', padding: '8px 12px' }}>킬/어시</th>
            </tr>
          </thead>
          <tbody>
            {data.rankings.map((p: KillParticipationEntry, i: number) => (
              <tr key={p.riotId} className="member-stats-row">
                <td style={{ padding: '8px 12px', color: i < 3 ? 'var(--color-primary)' : 'var(--color-text-secondary)', fontWeight: i < 3 ? 700 : 400 }}>{i + 1}</td>
                <td style={{ padding: '8px 12px' }}><PlayerLink riotId={p.riotId}><span style={{ fontWeight: 600 }}>{p.riotId.split('#')[0]}</span></PlayerLink></td>
                <td className="table-number" style={{ padding: '8px 12px', color: 'var(--color-text-secondary)' }}>{p.games}</td>
                <td className="table-number" style={{ padding: '8px 12px', fontWeight: 700, color: p.avgKp >= 70 ? 'var(--color-win)' : 'var(--color-primary)' }}>{p.avgKp.toFixed(1)}%</td>
                <td className="table-number" style={{ padding: '8px 12px', color: 'var(--color-win)' }}>{p.avgKpWin.toFixed(1)}%</td>
                <td className="table-number" style={{ padding: '8px 12px', color: 'var(--color-loss)' }}>{p.avgKpLoss.toFixed(1)}%</td>
                <td className="table-number" style={{ padding: '8px 12px', color: 'var(--color-text-secondary)' }}>{p.avgKills.toFixed(1)} / {p.avgAssists.toFixed(1)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
