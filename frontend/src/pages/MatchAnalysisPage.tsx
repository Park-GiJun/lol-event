import { lazy } from 'react';
import { TabPage } from '@/components/ds/TabPage';

const TABS = [
  { key: 'awards',      label: '어워즈',     component: lazy(() => import('./stats-tabs/AwardsTab')) },
  { key: 'multikill',   label: '멀티킬',     component: lazy(() => import('./stats-tabs/MultikillTab')) },
  { key: 'chaos',       label: '혼돈 지수',  component: lazy(() => import('./stats-tabs/ChaosTab')) },
  { key: 'defeat',      label: '떡락 지수',  component: lazy(() => import('./stats-tabs/DefeatTab')) },
  { key: 'comeback',    label: '컴백',       component: lazy(() => import('./stats-tabs/ComebackTab')) },
  { key: 'earlygame',   label: '초반 지배',  component: lazy(() => import('./stats-tabs/EarlyGameTab')) },
  { key: 'gamelength',  label: '게임 길이',  component: lazy(() => import('./stats-tabs/GameLengthTab')) },
  { key: 'timepattern', label: '시간 패턴',  component: lazy(() => import('./stats-tabs/TimePatternTab')) },
  { key: 'sessions',    label: '세션',       component: lazy(() => import('./stats-tabs/SessionsTab')) },
];

export function MatchAnalysisPage() {
  return <TabPage title="경기 분석" subtitle="경기에서 반복되는 패턴" tabs={TABS} />;
}
