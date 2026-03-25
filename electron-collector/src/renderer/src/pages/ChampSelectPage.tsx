import { useEffect, useState, useCallback } from 'react';
import { RefreshCw, Shield, Swords, Star } from 'lucide-react';
import { PlayerCard, PlayerData } from '../components/lobby/PlayerCard';
import { BanRecommendBadge } from '../components/lobby/BanRecommendBadge';

const API = 'https://api.gijun.net/api';
const DDRAGON = 'https://ddragon.leagueoflegends.com/cdn/14.24.1';
const CDN = 'https://raw.communitydragon.org/latest/plugins/rcp-be-lol-game-data/global/default/v1/champion-icons';

interface ChampSlot {
  cellId: number;
  championId: number;
  assignedPosition: string;
  riotId: string;
  summonerName: string;
  isMe: boolean;
}
interface BannedChamp { championId: number; team: 'blue' | 'red'; }
interface ChampSelectFull {
  myTeam: ChampSlot[];
  theirTeam: ChampSlot[];
  bans: BannedChamp[];
  phase: string;
  timer: number;
}

interface MatchupStat { opponent: string; opponentId: number; games: number; wins: number; winRate: number; }
interface ChampionMatchupResult { champion: string; championId: number; matchups: MatchupStat[]; }

const positionLabel: Record<string, string> = {
  top: '탑', jungle: '정글', middle: '미드', bottom: '원딜', utility: '서포터', '': '—',
};

function champIconUrl(id: number) {
  return `${CDN}/${id}.png`;
}

function winRateColor(wr: number) {
  if (wr >= 60) return '#ff4e50';
  if (wr >= 55) return '#ffb347';
  if (wr >= 50) return '#4caf50';
  return 'var(--color-text-secondary)';
}

function ChampIcon({ id, size = 28 }: { id: number; size?: number }) {
  if (!id) return <div style={{ width: size, height: size, borderRadius: 4, background: 'var(--color-bg-hover)' }} />;
  return (
    <img
      src={champIconUrl(id)}
      alt=""
      width={size} height={size}
      style={{ borderRadius: 4, objectFit: 'cover' }}
      onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }}
    />
  );
}

// 상대 챔피언별 카운터픽 분석 (같은 라인 기준)
function CounterSection({ enemies }: { enemies: ChampSlot[] }) {
  const [results, setResults] = useState<Record<string, ChampionMatchupResult>>({});
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const champIds = enemies.map(e => e.championId).filter(id => id > 0);
    if (champIds.length === 0) return;

    setLoading(true);
    fetch(`${DDRAGON}/data/ko_KR/champion.json`)
      .then(r => r.json())
      .then((json: { data: Record<string, { key: string; id: string; name: string }> }) => {
        const idToName: Record<number, string> = {};
        for (const champ of Object.values(json.data)) {
          idToName[parseInt(champ.key)] = champ.id;
        }
        return idToName;
      })
      .then(async (idToName) => {
        const entries: [string, ChampionMatchupResult][] = [];
        for (const e of enemies) {
          if (!e.championId) continue;
          const champName = idToName[e.championId];
          if (!champName) continue;
          try {
            const res = await fetch(`${API}/stats/matchup?vsChampion=${encodeURIComponent(champName)}&mode=normal&samePosition=true`);
            if (!res.ok) continue;
            const json = await res.json() as { data: ChampionMatchupResult };
            // 같은 라인 데이터 없으면 전체 기준 fallback
            const data = json.data;
            if (!data.matchups || data.matchups.length === 0) {
              const fallback = await fetch(`${API}/stats/matchup?vsChampion=${encodeURIComponent(champName)}&mode=normal`);
              if (fallback.ok) {
                const fJson = await fallback.json() as { data: ChampionMatchupResult };
                entries.push([String(e.championId), fJson.data]);
              }
            } else {
              entries.push([String(e.championId), data]);
            }
          } catch { /* ignore */ }
        }
        setResults(Object.fromEntries(entries));
      })
      .catch(() => { /* ignore */ })
      .finally(() => setLoading(false));
  }, [enemies.map(e => e.championId).join(',')]);

  const hasData = enemies.some(e => e.championId > 0);

  return (
    <div className="card" style={{ marginBottom: 'var(--spacing-md)' }}>
      <div className="card-header">
        <span className="card-title"><Swords size={14} style={{ marginRight: 6 }} />카운터픽 추천</span>
        <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)' }}>
          {loading ? '분석 중...' : '같은 라인 기준'}
        </span>
      </div>
      {!hasData && (
        <p style={{ color: 'var(--color-text-secondary)', fontSize: 'var(--font-size-sm)' }}>
          상대팀 챔피언 선택을 기다리는 중...
        </p>
      )}
      {enemies.filter(e => e.championId > 0).map(e => {
        const result = results[String(e.championId)];
        const top3 = result?.matchups.slice(0, 3) ?? [];
        return (
          <div key={e.cellId} style={{ marginBottom: 12 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 6 }}>
              <ChampIcon id={e.championId} size={24} />
              <span style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-error)' }}>
                {e.riotId || e.summonerName || '상대'}
              </span>
              <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)' }}>
                {positionLabel[e.assignedPosition?.toLowerCase() ?? ''] ?? '—'}
              </span>
              <span style={{ marginLeft: 'auto', fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)' }}>
                vs 카운터
              </span>
            </div>
            {top3.length > 0 ? (
              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                {top3.map(m => (
                  <div key={m.opponent} style={{
                    display: 'flex', alignItems: 'center', gap: 6,
                    padding: '4px 8px', borderRadius: 'var(--radius-sm)',
                    background: 'var(--color-bg-hover)', border: '1px solid var(--color-border)',
                  }}>
                    <ChampIcon id={m.opponentId} size={22} />
                    <div>
                      <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-primary)' }}>{m.opponent}</div>
                      <div style={{ fontSize: 10, color: winRateColor(m.winRate) }}>{m.winRate}% <span style={{ color: 'var(--color-text-secondary)' }}>({m.games}판)</span></div>
                    </div>
                  </div>
                ))}
              </div>
            ) : !loading ? (
              <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-disabled)' }}>데이터 없음</span>
            ) : null}
          </div>
        );
      })}
    </div>
  );
}

