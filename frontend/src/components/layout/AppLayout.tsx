import { useCallback, useState } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import {
  BarChart2, Gem, List, Menu, Radio, RefreshCw, Search, Shield, Shuffle,
  Swords, Target, TrendingUp, Trophy, UserRound, Users, Users2, X,
} from 'lucide-react';
import type { LucideIcon } from 'lucide-react';

/**
 * 데스크탑과 모바일을 한 벌로 처리하는 셸.
 *
 * 예전에는 /m 아래 별도 페이지 트리를 두고 화면 폭에 따라 리다이렉트했다.
 * 같은 화면을 두 번 만들다 보니 한쪽만 고쳐지는 일이 잦았다. 이제 한 벌로 가고
 * 폭에 따라 사이드바가 서랍으로, 하단 탭이 붙는 식으로만 달라진다.
 */

interface NavItem { to: string; icon: LucideIcon; label: string }

const NAV_GROUPS: { label?: string; items: NavItem[] }[] = [
  {
    items: [
      { to: '/', icon: BarChart2, label: '홈' },
      { to: '/players', icon: UserRound, label: '플레이어' },
      { to: '/champions', icon: Trophy, label: '챔피언' },
      { to: '/matches', icon: List, label: '경기' },
    ],
  },
  {
    label: '분석',
    items: [
      { to: '/rankings', icon: TrendingUp, label: '랭킹' },
      { to: '/player-analysis', icon: Users2, label: '플레이어 분석' },
      { to: '/champion-analysis', icon: Target, label: '챔피언 분석' },
      { to: '/match-analysis', icon: Swords, label: '경기 분석' },
      { to: '/efficiency', icon: Gem, label: '효율 분석' },
    ],
  },
  {
    label: '관리',
    items: [
      { to: '/team-builder', icon: Shuffle, label: '팀 빌더' },
      { to: '/members', icon: Users, label: '멤버 관리' },
      { to: '/lcu', icon: Radio, label: 'LCU 수집' },
      { to: '/sync', icon: RefreshCw, label: '동기화' },
      { to: '/admin', icon: Shield, label: '어드민' },
    ],
  },
];

/** 모바일 하단 탭. 자주 쓰는 넷과 메뉴. */
const BOTTOM_NAV: NavItem[] = [
  { to: '/', icon: BarChart2, label: '홈' },
  { to: '/players', icon: UserRound, label: '플레이어' },
  { to: '/champions', icon: Trophy, label: '챔피언' },
  { to: '/matches', icon: List, label: '경기' },
];

const TITLES: [string, string][] = [
  ['/players', '플레이어'],
  ['/champions', '챔피언'],
  ['/matches', '경기'],
  ['/rankings', '랭킹'],
  ['/members', '멤버 관리'],
  ['/team-builder', '팀 빌더'],
  ['/lcu', 'LCU 수집'],
  ['/sync', '동기화'],
  ['/admin', '어드민'],
];

function titleFor(pathname: string): string {
  if (pathname === '/') return 'LoL 내전';
  return TITLES.find(([prefix]) => pathname.startsWith(prefix))?.[1] ?? 'LoL 내전';
}

export function AppLayout() {
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [search, setSearch] = useState('');
  const location = useLocation();
  const navigate = useNavigate();
  const close = useCallback(() => setDrawerOpen(false), []);

  const submitSearch = (e: React.FormEvent) => {
    e.preventDefault();
    const q = search.trim();
    if (!q) return;
    navigate(`/players/${encodeURIComponent(q)}`);
    setSearch('');
  };

  return (
    <div className="t-shell">
      {drawerOpen && <div className="t-scrim" onClick={close} />}

      <aside className={`t-shell-side${drawerOpen ? ' open' : ''}`}>
        <div className="t-brand">
          <span className="t-brand-mark"><Swords size={15} /></span>
          LoL 내전
          <button
            className="t-iconbtn"
            onClick={close}
            aria-label="메뉴 닫기"
            style={{ marginLeft: 'auto' }}
          >
            <X size={18} />
          </button>
        </div>

        <form className="t-search" onSubmit={submitSearch} style={{ marginBottom: 12 }}>
          <Search size={15} />
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="닉네임#태그"
            aria-label="플레이어 검색"
          />
        </form>

        {/* 링크를 누르면 서랍을 닫는다. 이동했는데 서랍이 덮여 있으면 바뀐 걸 못 본다.
            경로 변화를 이펙트로 감시하는 대신 클릭 시점에 처리한다. */}
        <nav onClick={close}>
          {NAV_GROUPS.map((group, i) => (
            <div key={i}>
              {group.label && <div className="t-navgroup-label">{group.label}</div>}
              {group.items.map(({ to, icon: Icon, label }) => (
                <NavLink key={to} to={to} end={to === '/'} className="t-nav">
                  <Icon size={17} />
                  {label}
                </NavLink>
              ))}
            </div>
          ))}
        </nav>
      </aside>

      <div className="t-shell-main">
        <header className="t-topbar">
          <button className="t-iconbtn" onClick={() => setDrawerOpen(true)} aria-label="메뉴 열기">
            <Menu size={20} />
          </button>
          <span className="t-topbar-title">{titleFor(location.pathname)}</span>
          <button
            className="t-iconbtn"
            onClick={() => navigate('/players')}
            aria-label="플레이어 검색"
          >
            <Search size={19} />
          </button>
        </header>

        <main className="page-enter">
          <Outlet />
        </main>
      </div>

      <nav className="t-bottomnav">
        {BOTTOM_NAV.map(({ to, icon: Icon, label }) => (
          <NavLink key={to} to={to} end={to === '/'}>
            <Icon size={20} />
            {label}
          </NavLink>
        ))}
      </nav>
    </div>
  );
}
