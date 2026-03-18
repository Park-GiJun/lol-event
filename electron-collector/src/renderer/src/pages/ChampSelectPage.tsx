import { useEffect, useState, useCallback } from 'react';
import { RefreshCw, Shield, Swords, Star } from 'lucide-react';

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

interface PlayerChampStat { champion: string; championId: number; games: number; wins: number; winRate: number; }
interface PlayerDetail { riotId: string; games: number; wins: number; winRate: number; championStats: PlayerChampStat[]; }

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
  if (!id) return <div style={{ width: size, height: size, borderRadius: 4, background: 'var(--color-surface-2)' }} />;
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

// 상대 챔피언별 카운터픽 분석
function CounterSection({ enemies }: { enemies: ChampSlot[] }) {
  const [results, setResults] = useState<Record<string, ChampionMatchupResult>>({});
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const champIds = enemies.map(e => e.championId).filter(id => id > 0);
    if (champIds.length === 0) return;

    setLoading(true);
    // 먼저 champId → champName 매핑 필요: DDragon에서 가져옴
    fetch(`${DDRAGON}/data/ko_KR/champion.json`)
      .then(r => r.json())
      .then((json: { data: Record<string, { key: string; id: string; name: string }> }) => {
        const idToName: Record<number, string> = {};
        for (const champ of Object.values(json.data)) {
          idToName[parseInt(champ.key)] = champ.id; // 영문 id (Jinx, etc.)
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
            const res = await fetch(`${API}/stats/matchup?vsChampion=${encodeURIComponent(champName)}&mode=normal`);
            if (!res.ok) continue;
            const json = await res.json() as { data: ChampionMatchupResult };
            entries.push([String(e.championId), json.data]);
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
        {loading && <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)' }}>분석 중...</span>}
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
                    background: 'var(--color-surface-2)', border: '1px solid var(--color-border)',
                  }}>
                    <ChampIcon id={m.opponentId} size={22} />
                    <div>
                      <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-primary)' }}>{m.opponent}</div>
                      <div style={{ fontSize: 10, color: winRateColor(m.winRate) }}>{m.winRate}% ({m.games}판)</div>
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

// 상대 플레이어별 밴 추천 (최다픽 챔피언)
function BanRecommendSection({ enemies }: { enemies: ChampSlot[] }) {
  const [players, setPlayers] = useState<Record<string, PlayerDetail>>({});
  const [loading, setLoading] = useState(false);

  const riotIds = enemies.map(e => e.riotId).filter(Boolean);

  useEffect(() => {
    if (riotIds.length === 0) return;
    setLoading(true);
    Promise.all(
      riotIds.map(async (riotId) => {
        try {
          const res = await fetch(`${API}/stats/player/${encodeURIComponent(riotId)}?mode=all`);
          if (!res.ok) return null;
          const json = await res.json() as { data: PlayerDetail };
          return [riotId, json.data] as [string, PlayerDetail];
        } catch { return null; }
      })
    )
      .then(results => {
        const map: Record<string, PlayerDetail> = {};
        for (const r of results) if (r) map[r[0]] = r[1];
        setPlayers(map);
      })
      .finally(() => setLoading(false));
  }, [riotIds.join(',')]);

  const hasRiotIds = riotIds.length > 0;

  return (
    <div className="card" style={{ marginBottom: 'var(--spacing-md)' }}>
      <div className="card-header">
        <span className="card-title"><Shield size={14} style={{ marginRight: 6 }} />밴 추천</span>
        {loading && <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)' }}>분석 중...</span>}
      </div>
      {!hasRiotIds && (
        <p style={{ color: 'var(--color-text-secondary)', fontSize: 'var(--font-size-sm)' }}>
          상대팀 소환사 정보를 불러오는 중...
        </p>
      )}
      {enemies.filter(e => e.riotId).map(e => {
        const player = players[e.riotId];
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
                {top3.map((c, idx) => (
                  <div key={c.champion} style={{
                    display: 'flex', alignItems: 'center', gap: 6,
                    padding: '4px 8px', borderRadius: 'var(--radius-sm)',
                    background: idx === 0 ? 'rgba(232,64,64,0.1)' : 'var(--color-surface-2)',
                    border: `1px solid ${idx === 0 ? 'rgba(232,64,64,0.3)' : 'var(--color-border)'}`,
                  }}>
                    <ChampIcon id={c.championId} size={22} />
                    <div>
                      <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-primary)' }}>{c.champion}</div>
                      <div style={{ fontSize: 10, color: winRateColor(c.winRate) }}>{c.winRate}% ({c.games}판)</div>
                    </div>
                    {idx === 0 && <Star size={10} style={{ color: 'var(--color-warning)' }} />}
                  </div>
                ))}
              </div>
            ) : !loading ? (
              <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-disabled)' }}>내전 데이터 없음</span>
            ) : null}
          </div>
        );
      })}
    </div>
  );
}