// P-3: playerDetails를 prop으로 수신 — 자체 fetch 제거로 중복 요청 방지
function BanRecommendSection({ enemies, playerDetails }: { enemies: ChampSlot[]; playerDetails: Record<string, PlayerData> }) {
  const hasRiotIds = enemies.some(e => e.riotId);

  return (
    <div className="card" style={{ marginBottom: 'var(--spacing-md)' }}>
      <div className="card-header">
        <span className="card-title"><Shield size={14} style={{ marginRight: 6 }} />밴 추천</span>
      </div>
      {!hasRiotIds && (
        <p style={{ color: 'var(--color-text-secondary)', fontSize: 'var(--font-size-sm)' }}>
          상대팀 소환사 정보를 불러오는 중...
        </p>
      )}
      {enemies.filter(e => e.riotId).map(e => {
        const player = playerDetails[e.riotId];
        const top3 = player?.championStats?.slice(0, 3) ?? [];
        return (
          <div key={e.cellId} style={{ marginBottom: 12 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 6 }}>
              <ChampIcon id={e.championId} size={24} />
              <span style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-error)' }}>
                {e.riotId}
              </span>
              {player && (
                <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)', marginLeft: 4 }}>
                  {player.games}판 {player.winRate}%
                </span>
              )}
            </div>
            {top3.length > 0 ? (
              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                {/* P-4: 인덱스 비교로 교체 */}
                {top3.map((c, i) => (
                  <BanRecommendBadge
                    key={c.championId}
                    champion={c.champion}
                    championId={c.championId}
                    isHighThreat={i === 0}
                    winRate={c.winRate}
                    games={c.games}
                  />
                ))}
              </div>
            ) : (
              <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-disabled)' }}>내전 데이터 없음</span>
            )}
          </div>
        );
      })}
    </div>
  );
}

