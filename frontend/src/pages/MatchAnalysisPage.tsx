import { lazy, Suspense, useState } from 'react';
import { Button } from '../components/common/Button';
import { LoadingCenter } from '../components/common/Spinner';
import { MODES } from '../lib/lol';
import '../styles/pages/stats.css';

const AwardsTab = lazy(() => import('./stats-tabs/AwardsTab'));
const MultikillTab = lazy(() => import('./stats-tabs/MultikillTab'));
const ChaosTab = lazy(() => import('./stats-tabs/ChaosTab'));
const DefeatTab = lazy(() => import('./stats-tabs/DefeatTab'));
const ComebackTab = lazy(() => import('./stats-tabs/ComebackTab'));
const EarlyGameTab = lazy(() => import('./stats-tabs/EarlyGameTab'));
const GameLengthTab = lazy(() => import('./stats-tabs/GameLengthTab'));
const TimePatternTab = lazy(() => import('./stats-tabs/TimePatternTab'));
const SessionsTab = lazy(() => import('./stats-tabs/SessionsTab'));

const TABS = [
  { key: 'awards',      label: '🏆 어워즈' },
  { key: 'multikill',   label: '⚡ 멀티킬' },
  { key: 'chaos',       label: '💥 혼돈 지수' },
  { key: 'defeat',      label: '😵 떡락 지수' },
  { key: 'comeback',    label: '🔄 컴백' },
  { key: 'earlygame',   label: '🌅 초반 지배' },
  { key: 'gamelength',  label: '⏱️ 게임 길이' },
  { key: 'timepattern', label: '📅 시간패턴' },
  { key: 'sessions',    label: '📅 세션' },
];

export function MatchAnalysisPage() {
  const [mode, setMode] = useState('normal');
  const [tab, setTab]   = useState('awards');

  return (
    <div>
      <div className="page-header flex items-center justify-between">
        <div>
          <h1 className="page-title">경기 분석</h1>
          <p className="page-subtitle">경기 트렌드와 패턴 분석</p>
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
        {tab === 'awards'      && <div className="card" style={{ marginTop: 4 }}><AwardsTab mode={mode} /></div>}
        {tab === 'multikill'   && <div className="card" style={{ marginTop: 4 }}><MultikillTab mode={mode} /></div>}
        {tab === 'chaos'       && <div className="card" style={{ marginTop: 4 }}><ChaosTab mode={mode} /></div>}
        {tab === 'defeat'      && <div className="card" style={{ marginTop: 4 }}><DefeatTab mode={mode} /></div>}
        {tab === 'comeback'    && <div className="card" style={{ marginTop: 4 }}><ComebackTab mode={mode} /></div>}
        {tab === 'earlygame'   && <div className="card" style={{ marginTop: 4 }}><EarlyGameTab mode={mode} /></div>}
        {tab === 'gamelength'  && <div className="card" style={{ marginTop: 4 }}><GameLengthTab mode={mode} /></div>}
        {tab === 'timepattern' && <div className="card" style={{ marginTop: 4 }}><TimePatternTab mode={mode} /></div>}
        {tab === 'sessions'    && <div className="card" style={{ marginTop: 4 }}><SessionsTab mode={mode} /></div>}
      </Suspense>
    </div>
  );
}
