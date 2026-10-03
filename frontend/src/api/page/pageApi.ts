/**
 * 화면 단위 집계.
 *
 * 예전에는 홈이 6번, 소환사 화면이 5번 왕복했고 그중 듀오·라이벌은 전체 조합(각 57KB)을
 * 받아 화면에서 한 명 것만 골라 썼다. 이제 서버가 걸러서 한 번에 준다.
 */

import type { AxiosRequestConfig } from 'axios';
import { api } from '@/api/client';
import type { MaybePosition as Position } from '@/lib/position';
import type { GameMode, SampleGrade } from '@/types';
import type { MatchSummary } from '@/api/match/matchApi';
import type { EloRankEntry } from '@/api/rating/ratingApi';
import type { ChampionDetailStats, ChampionTierEntry } from '@/api/stats/championStatsApi';
import type { OverviewStats, WeeklyAwardsResult } from '@/api/stats/overviewStatsApi';

export function getHome(mode: GameMode, config?: AxiosRequestConfig) {
	return api.get<HomeResult>('/api/home', { params: { mode }, ...config });
}

export function getSummonerProfile(riotId: string, mode: GameMode, config?: AxiosRequestConfig) {
	return api.get<SummonerProfileResult>(`/api/summoner/${encodeURIComponent(riotId)}`, {
		params: { mode },
		...config
	});
}

export function getChampionPage(champion: string, mode: GameMode, config?: AxiosRequestConfig) {
	return api.get<ChampionPageResult>(`/api/champions/${encodeURIComponent(champion)}`, {
		params: { mode },
		...config
	});
}

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
	/** 실력 레이팅 표시값. 리더보드에 찍히는 것과 같은 숫자다. */
	elo: number;
	/** 배치 중이면 null */
	eloRank: number | null;
	/** 순위가 매겨진 전체 인원. "14위 / 41명" 처럼 쓴다. */
	eloRankedTotal: number;
	/** 이 사람의 두 레이팅 한 줄. 한 경기도 반영되지 않았으면 null. */
	rating: EloRankEntry | null;
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

export interface ChampionPageResult {
	detail: ChampionDetailStats;
	/** 표본 미달이면 tier 가 "?" 다. */
	tier: ChampionTierEntry | null;
	laneStrength: ChampionLaneStrength[];
	matchups: MatchupStat[];
	matchupMinGames: number;
}
