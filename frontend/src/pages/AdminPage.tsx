import { useState, useEffect, useRef } from 'react';
import { Shield, Lock, RefreshCw, Play, Users, Trophy, Zap } from 'lucide-react';
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

const POS_ORDER = ['TOP', 'JUNGLE', 'MID', 'BOTTOM', 'SUPPORT'] as const;
const POS_LABELS: Record<string, string> = {
  TOP: 'TOP', JUNGLE: 'JGL', MID: 'MID', BOTTOM: 'BOT', SUPPORT: 'SUP',
};

/** 포지션별 승리기여도 점수 (MVP 공식 근사) */
function calcPosScore(lane: LaneStat): number {
  const kda = (lane.avgKills * 3 + lane.avgAssists * 1.5) / Math.max(lane.avgDeaths, 1);
  const winBonus = (lane.winRate / 100) * 20;
  const vision = lane.avgVisionScore * 0.3;
  const cs = (lane.avgCs / 30) * 0.5;
  return Math.round((kda + winBonus + vision + cs) * 10) / 10;
}

/** 5명 선택 기반 예상 승률 계산 */
function calcExpectedWR(selected: string[], allStats: PlayerStats[], duos: DuoStat[]): number {
  if (!selected.length) return 0;

  const base =
    selected.reduce((sum, id) => sum + (allStats.find(s => s.riotId === id)?.winRate ?? 50), 0) /
    selected.length /
    100;

  const duoRates: number[] = [];
  for (let i = 0; i < selected.length; i++) {
    for (let j = i + 1; j < selected.length; j++) {
      const [a, b] = [selected[i], selected[j]];
      const d = duos.find(
        x => (x.player1 === a && x.player2 === b) || (x.player1 === b && x.player2 === a),
      );
      if (d) duoRates.push(d.winRate / 100);
    }
  }

  const synergy = duoRates.length
    ? (duoRates.reduce((a, b) => a + b, 0) / duoRates.length - 0.5) * 0.3
    : 0;

  return Math.min(Math.max(base + synergy, 0.05), 0.95);
}

