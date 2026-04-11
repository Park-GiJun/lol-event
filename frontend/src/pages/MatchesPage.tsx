/* eslint-disable react-refresh/only-export-components */
import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Trash2, Clock, Calendar, ChevronRight, ChevronDown, Trophy } from 'lucide-react';
import { api } from '../lib/api/api';
import type { Match, Participant, Team } from '../lib/types/match';
import { useDragon } from '../context/DragonContext';
import { Button } from '../components/common/Button';
import { Modal } from '../components/common/Modal';
import { Skeleton } from '../components/common/Skeleton';
import { PlayerLink } from '../components/common/PlayerLink';
import { ChampionLink } from '../components/common/ChampionLink';
import { fmt as fmtUtil, calcMvp as calcMvpUtil, MODES } from '../lib/lol';
import '../styles/pages/matches.css';

// ── Constants ──────────────────────────────────────────────────────────────
export const QUEUE_LABEL: Record<number, string> = { 0: '커스텀', 3130: '5v5 내전', 3270: '칼바람' };

export const TABS = ['요약', '딜/피해', '경제', '시야/오브젝트', '멀티킬', '팀 정보'] as const;
export type Tab = typeof TABS[number];

const DAY_NAMES = ['일', '월', '화', '수', '목', '금', '토'];

// ── Helpers ────────────────────────────────────────────────────────────────
// re-export for MatchDetailPage backward-compat
export const fmt = fmtUtil;
const calcMvp = calcMvpUtil;

function kda(p: Participant): string {
  return p.deaths === 0 ? 'Perfect' : ((p.kills + p.assists) / p.deaths).toFixed(2);
}

function shortVersion(v?: string) {
  if (!v) return null;
  const parts = v.split('.');
  return parts.slice(0, 2).join('.');
}

