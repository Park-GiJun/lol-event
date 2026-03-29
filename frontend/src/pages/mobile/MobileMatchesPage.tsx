import { useEffect, useState, useCallback, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronDown, ChevronUp } from 'lucide-react';
import { api } from '../../lib/api/api';
import type { Match } from '../../lib/types/match';
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

function ChampIcon({ championId, champion, size = 26 }: { championId: number; champion: string; size?: number }) {
  const { champions } = useDragon();
  const data = champions.get(championId);
  if (data?.imageUrl) {
    return (
      <img src={data.imageUrl} alt={champion}
        style={{ width: size, height: size, borderRadius: 4, objectFit: 'cover', flexShrink: 0 }}
        loading="lazy"
        onError={e => { (e.currentTarget as HTMLImageElement).style.display = 'none'; }}
      />
    );
  }
  return (
    <div style={{
      width: size, height: size, borderRadius: 4,
      background: 'var(--color-bg-hover)', display: 'flex', alignItems: 'center',
      justifyContent: 'center', fontSize: 9, color: 'var(--color-text-disabled)', flexShrink: 0,
    }}>
      {champion.slice(0, 2)}
    </div>
  );
}

function MatchCard({ match }: { match: Match }) {
  const navigate = useNavigate();
  const { champions } = useDragon();
  const [expanded, setExpanded] = useState(false);

  const blue = match.participants.filter(p => p.team === 'blue');
  const red  = match.participants.filter(p => p.team === 'red');
  const blueWin = blue[0]?.win ?? false;
  const blueTeam = match.teams?.find(t => t.teamId === 100);
  const redTeam  = match.teams?.find(t => t.teamId === 200);
  const mvp = calcMvp(match);
  const timeStr = new Date(match.gameCreation).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', hour12: false });
  const modeLabel = QUEUE_LABEL[match.queueId] ?? String(match.queueId);

  const borderColor = blueWin ? 'var(--color-info)' : 'var(--color-loss)';

  return (
    <div
      style={{
        background: 'var(--color-bg-card)',
        border: '1px solid var(--color-border)',
        borderLeft: `3px solid ${borderColor}`,
        borderRadius: 10,
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
          fontSize: 11, fontWeight: 700, padding: '2px 8px', borderRadius: 5, flexShrink: 0,
          background: blueWin ? 'rgba(59,158,255,0.15)' : 'rgba(232,64,64,0.15)',
          color: blueWin ? 'var(--color-info)' : 'var(--color-loss)',
          border: `1px solid ${blueWin ? 'rgba(59,158,255,0.3)' : 'rgba(232,64,64,0.3)'}`,
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

      {/* ── 챔피언 아이콘 행 ── */}
      <div style={{ padding: '8px 12px' }}>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr auto 1fr', gap: 8, alignItems: 'center' }}>
          {/* 블루팀 */}
          <div>
            <div style={{ display: 'flex', gap: 3, marginBottom: 4, flexWrap: 'wrap' }}>
              {blue.map((p, i) => <ChampIcon key={i} championId={p.championId} champion={p.champion} size={28} />)}
            </div>
            <div style={{ display: 'flex', gap: 6, fontSize: 10, color: 'var(--color-text-disabled)' }}>
              {blueTeam && (
                <>
                  <span>🐉{blueTeam.dragonKills}</span>
                  <span>🏰{blueTeam.towerKills}</span>
                  {blueTeam.baronKills > 0 && <span>🟣{blueTeam.baronKills}</span>}
                </>
              )}
              <span style={{ marginLeft: 'auto', color: blueWin ? 'var(--color-win)' : 'var(--color-loss)', fontWeight: 700 }}>
                {blue.reduce((s, p) => s + p.kills, 0)}킬
              </span>
            </div>
          </div>

          {/* VS */}
          <div style={{ fontSize: 10, fontWeight: 700, color: 'var(--color-text-disabled)', textAlign: 'center' }}>VS</div>

          {/* 레드팀 */}
          <div>
            <div style={{ display: 'flex', gap: 3, marginBottom: 4, flexWrap: 'wrap', justifyContent: 'flex-end' }}>
              {red.map((p, i) => <ChampIcon key={i} championId={p.championId} champion={p.champion} size={28} />)}
            </div>
            <div style={{ display: 'flex', gap: 6, fontSize: 10, color: 'var(--color-text-disabled)', justifyContent: 'flex-end' }}>
              <span style={{ marginRight: 'auto', color: blueWin ? 'var(--color-loss)' : 'var(--color-win)', fontWeight: 700 }}>
                {red.reduce((s, p) => s + p.kills, 0)}킬
              </span>
              {redTeam && (
                <>
                  {redTeam.baronKills > 0 && <span>🟣{redTeam.baronKills}</span>}
                  <span>🏰{redTeam.towerKills}</span>
                  <span>🐉{redTeam.dragonKills}</span>
                </>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* ── 플레이어 펼치기 버튼 ── */}
      <button
        onClick={() => setExpanded(e => !e)}
        style={{
          width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center',
          gap: 4, padding: '5px 0', background: 'none', border: 'none',
          borderTop: '1px solid var(--color-border)',
          fontSize: 11, color: 'var(--color-text-disabled)', cursor: 'pointer',
        }}
      >
        {expanded ? <><ChevronUp size={12} />접기</> : <><ChevronDown size={12} />플레이어 보기</>}
      </button>

      {/* ── 펼쳐진 플레이어 목록 ── */}
      {expanded && (
        <div style={{ borderTop: '1px solid var(--color-border)' }}>
          {/* 블루팀 */}
          <div style={{ padding: '6px 12px 2px', fontSize: 10, fontWeight: 700, color: 'var(--color-info)' }}>
            블루팀 {blueWin ? '✓ 승' : '패'}
          </div>
          {blue.map((p, i) => {
            const nameKo = champions.get(p.championId)?.nameKo ?? p.champion;
            const isAce = p.riotId === mvp.aceId;
            const isMvp = !isAce && p.riotId === mvp.blueMvpId;
            return (
              <div key={i} style={{
                display: 'flex', alignItems: 'center', gap: 8,
                padding: '5px 12px',
                background: isAce ? 'rgba(255,215,0,0.05)' : isMvp ? 'rgba(59,158,255,0.05)' : 'transparent',
              }}>
                <ChampIcon championId={p.championId} champion={p.champion} size={26} />
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 11, fontWeight: 600, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {nameKo}
                  </div>
                  <div style={{ fontSize: 10, color: 'var(--color-text-disabled)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {p.riotId.split('#')[0]}
                  </div>
                </div>
                {isAce && <span style={{ fontSize: 9, fontWeight: 700, color: '#FFD700', background: 'rgba(255,215,0,0.15)', padding: '1px 5px', borderRadius: 4 }}>ACE</span>}
                {isMvp && <span style={{ fontSize: 9, fontWeight: 700, color: 'var(--color-info)', background: 'rgba(59,158,255,0.15)', padding: '1px 5px', borderRadius: 4 }}>MVP</span>}
                <span style={{ fontSize: 12, fontWeight: 700, flexShrink: 0, color: 'var(--color-text-primary)' }}>
                  {p.kills}<span style={{ color: 'var(--color-text-disabled)', fontWeight: 400 }}>/</span>
                  <span style={{ color: 'var(--color-loss)' }}>{p.deaths}</span>
                  <span style={{ color: 'var(--color-text-disabled)', fontWeight: 400 }}>/</span>
                  {p.assists}
                </span>
              </div>
            );
          })}

          {/* 구분선 */}
          <div style={{ height: 1, background: 'var(--color-border)', margin: '2px 0' }} />

          {/* 레드팀 */}
          <div style={{ padding: '6px 12px 2px', fontSize: 10, fontWeight: 700, color: 'var(--color-loss)' }}>
            레드팀 {!blueWin ? '✓ 승' : '패'}
          </div>
          {red.map((p, i) => {
            const nameKo = champions.get(p.championId)?.nameKo ?? p.champion;
            const isAce = p.riotId === mvp.aceId;
            const isMvp = !isAce && p.riotId === mvp.redMvpId;
            return (
              <div key={i} style={{
                display: 'flex', alignItems: 'center', gap: 8,
                padding: '5px 12px',
                background: isAce ? 'rgba(255,215,0,0.05)' : isMvp ? 'rgba(232,64,64,0.05)' : 'transparent',
              }}>
                <ChampIcon championId={p.championId} champion={p.champion} size={26} />
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 11, fontWeight: 600, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {nameKo}
                  </div>
                  <div style={{ fontSize: 10, color: 'var(--color-text-disabled)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {p.riotId.split('#')[0]}
                  </div>
                </div>
                {isAce && <span style={{ fontSize: 9, fontWeight: 700, color: '#FFD700', background: 'rgba(255,215,0,0.15)', padding: '1px 5px', borderRadius: 4 }}>ACE</span>}
                {isMvp && <span style={{ fontSize: 9, fontWeight: 700, color: 'var(--color-loss)', background: 'rgba(232,64,64,0.15)', padding: '1px 5px', borderRadius: 4 }}>MVP</span>}
                <span style={{ fontSize: 12, fontWeight: 700, flexShrink: 0, color: 'var(--color-text-primary)' }}>
                  {p.kills}<span style={{ color: 'var(--color-text-disabled)', fontWeight: 400 }}>/</span>
                  <span style={{ color: 'var(--color-loss)' }}>{p.deaths}</span>
                  <span style={{ color: 'var(--color-text-disabled)', fontWeight: 400 }}>/</span>
                  {p.assists}
                </span>
              </div>
            );
          })}
          <div style={{ height: 8 }} />
        </div>
      )}
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
