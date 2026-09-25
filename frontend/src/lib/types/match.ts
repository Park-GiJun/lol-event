import type { MaybePosition as Position } from "@/lib/position";
export interface Participant {
  /**
   * 포지션 재배정 백필 결과.
   * Riot 원본 lane/role 은 5v5 내전에서 심하게 왜곡돼 있으니(정글 464 / 탑 156)
   * 포지션 표기는 반드시 이 값을 쓴다.
   */
  assignedPosition: Position;
  puuid: string | null;
  riotId: string;
  champion: string;
  championId: number;
  team: 'blue' | 'red';
  teamId: number;
  spell1Id: number;
  spell2Id: number;
  win: boolean;
  // 요약
  kills: number;
  deaths: number;
  assists: number;
  damage: number;
  cs: number;
  gold: number;
  visionScore: number;
  champLevel: number;
  // 멀티킬
  doubleKills: number;
  tripleKills: number;
  quadraKills: number;
  pentaKills: number;
  killingSprees: number;
  largestKillingSpree: number;
  largestMultiKill: number;
  largestCriticalStrike: number;
  longestTimeSpentLiving: number;
  // 퍼스트
  firstBloodKill: boolean;
  firstBloodAssist: boolean;
  firstTowerKill: boolean;
  firstTowerAssist: boolean;
  firstInhibitorKill: boolean;
  firstInhibitorAssist: boolean;
  inhibitorKills: number;
  turretKills: number;
  // 와드
  wardsKilled: number;
  wardsPlaced: number;
  sightWardsBoughtInGame: number;
  visionWardsBoughtInGame: number;
  // 아이템
  item0: number; item1: number; item2: number; item3: number;
  item4: number; item5: number; item6: number;
  // 룬
  perk0: number; perk0Var1: number; perk0Var2: number; perk0Var3: number;
  perk1: number; perk1Var1: number; perk1Var2: number; perk1Var3: number;
  perk2: number; perk2Var1: number; perk2Var2: number; perk2Var3: number;
  perk3: number; perk3Var1: number; perk3Var2: number; perk3Var3: number;
  perk4: number; perk4Var1: number; perk4Var2: number; perk4Var3: number;
  perk5: number; perk5Var1: number; perk5Var2: number; perk5Var3: number;
  perkPrimaryStyle: number;
  perkSubStyle: number;
  // 딜
  magicDamageDealt: number;
  magicDamageDealtToChampions: number;
  magicalDamageTaken: number;
  physicalDamageDealt: number;
  physicalDamageDealtToChampions: number;
  physicalDamageTaken: number;
  trueDamageDealt: number;
  trueDamageDealtToChampions: number;
  trueDamageTaken: number;
  totalDamageDealt: number;
  totalDamageDealtToChampions: number;
  totalDamageTaken: number;
  damageDealtToObjectives: number;
  damageDealtToTurrets: number;
  damageSelfMitigated: number;
  // 힐/CC
  totalHeal: number;
  totalUnitsHealed: number;
  timeCCingOthers: number;
  totalTimeCrowdControlDealt: number;
  // 미니언
  neutralMinionsKilled: number;
  neutralMinionsKilledEnemyJungle: number;
  /**
   * 응답에는 더 있지만 여기서 뺀 필드들:
   *   combatPlayerScore, objectivePlayerScore, totalPlayerScore, totalScoreRank
   *     — Riot Match-V4 시절 스코어. 전 행이 0 이다(수집분 190행 전수 확인).
   *   unrealKills — 펜타 초과 킬. 전 행이 0.
   *   neutralMinionsKilledTeamJungle — 아군 정글 몹. 전 행이 0.
   * 타입에 남겨 두면 화면에서 집어 쓰게 되고, 그러면 전원 0 인 칸이 생긴다.
   * 나중에 실제로 채워지기 시작하면 그때 다시 넣으면 된다.
   */
  // 항복
  gameEndedInSurrender: boolean;
  gameEndedInEarlySurrender: boolean;
  causedEarlySurrender: boolean;
  earlySurrenderAccomplice: boolean;
  teamEarlySurrendered: boolean;
  // 증강
  playerAugment1: number;
  playerAugment2: number;
  playerAugment3: number;
  playerAugment4: number;
  playerAugment5: number;
  playerAugment6: number;
  playerSubteamId: number;
  subteamPlacement: number;
  roleBoundItem: number;
  // 포지션
  lane: string | null;
  role: string | null;
}

export interface Team {
  teamId: number;
  win: boolean;
  baronKills: number;
  dragonKills: number;
  towerKills: number;
  inhibitorKills: number;
  riftHeraldKills: number;
  hordeKills: number;
  firstBlood: boolean;
  firstTower: boolean;
  firstBaron: boolean;
  firstInhibitor: boolean;
  firstDragon: boolean;
}

/** 백엔드 Position enum 을 그대로 따른다. 정의는 @/lib/position 한 곳에만 둔다. */
export type { Position };

export interface Match {
  matchId: string;
  queueId: number;
  gameCreation: number;
  gameDuration: number;
  gameMode: string;
  gameType: string;
  gameVersion: string;
  mapId: number;
  seasonId: number;
  platformId: string;
  participants: Participant[];
  teams: Team[];
}

export interface SaveMatchesResponse {
  saved: number;
  skipped: number;
  total: number;
}

// ── 목록 화면 전용 요약 타입 ────────────────────────────────────
// 상세 Participant 는 114개 필드라 목록에서 그대로 받으면 154경기에 3.6MB가 된다.

