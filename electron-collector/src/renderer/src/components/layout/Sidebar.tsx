import { NavLink } from 'react-router-dom';
import { Layers, Play, Shield, Search, Swords } from 'lucide-react';

const NAV_ITEMS = [
  { to: '/',            icon: Layers,  label: '매치 수집',   end: true },
  { to: '/live-game',   icon: Play,    label: '현재 게임',   end: false },
  { to: '/champ-select',icon: Shield,  label: '챔피언 선택', end: false },
  { to: '/custom',      icon: Swords,  label: '내전 분석',   end: false },
  { to: '/summoner',    icon: Search,  label: '소환사 검색', end: false },
];

export function Sidebar() {
  return (
    <aside className="sidebar">
      <nav className="sidebar-nav">
        {NAV_ITEMS.map(({ to, icon: Icon, label, end }) => (
          <NavLink key={to} to={to} end={end}
            className={({ isActive }) => `sidebar-item ${isActive ? 'active' : ''}`}>
            <Icon size={16} />
            {label}
          </NavLink>
        ))}
      </nav>
    </aside>
  );
}
