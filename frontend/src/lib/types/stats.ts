export interface ChampionCount {
  champ: string;
  count: number;
}

export interface PlayerStats {
  riotId: string;
  games: number;
  wins: number;
  losses: number;
  winRate: number;
  avgKills: number;
  avgDeaths: number;
  avgAssists: number;
  kda: number;
  avgDamage: number;
  avgCs: number;
  avgGold: number;
  avgVisionScore: number;
  topChampions: ChampionCount[];
}

export interface StatsResponse {
  stats: PlayerStats[];
  matchCount: number;
}

export interface ChampionStat {
  champion: string;
  championId: number;
  games: number;
  wins: number;
  winRate: number;
  avgKills: number;
  avgDeaths: number;
  avgAssists: number;
  kda: number;
  avgDamage: number;
  avgCs: number;
  avgGold: number;
}

export interface RecentMatchStat {
  matchId: string;
  champion: string;
  championId: number;
  win: boolean;
  kills: number;
  deaths: number;
  assists: number;
  damage: number;
  cs: number;
  gold: number;
  gameCreation: number;
  gameDuration: number;
  queueId: number;
}

export interface LaneStat {
  position: 'TOP' | 'JUNGLE' | 'MID' | 'BOTTOM' | 'SUPPORT';
  games: number;
  wins: number;
  winRate: number;
  avgKills: number;
  avgDeaths: number;
  avgAssists: number;
  kda: number;
  avgDamage: number;
  avgCs: number;
  avgGold: number;
  avgVisionScore: number;
  avgDamageTaken: number;
  avgObjectiveDamage: number;
  avgWardsPlaced: number;
  avgCcTime: number;
  avgNeutralMinions: number;
}

export interface PlayerDetailStats {
  riotId: string;
  games: number;
  wins: number;
  losses: number;
  winRate: number;
  avgKills: number;
  avgDeaths: number;
  avgAssists: number;
  kda: number;
  avgDamage: number;
  avgCs: number;
  avgGold: number;
  avgVisionScore: number;
  elo: number;
  eloRank: number | null;
  championStats: ChampionStat[];
  recentMatches: RecentMatchStat[];
  laneStats: LaneStat[];
}

export interface EloHistoryEntry {
  matchId: string;
  eloBefore: number;
  eloAfter: number;
  delta: number;
  win: boolean;
  gameCreation: number;
}

export interface PlayerEloHistoryResult {
  riotId: string;
  currentElo: number;
  eloRank: number | null;
  history: EloHistoryEntry[];
}

export interface EloRankEntry {
  rank: number;
  riotId: string;
  elo: number;
  games: number;
  wins: number;
  losses: number;
  winRate: number;
  winStreak: number;
  lossStreak: number;
}

export interface EloLeaderboardResult {
  players: EloRankEntry[];
}

export interface ChampionPlayerStat {
  riotId: string;
  games: number;
  wins: number;
  winRate: number;
  avgKills: number;
  avgDeaths: number;
  avgAssists: number;
  kda: number;
  avgDamage: number;
  avgCs: number;
  avgGold: number;
  avgVisionScore: number;
}

export interface ChampionItemStat {
  itemId: number;
  picks: number;
  wins: number;
  winRate: number;
}

export interface ChampionLaneStat {
  position: string;
  games: number;
  wins: number;
  winRate: number;
  avgKills: number;
  avgDeaths: number;
  avgAssists: number;
  kda: number;
  avgDamage: number;
  avgCs: number;
  avgGold: number;
}

export interface ChampionDetailStats {
  champion: string;
  championId: number;
  totalGames: number;
  totalWins: number;
  winRate: number;
  players: ChampionPlayerStat[];
  itemStats: ChampionItemStat[];
  laneStats: ChampionLaneStat[];
}

export interface ChampionPickStat {
  champion: string;
  championId: number;
  picks: number;
  wins: number;
  winRate: number;
  kda: number;
  avgKills: number;
  avgDeaths: number;
  avgAssists: number;
  avgDamage: number;
  avgCs: number;
}

export interface PlayerLeaderStat {
  riotId: string;
  displayValue: string;
  games: number;
}

