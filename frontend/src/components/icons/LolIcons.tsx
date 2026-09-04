import type { ReactElement, SVGProps } from "react";

/**
 * 이 사이트의 아이콘 전부.
 *
 * 예전에는 이모지와 lucide 를 섞어 썼다. 이모지는 OS 마다 그림이 달라 디자인이 흔들리고,
 * 미드 라인이나 바론처럼 대응하는 그림이 아예 없는 개념도 있었다. lucide 는 일관적이지만
 * LoL 고유 개념이 없어서 결국 이모지로 메워야 했고, 두 계열이 한 화면에 섞여 이질적이었다.
 *
 * 그래서 한 벌로 다시 만들었다. 규격은 전부 같다.
 *   viewBox 0 0 24 24 / stroke-width 2 / round cap·join / fill none / stroke currentColor
 * 색이 currentColor 라 부모의 color 를 따라가고, 테마 토큰이 그대로 먹는다.
 */

export interface IconProps extends Omit<SVGProps<SVGSVGElement>, "children"> {
  /** 한 변의 픽셀 크기. 기본 24. */
  size?: number | string;
}

/** 아이콘 컴포넌트 타입. 네비게이션처럼 아이콘을 값으로 넘기는 곳에서 쓴다. */
export type IconComponent = (props: IconProps) => ReactElement;

function base({ size = 24, ...rest }: IconProps) {
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

// ── LoL 고유 개념 ──────────────────────────────────────────────

/** 탑 라인 */
export function LaneTopIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="m13 5 8 8-8 8-8-8Z" />
      <path d="m4 8 4-4" />
    </svg>
  );
}

/** 정글 */
export function LaneJungleIcon(props: IconProps) {
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
export function LaneMidIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="m12 2 10 10-10 10-10-10Z" />
      <path d="m7 17 10-10" />
    </svg>
  );
}

/** 원딜(바텀) 라인 */
export function LaneAdcIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="m11 4 7 7-7 7-7-7Z" />
      <line x1="20" y1="14" x2="14" y2="20" />
    </svg>
  );
}

/** 서포터 */
export function LaneSupportIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="m12 3 9 9-9 9-9-9Z" />
      <path d="M12 9v6" />
      <path d="M9 12h6" />
    </svg>
  );
}

/** 드래곤 */
export function DragonIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M 5 22 C 5 17 6 13 8 11 C 6 10 4 8 2 4 C 5 3 8 4 11 7 C 14 7.5 17 8.5 21 11 L 21 13 L 15 14 L 14 17 L 19 18.5 L 18 20.5 L 13 20.5 C 12 21 11 21.5 11 22" />
      <circle cx="13" cy="11" r="1" />
    </svg>
  );
}

/** 바론 내셔 */
export function BaronIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M5 21v-5l-3-3 4-2-2-4 4 1 3-6 4 3q4 1 7 4l-8 4 7 4-4 1.5q-4 .5-6 2.5" />
      <path d="m18 11-1 2.5" />
      <circle cx="15" cy="8.5" r="1" />
    </svg>
  );
}

/** 포탑 */
export function TurretIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M2 21h20" /><path d="M5 21 7 9H4V3h3v3h3V3h4v3h3V3h3v6h-3l2 12" /><path d="M10 14v-2a2 2 0 0 1 4 0v2z" />
    </svg>
  );
}

/** 넥서스·오브젝트 */
export function NexusIcon(props: IconProps) {
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
export function WardIcon(props: IconProps) {
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
export function MinionIcon(props: IconProps) {
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
export function PentakillIcon(props: IconProps) {
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

// ── 일반 UI ───────────────────────────────────────────────────

/** 활동·상태 */
export function ActivityIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M2 12h4l3-9 6 18 3-9h4" />
    </svg>
  );
}

/** 경고 */
export function AlertCircleIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <circle cx="12" cy="12" r="10"/><line x1="12" x2="12" y1="8" y2="12"/><line x1="12" x2="12.01" y1="16" y2="16"/>
    </svg>
  );
}

/** 통계 */
export function BarChartIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M3 20h18" />
      <path d="M7 20v-6" />
      <path d="M12 20V9" />
      <path d="M17 20V4" />
    </svg>
  );
}

/** 완료 */
export function CheckCircleIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <circle cx="12" cy="12" r="10"/><path d="m9 12 2 2 4-4"/>
    </svg>
  );
}

/** 데이터 */
export function DatabaseIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <ellipse cx="12" cy="4" rx="9" ry="3" />
      <path d="M3 4v15a9 3 0 0 0 18 0V4" />
      <path d="M3 9a9 3 0 0 0 18 0" />
      <path d="M3 14a9 3 0 0 0 18 0" />
    </svg>
  );
}

