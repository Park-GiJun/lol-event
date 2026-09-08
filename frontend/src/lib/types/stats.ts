import type { Position } from "@/lib/position";

/**
 * 표본 신뢰 등급. 내전 데이터는 표본이 금방 한 자리 수로 떨어져서,
 * 승률 같은 비율 지표는 이 등급과 경기 수를 같이 보여주지 않으면 거짓말이 된다.
 */
export type SampleGrade = "HIGH" | "MEDIUM" | "LOW" | "INSUFFICIENT";

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
  /** 같은 포지션 상대와 비교한 라인전 점수(0~1, 0.5가 호각). 같은 승리에서 변동폭이 갈리는 근거다. */
  lanePerformance: number;
  gameCreation: number;
}

export interface PlayerEloHistoryResult {
  riotId: string;
  currentElo: number;
  eloRank: number | null;
  history: EloHistoryEntry[];
}

export interface EloRankEntry {
  /** 배치 중인 플레이어는 0. 순위를 매기지 않는다. */
  rank: number;
  riotId: string;
  elo: number;
  games: number;
  wins: number;
  losses: number;
  winRate: number;
  winStreak: number;
  lossStreak: number;
  /** 최소 경기 수 미달. 목록에는 남기되 순위에서는 뺀다. */
  placement: boolean;
  sampleGrade: SampleGrade;
}

