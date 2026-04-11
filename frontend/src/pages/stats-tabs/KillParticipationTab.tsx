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
        <div style={{ padding: '10px 16px', borderRadius: 'var(--radius-md)', background: 'linear-gradient(135deg, rgba(0,180,216,0.08) 0%, var(--glass-bg) 100%)', border: '1px solid rgba(0,180,216,0.2)', marginBottom: 16, fontSize: 13 }}>
          ⚡ KP왕: <strong><PlayerLink riotId={data.kpKing}>{data.kpKing.split('#')[0]}</PlayerLink></strong>
        </div>
      )}
      <div className="table-wrapper">
        <table className="table member-stats-table" style={{ fontSize: 13 }}>
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
                <td style={{ padding: '8px 12px', textAlign: 'right', color: 'var(--color-text-secondary)', fontVariantNumeric: 'tabular-nums' }}>{p.games}</td>
                <td style={{ padding: '8px 12px', textAlign: 'right', fontWeight: 700, color: p.avgKp >= 70 ? 'var(--color-win)' : 'var(--color-primary)', fontVariantNumeric: 'tabular-nums' }}>{p.avgKp.toFixed(1)}%</td>
                <td style={{ padding: '8px 12px', textAlign: 'right', color: 'var(--color-win)', fontVariantNumeric: 'tabular-nums' }}>{p.avgKpWin.toFixed(1)}%</td>
                <td style={{ padding: '8px 12px', textAlign: 'right', color: 'var(--color-loss)', fontVariantNumeric: 'tabular-nums' }}>{p.avgKpLoss.toFixed(1)}%</td>
                <td style={{ padding: '8px 12px', textAlign: 'right', color: 'var(--color-text-secondary)', fontVariantNumeric: 'tabular-nums' }}>{p.avgKills.toFixed(1)} / {p.avgAssists.toFixed(1)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