/** 내려받기 */
export function DownloadIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M12 17V3" />
      <path d="m6 11 6 6 6-6" />
      <path d="M19 21H5" />
    </svg>
  );
}

/** 바깥 링크 */
export function ExternalLinkIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" />
      <polyline points="15 3 21 3 21 9" />
      <line x1="10" y1="14" x2="21" y2="3" />
    </svg>
  );
}

/** 시야·보기 */
export function EyeIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z" />
      <circle cx="12" cy="12" r="3" />
    </svg>
  );
}

/** 게임 */
export function GamepadIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <line x1="6" x2="10" y1="12" y2="12" />
      <line x1="8" x2="8" y1="10" y2="14" />
      <line x1="15" x2="15.01" y1="13" y2="13" />
      <line x1="18" x2="18.01" y1="11" y2="11" />
      <path d="M18 11V7a2 2 0 0 0-2-2H8a2 2 0 0 0-2 2v4l-3.2 6.4a2 2 0 0 0 2.7 2.7L9 18h6l3.5 2.1a2 2 0 0 0 2.7-2.7L18 11Z" />
    </svg>
  );
}

/** 효율 */
export function GemIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M6 3h12l4 6-10 13L2 9Z" />
      <path d="M11 3 8 9l4 13 4-13-3-6" />
      <path d="M2 9h20" />
    </svg>
  );
}

/** 목록 */
export function ListIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M3 6h.01M8 6h13" />
      <path d="M3 12h.01M8 12h13" />
      <path d="M3 18h.01M8 18h13" />
    </svg>
  );
}

/** 로딩 */
export function LoaderIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M21 12a9 9 0 1 1-6.22-8.56"/>
    </svg>
  );
}

/** 잠금 */
export function LockIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <rect width="18" height="11" x="3" y="11" rx="2" ry="2" />
      <path d="M7 11V7a5 5 0 0 1 10 0v4" />
    </svg>
  );
}

/** 위치·포지션 */
export function MapPinIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z" />
      <circle cx="12" cy="10" r="3" />
    </svg>
  );
}

/** 메뉴 */
export function MenuIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <line x1="4" x2="20" y1="6" y2="6" /><line x1="4" x2="20" y1="12" y2="12" /><line x1="4" x2="20" y1="18" y2="18" />
    </svg>
  );
}

/** 모니터링 */
export function MonitorIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <rect width="20" height="14" x="2" y="3" rx="2" />
      <line x1="12" x2="12" y1="17" y2="21" />
      <line x1="8" x2="16" y1="21" y2="21" />
    </svg>
  );
}

/** 실행 */
export function PlayIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <polygon points="6 3 20 12 6 21 6 3" />
    </svg>
  );
}

/** 수집 */
export function RadioIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <circle cx="12" cy="12" r="1" />
      <path d="M8 9a5 5 0 0 0 0 6" />
      <path d="M16 9a5 5 0 0 1 0 6" />
      <path d="M6 4a10 10 0 0 0 0 16" />
      <path d="M18 4a10 10 0 0 1 0 16" />
    </svg>
  );
}

/** 새로고침 */
export function RefreshIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M21 12a9 9 0 1 1-9-9c2.52 0 4.93 1 6.74 2.74L21 8"/>
      <polyline points="21 3 21 8 16 8"/>
    </svg>
  );
}

/** 검색 */
export function SearchIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/>
    </svg>
  );
}

/** 방패·탑 */
export function ShieldIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M4 4h16v7c0 5.5-4.5 8.5-8 10-3.5-1.5-8-4.5-8-10Z" />
      <path d="M12 4v17" />
    </svg>
  );
}

/** 팀 섞기 */
export function ShuffleIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M2 18h1.4c1.3 0 2.5-.6 3.3-1.7l6.1-8.6c.7-1.1 2-1.7 3.3-1.7H22" />
      <path d="m18 2 4 4-4 4" />
      <path d="M2 6h1.4c1.3 0 2.5.6 3.3 1.7l6.1 8.6c.7 1.1 2 1.7 3.3 1.7H22" />
      <path d="m18 14 4 4-4 4" />
    </svg>
  );
}

/** 교전·경기 */
export function SwordsIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <line x1="4" y1="4" x2="20" y2="20" />
      <line x1="20" y1="4" x2="4" y2="20" />
      <path d="m13 17 4-4m2 8 2-2" />
      <path d="m7 13 4 4M3 19l2 2" />
    </svg>
  );
}

/** 과녁·정확도 */
export function TargetIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="6"/><circle cx="12" cy="12" r="2"/>
    </svg>
  );
}