export interface ParticipantSummary {
  puuid: string | null;
  riotId: string;
  champion: string;
  championId: number;
  team: "blue" | "red";
  teamId: number;
  win: boolean;
  kills: number;
  deaths: number;
  assists: number;
  damage: number;
  cs: number;
  gold: number;
  visionScore: number;
  champLevel: number;
  spell1Id: number;
  spell2Id: number;
  perkPrimaryStyle: number;
  perkSubStyle: number;
  perk0: number;
  item0: number; item1: number; item2: number;
  item3: number; item4: number; item5: number; item6: number;
  assignedPosition: Position;
}

export interface TeamSummary {
  teamId: number;
  win: boolean;
  baronKills: number;
  dragonKills: number;
  towerKills: number;
}

export interface MatchSummary {
  matchId: string;
  queueId: number;
  gameCreation: number;
  gameDuration: number;
  gameMode: string | null;
  participants: ParticipantSummary[];
  teams: TeamSummary[];
}

export interface MatchPage {
  matches: MatchSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}

// ──────────────── 경기 타임라인 ────────────────
//
// GET /api/matches/{matchId}/timeline
//
// 단위 규약
//  - 시간은 ms. `...ByMinute` 목록은 **index = 분**이다.
//  - 좌표는 0~14,870 (협곡 미니맵 스케일). y 는 위로 갈수록 커진다 — 그리려면 뒤집어야 한다.
//  - 비율은 0~100.
//
// 응답이 100KB 급이라 경기 상세 화면에서만 부른다. 목록에는 싣지 않는다.

export interface MapPointDto {
  x: number;
  y: number;
}

export interface MatchTimeline {
  matchId: string;
  /**
   * false 면 아래 목록이 전부 비어 있다. 404 가 아닌 것은 의도된 것이다 —
   * 타임라인은 새 수집기로 받은 경기에만 있고 그 이전 경기는 영구히 없다.
   */
  hasTimeline: boolean;
  durationMs: number;
  /** 프레임이 있는 마지막 분. 곡선의 x축 끝이다. */
  lastMinute: number;
  teams: TeamTimelineSeries[];
  /** 블루 − 레드. index = 분. */
  teamGoldDiffByMinute: number[];
  participants: ParticipantTimelineSeries[];
  kills: KillEntry[];
  objectives: ObjectiveEntry[];
  teamFights: TeamFightEntry[];
}

export interface TeamTimelineSeries {
  teamId: number;
  win: boolean;
  goldByMinute: number[];
  xpByMinute: number[];
  csByMinute: number[];
  /** 작을수록 뭉쳐 있다. 자기 기지 안의 프레임은 빠진다. */
  spreadByMinute: (number | null)[];
  avgSpread: number | null;
}

export interface ParticipantTimelineSeries {
  /** 타임라인 프레임의 키(1~10). */
  participantId: number;
  riotId: string;
  champion: string;
  championId: number;
  teamId: number;
  position: string;
  win: boolean;
  goldByMinute: number[];
  xpByMinute: number[];
  csByMinute: number[];
  levelByMinute: number[];
  /** 손에 든 골드. 뚝 떨어지는 분이 아이템을 산 시점이다. */
  currentGoldByMinute: number[];
  /** 라인 상대가 없었으면 빈 목록. */
  goldDiffByMinute: number[];
  /** index = 분. null 은 그 프레임에 좌표가 없었다는 뜻이다. */
  positionsByMinute: (MapPointDto | null)[];
  laneShareRate: number | null;
  roamRate: number | null;
  enemyHalfRate: number;
  counterJungleRate: number;
  framesSampled: number;
  goldDiff15: number | null;
  csDiff15: number | null;
  xpDiff15: number | null;
  earlyKills: number;
  earlyDeaths: number;
  earlyAssists: number;
  soloKills: number;
  firstDeathMs: number | null;
}

export interface KillEntry {
  timestampMs: number;
  minute: number;
  /** 0이면 사람이 아니다 — 포탑이나 미니언이 막타를 쳤다. */
  killerParticipantId: number;
  victimParticipantId: number;
  assistParticipantIds: number[];
  /** 킬을 올린 팀. 희생자의 팀을 뒤집어 구한 값이다. */
  killingTeamId: number | null;
  at: MapPointDto | null;
  region: string | null;
}

/** 오브젝트와 건물을 "먹은 팀" 관점으로 통일한 항목. 백엔드가 건물 팀을 이미 뒤집어 준다. */
export interface ObjectiveEntry {
  timestampMs: number;
  minute: number;
  /** DRAGON, BARON_NASHOR, RIFTHERALD, HORDE, TOWER_BUILDING, INHIBITOR_BUILDING … */
  kind: string;
  /** 드래곤 원소(FIRE_DRAGON 등). 해당 없으면 빈 값. */
  subType: string;
  /** 포탑의 라인(TOP_LANE 등). 해당 없으면 빈 값. */
  lane: string;
  towerType: string;
  killingTeamId: number | null;
  killerParticipantId: number;
  assistParticipantIds: number[];
  at: MapPointDto | null;
}

export interface TeamFightEntry {
  startMs: number;
  endMs: number;
  startMinute: number;
  team100Kills: number;
  team200Kills: number;
  /** 동수면 null — 교환으로 끝난 교전이다. */
  winnerTeamId: number | null;
  openedByTeamId: number | null;
  participantIds: number[];
  at: MapPointDto | null;
  region: string | null;
  /** false 면 킬 3개 미만이다. 솔로킬·2인 교전도 목록에는 들어온다. */
  isTeamFight: boolean;
  /** 교전 직후 넘어간 오브젝트의 종류. */
  objectiveKinds: string[];
}
