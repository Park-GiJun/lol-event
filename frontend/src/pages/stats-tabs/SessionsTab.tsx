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
          <div key={s.date} className="card" style={{ padding: '14px 16px', transition: 'border-color var(--transition-fast), background var(--transition-fast)' }}>
            <div className="grid-16" style={{ alignItems: 'flex-start', marginBottom: 8 }}>
              <div className="col-span-8">
                <div style={{ fontWeight: 800, fontSize: 15 }}>{s.date}</div>
                <div style={{ fontSize: 11, color: 'var(--color-text-secondary)', marginTop: 3 }}>
                  {s.games}경기 · 약 {s.totalDurationMin}분
                </div>
              </div>
              <div className="col-span-8" style={{ display: 'flex', gap: 12, alignItems: 'center', justifyContent: 'flex-end' }}>
                <div style={{ textAlign: 'center' }}>
                  <div style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>팀100</div>
                  <div style={{ fontWeight: 800, fontSize: 15, color: 'var(--color-win)', fontVariantNumeric: 'tabular-nums' }}>{s.team100Wins}</div>
                </div>
                <div style={{ fontSize: 11, color: 'var(--color-text-disabled)' }}>vs</div>
                <div style={{ textAlign: 'center' }}>
                  <div style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>팀200</div>
                  <div style={{ fontWeight: 800, fontSize: 15, color: 'var(--color-loss)', fontVariantNumeric: 'tabular-nums' }}>{s.team200Wins}</div>
                </div>
              </div>
            </div>
            <div className="grid-16" style={{ marginTop: 8 }}>
              {s.sessionMvp && (
                <div className="col-span-6" style={{ fontSize: 12 }}>
                  <span style={{ color: 'var(--color-text-disabled)' }}>MVP: </span>
                  <span style={{ fontWeight: 700, color: '#FFD700' }}>{s.sessionMvp.split('#')[0]}</span>
                  <span style={{ color: 'var(--color-text-disabled)', fontSize: 11 }}> (KDA {s.sessionMvpKda.toFixed(2)})</span>
                </div>
              )}
              {s.pentaKills > 0 && (
                <div className="col-span-4" style={{ fontSize: 12 }}>
                  <span style={{ color: 'var(--color-text-disabled)' }}>펜타킬: </span>
                  <span style={{ fontWeight: 700, color: '#f472b6' }}>{s.pentaKills}회</span>
                </div>
              )}
              <div className="col-span-4" style={{ fontSize: 12 }}>
                <span style={{ color: 'var(--color-text-disabled)' }}>총킬: </span>
                <span style={{ fontWeight: 600 }}>{s.totalKills}</span>
              </div>
            </div>
            {s.participants.length > 0 && (
              <div className="grid-16" style={{ marginTop: 8 }}>
                {s.participants.map(p => (
                  <span key={p} className="col-span-2" style={{ fontSize: 10, padding: '2px 6px', borderRadius: 10, background: 'var(--color-bg-hover)', color: 'var(--color-text-secondary)' }}>
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