export interface OverviewStats {
  matchCount: number;
  avgGameMinutes: number;
  topPickedChampions: ChampionPickStat[];
  topWinRateChampions: ChampionPickStat[];
  topBannedChampions: ChampionPickStat[];
  winRateLeader: PlayerLeaderStat | null;
  kdaLeader: PlayerLeaderStat | null;
  killsLeader: PlayerLeaderStat | null;
  damageLeader: PlayerLeaderStat | null;
  goldLeader: PlayerLeaderStat | null;
  csLeader: PlayerLeaderStat | null;
  visionLeader: PlayerLeaderStat | null;
  objectiveDamageLeader: PlayerLeaderStat | null;
  turretKillsLeader: PlayerLeaderStat | null;
  pentaKillsLeader: PlayerLeaderStat | null;
  wardsLeader: PlayerLeaderStat | null;
  ccLeader: PlayerLeaderStat | null;
  mostGamesPlayed: PlayerLeaderStat | null;
  firstBloodLeader: PlayerLeaderStat | null;
  totalBaronKills: number;
  totalDragonKills: number;
  totalTowerKills: number;
  totalRiftHeraldKills: number;
  totalInhibitorKills: number;
  totalFirstBloods: number;
  totalCs: number;
}

// ── 고급 통계 타입 ──────────────────────────────────

export interface MvpPlayerStat {
  riotId: string;
  games: number;
  mvpCount: number;
  aceCount: number;
  mvpRate: number;
  avgMvpScore: number;
  topChampion: string | null;
  topChampionId: number | null;
}

export interface MvpStatsResult {
  rankings: MvpPlayerStat[];
  totalGames: number;
}

export interface ChampionSynergy {
  champion1: string;
  champion1Id: number;
  champion2: string;
  champion2Id: number;
  games: number;
  wins: number;
  winRate: number;
  avgCombinedKills: number;
}

export interface ChampionSynergyResult {
  synergies: ChampionSynergy[];
  totalGames: number;
}

export interface DuoStat {
  player1: string;
  player2: string;
  games: number;
  wins: number;
  winRate: number;
  avgKills: number;
  avgDeaths: number;
  avgAssists: number;
  kda: number;
}

export interface DuoStatsResult {
  duos: DuoStat[];
}

export interface PlayerLaneStat {
  riotId: string;
  games: number;
  wins: number;
  winRate: number;
  avgKills: number;
  avgDeaths: number;
  avgAssists: number;
  kda: number;
  avgDamage: number;
  avgCs: number;
  avgGold: number;
  avgVisionScore: number;
  avgDamageTaken: number;
  avgObjectiveDamage: number;
  avgWardsPlaced: number;
  avgCcTime: number;
  avgNeutralMinions: number;
  topChampion: string | null;
  topChampionId: number | null;
}

export interface LaneLeaderboardResult {
  lane: string;
  players: PlayerLaneStat[];
}

export interface StreakResult {
  riotId: string;
  currentStreak: number;
  currentStreakType: 'WIN' | 'LOSS' | 'NONE';
  longestWinStreak: number;
  longestLossStreak: number;
  recentForm: string[];
  totalGames: number;
  wins: number;
  losses: number;
}

// ── 신규 리포트 타입 ──────────────────────────────────

// 주간 어워즈
export interface WeeklyAwardEntry {
  riotId: string;
  displayValue: string;
  games: number;
}
export interface WeeklyAwardsResult {
  mostDeaths: WeeklyAwardEntry | null;
  worstKda: WeeklyAwardEntry | null;
  highGoldLowDamage: WeeklyAwardEntry | null;
  mostSurrenders: WeeklyAwardEntry | null;
  pentaKillHero: WeeklyAwardEntry | null;
  loneHero: WeeklyAwardEntry | null;
  highestWinRate: WeeklyAwardEntry | null;
  mostGamesChampion: WeeklyAwardEntry | null;
}

// 떡락 지수
export interface DefeatContributionEntry {
  riotId: string;
  games: number;
  losses: number;
  avgDefeatScore: number;
  avgDeaths: number;
  avgDamage: number;
  worstMatch: string | null;
}
export interface DefeatContributionResult {
  rankings: DefeatContributionEntry[];
}

// 멀티킬 하이라이트
export interface MultiKillEvent {
  riotId: string;
  champion: string;
  championId: number;
  multiKillType: string;
  matchId: string;
  gameCreation: number;
}
export interface PlayerMultiKillStat {
  riotId: string;
  pentaKills: number;
  quadraKills: number;
  tripleKills: number;
  doubleKills: number;
  topChampion: string | null;
  topChampionId: number | null;
}
export interface MultiKillHighlightsResult {
  pentaKillEvents: MultiKillEvent[];
  recentHighlights: MultiKillEvent[];
  playerRankings: PlayerMultiKillStat[];
}

// 경기 혼돈 지수
export interface ChaosMatchEntry {
  matchId: string;
  gameCreation: number;
  gameDurationMin: number;
  chaosIndex: number;
  totalKills: number;
  killDensity: number;
  multiKillScore: number;
  gameTypeTag: string;
  participants: string[];
}
export interface ChaosMatchResult {
  topChaosMatches: ChaosMatchEntry[];
  topBloodBathMatches: ChaosMatchEntry[];
  topStrategicMatches: ChaosMatchEntry[];
  avgChaosIndex: number;
}

