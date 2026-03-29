import { useEffect, useState, useCallback } from 'react';
import { api } from '../../lib/api/api';
import type { SessionReportResult, SessionEntry } from '../../lib/types/stats';
import { LoadingCenter } from '../../components/common/Spinner';

export default function SessionsTab({ mode }: { mode: string }) {
  const [data, setData] = useState<SessionReportResult | null>(null);
  const [loading, setLoading] = useState(false);

  const load = useCallback(async () => {
    if (data) return;
    setLoading(true);
    try { setData(await api.get<SessionReportResult>(`/stats/sessions?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode, data]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  return (
    <div>
      <p style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginBottom: 12 }}>
        총 {data.totalSessions}개 세션
      </p>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        {data.sessions.map((s: SessionEntry) => (
          <div key={s.date} className="card" style={{ padding: 12 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 6 }}>
              <div>
                <div style={{ fontWeight: 700, fontSize: 14 }}>{s.date}</div>
                <div style={{ fontSize: 11, color: 'var(--color-text-secondary)', marginTop: 2 }}>
                  {s.games}경기 · 약 {s.totalDurationMin}분
                </div>
              </div>
              <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
                <div style={{ textAlign: 'center' }}>
                  <div style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>팀100</div>
                  <div style={{ fontWeight: 700, fontSize: 14, color: 'var(--color-win)' }}>{s.team100Wins}</div>
                </div>
                <div style={{ fontSize: 12, color: 'var(--color-text-disabled)' }}>vs</div>
                <div style={{ textAlign: 'center' }}>
                  <div style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>팀200</div>
                  <div style={{ fontWeight: 700, fontSize: 14, color: 'var(--color-loss)' }}>{s.team200Wins}</div>
                </div>
              </div>
            </div>
            <div style={{ display: 'flex', gap: 12, marginTop: 8, flexWrap: 'wrap' }}>
              {s.sessionMvp && (
                <div style={{ fontSize: 12 }}>
                  <span style={{ color: 'var(--color-text-disabled)' }}>MVP: </span>
                  <span style={{ fontWeight: 700, color: '#FFD700' }}>{s.sessionMvp.split('#')[0]}</span>
                  <span style={{ color: 'var(--color-text-disabled)', fontSize: 11 }}> (KDA {s.sessionMvpKda.toFixed(2)})</span>
                </div>
              )}
              {s.pentaKills > 0 && (
                <div style={{ fontSize: 12 }}>
                  <span style={{ color: 'var(--color-text-disabled)' }}>펜타킬: </span>
                  <span style={{ fontWeight: 700, color: '#f472b6' }}>{s.pentaKills}회</span>
                </div>
              )}
              <div style={{ fontSize: 12 }}>
                <span style={{ color: 'var(--color-text-disabled)' }}>총킬: </span>
                <span style={{ fontWeight: 600 }}>{s.totalKills}</span>
              </div>
            </div>
            {s.participants.length > 0 && (
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 4, marginTop: 8 }}>
                {s.participants.map(p => (
                  <span key={p} style={{ fontSize: 10, padding: '2px 6px', borderRadius: 10, background: 'var(--color-bg-hover)', color: 'var(--color-text-secondary)' }}>
                    {p.split('#')[0]}
                  </span>
                ))}
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
