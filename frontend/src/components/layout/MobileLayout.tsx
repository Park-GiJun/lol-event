import { Outlet, NavLink, useNavigate, useLocation } from 'react-router-dom';
import { BarChart2, List, Trophy, Swords, MoreHorizontal, ChevronLeft } from 'lucide-react';
import '../../styles/layouts/mobile-layout.css';

const TABS = [
  { to: '/m',           icon: Trophy,         label: '홈',      end: true },
  { to: '/m/stats',     icon: BarChart2,      label: '통계',    end: false },
  { to: '/m/matches',   icon: List,           label: '경기',    end: false },
  { to: '/m/champions', icon: Swords,         label: '챔피언',  end: false },
  { to: '/m/more',      icon: MoreHorizontal, label: '더보기',  end: false },
];

const TITLES: Record<string, string> = {
  '/m': 'Elo 리더보드',
  '/m/stats': '통계',
  '/m/matches': '경기 기록',
  '/m/players': '플레이어',
  '/m/champions': '챔피언',
  '/m/more': '더보기',
  '/m/members': '멤버 관리',
  '/m/admin': '관리자',
  '/m/sync': 'DataDragon',
  '/m/lcu': 'LCU 수집',
};

function getTitle(pathname: string): string {
  if (pathname.startsWith('/m/player/')) return '플레이어 통계';
  if (pathname.startsWith('/m/champion/')) return '챔피언 통계';
  if (pathname.startsWith('/m/match/')) return '경기 상세';
  return TITLES[pathname] ?? 'LoL 내전';
}

const SHOW_BACK = ['/m/player/', '/m/champion/', '/m/match/'];

export function MobileLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const showBack = SHOW_BACK.some(p => location.pathname.startsWith(p));

  return (
    <div className="m-layout">
      <header className="m-header">
        {showBack ? (
          <button className="m-header-back" onClick={() => navigate(-1)}>
            <ChevronLeft size={22} />
          </button>
        ) : <div style={{ width: 44 }} />}
        <span className="m-header-title">{getTitle(location.pathname)}</span>
        <div style={{ width: 44 }} />
      </header>

      <main className="m-content">
        <Outlet />
      </main>

      <nav className="m-bottom-nav">
        {TABS.map(({ to, icon: Icon, label, end }) => (
          <NavLink key={to} to={to} end={end}
            className={({ isActive }) => `m-bottom-nav-item${isActive ? ' active' : ''}`}>
            <Icon size={22} />
            <span>{label}</span>
          </NavLink>
        ))}
      </nav>
    </div>
  );
}
