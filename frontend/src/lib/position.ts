/**
 * 포지션 표기의 단일 출처.
 *
 * 지금까지 프론트에 표기가 셋 섞여 있었다.
 *   Riot 원본        TOP JUNGLE MIDDLE BOTTOM UTILITY   (MatchesPage, MatchDetailPage, ChampionPage, SummonerPage)
 *   절반만 옮긴 것    TOP JUNGLE MID    BOTTOM SUPPORT   (AdminPage, LaneTab, PositionPoolTab)
 *   백엔드 실제값     TOP JUNGLE MID    ADC    SUPPORT   ← 이것만 맞다
 *
 * 그래서 경기 목록에서 MID·ADC·SUPPORT 의 indexOf 가 전부 -1 이 됐고, 정렬 비교자가 -1 을
 * 뒤로 보내면서 탑·정글만 제자리에 서고 나머지 셋은 순서 없이 밀려났다.
 * BOTTOM 은 세 표기 어디서도 백엔드와 맞지 않아 여덟 군데 전부 틀려 있었다.
 *
 * 백엔드 com.gijun.main.domain.model.match.Position 을 그대로 따른다. 여기만 고치면 된다.
 */

export const POSITIONS = ['TOP', 'JUNGLE', 'MID', 'ADC', 'SUPPORT'] as const;

export type Position = (typeof POSITIONS)[number];
/** 배정되지 않은 참가자는 빈 문자열로 온다. */
export type MaybePosition = Position | '';

export const POSITION_LABEL: Record<Position, string> = {
  TOP: '탑',
  JUNGLE: '정글',
  MID: '미드',
  ADC: '원딜',
  SUPPORT: '서포터',
};

/** 화면 표기. 배정이 없으면 '-'. */
export function positionLabel(pos: string | null | undefined): string {
  return POSITION_LABEL[pos as Position] ?? '-';
}

/** 정렬용 순서. 배정이 없는 참가자는 맨 뒤로 보낸다. */
export function positionOrder(pos: string | null | undefined): number {
  const i = POSITIONS.indexOf(pos as Position);
  return i === -1 ? POSITIONS.length : i;
}

/** 탑 → 정글 → 미드 → 원딜 → 서포터 순으로 세우는 비교자. */
export function byPosition<T extends { assignedPosition?: string | null }>(a: T, b: T): number {
  return positionOrder(a.assignedPosition) - positionOrder(b.assignedPosition);
}