// 팀 구성 요약
function TeamRow({ slot, color }: { slot: ChampSlot; color: string }) {
  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 8,
      padding: '5px 0', borderBottom: '1px solid var(--color-border)',
    }}>
      <ChampIcon id={slot.championId} size={26} />
      <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)', width: 36 }}>
        {positionLabel[slot.assignedPosition?.toLowerCase() ?? ''] ?? '—'}
      </span>
      <span style={{ flex: 1, fontSize: 'var(--font-size-sm)', color: slot.isMe ? 'var(--color-primary)' : 'var(--color-text-primary)' }}>
        {slot.riotId || slot.summonerName || `Player`}
        {slot.isMe && <span style={{ marginLeft: 4, fontSize: 10, color: 'var(--color-primary)' }}>◀ 나</span>}
      </span>
      {!slot.championId && (
        <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-disabled)' }}>선택 중</span>
      )}
    </div>
  );
}

export function ChampSelectPage() {
  const [state, setState] = useState<ChampSelectFull | null>(null);
  const [loading, setLoading] = useState(false);
  const [tab, setTab] = useState<'counter' | 'ban' | 'teams'>('counter');

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const data = await window.lol.getChampSelectFull();
      setState(data);
    } catch {
      setState(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  // 자동 새로고침 (10초)
  useEffect(() => {
    const t = setInterval(load, 10_000);
    return () => clearInterval(t);
  }, [load]);

  const tabs = [
    { key: 'counter', label: '카운터픽' },
    { key: 'ban', label: '밴 추천' },
    { key: 'teams', label: '팀 구성' },
  ] as const;

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">챔피언 선택</h1>
        <p className="page-subtitle">상대 분석 · 카운터픽 · 밴 추천</p>
      </div>

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
        {!state && !loading && (
          <p style={{ color: 'var(--color-text-secondary)', fontSize: 'var(--font-size-sm)' }}>
            챔피언 선택 화면이 아닙니다
          </p>
        )}
      </div>

      {state && (
        <>
          {tab === 'counter' && <CounterSection enemies={state.theirTeam} />}
          {tab === 'ban' && <BanRecommendSection enemies={state.theirTeam} />}
          {tab === 'teams' && (
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--spacing-md)' }}>
              <div className="card">
                <div className="card-header">
                  <span className="card-title" style={{ color: 'var(--color-info)' }}>블루팀 (내 팀)</span>
                </div>
                {state.myTeam.map(s => <TeamRow key={s.cellId} slot={s} color="var(--color-info)" />)}
              </div>
              <div className="card">
                <div className="card-header">
                  <span className="card-title" style={{ color: 'var(--color-error)' }}>레드팀 (상대)</span>
                </div>
                {state.theirTeam.map(s => <TeamRow key={s.cellId} slot={s} color="var(--color-error)" />)}
              </div>
            </div>
          )}

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
