import type { Position } from '@/lib/position';

/**
 * 표본 신뢰 등급. 내전 데이터는 표본이 금방 한 자리 수로 떨어져서,
 * 승률 같은 비율 지표는 이 등급과 경기 수를 같이 보여주지 않으면 거짓말이 된다.
 */
export type SampleGrade = 'HIGH' | 'MEDIUM' | 'LOW' | 'INSUFFICIENT';

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

/**
 * `elo*` 는 **실력 레이팅(laneElo)** 이다. 화면이 "Elo" 라고 부르던 자리가 라인 레이팅으로 바뀌었다.
 * 전적 레이팅은 `teamElo*` 로 따로 온다.
 */
export interface EloHistoryEntry {
	matchId: string;
	eloBefore: number;
	eloAfter: number;
	delta: number;
	win: boolean;
	/** 라인 맞대결 결과. NONE 은 대결이 성립하지 않은 경기(칼바람이거나 포지션이 깨진 경기). */
	laneResult: 'WIN' | 'LOSS' | 'NONE';
	laneOpponent: string | null;
	teamEloBefore: number;
	teamEloAfter: number;
	teamDelta: number;
	gameCreation: number;
}

export interface PlayerEloHistoryResult {
	riotId: string;
	currentElo: number;
	eloRank: number | null;
	history: EloHistoryEntry[];
}

/**
 * 리더보드 한 줄. 실력 레이팅(laneElo)과 전적 레이팅(teamElo)은 끝까지 별개다 — 합쳐 쓰지 말 것.
 *
 * 표시·정렬은 `laneEloDisplay`(수축 적용)를 쓴다. 표본이 적은 사람이 과하게 튀는 것을 막는 값이다.
 * 계산에 넣을 일이 있으면 원값(`laneElo`)을 써야 한다.
 */
export interface EloRankEntry {
	/** 배치 중인 플레이어는 0. 순위를 매기지 않는다. */
	rank: number;
	riotId: string;

	/** 실력 레이팅 원값. */
	laneElo: number;
	/** 실력 레이팅 표시값(수축 적용). 화면은 이 값을 쓴다. */
	laneEloDisplay: number;
	laneDuels: number;
	laneWins: number;
	laneLosses: number;
	/** 라인 맞대결 승률(0~1). */
	laneWinRate: number;

	/** 전적 레이팅 원값. */
	teamElo: number;
	teamEloDisplay: number;
	teamGames: number;
	/** 팀 승률(0~1). */
	winRate: number;

	/** `teamElo - laneElo`. 절댓값이 크면 편성자의 평가와 라인 실적이 어긋나 있다는 뜻이다. */
	gap: number;

	/** 라인 맞대결 표본 미달. 목록에는 남기되 순위에서는 뺀다. */
	placement: boolean;
	mainPosition: string | null;

	/** `laneElo` 와 같은 값. */
	elo: number;
	/** `teamGames` 와 같은 값. */
	games: number;
	wins: number;
	losses: number;
	/** 표시 전용. 레이팅 산식에는 쓰이지 않는다. */
	winStreak: number;
	lossStreak: number;
	sampleGrade: SampleGrade;
}