// 성장 곡선
export interface GrowthCurveEntry {
  matchId: string;
  gameCreation: number;
  champion: string;
  win: boolean;
  kda: number;
  dmgShare: number;
  visionPerMin: number;
  csPerMin: number;
  rollingKda: number;
  rollingDmgShare: number;
  rollingCsPerMin: number;
}
export interface GrowthCurveResult {
  riotId: string;
  entries: GrowthCurveEntry[];
  totalGames: number;
  recentAvgKda: number;
  overallAvgKda: number;
  trend: 'IMPROVING' | 'DECLINING' | 'STABLE';
}

// 생존력&탱킹
export interface SurvivalIndexEntry {
  riotId: string;
  games: number;
  avgDamageTaken: number;
  avgSelfMitigated: number;
  avgMitigationRatio: number;
  avgTankShare: number;
  avgSurvivalRatio: number;
  avgDeaths: number;
  survivalIndex: number;
}
export interface SurvivalIndexResult {
  rankings: SurvivalIndexEntry[];
}

// 정글 점령
export interface JungleDominanceEntry {
  riotId: string;
  games: number;
  avgInvadeRatio: number;
  avgObjShare: number;
  avgKp: number;
  avgJungleCs: number;
  avgJungleDominance: number;
  playStyleTag: string;
  topChampion: string | null;
  topChampionId: number | null;
}
export interface JungleDominanceResult {
  rankings: JungleDominanceEntry[];
}

// 힐러/인챈터 기여
export interface SupportImpactEntry {
  riotId: string;
  games: number;
  avgHealShare: number;
  avgCcShare: number;
  avgVisionShare: number;
  avgShieldProxy: number;
  supportImpact: number;
  roleTag: string;
  topChampion: string | null;
  topChampionId: number | null;
}
export interface SupportImpactResult {
  rankings: SupportImpactEntry[];
}

// 숙명의 라이벌
export interface RivalMatchupEntry {
  player1: string;
  player2: string;
  games: number;
  player1Wins: number;
  player2Wins: number;
  player1WinRate: number;
}
export interface RivalMatchupResult {
  rivalries: RivalMatchupEntry[];
  topRivalry: RivalMatchupEntry | null;
}

// 팀 케미스트리
export interface TeamChemistryEntry {
  players: string[];
  games: number;
  wins: number;
  winRate: number;
  compositionSize: number;
}
export interface TeamChemistryResult {
  bestDuos: TeamChemistryEntry[];
  bestTrios: TeamChemistryEntry[];
  bestFullTeams: TeamChemistryEntry[];
  worstDuos: TeamChemistryEntry[];
}

// 포지션 장인 배지
export interface PositionBadgeEntry {
  position: string;
  riotId: string;
  games: number;
  winRate: number;
  kda: number;
  avgDamage: number;
  positionScore: number;
  topChampion: string | null;
  topChampionId: number | null;
}
export interface PositionBadgeResult {
  topPositions: PositionBadgeEntry[];
  allPositionRankings: Record<string, PositionBadgeEntry[]>;
}

// 챔피언 장인 인증서
export interface ChampionCertEntry {
  riotId: string;
  champion: string;
  championId: number;
  games: number;
  wins: number;
  winRate: number;
  avgKills: number;
  avgDeaths: number;
  avgAssists: number;
  kda: number;
  avgDamage: number;
  certified: boolean;
}
export interface ChampionCertificateResult {
  certifiedMasters: ChampionCertEntry[];
  topChampionMasters: Record<string, ChampionCertEntry>;
}

// 플레이스타일 DNA
export interface PlaystyleDnaEntry {
  riotId: string;
  games: number;
  aggression: number;
  durability: number;
  teamPlay: number;
  objectiveFocus: number;
  economy: number;
  visionControl: number;
  styleTag: string;
}
export interface PlaystyleDnaResult {
  players: PlaystyleDnaEntry[];
}

// 내전 메타 변화 추적기
export interface MetaShiftChampion {
  champion: string;
  championId: number;
  totalGames: number;
  pickRate: number;
  recentPickRate: number;
  olderPickRate: number;
  trend: number;
  winRate: number;
  metaTag: string;
}
export interface MetaShiftResult {
  risingChampions: MetaShiftChampion[];
  fallingChampions: MetaShiftChampion[];
  stableTopChampions: MetaShiftChampion[];
  totalMatchesAnalyzed: number;
}
