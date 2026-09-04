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
  unrealKills: number;
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
  neutralMinionsKilledTeamJungle: number;
  neutralMinionsKilledEnemyJungle: number;
  // 스코어
  combatPlayerScore: number;
  objectivePlayerScore: number;
  totalPlayerScore: number;
  totalScoreRank: number;
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
