import { NavLink } from 'react-router-dom';
import { Swords, Users, BarChart2, List, Radio, RefreshCw, Shield, UserRound, X, Trophy, Shuffle, FileBarChart } from 'lucide-react';

const NAV_ITEMS = [
  { to: '/',              icon: BarChart2,     label: '전체 통계' },
  { to: '/player-stats',  icon: UserRound,     label: '멤버 통계' },
  { to: '/reports',       icon: FileBarChart,  label: '리포트' },
  { to: '/champions',     icon: Trophy,        label: '챔피언 목록' },
  { to: '/team-builder',  icon: Shuffle,       label: '팀 빌더' },
  { to: '/members',       icon: Users,         label: '멤버 관리' },
  { to: '/matches',       icon: List,          label: '경기 목록' },
  { to: '/lcu',           icon: Radio,         label: 'LCU 수집' },
  { to: '/sync',          icon: RefreshCw,     label: '동기화' },
  { to: '/admin',         icon: Shield,        label: '어드민' },
];

interface SidebarProps {
  drawerOpen?: boolean;
  onClose?: () => void;
}

export function Sidebar({ drawerOpen = false, onClose }: SidebarProps) {
  return (
    <aside className={`sidebar${drawerOpen ? ' sidebar--open' : ''}`}>
      <div className="sidebar-logo">
        <Swords size={22} color="var(--color-primary)" />
        <span className="sidebar-logo-text">LoL 내전</span>
        {onClose && (
          <button className="sidebar-close-btn" onClick={onClose} aria-label="닫기">
            <X size={18} />
          </button>
        )}
      </div>
      <nav className="sidebar-nav">
        {NAV_ITEMS.map(({ to, icon: Icon, label }) => (
          <NavLink key={to} to={to} end={to === '/'}
            className={({ isActive }) => `sidebar-item ${isActive ? 'active' : ''}`}>
            <Icon size={18} />
            {label}
          </NavLink>
        ))}
      </nav>
    </aside>
  );
}
