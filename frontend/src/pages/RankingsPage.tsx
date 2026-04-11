import { lazy, Suspense, useState } from 'react';
import { Button } from '../components/common/Button';
import { LoadingCenter } from '../components/common/Spinner';
import { MODES } from '../lib/lol';
import '../styles/pages/stats.css';

const OverviewTab = lazy(() => import('./stats-tabs/OverviewTab'));
const EloTab = lazy(() => import('./stats-tabs/EloTab'));
const MvpTab = lazy(() => import('./stats-tabs/MvpTab'));
const TierTab = lazy(() => import('./stats-tabs/TierTab'));
const KillParticipationTab = lazy(() => import('./stats-tabs/KillParticipationTab'));

const TABS = [
  { key: 'overview', label: '📊 개요' },
  { key: 'elo',      label: '🏅 Elo 랭킹' },
  { key: 'mvp',      label: '🏆 MVP 랭킹' },
  { key: 'tier',     label: '🏅 티어리스트' },
  { key: 'kp',       label: '⚡ KP 랭킹' },
];

export function RankingsPage() {
  const [mode, setMode] = useState('normal');
  const [tab, setTab]   = useState('overview');

  return (
    <div>
      <div className="page-header flex items-center justify-between">
        <div>
          <h1 className="page-title">랭킹</h1>
          <p className="page-subtitle">순위와 등급 한눈에 보기</p>
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
        {tab === 'overview'  && <OverviewTab mode={mode} />}
        {tab === 'elo'       && <div className="card" style={{ marginTop: 4 }}><EloTab /></div>}
        {tab === 'mvp'       && <div className="card" style={{ marginTop: 4 }}><MvpTab mode={mode} /></div>}
        {tab === 'tier'      && <div className="card" style={{ marginTop: 4 }}><TierTab mode={mode} /></div>}
        {tab === 'kp'        && <div className="card" style={{ marginTop: 4 }}><KillParticipationTab mode={mode} /></div>}
      </Suspense>
    </div>
  );
}
