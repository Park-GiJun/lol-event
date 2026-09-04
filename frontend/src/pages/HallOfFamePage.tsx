import { lazy } from 'react';
import { SectionStack } from '@/components/ds/SectionStack';

/**
 * 명예의 전당 — 통계라기보다 이야깃거리.
 *
 * 혼돈 지수·떡락 지수·컴백 같은 것들은 154경기 규모에서 통계로는 노이즈에 가깝다.
 * 그렇다고 지울 것도 아니다. 공식 대회가 아니라 내전이고, 사람들이 사이트를 다시 열어 보는
 * 이유는 대개 이런 쪽이다. 다만 순위표 사이에 섞여 있으면 실력 지표처럼 읽히니 여기로 몰았다.
 *
 * 리더보드가 "누가 잘하나"라면 여기는 "무슨 일이 있었나"다.
 */

const SECTIONS = [
  { key: 'awards',      title: '이 주의 어워즈',
    hint: '기간별로 가장 눈에 띈 기록',
    component: lazy(() => import('./stats-tabs/AwardsTab')) },
  { key: 'multikill',   title: '멀티킬',
    hint: '펜타 · 쿼드라 · 트리플',
    component: lazy(() => import('./stats-tabs/MultikillTab')) },
  { key: 'comeback',    title: '역전극',
    hint: '지고 있다가 뒤집은 경기',
    component: lazy(() => import('./stats-tabs/ComebackTab')) },
  { key: 'defeat',      title: '떡락 지수',
    hint: '이기고 있다가 놓친 경기',
    component: lazy(() => import('./stats-tabs/DefeatTab')) },
  { key: 'chaos',       title: '혼돈 지수',
    hint: '킬이 가장 많이 터진 난장판',
    component: lazy(() => import('./stats-tabs/ChaosTab')) },
  { key: 'earlygame',   title: '초반 지배',
    hint: '초반을 어떻게 굴렸나',
    component: lazy(() => import('./stats-tabs/EarlyGameTab')) },
  { key: 'lategame',    title: '후반 지배',
    hint: '길어진 경기에서의 성적',
    component: lazy(() => import('./stats-tabs/LateGameTab')) },
  { key: 'surrender',   title: '서렌더',
    hint: '누가 언제 항복을 눌렀나',
    component: lazy(() => import('./stats-tabs/SurrenderTab')) },
  { key: 'gamelength',  title: '경기 시간',
    hint: '짧은 판과 긴 판의 성향 차이',
    component: lazy(() => import('./stats-tabs/GameLengthTab')) },
  { key: 'timepattern', title: '시간대별',
    hint: '몇 시에 강한가',
    component: lazy(() => import('./stats-tabs/TimePatternTab')) },
  { key: 'sessions',    title: '세션 기록',
    hint: '하루에 몰아서 한 날들',
    component: lazy(() => import('./stats-tabs/SessionsTab')) },
];

/** TabPage 와 달리 탭이 없다. 순서대로 훑어보는 화면이라 나누지 않는다. */
export function HallOfFamePage() {
  return (
    <div className="t-page">
      <div className="t-page-head">
        <h1 className="t-page-title">명예의 전당</h1>
        <p className="t-page-sub">순위가 아니라 기록. 이 내전에서 무슨 일이 있었는지 모아 둔 곳이다.</p>
      </div>
      <section className="t-card">
        <SectionStack sections={SECTIONS} mode="all" />
      </section>
    </div>
  );
}
