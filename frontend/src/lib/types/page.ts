/**
 * 화면 단위 집계 응답 타입.
 *
 * 지표 단위 엔드포인트(stats/*)는 그대로 남아 분석 탭들이 계속 쓰고,
 * 홈·소환사·챔피언 세 화면만 여기 타입을 쓴다.
 */
import type { MatchSummary, Position } from './match';
import type {
  ChampionDetailStats,
  ChampionTierEntry,
  EloRankEntry,
  MatchupStat,
  OverviewStats,
  SampleGrade,
  WeeklyAwardsResult,
} from './stats';

// ── 홈 ──────────────────────────────────────────────────────────

export interface HomePeriod {
  firstMatchAt: number | null;
  lastMatchAt: number | null;
  totalMatches: number;
  /** 경기에 한 번이라도 등장한 인원. 등록 멤버 수와 다르다. */
  playerCount: number;
}

export interface HomeResult {
  overview: OverviewStats;
  /** 배치 중인 플레이어는 들어가지 않는다. */
  topPlayers: EloRankEntry[];
  /** 표본을 충족한 챔피언만. */
  topChampions: ChampionTierEntry[];
  awards: WeeklyAwardsResult;
  recentMatches: MatchSummary[];
  period: HomePeriod;
}

// ── 소환사 ───────────────────────────────────────────────────────

export interface SummonerProfile {
  riotId: string;
  games: number;
  wins: number;
  losses: number;
  winRate: number;
  adjustedWinRate: number;
  sampleGrade: SampleGrade;
  kda: number;
  avgKills: number;
  avgDeaths: number;
  avgAssists: number;
  avgDamage: number;
  avgCs: number;
  avgGold: number;
  avgVisionScore: number;
  elo: number;
  /** 배치 중이면 null */
  eloRank: number | null;
  /** 순위가 매겨진 전체 인원. "14위 / 41명" 처럼 쓴다. */
  eloRankedTotal: number;
}

export interface SummonerStreak {
  /** 양수 = 연승, 음수 = 연패, 0 = 경기 없음 */
  current: number;
  type: 'WIN' | 'LOSS' | 'NONE';
  longestWin: number;
  longestLoss: number;
  /** 최근 10경기, 최신순 */
  recentForm: ('W' | 'L')[];
}

export interface SummonerTeammate {
  riotId: string;
  games: number;
  wins: number;
  winRate: number;
  adjustedWinRate: number;
  sampleGrade: SampleGrade;
}

export interface SummonerOpponent {
  riotId: string;
  games: number;
  /** 이 소환사가 이긴 횟수 */
  wins: number;
  losses: number;
  winRate: number;
  adjustedWinRate: number;
  sampleGrade: SampleGrade;
}

export interface SummonerPositionStat {
  position: Position;
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

export interface SummonerChampionStat {
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

export interface SummonerRecentMatch {
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

export interface SummonerProfileResult {
  profile: SummonerProfile;
  streak: SummonerStreak;
  championStats: SummonerChampionStat[];
  positionStats: SummonerPositionStat[];
  recentMatches: SummonerRecentMatch[];
  /** 같은 팀으로 함께 뛴 사람들. 함께한 경기 수 순. */
  teammates: SummonerTeammate[];
  /** 상대 팀으로 만난 사람들. 맞붙은 경기 수 순. */
  opponents: SummonerOpponent[];
}

// ── 챔피언 ───────────────────────────────────────────────────────

export interface ChampionPageResult {
  detail: ChampionDetailStats;
  /** 표본 미달이면 tier 가 "?" 다. */
  tier: ChampionTierEntry | null;
  matchups: MatchupStat[];
}
