import { useEffect, useState, useCallback } from 'react';
import { Trash2 } from 'lucide-react';
import { api } from '../lib/api/api';
import type { Match, Participant, Team, ParticipantStats } from '../lib/types/match';
import { Button } from '../components/common/Button';
import { Modal } from '../components/common/Modal';
import { LoadingCenter } from '../components/common/Spinner';

const MODES = [
  { value: 'normal', label: '5v5 내전' },
  { value: 'aram',   label: '칼바람' },
  { value: 'all',    label: '전체' },
];

const QUEUE_LABEL: Record<number, string> = { 0: '커스텀', 3130: '5v5 내전', 3270: '칼바람' };

const TABS = ['요약', '딜/피해', '경제', '시야/오브젝트', '멀티킬', '룬/증강', '팀 정보'] as const;
type Tab = typeof TABS[number];

function fmt(ms: number) {
  const m = Math.floor(ms / 60);
  const s = ms % 60;
  return `${m}:${String(s).padStart(2, '0')}`;
}

function n(v: unknown): number {
  return typeof v === 'number' ? v : 0;
}

function StatRow({ label, value }: { label: string; value: string | number }) {
  return (
    <div style={{ display: 'flex', justifyContent: 'space-between', padding: '3px 0', borderBottom: '1px solid var(--color-border)', fontSize: 'var(--font-size-xs)' }}>
      <span style={{ color: 'var(--color-text-secondary)' }}>{label}</span>
      <span style={{ fontVariantNumeric: 'tabular-nums', fontWeight: 500 }}>{typeof value === 'number' ? value.toLocaleString() : value}</span>
    </div>
  );
}

function StatGrid({ children }: { children: React.ReactNode }) {
  return (
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: '0 24px' }}>
      {children}
    </div>
  );
}