// 나의 픽 추천
function MyPickSection({ myTeam, bans, playerDetails }: {
  myTeam: ChampSlot[];
  bans: BannedChamp[];
  playerDetails: Record<string, PlayerData>;
}) {
  const mySlot = myTeam.find(s => s.isMe);
  const myRiotId = mySlot?.riotId;
  const myPosition = mySlot?.assignedPosition?.toLowerCase() ?? '';
  const myData = myRiotId ? playerDetails[myRiotId] : null;
  const bannedIds = new Set(bans.map(b => b.championId));

  const topChamps = (myData?.championStats ?? [])
    .filter(c => c.games >= 2)
    .sort((a, b) => b.winRate - a.winRate)
    .slice(0, 12);

  return (
    <div className="card" style={{ marginBottom: 'var(--spacing-md)' }}>
      <div className="card-header">
        <span className="card-title"><Star size={14} style={{ marginRight: 6 }} />내 픽 추천</span>
        <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)' }}>
          {positionLabel[myPosition] ?? '—'} · 내전 승률순 (2판↑)
        </span>
      </div>
      {!myRiotId && (
        <p style={{ color: 'var(--color-text-secondary)', fontSize: 'var(--font-size-sm)' }}>
          소환사 정보를 불러오는 중...
        </p>
      )}
      {myRiotId && topChamps.length === 0 && (
        <p style={{ color: 'var(--color-text-secondary)', fontSize: 'var(--font-size-sm)' }}>
          내전 데이터 없음
        </p>
      )}
      {topChamps.length > 0 && (
        <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
          {topChamps.map(c => {
            const isBanned = bannedIds.has(c.championId);
            return (
              <div key={c.champion} style={{
                display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 3,
                opacity: isBanned ? 0.3 : 1,
                position: 'relative',
              }}>
                <div style={{ position: 'relative' }}>
                  <ChampIcon id={c.championId} size={38} />
                  {isBanned && (
                    <div style={{
                      position: 'absolute', inset: 0, borderRadius: 4,
                      background: 'rgba(0,0,0,0.65)',
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      fontSize: 18,
                    }}>🚫</div>
                  )}
                </div>
                <div style={{ textAlign: 'center', lineHeight: 1.3 }}>
                  <div style={{
                    fontSize: 10,
                    fontFamily: "'Consolas', 'D2Coding', monospace",
                    color: winRateColor(c.winRate),
                  }}>
                    {c.winRate}%
                  </div>
                  <div style={{ fontSize: 9, color: 'var(--color-text-secondary)' }}>
                    {c.games}판
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

export function ChampSelectPage() {
  const [state, setState] = useState<ChampSelectFull | null>(null);
  const [loading, setLoading] = useState(false);
  const [lcuError, setLcuError] = useState(false);
  const [tab, setTab] = useState<'counter' | 'ban'>('ban');
  const [playerDetails, setPlayerDetails] = useState<Record<string, PlayerData>>({});
  // P-2: "fetch 시도 완료" 추적 — API 실패해도 영구 skeleton 방지
  const [fetchedRiotIds, setFetchedRiotIds] = useState<Set<string>>(new Set());

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const data = await window.lol.getChampSelectFull();
      setLcuError(false);
      setState(data);
    } catch {
      setLcuError(true);
      setState(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  // 자동 새로고침 (2초 — AC #1)
  useEffect(() => {
    const t = setInterval(load, 2_000);
    return () => clearInterval(t);
  }, [load]);

  // 양팀 플레이어 데이터 fetch (P-1: cancellation, P-2: fetchedRiotIds, P-3: 단일 fetch, P-5: .catch)
  useEffect(() => {
    if (!state) return;

    const allSlots = [...state.myTeam, ...state.theirTeam];
    const riotIds = allSlots.map(s => s.riotId).filter(Boolean);
    if (riotIds.length === 0) return;

    // P-1: 취소 플래그 — 빠른 state 교체 시 stale write 방지
    let cancelled = false;

    Promise.all(
      riotIds.map(async (riotId) => {
        try {
          const res = await fetch(`${API}/stats/player/${encodeURIComponent(riotId)}?mode=all`);
          if (!res.ok) return null;
          const json = await res.json() as { data: PlayerData };
          return [riotId, json.data] as [string, PlayerData];
        } catch { return null; }
      })
    ).then(results => {
      if (cancelled) return;
      const map: Record<string, PlayerData> = {};
      for (const r of results) if (r) map[r[0]] = r[1];
      setPlayerDetails(map);
      // P-2: 시도한 riotId 기록 (실패 포함)
      setFetchedRiotIds(new Set(riotIds));
    }).catch(() => { /* ignore */ }); // P-5

    return () => { cancelled = true; };
  }, [state?.myTeam.map(s => s.riotId).join(','), state?.theirTeam.map(s => s.riotId).join(',')]);

  const tabs = [
    { key: 'ban',     label: '밴 추천' },
    { key: 'pick',    label: '내 픽 추천' },
    { key: 'counter', label: '카운터픽' },
  ] as const;

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">챔피언 선택</h1>
        <p className="page-subtitle">상대 분석 · 밴 추천 · 카운터픽</p>
      </div>

      {/* 상태바 */}
      <div className="card" style={{ marginBottom: 'var(--spacing-md)' }}>
        <div className="card-header">
          <div style={{ display: 'flex', gap: 6 }}>
            {tabs.map(t => (
              <button
                key={t.key}
                className={`btn btn-sm ${tab === t.key ? 'btn-primary' : 'btn-secondary'}`}
                onClick={() => setTab(t.key)}
              >
                {t.label}
              </button>
            ))}
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            {state && (
              <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)' }}>
                {state.phase} {state.timer > 0 ? `⏱ ${state.timer}s` : ''}
              </span>
            )}
            <button className="btn btn-secondary btn-sm" onClick={load} disabled={loading}>
              <RefreshCw size={12} style={{ animation: loading ? 'spin 1s linear infinite' : 'none' }} />
            </button>
          </div>
        </div>

        {/* AC #3: LCU 미연결 안내 */}
        {lcuError && (
          <p style={{ color: 'var(--color-text-secondary)', fontSize: 'var(--font-size-sm)' }}>
            LoL 클라이언트를 실행해주세요
          </p>
        )}
        {!lcuError && !state && !loading && (
          <p style={{ color: 'var(--color-text-secondary)', fontSize: 'var(--font-size-sm)' }}>
            챔피언 선택 화면이 아닙니다
          </p>
        )}
      </div>

      {state && (
        <>
          {/* AC #2: grid-cols-2 PlayerCard 그리드 */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--spacing-md)', marginBottom: 'var(--spacing-md)' }}>
            {/* 좌측: 우리팀 */}
            <div className="card">
              <div className="card-header">
                <span className="card-title" style={{ color: 'var(--color-info)' }}>우리팀</span>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--spacing-sm)' }}>
                {state.myTeam.map(slot => (
                  <PlayerCard
                    key={slot.cellId}
                    riotId={slot.riotId || slot.summonerName || `Player ${slot.cellId}`}
                    data={slot.riotId ? (playerDetails[slot.riotId] ?? null) : null}
                    // P-2: fetchedRiotIds로 "로딩 중" vs "데이터 없음" 구분
                    loading={loading || (Boolean(slot.riotId) && !fetchedRiotIds.has(slot.riotId))}
                  />
                ))}
              </div>
            </div>

            {/* 우측: 상대팀 */}
            <div className="card">
              <div className="card-header">
                <span className="card-title" style={{ color: 'var(--color-error)' }}>상대팀</span>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--spacing-sm)' }}>
                {state.theirTeam.map(slot => (
                  <PlayerCard
                    key={slot.cellId}
                    riotId={slot.riotId || slot.summonerName || `Player ${slot.cellId}`}
                    data={slot.riotId ? (playerDetails[slot.riotId] ?? null) : null}
                    loading={loading || (Boolean(slot.riotId) && !fetchedRiotIds.has(slot.riotId))}
                  />
                ))}
              </div>
            </div>
          </div>

          {/* 분석 탭 */}
          {tab === 'ban'     && <BanRecommendSection enemies={state.theirTeam} playerDetails={playerDetails} />}
          {tab === 'pick'    && <MyPickSection myTeam={state.myTeam} bans={state.bans} playerDetails={playerDetails} />}
          {tab === 'counter' && <CounterSection enemies={state.theirTeam} />}

          {/* 밴 목록 */}
          {state.bans.length > 0 && (
            <div className="card" style={{ marginTop: 'var(--spacing-md)' }}>
              <div className="card-header">
                <span className="card-title">밴 목록</span>
              </div>
              <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                {state.bans.map((ban, i) => (
                  <div key={i} style={{
                    display: 'flex', alignItems: 'center', gap: 4,
                    padding: '3px 8px', borderRadius: 'var(--radius-sm)',
                    background: ban.team === 'blue' ? 'rgba(59,158,255,0.1)' : 'rgba(232,64,64,0.1)',
                    border: `1px solid ${ban.team === 'blue' ? 'rgba(59,158,255,0.3)' : 'rgba(232,64,64,0.3)'}`,
                  }}>
                    <ChampIcon id={ban.championId} size={20} />
                  </div>
                ))}
              </div>
            </div>
          )}
        </>
      )}

      <style>{`@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }`}</style>
    </div>
  );
}
