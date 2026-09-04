import { SearchIcon } from '@/components/icons/LolIcons';
import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { usePlayers } from '@/hooks/usePlayers';
import { parseRiotId } from '@/lib/lol';

const MAX_SUGGESTIONS = 8;

/**
 * 사이드바 플레이어 검색.
 *
 * 참가자가 70명뿐이라 전체 목록을 한 번 받아 두고 클라이언트에서 거른다. 서버를 왕복할 이유가 없다.
 *
 * 태그(#KR1)는 입력하지 않아도 된다. 이 규모에서 닉네임이 겹칠 일이 거의 없는데
 * 태그까지 정확히 쳐야 하면 사실상 검색이 아니라 받아쓰기가 된다. 태그를 쳐도 걸리게 두되,
 * 없어도 찾아지는 쪽이 기본이다. 목록은 경기 수가 많은 사람부터 보여준다 — 대개 그쪽을 찾는다.
 */
export function PlayerSearch({ onNavigate }: { onNavigate?: () => void }) {
  const [query, setQuery] = useState('');
  const [open, setOpen] = useState(false);
  const [cursor, setCursor] = useState(0);
  const boxRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();
  const { data } = usePlayers('all');

  const candidates = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return [];
    const rows = data?.stats ?? [];
    return rows
      .filter((p) => p.riotId.toLowerCase().includes(q))
      .sort((a, b) => b.games - a.games)
      .slice(0, MAX_SUGGESTIONS);
  }, [query, data]);

  // 바깥을 누르면 닫는다. 목록이 열린 채로 남아 다른 걸 가리는 게 제일 거슬린다.
  useEffect(() => {
    if (!open) return;
    const onDown = (e: MouseEvent) => {
      if (!boxRef.current?.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', onDown);
    return () => document.removeEventListener('mousedown', onDown);
  }, [open]);

  const go = (riotId: string) => {
    navigate(`/players/${encodeURIComponent(riotId)}`);
    setQuery('');
    setOpen(false);
    onNavigate?.();
  };

  const onKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Escape') { setOpen(false); return; }
    if (!candidates.length) return;
    if (e.key === 'ArrowDown') { e.preventDefault(); setCursor((c) => (c + 1) % candidates.length); }
    else if (e.key === 'ArrowUp') { e.preventDefault(); setCursor((c) => (c - 1 + candidates.length) % candidates.length); }
    else if (e.key === 'Enter') { e.preventDefault(); go(candidates[cursor].riotId); }
  };

  const showList = open && query.trim().length > 0;

  return (
    <div className="t-search" ref={boxRef} style={{ marginBottom: 12 }}>
      <SearchIcon size={15} />
      <input
        value={query}
        onChange={(e) => { setQuery(e.target.value); setOpen(true); setCursor(0); }}
        onFocus={() => setOpen(true)}
        onKeyDown={onKeyDown}
        placeholder="닉네임으로 검색"
        aria-label="플레이어 검색"
        autoComplete="off"
        role="combobox"
        aria-expanded={showList}
        aria-controls="player-search-list"
      />

      {showList && (
        <ul className="t-search-pop" id="player-search-list" role="listbox">
          {candidates.length === 0 ? (
            <li className="t-search-empty">찾는 사람이 없습니다</li>
          ) : (
            candidates.map((p, i) => {
              const { name, tag } = parseRiotId(p.riotId);
              return (
                <li key={p.riotId} role="option" aria-selected={i === cursor}>
                  <button
                    type="button"
                    className={`t-search-item${i === cursor ? ' active' : ''}`}
                    onMouseEnter={() => setCursor(i)}
                    onClick={() => go(p.riotId)}
                  >
                    <span className="t-search-name">{name}</span>
                    <span className="t-search-tag">#{tag}</span>
                    <span className="t-search-games">{p.games}경기</span>
                  </button>
                </li>
              );
            })
          )}
        </ul>
      )}
    </div>
  );
}
