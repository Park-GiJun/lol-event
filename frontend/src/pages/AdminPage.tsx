import { useState, useEffect, useRef } from 'react';
import { Shield, Lock, RefreshCw, Play, Users, Trophy, Zap, X, Trash2, BarChart2 } from 'lucide-react';
import { api } from '../lib/api/api';
import type {
  StatsResponse,
  PlayerStats,
  MvpStatsResult,
  MvpPlayerStat,
  DuoStatsResult,
  DuoStat,
  PlayerDetailStats,
  LaneStat,
} from '../lib/types/stats';
import { PlayerLink } from '../components/common/PlayerLink';
import '../styles/pages/monitoring.css';
import '../styles/pages/stats.css';

const ADMIN_PASSWORD = 'admin1234';
const SESSION_KEY = 'monitoring_auth';

interface BatchStatus {
  playerSnapshotCount: number;
  championSnapshotCount: number;
  championItemSnapshotCount: number;
  lastAggregatedAt: string | null;
  message: string;
}

type TeamKey = 'pool' | 'team1' | 'team2' | 'team3' | 'team4';
type TeamMap = Record<TeamKey, string[]>;

const TEAM_META: Record<Exclude<TeamKey, 'pool'>, { label: string; main: string; bg: string; border: string }> = {
  team1: { label: '팀 1', main: '#4A9EFF', bg: 'rgba(74,158,255,0.08)',  border: 'rgba(74,158,255,0.35)' },
  team2: { label: '팀 2', main: '#FF6B6B', bg: 'rgba(255,107,107,0.08)', border: 'rgba(255,107,107,0.35)' },
  team3: { label: '팀 3', main: '#51CF66', bg: 'rgba(81,207,102,0.08)',  border: 'rgba(81,207,102,0.35)' },
  team4: { label: '팀 4', main: '#CC5DE8', bg: 'rgba(204,93,232,0.08)',  border: 'rgba(204,93,232,0.35)' },
};

const POS_ORDER = ['TOP', 'JUNGLE', 'MID', 'BOTTOM', 'SUPPORT'] as const;
const POS_LABELS: Record<string, string> = {
  TOP: 'TOP', JUNGLE: 'JGL', MID: 'MID', BOTTOM: 'BOT', SUPPORT: 'SUP',
};

function calcPosScore(lane: LaneStat): number {
  const kda = (lane.avgKills * 3 + lane.avgAssists * 1.5) / Math.max(lane.avgDeaths, 1);
  const winBonus = (lane.winRate / 100) * 20;
  const vision = lane.avgVisionScore * 0.3;
  const cs = (lane.avgCs / 30) * 0.5;
  return Math.round((kda + winBonus + vision + cs) * 10) / 10;
}

function calcExpectedWR(members: string[], allStats: PlayerStats[], duos: DuoStat[]): number {
  if (!members.length) return 0;
  const base =
    members.reduce((sum, id) => sum + (allStats.find(s => s.riotId === id)?.winRate ?? 50), 0) /
    members.length / 100;
  const duoRates: number[] = [];
  for (let i = 0; i < members.length; i++) {
    for (let j = i + 1; j < members.length; j++) {
      const [a, b] = [members[i], members[j]];
      const d = duos.find(x => (x.player1 === a && x.player2 === b) || (x.player1 === b && x.player2 === a));
      if (d) duoRates.push(d.winRate / 100);
    }
  }
  const synergy = duoRates.length
    ? (duoRates.reduce((a, b) => a + b, 0) / duoRates.length - 0.5) * 0.3
    : 0;
  return Math.min(Math.max(base + synergy, 0.05), 0.95);
}

const thS: React.CSSProperties = {
  padding: '8px 12px', textAlign: 'left', fontSize: 11, fontWeight: 700,
  color: 'var(--color-text-secondary)', textTransform: 'uppercase', whiteSpace: 'nowrap',
};
const tdS: React.CSSProperties = { padding: '10px 12px', verticalAlign: 'middle' };