const thS: React.CSSProperties = {
  padding: '8px 12px',
  textAlign: 'left',
  fontSize: 11,
  fontWeight: 700,
  color: 'var(--color-text-secondary)',
  textTransform: 'uppercase',
  whiteSpace: 'nowrap',
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

  // data
  const [allStats, setAllStats] = useState<PlayerStats[]>([]);
  const [mvpStats, setMvpStats] = useState<MvpPlayerStat[]>([]);
  const [duoData, setDuoData] = useState<DuoStat[]>([]);
  const [posMap, setPosMap] = useState<Record<string, LaneStat[]>>({});
  const [posLoading, setPosLoading] = useState(false);
  const [posLoaded, setPosLoaded] = useState(false);

  // team builder
  const [selected, setSelected] = useState<string[]>([]);

  useEffect(() => {
    if (!authed) { inputRef.current?.focus(); return; }
    loadAll();
  }, [authed]);

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
  }

  async function loadPositions() {
    if (posLoaded || posLoading || !allStats.length) return;
    setPosLoading(true);
    const results = await Promise.allSettled(
      allStats.map(s =>
        api
          .get<PlayerDetailStats>(`/stats/player/${encodeURIComponent(s.riotId)}`)
          .then(r => ({ riotId: s.riotId, laneStats: r.laneStats })),
      ),
    );
    const map: Record<string, LaneStat[]> = {};
    results.forEach(r => {
      if (r.status === 'fulfilled') map[r.value.riotId] = r.value.laneStats;
    });
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

  async function triggerItemStats() {
    setTriggeringItems(true); setTriggerItemMsg('');
    try {
      await api.post('/batch/trigger-item-stats', {});
      setTriggerItemMsg('아이템 통계 집계가 완료되었습니다.');
      setTimeout(async () => setBatchStatus(await api.get<BatchStatus>('/batch/status')), 1000);
    } catch { setTriggerItemMsg('아이템 통계 집계에 실패했습니다.'); }
    finally { setTriggeringItems(false); }
  }

  function togglePlayer(riotId: string) {
    setSelected(prev =>
      prev.includes(riotId) ? prev.filter(id => id !== riotId) : prev.length < 5 ? [...prev, riotId] : prev,
    );
  }

  const teamWR = calcExpectedWR(selected, allStats, duoData);
  const selectedDuos = duoData
    .filter(d => selected.includes(d.player1) && selected.includes(d.player2))
    .sort((a, b) => b.winRate - a.winRate);
  const baseAvg =
    selected.length
      ? selected.reduce((s, id) => s + (allStats.find(p => p.riotId === id)?.winRate ?? 50), 0) / selected.length
      : 0;
  const totalPairs = (selected.length * (selected.length - 1)) / 2;

  // ─── 로그인 ───────────────────────────────────────────
  if (!authed) {
    return (
      <div className="monitoring-gate">
        <div className="monitoring-gate-card">
          <div className="monitoring-gate-icon">
            <Lock size={32} color="var(--color-primary)" />
          </div>
          <h2 className="monitoring-gate-title">어드민 접근 인증</h2>
          <p className="monitoring-gate-desc">접근하려면 관리자 비밀번호를 입력하세요.</p>
          <form
            onSubmit={e => {
              e.preventDefault();
              if (pw === ADMIN_PASSWORD) { sessionStorage.setItem(SESSION_KEY, 'true'); setAuthed(true); }
              else { setAuthErr('비밀번호가 올바르지 않습니다.'); setPw(''); inputRef.current?.focus(); }
            }}
            className="monitoring-gate-form"
          >
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

  // ─── 메인 ─────────────────────────────────────────────
  return (
    <div className="monitoring-page">
      <div className="monitoring-header">
        <div className="monitoring-header-left">
          <Shield size={20} color="var(--color-primary)" />
          <h1>어드민</h1>
        </div>
        <button className="btn btn-secondary"
          onClick={() => { sessionStorage.removeItem(SESSION_KEY); setAuthed(false); }}
          style={{ fontSize: '0.78rem' }}>
          잠금
        </button>
      </div>

      {/* ── 1. 배치 스케쥴러 ─────────────────────────── */}
      <section className="stats-section card" style={{ marginBottom: 20 }}>
        <div className="stats-section-title">
          <RefreshCw size={16} />
          통계 배치 스케쥴러
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
        {batchStatus && (
          <div style={{ marginTop: 16, display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12 }}>
            {[
              { label: '플레이어 스냅샷',   value: `${batchStatus.playerSnapshotCount}건` },
              { label: '챔피언 스냅샷',     value: `${batchStatus.championSnapshotCount}건` },
              { label: '챔피언 아이템 통계', value: `${batchStatus.championItemSnapshotCount}건` },
              { label: '마지막 집계',       value: batchStatus.lastAggregatedAt ?? '없음' },
            ].map(({ label, value }) => (
              <div key={label} className="summary-stat-card" style={{ alignItems: 'flex-start', gap: 4 }}>
                <span style={{ fontSize: 10, color: 'var(--color-text-secondary)', fontWeight: 700, textTransform: 'uppercase' }}>{label}</span>
                <span style={{ fontSize: 13, fontWeight: 600, color: 'var(--color-text-primary)' }}>{value}</span>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* ── 2. 포지션별 승리기여도 ───────────────────── */}
      <section className="stats-section card" style={{ marginBottom: 20 }}>
        <div className="stats-section-title" style={{ justifyContent: 'space-between' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <Trophy size={16} />
            포지션별 승리기여도
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
                      <td style={tdS}>
                        <span style={{ fontWeight: 600 }}>{player.riotId}</span>
                      </td>
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
                        if (!lane) return (
                          <td key={pos} style={{ ...tdS, color: 'var(--color-text-secondary)', opacity: 0.3 }}>—</td>
                        );
                        const score = calcPosScore(lane);
                        const scoreColor =
                          lane.winRate >= 60 ? 'var(--color-win)' :
                          lane.winRate < 45  ? 'var(--color-loss)' :
                          'var(--color-text-primary)';
                        return (
                          <td key={pos} style={tdS}>
                            <div style={{ color: scoreColor, fontWeight: 700, fontSize: 14 }}>{score}</div>
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

      {/* ── 3. 팀 빌더 & 예상 승률 ──────────────────── */}
      <section className="stats-section card" style={{ marginBottom: 20 }}>
        <div className="stats-section-title">
          <Users size={16} />
          팀 빌더 · 예상 승률
          <span className="stats-section-sub">최대 5명 선택 · 개인 승률 + 듀오 시너지 보정</span>
        </div>

        {/* 플레이어 선택 버튼 */}
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginBottom: 20 }}>
          {allStats.map(player => {
            const isSel = selected.includes(player.riotId);
            const mvp = mvpStats.find(m => m.riotId === player.riotId);
            const disabled = !isSel && selected.length >= 5;
            return (
              <button
                key={player.riotId}
                onClick={() => togglePlayer(player.riotId)}
                disabled={disabled}
                style={{
                  padding: '8px 14px',
                  borderRadius: 8,
                  border: isSel ? '2px solid var(--color-primary)' : '1px solid var(--color-border)',
                  background: isSel ? 'rgba(200,170,110,0.12)' : 'var(--color-surface)',
                  color: isSel ? 'var(--color-primary)' : 'var(--color-text-primary)',
                  cursor: disabled ? 'not-allowed' : 'pointer',
                  fontSize: 13,
                  fontWeight: isSel ? 700 : 400,
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  gap: 2,
                  opacity: disabled ? 0.35 : 1,
                  transition: 'all 0.15s',
                }}
              >
                <span>{player.riotId}</span>
                <span style={{ fontSize: 11, color: isSel ? 'inherit' : 'var(--color-text-secondary)' }}>
                  {player.winRate.toFixed(1)}%
                  {mvp ? ` · MVP ${mvp.avgMvpScore.toFixed(1)}` : ` · KDA ${player.kda.toFixed(2)}`}
                </span>
              </button>
            );
          })}
        </div>

        {selected.length === 0 ? (
          <p style={{ fontSize: 13, color: 'var(--color-text-secondary)' }}>
            위에서 플레이어를 선택하면 예상 승률이 계산됩니다.
          </p>
        ) : (
          <>
            {/* 예상 승률 */}
            <div style={{ display: 'flex', alignItems: 'center', gap: 24, flexWrap: 'wrap', marginBottom: 16 }}>
              <div>
                <div style={{ fontSize: 11, color: 'var(--color-text-secondary)', fontWeight: 700, textTransform: 'uppercase', marginBottom: 4 }}>
                  예상 승률
                </div>
                <div style={{
                  fontSize: 44,
                  fontWeight: 900,
                  lineHeight: 1,
                  color: teamWR >= 0.6 ? 'var(--color-win)' : teamWR < 0.45 ? 'var(--color-loss)' : 'var(--color-text-primary)',
                }}>
                  {(teamWR * 100).toFixed(1)}%
                </div>
              </div>
              <div style={{ flex: 1, minWidth: 200 }}>
                <div style={{ height: 10, background: 'rgba(255,255,255,0.08)', borderRadius: 5, overflow: 'hidden' }}>
                  <div style={{
                    height: '100%',
                    width: `${teamWR * 100}%`,
                    background: teamWR >= 0.6 ? 'var(--color-win)' : teamWR < 0.45 ? 'var(--color-loss)' : 'var(--color-primary)',
                    borderRadius: 5,
                    transition: 'width 0.4s ease',
                  }} />
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 6, fontSize: 11, color: 'var(--color-text-secondary)' }}>
                  <span>개인 평균 승률 {baseAvg.toFixed(1)}%</span>
                  <span>듀오 데이터 {selectedDuos.length} / {totalPairs}쌍</span>
                </div>
              </div>
              <button className="btn btn-secondary" onClick={() => setSelected([])}
                style={{ fontSize: 12, padding: '6px 12px' }}>
                초기화
              </button>
            </div>

            {/* 선택된 플레이어 요약 카드 */}
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: selectedDuos.length ? 16 : 0 }}>
              {selected.map(id => {
                const stat = allStats.find(s => s.riotId === id);
                const mvp = mvpStats.find(m => m.riotId === id);
                return (
                  <div key={id} className="summary-stat-card"
                    style={{ flex: '1 1 100px', minWidth: 100, alignItems: 'flex-start', gap: 2, cursor: 'pointer' }}
                    onClick={() => togglePlayer(id)}>
                    <span style={{ fontSize: 12, fontWeight: 700, color: 'var(--color-primary)' }}>{id}</span>
                    <span style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>
                      {stat?.winRate.toFixed(1)}% 승률
                    </span>
                    <span style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>
                      MVP {mvp?.avgMvpScore.toFixed(1) ?? '-'}
                    </span>
                  </div>
                );
              })}
            </div>

            {/* 팀 내 듀오 시너지 */}
            {selectedDuos.length > 0 && (
              <div>
                <div style={{ fontSize: 11, fontWeight: 700, color: 'var(--color-text-secondary)', marginBottom: 8, textTransform: 'uppercase' }}>
                  팀 내 듀오 시너지
                </div>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
                  {selectedDuos.map(duo => (
                    <div key={`${duo.player1}-${duo.player2}`} style={{
                      padding: '8px 12px',
                      borderRadius: 8,
                      background: 'var(--color-surface)',
                      border: '1px solid var(--color-border)',
                      fontSize: 13,
                      minWidth: 160,
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
              </div>
            )}
          </>
        )}
      </section>

      {/* ── 4. 전체 듀오 시너지 Top ──────────────────── */}
      <section className="stats-section card">
        <div className="stats-section-title">
          <Zap size={16} />
          듀오 시너지 전체
          <span className="stats-section-sub">승률 순 정렬</span>
        </div>
        {duoData.length === 0 ? (
          <p style={{ fontSize: 13, color: 'var(--color-text-secondary)' }}>듀오 데이터가 없습니다.</p>
        ) : (
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
            {duoData.slice(0, 20).map(duo => (
              <div key={`${duo.player1}-${duo.player2}`} style={{
                padding: '8px 12px',
                borderRadius: 8,
                background: 'var(--color-surface)',
                border: `1px solid ${duo.winRate >= 60 ? 'var(--color-win)' : duo.winRate < 45 ? 'var(--color-loss)' : 'var(--color-border)'}`,
                fontSize: 13,
                minWidth: 160,
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
