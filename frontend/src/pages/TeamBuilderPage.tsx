import { useState, useEffect } from 'react';
import { Users, X, RefreshCw } from 'lucide-react';
import { api } from '../lib/api/api';
import type { StatsResponse, PlayerStats, MvpStatsResult, MvpPlayerStat, DuoStatsResult, DuoStat, EloLeaderboardResult } from '../lib/types/stats';
import { PlayerLink } from '../components/common/PlayerLink';
import '../styles/pages/stats.css';

// ── 타입 ─────────────────────────────────────────────────────
type TeamKey = 'pool' | 'team1' | 'team2' | 'team3' | 'team4';
type TeamMap = Record<TeamKey, string[]>;

const TEAM_META: Record<Exclude<TeamKey, 'pool'>, { label: string; main: string; bg: string; border: string }> = {
  team1: { label: '팀 1', main: '#4A9EFF', bg: 'rgba(74,158,255,0.08)',  border: 'rgba(74,158,255,0.35)' },
  team2: { label: '팀 2', main: '#FF6B6B', bg: 'rgba(255,107,107,0.08)', border: 'rgba(255,107,107,0.35)' },
  team3: { label: '팀 3', main: '#51CF66', bg: 'rgba(81,207,102,0.08)',  border: 'rgba(81,207,102,0.35)' },
  team4: { label: '팀 4', main: '#CC5DE8', bg: 'rgba(204,93,232,0.08)',  border: 'rgba(204,93,232,0.35)' },
};

// ── 예상 승률 계산 (Elo + WR + KDA + 듀오시너지) ─────────────
function calcExpectedWR(
  members: string[],
  allStats: PlayerStats[],
  eloMap: Map<string, number>,
  duos: DuoStat[],
): number {
  if (!members.length) return 0;

  // 기본 승률
  const baseWR = members.reduce((sum, id) => {
    return sum + (allStats.find(s => s.riotId === id)?.winRate ?? 50);
  }, 0) / members.length / 100;

  // Elo 보정 (1000 기준, 최대 ±8%)
  const avgElo = members.reduce((sum, id) => sum + (eloMap.get(id) ?? 1000), 0) / members.length;
  const eloBonus = Math.max(-0.08, Math.min(0.08, (avgElo - 1000) / 500 * 0.08));

  // KDA 보정 (3.0 기준, 최대 ±4%)
  const avgKDA = members.reduce((sum, id) => {
    return sum + (allStats.find(s => s.riotId === id)?.kda ?? 3);
  }, 0) / members.length;
  const kdaBonus = Math.max(-0.04, Math.min(0.04, (avgKDA - 3) / 10 * 0.04));

  // 듀오 시너지 보정
  const duoRates: number[] = [];
  for (let i = 0; i < members.length; i++) {
    for (let j = i + 1; j < members.length; j++) {
      const [a, b] = [members[i], members[j]];
      const d = duos.find(x =>
        (x.player1 === a && x.player2 === b) || (x.player1 === b && x.player2 === a)
      );
      if (d) duoRates.push(d.winRate / 100);
    }
  }
  const synergy = duoRates.length
    ? (duoRates.reduce((a, b) => a + b, 0) / duoRates.length - 0.5) * 0.15
    : 0;

  return Math.min(Math.max(baseWR + eloBonus + kdaBonus + synergy, 0.05), 0.95);
}

// ── DropZone ─────────────────────────────────────────────────
interface DropZoneProps {
  teamKey: TeamKey;
  dragOver: TeamKey | null;
  onDragOverChange: (k: TeamKey | null) => void;
  onDrop: (to: TeamKey, riotId: string, from: TeamKey) => void;
  style?: React.CSSProperties;
  className?: string;
  children: React.ReactNode;
}
function DropZone({ teamKey, onDragOverChange, onDrop, style, className, children }: DropZoneProps) {
  return (
    <div
      className={className}
      style={style}
      onDragOver={e => { e.preventDefault(); onDragOverChange(teamKey); }}
      onDragLeave={e => { if (!e.currentTarget.contains(e.relatedTarget as Node)) onDragOverChange(null); }}
      onDrop={e => {
        e.preventDefault();
        onDrop(teamKey, e.dataTransfer.getData('riotId'), e.dataTransfer.getData('from') as TeamKey);
      }}
    >
      {children}
    </div>
  );
}

