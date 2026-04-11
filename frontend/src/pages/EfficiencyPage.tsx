import { lazy, Suspense, useState } from 'react';
import { Button } from '../components/common/Button';
import { LoadingCenter } from '../components/common/Spinner';
import { MODES } from '../lib/lol';
import '../styles/pages/stats.css';

const GoldEffTab = lazy(() => import('./stats-tabs/GoldEffTab'));
const SurvivalTab = lazy(() => import('./stats-tabs/SurvivalTab'));
const JungleTab = lazy(() => import('./stats-tabs/JungleTab'));
const SupportTab = lazy(() => import('./stats-tabs/SupportTab'));

const TABS = [
  { key: 'goldeff',  label: '💰 골드 효율' },
  { key: 'survival', label: '🛡️ 생존력' },
  { key: 'jungle',   label: '🌲 정글' },
  { key: 'support',  label: '💚 서폿 기여' },
];

export function EfficiencyPage() {
  const [mode, setMode] = useState('normal');
  const [tab, setTab]   = useState('goldeff');

  return (
    <div>
      <div className="page-header flex items-center justify-between">
        <div>
          <h1 className="page-title">효율 분석</h1>
          <p className="page-subtitle">골드 효율과 역할별 기여도</p>
        </div>
        <div className="flex gap-sm">
          {MODES.map(m => (
            <Button key={m.value} variant={mode === m.value ? 'primary' : 'secondary'}
              size="sm" onClick={() => setMode(m.value)}>{m.label}</Button>
          ))}
        </div>
      </div>

      <div className="stats-tab-bar" style={{ overflowX: 'auto', display: 'flex', flexWrap: 'nowrap', background: 'var(--glass-bg)', backdropFilter: 'blur(var(--glass-blur))', WebkitBackdropFilter: 'blur(var(--glass-blur))', borderRadius: 'var(--radius-lg)', padding: '4px', marginBottom: 20, border: '1px solid var(--glass-border)', borderBottomColor: 'var(--glass-border)' }}>
        {TABS.map(t => (
          <button key={t.key} className={`stats-tab-btn ${tab === t.key ? 'active' : ''}`}
            onClick={() => setTab(t.key)} style={{ flexShrink: 0 }}>
            {t.label}
          </button>
        ))}
      </div>

      <Suspense fallback={<LoadingCenter />}>
        {tab === 'goldeff'  && <div className="card" style={{ marginTop: 4 }}><GoldEffTab mode={mode} /></div>}
        {tab === 'survival' && <div className="card" style={{ marginTop: 4 }}><SurvivalTab mode={mode} /></div>}
        {tab === 'jungle'   && <div className="card" style={{ marginTop: 4 }}><JungleTab mode={mode} /></div>}
        {tab === 'support'  && <div className="card" style={{ marginTop: 4 }}><SupportTab mode={mode} /></div>}
      </Suspense>
    </div>
  );
}
