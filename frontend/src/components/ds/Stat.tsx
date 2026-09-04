import type { ReactNode } from 'react';
import type { SampleGrade } from '@/lib/types/stats';

/**
 * 비율 지표를 표본과 묶어서 보여주는 컴포넌트들.
 *
 * 154경기짜리 내전에서는 어떤 축으로 잘라도 표본이 금방 한 자리 수로 떨어진다.
 * "승률 100%" 만 크게 띄우면 그건 대개 5경기짜리다. 그래서 비율을 쓰는 자리에는
 * 경기 수가 항상 따라붙도록 컴포넌트 단위로 고정한다.
 */

/** 표본이 못 미더울 때만 배지를 단다. 충분하면 아무것도 붙이지 않는다. */
export function SampleBadge({ grade, games }: { grade: SampleGrade; games: number }) {
  if (grade === 'HIGH' || grade === 'MEDIUM') return null;
  return (
    <span className="t-chip t-chip-low" title={`${games}경기 — 표본이 적어 값이 크게 흔들립니다`}>
      표본 적음
    </span>
  );
}

interface RateProps {
  /** 관측 비율 (0~100) */
  value: number;
  games: number;
  grade?: SampleGrade;
  /** 보정값이 있으면 툴팁으로 같이 보여준다 */
  adjusted?: number;
}

/** 승률처럼 비율로 읽는 값. 경기 수를 떼어놓을 수 없다. */
export function Rate({ value, games, grade, adjusted }: RateProps) {
  return (
    <span
      className="t-person"
      title={adjusted !== undefined ? `보정 승률 ${adjusted}% (표본 ${games}경기)` : undefined}
    >
      <b style={{ fontVariantNumeric: 'tabular-nums' }}>{value}%</b>
      <span className="t-stat-sample">{games}경기</span>
      {grade && <SampleBadge grade={grade} games={games} />}
    </span>
  );
}

export function Stat({
  label,
  value,
  sample,
  hero = false,
}: {
  label: string;
  value: ReactNode;
  sample?: ReactNode;
  hero?: boolean;
}) {
  return (
    <div className="t-stat">
      <span className="t-stat-label">{label}</span>
      <span className={`t-stat-value${hero ? ' t-stat-hero' : ''}`}>{value}</span>
      {sample && <span className="t-stat-sample">{sample}</span>}
    </div>
  );
}

export function WinBar({ winRate }: { winRate: number }) {
  return (
    <div className="t-bar" aria-hidden>
      <div
        className={`t-bar-fill${winRate < 50 ? ' t-bar-fill-loss' : ''}`}
        style={{ width: `${Math.max(0, Math.min(100, winRate))}%` }}
      />
    </div>
  );
}

/** 최근 폼. 최신이 왼쪽. */
export function RecentForm({ form }: { form: string[] }) {
  if (!form.length) return <span className="t-stat-sample">기록 없음</span>;
  return (
    <span className="t-form">
      {form.map((r, i) => (
        <span key={i} className={`t-form-dot ${r === 'W' ? 't-form-w' : 't-form-l'}`}>
          {r === 'W' ? '승' : '패'}
        </span>
      ))}
    </span>
  );
}

export function TierBadge({ tier }: { tier: string }) {
  const known = ['S', 'A', 'B', 'C', 'D'].includes(tier);
  return (
    <span
      className={`t-tier${known ? ` t-tier-${tier}` : ''}`}
      title={known ? `${tier} 티어` : '표본이 부족해 티어를 매기지 않았습니다'}
    >
      {known ? tier : '–'}
    </span>
  );
}
