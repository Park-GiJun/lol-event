import { useEffect, useState, useCallback } from 'react';
import { Trash2, Clock, Calendar, ChevronRight } from 'lucide-react';
import { api } from '../lib/api/api';
import type { Match, Participant, Team } from '../lib/types/match';
import { Button } from '../components/common/Button';
import { Modal } from '../components/common/Modal';
import { LoadingCenter } from '../components/common/Spinner';
import '../styles/pages/matches.css';

// ── Constants ──────────────────────────────────────────────────────────────
const MODES = [
  { value: 'normal', label: '5v5 내전' },
  { value: 'aram',   label: '칼바람' },
  { value: 'all',    label: '전체' },
];

const QUEUE_LABEL: Record<number, string> = { 0: '커스텀', 3130: '5v5 내전', 3270: '칼바람' };

const TABS = ['요약', '딜/피해', '경제', '시야/오브젝트', '멀티킬', '팀 정보'] as const;
type Tab = typeof TABS[number];

// ── Helpers ────────────────────────────────────────────────────────────────
function fmt(secs: number) {
  const m = Math.floor(secs / 60);
  const s = secs % 60;
  return `${m}:${String(s).padStart(2, '0')}`;
}


function kda(p: Participant): string {
  return p.deaths === 0 ? 'Perfect' : ((p.kills + p.assists) / p.deaths).toFixed(2);
}

function shortVersion(v?: string) {
  if (!v) return null;
  const parts = v.split('.');
  return parts.slice(0, 2).join('.');
}

// ── Match Card (List) ──────────────────────────────────────────────────────
function MatchCard({ match, onOpen, onDelete }: {
  match: Match;
  onOpen: () => void;
  onDelete: () => void;
}) {
  const blue = match.participants.filter(p => p.team === 'blue');
  const red  = match.participants.filter(p => p.team === 'red');
  const blueWin = blue[0]?.win ?? false;
  const date = new Date(match.gameCreation);
  const dateStr = date.toLocaleDateString('ko-KR', { month: 'numeric', day: 'numeric' });
  const timeStr = date.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', hour12: false });

  return (
    <div className="match-card" onClick={onOpen}>
      {/* Header */}
      <div className="match-card-header">
        <div className="match-card-meta">
          <span className="match-mode-badge">{QUEUE_LABEL[match.queueId] ?? match.queueId}</span>
          <span className="match-meta-item"><Clock size={11} />{fmt(match.gameDuration)}</span>
          <span className="match-meta-item"><Calendar size={11} />{dateStr} {timeStr}</span>
          {shortVersion(match.gameVersion) && (
            <span className="match-meta-item match-version">v{shortVersion(match.gameVersion)}</span>
          )}
        </div>
        <div className="match-card-actions" onClick={e => e.stopPropagation()}>
          <button className="match-delete-btn" title="삭제" onClick={onDelete}>
            <Trash2 size={13} />
          </button>
          <ChevronRight size={15} className="match-arrow" />
        </div>
      </div>

      {/* Teams */}
      <div className="match-teams">
        <TeamColumn participants={blue} side="blue" win={blueWin} />
        <div className="match-vs">VS</div>
        <TeamColumn participants={red} side="red" win={!blueWin} />
      </div>
    </div>
  );
}

