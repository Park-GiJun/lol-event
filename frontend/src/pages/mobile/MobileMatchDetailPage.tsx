import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { api } from '../../lib/api/api';
import type { Match, Participant } from '../../lib/types/match';
import { useDragon } from '../../context/DragonContext';
import { LoadingCenter } from '../../components/common/Spinner';

const QUEUE_LABEL: Record<number, string> = { 0: '커스텀', 3130: '5v5 내전', 3270: '칼바람' };

function fmt(secs: number) {
  return `${Math.floor(secs / 60)}:${String(secs % 60).padStart(2, '0')}`;
}

function calcMvp(match: Match): { aceId: string; blueMvpId: string; redMvpId: string } {
  const dur = Math.max(match.gameDuration / 60, 1);
  const score = (p: Participant) => {
    const teamDmg = match.participants.filter(x => x.team === p.team).reduce((s, x) => s + x.damage, 0) || 1;
    return (p.kills + p.assists) / Math.max(p.deaths, 1) * 10
      + (p.damage / teamDmg) * 40 + p.visionScore / dur + p.cs / dur + (p.win ? 20 : 0);
  };
  const scored = match.participants.map(p => ({ p, s: score(p) }));
  const aceId = scored.reduce((a, b) => a.s >= b.s ? a : b).p.riotId;
  const blueMvpId = scored.filter(x => x.p.team === 'blue').reduce((a, b) => a.s >= b.s ? a : b).p.riotId;
  const redMvpId  = scored.filter(x => x.p.team === 'red').reduce((a, b) => a.s >= b.s ? a : b).p.riotId;
  return { aceId, blueMvpId, redMvpId };
}