function ParticipantSection({ p, tab }: { p: Participant; tab: Tab }) {
  const s = (p.stats ?? {}) as Partial<ParticipantStats>;

  if (tab === '딜/피해') return (
    <StatGrid>
      <StatRow label="총 딜 (챔피언)" value={n(s.totalDamageDealtToChampions)} />
      <StatRow label="마법 딜 (챔피언)" value={n(s.magicDamageDealtToChampions)} />
      <StatRow label="물리 딜 (챔피언)" value={n(s.physicalDamageDealtToChampions)} />
      <StatRow label="참 피해 (챔피언)" value={n(s.trueDamageDealtToChampions)} />
      <StatRow label="총 딜 (전체)" value={n(s.totalDamageDealt)} />
      <StatRow label="마법 딜 (전체)" value={n(s.magicDamageDealt)} />
      <StatRow label="물리 딜 (전체)" value={n(s.physicalDamageDealt)} />
      <StatRow label="참 피해 (전체)" value={n(s.trueDamageDealt)} />
      <StatRow label="오브젝트 딜" value={n(s.damageDealtToObjectives)} />
      <StatRow label="포탑 딜" value={n(s.damageDealtToTurrets)} />
      <StatRow label="총 피해받음" value={n(s.totalDamageTaken)} />
      <StatRow label="마법 피해받음" value={n(s.magicalDamageTaken)} />
      <StatRow label="물리 피해받음" value={n(s.physicalDamageTaken)} />
      <StatRow label="참 피해받음" value={n(s.trueDamageTaken)} />
      <StatRow label="피해 경감" value={n(s.damageSelfMitigated)} />
      <StatRow label="총 치유량" value={n(s.totalHeal)} />
      <StatRow label="치유 대상 수" value={n(s.totalUnitsHealed)} />
      <StatRow label="CC 시간 (초)" value={n(s.timeCCingOthers)} />
      <StatRow label="총 CC 시간" value={n(s.totalTimeCrowdControlDealt)} />
    </StatGrid>
  );

  if (tab === '경제') return (
    <StatGrid>
      <StatRow label="골드 획득" value={n(s.goldEarned)} />
      <StatRow label="골드 소비" value={n(s.goldSpent)} />
      <StatRow label="미니언 처치" value={n(s.totalMinionsKilled)} />
      <StatRow label="중립 몬스터 (전체)" value={n(s.neutralMinionsKilled)} />
      <StatRow label="중립 (우리 정글)" value={n(s.neutralMinionsKilledTeamJungle)} />
      <StatRow label="중립 (적 정글)" value={n(s.neutralMinionsKilledEnemyJungle)} />
      <StatRow label="아이템 0" value={n(s.item0)} />
      <StatRow label="아이템 1" value={n(s.item1)} />
      <StatRow label="아이템 2" value={n(s.item2)} />
      <StatRow label="아이템 3" value={n(s.item3)} />
      <StatRow label="아이템 4" value={n(s.item4)} />
      <StatRow label="아이템 5" value={n(s.item5)} />
      <StatRow label="아이템 6 (장신구)" value={n(s.item6)} />
      <StatRow label="챔피언 레벨" value={n(s.champLevel)} />
    </StatGrid>
  );

  if (tab === '시야/오브젝트') return (
    <StatGrid>
      <StatRow label="시야 점수" value={n(s.visionScore)} />
      <StatRow label="와드 설치" value={n(s.wardsPlaced)} />
      <StatRow label="와드 제거" value={n(s.wardsKilled)} />
      <StatRow label="감지 와드 구매" value={n(s.sightWardsBoughtInGame)} />
      <StatRow label="제어 와드 구매" value={n(s.visionWardsBoughtInGame)} />
      <StatRow label="포탑 처치" value={n(s.turretKills)} />
      <StatRow label="억제기 처치" value={n(s.inhibitorKills)} />
      <StatRow label="첫 피바람" value={s.firstBloodKill ? '✓' : '-'} />
      <StatRow label="첫 피바람 어시" value={s.firstBloodAssist ? '✓' : '-'} />
      <StatRow label="첫 포탑" value={s.firstTowerKill ? '✓' : '-'} />
      <StatRow label="첫 포탑 어시" value={s.firstTowerAssist ? '✓' : '-'} />
      <StatRow label="첫 억제기" value={s.firstInhibitorKill ? '✓' : '-'} />
      <StatRow label="첫 억제기 어시" value={s.firstInhibitorAssist ? '✓' : '-'} />
      <StatRow label="생존 최장 시간 (초)" value={n(s.longestTimeSpentLiving)} />
    </StatGrid>
  );

  if (tab === '멀티킬') return (
    <StatGrid>
      <StatRow label="킬" value={n(s.kills)} />
      <StatRow label="데스" value={n(s.deaths)} />
      <StatRow label="어시스트" value={n(s.assists)} />
      <StatRow label="더블킬" value={n(s.doubleKills)} />
      <StatRow label="트리플킬" value={n(s.tripleKills)} />
      <StatRow label="쿼드라킬" value={n(s.quadraKills)} />
      <StatRow label="펜타킬" value={n(s.pentaKills)} />
      <StatRow label="언리얼킬" value={n(s.unrealKills)} />
      <StatRow label="최대 연속킬" value={n(s.largestKillingSpree)} />
      <StatRow label="킬 스트릭 횟수" value={n(s.killingSprees)} />
      <StatRow label="최대 멀티킬" value={n(s.largestMultiKill)} />
      <StatRow label="최대 크리티컬" value={n(s.largestCriticalStrike)} />
    </StatGrid>
  );

  if (tab === '룬/증강') return (
    <StatGrid>
      <StatRow label="스펠1 ID" value={p.spell1Id ?? 0} />
      <StatRow label="스펠2 ID" value={p.spell2Id ?? 0} />
      <StatRow label="주 룬 스타일" value={n(s.perkPrimaryStyle)} />
      <StatRow label="보조 룬 스타일" value={n(s.perkSubStyle)} />
      <StatRow label="룬 0" value={n(s.perk0)} />
      <StatRow label="룬 0 Var1/2/3" value={`${n(s.perk0Var1)} / ${n(s.perk0Var2)} / ${n(s.perk0Var3)}`} />
      <StatRow label="룬 1" value={n(s.perk1)} />
      <StatRow label="룬 1 Var1/2/3" value={`${n(s.perk1Var1)} / ${n(s.perk1Var2)} / ${n(s.perk1Var3)}`} />
      <StatRow label="룬 2" value={n(s.perk2)} />
      <StatRow label="룬 2 Var1/2/3" value={`${n(s.perk2Var1)} / ${n(s.perk2Var2)} / ${n(s.perk2Var3)}`} />
      <StatRow label="룬 3" value={n(s.perk3)} />
      <StatRow label="룬 3 Var1/2/3" value={`${n(s.perk3Var1)} / ${n(s.perk3Var2)} / ${n(s.perk3Var3)}`} />
      <StatRow label="룬 4" value={n(s.perk4)} />
      <StatRow label="룬 4 Var1/2/3" value={`${n(s.perk4Var1)} / ${n(s.perk4Var2)} / ${n(s.perk4Var3)}`} />
      <StatRow label="룬 5" value={n(s.perk5)} />
      <StatRow label="룬 5 Var1/2/3" value={`${n(s.perk5Var1)} / ${n(s.perk5Var2)} / ${n(s.perk5Var3)}`} />
      <StatRow label="증강 1" value={n(s.playerAugment1)} />
      <StatRow label="증강 2" value={n(s.playerAugment2)} />
      <StatRow label="증강 3" value={n(s.playerAugment3)} />
      <StatRow label="증강 4" value={n(s.playerAugment4)} />
      <StatRow label="증강 5" value={n(s.playerAugment5)} />
      <StatRow label="증강 6" value={n(s.playerAugment6)} />
      <StatRow label="서브팀 ID" value={n(s.playerSubteamId)} />
      <StatRow label="서브팀 순위" value={n(s.subteamPlacement)} />
    </StatGrid>
  );

  return null;
}

