import { useEffect, useState, useCallback } from 'react';
import { RefreshCw, Swords } from 'lucide-react';

interface Participant {
  summonerName: string;
  championId: number;
  championName?: string;
  teamId: number;
  spell1Id: number;
  spell2Id: number;
}

interface Team {
  teamId: number;
  players: Participant[];
}

export function LiveGamePage() {
  const [phase, setPhase] = useState<string | null>(null);
  const [teams, setTeams] = useState<Team[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await window.lol.getLiveGame();
      if (!data) { setPhase(null); setTeams([]); return; }

      setPhase(data['phase'] as string);
      if (data['phase'] !== 'InProgress') { setTeams([]); return; }

      const session = data['session'] as Record<string, unknown>;
      const gameData = session?.['gameData'] as Record<string, unknown> | undefined;
      const rawTeams = gameData?.['teamOne'] || gameData?.['teamTwo']
        ? [
            { teamId: 100, players: (gameData?.['teamOne'] as unknown[]) ?? [] },
            { teamId: 200, players: (gameData?.['teamTwo'] as unknown[]) ?? [] },
          ]
        : [];

      setTeams(rawTeams.map(t => ({
        teamId: t.teamId,
        players: (t.players as Record<string, unknown>[]).map(p => ({
          summonerName: (p['summonerName'] ?? p['gameName'] ?? '???') as string,
          championId: p['championId'] as number,
          championName: p['championName'] as string | undefined,
          teamId: t.teamId,
          spell1Id: p['spell1Id'] as number,
          spell2Id: p['spell2Id'] as number,
        })),
      })));
    } catch {
      setError('LCU 연결 실패');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const teamColor = (teamId: number) => teamId === 100 ? 'var(--color-info)' : 'var(--color-error)';
  const teamLabel = (teamId: number) => teamId === 100 ? '블루팀' : '레드팀';

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">현재 게임</h1>
        <p className="page-subtitle">진행 중인 게임의 팀 구성을 표시합니다</p>
      </div>

      <div className="card" style={{ marginBottom: 'var(--spacing-md)' }}>
        <div className="card-header">
          <span className="card-title"><Swords size={14} style={{ marginRight: 6 }} />게임 상태</span>
          <button className="btn btn-secondary btn-sm" onClick={load} disabled={loading}>
            <RefreshCw size={12} style={{ animation: loading ? 'spin 1s linear infinite' : 'none' }} />
            {loading ? '로딩 중...' : '새로고침'}
          </button>
        </div>
        {error && <p style={{ color: 'var(--color-error)', fontSize: 'var(--font-size-sm)' }}>{error}</p>}
        {!error && phase === null && !loading && (
          <p style={{ color: 'var(--color-text-secondary)', fontSize: 'var(--font-size-sm)' }}>
            롤 클라이언트에 연결되지 않았습니다
          </p>
        )}
        {phase && phase !== 'InProgress' && (
          <p style={{ color: 'var(--color-text-secondary)', fontSize: 'var(--font-size-sm)' }}>
            현재 게임 없음 <span style={{ color: 'var(--color-primary)' }}>(Phase: {phase})</span>
          </p>
        )}
      </div>

      {teams.length > 0 && teams.map(team => (
        <div key={team.teamId} className="card">
          <div className="card-header">
            <span className="card-title" style={{ color: teamColor(team.teamId) }}>
              {teamLabel(team.teamId)} ({team.players.length}명)
            </span>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {team.players.map((p, i) => (
              <div key={i} style={{
                display: 'flex', alignItems: 'center', gap: 12,
                padding: '8px 12px', borderRadius: 'var(--radius-md)',
                background: 'var(--color-bg-hover)',
              }}>
                <span style={{
                  width: 28, height: 28, borderRadius: 4,
                  background: 'var(--color-bg-tertiary)',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  fontSize: 10, color: 'var(--color-text-secondary)', flexShrink: 0,
                }}>
                  {p.championId || '?'}
                </span>
                <span style={{ flex: 1, fontWeight: 500 }}>{p.summonerName}</span>
                {p.championName && (
                  <span style={{ color: 'var(--color-primary)', fontSize: 'var(--font-size-xs)' }}>
                    {p.championName}
                  </span>
                )}
              </div>
            ))}
          </div>
        </div>
      ))}

      <style>{`@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }`}</style>
    </div>
  );
}
