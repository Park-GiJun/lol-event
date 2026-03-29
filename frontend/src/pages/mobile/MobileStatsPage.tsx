import { lazy, Suspense, useState } from 'react';
import { LoadingCenter } from '../../components/common/Spinner';
import { MobilePlayerListPage } from './MobilePlayerListPage';
import { MODES } from '../../lib/lol';

const MobileOverviewTab        = lazy(() => import('./stats-tabs/MobileOverviewTab'));
const MobileEloTab             = lazy(() => import('./stats-tabs/MobileEloTab'));
const MobileMvpTab             = lazy(() => import('./stats-tabs/MobileMvpTab'));
const MobileSynergyTab         = lazy(() => import('./stats-tabs/MobileSynergyTab'));
const MobileDuoTab             = lazy(() => import('./stats-tabs/MobileDuoTab'));
const MobileLaneTab            = lazy(() => import('./stats-tabs/MobileLaneTab'));
const MobileAwardsGroup        = lazy(() => import('./stats-tabs/MobileAwardsGroup'));
const MobileTeamAnalysisGroup  = lazy(() => import('./stats-tabs/MobileTeamAnalysisGroup'));
const MobileMasteryGroup       = lazy(() => import('./stats-tabs/MobileMasteryGroup'));
const MobilePersonalAnalysisGroup = lazy(() => import('./stats-tabs/MobilePersonalAnalysisGroup'));

type MainTab = '개요' | '랭킹' | 'Elo' | 'MVP' | '시너지' | '듀오' | '라인' | '어워즈' | '팀분석' | '장인' | '개인분석';
const MAIN_TABS: MainTab[] = ['개요', '랭킹', 'Elo', 'MVP', '시너지', '듀오', '라인', '어워즈', '팀분석', '장인', '개인분석'];


const noModeTabGroups: MainTab[] = ['랭킹', 'Elo', '어워즈', '팀분석', '장인', '개인분석'];

export function MobileStatsPage() {
  const [tab, setTab] = useState<MainTab>('개요');
  const [mode, setMode] = useState('normal');

  return (
    <div>
      {/* Main tab bar */}
      <div className="m-tab-bar" style={{ overflowX: 'auto', scrollbarWidth: 'none', flexWrap: 'nowrap' }}>
        {MAIN_TABS.map(t => (
          <button key={t} className={`m-tab${tab === t ? ' active' : ''}`} onClick={() => setTab(t)}
            style={{ flexShrink: 0 }}>
            {t}
          </button>
        ))}
      </div>

      {/* Mode chips */}
      {!noModeTabGroups.includes(tab) && (
        <div className="m-sort-chips">
          {MODES.map(m => (
            <button key={m.value} className={`m-sort-chip${mode === m.value ? ' active' : ''}`} onClick={() => setMode(m.value)}>
              {m.label}
            </button>
          ))}
        </div>
      )}

      <Suspense fallback={<LoadingCenter />}>
        {tab === '개요'    && <MobileOverviewTab mode={mode} />}
        {tab === '랭킹'    && <MobilePlayerListPage />}
        {tab === 'Elo'     && <MobileEloTab />}
        {tab === 'MVP'     && <MobileMvpTab mode={mode} />}
        {tab === '시너지'  && <MobileSynergyTab mode={mode} />}
        {tab === '듀오'    && <MobileDuoTab mode={mode} />}
        {tab === '라인'    && <MobileLaneTab mode={mode} />}
        {tab === '어워즈'  && <MobileAwardsGroup />}
        {tab === '팀분석'  && <MobileTeamAnalysisGroup />}
        {tab === '장인'    && <MobileMasteryGroup />}
        {tab === '개인분석' && <MobilePersonalAnalysisGroup />}
      </Suspense>
    </div>
  );
}