// 오후 5시(17)~다음날 오후 4시 59분(16:59)을 하나의 내전 세션으로 묶음
// → 00:00~16:59 는 전날 날짜로 귀속
function getSessionKey(ts: number): string {
  const d = new Date(ts);
  if (d.getHours() < 17) {
    d.setDate(d.getDate() - 1);
  }
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

function formatSessionLabel(key: string): string {
  const [y, m, d] = key.split('-').map(Number);
  const date = new Date(y, m - 1, d);
  return `${m}월 ${d}일 (${DAY_NAMES[date.getDay()]})`;
}

// ── Dragon Image Helpers ───────────────────────────────────────────────────
function ChampionImg({ championId, champion, size, side }: {
  championId: number; champion: string; size: number; side: 'blue' | 'red';
}) {
  const { champions } = useDragon();
  const data = champions.get(championId);
  if (data?.imageUrl) {
    return (
      <img src={data.imageUrl} alt={champion} width={size} height={size}
        className={`dragon-champ-img champ-img-hover ${side}`}
        style={{ borderRadius: size <= 20 ? 3 : 4 }}
        onError={e => { (e.currentTarget as HTMLImageElement).style.display = 'none'; }}
      />
    );
  }
  return (
    <div className={`sb-champion-badge ${side}`} style={{ width: size, height: size, fontSize: size * 0.36 }}>
      {champion.slice(0, 2)}
    </div>
  );
}

function ItemImg({ itemId }: { itemId: number }) {
  const { items } = useDragon();
  if (!itemId) return <div className="dragon-item-empty" />;
  const data = items.get(itemId);
  if (data?.imageUrl) {
    return (
      <img src={data.imageUrl} alt={data.nameKo} title={data.nameKo}
        width={22} height={22} className="dragon-item-img"
        onError={e => { (e.currentTarget as HTMLImageElement).style.display = 'none'; }}
      />
    );
  }
  return <div className="dragon-item-empty" />;
}

function SpellImg({ spellId }: { spellId: number }) {
  const { spells } = useDragon();
  const data = spells.get(spellId);
  if (data?.imageUrl) {
    return (
      <img src={data.imageUrl} alt={data.nameKo} title={data.nameKo}
        width={16} height={16} className="dragon-spell-img"
        onError={e => { (e.currentTarget as HTMLImageElement).style.display = 'none'; }}
      />
    );
  }
  return <div className="dragon-spell-empty" />;
}

// ── Match Card (List) ──────────────────────────────────────────────────────
function MatchCard({ match, onOpen, onDelete }: {
  match: Match; onOpen: () => void; onDelete: () => void;
}) {
  const blue = match.participants.filter(p => p.team === 'blue');
  const red  = match.participants.filter(p => p.team === 'red');
  const blueWin = blue[0]?.win ?? false;
  const date = new Date(match.gameCreation);
  const timeStr = date.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', hour12: false });
  const mvp = calcMvp(match);

  return (
    <div className="match-card" onClick={onOpen}>
      {/* Header */}
      <div className="match-card-header">
        <div className="match-card-meta">
          <span className="match-mode-badge">{QUEUE_LABEL[match.queueId] ?? match.queueId}</span>
          <span className="match-meta-item"><Clock size={11} />{fmt(match.gameDuration)}</span>
          <span className="match-meta-item"><Calendar size={11} />{timeStr}</span>
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
        <TeamColumn participants={blue} side="blue" win={blueWin} mvpRiotId={mvp.aceId} teamMvpRiotId={mvp.blueMvpId} />
        <div className="match-vs">VS</div>
        <TeamColumn participants={red} side="red" win={!blueWin} mvpRiotId={mvp.aceId} teamMvpRiotId={mvp.redMvpId} />
      </div>
    </div>
  );
}

function TeamColumn({ participants, side, win, mvpRiotId, teamMvpRiotId }: {
  participants: Participant[];
  side: 'blue' | 'red';
  win: boolean;
  mvpRiotId: string;
  teamMvpRiotId: string;
}) {
  const { champions } = useDragon();
  return (
    <div className={`match-team match-team-${side}`}>
      <div className="match-team-label">
        <span className="match-team-name">{side === 'blue' ? '블루팀' : '레드팀'}</span>
        <span className={`match-result-badge ${win ? 'win' : 'loss'}`}>{win ? '승' : '패'}</span>
      </div>
      <div className="match-players">
        {participants.map((p, i) => {
          const isAce      = p.riotId === mvpRiotId;
          const isTeamMvp  = !isAce && p.riotId === teamMvpRiotId;
          const nameKo     = champions.get(p.championId)?.nameKo || p.champion;
          return (
            <div key={i} className={`match-player-row${isAce ? ' match-player-ace' : isTeamMvp ? ' match-player-mvp' : ''}`}>
              <div className="match-champion-cell">
                <ChampionImg championId={p.championId} champion={p.champion} size={16} side={side} />
                <ChampionLink champion={p.champion} championId={p.championId}>
                  <span className="match-champion">{nameKo}</span>
                </ChampionLink>
              </div>
              <PlayerLink riotId={p.riotId}>
                <span className="match-player-name">{p.riotId.split('#')[0]}</span>
              </PlayerLink>
              {isAce && (
                <span className="match-mvp-badge match-mvp-badge--ace">
                  <Trophy size={9} />ACE
                </span>
              )}
              {isTeamMvp && (
                <span className="match-mvp-badge match-mvp-badge--team">MVP</span>
              )}
              <span className="match-kda">{p.kills}/{p.deaths}/{p.assists}</span>
            </div>
          );
        })}
      </div>
    </div>
  );
}

// ── Session Group (날짜 묶음) ──────────────────────────────────────────────
function MatchGroup({ sessionKey, matches, defaultOpen, onOpen, onDelete }: {
  sessionKey: string;
  matches: Match[];
  defaultOpen: boolean;
  onOpen: (m: Match) => void;
  onDelete: (id: string) => void;
}) {
  const [open, setOpen] = useState(defaultOpen);
  const label = formatSessionLabel(sessionKey);

  return (
    <div className="match-group">
      <button className="match-group-header" onClick={() => setOpen(o => !o)}>
        <div className="match-group-header-left">
          {open ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
          <span className="match-group-date">{label}</span>
          <span className="match-group-count">{matches.length}경기</span>
          <span className="match-group-date-line" />
        </div>
        <div className="match-group-summary">
          {(() => {
            const wins  = matches.filter(m => m.participants.find(p => p.team === 'blue')?.win).length;
            const total = matches.length;
            return <span className="match-group-winloss">{wins}승 {total - wins}패</span>;
          })()}
        </div>
      </button>

      {open && (
        <div className="match-group-body">
          {matches.map(m => (
            <MatchCard
              key={m.matchId}
              match={m}
              onOpen={() => onOpen(m)}
              onDelete={() => onDelete(m.matchId)}
            />
          ))}
        </div>
      )}
    </div>
  );
}

// ── Scoreboard ─────────────────────────────────────────────────────────────
export function Scoreboard({ match }: { match: Match }) {
  const maxDmg = Math.max(...match.participants.map(p => p.damage), 1);
  const mvp = calcMvp(match);
  const { champions } = useDragon();

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
              <span className="scoreboard-team-name">{side === 'blue' ? '블루팀' : '레드팀'}</span>
              <span className={`sb-result ${win ? 'win' : 'loss'}`}>{win ? '승리' : '패배'}</span>
              <span className="scoreboard-team-kills">
                {totalKills}킬
                {teamData && (
                  <>{' '}· 바론 {teamData.baronKills ?? 0}{' '}· 드래곤 {teamData.dragonKills ?? 0}{' '}· 포탑 {teamData.towerKills ?? 0}</>
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
                  const isAce     = p.riotId === mvp.aceId;
                  const isTeamMvp = !isAce && p.riotId === (side === 'blue' ? mvp.blueMvpId : mvp.redMvpId);
                  const dmgPct    = (p.damage / maxDmg) * 100;
                  const nameKo    = champions.get(p.championId)?.nameKo || p.champion;
                  return (
                    <tr key={i} className={isAce ? 'sb-row-ace' : isTeamMvp ? 'sb-row-mvp' : ''}>
                      <td>
                        <div className="sb-player">
                          <div className="sb-champ-spell-col">
                            <ChampionImg championId={p.championId} champion={p.champion} size={34} side={side} />
                            <div className="sb-spells">
                              <SpellImg spellId={p.spell1Id} />
                              <SpellImg spellId={p.spell2Id} />
                            </div>
                          </div>
                          <div className="sb-player-info">
                            <div className="sb-champion-name">
                              <ChampionLink champion={p.champion} championId={p.championId}>{nameKo}</ChampionLink>
                              {isAce && <span className="sb-mvp-badge sb-mvp-badge--ace"><Trophy size={9} />ACE</span>}
                              {isTeamMvp && <span className="sb-mvp-badge sb-mvp-badge--team">MVP</span>}
                            </div>
                            <PlayerLink riotId={p.riotId}>
                              <div className="sb-riot-id">{p.riotId.split('#')[0]}</div>
                            </PlayerLink>
                            <div className="sb-items">
                              {[p.item0, p.item1, p.item2, p.item3, p.item4, p.item5, p.item6].map((id, idx) => (
                                <ItemImg key={idx} itemId={id} />
                              ))}
                            </div>
                          </div>
                        </div>
                      </td>
                      <td style={{ textAlign: 'center' }}>
                        <span className={`sb-result ${p.win ? 'win' : 'loss'}`}>{p.win ? '승' : '패'}</span>
                      </td>
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
                      <td className="sb-damage-cell">
                        <div className="sb-damage-num">{p.damage.toLocaleString()}</div>
                        <div className="sb-damage-bar-track">
                          <div className={`sb-damage-bar-fill ${side}`} style={{ width: `${dmgPct}%` }} />
                        </div>
                      </td>
                      <td className="td-num">
                        <div>{p.cs}</div>
                        <div style={{ fontSize: 10, color: 'var(--color-text-secondary)' }}>
                          {(p.cs / Math.max(match.gameDuration / 60, 1)).toFixed(1)}/분
                        </div>
                      </td>
                      <td className="td-num">{p.gold.toLocaleString()}</td>
                      <td className="td-num">{p.visionScore}</td>
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

// ── Stat Tabs ──────────────────────────────────────────────────────────────
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
        <ChampionImg championId={p.championId} champion={p.champion} size={24} side={p.team} />
        <span>{p.champion}</span>
        <span className="sb-riot-id">{p.riotId.split('#')[0]}</span>
        <span className={`sb-result ${p.win ? 'win' : 'loss'}`}>{p.win ? '승' : '패'}</span>
        <span className="sb-kda-ratio" style={{ marginLeft: 'auto' }}>
          {p.kills}/{p.deaths}/{p.assists} · {kda(p)} KDA
        </span>
      </div>
      {renderStats()}
    </div>
  );
}

export function StatsTab({ match, tab }: { match: Match; tab: Tab }) {
  return (
    <div>
      {(['blue', 'red'] as const).map(side => (
        <div key={side} className="stat-section-heading">
          <div className={`stat-team-label--${side}`}>
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
export function TeamInfoTab({ teams }: { teams: Team[] }) {
  return (
    <div className="team-info-section">
      {teams.map(t => {
        const side = t.teamId === 100 ? 'blue' : 'red';
        const win  = t.win;
        return (
          <div key={t.teamId} className="team-info-block">
            <div className={`team-info-header ${side}-team`} style={{ borderLeft: `3px solid var(--color-${side === 'blue' ? 'blue' : 'red'})` }}>
              <span className={`scoreboard-team-name`} style={{ color: side === 'blue' ? 'var(--color-blue)' : 'var(--color-red)' }}>{side === 'blue' ? '블루팀' : '레드팀'}</span>
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
            <div className="grid-16" style={{ padding: '8px 14px', borderTop: '1px solid var(--color-border)' }}>
              {[
                { label: '첫 피바람', v: t.firstBlood },
                { label: '첫 포탑',  v: t.firstTower },
                { label: '첫 바론',  v: t.firstBaron },
                { label: '첫 억제기', v: t.firstInhibitor },
                { label: '첫 드래곤', v: t.firstDragon },
              ].filter(x => x.v).map(({ label }) => (
                <span key={label} className="col-span-4 team-info-first-badge">
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
export function MatchDetailModal({ match, onClose }: { match: Match; onClose: () => void }) {
  const [tab, setTab] = useState<Tab>('요약');

  return (
    <Modal isOpen title={`${QUEUE_LABEL[match.queueId] ?? match.queueId} — ${match.matchId}`}
      onClose={onClose} size="xl">
      <div className="match-detail-meta">
        <div className="match-detail-meta-item"><Clock size={13} /><strong>{fmt(match.gameDuration)}</strong></div>
        <div className="match-detail-meta-item"><Calendar size={13} /><strong>{new Date(match.gameCreation).toLocaleString('ko-KR')}</strong></div>
        {match.gameVersion && <div className="match-detail-meta-item"><span>버전</span><strong>{match.gameVersion}</strong></div>}
        {match.gameMode && <div className="match-detail-meta-item"><span>모드</span><strong>{match.gameMode}</strong></div>}
      </div>
      <div className="match-tabs">
        {TABS.map(t => (
          <button key={t} className={`match-tab ${tab === t ? 'active' : ''}`} onClick={() => setTab(t)}>{t}</button>
        ))}
      </div>
      {tab === '요약'     && <Scoreboard match={match} />}
      {tab === '팀 정보' && <TeamInfoTab teams={match.teams ?? []} />}
      {(tab === '딜/피해' || tab === '경제' || tab === '시야/오브젝트' || tab === '멀티킬') && (
        <StatsTab match={match} tab={tab} />
      )}
    </Modal>
  );
}

// ── Main Page ──────────────────────────────────────────────────────────────
export function MatchesPage() {
  const [matches, setMatches]   = useState<Match[]>([]);
  const [mode, setMode]         = useState('normal');
  const [loading, setLoading]   = useState(true);
  const navigate = useNavigate();

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

  // 세션 키 기준으로 그룹핑 (최신순)
  const groups: { key: string; matches: Match[] }[] = [];
  const groupMap = new Map<string, Match[]>();
  for (const m of matches) {
    const key = getSessionKey(m.gameCreation);
    if (!groupMap.has(key)) groupMap.set(key, []);
    groupMap.get(key)!.push(m);
  }
  for (const [key, ms] of groupMap) {
    groups.push({ key, matches: ms });
  }
  groups.sort((a, b) => b.key.localeCompare(a.key));

  return (
    <div>
      <div className="page-header flex items-center justify-between">
        <div>
          <h1 className="page-title">경기 목록</h1>
          <p className="page-subtitle">{matches.length}건 · {groups.length}일</p>
        </div>
        <div className="flex gap-sm">
          {MODES.map(m => (
            <Button key={m.value} variant={mode === m.value ? 'primary' : 'secondary'} size="sm" onClick={() => setMode(m.value)}>
              {m.label}
            </Button>
          ))}
        </div>
      </div>

      {loading ? (
        <div className="matches-list">
          {Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="match-group">
              <div className="match-group-header" style={{ pointerEvents: 'none' }}>
                <Skeleton className="h-4 w-40" />
                <Skeleton className="h-4 w-16" />
              </div>
              <div className="match-group-body">
                <div className="match-card">
                  <div className="match-card-header">
                    <div className="match-card-meta" style={{ gap: 8 }}>
                      <Skeleton className="h-4 w-20" />
                      <Skeleton className="h-4 w-14" />
                      <Skeleton className="h-4 w-14" />
                    </div>
                  </div>
                  <div className="match-teams">
                    <div className="match-team match-team-blue">
                      <div className="match-team-label">
                        <Skeleton className="h-4 w-24" />
                      </div>
                      <div className="match-players">
                        {Array.from({ length: 5 }).map((_, j) => (
                          <div key={j} className="match-player-row">
                            <Skeleton className="h-4 w-4 rounded" />
                            <Skeleton className="h-3 w-16" />
                            <Skeleton className="h-3 w-12" />
                          </div>
                        ))}
                      </div>
                    </div>
                    <div className="match-vs">VS</div>
                    <div className="match-team match-team-red">
                      <div className="match-team-label">
                        <Skeleton className="h-4 w-24" />
                      </div>
                      <div className="match-players">
                        {Array.from({ length: 5 }).map((_, j) => (
                          <div key={j} className="match-player-row">
                            <Skeleton className="h-4 w-4 rounded" />
                            <Skeleton className="h-3 w-16" />
                            <Skeleton className="h-3 w-12" />
                          </div>
                        ))}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="matches-list">
          {groups.length === 0 && (
            <div style={{ textAlign: 'center', padding: '60px 0', color: 'var(--color-text-secondary)' }}>
              경기 데이터가 없습니다
            </div>
          )}
          {groups.map((g, idx) => (
            <MatchGroup
              key={g.key}
              sessionKey={g.key}
              matches={g.matches}
              defaultOpen={idx === 0}
              onOpen={(m) => navigate(`/matches/${m.matchId}`)}
              onDelete={handleDelete}
            />
          ))}
        </div>
      )}
    </div>
  );
}
