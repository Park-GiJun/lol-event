export interface Participant {
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