// ── 공통 이미지 컴포넌트 ──
function ChampIcon({ p, size = 40 }: { p: Participant; size?: number }) {
  const { champions } = useDragon();
  const data = champions.get(p.championId);
  if (data?.imageUrl) {
    return <img src={data.imageUrl} alt={p.champion} style={{ width: size, height: size, borderRadius: 6, objectFit: 'cover', flexShrink: 0 }}
      onError={e => { (e.currentTarget as HTMLImageElement).style.display = 'none'; }} />;
  }
  return <div style={{ width: size, height: size, borderRadius: 6, background: 'var(--color-bg-hover)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 11, color: 'var(--color-text-disabled)', flexShrink: 0 }}>{p.champion.slice(0, 2)}</div>;
}

function ItemIcon({ itemId }: { itemId: number }) {
  const { items } = useDragon();
  if (!itemId) return <div style={{ width: 20, height: 20, borderRadius: 3, background: 'var(--color-bg-hover)' }} />;
  const data = items.get(itemId);
  if (data?.imageUrl) {
    return <img src={data.imageUrl} alt="" style={{ width: 20, height: 20, borderRadius: 3, objectFit: 'cover' }}
      onError={e => { (e.currentTarget as HTMLImageElement).style.display = 'none'; }} />;
  }
  return <div style={{ width: 20, height: 20, borderRadius: 3, background: 'var(--color-bg-hover)' }} />;
}

// ── 요약 탭: 플레이어 카드 ──
function PlayerCard({ p, aceId, mvpId, maxDmg }: { p: Participant; aceId: string; mvpId: string; maxDmg: number }) {
  const { champions } = useDragon();
  const nameKo = champions.get(p.championId)?.nameKo ?? p.champion;
  const isAce = p.riotId === aceId;
  const isMvp = !isAce && p.riotId === mvpId;
  const kda = p.deaths === 0 ? 'Perfect' : ((p.kills + p.assists) / p.deaths).toFixed(2);
  const dmgPct = maxDmg > 0 ? (p.damage / maxDmg) * 100 : 0;
  const items = [p.item0, p.item1, p.item2, p.item3, p.item4, p.item5, p.item6];

  return (
    <div style={{
      display: 'flex', gap: 10, padding: '10px 12px', alignItems: 'flex-start',
      borderBottom: '1px solid var(--color-border)',
      background: isAce ? 'rgba(255,215,0,0.04)' : isMvp ? 'rgba(59,158,255,0.04)' : 'transparent',
    }}>
      {/* 챔피언 아이콘 */}
      <div style={{ position: 'relative', flexShrink: 0 }}>
        <ChampIcon p={p} size={40} />
        <div style={{
          position: 'absolute', bottom: -2, right: -2,
          fontSize: 9, fontWeight: 800, background: 'var(--color-bg-primary)',
          color: 'var(--color-text-secondary)', borderRadius: 3, padding: '0 2px', lineHeight: '14px',
        }}>{p.champLevel}</div>
      </div>

      {/* 메인 정보 */}
      <div style={{ flex: 1, minWidth: 0 }}>
        {/* 이름 + 배지 */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 5, marginBottom: 2 }}>
          <span style={{ fontSize: 12, fontWeight: 700, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{nameKo}</span>
          <span style={{ fontSize: 10, color: 'var(--color-text-disabled)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', minWidth: 0 }}>
            {p.riotId.split('#')[0]}
          </span>
          {isAce && <span style={{ fontSize: 9, fontWeight: 700, color: '#FFD700', background: 'rgba(255,215,0,0.15)', padding: '1px 5px', borderRadius: 4, flexShrink: 0 }}>ACE</span>}
          {isMvp && <span style={{ fontSize: 9, fontWeight: 700, color: 'var(--color-info)', background: 'rgba(59,158,255,0.15)', padding: '1px 5px', borderRadius: 4, flexShrink: 0 }}>MVP</span>}
        </div>

        {/* KDA + CS + 골드 */}
        <div style={{ display: 'flex', gap: 10, marginBottom: 4, alignItems: 'baseline' }}>
          <span style={{ fontSize: 13, fontWeight: 800 }}>
            {p.kills}/<span style={{ color: 'var(--color-loss)' }}>{p.deaths}</span>/{p.assists}
          </span>
          <span style={{ fontSize: 10, color: 'var(--color-text-secondary)', fontWeight: 600 }}>{kda} KDA</span>
          <span style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>CS {p.cs}</span>
          <span style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>{(p.gold / 1000).toFixed(1)}k</span>
        </div>

        {/* 아이템 */}
        <div style={{ display: 'flex', gap: 2, marginBottom: 5 }}>
          {items.map((id, i) => <ItemIcon key={i} itemId={id} />)}
        </div>

        {/* 딜량 바 */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <div style={{ flex: 1, height: 4, background: 'var(--color-bg-hover)', borderRadius: 2, overflow: 'hidden' }}>
            <div style={{
              width: `${dmgPct}%`, height: '100%', borderRadius: 2,
              background: p.team === 'blue' ? 'var(--color-info)' : 'var(--color-loss)',
            }} />
          </div>
          <span style={{ fontSize: 10, color: 'var(--color-text-disabled)', flexShrink: 0 }}>
            {(p.damage / 1000).toFixed(1)}k
          </span>
        </div>
      </div>
    </div>
  );
}

// ── 딜/피해 탭 ──
function DamageTab({ team }: { team: Participant[] }) {
  const maxDmg  = Math.max(...team.map(p => p.totalDamageDealtToChampions), 1);
  const maxTaken = Math.max(...team.map(p => p.totalDamageTaken), 1);
  const { champions } = useDragon();
  return (
    <>
      {team.map((p, i) => {
        const nameKo = champions.get(p.championId)?.nameKo ?? p.champion;
        return (
          <div key={i} style={{ padding: '8px 12px', borderBottom: '1px solid var(--color-border)' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 6 }}>
              <ChampIcon p={p} size={28} />
              <span style={{ fontSize: 12, fontWeight: 600 }}>{nameKo}</span>
              <span style={{ fontSize: 10, color: 'var(--color-text-disabled)', marginLeft: 2 }}>{p.riotId.split('#')[0]}</span>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
              <StatBar label="가한 딜" value={p.totalDamageDealtToChampions} max={maxDmg} color="var(--color-info)" />
              <StatBar label="받은 딜" value={p.totalDamageTaken} max={maxTaken} color="var(--color-loss)" />
            </div>
          </div>
        );
      })}
    </>
  );
}

// ── 경제 탭 ──
function EconomyTab({ team }: { team: Participant[] }) {
  const maxGold = Math.max(...team.map(p => p.gold), 1);
  const { champions } = useDragon();
  return (
    <>
      {team.map((p, i) => {
        const nameKo = champions.get(p.championId)?.nameKo ?? p.champion;
        return (
          <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '8px 12px', borderBottom: '1px solid var(--color-border)' }}>
            <ChampIcon p={p} size={32} />
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 12, fontWeight: 600, marginBottom: 3 }}>{nameKo}</div>
              <div style={{ display: 'flex', gap: 12 }}>
                <div><div style={{ fontSize: 13, fontWeight: 700 }}>{p.cs}</div><div style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>CS</div></div>
                <div><div style={{ fontSize: 13, fontWeight: 700 }}>{(p.gold / 1000).toFixed(1)}k</div><div style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>골드</div></div>
                <div><div style={{ fontSize: 13, fontWeight: 700 }}>{p.visionScore}</div><div style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>시야</div></div>
              </div>
            </div>
            <div style={{ width: 36, height: 36, borderRadius: '50%', border: '2px solid var(--color-border)', display: 'flex', alignItems: 'center', justifyContent: 'center', flexDirection: 'column', flexShrink: 0 }}>
              <div style={{ fontSize: 10, fontWeight: 800, color: 'var(--color-primary)' }}>{Math.round((p.gold / maxGold) * 100)}%</div>
            </div>
          </div>
        );
      })}
    </>
  );
}

