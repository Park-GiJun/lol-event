import { diffColor, signed } from '@/lib/timeline';

/**
 * 표 안의 15분 격차 한 칸.
 *
 * 타임라인은 새 수집기로 받은 경기에만 있어서, 같은 줄의 다른 칸(전체 경기 기준)과 표본이 다르다.
 * 그래서 격차 옆에 그 값을 만든 경기 수를 붙인다.
 */
export function Diff15({
  value,
  games,
  even = 100,
  digits = 0,
}: {
  value: number | null | undefined;
  /** 이 값을 만든 경기 수. 주면 옆에 작게 붙인다. */
  games?: number;
  /** 이 폭 이하는 호각으로 보고 물들이지 않는다. */
  even?: number;
  digits?: number;
}) {
  if (value == null) return <span className="t-stat-sample">-</span>;
  return (
    <span style={{ whiteSpace: 'nowrap', fontVariantNumeric: 'tabular-nums' }}>
      <b style={{ color: diffColor(value, even) }}>{signed(value, digits)}</b>
      {games != null && <span className="t-stat-sample"> {games}경기</span>}
    </span>
  );
}
