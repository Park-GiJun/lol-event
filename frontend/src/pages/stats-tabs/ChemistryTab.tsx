import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../../lib/api/api';
import type { TeamChemistryResult, TeamChemistryEntry } from '../../lib/types/stats';
import { LoadingCenter } from '../../components/common/Spinner';
import { RankBadge, WinRateBar } from './shared';

export default function ChemistryTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const [data, setData]       = useState<TeamChemistryResult | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try { setData(await api.get<TeamChemistryResult>(`/stats/team-chemistry?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  const renderSection = (title: string, items: TeamChemistryEntry[]) => (
    <section style={{ marginBottom: 24 }}>
      <h3 style={{ fontSize: 13, fontWeight: 700, marginBottom: 10 }}>{title}</h3>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        {items.slice(0, 5).map((entry, i) => (
          <div key={entry.players.join('-')} className="card" style={{ padding: '10px 14px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <RankBadge rank={i + 1} />
              <div style={{ flex: 1 }}>
                <div style={{ fontWeight: 600, fontSize: 13 }}>
                  {entry.players.map(p => (
                    <span key={p} style={{ cursor: 'pointer', marginRight: 6 }}
                      onClick={() => navigate(`/player-stats/${encodeURIComponent(p)}`)}>
                      {p.split('#')[0]}
                    </span>
                  ))}
                </div>
              </div>
              <div style={{ textAlign: 'right' }}>
                <div style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>{entry.games}판</div>
                <WinRateBar winRate={entry.winRate} wins={entry.wins} losses={entry.games - entry.wins} />
              </div>
            </div>
          </div>
        ))}
        {!items.length && <p style={{ fontSize: 12, color: 'var(--color-text-disabled)' }}>데이터 없음</p>}
      </div>
    </section>
  );

  return (
    <div>
      {renderSection('👥 최강 2인조', data.bestDuos)}
      {renderSection('👥 최강 3인조', data.bestTrios)}
      {renderSection('👥 최강 5인팀', data.bestFullTeams)}
    </div>
  );
}
