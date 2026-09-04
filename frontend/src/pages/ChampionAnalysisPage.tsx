import { lazy } from 'react';
import { TabPage } from '@/components/ds/TabPage';

const TABS = [
  { key: 'ban',         label: '밴 분석',      component: lazy(() => import('./stats-tabs/BanAnalysisTab')) },
  { key: 'certificate', label: '장인 인증',    component: lazy(() => import('./stats-tabs/CertificateTab')) },
];

export function ChampionAnalysisPage() {
  return <TabPage title="챔피언 분석" subtitle="조합과 메타의 흐름" tabs={TABS} />;
}