function TeamInfoSection({ teams }: { teams: Team[] }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
      {teams.map((t) => {
        const teamName = t.teamId === 100 ? '블루팀' : '레드팀';
        const badgeClass = t.teamId === 100 ? 'badge badge-blue' : 'badge badge-red';
        return (
          <div key={t.teamId}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
              <span className={badgeClass}>{teamName}</span>
              <span className={`badge badge-${t.win === 'Win' ? 'win' : 'loss'}`}>{t.win === 'Win' ? '승' : '패'}</span>
            </div>
            <StatGrid>
              <StatRow label="바론" value={t.baronKills} />
              <StatRow label="드래곤" value={t.dragonKills} />
              <StatRow label="포탑" value={t.towerKills} />
              <StatRow label="억제기" value={t.inhibitorKills} />
              <StatRow label="전령" value={t.riftHeraldKills} />
              <StatRow label="나서스 군주" value={t.hordeKills} />
              <StatRow label="빌마우" value={t.vilemawKills} />
              <StatRow label="첫 바론" value={t.firstBaron ? '✓' : '-'} />
              <StatRow label="첫 드래곤" value={t.firstDargon ? '✓' : '-'} />
              <StatRow label="첫 포탑" value={t.firstTower ? '✓' : '-'} />
              <StatRow label="첫 억제기" value={t.firstInhibitor ? '✓' : '-'} />
              <StatRow label="첫 피바람" value={t.firstBlood ? '✓' : '-'} />
            </StatGrid>
            {t.bans && t.bans.length > 0 && (
              <div style={{ marginTop: '8px' }}>
                <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)', marginBottom: '4px' }}>밴 목록</div>
                <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                  {t.bans.map((ban, i) => (
                    <span key={i} className="badge badge-secondary" style={{ fontSize: 'var(--font-size-xs)' }}>
                      #{ban.pickTurn} ID:{ban.championId}
                    </span>
                  ))}
                </div>
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
}

export function MatchesPage() {
  const [matches, setMatches] = useState<Match[]>([]);
  const [mode, setMode] = useState('normal');
  const [loading, setLoading] = useState(true);
  const [detail, setDetail] = useState<Match | null>(null);
  const [activeTab, setActiveTab] = useState<Tab>('요약');

  const load = useCallback(async () => {
    setLoading(true);
    try { setMatches(await api.get<Match[]>(`/matches?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode]);

  useEffect(() => { load(); }, [load]);

  const handleDelete = async (matchId: string) => {
    if (!confirm('삭제하시겠습니까?')) return;
    await api.delete(`/matches/${matchId}`);
    load();
  };

  const openDetail = (m: Match) => {
    setDetail(m);
    setActiveTab('요약');
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
            <Button key={m.value} variant={mode === m.value ? 'primary' : 'secondary'}
              size="sm" onClick={() => setMode(m.value)}>{m.label}</Button>
          ))}
        </div>
      </div>

      {loading ? <LoadingCenter /> : (
        <div className="card">
          <div className="table-wrapper">
            <table className="table">
              <thead><tr><th>경기 ID</th><th>모드</th><th>날짜</th><th>시간</th><th>버전</th><th>참가자</th><th></th></tr></thead>
              <tbody>
                {matches.map(m => (
                  <tr key={m.matchId} style={{ cursor: 'pointer' }} onClick={() => openDetail(m)}>
                    <td className="text-xs text-secondary">{m.matchId}</td>
                    <td><span className="badge badge-gold">{QUEUE_LABEL[m.queueId] ?? m.queueId}</span></td>
                    <td>{new Date(m.gameCreation).toLocaleDateString('ko-KR')}</td>
                    <td>{fmt(m.gameDuration)}</td>
                    <td className="text-xs text-secondary">{m.gameVersion ?? '-'}</td>
                    <td>{m.participants.length}명</td>
                    <td onClick={e => e.stopPropagation()}>
                      <Button variant="ghost" size="sm" onClick={() => handleDelete(m.matchId)}>
                        <Trash2 size={14} color="var(--color-error)" />
                      </Button>
                    </td>
                  </tr>
                ))}
                {!matches.length && (
                  <tr><td colSpan={7} style={{ textAlign: 'center', color: 'var(--color-text-secondary)', padding: '32px' }}>데이터 없음</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {detail && (
        <Modal isOpen={!!detail} onClose={() => setDetail(null)} title={`경기 상세 — ${detail.matchId}`} size="xl"
          footer={<Button onClick={() => setDetail(null)}>닫기</Button>}>

          {/* 경기 메타 */}
          <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap', marginBottom: '16px', fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)' }}>
            <span>모드: <strong>{QUEUE_LABEL[detail.queueId] ?? detail.queueId}</strong></span>
            <span>시간: <strong>{fmt(detail.gameDuration)}</strong></span>
            <span>날짜: <strong>{new Date(detail.gameCreation).toLocaleString('ko-KR')}</strong></span>
            {detail.gameVersion && <span>버전: <strong>{detail.gameVersion}</strong></span>}
            {detail.gameMode && <span>gameMode: <strong>{detail.gameMode}</strong></span>}
            {detail.gameType && <span>gameType: <strong>{detail.gameType}</strong></span>}
            {detail.mapId ? <span>맵ID: <strong>{detail.mapId}</strong></span> : null}
            {detail.seasonId ? <span>시즌: <strong>{detail.seasonId}</strong></span> : null}
          </div>

          {/* 탭 */}
          <div style={{ display: 'flex', gap: '4px', marginBottom: '16px', flexWrap: 'wrap' }}>
            {TABS.map(t => (
              <Button key={t} variant={activeTab === t ? 'primary' : 'secondary'} size="sm" onClick={() => setActiveTab(t)}>{t}</Button>
            ))}
          </div>

          {/* 팀 정보 탭 */}
          {activeTab === '팀 정보' && (
            <TeamInfoSection teams={detail.teams ?? []} />
          )}

          {/* 요약 탭 */}
          {activeTab === '요약' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              {(['blue', 'red'] as const).map(team => (
                <div key={team}>
                  <div className={`badge badge-${team}`} style={{ marginBottom: '8px' }}>{team === 'blue' ? '블루팀' : '레드팀'}</div>
                  <table className="table">
                    <thead>
                      <tr>
                        <th>플레이어</th><th>챔피언</th><th>결과</th>
                        <th className="table-number">K/D/A</th>
                        <th className="table-number">딜량</th>
                        <th className="table-number">CS</th>
                        <th className="table-number">골드</th>
                        <th className="table-number">시야</th>
                        <th className="table-number">레벨</th>
                      </tr>
                    </thead>
                    <tbody>
                      {detail.participants.filter(p => p.team === team).map((p, i) => (
                        <tr key={i}>
                          <td style={{ fontSize: 'var(--font-size-xs)' }}>{p.riotId}</td>
                          <td>{p.champion}</td>
                          <td><span className={`badge badge-${p.win ? 'win' : 'loss'}`}>{p.win ? '승' : '패'}</span></td>
                          <td className="table-number">{p.kills}/{p.deaths}/{p.assists}</td>
                          <td className="table-number">{p.damage.toLocaleString()}</td>
                          <td className="table-number">{p.cs}</td>
                          <td className="table-number">{p.gold.toLocaleString()}</td>
                          <td className="table-number">{p.visionScore}</td>
                          <td className="table-number">{(p.stats as Partial<ParticipantStats>)?.champLevel ?? '-'}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ))}
            </div>
          )}

          {/* 상세 스탯 탭 (팀별 → 플레이어별) */}
          {activeTab !== '요약' && activeTab !== '팀 정보' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
              {(['blue', 'red'] as const).map(team => (
                <div key={team}>
                  <div className={`badge badge-${team}`} style={{ marginBottom: '10px' }}>{team === 'blue' ? '블루팀' : '레드팀'}</div>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                    {detail.participants.filter(p => p.team === team).map((p, i) => (
                      <div key={i} style={{ background: 'var(--color-bg-hover)', borderRadius: 'var(--radius-md)', padding: '10px 14px' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px', fontSize: 'var(--font-size-sm)', fontWeight: 600 }}>
                          <span>{p.riotId}</span>
                          <span style={{ color: 'var(--color-text-secondary)' }}>— {p.champion}</span>
                          <span className={`badge badge-${p.win ? 'win' : 'loss'}`}>{p.win ? '승' : '패'}</span>
                        </div>
                        <ParticipantSection p={p} tab={activeTab} />
                      </div>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          )}
        </Modal>
      )}
    </div>
  );
}
