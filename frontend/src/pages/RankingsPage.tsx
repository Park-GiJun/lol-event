import { lazy } from 'react';
import { TabPage } from '@/components/ds/TabPage';

/**
 * 리더보드 — "누가 잘하나".
 *
 * 개요는 홈이 이미 같은 내용을 보여주고 있어 뺐고, 티어리스트는 사람이 아니라 챔피언 이야기라
 * 챔피언 화면으로 옮겼다. 여기 남는 건 전부 사람 순위다.
 */
const TABS = [
  { key: 'elo',  label: '종합',    component: lazy(() => import('./stats-tabs/EloTab')) },
  { key: 'mvp',  label: 'MVP',    component: lazy(() => import('./stats-tabs/MvpTab')) },
  { key: 'kp',   label: '킬 관여', component: lazy(() => import('./stats-tabs/KillParticipationTab')) },
  { key: 'lane', label: '라인별',  component: lazy(() => import('./stats-tabs/LaneTab')) },
];

export function RankingsPage() {
  return <TabPage title="리더보드" subtitle="Elo 와 부문별 순위" tabs={TABS} />;
}
