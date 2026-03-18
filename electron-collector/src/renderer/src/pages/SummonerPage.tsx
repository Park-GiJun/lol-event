import { useState, useRef } from 'react';
import { Search, ChevronDown, ChevronUp } from 'lucide-react';

const API = 'https://api.gijun.net/api';

interface PlayerStats { riotId: string; games: number; wins: number; }
interface StatsListResult { stats: PlayerStats[]; }

interface ChampionStat {
  champion: string; championId: number;
  games: number; wins: number; winRate: number;
  avgKills: number; avgDeaths: number; avgAssists: number; kda: number;
}
interface RecentMatch {
  matchId: string; champion: string; championId: number;
  win: boolean; kills: number; deaths: number; assists: number;
  damage: number; cs: number; gold: number;
  gameCreation: number; gameDuration: number; queueId: number;
}
interface PlayerDetail {
  riotId: string; games: number; wins: number; losses: number; winRate: number;
  avgKills: number; avgDeaths: number; avgAssists: number; kda: number;
  championStats: ChampionStat[];
  recentMatches: RecentMatch[];
}

interface MatchParticipant {
  riotId: string; champion: string; championId: number;
  team: 'blue' | 'red'; win: boolean;
  kills: number; deaths: number; assists: number;
  damage: number; cs: number; gold: number;
  item0: number; item1: number; item2: number; item3: number; item4: number; item5: number; item6: number;
}
interface MatchDetail {
  matchId: string; queueId: number; gameCreation: number; gameDuration: number;
  participants: MatchParticipant[];
}

async function apiFetch<T>(path: string): Promise<T> {
  const res = await fetch(`${API}${path}`);
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  const json = await res.json();
  return json.data as T;
}

const QUEUE_LABEL: Record<number, string> = { 0: '커스텀', 3130: '5v5 내전', 3270: '칼바람' };

function fmt(secs: number) {
  return `${Math.floor(secs / 60)}:${String(secs % 60).padStart(2, '0')}`;
}

function champIcon(championId: number, size = 28) {
  return (
    <img
      src={`https://raw.communitydragon.org/latest/plugins/rcp-be-lol-game-data/global/default/v1/champion-icons/${championId}.png`}
      width={size} height={size}
      style={{ borderRadius: 4, border: '1px solid var(--color-border)', flexShrink: 0 }}
      onError={e => { (e.target as HTMLImageElement).style.display = 'none'; }}
      alt=""
    />
  );
}

function WrColor(wr: number) {
  return wr >= 60 ? 'var(--color-win)' : wr >= 50 ? 'var(--color-primary)' : 'var(--color-loss)';
}

