import { lazy } from 'react';
import { TabPage } from '@/components/ds/TabPage';

const TABS = [
  { key: 'lane',     label: '라인별',      component: lazy(() => import('./stats-tabs/LaneTab')) },
  { key: 'position', label: '포지션 배지',  component: lazy(() => import('./stats-tabs/PositionTab')) },
  { key: 'pospool',  label: '포지션 풀',    component: lazy(() => import('./stats-tabs/PositionPoolTab')) },
  { key: 'duo',      label: '듀오 시너지',  component: lazy(() => import('./stats-tabs/DuoTab')) },
  { key: 'rival',    label: '라이벌',      component: lazy(() => import('./stats-tabs/RivalTab')) },
  { key: 'compare',  label: '비교',        component: lazy(() => import('./stats-tabs/CompareTab')) },
  { key: 'dna',      label: '플레이스타일', component: lazy(() => import('./stats-tabs/DnaTab')) },
];

export function PlayerAnalysisPage() {
  return <TabPage title="플레이어 분석" subtitle="사람별로 깊게 들여다보기" tabs={TABS} />;
}
