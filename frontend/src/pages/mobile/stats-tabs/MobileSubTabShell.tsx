import { useState } from 'react';
import { MODES } from '@/lib/lol';

interface MobileSubTabShellProps<T extends string> {
  tabs: readonly T[];
  defaultTab: T;
  renderTab: (sub: T, mode: string) => React.ReactNode;
}

export function MobileSubTabShell<T extends string>({
  tabs,
  defaultTab,
  renderTab,
}: MobileSubTabShellProps<T>) {
  const [sub, setSub] = useState<T>(defaultTab);
  const [mode, setMode] = useState('normal');

  return (
    <div>
      <div className="m-sort-chips" style={{ marginBottom: 0 }}>
        {MODES.map(m => (
          <button key={m.value} className={`m-sort-chip${mode === m.value ? ' active' : ''}`} onClick={() => setMode(m.value)}>
            {m.label}
          </button>
        ))}
      </div>
      <div className="m-tab-bar" style={{ overflowX: 'auto', scrollbarWidth: 'none', flexWrap: 'nowrap' }}>
        {tabs.map(t => (
          <button key={t} className={`m-tab${sub === t ? ' active' : ''}`} onClick={() => setSub(t)} style={{ flexShrink: 0, fontSize: 12 }}>
            {t}
          </button>
        ))}
      </div>
      {renderTab(sub, mode)}
    </div>
  );
}
