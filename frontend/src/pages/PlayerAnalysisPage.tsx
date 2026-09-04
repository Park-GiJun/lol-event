import { lazy } from 'react';
import { TabPage } from '@/components/ds/TabPage';
import { SectionStack } from '@/components/ds/SectionStack';
import type { Section } from '@/components/ds/SectionStack';

/**
 * 선수 — "이 사람은 어떤 선수인가".
 *
 * 예전에는 라인별·포지션 배지·포지션 풀·듀오·라이벌·비교·플레이스타일이 각각 탭이었고,
 * 효율 관련 여섯 개는 아예 다른 페이지에 있었다. 성격이 같은 것끼리 묶어 탭을 다섯으로 줄였다.
 */

const 포지션_SECTIONS: Section[] = [
  { key: 'badge', title: '포지션별 1위', hint: '라인마다 가장 점수가 높은 사람',
    component: lazy(() => import('./stats-tabs/PositionTab')) },
  { key: 'pool',  title: '포지션 풀',   hint: '누가 어느 자리를 얼마나 소화하는가',
    component: lazy(() => import('./stats-tabs/PositionPoolTab')) },
];

const 관계_SECTIONS: Section[] = [
  { key: 'duo',   title: '같은 팀일 때', hint: '함께 뛰었을 때의 전적. 표본이 적은 조합은 승률이 흔들리니 판수를 같이 본다',
    component: lazy(() => import('./stats-tabs/DuoTab')) },
  { key: 'rival', title: '맞붙었을 때',  hint: '상대 팀으로 만난 전적',
    component: lazy(() => import('./stats-tabs/RivalTab')) },
];

const 효율_SECTIONS: Section[] = [
  { key: 'gold',    title: '골드 효율',   hint: '같은 골드로 얼마나 딜을 뽑았나',
    component: lazy(() => import('./stats-tabs/GoldEffTab')) },
  { key: 'damage',  title: '데미지 분석', hint: '물리·마법·고정 피해 구성',
    component: lazy(() => import('./stats-tabs/DamageAnalysisTab')) },
  { key: 'survive', title: '생존력',      hint: '얼마나 안 죽고 버티는가',
    component: lazy(() => import('./stats-tabs/SurvivalTab')) },
  { key: 'vision',  title: '시야 지배',   hint: '와드와 시야 점수',
    component: lazy(() => import('./stats-tabs/VisionDominanceTab')) },
  { key: 'jungle',  title: '정글 기여',   hint: '정글러 전용 지표',
    component: lazy(() => import('./stats-tabs/JungleTab')) },
  { key: 'support', title: '서폿 기여',   hint: '서포터 전용 지표',
    component: lazy(() => import('./stats-tabs/SupportTab')) },
];

const TABS = [
  { key: 'position', label: '포지션',      component: 포지션 },
  { key: 'relation', label: '관계',        component: 관계 },
  { key: 'efficiency', label: '효율',      component: 효율 },
  { key: 'compare',  label: '비교',        component: lazy(() => import('./stats-tabs/CompareTab')) },
  { key: 'dna',      label: '플레이스타일', component: lazy(() => import('./stats-tabs/DnaTab')) },
];

export function PlayerAnalysisPage() {
  return <TabPage title="선수 분석" subtitle="포지션 · 관계 · 효율" tabs={TABS} />;
}

function 포지션({ mode }: { mode: string }) { return <SectionStack sections={포지션_SECTIONS} mode={mode} />; }
function 관계({ mode }: { mode: string })   { return <SectionStack sections={관계_SECTIONS} mode={mode} />; }
function 효율({ mode }: { mode: string })   { return <SectionStack sections={효율_SECTIONS} mode={mode} />; }
