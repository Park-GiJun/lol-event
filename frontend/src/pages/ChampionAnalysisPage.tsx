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
      <div className="page-header flex items-center justify-between">
        <div>
          <h1 className="page-title">챔피언 분석</h1>
          <p className="page-subtitle">챔피언 시너지와 메타 분석</p>
        </div>
        <div className="flex gap-sm">
          {MODES.map(m => (
            <Button key={m.value} variant={mode === m.value ? 'primary' : 'secondary'}
              size="sm" onClick={() => setMode(m.value)}>{m.label}</Button>
          ))}
        </div>
      </div>

      <div className="stats-tab-bar" style={{ overflowX: 'auto', display: 'flex', flexWrap: 'nowrap', borderRadius: 'var(--radius-lg)', padding: '4px', marginBottom: 20, border: '1px solid var(--color-border)', borderBottomColor: 'var(--color-border)' }}>
        {TABS.map(t => (
          <button key={t.key} className={`stats-tab-btn ${tab === t.key ? 'active' : ''}`}
            onClick={() => setTab(t.key)} style={{ flexShrink: 0 }}>
            {t.label}
          </button>
        ))}
      </div>

      <Suspense fallback={<LoadingCenter />}>
        {tab === 'synergy'     && <div className="card" style={{ marginTop: 4 }}><SynergyTab mode={mode} /></div>}
        {tab === 'chemistry'   && <div className="card" style={{ marginTop: 4 }}><ChemistryTab mode={mode} /></div>}
        {tab === 'ban'         && <div className="card" style={{ marginTop: 4 }}><BanAnalysisTab mode={mode} /></div>}
        {tab === 'certificate' && <div className="card" style={{ marginTop: 4 }}><CertificateTab mode={mode} /></div>}
        {tab === 'meta'        && <div className="card" style={{ marginTop: 4 }}><MetaTab mode={mode} /></div>}
      </Suspense>
    </div>
  );
}
