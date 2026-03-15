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
  championStats: ChampionStat[];
  recentMatches: RecentMatchStat[];
  laneStats: LaneStat[];
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

export interface ChampionDetailStats {
  champion: string;
  championId: number;
  totalGames: number;
  totalWins: number;
  winRate: number;
  players: ChampionPlayerStat[];
}

export interface ChampionPickStat {
  champion: string;
  championId: number;
  picks: number;
  wins: number;
  winRate: number;
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
  totalBaronKills: number;
  totalDragonKills: number;
  totalTowerKills: number;
  totalRiftHeraldKills: number;
  totalInhibitorKills: number;
  totalFirstBloods: number;
}
