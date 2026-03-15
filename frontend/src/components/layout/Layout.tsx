import { Outlet, useLocation, NavLink } from 'react-router-dom';
import { useState, useCallback, useEffect } from 'react';
import { Swords, Menu, X, BarChart2, UserRound, List, Users } from 'lucide-react';
import { Sidebar } from './Sidebar';

const BOTTOM_NAV = [
  { to: '/',             icon: BarChart2, label: '통계',  end: true },
  { to: '/player-stats', icon: UserRound, label: '멤버',  end: false },
  { to: '/matches',      icon: List,      label: '경기',  end: false },
  { to: '/members',      icon: Users,     label: '멤버관리', end: false },
];

export function Layout() {
  const [drawerOpen, setDrawerOpen] = useState(false);
  const close = useCallback(() => setDrawerOpen(false), []);
  const location = useLocation();

  // 페이지 이동 시 드로어 닫기
  useEffect(() => { close(); }, [location.pathname, close]);

  return (
    <div className="layout">
      <Sidebar drawerOpen={drawerOpen} onClose={close} />

      {/* 모바일 드로어 오버레이 */}
      {drawerOpen && (
        <div className="sidebar-overlay" onClick={close} />
      )}

      <main className="main-content">
        {/* 모바일 상단 헤더 */}
        <header className="mobile-header">
          <div className="mobile-header-logo">
            <Swords size={20} color="var(--color-primary)" />
            <span className="mobile-header-title">LoL 내전</span>
          </div>
          <button
            className="mobile-hamburger"
            onClick={() => setDrawerOpen(o => !o)}
            aria-label="메뉴"
          >
            {drawerOpen ? <X size={22} /> : <Menu size={22} />}
          </button>
        </header>

        <div className="page-content">
          <Outlet />
        </div>
      </main>

      {/* 모바일 하단 네비게이션 */}
      <nav className="bottom-nav">
        {BOTTOM_NAV.map(({ to, icon: Icon, label, end }) => (
          <NavLink
            key={to}
            to={to}
            end={end}
            className={({ isActive }) => `bottom-nav-item${isActive ? ' active' : ''}`}
          >
            <Icon size={20} />
            <span className="bottom-nav-label">{label}</span>
          </NavLink>
        ))}
        <button
          className={`bottom-nav-item${drawerOpen ? ' active' : ''}`}
          onClick={() => setDrawerOpen(o => !o)}
        >
          <Menu size={20} />
          <span className="bottom-nav-label">더보기</span>
        </button>
      </nav>
    </div>
  );
}