export interface EloLeaderboardResult {
	/** 순위가 매겨진 플레이어가 먼저, 배치 중인 플레이어가 뒤에 온다. */
	players: EloRankEntry[];
	/** 순위에 들어가기 위한 최소 **라인 맞대결 수**. 경기 수가 아니다. */
	minDuels: number;
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
/**
 * 세션 하나. 세션은 **오전 6시에 시작하는 하루**다 — 새벽 경기는 전날 세션에 들어간다.
 * 자정으로 자르면 23:40 에 시작한 판과 00:20 에 끝난 판이 갈려서 그렇게 뒀다.
 */
export interface SessionEntry {
	/** `yyyy-MM-dd`. 세션 상세의 식별자다. */
	date: string;
	/** 그 세션의 경기들. 시작 시각 오름차순. */
	matchIds: string[];
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

export interface SessionMatchEntry {
	matchId: string;
	gameCreation: number;
	durationSec: number;
	winnerTeamId: number | null;
	totalKills: number;
	hasTimeline: boolean;
	/** 스파크라인용. 블루 − 레드, index = 분. 타임라인이 없으면 빈 목록. */
	teamGoldDiffByMinute: number[];
	/** 15분 팀 골드 격차(블루 − 레드). 타임라인이 없으면 null. */
	goldDiffAt15: number | null;
}

export interface SessionPlayerEntry {
	riotId: string;
	games: number;
	wins: number;
	kills: number;
	deaths: number;
	assists: number;
	kda: number;
	/** 타임라인이 있는 경기만. 없으면 null. */
	avgGoldDiff15: number | null;
	laneGames: number;
	earlyKills: number;
	earlyDeaths: number;
	soloKills: number;
}

export interface SessionStreak {
	riotId: string;
	length: number;
}

export interface SessionDetailResult {
	date: string;
	games: number;
	/** 그중 타임라인이 있는 경기 수. 아래 타임라인 수치 전부의 분모다. */
	timelineGames: number;
	firstGameAt: number;
	lastGameAt: number;
	totalDurationMin: number;
	totalKills: number;
	pentaKills: number;
	team100Wins: number;
	team200Wins: number;
	matches: SessionMatchEntry[];
	players: SessionPlayerEntry[];
	teamFights: number;
	team100FightWins: number;
	team200FightWins: number;
	/** 팀이 매 판 바뀌므로 **사람** 기준이다. 2연승부터 기록으로 본다. */
	longestWinStreak: SessionStreak | null;
	longestLossStreak: SessionStreak | null;
	/** 15분에 가장 크게 뒤지고도 이긴 경기. */
	biggestComeback: SessionMatchEntry | null;
	longestGame: SessionMatchEntry | null;
	shortestGame: SessionMatchEntry | null;
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

// ────────── 타임라인 지표 ──────────
//
// 타임라인은 새 수집기로 받은 경기에만 있다. 모집단이 전체 경기보다 훨씬 작으니 경기 수를 늘 같이 보여 준다.
// 비율(…Rate)은 0~100, 격차(…Diff)는 "나 − 같은 자리 상대"다.
// 선수·포지션·챔피언 어느 단위로 묶든 같은 TimelineAverages 를 쓴다.

export interface TimelineAverages {
	/** 타임라인이 있는 경기 수. */
	games: number;
	/** 그중 라인 상대가 있어 격차를 잴 수 있었던 경기 수. */
	laneGames: number;
	winRate: number;
	avgGoldDiff15: number | null;
	avgCsDiff15: number | null;
	avgXpDiff15: number | null;
	/** 15분 골드가 라인 상대보다 앞선 경기 비율. */
	laneLeadRate: number | null;
	/** 15분에 라인 상대보다 앞섰던 경기의 승률. */
	leadWinRate: number | null;
	leadGames: number;
	avgCsAt10: number | null;
	avgGoldAt15: number | null;
	avgEarlyKills: number;
	avgEarlyDeaths: number;
	avgEarlyAssists: number;
	avgSoloKills: number;
	firstBloodRate: number;
	avgFirstDeathMinute: number | null;
}

export interface TimelinePlayerEntry {
	riotId: string;
	/** 전체 표에서는 가장 많이 선 포지션, 라인별 표에서는 그 라인. */
	position: string | null;
	stats: TimelineAverages;
}

export interface TimelinePositionEntry {
	position: string;
	stats: TimelineAverages;
}

export interface TimelineChampionEntry {
	champion: string;
	championId: number;
	stats: TimelineAverages;
	/** 경기 수 내림차순. */
	byPosition: TimelinePositionEntry[];
}

export interface TimelineStatsResult {
	games: number;
	/** 15분 골드가 앞선 팀의 승률. */
	goldLeadWinRate: number | null;
	goldLeadGames: number;
	/** 15분에 1,500골드 이상 뒤지고도 이긴 경기 수. */
	comebackGames: number;
	avgTeamGoldGapAt15: number;
	players: TimelinePlayerEntry[];
	/** TOP → SUPPORT 순. */
	positions: TimelinePositionEntry[];
}

export interface TimelineLaneResult {
	position: string;
	summary: TimelineAverages | null;
	players: TimelinePlayerEntry[];
}

export interface TimelineChampionsResult {
	games: number;
	champions: TimelineChampionEntry[];
	/** 분별 평균 골드 격차. **champion 을 지정해 부를 때만 채워진다.** */
	curve: GoldDiffPoint[];
	/** 그 챔피언이 나온 경기. 최신순. 위와 같은 조건이다. */
	matches: TimelineChampionGame[];
}

/**
 * 챔피언이 나온 한 경기.
 *
 * 표본이 적을 때 평균을 억지로 내지 않고 판을 그대로 늘어놓기 위한 것이다 — 챔피언 하나는
 * 보통 한두 판이라, 평균보다 "그 판이 어땠나"가 정직하다.
 */
export interface TimelineChampionGame {
	matchId: string;
	gameCreation: number;
	riotId: string;
	position: string;
	win: boolean;
	goldDiff15: number | null;
	csDiff15: number | null;
	opponentChampion: string | null;
	opponentChampionId: number | null;
	/** index = 분. 라인 상대가 없었으면 빈 목록. */
	goldDiffByMinute: number[];
}

export interface GoldDiffPoint {
	minute: number;
	avgGoldDiff: number;
	games: number;
}

export interface PlayerTimelineGame {
	matchId: string;
	gameCreation: number;
	champion: string;
	championId: number;
	position: string;
	win: boolean;
	opponentRiotId: string | null;
	opponentChampion: string | null;
	opponentChampionId: number | null;
	goldDiff15: number | null;
	csDiff15: number | null;
	xpDiff15: number | null;
	earlyKills: number;
	earlyDeaths: number;
	earlyAssists: number;
	soloKills: number;
	/** index = 분. */
	goldDiffByMinute: number[];
}

export interface PlayerTimelineResult {
	riotId: string;
	summary: TimelineAverages | null;
	goldDiffRank: number | null;
	rankedPlayers: number;
	byPosition: TimelinePositionEntry[];
	byChampion: TimelineChampionEntry[];
	goldDiffCurve: GoldDiffPoint[];
	/** 최신순. */
	games: PlayerTimelineGame[];
	/** 좌표·한타 지표. 배치가 아직 안 돌았으면 null — "집계 대기 중"을 띄운다. */
	positionStats: PlayerPositionStats | null;
	deathHeatmap: HeatmapGrid | null;
}

// ──────────────── 좌표 지표 ────────────────
//
// 위의 15분 격차와 **신뢰도가 다르다.** 프레임은 분당 1점이라 체류 비율은 실제 시간의
// 비율이 아니라 분 경계에 어디 있었는지의 비율이다 — 30초짜리 갱킹은 사라지거나 1분으로
// 과대 집계된다. 반면 킬·데스 좌표는 실제 이벤트 좌표라 정확하다.
// 두 계열을 같은 표에 섞어 놓지 마라.

export interface PlayerPositionStats {
	/** 이 지표들의 모집단. 타임라인이 있는 경기 수다. */
	games: number;
	/** 좌표가 있는 프레임 수(0분 제외). 모든 비율의 분모다. */
	framesSampled: number;
	/** 라인전 동안 자기 라인에 있던 비율(0~100). **정글은 null** (0 이 아니다). */
	laneShareRate: number | null;
	/** 자기 라인도 자기 기지도 아닌 곳에 있던 비율(0~100). 귀환은 빠진다. 정글은 null. */
	roamRate: number | null;
	enemyHalfRate: number;
	/** 상대 진영 중 정글에 있던 비율(0~100). 카운터 정글의 대리 지표. */
	counterJungleRate: number;
	/** 킬 3개 이상인 교전에 낀 횟수. */
	teamfights: number;
	teamfightKills: number;
	teamfightDeaths: number;
	aggregatedAt: number;
}

/** 히트맵. 좌표를 격자로 접어 담는다. */
export interface HeatmapGrid {
	/** 한 변의 칸 수. 좌표를 비율로 바꿀 때 쓴다. */
	grid: number;
	cells: HeatmapCellEntry[];
	/** null 이면 배치가 아직 안 돌았다. cells 가 빈 것과 구분해야 "집계 대기 중"을 띄운다. */
	aggregatedAt: number | null;
}

export interface HeatmapCellEntry {
	/** EARLY(0~15분) / MID(15~25) / LATE(25+). */
	phase: string;
	x: number;
	y: number;
	count: number;
}