function TeamColumn({ participants, side, win }: {
  participants: Participant[];
  side: 'blue' | 'red';
  win: boolean;
}) {
  return (
    <div className={`match-team match-team-${side}`}>
      <div className="match-team-label">
        <span className="match-team-name">{side === 'blue' ? '블루팀' : '레드팀'}</span>
        <span className={`match-result-badge ${win ? 'win' : 'loss'}`}>{win ? '승' : '패'}</span>
      </div>
      <div className="match-players">
        {participants.map((p, i) => (
          <div key={i} className="match-player-row">
            <span className="match-champion">{p.champion}</span>
            <span className="match-player-name">{p.riotId.split('#')[0]}</span>
            <span className="match-kda">{p.kills}/{p.deaths}/{p.assists}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

// ── Scoreboard (요약 탭) ───────────────────────────────────────────────────
function Scoreboard({ match }: { match: Match }) {
  const maxDmg = Math.max(...match.participants.map(p => p.damage), 1);

  return (
    <div>
      {(['blue', 'red'] as const).map(side => {
        const team = match.participants.filter(p => p.team === side);
        const win  = team[0]?.win ?? false;
        const totalKills = team.reduce((s, p) => s + p.kills, 0);
        const teamData = match.teams?.find(t => t.teamId === (side === 'blue' ? 100 : 200));


        return (
          <div key={side} className="scoreboard-section">
            <div className={`scoreboard-team-header ${side}-team`}>
              <span className="scoreboard-team-name">
                {side === 'blue' ? '블루팀' : '레드팀'}
              </span>
              <span className={`sb-result ${win ? 'win' : 'loss'}`}>{win ? '승리' : '패배'}</span>
              <span className="scoreboard-team-kills">
                {totalKills}킬
                {teamData && (
                  <>
                    {' '}· 바론 {teamData.baronKills ?? 0}
                    {' '}· 드래곤 {teamData.dragonKills ?? 0}
                    {' '}· 포탑 {teamData.towerKills ?? 0}
                  </>
                )}
              </span>
            </div>

            <table className="scoreboard-table">
              <thead>
                <tr>
                  <th style={{ width: 160, textAlign: 'left' }}>플레이어</th>
                  <th style={{ textAlign: 'center' }}>결과</th>
                  <th style={{ textAlign: 'center' }}>K / D / A</th>
                  <th className="th-damage">피해량</th>
                  <th className="td-num" style={{ textAlign: 'right' }}>CS</th>
                  <th className="td-num" style={{ textAlign: 'right' }}>골드</th>
                  <th className="td-num" style={{ textAlign: 'right' }}>시야</th>
                  <th className="td-num" style={{ textAlign: 'right' }}>Lv</th>
                </tr>
              </thead>
              <tbody>
                {team.map((p, i) => {
                  const dmgPct = (p.damage / maxDmg) * 100;
                  return (
                    <tr key={i}>
                      {/* 플레이어 */}
                      <td>
                        <div className="sb-player">
                          <div className={`sb-champion-badge ${side}`}>
                            {p.champion.slice(0, 2)}
                          </div>
                          <div className="sb-player-info">
                            <div className="sb-champion-name">{p.champion}</div>
                            <div className="sb-riot-id">{p.riotId.split('#')[0]}</div>
                          </div>
                        </div>
                      </td>

                      {/* 결과 */}
                      <td style={{ textAlign: 'center' }}>
                        <span className={`sb-result ${p.win ? 'win' : 'loss'}`}>
                          {p.win ? '승' : '패'}
                        </span>
                      </td>

                      {/* KDA */}
                      <td style={{ textAlign: 'center' }}>
                        <div className="sb-kda">
                          <div className="sb-kda-nums">
                            <span className="kills">{p.kills}</span>
                            <span className="slash"> / </span>
                            <span className="deaths">{p.deaths}</span>
                            <span className="slash"> / </span>
                            <span className="assists">{p.assists}</span>
                          </div>
                          <div className="sb-kda-ratio">{kda(p)} KDA</div>
                        </div>
                      </td>

                      {/* 피해량 바 */}
                      <td className="sb-damage-cell">
                        <div className="sb-damage-num">{p.damage.toLocaleString()}</div>
                        <div className="sb-damage-bar-track">
                          <div
                            className={`sb-damage-bar-fill ${side}`}
                            style={{ width: `${dmgPct}%` }}
                          />
                        </div>
                      </td>

                      {/* CS */}
                      <td className="td-num">
                        <div>{p.cs}</div>
                        <div style={{ fontSize: 10, color: 'var(--color-text-secondary)' }}>
                          {(p.cs / Math.max(match.gameDuration / 60, 1)).toFixed(1)}/분
                        </div>
                      </td>

                      {/* 골드 */}
                      <td className="td-num">{p.gold.toLocaleString()}</td>

                      {/* 시야 */}
                      <td className="td-num">{p.visionScore}</td>

                      {/* 레벨 */}
                      <td className="td-num">{p.champLevel || '-'}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        );
      })}
    </div>
  );
}

// ── Stat Tabs (딜/피해, 경제, 시야, 멀티킬) ───────────────────────────────
function StatRow({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="stat-row">
      <span className="stat-row-label">{label}</span>
      <span className="stat-row-value">{typeof value === 'number' ? value.toLocaleString() : value}</span>
    </div>
  );
}

function PlayerStatBlock({ p, tab }: { p: Participant; tab: Tab }) {
  const renderStats = () => {
    if (tab === '딜/피해') return (
      <div className="stat-grid">
        <StatRow label="챔피언 피해 (총)" value={p.totalDamageDealtToChampions} />
        <StatRow label="챔피언 피해 (마법)" value={p.magicDamageDealtToChampions} />
        <StatRow label="챔피언 피해 (물리)" value={p.physicalDamageDealtToChampions} />
        <StatRow label="챔피언 피해 (참)" value={p.trueDamageDealtToChampions} />
        <StatRow label="전체 딜 (총)" value={p.totalDamageDealt} />
        <StatRow label="오브젝트 딜" value={p.damageDealtToObjectives} />
        <StatRow label="포탑 딜" value={p.damageDealtToTurrets} />
        <StatRow label="피해 경감" value={p.damageSelfMitigated} />
        <StatRow label="받은 피해 (총)" value={p.totalDamageTaken} />
        <StatRow label="받은 피해 (마법)" value={p.magicalDamageTaken} />
        <StatRow label="받은 피해 (물리)" value={p.physicalDamageTaken} />
        <StatRow label="받은 피해 (참)" value={p.trueDamageTaken} />
        <StatRow label="총 치유" value={p.totalHeal} />
        <StatRow label="CC 시간 (초)" value={p.timeCCingOthers} />
      </div>
    );

    if (tab === '경제') return (
      <div className="stat-grid">
        <StatRow label="골드" value={p.gold} />
        <StatRow label="미니언" value={p.cs} />
        <StatRow label="중립 몬스터" value={p.neutralMinionsKilled} />
        <StatRow label="중립 (우리 정글)" value={p.neutralMinionsKilledTeamJungle} />
        <StatRow label="중립 (적 정글)" value={p.neutralMinionsKilledEnemyJungle} />
        <StatRow label="챔피언 레벨" value={p.champLevel} />
      </div>
    );

    if (tab === '시야/오브젝트') return (
      <div className="stat-grid">
        <StatRow label="시야 점수" value={p.visionScore} />
        <StatRow label="와드 설치" value={p.wardsPlaced} />
        <StatRow label="와드 제거" value={p.wardsKilled} />
        <StatRow label="제어 와드 구매" value={p.visionWardsBoughtInGame} />
        <StatRow label="포탑 처치" value={p.turretKills} />
        <StatRow label="억제기 처치" value={p.inhibitorKills} />
        <StatRow label="첫 피바람" value={p.firstBloodKill ? '✓' : '-'} />
        <StatRow label="첫 포탑" value={p.firstTowerKill ? '✓' : '-'} />
      </div>
    );

    if (tab === '멀티킬') return (
      <div className="stat-grid">
        <StatRow label="더블킬" value={p.doubleKills} />
        <StatRow label="트리플킬" value={p.tripleKills} />
        <StatRow label="쿼드라킬" value={p.quadraKills} />
        <StatRow label="펜타킬" value={p.pentaKills} />
        <StatRow label="최대 연속킬" value={p.largestKillingSpree} />
        <StatRow label="최대 멀티킬" value={p.largestMultiKill} />
        <StatRow label="최대 크리티컬" value={p.largestCriticalStrike} />
      </div>
    );

    return null;
  };

  return (
    <div className="stat-player-block">
      <div className="stat-player-header">
        <span className={`sb-champion-badge ${p.team}`}>{p.champion.slice(0, 2)}</span>
        <span>{p.champion}</span>
        <span style={{ color: 'var(--color-text-secondary)', fontWeight: 400 }}>
          {p.riotId.split('#')[0]}
        </span>
        <span className={`sb-result ${p.win ? 'win' : 'loss'}`}>{p.win ? '승' : '패'}</span>
        <span style={{ marginLeft: 'auto', fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)' }}>
          {p.kills}/{p.deaths}/{p.assists} · {kda(p)} KDA
        </span>
      </div>
      {renderStats()}
    </div>
  );
}

function StatsTab({ match, tab }: { match: Match; tab: Tab }) {
  return (
    <div>
      {(['blue', 'red'] as const).map(side => (
        <div key={side} style={{ marginBottom: 20 }}>
          <div style={{
            fontSize: 'var(--font-size-xs)', fontWeight: 700, letterSpacing: '0.5px',
            color: side === 'blue' ? 'var(--color-blue)' : 'var(--color-red)',
            marginBottom: 8, textTransform: 'uppercase'
          }}>
            {side === 'blue' ? '블루팀' : '레드팀'}
          </div>
          {match.participants.filter(p => p.team === side).map((p, i) => (
            <PlayerStatBlock key={i} p={p} tab={tab} />
          ))}
        </div>
      ))}
    </div>
  );
}

// ── Team Info Tab ──────────────────────────────────────────────────────────
function TeamInfoTab({ teams }: { teams: Team[] }) {
  return (
    <div className="team-info-section">
      {teams.map(t => {
        const side = t.teamId === 100 ? 'blue' : 'red';
        const win  = t.win;
        return (
          <div key={t.teamId} className="team-info-block">
            <div className={`team-info-header ${side}-team`} style={{
              borderLeft: `3px solid var(--color-${side === 'blue' ? 'blue' : 'red'})`,
            }}>
              <span className="scoreboard-team-name">
                {side === 'blue' ? '블루팀' : '레드팀'}
              </span>
              <span className={`sb-result ${win ? 'win' : 'loss'}`}>{win ? '승리' : '패배'}</span>
            </div>
            <div className="team-info-stats">
              {[
                { label: '바론',   value: t.baronKills,      icon: '🐉' },
                { label: '드래곤', value: t.dragonKills,     icon: '🔥' },
                { label: '포탑',   value: t.towerKills,      icon: '🏰' },
                { label: '억제기', value: t.inhibitorKills,  icon: '⚡' },
                { label: '전령',   value: t.riftHeraldKills, icon: '👁' },
                { label: '나서스', value: t.hordeKills,      icon: '🐕' },
              ].map(({ label, value }) => (
                <div key={label} className="team-info-stat">
                  <div className="team-info-stat-value">{value ?? 0}</div>
                  <div className="team-info-stat-label">{label}</div>
                </div>
              ))}
            </div>
            {/* First objectives */}
            <div style={{ padding: '8px 14px', display: 'flex', gap: 12, flexWrap: 'wrap', borderTop: '1px solid var(--color-border)' }}>
              {[
                { label: '첫 피바람', v: t.firstBlood },
                { label: '첫 포탑',  v: t.firstTower },
                { label: '첫 바론',  v: t.firstBaron },
                { label: '첫 억제기', v: t.firstInhibitor },
                { label: '첫 드래곤', v: t.firstDragon },
              ].filter(x => x.v).map(({ label }) => (
                <span key={label} style={{
                  fontSize: 'var(--font-size-xs)',
                  background: 'rgba(200,155,60,0.1)',
                  border: '1px solid rgba(200,155,60,0.25)',
                  color: 'var(--color-primary)',
                  borderRadius: 'var(--radius-sm)',
                  padding: '2px 8px',
                }}>
                  {label}
                </span>
              ))}
            </div>
          </div>
        );
      })}
    </div>
  );
}

// ── Detail Modal ───────────────────────────────────────────────────────────
function MatchDetailModal({ match, onClose }: { match: Match; onClose: () => void }) {
  const [tab, setTab] = useState<Tab>('요약');

  return (
    <Modal isOpen title={`${QUEUE_LABEL[match.queueId] ?? match.queueId} — ${match.matchId}`}
      onClose={onClose} size="xl">

      {/* Meta */}
      <div className="match-detail-meta">
        <div className="match-detail-meta-item">
          <Clock size={13} />
          <strong>{fmt(match.gameDuration)}</strong>
        </div>
        <div className="match-detail-meta-item">
          <Calendar size={13} />
          <strong>{new Date(match.gameCreation).toLocaleString('ko-KR')}</strong>
        </div>
        {match.gameVersion && (
          <div className="match-detail-meta-item">
            <span>버전</span>
            <strong>{match.gameVersion}</strong>
          </div>
        )}
        {match.gameMode && (
          <div className="match-detail-meta-item">
            <span>모드</span>
            <strong>{match.gameMode}</strong>
          </div>
        )}
      </div>

      {/* Tabs */}
      <div className="match-tabs">
        {TABS.map(t => (
          <button key={t} className={`match-tab ${tab === t ? 'active' : ''}`}
            onClick={() => setTab(t)}>
            {t}
          </button>
        ))}
      </div>

      {/* Content */}
      {tab === '요약'      && <Scoreboard match={match} />}
      {tab === '팀 정보'  && <TeamInfoTab teams={match.teams ?? []} />}
      {(tab === '딜/피해' || tab === '경제' || tab === '시야/오브젝트' || tab === '멀티킬') && (
        <StatsTab match={match} tab={tab} />
      )}
    </Modal>
  );
}

// ── Main Page ──────────────────────────────────────────────────────────────
export function MatchesPage() {
  const [matches, setMatches] = useState<Match[]>([]);
  const [mode, setMode]       = useState('normal');
  const [loading, setLoading] = useState(true);
  const [detail, setDetail]   = useState<Match | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try { setMatches(await api.get<Match[]>(`/matches?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode]);

  useEffect(() => { load(); }, [load]);

  const handleDelete = async (matchId: string) => {
    if (!confirm('이 경기를 삭제하시겠습니까?')) return;
    await api.delete(`/matches/${matchId}`);
    load();
  };

  return (
    <div>
      <div className="page-header flex items-center justify-between">
        <div>
          <h1 className="page-title">경기 목록</h1>
          <p className="page-subtitle">{matches.length}건</p>
        </div>
        <div className="flex gap-sm">
          {MODES.map(m => (
            <Button key={m.value}
              variant={mode === m.value ? 'primary' : 'secondary'}
              size="sm"
              onClick={() => setMode(m.value)}>
              {m.label}
            </Button>
          ))}
        </div>
      </div>

      {loading ? <LoadingCenter /> : (
        <div className="matches-list">
          {matches.length === 0 && (
            <div style={{ textAlign: 'center', padding: '60px 0', color: 'var(--color-text-secondary)' }}>
              경기 데이터가 없습니다
            </div>
          )}
          {matches.map(m => (
            <MatchCard
              key={m.matchId}
              match={m}
              onOpen={() => setDetail(m)}
              onDelete={() => handleDelete(m.matchId)}
            />
          ))}
        </div>
      )}

      {detail && (
        <MatchDetailModal match={detail} onClose={() => setDetail(null)} />
      )}
    </div>
  );
}
