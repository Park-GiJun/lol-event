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
      <div className="section-head">
        <span className="icon-chip">📅</span>
        <span className="section-head-title">세션 리포트</span>
        <span className="section-head-action badge badge-primary badge-sm">총 {data.totalSessions}개</span>
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--spacing-sm)' }}>
        {data.sessions.map((s: SessionEntry) => (
          <div key={s.date} className="card" style={{ padding: 'var(--spacing-md)' }}>
            <div className="grid-16" style={{ alignItems: 'flex-start', marginBottom: 'var(--spacing-sm)' }}>
              <div className="col-span-8">
                <div style={{ fontWeight: 'var(--font-weight-extrabold)', fontSize: 'var(--font-size-md)' }}>{s.date}</div>
                <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)', marginTop: 'var(--spacing-2xs)' }}>
                  {s.games}경기 · 약 {s.totalDurationMin}분
                </div>
              </div>
              <div className="col-span-8" style={{ display: 'flex', gap: 'var(--spacing-md)', alignItems: 'center', justifyContent: 'flex-end' }}>
                <div style={{ textAlign: 'center' }}>
                  <div style={{ fontSize: 'var(--font-size-2xs)', color: 'var(--color-text-disabled)' }}>팀100</div>
                  <div style={{ fontWeight: 'var(--font-weight-extrabold)', fontSize: 'var(--font-size-md)', color: 'var(--color-win)', fontVariantNumeric: 'tabular-nums' }}>{s.team100Wins}</div>
                </div>
                <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-disabled)' }}>vs</div>
                <div style={{ textAlign: 'center' }}>
                  <div style={{ fontSize: 'var(--font-size-2xs)', color: 'var(--color-text-disabled)' }}>팀200</div>
                  <div style={{ fontWeight: 'var(--font-weight-extrabold)', fontSize: 'var(--font-size-md)', color: 'var(--color-loss)', fontVariantNumeric: 'tabular-nums' }}>{s.team200Wins}</div>
                </div>
              </div>
            </div>
            <div className="grid-16" style={{ marginTop: 'var(--spacing-sm)' }}>
              {s.sessionMvp && (
                <div className="col-span-6" style={{ fontSize: 'var(--font-size-sm)' }}>
                  <span style={{ color: 'var(--color-text-disabled)' }}>MVP: </span>
                  <span style={{ fontWeight: 'var(--font-weight-bold)', color: 'var(--color-primary)' }}>{s.sessionMvp.split('#')[0]}</span>
                  <span style={{ color: 'var(--color-text-disabled)', fontSize: 'var(--font-size-xs)' }}> (KDA {s.sessionMvpKda.toFixed(2)})</span>
                </div>
              )}
              {s.pentaKills > 0 && (
                <div className="col-span-4" style={{ fontSize: 'var(--font-size-sm)' }}>
                  <span style={{ color: 'var(--color-text-disabled)' }}>펜타킬: </span>
                  <span style={{ fontWeight: 'var(--font-weight-bold)', color: 'var(--color-primary)' }}>{s.pentaKills}회</span>
                </div>
              )}
              <div className="col-span-4" style={{ fontSize: 'var(--font-size-sm)' }}>
                <span style={{ color: 'var(--color-text-disabled)' }}>총킬: </span>
                <span style={{ fontWeight: 'var(--font-weight-semibold)' }}>{s.totalKills}</span>
              </div>
            </div>
            {s.participants.length > 0 && (
              <div className="grid-16" style={{ marginTop: 'var(--spacing-sm)' }}>
                {s.participants.map(p => (
                  <span key={p} className="col-span-2 badge badge-normal badge-sm">
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