export function AdminPage() {
  const [authed, setAuthed] = useState(() => sessionStorage.getItem(SESSION_KEY) === 'true');
  const [pw, setPw] = useState('');
  const [authErr, setAuthErr] = useState('');
  const inputRef = useRef<HTMLInputElement>(null);

  // batch
  const [batchStatus, setBatchStatus] = useState<BatchStatus | null>(null);
  const [triggering, setTriggering] = useState(false);
  const [triggerMsg, setTriggerMsg] = useState('');
  const [triggeringItems, setTriggeringItems] = useState(false);
  const [triggerItemMsg, setTriggerItemMsg] = useState('');
  const [clearingCache, setClearingCache] = useState(false);
  const [clearCacheMsg, setClearCacheMsg] = useState('');

  // elo reset
  const [eloResetting, setEloResetting] = useState(false);
  const [eloResetMsg, setEloResetMsg] = useState('');
  const [eloLeaderboard, setEloLeaderboard] = useState<{ rank: number; riotId: string; elo: number; games: number }[]>([]);
  const [eloLoading, setEloLoading] = useState(false);

  // data
  const [allStats, setAllStats] = useState<PlayerStats[]>([]);
  const [mvpStats, setMvpStats] = useState<MvpPlayerStat[]>([]);
  const [duoData, setDuoData] = useState<DuoStat[]>([]);
  const [posMap, setPosMap] = useState<Record<string, LaneStat[]>>({});
  const [posLoading, setPosLoading] = useState(false);
  const [posLoaded, setPosLoaded] = useState(false);

  // team builder
  const [teams, setTeams] = useState<TeamMap>({ pool: [], team1: [], team2: [], team3: [], team4: [] });
  const [dragOver, setDragOver] = useState<TeamKey | null>(null);

  useEffect(() => {
    if (!authed) { inputRef.current?.focus(); return; }
    loadAll();
  }, [authed]);

  // allStats 로드 완료 시 pool 초기화
  useEffect(() => {
    if (allStats.length) {
      setTeams(prev => ({ ...prev, pool: allStats.map(s => s.riotId) }));
    }
  }, [allStats]);

  async function loadAll() {
    const [sr, mr, dr, br] = await Promise.allSettled([
      api.get<StatsResponse>('/stats'),
      api.get<MvpStatsResult>('/stats/mvp'),
      api.get<DuoStatsResult>('/stats/duo?minGames=1'),
      api.get<BatchStatus>('/batch/status'),
    ]);
    if (sr.status === 'fulfilled') setAllStats((sr.value as StatsResponse).stats);
    if (mr.status === 'fulfilled') setMvpStats((mr.value as MvpStatsResult).rankings);
    if (dr.status === 'fulfilled') setDuoData((dr.value as DuoStatsResult).duos);
    if (br.status === 'fulfilled') setBatchStatus(br.value as BatchStatus);
    loadEloLeaderboard();
  }

  async function loadEloLeaderboard() {
    setEloLoading(true);
    try {
      const res = await api.get<{ players: { rank: number; riotId: string; elo: number; games: number }[] }>('/stats/elo');
      setEloLeaderboard(res.players);
    } catch { /* 조용히 실패 */ }
    finally { setEloLoading(false); }
  }

  async function resetElo() {
    if (!window.confirm('전체 Elo 데이터를 초기화하고 전체 매치를 재집계합니다.\n매치 수에 따라 시간이 걸릴 수 있습니다. 계속하시겠습니까?')) return;
    setEloResetting(true);
    setEloResetMsg('');
    try {
      await api.post('/admin/elo/reset', {});
      setEloResetMsg('Elo 재집계 완료! 리더보드를 새로고침합니다...');
      await loadEloLeaderboard();
    } catch {
      setEloResetMsg('Elo 재집계에 실패했습니다.');
    } finally {
      setEloResetting(false);
    }
  }

  async function loadPositions() {
    if (posLoaded || posLoading || !allStats.length) return;
    setPosLoading(true);
    const results = await Promise.allSettled(
      allStats.map(s =>
        api.get<PlayerDetailStats>(`/stats/player/${encodeURIComponent(s.riotId)}`)
          .then(r => ({ riotId: s.riotId, laneStats: r.laneStats })),
      ),
    );
    const map: Record<string, LaneStat[]> = {};
    results.forEach(r => { if (r.status === 'fulfilled') map[r.value.riotId] = r.value.laneStats; });
    setPosMap(map);
    setPosLoaded(true);
    setPosLoading(false);
  }

  async function triggerBatch() {
    setTriggering(true); setTriggerMsg('');
    try {
      await api.post('/batch/trigger', {});
      setTriggerMsg('배치 실행 요청이 전송되었습니다.');
      setTimeout(async () => setBatchStatus(await api.get<BatchStatus>('/batch/status')), 2000);
    } catch { setTriggerMsg('배치 실행에 실패했습니다.'); }
    finally { setTriggering(false); }
  }

  async function clearAllCache() {
    setClearingCache(true); setClearCacheMsg('');
    try {
      await Promise.all([
        api.post('/ddragon/sync', {}),
        api.post('/batch/trigger', {}),
      ]);
      setClearCacheMsg('전체 캐시가 초기화되었습니다.');
      setTimeout(async () => setBatchStatus(await api.get<BatchStatus>('/batch/status')), 2000);
    } catch { setClearCacheMsg('캐시 초기화에 실패했습니다.'); }
    finally { setClearingCache(false); }
  }

  async function triggerItemStats() {
    setTriggeringItems(true); setTriggerItemMsg('');
    try {
      await api.post('/batch/trigger-item-stats', {});
      setTriggerItemMsg('아이템 통계 집계가 완료되었습니다.');
      setTimeout(async () => setBatchStatus(await api.get<BatchStatus>('/batch/status')), 1000);
    } catch { setTriggerItemMsg('아이템 통계 집계에 실패했습니다.'); }
    finally { setTriggeringItems(false); }
  }

  // ── Drag & Drop ────────────────────────────────────
  function onDrop(to: TeamKey, riotId: string, from: TeamKey) {
    setDragOver(null);
    if (!riotId || from === to) return;
    if (to !== 'pool' && teams[to].length >= 5) return;
    setTeams(prev => {
      const next = { ...prev };
      next[from] = prev[from].filter(id => id !== riotId);
      next[to] = [...prev[to], riotId];
      return next;
    });
  }

  function removeFromTeam(riotId: string, from: TeamKey) {
    setTeams(prev => ({
      ...prev,
      [from]: prev[from].filter(id => id !== riotId),
      pool: from !== 'pool' ? [...prev.pool, riotId] : prev.pool,
    }));
  }

  function resetTeams() {
    setTeams({ pool: allStats.map(s => s.riotId), team1: [], team2: [], team3: [], team4: [] });
  }

  // ── Login ──────────────────────────────────────────
  if (!authed) {
    return (
      <div className="monitoring-gate">
        <div className="monitoring-gate-card">
          <div className="monitoring-gate-icon"><Lock size={32} color="var(--color-primary)" /></div>
          <h2 className="monitoring-gate-title">어드민 접근 인증</h2>
          <p className="monitoring-gate-desc">접근하려면 관리자 비밀번호를 입력하세요.</p>
          <form onSubmit={e => {
            e.preventDefault();
            if (pw === ADMIN_PASSWORD) { sessionStorage.setItem(SESSION_KEY, 'true'); setAuthed(true); }
            else { setAuthErr('비밀번호가 올바르지 않습니다.'); setPw(''); inputRef.current?.focus(); }
          }} className="monitoring-gate-form">
            <input ref={inputRef} type="password" value={pw} onChange={e => setPw(e.target.value)}
              placeholder="비밀번호" className={`monitoring-gate-input${authErr ? ' error' : ''}`}
              autoComplete="current-password" />
            {authErr && <p className="monitoring-gate-error">{authErr}</p>}
            <button type="submit" className="btn btn-primary monitoring-gate-btn">확인</button>
          </form>
        </div>
      </div>
    );
  }

  // ── Main ───────────────────────────────────────────
  return (
    <div className="monitoring-page">
      <div className="monitoring-header">
        <div className="monitoring-header-left">
          <Shield size={20} color="var(--color-primary)" />
          <h1>어드민</h1>
        </div>
        <button className="btn btn-secondary"
          onClick={() => { sessionStorage.removeItem(SESSION_KEY); setAuthed(false); }}
          style={{ fontSize: '0.78rem' }}>잠금</button>
      </div>

      {/* ── 1. 배치 스케쥴러 ──────────────────────── */}
      <section className="stats-section card" style={{ marginBottom: 20 }}>
        <div className="stats-section-title">
          <RefreshCw size={16} />통계 배치 스케쥴러
          <span className="stats-section-sub">매일 04:00 자동 실행 | Kafka 이벤트 수신 시 자동 실행</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 16, flexWrap: 'wrap' }}>
          <button className="btn btn-primary" onClick={triggerBatch} disabled={triggering}
            style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <Play size={14} />{triggering ? '실행 중...' : '배치 수동 실행'}
          </button>
          <button className="btn btn-secondary" onClick={loadAll}
            style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <RefreshCw size={14} />상태 새로고침
          </button>
          {triggerMsg && (
            <span style={{ fontSize: 13, color: triggerMsg.includes('실패') ? 'var(--color-loss)' : 'var(--color-win)' }}>
              {triggerMsg}
            </span>
          )}
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 16, flexWrap: 'wrap', marginTop: 12 }}>
          <button className="btn btn-secondary" onClick={triggerItemStats} disabled={triggeringItems}
            style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <Play size={14} />{triggeringItems ? '집계 중...' : '아이템 통계만 재집계'}
          </button>
          {triggerItemMsg && (
            <span style={{ fontSize: 13, color: triggerItemMsg.includes('실패') ? 'var(--color-loss)' : 'var(--color-win)' }}>
              {triggerItemMsg}
            </span>
          )}
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 16, flexWrap: 'wrap', marginTop: 12, paddingTop: 12, borderTop: '1px solid var(--color-border)' }}>
          <button className="btn btn-secondary" onClick={clearAllCache} disabled={clearingCache}
            style={{ display: 'flex', alignItems: 'center', gap: 6, color: 'var(--color-loss)' }}>
            <Trash2 size={14} />{clearingCache ? '초기화 중...' : '전체 캐시 초기화'}
          </button>
          {clearCacheMsg && (
            <span style={{ fontSize: 13, color: clearCacheMsg.includes('실패') ? 'var(--color-loss)' : 'var(--color-win)' }}>
              {clearCacheMsg}
            </span>
          )}
        </div>
        {batchStatus && (
          <div style={{ marginTop: 16, display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12 }}>
            {[
              { label: '플레이어 스냅샷',    value: `${batchStatus.playerSnapshotCount}건` },
              { label: '챔피언 스냅샷',      value: `${batchStatus.championSnapshotCount}건` },
              { label: '챔피언 아이템 통계', value: `${batchStatus.championItemSnapshotCount}건` },
              { label: '마지막 집계',        value: batchStatus.lastAggregatedAt ?? '없음' },
            ].map(({ label, value }) => (
              <div key={label} className="summary-stat-card" style={{ alignItems: 'flex-start', gap: 4 }}>
                <span style={{ fontSize: 10, color: 'var(--color-text-secondary)', fontWeight: 700, textTransform: 'uppercase' }}>{label}</span>
                <span style={{ fontSize: 13, fontWeight: 600, color: 'var(--color-text-primary)' }}>{value}</span>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* ── 2. Elo 관리 ──────────────────────────── */}
      <section className="stats-section card" style={{ marginBottom: 20 }}>
        <div className="stats-section-title" style={{ justifyContent: 'space-between' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <BarChart2 size={16} />Elo 관리
            <span className="stats-section-sub">전체 초기화 후 매치 시간순 재집계</span>
          </div>
          <button className="btn btn-secondary" onClick={loadEloLeaderboard} disabled={eloLoading}
            style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 12 }}>
            <RefreshCw size={12} />{eloLoading ? '로딩 중...' : '새로고침'}
          </button>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 16, flexWrap: 'wrap' }}>
          <button className="btn btn-secondary" onClick={resetElo} disabled={eloResetting}
            style={{ display: 'flex', alignItems: 'center', gap: 6, color: 'var(--color-loss)' }}>
            <Trash2 size={14} />{eloResetting ? '재집계 중...' : 'Elo 전체 초기화 및 재집계'}
          </button>
          {eloResetMsg && (
            <span style={{ fontSize: 13, color: eloResetMsg.includes('실패') ? 'var(--color-loss)' : 'var(--color-win)' }}>
              {eloResetMsg}
            </span>
          )}
        </div>

        {eloLeaderboard.length > 0 && (
          <div style={{ marginTop: 16, overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--color-border)' }}>
                  <th style={thS}>순위</th>
                  <th style={thS}>플레이어</th>
                  <th style={thS}>Elo</th>
                  <th style={thS}>판수</th>
                </tr>
              </thead>
              <tbody>
                {eloLeaderboard.map(entry => (
                  <tr key={entry.riotId} style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                    <td style={{ ...tdS, fontWeight: 700, color: entry.rank <= 3 ? 'var(--color-primary)' : 'var(--color-text-secondary)', width: 48 }}>
                      #{entry.rank}
                    </td>
                    <td style={{ ...tdS, fontWeight: 600 }}><PlayerLink riotId={entry.riotId}>{entry.riotId}</PlayerLink></td>
                    <td style={{ ...tdS, fontWeight: 700, color: entry.elo >= 1100 ? 'var(--color-win)' : entry.elo < 900 ? 'var(--color-loss)' : 'var(--color-text-primary)' }}>
                      {entry.elo.toFixed(1)}
                    </td>
                    <td style={{ ...tdS, color: 'var(--color-text-secondary)' }}>{entry.games}판</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        {!eloLoading && eloLeaderboard.length === 0 && (
          <p style={{ fontSize: 13, color: 'var(--color-text-secondary)', margin: '12px 0' }}>
            Elo 데이터가 없습니다. [Elo 전체 초기화 및 재집계] 버튼을 눌러 집계하세요.
          </p>
        )}
      </section>

      {/* ── 3. 포지션별 승리기여도 ────────────────── */}
      <section className="stats-section card" style={{ marginBottom: 20 }}>
        <div className="stats-section-title" style={{ justifyContent: 'space-between' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <Trophy size={16} />포지션별 승리기여도
            <span className="stats-section-sub">KDA · 승률 · 비전 · CS 종합 기여 점수</span>
          </div>
          {!posLoaded && (
            <button className="btn btn-secondary" onClick={loadPositions}
              disabled={posLoading || !allStats.length} style={{ fontSize: 12 }}>
              {posLoading ? '로딩 중...' : '통계 불러오기'}
            </button>
          )}
        </div>
        {posLoaded ? (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--color-border)' }}>
                  <th style={thS}>플레이어</th>
                  <th style={thS}>MVP점수</th>
                  {POS_ORDER.map(pos => <th key={pos} style={thS}>{POS_LABELS[pos]}</th>)}
                </tr>
              </thead>
              <tbody>
                {allStats.map(player => {
                  const lanes = posMap[player.riotId] ?? [];
                  const mvp = mvpStats.find(m => m.riotId === player.riotId);
                  return (
                    <tr key={player.riotId} style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                      <td style={tdS}><PlayerLink riotId={player.riotId}><span style={{ fontWeight: 600 }}>{player.riotId}</span></PlayerLink></td>
                      <td style={{ ...tdS, color: 'var(--color-primary)', fontWeight: 700 }}>
                        {mvp ? mvp.avgMvpScore.toFixed(1) : '-'}
                        {mvp && mvp.mvpCount > 0 && (
                          <span style={{ fontSize: 10, color: 'var(--color-text-secondary)', marginLeft: 4 }}>
                            ({mvp.mvpCount}MVP)
                          </span>
                        )}
                      </td>
                      {POS_ORDER.map(pos => {
                        const lane = lanes.find(l => l.position === pos);
                        if (!lane) return <td key={pos} style={{ ...tdS, color: 'var(--color-text-secondary)', opacity: 0.3 }}>—</td>;
                        const score = calcPosScore(lane);
                        const col = lane.winRate >= 60 ? 'var(--color-win)' : lane.winRate < 45 ? 'var(--color-loss)' : 'var(--color-text-primary)';
                        return (
                          <td key={pos} style={tdS}>
                            <div style={{ color: col, fontWeight: 700, fontSize: 14 }}>{score}</div>
                            <div style={{ fontSize: 10, color: 'var(--color-text-secondary)', marginTop: 2 }}>
                              {lane.winRate.toFixed(0)}% · {lane.games}판
                            </div>
                          </td>
                        );
                      })}
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        ) : (
          <p style={{ fontSize: 13, color: 'var(--color-text-secondary)', margin: '12px 0' }}>
            [통계 불러오기] 버튼을 눌러 포지션별 데이터를 로드하세요.
          </p>
        )}
      </section>

      {/* ── 4. 팀 빌더 (Drag & Drop 4팀) ─────────── */}
      <section className="stats-section card" style={{ marginBottom: 20 }}>
        <div className="stats-section-title" style={{ justifyContent: 'space-between' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <Users size={16} />팀 빌더 · 예상 승률
            <span className="stats-section-sub">드래그 &amp; 드롭으로 4팀 구성 · 각 팀 최대 5명</span>
          </div>
          <button className="btn btn-secondary" onClick={resetTeams} style={{ fontSize: 12 }}>초기화</button>
        </div>

        {/* 플레이어 풀 */}
        <DropZone
          teamKey="pool"
          dragOver={dragOver}
          onDragOverChange={setDragOver}
          onDrop={onDrop}
          style={{
            minHeight: 64,
            marginBottom: 20,
            padding: 12,
            borderRadius: 8,
            border: `1px dashed ${dragOver === 'pool' ? 'var(--color-primary)' : 'var(--color-border)'}`,
            background: dragOver === 'pool' ? 'rgba(200,170,110,0.06)' : 'transparent',
            transition: 'all 0.15s',
          }}
        >
          <div style={{ fontSize: 11, fontWeight: 700, color: 'var(--color-text-secondary)', textTransform: 'uppercase', marginBottom: 8 }}>
            미배정 플레이어 ({teams.pool.length}명)
          </div>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
            {teams.pool.length === 0 && (
              <span style={{ fontSize: 12, color: 'var(--color-text-secondary)', opacity: 0.5 }}>모든 플레이어가 팀에 배정됨</span>
            )}
            {teams.pool.map(id => (
              <PlayerChip
                key={id} riotId={id} from="pool"
                allStats={allStats} mvpStats={mvpStats}
                color="var(--color-text-primary)"
              />
            ))}
          </div>
        </DropZone>

        {/* 4팀 그리드 */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 16 }}>
          {(Object.keys(TEAM_META) as Exclude<TeamKey, 'pool'>[]).map(tk => {
            const meta = TEAM_META[tk];
            const members = teams[tk];
            const wr = calcExpectedWR(members, allStats, duoData);
            const teamDuos = duoData
              .filter(d => members.includes(d.player1) && members.includes(d.player2))
              .sort((a, b) => b.winRate - a.winRate);

            return (
              <DropZone
                key={tk} teamKey={tk}
                dragOver={dragOver} onDragOverChange={setDragOver} onDrop={onDrop}
                style={{
                  borderRadius: 10,
                  border: `1px solid ${dragOver === tk ? meta.main : meta.border}`,
                  background: dragOver === tk ? meta.bg.replace('0.08', '0.14') : meta.bg,
                  padding: 14,
                  minHeight: 160,
                  transition: 'all 0.15s',
                }}
              >
                {/* 팀 헤더 */}
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
                  <span style={{ fontWeight: 700, fontSize: 14, color: meta.main }}>{meta.label}</span>
                  {members.length > 0 && (
                    <div style={{ textAlign: 'right' }}>
                      <div style={{
                        fontSize: 22, fontWeight: 900, lineHeight: 1,
                        color: wr >= 0.6 ? 'var(--color-win)' : wr < 0.45 ? 'var(--color-loss)' : meta.main,
                      }}>
                        {(wr * 100).toFixed(1)}%
                      </div>
                      <div style={{ fontSize: 10, color: 'var(--color-text-secondary)' }}>예상 승률</div>
                    </div>
                  )}
                </div>

                {/* 승률 게이지 */}
                {members.length > 0 && (
                  <div style={{ height: 4, background: 'rgba(255,255,255,0.08)', borderRadius: 2, marginBottom: 12, overflow: 'hidden' }}>
                    <div style={{
                      height: '100%', width: `${wr * 100}%`, borderRadius: 2,
                      background: wr >= 0.6 ? 'var(--color-win)' : wr < 0.45 ? 'var(--color-loss)' : meta.main,
                      transition: 'width 0.4s ease',
                    }} />
                  </div>
                )}

                {/* 플레이어 칩 */}
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, minHeight: 36 }}>
                  {members.length === 0 && (
                    <span style={{ fontSize: 12, color: 'var(--color-text-secondary)', opacity: 0.4, alignSelf: 'center' }}>
                      여기에 드롭
                    </span>
                  )}
                  {members.map(id => (
                    <PlayerChip
                      key={id} riotId={id} from={tk}
                      allStats={allStats} mvpStats={mvpStats}
                      color={meta.main}
                      onRemove={() => removeFromTeam(id, tk)}
                    />
                  ))}
                </div>

                {/* 팀 내 듀오 시너지 */}
                {teamDuos.length > 0 && (
                  <div style={{ marginTop: 10, paddingTop: 10, borderTop: `1px solid ${meta.border}` }}>
                    <div style={{ fontSize: 10, color: 'var(--color-text-secondary)', fontWeight: 700, textTransform: 'uppercase', marginBottom: 6 }}>
                      듀오 시너지
                    </div>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                      {teamDuos.map(d => (
                        <div key={`${d.player1}-${d.player2}`}
                          style={{ display: 'flex', justifyContent: 'space-between', fontSize: 11 }}>
                          <span style={{ color: 'var(--color-text-secondary)' }}>
                            {d.player1} + {d.player2}
                          </span>
                          <span style={{
                            fontWeight: 700,
                            color: d.winRate >= 60 ? 'var(--color-win)' : d.winRate < 45 ? 'var(--color-loss)' : 'var(--color-text-primary)',
                          }}>
                            {d.winRate.toFixed(1)}% ({d.games}판)
                          </span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </DropZone>
            );
          })}
        </div>
      </section>

      {/* ── 5. 전체 듀오 시너지 Top ───────────────── */}
      <section className="stats-section card">
        <div className="stats-section-title">
          <Zap size={16} />듀오 시너지 전체
          <span className="stats-section-sub">승률 순 정렬</span>
        </div>
        {duoData.length === 0 ? (
          <p style={{ fontSize: 13, color: 'var(--color-text-secondary)' }}>듀오 데이터가 없습니다.</p>
        ) : (
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
            {duoData.slice(0, 20).map(duo => (
              <div key={`${duo.player1}-${duo.player2}`} style={{
                padding: '8px 12px', borderRadius: 8,
                background: 'var(--color-surface)',
                border: `1px solid ${duo.winRate >= 60 ? 'var(--color-win)' : duo.winRate < 45 ? 'var(--color-loss)' : 'var(--color-border)'}`,
                fontSize: 13, minWidth: 160,
              }}>
                <div style={{ fontWeight: 600, marginBottom: 4 }}>
                  {duo.player1} <span style={{ color: 'var(--color-text-secondary)' }}>+</span> {duo.player2}
                </div>
                <div style={{ display: 'flex', gap: 10, fontSize: 12 }}>
                  <span style={{
                    fontWeight: 700,
                    color: duo.winRate >= 60 ? 'var(--color-win)' : duo.winRate < 45 ? 'var(--color-loss)' : 'var(--color-text-primary)',
                  }}>
                    {duo.winRate.toFixed(1)}%
                  </span>
                  <span style={{ color: 'var(--color-text-secondary)' }}>{duo.games}판</span>
                  <span style={{ color: 'var(--color-text-secondary)' }}>KDA {duo.kda.toFixed(2)}</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

// ── 서브 컴포넌트 ──────────────────────────────────────

interface DropZoneProps {
  teamKey: TeamKey;
  dragOver: TeamKey | null;
  onDragOverChange: (key: TeamKey | null) => void;
  onDrop: (to: TeamKey, riotId: string, from: TeamKey) => void;
  style?: React.CSSProperties;
  children: React.ReactNode;
}
function DropZone({ teamKey, onDragOverChange, onDrop, style, children }: DropZoneProps) {
  return (
    <div
      style={style}
      onDragOver={e => { e.preventDefault(); onDragOverChange(teamKey); }}
      onDragLeave={e => {
        // 자식 요소로 이동할 때는 dragLeave 무시
        if (!e.currentTarget.contains(e.relatedTarget as Node)) {
          onDragOverChange(null);
        }
      }}
      onDrop={e => {
        e.preventDefault();
        const riotId = e.dataTransfer.getData('riotId');
        const from = e.dataTransfer.getData('from') as TeamKey;
        onDrop(teamKey, riotId, from);
      }}
    >
      {children}
    </div>
  );
}

interface PlayerChipProps {
  riotId: string;
  from: TeamKey;
  allStats: PlayerStats[];
  mvpStats: MvpPlayerStat[];
  color: string;
  onRemove?: () => void;
}
function PlayerChip({ riotId, from, allStats, mvpStats, color, onRemove }: PlayerChipProps) {
  const stat = allStats.find(s => s.riotId === riotId);
  const mvp = mvpStats.find(m => m.riotId === riotId);
  return (
    <div
      draggable
      onDragStart={e => {
        e.dataTransfer.setData('riotId', riotId);
        e.dataTransfer.setData('from', from);
        e.dataTransfer.effectAllowed = 'move';
      }}
      style={{
        display: 'flex', alignItems: 'center', gap: 6,
        padding: '5px 8px', borderRadius: 6,
        border: `1px solid ${color}44`,
        background: `${color}11`,
        cursor: 'grab', userSelect: 'none',
        fontSize: 12,
      }}
    >
      <div>
        <div style={{ fontWeight: 700, color }}>{riotId}</div>
        <div style={{ fontSize: 10, color: 'var(--color-text-secondary)' }}>
          {stat?.winRate.toFixed(1)}% · MVP {mvp?.avgMvpScore.toFixed(1) ?? '-'}
        </div>
      </div>
      {onRemove && (
        <button
          onClick={e => { e.stopPropagation(); onRemove(); }}
          style={{ background: 'none', border: 'none', cursor: 'pointer', padding: 0, color: 'var(--color-text-secondary)', display: 'flex', alignItems: 'center' }}
        >
          <X size={12} />
        </button>
      )}
    </div>
  );
}
