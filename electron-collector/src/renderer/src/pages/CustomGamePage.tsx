import { useEffect, useState, useCallback } from 'react';
import { RefreshCw, Swords } from 'lucide-react';

const SERVER_URL = 'https://api.gijun.net';

interface ChampionStat {
  champion: string;
  championId: number;
  games: number;
  wins: number;
  winRate: number;
}

interface TeamMember {
  summonerName: string;
  riotId: string;
  isMe: boolean;
}

interface CustomTeams {
  phase: string;
  blueTeam: TeamMember[];
  redTeam: TeamMember[];
}

interface PlayerData extends TeamMember {
  championStats: ChampionStat[] | null;
}

async function fetchChampionStats(riotId: string): Promise<ChampionStat[] | null> {
  try {
    const res = await fetch(`${SERVER_URL}/api/stats/player/${encodeURIComponent(riotId)}?mode=all`);
    if (!res.ok) return null;
    const json = await res.json() as { data?: { championStats?: ChampionStat[] } };
    const stats = json.data?.championStats;
    if (!stats || stats.length === 0) return null;
    return stats.slice(0, 3);
  } catch {
    return null;
  }
}

function ChampIcon({ championId, champion }: { championId: number; champion: string }) {
  const src = `https://raw.communitydragon.org/latest/plugins/rcp-be-lol-game-data/global/default/v1/champion-icons/${championId}.png`;
  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2 }}>
      <img
        src={src}
        alt={champion}
        title={champion}
        style={{ width: 36, height: 36, borderRadius: 4, border: '1px solid var(--color-border)', objectFit: 'cover' }}
        onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }}
      />
      <span style={{ fontSize: 9, color: 'var(--color-text-secondary)', maxWidth: 40, textAlign: 'center', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
        {champion}
      </span>
    </div>
  );
}

function PlayerCard({ player, accentColor }: { player: PlayerData; accentColor: string }) {
  return (
    <div style={{
      padding: '10px 12px',
      borderRadius: 8,
      background: 'var(--color-bg-secondary)',
      border: `1px solid ${player.isMe ? accentColor : 'var(--color-border)'}`,
      marginBottom: 6,
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 8 }}>
        {player.isMe && (
          <span style={{ fontSize: 9, background: accentColor, color: '#fff', borderRadius: 3, padding: '1px 5px', fontWeight: 700 }}>나</span>
        )}
        <span style={{ fontSize: 13, fontWeight: 600, color: 'var(--color-text)' }}>
          {player.summonerName || player.riotId}
        </span>
        {player.riotId && player.summonerName !== player.riotId && (
          <span style={{ fontSize: 10, color: 'var(--color-text-secondary)' }}>{player.riotId}</span>
        )}
      </div>

      {player.championStats === null ? (
        <span style={{ fontSize: 10, color: 'var(--color-text-secondary)' }}>내전 기록 없음</span>
      ) : (
        <div style={{ display: 'flex', gap: 10 }}>
          {player.championStats.map((stat, i) => (
            <div key={i} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2 }}>
              <ChampIcon championId={stat.championId} champion={stat.champion} />
              <span style={{ fontSize: 9, color: 'var(--color-text-secondary)' }}>{stat.games}판</span>
              <span style={{
                fontSize: 9,
                color: stat.winRate >= 60 ? 'var(--color-success)' : stat.winRate >= 50 ? 'var(--color-warning)' : 'var(--color-error)',
              }}>
                {stat.winRate}%
              </span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function TeamColumn({ title, color, players }: { title: string; color: string; players: PlayerData[] }) {
  return (
    <div style={{ flex: 1, minWidth: 0 }}>
      <div style={{
        display: 'flex', alignItems: 'center', gap: 8,
        marginBottom: 10, paddingBottom: 8,
        borderBottom: `2px solid ${color}`,
      }}>
        <span style={{ fontWeight: 700, fontSize: 14, color }}>{title}</span>
        <span style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>{players.length}명</span>
      </div>
      {players.length === 0 ? (
        <p style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>플레이어 없음</p>
      ) : (
        players.map((p, i) => <PlayerCard key={p.riotId || i} player={p} accentColor={color} />)
      )}
    </div>
  );
}

export function CustomGamePage() {
  const [blueTeam, setBlueTeam] = useState<PlayerData[]>([]);
  const [redTeam, setRedTeam] = useState<PlayerData[]>([]);
  const [phase, setPhase] = useState<string>('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result: CustomTeams | null = await window.lol.getCustomMostPicks();
      if (!result) { setError('LCU 연결 실패'); return; }

      setPhase(result.phase);

      const toPlayerData = async (m: TeamMember): Promise<PlayerData> => ({
        ...m,
        championStats: await fetchChampionStats(m.riotId),
      });

      const [blue, red] = await Promise.all([
        Promise.all(result.blueTeam.map(toPlayerData)),
        Promise.all(result.redTeam.map(toPlayerData)),
      ]);

      setBlueTeam(blue);
      setRedTeam(red);
      setLastUpdated(new Date());
    } catch {
      setError('데이터 로드 실패');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
    const interval = setInterval(load, 10_000);
    return () => clearInterval(interval);
  }, [load]);

  const phaseLabel: Record<string, string> = {
    Lobby: '대기방', ChampSelect: '챔피언 선택', InProgress: '게임 중', None: '대기 중',
  };

  const hasData = blueTeam.length > 0 || redTeam.length > 0;

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">내전 분석</h1>
        <p className="page-subtitle">블루팀 / 레드팀 모스트픽</p>
      </div>

      <div className="card" style={{ marginBottom: 'var(--spacing-md)' }}>
        <div className="card-header">
          <span className="card-title">
            <Swords size={14} style={{ marginRight: 6 }} />
            {(phaseLabel[phase] ?? phase) || '연결 중...'}
          </span>
          <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
            {lastUpdated && (
              <span style={{ color: 'var(--color-text-secondary)', fontSize: 'var(--font-size-xs)' }}>
                {lastUpdated.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', second: '2-digit' })}
              </span>
            )}
            <button className="btn btn-secondary btn-sm" onClick={load} disabled={loading}>
              <RefreshCw size={12} style={{ animation: loading ? 'spin 1s linear infinite' : 'none' }} />
              {loading ? '로딩 중...' : '새로고침'}
            </button>
          </div>
        </div>
        {error && <p style={{ color: 'var(--color-error)', fontSize: 'var(--font-size-sm)', marginTop: 4 }}>{error}</p>}
      </div>

      {!hasData && !loading && phase && (
        <div className="card">
          <p style={{ color: 'var(--color-text-secondary)', fontSize: 'var(--font-size-sm)' }}>
            팀 정보가 없습니다
          </p>
        </div>
      )}

      {hasData && (
        <div style={{ display: 'flex', gap: 16 }}>
          <TeamColumn title="블루팀" color="#4A90D9" players={blueTeam} />
          <div style={{ width: 1, background: 'var(--color-border)', flexShrink: 0 }} />
          <TeamColumn title="레드팀" color="#D94A4A" players={redTeam} />
        </div>
      )}

      <style>{`@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }`}</style>
    </div>
  );
}
