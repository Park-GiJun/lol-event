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
}
