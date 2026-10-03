/** 플레이어 축 통계 — `/api/stats`, `/api/stats/player/*`, `/api/stats/compare`. */

import type { AxiosRequestConfig } from 'axios';
import { api } from '@/api/client';
import type { MaybePosition as Position } from '@/lib/position';
import type { GameMode } from '@/types';

export function getStats(mode?: GameMode, config?: AxiosRequestConfig) {
	return api.get<StatsResponse>('/api/stats', { params: { mode }, ...config });
}

export function getPlayerStats(riotId: string, mode?: GameMode, config?: AxiosRequestConfig) {
	return api.get<PlayerDetailStats>(`/api/stats/player/${encodeURIComponent(riotId)}`, {
		params: { mode },
		...config
	});
}

export function getPlayerComparison(
	player1: string,
	player2: string,
	mode: GameMode,
	config?: AxiosRequestConfig
) {
	return api.get<PlayerComparisonResult>('/api/stats/compare', {
		params: { player1, player2, mode },
		...config
	});
}

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
