import { lazy, Suspense, useState } from 'react';
import { Button } from '../components/common/Button';
import { LoadingCenter } from '../components/common/Spinner';
import { MODES } from '../lib/lol';
import '../styles/pages/stats.css';

const GoldEffTab       = lazy(() => import('./stats-tabs/GoldEffTab'));
const SurvivalTab      = lazy(() => import('./stats-tabs/SurvivalTab'));
const JungleTab        = lazy(() => import('./stats-tabs/JungleTab'));
const SupportTab       = lazy(() => import('./stats-tabs/SupportTab'));
const DamageAnalysisTab = lazy(() => import('./stats-tabs/DamageAnalysisTab'));
const VisionDominanceTab = lazy(() => import('./stats-tabs/VisionDominanceTab'));
const SurrenderTab     = lazy(() => import('./stats-tabs/SurrenderTab'));
const LateGameTab      = lazy(() => import('./stats-tabs/LateGameTab'));

const TABS = [
  { key: 'goldeff',   icon: '💰', label: '골드 효율' },
  { key: 'survival',  icon: '🛡️', label: '생존력' },
  { key: 'jungle',    icon: '🌲', label: '정글' },
  { key: 'support',   icon: '💚', label: '서폿 기여' },
  { key: 'damage',    icon: '🔥', label: '데미지 분석' },
  { key: 'vision',    icon: '👁️', label: '시야 지배' },
  { key: 'surrender', icon: '🏳️', label: '서렌더 분석' },
  { key: 'lategame',  icon: '👑', label: '후반 지배' },
];

export function EfficiencyPage() {
  const [mode, setMode] = useState('normal');
  const [tab, setTab]   = useState('goldeff');

  return (
    <div>
      <div className="hero-banner flex items-center justify-between" style={{ marginBottom: 'var(--spacing-lg)', gap: 'var(--spacing-md)' }}>
        <div>
          <div className="hero-eyebrow">EFFICIENCY</div>
          <h1 className="hero-title">효율 분석</h1>
          <p className="hero-subtitle">골드 효율과 역할별 기여도</p>
        </div>
        <div className="flex gap-sm" style={{ flexShrink: 0 }}>
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
            <span className="icon-chip icon-chip-sm">{t.icon}</span>
            {t.label}
          </button>
        ))}
      </div>

      <Suspense fallback={<LoadingCenter />}>
        {tab === 'goldeff'   && <div className="card"><GoldEffTab mode={mode} /></div>}
        {tab === 'survival'  && <div className="card"><SurvivalTab mode={mode} /></div>}
        {tab === 'jungle'    && <div className="card"><JungleTab mode={mode} /></div>}
        {tab === 'support'   && <div className="card"><SupportTab mode={mode} /></div>}
        {tab === 'damage'    && <div className="card"><DamageAnalysisTab mode={mode} /></div>}
        {tab === 'vision'    && <div className="card"><VisionDominanceTab mode={mode} /></div>}
        {tab === 'surrender' && <div className="card"><SurrenderTab mode={mode} /></div>}
        {tab === 'lategame'  && <div className="card"><LateGameTab mode={mode} /></div>}
      </Suspense>
    </div>
  );
}
