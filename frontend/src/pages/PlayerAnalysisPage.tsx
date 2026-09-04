import { lazy } from 'react';
import { TabPage } from '@/components/ds/TabPage';

/**
 * 선수 분석 — "이 사람은 어떤 선수인가".
 *
 * 예전에는 이 지표들이 두 페이지(플레이어 분석 · 효율 분석)에 흩어져 있었다.
 * 성격이 같은 것들이라 한 화면으로 모았다. 탭 이름만으로는 무엇을 보는지 안 읽히는 게 많아
 * 각 탭에 한 줄 설명을 붙였다.
 */
const TABS = [
  { key: 'position', label: '포지션 1위', hint: '라인마다 가장 점수가 높은 사람',
    component: lazy(() => import('./stats-tabs/PositionTab')) },
  { key: 'pospool',  label: '포지션 풀',  hint: '누가 어느 자리를 얼마나 소화하는가',
    component: lazy(() => import('./stats-tabs/PositionPoolTab')) },
  { key: 'duo',      label: '듀오',      hint: '같은 팀으로 뛰었을 때의 전적. 표본이 적은 조합은 승률이 흔들리니 판수를 같이 본다',
    component: lazy(() => import('./stats-tabs/DuoTab')) },
  { key: 'rival',    label: '라이벌',    hint: '상대 팀으로 맞붙은 전적',
    component: lazy(() => import('./stats-tabs/RivalTab')) },
  { key: 'gold',     label: '골드 효율',  hint: '같은 골드로 얼마나 딜을 뽑았나',
    component: lazy(() => import('./stats-tabs/GoldEffTab')) },
  { key: 'damage',   label: '데미지',    hint: '물리 · 마법 · 고정 피해 구성',
    component: lazy(() => import('./stats-tabs/DamageAnalysisTab')) },
  { key: 'survive',  label: '생존력',    hint: '얼마나 안 죽고 버티는가',
    component: lazy(() => import('./stats-tabs/SurvivalTab')) },
  { key: 'vision',   label: '시야',      hint: '와드와 시야 점수',
    component: lazy(() => import('./stats-tabs/VisionDominanceTab')) },
  { key: 'jungle',   label: '정글',      hint: '정글러 전용 지표',
    component: lazy(() => import('./stats-tabs/JungleTab')) },
  { key: 'support',  label: '서폿',      hint: '서포터 전용 지표',
    component: lazy(() => import('./stats-tabs/SupportTab')) },
  { key: 'compare',  label: '비교',      hint: '두 사람을 나란히 놓고 본다',
    component: lazy(() => import('./stats-tabs/CompareTab')) },
  { key: 'dna',      label: '플레이스타일', hint: '지표 구성으로 본 성향',
    component: lazy(() => import('./stats-tabs/DnaTab')) },
];

export function PlayerAnalysisPage() {
  return <TabPage title="선수 분석" subtitle="포지션 · 관계 · 효율" tabs={TABS} />;
}
