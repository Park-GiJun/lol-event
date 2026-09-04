import { Suspense } from 'react';
import type { ComponentType } from 'react';
import { LoadingCenter } from '@/components/common/Spinner';
import { ErrorBoundary } from '@/components/common/ErrorBoundary';

/**
 * 여러 지표를 한 화면에 세로로 이어 붙인다.
 *
 * 원래는 지표 하나가 탭 하나였다. 31개가 다섯 페이지에 흩어져 있었고, 어느 탭에 뭐가 있는지
 * 아무도 외우지 못했다. 그렇다고 탭만 네 페이지로 옮겨 담으면 한 페이지에 열다섯 개가 되어
 * 산만함이 화면 안으로 자리를 옮길 뿐이다.
 *
 * 그래서 서로 붙어 있어야 할 지표들은 탭을 없애고 세로로 잇는다. 스크롤 한 번이면 다 보이고,
 * 각 섹션에 제목이 붙어 있어 무엇을 보고 있는지 늘 알 수 있다.
 */

export interface Section {
  key: string;
  title: string;
  /** 이 섹션이 무엇을 말하는지 한 줄. 지표 이름만으로 안 읽히는 것들이 많다. */
  hint?: string;
  component: ComponentType<{ mode: string }>;
}

export function SectionStack({ sections, mode }: { sections: Section[]; mode: string }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 28 }}>
      {sections.map(({ key, title, hint, component: Body }) => (
        <section key={key}>
          <div className="t-section-head">
            <h3 className="t-section-title">{title}</h3>
            {hint && <span className="t-section-hint">{hint}</span>}
          </div>
          {/* 섹션마다 따로 감싼다. 한 화면에 열 개 넘게 붙어 있어서, 경계가 없으면
              한 섹션이 던질 때 페이지 전체가 빈 화면이 된다. 어느 섹션이 문제인지도 안 보인다. */}
          <ErrorBoundary
            fallback={
              <p className="t-empty">
                이 섹션을 불러오지 못했습니다.
                <span className="t-detail-sub" style={{ display: 'block', marginTop: 4 }}>
                  나머지 항목은 정상입니다.
                </span>
              </p>
            }
          >
            <Suspense fallback={<LoadingCenter />}>
              <Body mode={mode} />
            </Suspense>
          </ErrorBoundary>
        </section>
      ))}
    </div>
  );
}
