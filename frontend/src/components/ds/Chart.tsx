import { useId, type ReactNode } from 'react';
import { MAP_MAX, gridCellToSvg, heatIntensity, linePoints, mapToSvg, niceScale, timeFraction } from '@/lib/chart';
import { diffColor, signed } from '@/lib/timeline';

/**
 * 차트 프리미티브. 전부 순수 인라인 SVG 다 — 이 저장소에 차트 라이브러리를 들이지 않는다.
 *
 * 좌표 계산은 `lib/chart.ts` 가 하고 여기서는 그리기만 한다. 지켜야 할 기법 네 가지:
 *
 * 1. `preserveAspectRatio="none"` 으로 가로를 늘일 때 **모든 stroke 에
 *    `vectorEffect="non-scaling-stroke"`** 를 준다. 없으면 선 굵기가 같이 늘어난다.
 * 2. 툴팁은 JS 상태가 아니라 투명 `<rect>` + `<title>` 로 만든다. 점을 직접 찍으면
 *    `preserveAspectRatio="none"` 에 눌려 타원이 된다.
 * 3. 축 라벨은 SVG **밖** 의 `position:absolute` div 로 둔다. 안에 넣으면 글자가 눌린다.
 * 4. `clipPath` id 는 `useId()` 로 만든다. 한 화면에 차트가 여럿이면 id 가 부딪힌다.
 */

// ────────── 스파크라인 ──────────

/**
 * 표 한 줄에 붙는 작은 곡선.
 *
 * [scale] 을 주지 않으면 값마다 축을 따로 잡는다 — 모양만 비교할 때 쓴다. 여러 줄의 크기를
 * 견주려면 바깥에서 같은 [scale] 을 내려 줘야 한다.
 */
export function Sparkline({
  values,
  width = 80,
  height = 24,
  scale,
}: {
  values: number[];
  width?: number;
  height?: number;
  scale?: number;
}) {
  if (values.length < 2) return null;

  const axis = scale ?? niceScale(values);
  const points = linePoints(values, { width, height, scale: axis });
  const last = values[values.length - 1];

  return (
    <svg viewBox={`0 0 ${width} ${height}`} width={width} height={height} aria-hidden>
      <line x1={0} x2={width} y1={height / 2} y2={height / 2} stroke="var(--color-border)" strokeDasharray="2 2" />
      <polyline points={points} fill="none" stroke={diffColor(last)} strokeWidth={1.5} />
    </svg>
  );
}

// ────────── 기준선 위아래로 갈리는 면적 차트 ──────────

export type DivergingPoint = {
  /** 가로축 값. 보통 분이다. */
  at: number;
  value: number;
  /** 툴팁에 덧붙일 설명. 표본 수 같은 것. */
  note?: string;
};

/**
 * 기준선(0) 위는 이긴 색, 아래는 진 색으로 칠하는 면적 차트.
 *
 * 골드 격차처럼 **부호가 의미인** 값에 쓴다. 뒤로 갈수록 표본이 줄어드는 경우가 많아
 * 점마다 [DivergingPoint.note] 를 툴팁에 달 수 있게 해 뒀다.
 */
export function DivergingArea({
  points,
  label,
  height = 150,
  unit = '',
  tickStep = 5,
}: {
  points: DivergingPoint[];
  label: string;
  height?: number;
  unit?: string;
  tickStep?: number;
}) {
  const id = useId();
  const W = 600;
  const PAD = 6;
  const mid = height / 2;

  if (points.length < 2) return null;

  const maxAt = points[points.length - 1].at || 1;
  const scale = niceScale(points.map(p => p.value));
  const x = (at: number) => (at / maxAt) * W;
  const y = (v: number) => mid - (v / scale) * (mid - PAD);

  const line = points.map(p => `${x(p.at)},${y(p.value)}`).join(' ');
  const area = `M${x(points[0].at)},${mid} L${line.replace(/ /g, ' L')} L${x(maxAt)},${mid} Z`;

  const ticks: number[] = [];
  for (let t = tickStep; t < maxAt; t += tickStep) ticks.push(t);

  return (
    <div style={{ marginTop: 18 }}>
      <div
        style={{
          display: 'flex', justifyContent: 'space-between', fontSize: 12,
          color: 'var(--color-text-secondary)', marginBottom: 4,
        }}
      >
        <span>{label}</span>
        <span style={{ fontVariantNumeric: 'tabular-nums' }}>±{scale.toLocaleString()}</span>
      </div>

      <svg viewBox={`0 0 ${W} ${height}`} width="100%" height={height} preserveAspectRatio="none" role="img" aria-label={label}>
        <defs>
          <clipPath id={`${id}-up`}><rect x="0" y="0" width={W} height={mid} /></clipPath>
          <clipPath id={`${id}-down`}><rect x="0" y={mid} width={W} height={mid} /></clipPath>
        </defs>

        {ticks.map(t => (
          <line key={t} x1={x(t)} x2={x(t)} y1={0} y2={height} stroke="var(--color-border)" strokeWidth={1} vectorEffect="non-scaling-stroke" />
        ))}
        <line x1={0} x2={W} y1={mid} y2={mid} stroke="var(--color-border)" strokeDasharray="4 4" vectorEffect="non-scaling-stroke" />

        <path d={area} fill="var(--color-win)" fillOpacity={0.18} clipPath={`url(#${id}-up)`} />
        <path d={area} fill="var(--color-loss)" fillOpacity={0.18} clipPath={`url(#${id}-down)`} />
        <polyline points={line} fill="none" stroke="var(--color-text-primary)" strokeWidth={1.5} vectorEffect="non-scaling-stroke" />

        {points.map(p => (
          <rect key={p.at} x={x(p.at) - W / maxAt / 2} y={0} width={W / maxAt} height={height} fill="transparent">
            <title>{`${p.at}${unit} · ${signed(p.value)}${p.note ? ` (${p.note})` : ''}`}</title>
          </rect>
        ))}
      </svg>

      <div style={{ position: 'relative', height: 16, fontSize: 11, color: 'var(--color-text-secondary)' }}>
        {ticks.map(t => (
          <span key={t} style={{ position: 'absolute', left: `${(t / maxAt) * 100}%`, transform: 'translateX(-50%)' }}>
            {t}{unit}
          </span>
        ))}
      </div>
    </div>
  );
}