// ── PlayerChip ───────────────────────────────────────────────
interface PlayerChipProps {
  riotId: string;
  from: TeamKey;
  allStats: PlayerStats[];
  mvpStats: MvpPlayerStat[];
  eloMap: Map<string, number>;
  color: string;
  onRemove?: () => void;
}
function PlayerChip({ riotId, from, allStats, mvpStats, eloMap, color, onRemove }: PlayerChipProps) {
  const stat = allStats.find(s => s.riotId === riotId);
  const mvp = mvpStats.find(m => m.riotId === riotId);
  const elo = eloMap.get(riotId);
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
        border: `1px solid ${color}44`, background: `${color}11`,
        cursor: 'grab', userSelect: 'none', fontSize: 12,
      }}
    >
      <div>
        <div style={{ fontWeight: 700, color }}>{riotId.split('#')[0]}</div>
        <div style={{ fontSize: 10, color: 'var(--color-text-secondary)' }}>
          WR {stat?.winRate.toFixed(0) ?? '—'}%
          {elo != null ? ` · Elo ${elo.toFixed(0)}` : ''}
          {mvp ? ` · MVP ${mvp.avgMvpScore.toFixed(1)}` : ''}
        </div>
      </div>
      {onRemove && (
        <button
          onClick={e => { e.stopPropagation(); onRemove(); }}
          style={{ background: 'none', border: 'none', cursor: 'pointer', padding: 0, color: 'var(--color-text-secondary)', display: 'flex' }}
        >
          <X size={12} />
        </button>
      )}
    </div>
  );
}

