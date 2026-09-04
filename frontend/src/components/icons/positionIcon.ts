import { LaneTopIcon, LaneJungleIcon, LaneMidIcon, LaneAdcIcon, LaneSupportIcon } from './LolIcons';

/**
 * 포지션 코드 -> 아이콘. 키는 @/lib/position 의 Position 과 같다.
 *
 * 컴포넌트 파일(LolIcons.tsx)이 아니라 여기 있는 이유는 react-refresh 규칙 때문이다.
 * 컴포넌트 파일이 상수도 같이 내보내면 HMR 이 그 파일 전체를 다시 마운트해 상태가 날아간다.
 */
export const POSITION_ICON = {
  TOP: LaneTopIcon,
  JUNGLE: LaneJungleIcon,
  MID: LaneMidIcon,
  ADC: LaneAdcIcon,
  SUPPORT: LaneSupportIcon,
} as const;