// ────────── 이벤트 띠 ──────────

export type StripEvent = {
  key: string;
  timestampMs: number;
  /** 아이콘. `LolIcons` 의 것을 그대로 넣는다. */
  icon: ReactNode;
  /** 100 이면 블루, 200 이면 레드. 없으면 중립색. */
  teamId?: number | null;
  title: string;
};

/**
 * 시간축에 사건을 늘어놓는 띠. 경기의 흐름을 한 줄로 읽게 한다.
 *
 * 아이콘이 겹치는 것은 막지 않는다 — 한타 직후에 오브젝트가 몰리는 게 사실이고, 그걸
 * 흩뜨리면 시각이 거짓말이 된다. 대신 위아래 두 줄로 팀을 갈라 겹침을 줄인다.
 */
export function EventStrip({
  events,
  durationMs,
  label,
}: {
  events: StripEvent[];
  durationMs: number;
  label: string;
}) {
  if (events.length === 0) return null;

  const row = (teamId: number) => events.filter(e => e.teamId === teamId);
  const neutral = events.filter(e => e.teamId !== 100 && e.teamId !== 200);

  const marks = (list: StripEvent[], color: string) =>
    list.map(e => (
      <span
        key={e.key}
        title={e.title}
        style={{
          position: 'absolute',
          left: `${timeFraction(e.timestampMs, durationMs) * 100}%`,
          transform: 'translateX(-50%)',
          color,
          display: 'flex',
          lineHeight: 0,
        }}
      >
        {e.icon}
      </span>
    ));

  return (
    <div style={{ marginTop: 18 }} role="img" aria-label={label}>
      <div style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginBottom: 6 }}>{label}</div>
      <div style={{ position: 'relative', height: 20 }}>{marks(row(100), 'var(--color-win)')}</div>
      <div
        style={{
          position: 'relative', height: 1, background: 'var(--color-border)', margin: '4px 0',
        }}
      >
        {marks(neutral, 'var(--color-text-secondary)')}
      </div>
      <div style={{ position: 'relative', height: 20 }}>{marks(row(200), 'var(--color-loss)')}</div>
    </div>
  );
}

// ────────── 미니맵 ──────────

export type MapMark = {
  key: string;
  x: number;
  y: number;
  /** 100 이면 블루, 200 이면 레드. 없으면 중립색. */
  teamId?: number | null;
  title: string;
};

export type HeatCell = { x: number; y: number; count: number };

/**
 * 협곡 미니맵 위에 점이나 격자를 얹는다.
 *
 * 배경은 `public/minimap.png` 다. 밝은 배경 위에서 도드라지지 않게 눌러 깔고, 그 위의
 * 데이터가 주인공이 되게 한다.
 *
 * **y 는 [mapToSvg] 가 뒤집는다** — 협곡은 y 가 위로 커지고 SVG 는 아래로 커진다.
 */
export function MapScatter({
  marks = [],
  cells = [],
  grid = 32,
  label,
  size = 320,
}: {
  marks?: MapMark[];
  cells?: HeatCell[];
  grid?: number;
  label: string;
  size?: number;
}) {
  const maxCount = Math.max(0, ...cells.map(c => c.count));

  return (
    <div style={{ position: 'relative', width: '100%', maxWidth: size, aspectRatio: '1 / 1' }}>
      <img
        src="/minimap.png"
        alt=""
        aria-hidden
        style={{
          position: 'absolute', inset: 0, width: '100%', height: '100%',
          // 데이터가 배경에 묻히지 않게 눌러 깐다.
          filter: 'brightness(1.15) saturate(0.55)', opacity: 0.45, borderRadius: 'var(--radius-md)',
        }}
      />
      <svg
        viewBox={`0 0 ${size} ${size}`}
        style={{ position: 'absolute', inset: 0, width: '100%', height: '100%' }}
        role="img"
        aria-label={label}
      >
        {cells.map(c => {
          const rect = gridCellToSvg(c, grid, size);
          return (
            <rect
              key={`${c.x}-${c.y}`}
              x={rect.x}
              y={rect.y}
              width={rect.size}
              height={rect.size}
              fill="var(--color-loss)"
              fillOpacity={heatIntensity(c.count, maxCount) * 0.75}
            >
              <title>{`${c.count}회`}</title>
            </rect>
          );
        })}

        {marks.map(m => {
          const at = mapToSvg(m, size);
          return (
            <circle
              key={m.key}
              cx={at.x}
              cy={at.y}
              r={4}
              fill={m.teamId === 100 ? 'var(--color-win)' : m.teamId === 200 ? 'var(--color-loss)' : 'var(--color-text-secondary)'}
              fillOpacity={0.75}
              stroke="var(--color-bg, #fff)"
              strokeWidth={1}
            >
              <title>{m.title}</title>
            </circle>
          );
        })}
      </svg>
    </div>
  );
}

/** 좌표를 쓰는 화면이 맵 크기를 다시 적지 않게 다시 내보낸다. */
export { MAP_MAX };
