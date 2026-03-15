import { NavLink } from 'react-router-dom';
import { Swords, Users, BarChart2, List, Radio, RefreshCw } from 'lucide-react';

const NAV_ITEMS = [
  { to: '/',        icon: BarChart2, label: '통계' },
  { to: '/members', icon: Users,     label: '멤버 관리' },
  { to: '/matches', icon: List,      label: '경기 목록' },
  { to: '/lcu',     icon: Radio,     label: 'LCU 수집' },
  { to: '/sync',    icon: RefreshCw, label: '동기화' },
];

export function Sidebar() {
  return (
    <aside className="sidebar">
      <div className="sidebar-logo">
        <Swords size={22} color="var(--color-primary)" />
        <span className="sidebar-logo-text">LoL 내전</span>
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
