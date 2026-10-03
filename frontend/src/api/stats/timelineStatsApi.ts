/** 타임라인(15분) 지표. 타임라인이 있는 경기만 대상이다. */

import type { AxiosRequestConfig } from 'axios';
import { api } from '@/api/client';
import type { GameMode } from '@/types';

export function getTimelineStats(mode: GameMode, config?: AxiosRequestConfig) {
	return api.get<TimelineStatsResult>('/api/stats/timeline', { params: { mode }, ...config });
}

/** @param lane TOP / JUNGLE / MID / ADC / SUPPORT */
export function getTimelineLane(lane: string, mode: GameMode, config?: AxiosRequestConfig) {
	return api.get<TimelineLaneResult>('/api/stats/timeline/lane', {
		params: { lane, mode },
		...config
	});
}

/** `champion` 을 주면 그 챔피언만 받는다. */
export function getTimelineChampions(
	mode: GameMode,
	champion?: string,
	config?: AxiosRequestConfig
) {
	return api.get<TimelineChampionsResult>('/api/stats/timeline/champions', {
		params: { mode, champion },
		...config
	});
}

export function getPlayerTimeline(riotId: string, config?: AxiosRequestConfig) {
	return api.get<PlayerTimelineResult>(
		`/api/stats/player/${encodeURIComponent(riotId)}/timeline`,
		config
	);
}

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