/** 삭제 */
export function TrashIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M3 6h18" />
      <path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6" />
      <path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2" />
      <line x1="10" x2="10" y1="11" y2="17" />
      <line x1="14" x2="14" y1="11" y2="17" />
    </svg>
  );
}

/** 상승 */
export function TrendingUpIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <polyline points="22 7 13.5 15.5 8.5 10.5 2 17" /><polyline points="16 7 22 7 22 13" />
    </svg>
  );
}

/** 하락 */
export function TrendingDownIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <polyline points="22 17 13.5 8.5 8.5 13.5 2 7" />
      <polyline points="16 17 22 17 22 11" />
    </svg>
  );
}

/** 우승·랭킹 */
export function TrophyIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M6 9H4.5a2.5 2.5 0 0 1 0-5H6" />
      <path d="M18 9h1.5a2.5 2.5 0 0 0 0-5H18" />
      <path d="M6 2h12v7a6 6 0 0 1-12 0Z" />
      <path d="M12 15v3" />
      <path d="M9 18h6l2 4H7Z" />
    </svg>
  );
}

/** 멤버 추가 */
export function UserPlusIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
      <circle cx="9" cy="7" r="4" />
      <line x1="19" x2="19" y1="8" y2="14" />
      <line x1="16" x2="22" y1="11" y2="11" />
    </svg>
  );
}

/** 플레이어 */
export function UserIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <circle cx="12" cy="7" r="4"/>
      <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
    </svg>
  );
}

/** 두 명 */
export function UsersIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" /><circle cx="9" cy="7" r="4" /><path d="M22 21v-2a4 4 0 0 0-3-3.87" /><path d="M16 3.13a4 4 0 0 1 0 7.75" />
    </svg>
  );
}

/** 팀 */
export function UsersThreeIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M7 21v-2a4 4 0 0 1 4-4h2a4 4 0 0 1 4 4v2" />
      <circle cx="12" cy="7" r="3" />
      <path d="M6 10a3 3 0 0 1 0-6M2 21v-2a4 4 0 0 1 4-4" />
      <path d="M18 4a3 3 0 0 1 0 6M22 21v-2a4 4 0 0 0-4-4" />
    </svg>
  );
}

/** 닫기 */
export function CloseIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M18 6 6 18"/><path d="m6 6 12 12"/>
    </svg>
  );
}

/** 임팩트 */
export function ZapIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" />
    </svg>
  );
}

/** 1위 */
export function CrownIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M4.5 17 2.5 7l3.5 5 2.5-6 2 4.5 1.5-6.5 1.5 6.5 2-4.5 2.5 6 3.5-5L19.5 17" />
      <rect x="4" y="17" width="16" height="4" rx="1" />
    </svg>
  );
}

/** 데스 */
export function SkullIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M8 20h8a1 1 0 0 0 1-1v-4c2 0 3.5-1.5 4-4 0-5-4-8-9-8s-9 3-9 8c.5 2.5 2 4 4 4v4a1 1 0 0 0 1 1z"/>
      <circle cx="9" cy="12" r="1"/>
      <circle cx="15" cy="12" r="1"/>
      <path d="M10 17v3m4-3v3"/>
    </svg>
  );
}

/** 골드 */
export function CoinsIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <ellipse cx="10" cy="7" rx="7" ry="3.5"/>
      <path d="M3 7v3a7 3.5 0 0 0 14 0V7"/>
      <path d="M7 14a7 3.5 0 0 0 14 0"/>
      <path d="M7 14v3a7 3.5 0 0 0 14 0v-3"/>
    </svg>
  );
}

/** 항복 */
export function FlagIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z" /><line x1="4" x2="4" y1="22" y2="15" />
    </svg>
  );
}

/** 연승 */
export function FlameIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M12 2c1 2.5 3 4.5 4.5 6.5 1.5 2 2.5 4 2.5 6.5a7 7 0 0 1-14 0c0-2.5 1-4.5 2.5-6.5 1.5-2 3.5-4 4.5-6.5z" />
      <path d="M12 12c-1.5 1.5-2 2.5-2 4a2 2 0 0 0 4 0c0-1.5-.5-2.5-2-4z" />
    </svg>
  );
}

/** 별점 */
export function StarIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
    </svg>
  );
}

/** 메달 */
export function MedalIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M9 11 5 2h4l3 8 3-8h4l-4 9" />
      <circle cx="12" cy="15" r="5" />
    </svg>
  );
}

/** 팁 */
export function BulbIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M12 2v2M4 4l2 2M20 4l-2 2M2 12h2M20 12h2" />
      <path d="M9 16a5 5 0 1 1 6 0" />
      <path d="M9 19h6" />
      <path d="M10 22h4" />
    </svg>
  );
}

