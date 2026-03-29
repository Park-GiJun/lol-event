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
        <div className="card" style={{ marginBottom: 20, padding: '14px 18px', borderLeft: '3px solid var(--color-primary)' }}>
          <div style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginBottom: 4 }}>🔥 최대 라이벌</div>
          <div style={{ fontWeight: 700, fontSize: 15 }}>
            {data.topRivalry.player1.split('#')[0]} vs {data.topRivalry.player2.split('#')[0]}
          </div>
          <div style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginTop: 4 }}>
            {data.topRivalry.games}회 맞대결
          </div>
        </div>
      )}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        {data.rivalries.map((r: RivalMatchupEntry) => {
          const p2WinRate = Math.round(100 - r.player1WinRate);
          return (
            <div key={`${r.player1}-${r.player2}`} className="card" style={{ padding: '12px 16px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                <div style={{ flex: 1, textAlign: 'right' }}>
                  <div style={{ fontWeight: 700, fontSize: 13, cursor: 'pointer' }}
                    onClick={() => navigate(`/player-stats/${encodeURIComponent(r.player1)}`)}>
                    {r.player1.split('#')[0]}
                  </div>
                  <div style={{ fontSize: 12, color: r.player1WinRate >= 50 ? 'var(--color-win)' : 'var(--color-loss)', fontWeight: 600 }}>
                    {r.player1WinRate}% ({r.player1Wins}승)
                  </div>
                </div>
                <div style={{ textAlign: 'center', minWidth: 60 }}>
                  <div style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>{r.games}회</div>
                  <div style={{ fontSize: 12, fontWeight: 700, color: 'var(--color-text-secondary)' }}>VS</div>
                </div>
                <div style={{ flex: 1 }}>
                  <div style={{ fontWeight: 700, fontSize: 13, cursor: 'pointer' }}
                    onClick={() => navigate(`/player-stats/${encodeURIComponent(r.player2)}`)}>
                    {r.player2.split('#')[0]}
                  </div>
                  <div style={{ fontSize: 12, color: p2WinRate >= 50 ? 'var(--color-win)' : 'var(--color-loss)', fontWeight: 600 }}>
                    {p2WinRate}% ({r.player2Wins}승)
                  </div>
                </div>
              </div>
              <div style={{ marginTop: 8, height: 6, background: 'var(--color-bg-hover)', borderRadius: 3, overflow: 'hidden', display: 'flex' }}>
                <div style={{ width: `${r.player1WinRate}%`, background: 'var(--color-primary)', transition: 'width 0.3s' }} />
                <div style={{ flex: 1, background: 'var(--color-loss)' }} />
              </div>
            </div>
          );
        })}
        {!data.rivalries.length && (
          <p style={{ fontSize: 13, color: 'var(--color-text-secondary)', padding: '24px 0' }}>데이터 없음</p>
        )}
      </div>
    </div>
  );
}
