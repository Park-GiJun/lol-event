import { useEffect, useState, useCallback } from 'react';
import { ChevronDown, ChevronRight } from 'lucide-react';
import { api } from '../../lib/api/api';
import type { Match, Participant } from '../../lib/types/match';
import { useDragon } from '../../context/DragonContext';
import { LoadingCenter } from '../../components/common/Spinner';

const MODES = [
  { value: 'normal', label: '5v5' },
  { value: 'aram', label: '칼바람' },
  { value: 'all', label: '전체' },
];

const DAY_NAMES = ['일', '월', '화', '수', '목', '금', '토'];

function fmt(secs: number) {
  const m = Math.floor(secs / 60);
  const s = secs % 60;
  return `${m}:${String(s).padStart(2, '0')}`;
}

// 오후 5시~다음날 오후 4시 59분을 하나의 세션으로 묶음
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

function ChampIcon({ championId, champion }: { championId: number; champion: string }) {
  const { champions } = useDragon();
  const data = champions.get(championId);
  if (data?.imageUrl) {
    return (
      <img src={data.imageUrl} alt={champion} className="m-champ-icon"
        onError={e => { (e.currentTarget as HTMLImageElement).style.display = 'none'; }}
      />
    );
  }
  return (
    <div className="m-champ-icon-placeholder">{champion.slice(0, 2)}</div>
  );
}

function MatchCard({ match }: { match: Match }) {
  const { champions } = useDragon();
  const [expanded, setExpanded] = useState(false);
  const blue = match.participants.filter(p => p.team === 'blue');
  const red = match.participants.filter(p => p.team === 'red');
  const blueWin = blue[0]?.win ?? false;
  const timeStr = new Date(match.gameCreation).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', hour12: false });

  return (
    <div className="m-match-card">
      <div className="m-match-header" onClick={() => setExpanded(e => !e)}>
        <span className={`m-win-badge ${blueWin ? 'win' : 'loss'}`}>{blueWin ? '블루 승' : '레드 승'}</span>
        <span style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>{fmt(match.gameDuration)}</span>
        <span style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>{timeStr}</span>
        <span className="m-match-toggle">
          {expanded ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
        </span>
      </div>

      {expanded && (
        <div className="m-participants">
          {/* blue column */}
          <div>
            {blue.map((p, i) => {
              const nameKo = champions.get(p.championId)?.nameKo ?? p.champion;
              return (
                <div key={i} className="m-participant-row">
                  <ChampIcon championId={p.championId} champion={p.champion} />
                  <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{nameKo}</span>
                  <span style={{ marginLeft: 'auto', flexShrink: 0 }}>{p.kills}/{p.deaths}/{p.assists}</span>
                </div>
              );
            })}
          </div>
          {/* red column */}
          <div>
            {red.map((p, i) => {
              const nameKo = champions.get(p.championId)?.nameKo ?? p.champion;
              return (
                <div key={i} className="m-participant-row">
                  <ChampIcon championId={p.championId} champion={p.champion} />
                  <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{nameKo}</span>
                  <span style={{ marginLeft: 'auto', flexShrink: 0 }}>{p.kills}/{p.deaths}/{p.assists}</span>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}

function SessionGroup({ sessionKey, matches, defaultOpen }: { sessionKey: string; matches: Match[]; defaultOpen: boolean }) {
  const [open, setOpen] = useState(defaultOpen);
  const label = formatSessionLabel(sessionKey);
  const wins = matches.filter(m => m.participants.find(p => p.team === 'blue')?.win).length;

  return (
    <div className="m-session-group">
      <button
        onClick={() => setOpen(o => !o)}
        style={{ display: 'flex', alignItems: 'center', gap: 8, width: '100%', background: 'none', border: 'none', padding: '8px 0', cursor: 'pointer', color: 'var(--color-text-primary)' }}
      >
        {open ? <ChevronDown size={15} /> : <ChevronRight size={15} />}
        <span style={{ fontSize: 13, fontWeight: 700 }}>{label}</span>
        <span style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>{matches.length}경기</span>
        <span style={{ marginLeft: 'auto', fontSize: 11, color: 'var(--color-text-secondary)' }}>
          <span style={{ color: 'var(--color-win)' }}>{wins}승</span> {matches.length - wins}패
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

  // Group by session
  const groupMap = new Map<string, Match[]>();
  for (const m of matches) {
    const key = getSessionKey(m.gameCreation);
    if (!groupMap.has(key)) groupMap.set(key, []);
    groupMap.get(key)!.push(m);
  }
  const groups = Array.from(groupMap.entries())
    .map(([key, ms]) => ({ key, matches: ms }))
    .sort((a, b) => b.key.localeCompare(a.key));

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
