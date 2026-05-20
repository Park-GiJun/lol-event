import { useEffect, useState, useCallback, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronDown } from 'lucide-react';
import { api } from '../../lib/api/api';
import type { Match, Participant } from '../../lib/types/match';
import { useDragon } from '../../context/DragonContext';
import { LoadingCenter } from '../../components/common/Spinner';
import { fmt, calcMvp, MODES } from '../../lib/lol';

const QUEUE_LABEL: Record<number, string> = { 0: '커스텀', 3130: '5v5 내전', 3270: '칼바람' };
const DAY_NAMES = ['일', '월', '화', '수', '목', '금', '토'];

function getSessionKey(ts: number): string {
  const d = new Date(ts);
  if (d.getHours() < 17) d.setDate(d.getDate() - 1);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

function formatSessionLabel(key: string): string {
  const [y, m, d] = key.split('-').map(Number);
  const date = new Date(y, m - 1, d);
  return `${m}월 ${d}일 (${DAY_NAMES[date.getDay()]})`;
}

function ChampIcon({ championId, champion, size = 26, className }: { championId: number; champion: string; size?: number; className?: string }) {
  const { champions } = useDragon();
  const data = champions.get(championId);
  if (data?.imageUrl) {
    return (
      <img src={data.imageUrl} alt={champion}
        className={className}
        style={{ width: size, height: size, borderRadius: 4, objectFit: 'cover', flexShrink: 0 }}
        loading="lazy"
        onError={e => { (e.currentTarget as HTMLImageElement).style.display = 'none'; }}
      />
    );
  }
  return (
    <div style={{
      width: size, height: size, borderRadius: 4,
      background: 'rgba(255,255,255,0.05)', display: 'flex', alignItems: 'center',
      justifyContent: 'center', fontSize: 9, color: 'var(--color-text-disabled)', flexShrink: 0,
    }}>
      {champion.slice(0, 2)}
    </div>
  );
}

function PlayerLine({ p, accent, mvpBg, isAce, isMvp }: { p: Participant; accent: string; mvpBg: string; isAce: boolean; isMvp: boolean }) {
  const { champions } = useDragon();
  const nameKo = champions.get(p.championId)?.nameKo ?? p.champion;
  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 5,
      padding: '3px 5px', borderRadius: 6,
      background: isAce ? 'rgba(255,215,0,0.08)' : isMvp ? mvpBg : 'transparent',
    }}>
      <ChampIcon championId={p.championId} champion={p.champion} size={22} />
      <span style={{
        flex: 1, minWidth: 0, fontSize: 10, color: 'var(--color-text-secondary)',
        overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
      }}>
        {nameKo}
        {isAce && <span style={{ marginLeft: 3, fontSize: 8, fontWeight: 700, color: '#FFD700' }}>ACE</span>}
        {isMvp && <span style={{ marginLeft: 3, fontSize: 8, fontWeight: 700, color: accent }}>MVP</span>}
      </span>
      <span style={{ fontSize: 10, fontWeight: 700, flexShrink: 0, whiteSpace: 'nowrap', color: 'var(--color-text-primary)' }}>
        {p.kills}<span style={{ color: 'var(--color-text-disabled)', fontWeight: 400 }}>/</span>
        <span style={{ color: 'var(--color-loss)' }}>{p.deaths}</span>
        <span style={{ color: 'var(--color-text-disabled)', fontWeight: 400 }}>/</span>
        {p.assists}
      </span>
    </div>
  );
}

