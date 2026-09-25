/**
 * 차트의 좌표 계산. 그리는 것은 `components/ds/Chart.tsx` 가 한다.
 *
 * 순수 함수만 여기 둔다 — 테스트 커버리지가 `src/lib/**` 와 `src/hooks/**` 로 한정돼 있어서,
 * 계산을 컴포넌트 안에 두면 검증할 방법이 없다.
 */

/**
 * 소환사의 협곡 좌표의 최댓값. 백엔드 `MapGeometry.MAP_MAX` 와 같은 축이다.
 *
 * 실제로 밟히는 범위는 이보다 좁다(17판 실측 x 130~14,589 / y 135~14,673). 화면 비율로
 * 정규화하는 데만 쓰고 영역 판정에는 쓰지 않는다 — 판정은 백엔드가 한다.
 */
export const MAP_MAX = 14870;

/** 눈금 최댓값. [step] 단위로 올려 잡아 축 숫자가 읽기 좋게 한다. */
export function niceScale(values: number[], step = 500): number {
  const max = Math.max(0, ...values.map(Math.abs));
  return Math.max(step, Math.ceil(max / step) * step);
}

/**
 * 값 목록을 `polyline` 의 points 문자열로.
 *
 * 기준선(0)이 세로 가운데다. [pad] 는 위아래로 남기는 여유 — 없으면 최댓값이 테두리에 붙는다.
 */
export function linePoints(
  values: number[],
  opts: { width: number; height: number; scale: number; pad?: number },
): string {
  const { width, height, scale, pad = 1 } = opts;
  if (values.length === 0) return '';
  if (values.length === 1) return `0,${height / 2} ${width},${height / 2}`;

  const mid = height / 2;
  return values
    .map((v, i) => `${(i / (values.length - 1)) * width},${mid - (v / scale) * (mid - pad)}`)
    .join(' ');
}

/**
 * 맵 좌표를 SVG 좌표로. **y 를 뒤집는다.**
 *
 * 협곡은 y 가 위로 갈수록 커지고(블루 본진이 아래쪽) SVG 는 아래로 갈수록 커진다. 뒤집지
 * 않으면 미니맵이 위아래로 거꾸로 나온다 — 탑 라인이 아래에 그려진다.
 */
export function mapToSvg(point: { x: number; y: number }, size: number): { x: number; y: number } {
  return {
    x: (point.x / MAP_MAX) * size,
    y: size - (point.y / MAP_MAX) * size,
  };
}

/**
 * 히트맵 격자 칸을 SVG 사각형으로. 맵 좌표와 같은 이유로 y 를 뒤집는다.
 *
 * 돌려주는 `y` 는 칸의 **위쪽** 변이다(SVG `rect` 가 그걸 원한다).
 */
export function gridCellToSvg(
  cell: { x: number; y: number },
  grid: number,
  size: number,
): { x: number; y: number; size: number } {
  const cellSize = size / grid;
  return {
    x: cell.x * cellSize,
    y: size - (cell.y + 1) * cellSize,
    size: cellSize,
  };
}

/**
 * 히트맵 칸의 진하기(0~1). 가장 많은 칸을 1 로 둔다.
 *
 * 제곱근을 쓴다 — 한 칸에 몰린 값이 다른 칸을 전부 투명하게 만드는 걸 막는다. 표본이
 * 작을 때 특히 그렇다(17판이면 대부분 칸이 1~2 카운트다).
 */
export function heatIntensity(count: number, max: number): number {
  if (max <= 0) return 0;
  return Math.sqrt(count / max);
}

/** 분을 `12:30` 꼴로. 이벤트 띠의 눈금에 쓴다. */
export function formatClock(ms: number): string {
  const total = Math.max(0, Math.round(ms / 1000));
  return `${Math.floor(total / 60)}:${String(total % 60).padStart(2, '0')}`;
}

/**
 * 시간축에서의 위치(0~1). [durationMs] 가 0이면 0 을 돌려준다.
 *
 * 1 을 넘지 않게 자른다 — 프레임 타임스탬프가 경기 길이를 조금 넘겨 오는 경우가 있어서
 * (LCU 는 정각보다 조금씩 밀린다) 그대로 쓰면 아이콘이 오른쪽으로 삐져나간다.
 */
export function timeFraction(ms: number, durationMs: number): number {
  if (durationMs <= 0) return 0;
  return Math.min(1, Math.max(0, ms / durationMs));
}