function StatBar({ label, value, max, color }: { label: string; value: number; max: number; color: string }) {
  const pct = max > 0 ? (value / max) * 100 : 0;
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
      <span style={{ fontSize: 10, color: 'var(--color-text-disabled)', width: 48, flexShrink: 0 }}>{label}</span>
      <div style={{ flex: 1, height: 6, background: 'var(--color-bg-hover)', borderRadius: 3, overflow: 'hidden' }}>
        <div style={{ width: `${pct}%`, height: '100%', background: color, borderRadius: 3 }} />
      </div>
      <span style={{ fontSize: 10, color: 'var(--color-text-secondary)', width: 38, textAlign: 'right', flexShrink: 0 }}>
        {(value / 1000).toFixed(1)}k
      </span>
    </div>
  );
}

// ── 팀 정보 탭 ──
function TeamInfoTab({ match }: { match: Match }) {
  const blue = match.teams?.find(t => t.teamId === 100);
  const red  = match.teams?.find(t => t.teamId === 200);
  const blueWin = match.participants.find(p => p.team === 'blue')?.win ?? false;
  if (!blue || !red) return null;

  const rows = [
    { label: '킬', blue: match.participants.filter(p => p.team === 'blue').reduce((s, p) => s + p.kills, 0), red: match.participants.filter(p => p.team === 'red').reduce((s, p) => s + p.kills, 0) },
    { label: '바론', blue: blue.baronKills, red: red.baronKills },
    { label: '드래곤', blue: blue.dragonKills, red: red.dragonKills },
    { label: '포탑', blue: blue.towerKills, red: red.towerKills },
    { label: '억제기', blue: blue.inhibitorKills, red: red.inhibitorKills },
    { label: '전령', blue: blue.riftHeraldKills, red: red.riftHeraldKills },
  ];

  return (
    <div style={{ padding: '12px 0' }}>
      {/* 팀 헤더 */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr auto 1fr', gap: 8, padding: '0 12px 12px', borderBottom: '1px solid var(--color-border)' }}>
        <div style={{ textAlign: 'left' }}>
          <div style={{ fontSize: 13, fontWeight: 800, color: 'var(--color-info)' }}>블루팀</div>
          <div style={{ fontSize: 11, color: blueWin ? 'var(--color-win)' : 'var(--color-loss)' }}>{blueWin ? '승리' : '패배'}</div>
        </div>
        <div style={{ fontSize: 11, color: 'var(--color-text-disabled)', alignSelf: 'center' }}>VS</div>
        <div style={{ textAlign: 'right' }}>
          <div style={{ fontSize: 13, fontWeight: 800, color: 'var(--color-loss)' }}>레드팀</div>
          <div style={{ fontSize: 11, color: !blueWin ? 'var(--color-win)' : 'var(--color-loss)' }}>{!blueWin ? '승리' : '패배'}</div>
        </div>
      </div>

      {/* 통계 행 */}
      {rows.map(r => (
        <div key={r.label} style={{ display: 'grid', gridTemplateColumns: '1fr 60px 1fr', alignItems: 'center', padding: '8px 12px', borderBottom: '1px solid var(--color-border)' }}>
          <div style={{ fontSize: 16, fontWeight: 800, color: r.blue > r.red ? 'var(--color-text-primary)' : 'var(--color-text-disabled)', textAlign: 'left' }}>
            {r.blue}
          </div>
          <div style={{ fontSize: 11, color: 'var(--color-text-secondary)', textAlign: 'center', fontWeight: 600 }}>{r.label}</div>
          <div style={{ fontSize: 16, fontWeight: 800, color: r.red > r.blue ? 'var(--color-text-primary)' : 'var(--color-text-disabled)', textAlign: 'right' }}>
            {r.red}
          </div>
        </div>
      ))}

      {/* 퍼스트 배지 */}
      <div style={{ padding: '10px 12px', display: 'flex', flexWrap: 'wrap', gap: 6 }}>
        {blue.firstBlood && <Badge label="퍼스트 블러드" team="blue" />}
        {red.firstBlood && <Badge label="퍼스트 블러드" team="red" />}
        {blue.firstTower && <Badge label="퍼스트 포탑" team="blue" />}
        {red.firstTower && <Badge label="퍼스트 포탑" team="red" />}
        {blue.firstBaron && <Badge label="퍼스트 바론" team="blue" />}
        {red.firstBaron && <Badge label="퍼스트 바론" team="red" />}
        {blue.firstDragon && <Badge label="퍼스트 드래곤" team="blue" />}
        {red.firstDragon && <Badge label="퍼스트 드래곤" team="red" />}
      </div>
    </div>
  );
}

function Badge({ label, team }: { label: string; team: 'blue' | 'red' }) {
  const color = team === 'blue' ? 'var(--color-info)' : 'var(--color-loss)';
  return (
    <span style={{
      fontSize: 10, fontWeight: 600, padding: '3px 8px', borderRadius: 20,
      background: team === 'blue' ? 'rgba(59,158,255,0.1)' : 'rgba(232,64,64,0.1)',
      border: `1px solid ${color}`, color,
    }}>
      {team === 'blue' ? '🔵' : '🔴'} {label}
    </span>
  );
}

// ── 팀 섹션 래퍼 ──
function TeamSection({ match, side, tab, mvp }: {
  match: Match;
  side: 'blue' | 'red';
  tab: string;
  mvp: ReturnType<typeof calcMvp>;
}) {
  const team = match.participants.filter(p => p.team === side);
  const win = team[0]?.win ?? false;
  const maxDmg = Math.max(...match.participants.map(p => p.damage), 1);
  const teamData = match.teams?.find(t => t.teamId === (side === 'blue' ? 100 : 200));
  const totalKills = team.reduce((s, p) => s + p.kills, 0);
  const color = side === 'blue' ? 'var(--color-info)' : 'var(--color-loss)';
  const mvpId = side === 'blue' ? mvp.blueMvpId : mvp.redMvpId;

  return (
    <div style={{ marginBottom: 8 }}>
      {/* 팀 헤더 */}
      <div style={{
        display: 'flex', alignItems: 'center', gap: 8,
        padding: '8px 12px', background: 'var(--color-bg-hover)',
        borderBottom: '1px solid var(--color-border)',
      }}>
        <span style={{ fontSize: 13, fontWeight: 800, color }}>{side === 'blue' ? '블루팀' : '레드팀'}</span>
        <span style={{
          fontSize: 11, fontWeight: 700, padding: '1px 7px', borderRadius: 4,
          background: win ? 'rgba(16,185,129,0.15)' : 'rgba(239,68,68,0.1)',
          color: win ? 'var(--color-win)' : 'var(--color-loss)',
        }}>{win ? '승리' : '패배'}</span>
        <span style={{ fontSize: 11, color: 'var(--color-text-secondary)', marginLeft: 2 }}>
          {totalKills}킬
          {teamData && ` · 드래곤 ${teamData.dragonKills} · 포탑 ${teamData.towerKills}`}
        </span>
      </div>

      {/* 플레이어 목록 */}
      {tab === '요약' && team.map((p, i) => (
        <PlayerCard key={i} p={p} aceId={mvp.aceId} mvpId={mvpId} maxDmg={maxDmg} />
      ))}
      {tab === '딜/피해' && <DamageTab team={team} />}
      {tab === '경제' && <EconomyTab team={team} />}
    </div>
  );
}

// ── 메인 페이지 ──
const TABS = ['요약', '딜/피해', '경제', '팀정보'] as const;
type Tab = typeof TABS[number];

export function MobileMatchDetailPage() {
  const { matchId } = useParams<{ matchId: string }>();
  const navigate = useNavigate();
  const [data, setData] = useState<Match | null>(null);
  const [loading, setLoading] = useState(true);
  const [tab, setTab] = useState<Tab>('요약');

  useEffect(() => {
    if (!matchId) { navigate('/m/matches', { replace: true }); return; }
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setLoading(true);
    api.get<Match>(`/matches/${encodeURIComponent(matchId)}`)
      .then(setData)
      .catch(() => setData(null))
      .finally(() => setLoading(false));
  }, [matchId, navigate]);

  if (loading) return <LoadingCenter />;

  if (!data) return (
    <div style={{ textAlign: 'center', padding: '60px 0', color: 'var(--color-text-secondary)' }}>
      경기 데이터를 불러올 수 없습니다.
    </div>
  );

  const blueWin = data.participants.find(p => p.team === 'blue')?.win ?? false;
  const mvp = calcMvp(data);
  const modeLabel = QUEUE_LABEL[data.queueId] ?? String(data.queueId);
  const dateStr = new Date(data.gameCreation).toLocaleDateString('ko-KR', { month: 'long', day: 'numeric' });

  return (
    <div>
      {/* 경기 메타 정보 */}
      <div style={{ fontSize: 11, color: 'var(--color-text-disabled)', marginBottom: 12, textAlign: 'center' }}>
        {modeLabel} · {fmt(data.gameDuration)} · {dateStr}
      </div>

      {/* 결과 배너 */}
      <div style={{
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        padding: '10px 14px', borderRadius: 10, marginBottom: 12,
        background: blueWin ? 'rgba(59,158,255,0.08)' : 'rgba(232,64,64,0.08)',
        border: `1px solid ${blueWin ? 'rgba(59,158,255,0.2)' : 'rgba(232,64,64,0.2)'}`,
      }}>
        <div>
          <div style={{ fontSize: 14, fontWeight: 800, color: blueWin ? 'var(--color-info)' : 'var(--color-loss)' }}>
            {blueWin ? '🔵 블루팀 승리' : '🔴 레드팀 승리'}
          </div>
          <div style={{ fontSize: 11, color: 'var(--color-text-secondary)', marginTop: 2 }}>
            {data.participants.filter(p => p.team === 'blue').reduce((s, p) => s + p.kills, 0)}
            {' '}<span style={{ color: 'var(--color-text-disabled)' }}>:</span>{' '}
            {data.participants.filter(p => p.team === 'red').reduce((s, p) => s + p.kills, 0)} 킬
          </div>
        </div>
        <div style={{ textAlign: 'right' }}>
          <div style={{ fontSize: 11, color: 'var(--color-text-disabled)' }}>게임 시간</div>
          <div style={{ fontSize: 13, fontWeight: 700 }}>{fmt(data.gameDuration)}</div>
        </div>
      </div>

      {/* 탭 */}
      <div className="m-tab-bar" style={{ marginBottom: 8 }}>
        {TABS.map(t => (
          <button key={t} className={`m-tab${tab === t ? ' active' : ''}`} onClick={() => setTab(t)}>
            {t}
          </button>
        ))}
      </div>

      {/* 본문 */}
      <div style={{ borderRadius: 10, overflow: 'hidden', border: '1px solid var(--color-border)', background: 'var(--color-bg-card)' }}>
        {tab === '팀정보' ? (
          <TeamInfoTab match={data} />
        ) : (
          <>
            <TeamSection match={data} side="blue" tab={tab} mvp={mvp} />
            <div style={{ height: 6, background: 'var(--color-bg-primary)' }} />
            <TeamSection match={data} side="red" tab={tab} mvp={mvp} />
          </>
        )}
      </div>
    </div>
  );
}