function MatchCard({ match }: { match: Match }) {
  const navigate = useNavigate();

  const blue = match.participants.filter(p => p.team === 'blue');
  const red  = match.participants.filter(p => p.team === 'red');
  const blueWin = blue[0]?.win ?? false;
  const blueTeam = match.teams?.find(t => t.teamId === 100);
  const redTeam  = match.teams?.find(t => t.teamId === 200);
  const mvp = calcMvp(match);
  const blueKills = blue.reduce((s, p) => s + p.kills, 0);
  const redKills = red.reduce((s, p) => s + p.kills, 0);
  const timeStr = new Date(match.gameCreation).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', hour12: false });
  const modeLabel = QUEUE_LABEL[match.queueId] ?? String(match.queueId);

  const borderColor = blueWin ? 'var(--color-info)' : 'var(--color-loss)';

  return (
    <div
      style={{
        background: 'var(--bg-surface)',
        border: '1px solid var(--color-border)',
        borderLeft: `3px solid ${borderColor}`,
        borderRadius: 12,
        marginBottom: 8,
        overflow: 'hidden',
      }}
    >
      {/* ── 헤더: 결과 + 모드 + 시간 (클릭 시 상세 이동) ── */}
      <div
        onClick={() => navigate(`/m/match/${encodeURIComponent(match.matchId)}`)}
        style={{
          display: 'flex', alignItems: 'center', gap: 8,
          padding: '8px 12px', cursor: 'pointer',
          borderBottom: '1px solid var(--color-border)',
        }}
      >
        <span style={{
          fontSize: 11, fontWeight: 700, padding: '2px 8px', borderRadius: 6, flexShrink: 0,
          background: blueWin ? 'rgba(59,158,255,0.12)' : 'rgba(232,64,64,0.12)',
          color: blueWin ? 'var(--color-info)' : 'var(--color-loss)',
          border: `1px solid ${blueWin ? 'rgba(59,158,255,0.25)' : 'rgba(232,64,64,0.25)'}`,
        }}>
          {blueWin ? '블루 승' : '레드 승'}
        </span>
        <span style={{ fontSize: 11, color: 'var(--color-text-secondary)', fontWeight: 600 }}>{modeLabel}</span>
        <span style={{ fontSize: 11, color: 'var(--color-text-disabled)' }}>·</span>
        <span style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>{fmt(match.gameDuration)}</span>
        <span style={{ marginLeft: 'auto', fontSize: 11, color: 'var(--color-text-disabled)' }}>{timeStr}</span>
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--color-text-disabled)" strokeWidth="2">
          <polyline points="9 18 15 12 9 6" />
        </svg>
      </div>

      {/* ── 양 팀 로스터 (한눈에) ── */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 6, padding: '8px 10px' }}>
        {/* 블루팀 */}
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 5, marginBottom: 3, padding: '0 5px' }}>
            <span style={{ fontSize: 10, fontWeight: 700, color: 'var(--color-info)' }}>블루 {blueWin ? '승' : '패'}</span>
            {blueTeam && (
              <span style={{ fontSize: 9, color: 'var(--color-text-disabled)' }}>
                🐉{blueTeam.dragonKills} 🏰{blueTeam.towerKills}{blueTeam.baronKills > 0 ? ` 🟣${blueTeam.baronKills}` : ''}
              </span>
            )}
            <span style={{ marginLeft: 'auto', fontSize: 10, fontWeight: 700, color: blueWin ? 'var(--color-win)' : 'var(--color-loss)' }}>{blueKills}킬</span>
          </div>
          {blue.map((p, i) => (
            <PlayerLine key={i} p={p} accent="var(--color-info)" mvpBg="rgba(59,158,255,0.10)"
              isAce={p.riotId === mvp.aceId}
              isMvp={p.riotId !== mvp.aceId && p.riotId === mvp.blueMvpId} />
          ))}
        </div>

        {/* 레드팀 */}
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 5, marginBottom: 3, padding: '0 5px' }}>
            <span style={{ fontSize: 10, fontWeight: 700, color: 'var(--color-loss)' }}>레드 {!blueWin ? '승' : '패'}</span>
            {redTeam && (
              <span style={{ fontSize: 9, color: 'var(--color-text-disabled)' }}>
                🐉{redTeam.dragonKills} 🏰{redTeam.towerKills}{redTeam.baronKills > 0 ? ` 🟣${redTeam.baronKills}` : ''}
              </span>
            )}
            <span style={{ marginLeft: 'auto', fontSize: 10, fontWeight: 700, color: !blueWin ? 'var(--color-win)' : 'var(--color-loss)' }}>{redKills}킬</span>
          </div>
          {red.map((p, i) => (
            <PlayerLine key={i} p={p} accent="var(--color-loss)" mvpBg="rgba(232,64,64,0.10)"
              isAce={p.riotId === mvp.aceId}
              isMvp={p.riotId !== mvp.aceId && p.riotId === mvp.redMvpId} />
          ))}
        </div>
      </div>
    </div>
  );
}

function SessionGroup({ sessionKey, matches, defaultOpen }: {
  sessionKey: string; matches: Match[]; defaultOpen: boolean;
}) {
  const [open, setOpen] = useState(defaultOpen);
  const label = formatSessionLabel(sessionKey);
  const wins = matches.filter(m => m.participants.find(p => p.team === 'blue')?.win).length;

  return (
    <div style={{ marginBottom: 4 }}>
      <button
        onClick={() => setOpen(o => !o)}
        style={{
          display: 'flex', alignItems: 'center', gap: 8, width: '100%',
          background: 'none', border: 'none', padding: '8px 4px',
          cursor: 'pointer', color: 'var(--color-text-primary)',
        }}
      >
        {open ? <ChevronDown size={14} /> : <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><polyline points="9 18 15 12 9 6" /></svg>}
        <span style={{ fontSize: 13, fontWeight: 700 }}>{label}</span>
        <span style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>{matches.length}경기</span>
        <span style={{ marginLeft: 'auto', fontSize: 11 }}>
          <span style={{ color: 'var(--color-win)' }}>{wins}승</span>
          <span style={{ color: 'var(--color-text-disabled)' }}> {matches.length - wins}패</span>
        </span>
      </button>
      {open && matches.map(m => <MatchCard key={m.matchId} match={m} />)}
    </div>
  );
}

export function MobileMatchesPage() {
  const [matches, setMatches] = useState<Match[]>([]);
  const [mode, setMode] = useState('normal');
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setMatches(await api.get<Match[]>(`/matches?mode=${mode}`));
    } finally {
      setLoading(false);
    }
  }, [mode]);

  useEffect(() => { load(); }, [load]);

  const groups = useMemo(() => {
    const groupMap = new Map<string, Match[]>();
    for (const m of matches) {
      const key = getSessionKey(m.gameCreation);
      if (!groupMap.has(key)) groupMap.set(key, []);
      groupMap.get(key)!.push(m);
    }
    return Array.from(groupMap.entries())
      .map(([key, ms]) => ({ key, matches: ms }))
      .sort((a, b) => b.key.localeCompare(a.key));
  }, [matches]);

  return (
    <div>
      {/* Mode chips */}
      <div className="m-sort-chips">
        {MODES.map(m => (
          <button key={m.value} className={`m-sort-chip${mode === m.value ? ' active' : ''}`} onClick={() => setMode(m.value)}>
            {m.label}
          </button>
        ))}
      </div>

      {loading ? <LoadingCenter /> : (
        <>
          {groups.length === 0 && <div className="m-empty">경기 데이터가 없습니다</div>}
          {groups.map((g, i) => (
            <SessionGroup key={g.key} sessionKey={g.key} matches={g.matches} defaultOpen={i === 0} />
          ))}
        </>
      )}
    </div>
  );
}
