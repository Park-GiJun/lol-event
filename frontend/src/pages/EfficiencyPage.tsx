import { lazy } from 'react';
import { TabPage } from '@/components/ds/TabPage';

const TABS = [
  { key: 'goldeff',   label: '골드 효율',   component: lazy(() => import('./stats-tabs/GoldEffTab')) },
  { key: 'survival',  label: '생존력',      component: lazy(() => import('./stats-tabs/SurvivalTab')) },
  { key: 'jungle',    label: '정글',        component: lazy(() => import('./stats-tabs/JungleTab')) },
  { key: 'support',   label: '서폿 기여',   component: lazy(() => import('./stats-tabs/SupportTab')) },
  { key: 'damage',    label: '데미지 분석', component: lazy(() => import('./stats-tabs/DamageAnalysisTab')) },
  { key: 'vision',    label: '시야 지배',   component: lazy(() => import('./stats-tabs/VisionDominanceTab')) },
  { key: 'surrender', label: '서렌더 분석', component: lazy(() => import('./stats-tabs/SurrenderTab')) },
  { key: 'lategame',  label: '후반 지배',   component: lazy(() => import('./stats-tabs/LateGameTab')) },
];

export function EfficiencyPage() {
  return <TabPage title="효율 분석" subtitle="골드와 역할별 기여도" tabs={TABS} />;
}