// ── 메인 페이지 ───────────────────────────────────────────────
export function TeamBuilderPage() {
  const [allStats, setAllStats] = useState<PlayerStats[]>([]);
  const [mvpStats, setMvpStats] = useState<MvpPlayerStat[]>([]);
  const [duoData, setDuoData]   = useState<DuoStat[]>([]);
  const [eloMap, setEloMap]     = useState<Map<string, number>>(new Map());
  const [loading, setLoading]   = useState(true);
  const [teams, setTeams]       = useState<TeamMap>({ pool: [], team1: [], team2: [], team3: [], team4: [] });
  const [dragOver, setDragOver] = useState<TeamKey | null>(null);

  async function loadAll() {
    setLoading(true);
    const [sr, mr, dr, er] = await Promise.allSettled([
      api.get<StatsResponse>('/stats'),
      api.get<MvpStatsResult>('/stats/mvp'),
      api.get<DuoStatsResult>('/stats/duo?minGames=1'),
      api.get<EloLeaderboardResult>('/stats/elo'),
    ]);
    if (sr.status === 'fulfilled') setAllStats((sr.value as StatsResponse).stats);
    if (mr.status === 'fulfilled') setMvpStats((mr.value as MvpStatsResult).rankings);
    if (dr.status === 'fulfilled') setDuoData((dr.value as DuoStatsResult).duos);
    if (er.status === 'fulfilled') {
      const map = new Map<string, number>();
      (er.value as EloLeaderboardResult).players.forEach(p => map.set(p.riotId, p.elo));
      setEloMap(map);
    }
    setLoading(false);
  }

  // eslint-disable-next-line react-hooks/set-state-in-effect
  useEffect(() => { loadAll(); }, []);

  useEffect(() => {
    if (allStats.length) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setTeams(prev => ({ ...prev, pool: allStats.map(s => s.riotId) }));
    }
  }, [allStats]);

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

  return (
    <div>
      <div className="page-header flex items-center justify-between">
        <div>
          <h1 className="page-title">팀 빌더</h1>
          <p className="page-subtitle">드래그&드롭으로 4팀 구성 · 예상 승률 = WR + Elo + KDA + 듀오시너지</p>
        </div>
        <button className="btn btn-secondary" onClick={resetTeams} style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <RefreshCw size={14} />초기화
        </button>
      </div>

      {loading ? (
        <div className="card" style={{ padding: 32, textAlign: 'center', color: 'var(--color-text-secondary)' }}>
          데이터 로딩 중...
        </div>
      ) : (
        <>
          {/* 플레이어 풀 */}
          <div className="card" style={{ marginBottom: 16 }}>
            <DropZone
              teamKey="pool" dragOver={dragOver}
              onDragOverChange={setDragOver} onDrop={onDrop}
              style={{
                minHeight: 64, padding: 4,
                borderRadius: 8,
                border: `1px dashed ${dragOver === 'pool' ? 'var(--color-primary)' : 'var(--color-border)'}`,
                background: dragOver === 'pool' ? 'rgba(200,170,110,0.06)' : 'transparent',
                transition: 'all 0.15s',
              }}
            >
              <div style={{ fontSize: 11, fontWeight: 700, color: 'var(--color-text-secondary)', textTransform: 'uppercase', marginBottom: 8 }}>
                미배정 플레이어 ({teams.pool.length}명)
              </div>
              <div className="grid-16">
                {teams.pool.length === 0 && (
                  <span className="col-span-16" style={{ fontSize: 12, color: 'var(--color-text-secondary)', opacity: 0.5 }}>모든 플레이어 배정 완료</span>
                )}
                {teams.pool.map(id => (
                  <PlayerChip key={id} riotId={id} from="pool"
                    allStats={allStats} mvpStats={mvpStats} eloMap={eloMap}
                    color="var(--color-text-primary)" className="col-span-2" />
                ))}
              </div>
            </DropZone>
          </div>

          {/* 4팀 그리드 */}
          <div className="grid-16">
            {(Object.keys(TEAM_META) as Exclude<TeamKey, 'pool'>[]).map(tk => {
              const meta = TEAM_META[tk];
              const members = teams[tk];
              const wr = calcExpectedWR(members, allStats, eloMap, duoData);
              const teamDuos = duoData
                .filter(d => members.includes(d.player1) && members.includes(d.player2))
                .sort((a, b) => b.winRate - a.winRate);
              const avgElo = members.length
                ? members.reduce((s, id) => s + (eloMap.get(id) ?? 1000), 0) / members.length
                : null;

              return (
                <DropZone key={tk} teamKey={tk} dragOver={dragOver}
                  className="col-span-8"
                  onDragOverChange={setDragOver} onDrop={onDrop}
                  style={{
                    borderRadius: 10,
                    border: `1px solid ${dragOver === tk ? meta.main : meta.border}`,
                    background: dragOver === tk ? meta.bg.replace('0.08', '0.14') : meta.bg,
                    padding: 16, minHeight: 180, transition: 'all 0.15s',
                  }}
                >
                  {/* 팀 헤더 */}
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 12 }}>
                    <div>
                      <span style={{ fontWeight: 700, fontSize: 15, color: meta.main }}>{meta.label}</span>
                      {avgElo != null && (
                        <div style={{ fontSize: 10, color: 'var(--color-text-secondary)', marginTop: 2 }}>
                          평균 Elo {avgElo.toFixed(0)}
                        </div>
                      )}
                    </div>
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
                  <div className="grid-16" style={{ minHeight: 36 }}>
                    {members.length === 0 && (
                      <span className="col-span-16" style={{ fontSize: 12, color: 'var(--color-text-secondary)', opacity: 0.4, alignSelf: 'center' }}>
                        여기에 드롭
                      </span>
                    )}
                    {members.map(id => (
                      <PlayerChip key={id} riotId={id} from={tk}
                        allStats={allStats} mvpStats={mvpStats} eloMap={eloMap}
                        color={meta.main} className="col-span-2"
                        onRemove={() => removeFromTeam(id, tk)} />
                    ))}
                  </div>

                  {/* 듀오 시너지 */}
                  {teamDuos.length > 0 && (
                    <div style={{ marginTop: 12, paddingTop: 12, borderTop: `1px solid ${meta.border}` }}>
                      <div style={{ fontSize: 10, color: 'var(--color-text-secondary)', fontWeight: 700, textTransform: 'uppercase', marginBottom: 6 }}>
                        듀오 시너지
                      </div>
                      <div style={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
                        {teamDuos.map(d => (
                          <div key={`${d.player1}-${d.player2}`}
                            style={{ display: 'flex', justifyContent: 'space-between', fontSize: 11 }}>
                            <span style={{ color: 'var(--color-text-secondary)' }}>
                              {d.player1.split('#')[0]} + {d.player2.split('#')[0]}
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

          {/* 전체 듀오 시너지 참고 */}
          {duoData.length > 0 && (
            <div className="card" style={{ marginTop: 16 }}>
              <div style={{ fontSize: 13, fontWeight: 700, marginBottom: 12, color: 'var(--color-text-secondary)', textTransform: 'uppercase', letterSpacing: 0.5 }}>
                <Users size={14} style={{ display: 'inline', marginRight: 6 }} />
                듀오 시너지 전체 참고
              </div>
              <div className="grid-16">
                {[...duoData].sort((a, b) => b.winRate - a.winRate).slice(0, 20).map(d => (
                  <div key={`${d.player1}-${d.player2}`} className="col-span-4" style={{
                    padding: '7px 11px', borderRadius: 8,
                    background: 'var(--color-bg-secondary)',
                    border: `1px solid ${d.winRate >= 60 ? 'var(--color-win)' : d.winRate < 45 ? 'var(--color-loss)' : 'var(--color-border)'}`,
                    fontSize: 12,
                  }}>
                    <div style={{ fontWeight: 600, marginBottom: 3 }}>
                      <PlayerLink riotId={d.player1}>{d.player1.split('#')[0]}</PlayerLink>
                      <span style={{ color: 'var(--color-text-secondary)', margin: '0 4px' }}>+</span>
                      <PlayerLink riotId={d.player2}>{d.player2.split('#')[0]}</PlayerLink>
                    </div>
                    <div style={{ display: 'flex', gap: 8 }}>
                      <span style={{ fontWeight: 700, color: d.winRate >= 60 ? 'var(--color-win)' : d.winRate < 45 ? 'var(--color-loss)' : 'var(--color-text-primary)' }}>
                        {d.winRate.toFixed(1)}%
                      </span>
                      <span style={{ color: 'var(--color-text-disabled)' }}>{d.games}판</span>
                      <span style={{ color: 'var(--color-text-disabled)' }}>KDA {d.kda.toFixed(1)}</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}
