import { lazy } from 'react';
import { TabPage } from '@/components/ds/TabPage';

const TABS = [
  { key: 'overview', label: '개요',      component: lazy(() => import('./stats-tabs/OverviewTab')) },
  { key: 'elo',      label: 'Elo',       component: lazy(() => import('./stats-tabs/EloTab')) },
  { key: 'mvp',      label: 'MVP',       component: lazy(() => import('./stats-tabs/MvpTab')) },
  { key: 'tier',     label: '티어리스트', component: lazy(() => import('./stats-tabs/TierTab')) },
  { key: 'kp',       label: '킬 관여',    component: lazy(() => import('./stats-tabs/KillParticipationTab')) },
];

export function RankingsPage() {
  return <TabPage title="랭킹" subtitle="순위와 등급을 한눈에" tabs={TABS} />;
}
