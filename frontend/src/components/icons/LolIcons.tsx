import type { SVGProps } from "react";

/**
 * LoL 고유 개념 아이콘.
 *
 * 나머지 UI 아이콘(트로피·왕관·해골·불꽃 등)은 lucide-react 를 그대로 쓴다. 여기 있는 것들은
 * lucide 에 없어서 그동안 이모지로 때우던 것들이다 — 라인 5종, 드래곤, 바론, 포탑, 와드, CS.
 * 이모지는 OS 마다 모양이 달라 디자인이 깨지고, 미드 라인처럼 아예 대응하는 그림이 없는 개념도 있었다.
 *
 * lucide 와 같은 규격이라 나란히 놓아도 선 굵기와 크기가 맞는다.
 *   viewBox 0 0 24 24 / stroke-width 2 / round cap·join / fill none / stroke currentColor
 * 색은 currentColor 라 부모의 color 를 따라간다. 테마 토큰이 그대로 먹는다.
 */

export interface LolIconProps extends Omit<SVGProps<SVGSVGElement>, "children"> {
  /** 한 변의 픽셀 크기. 기본 24. */
  size?: number | string;
}

function base({ size = 24, ...rest }: LolIconProps) {
  return {
    xmlns: "http://www.w3.org/2000/svg",
    width: size,
    height: size,
    viewBox: "0 0 24 24",
    fill: "none",
    stroke: "currentColor",
    strokeWidth: 2,
    strokeLinecap: "round" as const,
    strokeLinejoin: "round" as const,
    "aria-hidden": true,
    ...rest,
  };
}

/** 탑 라인 */
export function LaneTopIcon(props: LolIconProps) {
  return (
    <svg {...base(props)}>
      <path d="m13 5 8 8-8 8-8-8Z" />
      <path d="m4 8 4-4" />
    </svg>
  );
}

/** 정글 */
export function LaneJungleIcon(props: LolIconProps) {
  return (
    <svg {...base(props)}>
      <path d="M12 2 22 12 12 22 2 12Z" />
      <path d="M12 7v10" />
      <path d="m9 11 3-4 3 4" />
      <path d="m8 15 4-4 4 4" />
    </svg>
  );
}

/** 미드 라인 */
export function LaneMidIcon(props: LolIconProps) {
  return (
    <svg {...base(props)}>
      <path d="m12 2 10 10-10 10-10-10Z" />
      <path d="m7 17 10-10" />
    </svg>
  );
}

/** 원딜(바텀) 라인 */
export function LaneAdcIcon(props: LolIconProps) {
  return (
    <svg {...base(props)}>
      <path d="m11 4 7 7-7 7-7-7Z" />
      <line x1="20" y1="14" x2="14" y2="20" />
    </svg>
  );
}

/** 서포터 */
export function LaneSupportIcon(props: LolIconProps) {
  return (
    <svg {...base(props)}>
      <path d="m12 3 9 9-9 9-9-9Z" />
      <path d="M12 9v6" />
      <path d="M9 12h6" />
    </svg>
  );
}

/** 드래곤 */
export function DragonIcon(props: LolIconProps) {
  return (
    <svg {...base(props)}>
      <path d="M 5 22 C 5 17 6 13 8 11 C 6 10 4 8 2 4 C 5 3 8 4 11 7 C 14 7.5 17 8.5 21 11 L 21 13 L 15 14 L 14 17 L 19 18.5 L 18 20.5 L 13 20.5 C 12 21 11 21.5 11 22" />
      <circle cx="13" cy="11" r="1" />
    </svg>
  );
}

/** 바론 내셔 */
export function BaronIcon(props: LolIconProps) {
  return (
    <svg {...base(props)}>
      <path d="M5 21v-5l-3-3 4-2-2-4 4 1 3-6 4 3q4 1 7 4l-8 4 7 4-4 1.5q-4 .5-6 2.5" />
      <path d="m18 11-1 2.5" />
      <circle cx="15" cy="8.5" r="1" />
    </svg>
  );
}

/** 포탑 */
export function TurretIcon(props: LolIconProps) {
  return (
    <svg {...base(props)}>
      <path d="M2 21h20" /><path d="M5 21 7 9H4V3h3v3h3V3h4v3h3V3h3v6h-3l2 12" /><path d="M10 14v-2a2 2 0 0 1 4 0v2z" />
    </svg>
  );
}

/** 넥서스·오브젝트 */
export function NexusIcon(props: LolIconProps) {
  return (
    <svg {...base(props)}>
      <path d="M12 3 18 10 12 17 6 10ZM12 3v14" />
      <path d="m6 10 6 2 6-2" />
      <path d="M12 17v4m-4 0h8" />
      <path d="m4 4 2 2m14-2-2 2" />
    </svg>
  );
}

/** 와드·시야 */
export function WardIcon(props: LolIconProps) {
  return (
    <svg {...base(props)}>
      <path d="M12 2v2" />
      <path d="M6 10a6 6 0 0 1 12 0c0 4-3 6.5-6 7.5-3-1-6-3.5-6-7.5Z" />
      <path d="M7 10.5q5-3.5 10 0q-5 3.5-10 0Z" />
      <circle cx="12" cy="10.5" r="1" />
      <path d="M12 17.5V22M9 20h6" />
    </svg>
  );
}

/** 미니언 CS */
export function MinionIcon(props: LolIconProps) {
  return (
    <svg {...base(props)}>
      <path d="M12 2v20" />
      <path d="M7 4l5 5 5-5" />
      <path d="M7 9l5 5 5-5" />
      <path d="M7 14l5 5 5-5" />
    </svg>
  );
}

/** 펜타킬 */
export function PentakillIcon(props: LolIconProps) {
  return (
    <svg {...base(props)}>
      <path d="M12 20V4" />
      <path d="M12 20Q9 12 7 6" />
      <path d="M12 20Q15 12 17 6" />
      <path d="M12 20Q6 16 3 11" />
      <path d="M12 20Q18 16 21 11" />
    </svg>
  );
}