// ── 매치 확장 카드 ─────────────────────────────────────
function MatchExpandedView({ matchId, searchedRiotId }: { matchId: string; searchedRiotId: string }) {
  const [detail, setDetail] = useState<MatchDetail | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const fetched = useRef(false);

  if (!fetched.current) {
    fetched.current = true;
    setLoading(true);
    apiFetch<MatchDetail>(`/matches/${encodeURIComponent(matchId)}`)
      .then(setDetail)
      .catch(() => setError('매치 정보 로드 실패'))
      .finally(() => setLoading(false));
  }

  if (loading) return <div style={{ padding: '8px 0', color: 'var(--color-text-secondary)', fontSize: 11 }}>로딩 중...</div>;
  if (error || !detail) return <div style={{ padding: '8px 0', color: 'var(--color-error)', fontSize: 11 }}>{error ?? '데이터 없음'}</div>;

  const blue = detail.participants.filter(p => p.team === 'blue');
  const red  = detail.participants.filter(p => p.team === 'red');
  const blueWin = blue[0]?.win ?? false;

  const TeamRow = ({ p }: { p: MatchParticipant }) => {
    const isMe = p.riotId === searchedRiotId;
    const kdaVal = p.deaths === 0 ? '완벽' : ((p.kills + p.assists) / p.deaths).toFixed(2);
    return (
      <div style={{
        display: 'flex', alignItems: 'center', gap: 6,
        padding: '3px 0',
        background: isMe ? 'rgba(200,155,60,0.08)' : 'transparent',
        borderRadius: 3,
      }}>
        {champIcon(p.championId, 22)}
        <span style={{
          flex: 1, fontSize: 11, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
          color: isMe ? 'var(--color-primary)' : 'var(--color-text-primary)',
          fontWeight: isMe ? 700 : 400,
        }}>{p.riotId.split('#')[0]}</span>
        <span style={{ fontSize: 10, color: 'var(--color-text-secondary)', whiteSpace: 'nowrap' }}>
          <span style={{ color: 'var(--color-text-primary)' }}>{p.kills}</span>
          <span style={{ color: 'var(--color-text-disabled)' }}>/</span>
          <span style={{ color: 'var(--color-error)' }}>{p.deaths}</span>
          <span style={{ color: 'var(--color-text-disabled)' }}>/</span>
          <span style={{ color: 'var(--color-text-primary)' }}>{p.assists}</span>
          <span style={{ color: 'var(--color-text-disabled)', marginLeft: 4 }}>{kdaVal}</span>
        </span>
        <span style={{ fontSize: 10, color: 'var(--color-text-disabled)', whiteSpace: 'nowrap', minWidth: 36, textAlign: 'right' }}>
          {p.cs}CS
        </span>
        <span style={{ fontSize: 10, color: 'var(--color-text-disabled)', whiteSpace: 'nowrap', minWidth: 40, textAlign: 'right' }}>
          {(p.damage / 1000).toFixed(1)}k딜
        </span>
      </div>
    );
  };

  return (
    <div style={{ marginTop: 8, paddingTop: 8, borderTop: '1px solid var(--color-border)' }}>
      <div style={{ display: 'flex', gap: 12 }}>
        {/* 블루팀 */}
        <div style={{ flex: 1 }}>
          <div style={{ fontSize: 10, fontWeight: 700, color: blueWin ? 'var(--color-win)' : 'var(--color-loss)', marginBottom: 4 }}>
            🔵 블루팀 {blueWin ? '승' : '패'}
          </div>
          {blue.map(p => <TeamRow key={p.riotId} p={p} />)}
        </div>
        {/* 레드팀 */}
        <div style={{ flex: 1 }}>
          <div style={{ fontSize: 10, fontWeight: 700, color: !blueWin ? 'var(--color-win)' : 'var(--color-loss)', marginBottom: 4 }}>
            🔴 레드팀 {!blueWin ? '승' : '패'}
          </div>
          {red.map(p => <TeamRow key={p.riotId} p={p} />)}
        </div>
      </div>
    </div>
  );
}

// ── 매치 행 ────────────────────────────────────────────
function MatchRow({ m, searchedRiotId }: { m: RecentMatch; searchedRiotId: string }) {
  const [expanded, setExpanded] = useState(false);
  const kda = m.deaths === 0 ? '완벽' : ((m.kills + m.assists) / m.deaths).toFixed(2);
  const date = new Date(m.gameCreation).toLocaleDateString('ko-KR', { month: 'numeric', day: 'numeric' });
  const wrColor = m.win ? 'var(--color-win)' : 'var(--color-loss)';

  return (
    <div style={{
      borderRadius: 6,
      border: `1px solid ${m.win ? 'rgba(11,196,180,0.25)' : 'rgba(232,64,64,0.2)'}`,
      background: m.win ? 'rgba(11,196,180,0.04)' : 'rgba(232,64,64,0.04)',
      marginBottom: 6,
      overflow: 'hidden',
    }}>
      {/* 헤더 행 */}
      <div
        style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '7px 10px', cursor: 'pointer' }}
        onClick={() => setExpanded(v => !v)}
      >
        {/* 승패 */}
        <span style={{ fontSize: 12, fontWeight: 700, color: wrColor, width: 18, flexShrink: 0 }}>
          {m.win ? '승' : '패'}
        </span>

        {/* 챔피언 아이콘 */}
        {champIcon(m.championId, 28)}

        {/* K/D/A + KDA */}
        <div style={{ flex: 1 }}>
          <span style={{ fontSize: 12, fontWeight: 600 }}>
            {m.kills} / <span style={{ color: 'var(--color-error)' }}>{m.deaths}</span> / {m.assists}
          </span>
          <span style={{ fontSize: 10, color: 'var(--color-text-secondary)', marginLeft: 6 }}>
            {kda} KDA
          </span>
        </div>

        {/* CS · 딜 */}
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 1 }}>
          <span style={{ fontSize: 10, color: 'var(--color-text-secondary)' }}>CS {m.cs}</span>
          <span style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>{(m.damage / 1000).toFixed(1)}k딜</span>
        </div>

        {/* 날짜 · 시간 */}
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 1, minWidth: 52 }}>
          <span style={{ fontSize: 10, color: 'var(--color-text-secondary)' }}>{date}</span>
          <span style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>{fmt(m.gameDuration)}</span>
        </div>

        {/* 큐 */}
        <span style={{ fontSize: 10, color: 'var(--color-text-disabled)', minWidth: 36, textAlign: 'right' }}>
          {QUEUE_LABEL[m.queueId] ?? `Q${m.queueId}`}
        </span>

        {/* 토글 아이콘 */}
        {expanded
          ? <ChevronUp size={12} style={{ color: 'var(--color-text-disabled)', flexShrink: 0 }} />
          : <ChevronDown size={12} style={{ color: 'var(--color-text-disabled)', flexShrink: 0 }} />
        }
      </div>

      {/* 확장 영역 */}
      {expanded && (
        <div style={{ padding: '0 10px 10px' }}>
          <MatchExpandedView matchId={m.matchId} searchedRiotId={searchedRiotId} />
        </div>
      )}
    </div>
  );
}

