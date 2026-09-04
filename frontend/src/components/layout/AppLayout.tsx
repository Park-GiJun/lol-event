import { BarChartIcon, CloseIcon, ListIcon, MedalIcon, MenuIcon, RadioIcon, RefreshIcon, SearchIcon, ShieldIcon, ShuffleIcon, SwordsIcon, TargetIcon, TrendingUpIcon, TrophyIcon, UserIcon, UsersIcon, UsersThreeIcon } from '@/components/icons/LolIcons';
import type { IconComponent } from '@/components/icons/LolIcons';
import { useCallback, useState } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { PlayerSearch } from './PlayerSearch';

/**
 * 데스크탑과 모바일을 한 벌로 처리하는 셸.
 *
 * 예전에는 /m 아래 별도 페이지 트리를 두고 화면 폭에 따라 리다이렉트했다.
 * 같은 화면을 두 번 만들다 보니 한쪽만 고쳐지는 일이 잦았다. 이제 한 벌로 가고
 * 폭에 따라 사이드바가 서랍으로, 하단 탭이 붙는 식으로만 달라진다.
 */

interface NavItem { to: string; icon: IconComponent; label: string }

const NAV_GROUPS: { label?: string; items: NavItem[] }[] = [
  {
    items: [
      { to: '/', icon: BarChartIcon, label: '홈' },
      { to: '/players', icon: UserIcon, label: '플레이어' },
      { to: '/champions', icon: TrophyIcon, label: '챔피언' },
      { to: '/matches', icon: ListIcon, label: '경기' },
    ],
  },
  {
    // 분석 화면은 넷이다. 예전에는 다섯 페이지에 탭이 31개 흩어져 있었는데
    // 어느 탭에 뭐가 있는지 아무도 외우지 못했다.
    label: '분석',
    items: [
      { to: '/rankings', icon: TrendingUpIcon, label: '리더보드' },
      { to: '/player-analysis', icon: UsersThreeIcon, label: '선수 분석' },
      { to: '/champion-analysis', icon: TargetIcon, label: '챔피언 분석' },
      { to: '/hall', icon: MedalIcon, label: '명예의 전당' },
    ],
  },
  {
    label: '관리',
    items: [
      { to: '/team-builder', icon: ShuffleIcon, label: '팀 빌더' },
      { to: '/members', icon: UsersIcon, label: '멤버 관리' },
      { to: '/lcu', icon: RadioIcon, label: 'LCU 수집' },
      { to: '/sync', icon: RefreshIcon, label: '동기화' },
      { to: '/admin', icon: ShieldIcon, label: '어드민' },
    ],
  },
];

/** 모바일 하단 탭. 자주 쓰는 넷과 메뉴. */
const BOTTOM_NAV: NavItem[] = [
  { to: '/', icon: BarChartIcon, label: '홈' },
  { to: '/players', icon: UserIcon, label: '플레이어' },
  { to: '/champions', icon: TrophyIcon, label: '챔피언' },
  { to: '/matches', icon: ListIcon, label: '경기' },
];

const TITLES: [string, string][] = [
  ['/hall', '명예의 전당'],
  ['/player-analysis', '선수 분석'],
  ['/champion-analysis', '챔피언 분석'],
  ['/players', '플레이어'],
  ['/champions', '챔피언'],
  ['/matches', '경기'],
  ['/rankings', '리더보드'],
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
  const location = useLocation();
  const navigate = useNavigate();
  const close = useCallback(() => setDrawerOpen(false), []);


  return (
    <div className="t-shell">
      {drawerOpen && <div className="t-scrim" onClick={close} />}

      <aside className={`t-shell-side${drawerOpen ? ' open' : ''}`}>
        <div className="t-brand">
          <span className="t-brand-mark"><SwordsIcon size={15} /></span>
          LoL 내전
          <button
            className="t-iconbtn"
            onClick={close}
            aria-label="메뉴 닫기"
            style={{ marginLeft: 'auto' }}
          >
            <CloseIcon size={18} />
          </button>
        </div>

        <PlayerSearch onNavigate={close} />

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
            <MenuIcon size={20} />
          </button>
          <span className="t-topbar-title">{titleFor(location.pathname)}</span>
          <button
            className="t-iconbtn"
            onClick={() => navigate('/players')}
            aria-label="플레이어 검색"
          >
            <SearchIcon size={19} />
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
