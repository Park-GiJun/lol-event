import { lazy, Suspense, useState } from 'react';
import { Button } from '../components/common/Button';
import { LoadingCenter } from '../components/common/Spinner';
import { MODES } from '../lib/lol';
import '../styles/pages/stats.css';

const OverviewTab = lazy(() => import('./stats-tabs/OverviewTab'));
const EloTab = lazy(() => import('./stats-tabs/EloTab'));
const MvpTab = lazy(() => import('./stats-tabs/MvpTab'));
const LaneTab = lazy(() => import('./stats-tabs/LaneTab'));
const SynergyTab = lazy(() => import('./stats-tabs/SynergyTab'));
const DuoTab = lazy(() => import('./stats-tabs/DuoTab'));
const AwardsTab = lazy(() => import('./stats-tabs/AwardsTab'));
const ChaosTab = lazy(() => import('./stats-tabs/ChaosTab'));
const MultikillTab = lazy(() => import('./stats-tabs/MultikillTab'));
const DefeatTab = lazy(() => import('./stats-tabs/DefeatTab'));
const RivalTab = lazy(() => import('./stats-tabs/RivalTab'));
const ChemistryTab = lazy(() => import('./stats-tabs/ChemistryTab'));
const SurvivalTab = lazy(() => import('./stats-tabs/SurvivalTab'));
const JungleTab = lazy(() => import('./stats-tabs/JungleTab'));
const SupportTab = lazy(() => import('./stats-tabs/SupportTab'));
const PositionTab = lazy(() => import('./stats-tabs/PositionTab'));
const CertificateTab = lazy(() => import('./stats-tabs/CertificateTab'));
const DnaTab = lazy(() => import('./stats-tabs/DnaTab'));
const MetaTab = lazy(() => import('./stats-tabs/MetaTab'));
const CompareTab = lazy(() => import('./stats-tabs/CompareTab'));
const SessionsTab = lazy(() => import('./stats-tabs/SessionsTab'));
const TierTab = lazy(() => import('./stats-tabs/TierTab'));
const GameLengthTab = lazy(() => import('./stats-tabs/GameLengthTab'));
const EarlyGameTab = lazy(() => import('./stats-tabs/EarlyGameTab'));
const ComebackTab = lazy(() => import('./stats-tabs/ComebackTab'));
const GoldEffTab = lazy(() => import('./stats-tabs/GoldEffTab'));
const BanAnalysisTab = lazy(() => import('./stats-tabs/BanAnalysisTab'));
const TimePatternTab = lazy(() => import('./stats-tabs/TimePatternTab'));
const KillParticipationTab = lazy(() => import('./stats-tabs/KillParticipationTab'));
const PositionPoolTab = lazy(() => import('./stats-tabs/PositionPoolTab'));

const TABS = [
  { key: 'overview',    label: '📊 개요' },
  { key: 'elo',         label: '🏅 Elo 랭킹' },
  { key: 'mvp',         label: '🏆 MVP 랭킹' },
  { key: 'lane',        label: '🗺️ 라인별' },
  { key: 'synergy',     label: '⚡ 챔피언 시너지' },
  { key: 'duo',         label: '🤝 듀오 시너지' },
  { key: 'awards',      label: '🏆 어워즈' },
  { key: 'chaos',       label: '💥 혼돈 지수' },
  { key: 'multikill',   label: '⚡ 멀티킬' },
  { key: 'defeat',      label: '😵 떡락 지수' },
  { key: 'rival',       label: '⚔️ 라이벌' },
  { key: 'chemistry',   label: '🤝 팀 케미' },
  { key: 'survival',    label: '🛡️ 생존력' },
  { key: 'jungle',      label: '🌲 정글' },
  { key: 'support',     label: '💚 서폿 기여' },
  { key: 'position',    label: '📍 포지션 배지' },
  { key: 'certificate', label: '🎖️ 장인 인증' },
  { key: 'dna',         label: '🧬 플레이스타일' },
  { key: 'meta',        label: '📈 메타 추적' },
  { key: 'compare',    label: '⚔️ 비교' },
  { key: 'sessions',   label: '📅 세션' },
  { key: 'tier',       label: '🏅 티어리스트' },
  { key: 'gamelength', label: '⏱️ 게임 길이' },
  { key: 'earlygame',  label: '🌅 초반 지배' },
  { key: 'comeback',   label: '🔄 컴백' },
  { key: 'goldeff',    label: '💰 골드 효율' },
  { key: 'ban',        label: '🚫 밴 분석' },
  { key: 'timepattern', label: '📅 시간패턴' },
  { key: 'kp',         label: '⚡ KP 랭킹' },
  { key: 'pospool',    label: '📍 포지션 풀' },
];

