import { useNavigate } from 'react-router-dom';
import { Users, Shield, Database, Monitor, UserRound } from 'lucide-react';

const ITEMS = [
  { to: '/m/players', icon: UserRound, label: '플레이어',   desc: '개인 통계 확인' },
  { to: '/m/members', icon: Users,     label: '멤버 관리',  desc: '멤버 등록·삭제' },
  { to: '/m/admin',   icon: Shield,    label: '관리자',     desc: '배치·Elo 관리' },
  { to: '/m/sync',    icon: Database,  label: 'DataDragon', desc: '챔피언·아이템 동기화' },
  { to: '/m/lcu',     icon: Monitor,   label: 'LCU 수집',   desc: '수집기 다운로드' },
];

export function MobileMorePage() {
  const navigate = useNavigate();

  return (
    <div>
      <p className="m-section-title">기능</p>
      <div className="m-more-grid">
        {ITEMS.map(({ to, icon: Icon, label, desc }) => (
          <button
            key={to}
            className="m-more-item"
            onClick={() => navigate(to)}
            style={{ background: 'var(--bg-surface)', border: '1px solid var(--color-border)', borderRadius: 12, padding: '20px 12px', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 10, cursor: 'pointer', transition: 'background 0.15s', width: '100%' }}
          >
            <div className="m-more-item-icon">
              <Icon size={22} />
            </div>
            <div style={{ textAlign: 'center' }}>
              <div className="m-more-item-label">{label}</div>
              <div style={{ fontSize: 11, color: 'var(--color-text-secondary)', marginTop: 2 }}>{desc}</div>
            </div>
          </button>
        ))}
      </div>
    </div>
  );
}
