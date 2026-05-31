import { lazy, Suspense, useState } from 'react';
import { Button } from '../components/common/Button';
import { LoadingCenter } from '../components/common/Spinner';
import { MODES } from '../lib/lol';
import '../styles/pages/stats.css';

const SynergyTab = lazy(() => import('./stats-tabs/SynergyTab'));
const ChemistryTab = lazy(() => import('./stats-tabs/ChemistryTab'));
const BanAnalysisTab = lazy(() => import('./stats-tabs/BanAnalysisTab'));
const CertificateTab = lazy(() => import('./stats-tabs/CertificateTab'));
const MetaTab = lazy(() => import('./stats-tabs/MetaTab'));

const TABS = [
  { key: 'synergy',     label: '⚡ 챔피언 시너지' },
  { key: 'chemistry',   label: '🤝 팀 케미' },
  { key: 'ban',         label: '🚫 밴 분석' },
  { key: 'certificate', label: '🎖️ 장인 인증' },
  { key: 'meta',        label: '📈 메타 추적' },
];

export function ChampionAnalysisPage() {
  const [mode, setMode] = useState('normal');
  const [tab, setTab]   = useState('synergy');

  return (
    <div>
      <div className="hero-banner flex items-center justify-between" style={{ gap: 'var(--spacing-md)', flexWrap: 'wrap', marginBottom: 'var(--spacing-lg)' }}>
        <div>
          <div className="hero-eyebrow">Champion Analysis</div>
          <h1 className="hero-title">챔피언 분석</h1>
          <p className="hero-subtitle">챔피언 시너지와 메타 분석</p>
        </div>
        <div className="flex gap-sm" style={{ position: 'relative', zIndex: 1 }}>
          {MODES.map(m => (
            <Button key={m.value} variant={mode === m.value ? 'primary' : 'secondary'}
              size="sm" onClick={() => setMode(m.value)}>{m.label}</Button>
          ))}
        </div>
      </div>

      <div className="tab-bar" style={{ marginBottom: 'var(--spacing-lg)' }}>
        {TABS.map(t => (
          <button key={t.key} className={`tab-bar-item ${tab === t.key ? 'active' : ''}`}
            onClick={() => setTab(t.key)}>
            {t.label}
          </button>
        ))}
      </div>

      <Suspense fallback={<LoadingCenter />}>
        {tab === 'synergy'     && <div className="card"><SynergyTab mode={mode} /></div>}
        {tab === 'chemistry'   && <div className="card"><ChemistryTab mode={mode} /></div>}
        {tab === 'ban'         && <div className="card"><BanAnalysisTab mode={mode} /></div>}
        {tab === 'certificate' && <div className="card"><CertificateTab mode={mode} /></div>}
        {tab === 'meta'        && <div className="card"><MetaTab mode={mode} /></div>}
      </Suspense>
    </div>
  );
}
