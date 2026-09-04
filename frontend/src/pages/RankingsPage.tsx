import { lazy } from 'react';
import { TabPage } from '@/components/ds/TabPage';

/**
 * 리더보드 — "누가 잘하나".
 *
 * 개요는 홈이 이미 같은 내용을 보여주고 있어 뺐고, 티어리스트는 사람이 아니라 챔피언 이야기라
 * 챔피언 화면으로 옮겼다. 여기 남는 건 전부 사람 순위다.
 */
const TABS = [
  { key: 'elo',  label: '종합',
    hint: '승패로만 매기는 실력 점수. 이긴 팀이 얻은 만큼 진 팀이 잃어 총합은 늘 그대로다. 3경기 미만은 배치 중으로 순위에서 뺀다',
    component: lazy(() => import('./stats-tabs/EloTab')) },
  { key: 'mvp',  label: 'MVP',
    hint: '경기마다 가장 잘한 사람을 뽑아 누적한 것',
    component: lazy(() => import('./stats-tabs/MvpTab')) },
  { key: 'kp',   label: '킬 관여',
    hint: '팀이 낸 킬 중 내가 킬이나 어시스트로 낀 비율',
    component: lazy(() => import('./stats-tabs/KillParticipationTab')) },
  { key: 'lane', label: '라인별',
    hint: '라인을 골라 그 자리에서의 성적만 본다',
    component: lazy(() => import('./stats-tabs/LaneTab')) },
];

export function RankingsPage() {
  return <TabPage title="리더보드" subtitle="Elo 와 부문별 순위" tabs={TABS} />;
}
