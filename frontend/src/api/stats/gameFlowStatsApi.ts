/** 경기 흐름 통계. */

import type { AxiosRequestConfig } from 'axios';
import { api } from '@/api/client';
import type { GameMode } from '@/types';

export function getTimePattern(mode: GameMode, config?: AxiosRequestConfig) {
	return api.get<TimePatternResult>('/api/stats/time-pattern', { params: { mode }, ...config });
}

export function getGameLengthTendency(mode: GameMode, config?: AxiosRequestConfig) {
	return api.get<GameLengthTendencyResult>('/api/stats/game-length-tendency', {
		params: { mode },
		...config
	});
}

export function getEarlyGameDominance(mode: GameMode, config?: AxiosRequestConfig) {
	return api.get<EarlyGameDominanceResult>('/api/stats/early-game', {
		params: { mode },
		...config
	});
}

export function getComebackIndex(mode: GameMode, config?: AxiosRequestConfig) {
	return api.get<ComebackIndexResult>('/api/stats/comeback', { params: { mode }, ...config });
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
