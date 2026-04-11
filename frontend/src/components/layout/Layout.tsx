import { Outlet, useLocation, NavLink, useNavigate } from 'react-router-dom';
import { useState, useCallback, useEffect } from 'react';
import { Swords, Menu, X, BarChart2, UserRound, List, Users, Search } from 'lucide-react';
import { Sidebar } from './Sidebar';

const BOTTOM_NAV = [
  { to: '/',             icon: BarChart2, label: '통계',    end: true },
  { to: '/player-stats', icon: UserRound, label: '멤버',    end: false },
  { to: '/matches',      icon: List,      label: '경기',    end: false },
  { to: '/members',      icon: Users,     label: '멤버관리', end: false },
];

export function Layout() {
  const [drawerOpen, setDrawerOpen] = useState(false);
  const close = useCallback(() => setDrawerOpen(false), []);
  const location = useLocation();
  const navigate = useNavigate();
  const [search, setSearch] = useState('');

  // eslint-disable-next-line react-hooks/set-state-in-effect
  useEffect(() => { close(); }, [location.pathname, close]);

  return (
    <div className="layout">
      <Sidebar drawerOpen={drawerOpen} onClose={close} />

      {drawerOpen && (
        <div className="sidebar-overlay" onClick={close} />
      )}

      <main className="main-content">
        {/* 모바일 상단 헤더 */}
        <header className="mobile-header">
          <div className="mobile-header-logo">
            <div className="mobile-header-logo-icon">
              <Swords size={16} />
            </div>
            <span className="mobile-header-title">LoL 내전</span>
          </div>

          <div className="mobile-header-search">
            <Search size={13} className="mobile-search-icon" />
            <input
              className="mobile-search-input"
              value={search}
              onChange={e => setSearch(e.target.value)}
              onKeyDown={e => {
                if (e.key === 'Enter' && search.trim()) {
                  navigate(`/player-stats/${encodeURIComponent(search.trim())}`);
                  setSearch('');
                }
              }}
              placeholder="닉네임#태그"
            />
          </div>

          <button
            className={`mobile-hamburger${drawerOpen ? ' active' : ''}`}
            onClick={() => setDrawerOpen(o => !o)}
            aria-label="메뉴"
          >
            {drawerOpen ? <X size={20} /> : <Menu size={20} />}
          </button>
        </header>

        <div className="page-content page-enter">
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
            {({ isActive }) => (
              <>
                <span className="bottom-nav-icon">
                  <Icon size={20} />
                  {isActive && <span className="bottom-nav-dot" />}
                </span>
                <span className="bottom-nav-label">{label}</span>
              </>
            )}
          </NavLink>
        ))}
        <button
          className={`bottom-nav-item${drawerOpen ? ' active' : ''}`}
          onClick={() => setDrawerOpen(o => !o)}
        >
          <span className="bottom-nav-icon">
            <Menu size={20} />
            {drawerOpen && <span className="bottom-nav-dot" />}
          </span>
          <span className="bottom-nav-label">더보기</span>
        </button>
      </nav>
    </div>
  );
}