export interface EloLeaderboardResult {
  /** 순위가 매겨진 플레이어가 먼저, 배치 중인 플레이어가 뒤에 온다. */
  players: EloRankEntry[];
  minGames: number;
  rankedCount: number;
  placementCount: number;
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

export interface ChampionRuneStat {
  /** 핵심 룬(키스톤) id. */
  keystone: number;
  /** 주 계열 id (정밀 8000 / 지배 8100 / 마법 8200 / 영감 8300 / 결의 8400). */
  primaryStyle: number;
  /** 보조 계열 id. */
  subStyle: number;
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
  /** 룬 정보가 실려 오지 않은 경기만 있으면 빈 배열이다. */
  runeStats: ChampionRuneStat[];
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
  /** 관측 승률. 항상 games 와 같이 보여줄 것. */
  winRate: number;
  /** 전체 평균 쪽으로 끌어당긴 승률. 정렬은 이 값으로 한다. */
  adjustedWinRate: number;
  sampleGrade: SampleGrade;
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

// ── 2차 신규 리포트 타입 ──────────────────────────────────

// 플레이어 비교
export interface PlayerStatSnapshot {
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
export interface PlayerComparisonResult {
  player1: string;
  player2: string;
  togetherGames: number;
  togetherWinRate: number;
  p1TogetherStats: PlayerStatSnapshot | null;
  p2TogetherStats: PlayerStatSnapshot | null;
  versusGames: number;
  player1VsWinRate: number;
  p1VersusStats: PlayerStatSnapshot | null;
  p2VersusStats: PlayerStatSnapshot | null;
  overallP1Stats: PlayerStatSnapshot;
  overallP2Stats: PlayerStatSnapshot;
}

// 세션 분석
export interface SessionEntry {
  date: string;
  games: number;
  totalDurationMin: number;
  sessionMvp: string | null;
  sessionMvpKda: number;
  team100Wins: number;
  team200Wins: number;
  totalKills: number;
  pentaKills: number;
  participants: string[];
}
export interface SessionReportResult {
  sessions: SessionEntry[];
  totalSessions: number;
}

// 챔피언 티어리스트
export interface ChampionTierEntry {
  champion: string;
  championId: number;
  tier: string;
  tierScore: number;
  games: number;
  /** 관측 승률. 항상 games 와 같이 보여줄 것. */
  winRate: number;
  /** 전체 평균 쪽으로 끌어당긴 승률. 정렬은 이 값으로 한다. */
  adjustedWinRate: number;
  sampleGrade: SampleGrade;
  kda: number;
  pickRate: number;
  avgDamage: number;
}
export interface ChampionTierResult {
  tierList: ChampionTierEntry[];
  byTier: Record<string, ChampionTierEntry[]>;
  totalMatches: number;
}

// 게임 길이별 성향
export interface GameLengthBucket {
  games: number;
  wins: number;
  winRate: number;
  avgKills: number;
  avgDeaths: number;
  avgDamage: number;
  avgCsPerMin: number;
}
export interface GameLengthTendencyEntry {
  riotId: string;
  totalGames: number;
  shortGame: GameLengthBucket;
  midGame: GameLengthBucket;
  longGame: GameLengthBucket;
  tendency: string;
}
export interface ChampionLengthTendency {
  champion: string;
  championId: number;
  shortWinRate: number;
  midWinRate: number;
  longWinRate: number;
  bestLength: string;
}
export interface GameLengthTendencyResult {
  players: GameLengthTendencyEntry[];
  championTendencies: ChampionLengthTendency[];
}

// 초반 지배력
export interface EarlyGameDominanceEntry {
  riotId: string;
  games: number;
  firstBloodRate: number;
  firstTowerRate: number;
  earlyGameScore: number;
  firstBloodWinRate: number;
  noFirstBloodWinRate: number;
  badges: string[];
}
export interface EarlyGameDominanceResult {
  rankings: EarlyGameDominanceEntry[];
  firstBloodKing: string | null;
  towerDestroyer: string | null;
  overallFirstBloodWinRate: number;
  overallFirstTowerWinRate: number;
}

// 컴백 지수
export interface ComebackIndexEntry {
  riotId: string;
  totalGames: number;
  totalWinRate: number;
  contestGames: number;
  contestWinRate: number;
  surrenderGames: number;
  surrenderWinRate: number;
  comebackBonus: number;
  isKing: boolean;
}
export interface ComebackMatchEntry {
  matchId: string;
  gameCreation: number;
  gameDurationMin: number;
  winnerParticipants: string[];
}
export interface ComebackIndexResult {
  rankings: ComebackIndexEntry[];
  comebackKing: string | null;
  topComebackMatches: ComebackMatchEntry[];
}

// 골드 효율
export interface GoldEfficiencyEntry {
  riotId: string;
  games: number;
  avgDmgPerGold: number;
  avgVisionPerGold: number;
  avgObjPerGold: number;
  avgCsPerGold: number;
  goldEfficiencyScore: number;
  tags: string[];
}
export interface GoldEfficiencyResult {
  rankings: GoldEfficiencyEntry[];
  dmgEfficiencyKing: string | null;
  visionEfficiencyKing: string | null;
  csEfficiencyKing: string | null;
  objEfficiencyKing: string | null;
}

// ── 데미지/시야/서렌더/후반 분석 ──────────────────────────────────

// 데미지 분석
export interface DamagePlayerEntry {
  riotId: string;
  games: number;
  avgTotalDamage: number;
  avgPhysicalDamage: number;
  avgMagicDamage: number;
  avgTrueDamage: number;
  physicalRatio: number;
  magicRatio: number;
  trueRatio: number;
  damageProfile: 'AD' | 'AP' | 'Hybrid' | 'Tank' | 'Unknown';
  avgDamageTaken: number;
  avgTurretDamage: number;
}
/** 백엔드는 players 로 내려준다. 예전 타입은 rankings 라 undefined.map 으로 터졌다. */
export interface DamageAnalysisResult {
  players: DamagePlayerEntry[];
}

// 시야 지배
export interface VisionPlayerEntry {
  riotId: string;
  games: number;
  avgVisionScore: number;
  avgWardsPlaced: number;
  avgWardsKilled: number;
  avgControlWardsBought: number;
  visionIndex: number;
}
/** 백엔드는 players 만 내려준다. visionKing 은 목록에서 뽑는다. */
export interface VisionDominanceResult {
  players: VisionPlayerEntry[];
}

// 서렌더 분석
export interface SurrenderPlayerEntry {
  riotId: string;
  games: number;
  surrenderGames: number;
  earlySurrenderGames: number;
  causedEarlySurrenderGames: number;
  surrenderRate: number;
  earlySurrenderRate: number;
}
export interface SurrenderAnalysisResult {
  totalGames: number;
  surrenderGames: number;
  earlySurrenderGames: number;
  overallSurrenderRate: number;
  overallEarlySurrenderRate: number;
  players: SurrenderPlayerEntry[];
}

// 후반 지배
export interface LateGamePlayerEntry {
  riotId: string;
  games: number;
  avgInhibitorKills: number;
  avgSurvivalSeconds: number;
  avgKillingSpree: number;
  lateGameScore: number;
  longestKillingSpree: number;
  topChampion: string | null;
  topChampionId: number | null;
}
/** 백엔드는 players 만 내려준다. lateGameKing 은 목록에서 뽑는다. */
export interface LateGameResult {
  players: LateGamePlayerEntry[];
}

// ── 신규 추가 4종 ──────────────────────────────────────────────

// 밴 분석
export interface BanEntry {
  champion: string;
  championId: number;
  banCount: number;
  banRate: number;
}
export interface BanAnalysisResult {
  topBanned: BanEntry[];
  totalGamesAnalyzed: number;
  mostBannedChampion: string | null;
}

// 요일/시간대 분석
export interface DayPatternEntry {
  dayOfWeek: number;
  dayName: string;
  sessions: number;
  games: number;
  winRate: number;
}
export interface HourPatternEntry {
  hour: number;
  games: number;
  winRate: number;
}
export interface TimePatternResult {
  byDay: DayPatternEntry[];
  byHour: HourPatternEntry[];
  busiestDay: string | null;
  busiestHour: number | null;
  totalGames: number;
}

// 킬 가담률
export interface KillParticipationEntry {
  riotId: string;
  games: number;
  avgKp: number;
  avgKpWin: number;
  avgKpLoss: number;
  avgKills: number;
  avgAssists: number;
}
export interface KillParticipationResult {
  rankings: KillParticipationEntry[];
  kpKing: string | null;
}

// 포지션별 챔피언 풀
export interface PositionChampEntry {
  champion: string;
  championId: number;
  games: number;
  winRate: number;
  kda: number;
}
export interface PlayerPositionEntry {
  riotId: string;
  position: string;
  games: number;
  winRate: number;
  topChampion: string | null;
  topChampionId: number | null;
  champions: PositionChampEntry[];
}
export interface PositionChampionPoolResult {
  allPlayers: PlayerPositionEntry[];
}

// 챔피언 상성 (백엔드 ChampionMatchupResult)

/** 같은 라인 상대와의 평균 격차. 양수면 이 챔피언이 앞선다. */
export interface LaneGap {
  goldDiff: number;
  csDiff: number;
  damageDiff: number;
  killDiff: number;
  visionDiff: number;
}

/**
 * 챔피언 x 라인 단위 라인전 지표.
 * 개별 상성은 표본이 중앙 1경기라 못 읽는다. 축을 하나 위로 올린 값이다.
 */
export interface ChampionLaneStrength {
  champion: string;
  championId: number;
  position: string;
  games: number;
  wins: number;
  winRate: number;
  adjustedWinRate: number;
  sampleGrade: SampleGrade;
  gap: LaneGap;
}

/** 개별 상성. 최소 표본을 넘긴 것만 내려온다. */
export interface MatchupStat {
  opponent: string;
  opponentId: number;
  position: string;
  games: number;
  wins: number;
  winRate: number;
  adjustedWinRate: number;
  sampleGrade: SampleGrade;
  gap: LaneGap;
}

export interface ChampionMatchupResult {
  champion: string;
  championId: number;
  laneStrength: ChampionLaneStrength[];
  matchups: MatchupStat[];
  /** 개별 상성에 적용된 최소 표본. 화면에 밝힌다. */
  minGames: number;
}
