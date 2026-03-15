import { useEffect, useState, useCallback } from 'react';
import { User, RefreshCw } from 'lucide-react';

interface RecentGame {
  gameId: number;
  gameCreation: number;
  gameDuration: number;
  queueId: number;
  win: boolean;
  champion: string;
  kills: number;
  deaths: number;
  assists: number;
}

const QUEUE_LABELS: Record<number, string> = {
  0: '내전',
  3130: '내전(ARAM)',
  3270: '내전',
};

export function SummonerPage() {
  const [puuid, setPuuid] = useState<string | null>(null);
  const [gameName, setGameName] = useState<string>('');
  const [games, setGames] = useState<RecentGame[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      // lcu:status-request는 event 방식이므로 직접 API 호출
      const data = await window.lol.getLiveGame(); // reuse to check connection
      // 현재 소환사 정보는 Titlebar에서 onStatus로 받으므로, 여기서는 별도 처리
      // puuid가 없으면 안내 표시
      if (!puuid) {
        setError('소환사 정보를 불러올 수 없습니다. 수집 탭에서 롤 클라이언트 연결을 확인해주세요.');
        setGames([]);
        return;
      }

      const history = await window.lol.getSummonerHistory(puuid);
      if (!history) { setGames([]); return; }

      const rawGames = ((history['games'] as Record<string, unknown>)?.['games'] as unknown[]) ?? [];
      const filtered = rawGames
        .map(g => g as Record<string, unknown>)
        .filter(g => Object.keys(QUEUE_LABELS).includes(String(g['queueId'])))
        .slice(0, 20)
        .map(g => {
          const participants = (g['participants'] as Record<string, unknown>[]) ?? [];
          const me = participants[0] ?? {};
          const stats = (me['stats'] as Record<string, unknown>) ?? {};
          return {
            gameId: g['gameId'] as number,
            gameCreation: g['gameCreation'] as number,
            gameDuration: g['gameDuration'] as number,
            queueId: g['queueId'] as number,
            win: stats['win'] === true,
            champion: (me['champion'] as string) ?? '?',
            kills: (stats['kills'] as number) ?? 0,
            deaths: (stats['deaths'] as number) ?? 0,
            assists: (stats['assists'] as number) ?? 0,
          };
        });

      setGames(filtered);
    } catch {
      setError('전적 조회 실패');
    } finally {
      setLoading(false);
    }
  }, [puuid]);

  // Titlebar의 onStatus에서 puuid를 받으면 업데이트
  useEffect(() => {
    window.lol.onStatus((s) => {
      if (s['puuid']) setPuuid(s['puuid'] as string);
      if (s['gameName']) setGameName(`${s['gameName']}#${s['tagLine'] ?? ''}`);
    });
    window.lol.requestStatus();
  }, []);

  useEffect(() => {
    if (puuid) load();
  }, [puuid, load]);

  const kda = (k: number, d: number, a: number) =>
    d === 0 ? '완벽' : ((k + a) / d).toFixed(2);

  const duration = (sec: number) =>
    `${Math.floor(sec / 60)}:${String(sec % 60).padStart(2, '0')}`;

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">소환사 전적</h1>
        <p className="page-subtitle">내전 최근 전적을 조회합니다</p>
      </div>

      <div className="card" style={{ marginBottom: 'var(--spacing-md)' }}>
        <div className="card-header">
          <span className="card-title">
            <User size={14} style={{ marginRight: 6 }} />
            {gameName || '소환사 정보 없음'}
          </span>
          <button className="btn btn-secondary btn-sm" onClick={load} disabled={loading || !puuid}>
            <RefreshCw size={12} style={{ animation: loading ? 'spin 1s linear infinite' : 'none' }} />
            {loading ? '조회 중...' : '새로고침'}
          </button>
        </div>
        {error && <p style={{ color: 'var(--color-text-secondary)', fontSize: 'var(--font-size-sm)' }}>{error}</p>}
      </div>

      {games.length > 0 && (
        <div className="card">
          <div className="card-header">
            <span className="card-title">최근 내전 ({games.length}게임)</span>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
            {games.map((g) => (
              <div key={g.gameId} style={{
                display: 'flex', alignItems: 'center', gap: 12,
                padding: '8px 12px', borderRadius: 'var(--radius-sm)',
                background: g.win ? 'rgba(11,196,180,0.06)' : 'rgba(232,64,64,0.06)',
                borderLeft: `3px solid ${g.win ? 'var(--color-win)' : 'var(--color-loss)'}`,
              }}>
                <span style={{
                  fontSize: 'var(--font-size-xs)', fontWeight: 700,
                  color: g.win ? 'var(--color-win)' : 'var(--color-loss)',
                  width: 24, flexShrink: 0,
                }}>
                  {g.win ? '승' : '패'}
                </span>
                <span style={{ width: 70, fontSize: 'var(--font-size-xs)', color: 'var(--color-primary)', flexShrink: 0 }}>
                  {g.champion}
                </span>
                <span style={{ flex: 1, fontSize: 'var(--font-size-xs)', color: 'var(--color-text-primary)', fontWeight: 500 }}>
                  {g.kills}/{g.deaths}/{g.assists}
                  <span style={{ color: 'var(--color-text-secondary)', marginLeft: 6 }}>
                    KDA {kda(g.kills, g.deaths, g.assists)}
                  </span>
                </span>
                <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)', flexShrink: 0 }}>
                  {duration(g.gameDuration)}
                </span>
                <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-disabled)', flexShrink: 0 }}>
                  {QUEUE_LABELS[g.queueId] ?? `Q${g.queueId}`}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}

      {games.length === 0 && !loading && !error && puuid && (
        <div className="card" style={{ textAlign: 'center', padding: '32px 24px' }}>
          <p style={{ color: 'var(--color-text-secondary)', fontSize: 'var(--font-size-sm)' }}>
            최근 내전 기록이 없습니다
          </p>
        </div>
      )}

      <style>{`@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }`}</style>
    </div>
  );
}
