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

interface OpponentData {
  summonerName: string;
  riotId: string;
  championStats: ChampionStat[] | null; // null = 서버에 데이터 없음
}

interface CustomOpponents {
  isCustom: boolean;
  phase: string;
  opponents: { summonerName: string; riotId: string }[];
}

function ChampIcon({ championId, champion }: { championId: number; champion: string }) {
  const src = `https://raw.communitydragon.org/latest/plugins/rcp-be-lol-game-data/global/default/v1/champion-icons/${championId}.png`;
  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4 }}>
      <img
        src={src}
        alt={champion}
        title={champion}
        style={{
          width: 40, height: 40, borderRadius: 6,
          border: '1px solid var(--color-border)',
          background: 'var(--color-bg-secondary)',
          objectFit: 'cover',
        }}
        onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }}
      />
      <span style={{ fontSize: 10, color: 'var(--color-text-secondary)', maxWidth: 44, textAlign: 'center', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
        {champion}
      </span>
    </div>
  );
}

async function fetchChampionStats(riotId: string): Promise<ChampionStat[] | null> {
  try {
    const res = await fetch(`${SERVER_URL}/api/stats/player/${encodeURIComponent(riotId)}?mode=all`);
    if (!res.ok) return null;
    const json = await res.json() as { data?: { championStats?: ChampionStat[] } };
    const stats = json.data?.championStats;
    if (!stats || stats.length === 0) return null;
    // 서버에서 이미 games 기준 정렬되어 있음, 상위 3개만
    return stats.slice(0, 3);
  } catch {
    return null;
  }
}

export function CustomGamePage() {
  const [opponents, setOpponents] = useState<OpponentData[]>([]);
  const [phase, setPhase] = useState<string>('');
  const [isCustom, setIsCustom] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result: CustomOpponents | null = await window.lol.getCustomMostPicks();
      if (!result) {
        setError('LCU 연결 실패');
        return;
      }

      setPhase(result.phase);
      setIsCustom(result.isCustom);

      if (!result.isCustom || result.opponents.length === 0) {
        setOpponents([]);
        return;
      }

      // 서버에서 각 상대의 챔피언 통계 병렬 조회
      const opponentData = await Promise.all(
        result.opponents.map(async ({ summonerName, riotId }) => ({
          summonerName,
          riotId,
          championStats: await fetchChampionStats(riotId),
        }))
      );

      setOpponents(opponentData);
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
    Lobby: '대기방',
    ChampSelect: '챔피언 선택',
    InProgress: '게임 중',
    None: '대기 중',
  };

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">내전 분석</h1>
        <p className="page-subtitle">커스텀 게임 상대팀의 모스트픽을 표시합니다</p>
      </div>

      <div className="card" style={{ marginBottom: 'var(--spacing-md)' }}>
        <div className="card-header">
          <span className="card-title">
            <Swords size={14} style={{ marginRight: 6 }} />
            상태
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

        {error && <p style={{ color: 'var(--color-error)', fontSize: 'var(--font-size-sm)' }}>{error}</p>}

        {!error && (
          <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
            <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)' }}>페이즈:</span>
            <span style={{
              fontSize: 'var(--font-size-xs)',
              color: isCustom ? 'var(--color-primary)' : 'var(--color-text-secondary)',
              fontWeight: isCustom ? 600 : 400,
            }}>
              {(phaseLabel[phase] ?? phase) || '연결 중...'}
            </span>
            {phase && !isCustom && (
              <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-warning)' }}>
                커스텀 게임이 아닙니다
              </span>
            )}
          </div>
        )}
      </div>

      {isCustom && opponents.length === 0 && !loading && (
        <div className="card">
          <p style={{ color: 'var(--color-text-secondary)', fontSize: 'var(--font-size-sm)' }}>
            {phase === 'Lobby' ? '상대팀에 아직 플레이어가 없습니다' : '상대팀 정보를 가져올 수 없습니다'}
          </p>
        </div>
      )}

      {opponents.length > 0 && (
        <>
          <div className="card" style={{ marginBottom: 'var(--spacing-sm)' }}>
            <div className="card-header">
              <span className="card-title" style={{ color: 'var(--color-error)' }}>상대팀 모스트픽</span>
              <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)' }}>
                내전 기록 기준
              </span>
            </div>
          </div>

          {opponents.map((opp, i) => (
            <div className="card" key={opp.riotId || i} style={{ marginBottom: 'var(--spacing-sm)' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 10 }}>
                <div>
                  <span style={{ fontSize: 'var(--font-size-sm)', fontWeight: 600 }}>
                    {opp.summonerName || opp.riotId || `상대 ${i + 1}`}
                  </span>
                  {opp.riotId && opp.summonerName !== opp.riotId && (
                    <span style={{ marginLeft: 6, fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)' }}>
                      {opp.riotId}
                    </span>
                  )}
                </div>
              </div>

              {opp.championStats === null ? (
                <p style={{ color: 'var(--color-text-secondary)', fontSize: 'var(--font-size-xs)' }}>
                  내전 기록 없음
                </p>
              ) : (
                <div style={{ display: 'flex', gap: 16, alignItems: 'flex-start', flexWrap: 'wrap' }}>
                  {opp.championStats.map((stat, j) => (
                    <div key={j} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4 }}>
                      <ChampIcon championId={stat.championId} champion={stat.champion} />
                      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2 }}>
                        <span style={{
                          fontSize: 10, color: 'var(--color-text-secondary)',
                          background: 'var(--color-bg-secondary)',
                          borderRadius: 4, padding: '1px 5px',
                        }}>
                          {stat.games}판
                        </span>
                        <span style={{
                          fontSize: 10,
                          color: stat.winRate >= 60 ? 'var(--color-success)' : stat.winRate >= 50 ? 'var(--color-warning)' : 'var(--color-error)',
                        }}>
                          {stat.winRate}%
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          ))}
        </>
      )}

      <style>{`@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }`}</style>
    </div>
  );
}
