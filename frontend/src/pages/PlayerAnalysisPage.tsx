import { lazy, Suspense, useState } from 'react';
import { Button } from '../components/common/Button';
import { LoadingCenter } from '../components/common/Spinner';
import { MODES } from '../lib/lol';
import '../styles/pages/stats.css';

const LaneTab = lazy(() => import('./stats-tabs/LaneTab'));
const PositionTab = lazy(() => import('./stats-tabs/PositionTab'));
const PositionPoolTab = lazy(() => import('./stats-tabs/PositionPoolTab'));
const DuoTab = lazy(() => import('./stats-tabs/DuoTab'));
const RivalTab = lazy(() => import('./stats-tabs/RivalTab'));
const CompareTab = lazy(() => import('./stats-tabs/CompareTab'));
const DnaTab = lazy(() => import('./stats-tabs/DnaTab'));

const TABS = [
  { key: 'lane',     label: '🗺️ 라인별' },
  { key: 'position', label: '📍 포지션 배지' },
  { key: 'pospool',  label: '📍 포지션 풀' },
  { key: 'duo',      label: '🤝 듀오 시너지' },
  { key: 'rival',    label: '⚔️ 라이벌' },
  { key: 'compare',  label: '⚔️ 비교' },
  { key: 'dna',      label: '🧬 플레이스타일' },
];

export function PlayerAnalysisPage() {
  const [mode, setMode] = useState('normal');
  const [tab, setTab]   = useState('lane');

  return (
    <div>
      <div className="page-header flex items-center justify-between">
        <div>
          <h1 className="page-title">플레이어 분석</h1>
          <p className="page-subtitle">플레이어별 심층 분석</p>
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
        {tab === 'lane'     && <div className="card" style={{ marginTop: 4 }}><LaneTab mode={mode} /></div>}
        {tab === 'position' && <div className="card" style={{ marginTop: 4 }}><PositionTab mode={mode} /></div>}
        {tab === 'pospool'  && <div className="card" style={{ marginTop: 4 }}><PositionPoolTab mode={mode} /></div>}
        {tab === 'duo'      && <div className="card" style={{ marginTop: 4 }}><DuoTab mode={mode} /></div>}
        {tab === 'rival'    && <div className="card" style={{ marginTop: 4 }}><RivalTab mode={mode} /></div>}
        {tab === 'compare'  && <div className="card" style={{ marginTop: 4 }}><CompareTab mode={mode} /></div>}
        {tab === 'dna'      && <div className="card" style={{ marginTop: 4 }}><DnaTab mode={mode} /></div>}
      </Suspense>
    </div>
  );
}
