import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../../lib/api/api';
import type { RivalMatchupResult, RivalMatchupEntry } from '../../lib/types/stats';
import { LoadingCenter } from '../../components/common/Spinner';

export default function RivalTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const [data, setData]       = useState<RivalMatchupResult | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try { setData(await api.get<RivalMatchupResult>(`/stats/rival-matchup?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  return (
    <div>
      {data.topRivalry && (
        <div className="hero-banner" style={{ marginBottom: 'var(--spacing-lg)' }}>
          <div className="hero-eyebrow">🔥 최대 라이벌</div>
          <h2 className="hero-title">
            {data.topRivalry.player1.split('#')[0]} <span style={{ color: 'var(--color-text-disabled)', fontWeight: 'var(--font-weight-normal)' }}>vs</span> {data.topRivalry.player2.split('#')[0]}
          </h2>
          <div className="hero-subtitle">
            {data.topRivalry.games}회 맞대결
          </div>
        </div>
      )}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--spacing-sm)' }}>
        {data.rivalries.map((r: RivalMatchupEntry) => {
          const p2WinRate = Math.round(100 - r.player1WinRate);
          return (
            <div key={`${r.player1}-${r.player2}`} className="card" style={{ padding: '14px 18px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--spacing-sm)' }}>
                <div style={{ flex: 1, textAlign: 'right' }}>
                  <div style={{ fontWeight: 'var(--font-weight-bold)', fontSize: 'var(--font-size-sm)', cursor: 'pointer', transition: 'color var(--transition-fast)' }}
                    onClick={() => navigate(`/player-stats/${encodeURIComponent(r.player1)}`)}>
                    {r.player1.split('#')[0]}
                  </div>
                  <div style={{ fontSize: 'var(--font-size-xs)', color: r.player1WinRate >= 50 ? 'var(--color-win)' : 'var(--color-loss)', fontWeight: 'var(--font-weight-bold)', fontVariantNumeric: 'tabular-nums' }}>
                    {r.player1WinRate}% ({r.player1Wins}승)
                  </div>
                </div>
                <div style={{ textAlign: 'center', minWidth: 60 }}>
                  <div style={{ fontSize: 'var(--font-size-2xs)', color: 'var(--color-text-disabled)' }}>{r.games}회</div>
                  <div style={{ fontSize: 'var(--font-size-sm)', fontWeight: 'var(--font-weight-extrabold)', color: 'var(--color-text-secondary)', letterSpacing: 'var(--tracking-wide)' }}>VS</div>
                </div>
                <div style={{ flex: 1 }}>
                  <div style={{ fontWeight: 'var(--font-weight-bold)', fontSize: 'var(--font-size-sm)', cursor: 'pointer', transition: 'color var(--transition-fast)' }}
                    onClick={() => navigate(`/player-stats/${encodeURIComponent(r.player2)}`)}>
                    {r.player2.split('#')[0]}
                  </div>
                  <div style={{ fontSize: 'var(--font-size-xs)', color: p2WinRate >= 50 ? 'var(--color-win)' : 'var(--color-loss)', fontWeight: 'var(--font-weight-bold)', fontVariantNumeric: 'tabular-nums' }}>
                    {p2WinRate}% ({r.player2Wins}승)
                  </div>
                </div>
              </div>
              <div style={{ marginTop: 'var(--spacing-sm)', height: 6, background: 'var(--color-bg-hover)', borderRadius: 'var(--radius-sm)', overflow: 'hidden', display: 'flex' }}>
                <div style={{ width: `${r.player1WinRate}%`, background: 'var(--gradient-primary)', transition: 'width 0.4s ease' }} />
                <div style={{ flex: 1, background: 'var(--color-loss)' }} />
              </div>
            </div>
          );
        })}
        {!data.rivalries.length && (
          <p style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-text-secondary)', padding: 'var(--spacing-lg) 0' }}>데이터 없음</p>
        )}
      </div>
    </div>
  );
}