export function StatsPage() {
  const [mode, setMode] = useState('normal');
  const [tab, setTab]   = useState('overview');

  return (
    <div>
      <div className="page-header flex items-center justify-between">
        <div>
          <h1 className="page-title">통계</h1>
          <p className="page-subtitle">내전 현황 한눈에 보기</p>
        </div>
        <div className="flex gap-sm">
          {MODES.map(m => (
            <Button key={m.value} variant={mode === m.value ? 'primary' : 'secondary'}
              size="sm" onClick={() => setMode(m.value)}>{m.label}</Button>
          ))}
        </div>
      </div>

      {/* 탭 */}
      <div className="stats-tab-bar" style={{ overflowX: 'auto', display: 'flex', flexWrap: 'nowrap' }}>
        {TABS.map(t => (
          <button key={t.key} className={`stats-tab-btn ${tab === t.key ? 'active' : ''}`}
            onClick={() => setTab(t.key)} style={{ flexShrink: 0 }}>
            {t.label}
          </button>
        ))}
      </div>

      {/* 탭 컨텐츠 */}
      <Suspense fallback={<LoadingCenter />}>
        {tab === 'overview'    && <OverviewTab mode={mode} />}
        {tab === 'elo'         && <div className="card" style={{ marginTop: 4 }}><EloTab /></div>}
        {tab === 'mvp'         && <div className="card" style={{ marginTop: 4 }}><MvpTab         mode={mode} /></div>}
        {tab === 'lane'        && <div className="card" style={{ marginTop: 4 }}><LaneTab        mode={mode} /></div>}
        {tab === 'synergy'     && <div className="card" style={{ marginTop: 4 }}><SynergyTab     mode={mode} /></div>}
        {tab === 'duo'         && <div className="card" style={{ marginTop: 4 }}><DuoTab         mode={mode} /></div>}
        {tab === 'awards'      && <div className="card" style={{ marginTop: 4 }}><AwardsTab      mode={mode} /></div>}
        {tab === 'chaos'       && <div className="card" style={{ marginTop: 4 }}><ChaosTab       mode={mode} /></div>}
        {tab === 'multikill'   && <div className="card" style={{ marginTop: 4 }}><MultikillTab   mode={mode} /></div>}
        {tab === 'defeat'      && <div className="card" style={{ marginTop: 4 }}><DefeatTab      mode={mode} /></div>}
        {tab === 'rival'       && <div className="card" style={{ marginTop: 4 }}><RivalTab       mode={mode} /></div>}
        {tab === 'chemistry'   && <div className="card" style={{ marginTop: 4 }}><ChemistryTab   mode={mode} /></div>}
        {tab === 'survival'    && <div className="card" style={{ marginTop: 4 }}><SurvivalTab    mode={mode} /></div>}
        {tab === 'jungle'      && <div className="card" style={{ marginTop: 4 }}><JungleTab      mode={mode} /></div>}
        {tab === 'support'     && <div className="card" style={{ marginTop: 4 }}><SupportTab     mode={mode} /></div>}
        {tab === 'position'    && <div className="card" style={{ marginTop: 4 }}><PositionTab    mode={mode} /></div>}
        {tab === 'certificate' && <div className="card" style={{ marginTop: 4 }}><CertificateTab mode={mode} /></div>}
        {tab === 'dna'         && <div className="card" style={{ marginTop: 4 }}><DnaTab         mode={mode} /></div>}
        {tab === 'meta'        && <div className="card" style={{ marginTop: 4 }}><MetaTab        mode={mode} /></div>}
        {tab === 'compare'    && <div className="card" style={{ marginTop: 4 }}><CompareTab     mode={mode} /></div>}
        {tab === 'sessions'   && <div className="card" style={{ marginTop: 4 }}><SessionsTab    mode={mode} /></div>}
        {tab === 'tier'       && <div className="card" style={{ marginTop: 4 }}><TierTab        mode={mode} /></div>}
        {tab === 'gamelength' && <div className="card" style={{ marginTop: 4 }}><GameLengthTab  mode={mode} /></div>}
        {tab === 'earlygame'  && <div className="card" style={{ marginTop: 4 }}><EarlyGameTab   mode={mode} /></div>}
        {tab === 'comeback'   && <div className="card" style={{ marginTop: 4 }}><ComebackTab    mode={mode} /></div>}
        {tab === 'goldeff'    && <div className="card" style={{ marginTop: 4 }}><GoldEffTab     mode={mode} /></div>}
        {tab === 'ban'         && <div className="card" style={{ marginTop: 4 }}><BanAnalysisTab mode={mode} /></div>}
        {tab === 'timepattern' && <div className="card" style={{ marginTop: 4 }}><TimePatternTab mode={mode} /></div>}
        {tab === 'kp'          && <div className="card" style={{ marginTop: 4 }}><KillParticipationTab mode={mode} /></div>}
        {tab === 'pospool'     && <div className="card" style={{ marginTop: 4 }}><PositionPoolTab mode={mode} /></div>}
      </Suspense>
    </div>
  );
}