/** 밴 */
export function BanIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <circle cx="12" cy="12" r="10"/>
      <line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/>
    </svg>
  );
}

/** 듀오 */
export function HandshakeIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="m11 17 2 2a1 1 0 1 0 3-3" />
      <path d="m14 14 2.5 2.5a1 1 0 1 0 3-3l-3.88-3.88a3 3 0 0 0-4.24 0l-.88.88a1 1 0 1 1-1.42-1.42l4.88-4.88a1 1 0 0 1 1.42 0l4.59 4.59a2 2 0 0 1 0 2.82l-5 5a2 2 0 0 1-2.83 0" />
      <path d="m7 21 1.5-1.5" />
      <path d="m3 17 4-4" />
      <path d="m2 13 6-6" />
    </svg>
  );
}

/** 부진 */
export function FrownIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <circle cx="12" cy="12" r="10" />
      <path d="M16 16s-1.5-2-4-2-4 2-4 2" />
      <line x1="9" x2="9.01" y1="9" y2="9" />
      <line x1="15" x2="15.01" y1="9" y2="9" />
    </svg>
  );
}

/** 활약 */
export function HeroIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <circle cx="14" cy="4" r="2"/>
      <path d="M14 8v6"/>
      <path d="m11 21 3-7 3 7"/>
      <path d="m11.5 12.5-2.5-2 3-2.5h4l3 2.5-2.5 2"/>
      <path d="M14 8c-5-1-9 .5-12 4 1.5 3.5 2 6 4 8 2.5-1 4.5-2 6-4"/>
    </svg>
  );
}

/** 시간 */
export function TimerIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <circle cx="12" cy="14" r="8" />
      <line x1="12" y1="2" x2="12" y2="6" />
      <line x1="10" y1="2" x2="14" y2="2" />
      <line x1="12" y1="14" x2="15" y2="11" />
    </svg>
  );
}

/** 폭발·딜 */
export function BurstIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="m12 2 2.5 5.5L21 5l-4 6 5 2-5.5 3 2 5.5-4.5-3.5L10 22l-2.5-5.5L3 17.5l3-5L2 9l5.5-2.5Z"/>
    </svg>
  );
}

/** 관측·와드 */
export function TelescopeIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M10.5 13 21 6l-2-3-10.5 7Z"/>
      <path d="M9.5 11.5 5 14.5"/>
      <path d="m4 13 2 3"/>
      <path d="m5 21 7-9 7 9"/>
      <path d="M12 12v9"/>
    </svg>
  );
}

/** CC */
export function SnowflakeIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M12 2v20M3.5 7l17 10M3.5 17l17-10"/>
      <path d="m8.5 5 3.5 2 3.5-2M8.5 19l3.5-2 3.5 2"/>
      <path d="M4 11.5 7.5 9.5V5.5M4 12.5 7.5 14.5v4M20 11.5 16.5 9.5V5.5M20 12.5 16.5 14.5v4"/>
    </svg>
  );
}

/** 킬 */
export function DaggerIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M9 14V9l3-7 3 7v5" />
      <line x1="12" y1="6" x2="12" y2="12" />
      <line x1="5" y1="14" x2="19" y2="14" />
      <line x1="12" y1="14" x2="12" y2="21" />
      <line x1="10" y1="21" x2="14" y2="21" />
    </svg>
  );
}

/** 기간 */
export function CalendarIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <rect width="18" height="18" x="3" y="4" rx="2" />
      <path d="M8 2v4" />
      <path d="M16 2v4" />
      <path d="M3 10h18" />
    </svg>
  );
}

/** 시각 */
export function ClockIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <circle cx="12" cy="12" r="10" />
      <polyline points="12 6 12 12 16 14" />
    </svg>
  );
}

/** 체급 */
export function MuscleIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M 4 20 h 13 a 4 4 0 0 0 4 -4 V 7 a 3 3 0 0 0 -3 -3 c -1.5 0 -3 0.8 -3.5 2 c -1.2 0.5 -1.8 1.8 -1.5 3 c 0.4 0.8 1.2 1 2 0.5 V 14 C 13 9 8.5 8 6 13 C 3.5 13 2 15 2 17.5 c 0 1.2 0.8 2.5 2 2.5 Z" />
      <path d="M 9 13.5 c 1 -1.5 2.5 -1.5 3.5 0" />
    </svg>
  );
}

/** 정렬 */
export function SortToggleIcon(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="m7 9 5-5 5 5" />
      <path d="m7 15 5 5 5-5" />
    </svg>
  );
}
