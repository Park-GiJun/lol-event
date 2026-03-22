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
