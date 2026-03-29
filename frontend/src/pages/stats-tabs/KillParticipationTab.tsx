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
  if (!data || data.rankings.length === 0) return <div style={{ padding: 24, color: 'var(--color-text-secondary)' }}>데이터 없음</div>;

  return (
    <div>
      {data.kpKing && (
        <div style={{ padding: '8px 12px', borderRadius: 8, background: 'var(--color-bg-hover)', marginBottom: 12, fontSize: 13 }}>
          ⚡ KP왕: <strong><PlayerLink riotId={data.kpKing}>{data.kpKing.split('#')[0]}</PlayerLink></strong>
        </div>
      )}
      <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
        <thead>
          <tr style={{ borderBottom: '1px solid var(--color-border)', color: 'var(--color-text-secondary)', fontSize: 11 }}>
            <th style={{ textAlign: 'left', padding: '6px 8px' }}>#</th>
            <th style={{ textAlign: 'left', padding: '6px 8px' }}>플레이어</th>
            <th style={{ textAlign: 'right', padding: '6px 8px' }}>게임</th>
            <th style={{ textAlign: 'right', padding: '6px 8px' }}>평균 KP</th>
            <th style={{ textAlign: 'right', padding: '6px 8px' }}>승리 KP</th>
            <th style={{ textAlign: 'right', padding: '6px 8px' }}>패배 KP</th>
            <th style={{ textAlign: 'right', padding: '6px 8px' }}>킬/어시</th>
          </tr>
        </thead>
        <tbody>
          {data.rankings.map((p: KillParticipationEntry, i: number) => (
            <tr key={p.riotId} style={{ borderBottom: '1px solid var(--color-border)' }}>
              <td style={{ padding: '8px', color: i < 3 ? 'var(--color-primary)' : 'var(--color-text-secondary)', fontWeight: i < 3 ? 700 : 400 }}>{i + 1}</td>
              <td style={{ padding: '8px' }}><PlayerLink riotId={p.riotId}>{p.riotId.split('#')[0]}</PlayerLink></td>
              <td style={{ padding: '8px', textAlign: 'right', color: 'var(--color-text-secondary)' }}>{p.games}</td>
              <td style={{ padding: '8px', textAlign: 'right', fontWeight: 700, color: p.avgKp >= 70 ? '#4CAF50' : 'inherit' }}>{p.avgKp.toFixed(1)}%</td>
              <td style={{ padding: '8px', textAlign: 'right', color: 'var(--color-win)' }}>{p.avgKpWin.toFixed(1)}%</td>
              <td style={{ padding: '8px', textAlign: 'right', color: 'var(--color-loss)' }}>{p.avgKpLoss.toFixed(1)}%</td>
              <td style={{ padding: '8px', textAlign: 'right', color: 'var(--color-text-secondary)' }}>{p.avgKills.toFixed(1)} / {p.avgAssists.toFixed(1)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
