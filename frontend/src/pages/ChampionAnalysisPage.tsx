import { lazy } from 'react';
import { TabPage } from '@/components/ds/TabPage';

/**
 * 챔피언 — 챔피언 자체에 대한 이야기.
 *
 * 챔피언 시너지와 팀 케미는 걷어냈다. 154경기에서 챔피언 2인 조합은 2,142가지가 나오고
 * 중앙 표본이 1회다. 3인 조합은 최대가 4회라 어떤 컷을 걸어도 남는 게 없었다.
 */
const TABS = [
  { key: 'tier',        label: '티어리스트', component: lazy(() => import('./stats-tabs/TierTab')) },
  { key: 'certificate', label: '장인',      component: lazy(() => import('./stats-tabs/CertificateTab')) },
  { key: 'ban',         label: '밴 분석',    component: lazy(() => import('./stats-tabs/BanAnalysisTab')) },
];

export function ChampionAnalysisPage() {
  return <TabPage title="챔피언 분석" subtitle="티어 · 장인 · 밴" tabs={TABS} />;
}
