import { Suspense, useState } from 'react';
import type { ComponentType } from 'react';
import { LoadingCenter } from '@/components/common/Spinner';
import { ErrorBoundary } from '@/components/common/ErrorBoundary';

/**
 * 분석 화면들의 공용 껍데기.
 *
 * 랭킹·선수 분석·챔피언 분석·명예의 전당이 전부 같은 모양이었다.
 * 제목 + 탭 바 + 탭 내용. 여러 군데에 같은 마크업이 복사돼 있어 한쪽만 고쳐지곤 했다.
 *
 * 모드 선택 버튼은 뺐다. MODES 에 '5v5 내전' 하나뿐이라 누를 곳이 없는 스위치였고,
 * 백엔드에서도 normal 과 all 이 같은 queueId 로 매핑돼 실제로 아무것도 바뀌지 않았다.
 */

export interface TabDef {
  key: string;
  label: string;
  /** 탭 이름만으로 무엇을 보는지 안 읽히는 지표가 많다. 있으면 내용 위에 한 줄로 띄운다. */
  hint?: string;
  /** 탭 내용. mode 를 받는 기존 탭 컴포넌트를 그대로 넘긴다. */
  component: ComponentType<{ mode: string }>;
}

/** 기존 탭들이 기대하는 값. 백엔드에서 normal 과 동일하게 처리된다. */
const MODE = 'all';

export function TabPage({
  title,
  subtitle,
  tabs,
}: {
  title: string;
  subtitle?: string;
  tabs: TabDef[];
}) {
  const [active, setActive] = useState(tabs[0]?.key ?? '');
  const current = tabs.find((t) => t.key === active) ?? tabs[0];
  const Body = current?.component;

  return (
    <div className="t-page">
      <div className="t-page-head">
        <h1 className="t-page-title">{title}</h1>
        {subtitle && <p className="t-page-sub">{subtitle}</p>}
      </div>

      <div className="t-tabs">
        {tabs.map((t) => (
          <button
            key={t.key}
            className={`t-tab${active === t.key ? ' active' : ''}`}
            onClick={() => setActive(t.key)}
          >
            {t.label}
          </button>
        ))}
      </div>

      <section className="t-card">
        {current?.hint && <p className="t-tab-hint">{current.hint}</p>}
        {/* 탭 하나가 던져도 페이지 전체가 빈 화면이 되지 않게 한다.
            실제로 응답 필드가 어긋난 탭 셋이 이렇게 페이지를 통째로 죽이고 있었다. */}
        <ErrorBoundary
          fallback={
            <p className="t-empty">
              이 항목을 불러오지 못했습니다.
              <span className="t-detail-sub" style={{ display: 'block', marginTop: 4 }}>
                다른 탭은 정상입니다.
              </span>
            </p>
          }
        >
          <Suspense fallback={<LoadingCenter />}>
            {Body && <Body key={active} mode={MODE} />}
          </Suspense>
        </ErrorBoundary>
      </section>
    </div>
  );
}