// ── 메인 페이지 ────────────────────────────────────────
export function SummonerPage() {
  const [query, setQuery]           = useState('');
  const [searching, setSearching]   = useState(false);
  const [candidates, setCandidates] = useState<string[]>([]);
  const [selected, setSelected]     = useState<string | null>(null);
  const [detail, setDetail]         = useState<PlayerDetail | null>(null);
  const [loadingDetail, setLoadingDetail] = useState(false);
  const [searchError, setSearchError] = useState<string | null>(null);

  const doSearch = async () => {
    const q = query.trim().toLowerCase();
    if (!q) return;
    setSearching(true);
    setSearchError(null);
    setCandidates([]);
    setSelected(null);
    setDetail(null);
    try {
      const result = await apiFetch<StatsListResult>('/stats?mode=all');
      const matched = result.stats
        .map(p => p.riotId)
        .filter(id => id.split('#')[0].toLowerCase().includes(q));
      if (matched.length === 0) {
        setSearchError('검색 결과 없음');
      } else if (matched.length === 1) {
        setCandidates(matched);
        selectPlayer(matched[0]);
      } else {
        setCandidates(matched);
      }
    } catch {
      setSearchError('검색 실패');
    } finally {
      setSearching(false);
    }
  };

  const selectPlayer = async (riotId: string) => {
    setSelected(riotId);
    setLoadingDetail(true);
    setDetail(null);
    try {
      const d = await apiFetch<PlayerDetail>(`/stats/player/${encodeURIComponent(riotId)}?mode=all`);
      setDetail(d);
    } catch {
      setSearchError('전적 로드 실패');
    } finally {
      setLoadingDetail(false);
    }
  };

  const winColor = detail ? WrColor(detail.winRate) : 'var(--color-text-primary)';

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      {/* 헤더 */}
      <div className="page-header">
        <h1 className="page-title">소환사 검색</h1>
        <p className="page-subtitle">닉네임으로 내전 전적 조회</p>
      </div>

      {/* 검색창 */}
      <div className="card" style={{ marginBottom: 12 }}>
        <div style={{ display: 'flex', gap: 8 }}>
          <input
            value={query}
            onChange={e => setQuery(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && doSearch()}
            placeholder="닉네임 입력 (태그 제외)"
            style={{
              flex: 1, padding: '7px 12px',
              background: 'var(--color-bg-hover)', border: '1px solid var(--color-border)',
              borderRadius: 'var(--radius-md)', color: 'var(--color-text-primary)',
              fontSize: 'var(--font-size-sm)', outline: 'none',
            }}
          />
          <button className="btn btn-primary btn-sm" onClick={doSearch} disabled={searching || !query.trim()}>
            <Search size={12} />
            {searching ? '검색 중...' : '검색'}
          </button>
        </div>
        {searchError && (
          <p style={{ color: 'var(--color-error)', fontSize: 'var(--font-size-xs)', marginTop: 8 }}>{searchError}</p>
        )}
      </div>

      {/* 후보 목록 (여러 명 검색된 경우) */}
      {candidates.length > 1 && (
        <div className="card" style={{ marginBottom: 12 }}>
          <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)', marginBottom: 8 }}>
            {candidates.length}명 검색됨 — 선택하세요
          </div>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
            {candidates.map(id => (
              <button key={id}
                onClick={() => selectPlayer(id)}
                style={{
                  padding: '4px 10px', borderRadius: 'var(--radius-sm)',
                  background: selected === id ? 'var(--color-primary)' : 'var(--color-bg-hover)',
                  color: selected === id ? 'var(--color-text-inverse)' : 'var(--color-text-primary)',
                  border: `1px solid ${selected === id ? 'var(--color-primary)' : 'var(--color-border)'}`,
                  fontSize: 'var(--font-size-xs)', fontWeight: selected === id ? 700 : 400,
                  cursor: 'pointer',
                }}
              >
                {id}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* 결과 영역 (스크롤 가능) */}
      <div style={{ flex: 1, overflowY: 'auto' }}>
        {loadingDetail && (
          <div className="card" style={{ textAlign: 'center', color: 'var(--color-text-secondary)', fontSize: 'var(--font-size-sm)' }}>
            전적 조회 중...
          </div>
        )}

        {detail && !loadingDetail && (
          <>
            {/* 요약 배너 */}
            <div className="card" style={{ marginBottom: 12 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2 }}>
                  <span style={{ fontSize: 24, fontWeight: 800, color: winColor }}>{detail.winRate}%</span>
                  <span style={{ fontSize: 10, color: 'var(--color-text-secondary)' }}>
                    <span style={{ color: 'var(--color-win)' }}>{detail.wins}W</span>
                    {' '}<span style={{ color: 'var(--color-loss)' }}>{detail.losses}L</span>
                  </span>
                </div>
                <div style={{ width: 1, height: 36, background: 'var(--color-border)' }} />
                <div>
                  <div style={{ fontSize: 18, fontWeight: 700 }}>{detail.kda.toFixed(2)} KDA</div>
                  <div style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>
                    {detail.avgKills.toFixed(1)} /&nbsp;
                    <span style={{ color: 'var(--color-error)' }}>{detail.avgDeaths.toFixed(1)}</span>
                    &nbsp;/ {detail.avgAssists.toFixed(1)}
                  </div>
                </div>
                <div style={{ marginLeft: 'auto', textAlign: 'right' }}>
                  <div style={{ fontSize: 'var(--font-size-md)', fontWeight: 700 }}>{detail.riotId.split('#')[0]}</div>
                  <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-disabled)' }}>
                    #{detail.riotId.split('#')[1]} · {detail.games}판
                  </div>
                </div>
              </div>
            </div>

            {/* 챔피언 통계 */}
            {detail.championStats.length > 0 && (
              <div className="card" style={{ marginBottom: 12 }}>
                <div style={{ fontWeight: 700, fontSize: 'var(--font-size-sm)', marginBottom: 10 }}>챔피언별 통계</div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                  {detail.championStats.map(c => {
                    const wrC = WrColor(c.winRate);
                    const kdaC = c.kda >= 5 ? 'var(--color-win)' : c.kda >= 3 ? 'var(--color-primary)' : 'var(--color-text-primary)';
                    return (
                      <div key={c.champion} style={{
                        display: 'flex', alignItems: 'center', gap: 8,
                        padding: '5px 8px', borderRadius: 'var(--radius-sm)',
                        background: 'var(--color-bg-hover)',
                      }}>
                        {champIcon(c.championId, 26)}
                        <span style={{ flex: 1, fontSize: 12, fontWeight: 600 }}>{c.champion}</span>
                        <span style={{ fontSize: 11, color: 'var(--color-text-secondary)', minWidth: 36 }}>
                          {c.games}판
                        </span>
                        {/* 승률 바 */}
                        <div style={{ display: 'flex', alignItems: 'center', gap: 5, minWidth: 80 }}>
                          <div style={{ flex: 1, height: 4, background: 'var(--color-border)', borderRadius: 2, overflow: 'hidden' }}>
                            <div style={{ width: `${c.winRate}%`, height: '100%', background: wrC, borderRadius: 2 }} />
                          </div>
                          <span style={{ fontSize: 10, fontWeight: 700, color: wrC, minWidth: 28 }}>{c.winRate}%</span>
                        </div>
                        <span style={{ fontSize: 11, fontWeight: 700, color: kdaC, minWidth: 38, textAlign: 'right' }}>
                          {c.kda.toFixed(2)}
                        </span>
                        <span style={{ fontSize: 10, color: 'var(--color-text-secondary)', minWidth: 68, textAlign: 'right' }}>
                          {c.avgKills.toFixed(1)}/
                          <span style={{ color: 'var(--color-error)' }}>{c.avgDeaths.toFixed(1)}</span>
                          /{c.avgAssists.toFixed(1)}
                        </span>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}

            {/* 최근 경기 */}
            {detail.recentMatches.length > 0 && (
              <div className="card" style={{ marginBottom: 12 }}>
                <div style={{ fontWeight: 700, fontSize: 'var(--font-size-sm)', marginBottom: 10 }}>
                  최근 경기 ({detail.recentMatches.length}게임)
                </div>
                {detail.recentMatches.map(m => (
                  <MatchRow key={`${m.matchId}-${m.champion}`} m={m} searchedRiotId={detail.riotId} />
                ))}
              </div>
            )}
          </>
        )}
      </div>

      <style>{`@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }`}</style>
    </div>
  );
}
