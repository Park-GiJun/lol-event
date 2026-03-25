import { useEffect, useState, useCallback } from 'react';
import { RefreshCw, Swords } from 'lucide-react';

const API = 'https://api.gijun.net/api';
const CDN = 'https://raw.communitydragon.org/latest/plugins/rcp-be-lol-game-data/global/default/v1/champion-icons';

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

interface PlayerStats {
  games: number;
  winRate: number;
  elo?: number;
  championStats: Array<{ champion: string; championId: number; games: number; winRate: number }>;
}

function ChampIcon({ id, size = 28 }: { id: number; size?: number }) {
  if (!id) return <div style={{ width: size, height: size, borderRadius: 4, background: 'var(--color-bg-hover)' }} />;
  return (
    <img
      src={`${CDN}/${id}.png`}
      alt=""
      width={size}
      height={size}
      style={{ borderRadius: 4, objectFit: 'cover', flexShrink: 0 }}
      onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }}
    />
  );
}

function winRateColor(wr: number) {
  if (wr >= 60) return '#ff4e50';
  if (wr >= 55) return '#ffb347';
  if (wr >= 50) return '#4caf50';
  return 'var(--color-text-secondary)';
}

export function LiveGamePage() {
  const [phase, setPhase] = useState<string | null>(null);
  const [teams, setTeams] = useState<Team[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  // summonerName → riotId 매핑
  const [riotIdMap, setRiotIdMap] = useState<Record<string, string>>({});
  // riotId → stats
  const [statsMap, setStatsMap] = useState<Record<string, PlayerStats>>({});

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

  // 팀 로드 후 riotId 매핑 + 내전 통계 fetch
  useEffect(() => {
    if (!teams.length) return;
    let cancelled = false;

    window.lol.getCustomMostPicks().then(picks => {
      if (cancelled || !picks) return;
      const map: Record<string, string> = {};
      for (const p of [...(picks.blueTeam ?? []), ...(picks.redTeam ?? [])]) {
        const tp = p as { summonerName?: string; riotId?: string };
        if (tp.riotId && tp.summonerName) map[tp.summonerName] = tp.riotId;
      }
      setRiotIdMap(map);

      const riotIds = [...new Set(Object.values(map))];
      Promise.all(
        riotIds.map(async (riotId) => {
          try {
            const res = await fetch(`${API}/stats/player/${encodeURIComponent(riotId)}?mode=all`);
            if (!res.ok) return null;
            const json = await res.json() as { data: PlayerStats };
            return [riotId, json.data] as [string, PlayerStats];
          } catch { return null; }
        })
      ).then(results => {
        if (cancelled) return;
        const sm: Record<string, PlayerStats> = {};
        for (const r of results) if (r) sm[r[0]] = r[1];
        setStatsMap(sm);
      });
    });

    return () => { cancelled = true; };
  }, [teams]);

  const teamColor = (teamId: number) => teamId === 100 ? 'var(--color-info)' : 'var(--color-error)';
  const teamLabel = (teamId: number) => teamId === 100 ? '블루팀' : '레드팀';

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">현재 게임</h1>
        <p className="page-subtitle">진행 중인 게임의 팀 구성 및 내전 통계</p>
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
        <div key={team.teamId} className="card" style={{ marginBottom: 'var(--spacing-md)' }}>
          <div className="card-header">
            <span className="card-title" style={{ color: teamColor(team.teamId) }}>
              {teamLabel(team.teamId)} ({team.players.length}명)
            </span>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {team.players.map((p, i) => {
              const riotId = riotIdMap[p.summonerName];
              const stats = riotId ? statsMap[riotId] : undefined;
              const eloVal = stats?.elo != null && Number.isFinite(stats.elo) ? Math.round(stats.elo) : null;
              const eloColor = eloVal == null ? 'var(--color-text-secondary)'
                : eloVal >= 1200 ? 'var(--color-win)'
                : eloVal >= 1000 ? 'var(--color-primary)'
                : 'var(--color-loss)';
              const topChamps = stats?.championStats.slice(0, 3) ?? [];

              return (
                <div key={i} style={{
                  display: 'flex', alignItems: 'center', gap: 10,
                  padding: '8px 12px', borderRadius: 'var(--radius-md)',
                  background: 'var(--color-bg-hover)',
                }}>
                  {/* 챔피언 아이콘 */}
                  <ChampIcon id={p.championId} size={32} />

                  {/* 닉네임 + Elo */}
                  <div style={{ minWidth: 0, flex: 1 }}>
                    <div style={{
                      fontWeight: 500, fontSize: 'var(--font-size-sm)',
                      overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
                    }}>
                      {p.summonerName}
                    </div>
                    <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginTop: 2 }}>
                      {eloVal !== null ? (
                        <span style={{ fontSize: 10, fontFamily: 'monospace', color: eloColor }}>
                          Elo {eloVal}
                        </span>
                      ) : stats ? (
                        <span style={{ fontSize: 10, color: 'var(--color-text-secondary)' }}>데이터 없음</span>
                      ) : null}
                      {stats && (
                        <span style={{ fontSize: 10, color: 'var(--color-text-secondary)' }}>
                          {stats.games}판 {stats.winRate}%
                        </span>
                      )}
                    </div>
                  </div>

                  {/* 내전 top3 챔피언 */}
                  <div style={{ display: 'flex', gap: 4 }}>
                    {topChamps.map(c => (
                      <div key={c.championId} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 1 }}>
                        <ChampIcon id={c.championId} size={24} />
                        <span style={{ fontSize: 9, fontFamily: 'monospace', color: winRateColor(c.winRate) }}>
                          {c.winRate}%
                        </span>
                      </div>
                    ))}
                  </div>

                  {/* 챔피언명 (있으면) */}
                  {p.championName && (
                    <span style={{ color: 'var(--color-primary)', fontSize: 'var(--font-size-xs)', flexShrink: 0 }}>
                      {p.championName}
                    </span>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      ))}

      <style>{`@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }`}</style>
    </div>
  );
}
