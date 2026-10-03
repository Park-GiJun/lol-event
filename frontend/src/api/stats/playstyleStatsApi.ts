/** 플레이 성향 통계. */

import type { AxiosRequestConfig } from 'axios';
import { api } from '@/api/client';
import type { GameMode } from '@/types';

export function getPlaystyleDna(mode: GameMode, config?: AxiosRequestConfig) {
	return api.get<PlaystyleDnaResult>('/api/stats/playstyle-dna', { params: { mode }, ...config });
}

export function getPositionBadge(mode: GameMode, config?: AxiosRequestConfig) {
	return api.get<PositionBadgeResult>('/api/stats/position-badge', { params: { mode }, ...config });
}

export function getPositionChampionPool(mode: GameMode, config?: AxiosRequestConfig) {
	return api.get<PositionChampionPoolResult>('/api/stats/position-champion-pool', {
		params: { mode },
		...config
	});
}

export function getSurvivalIndex(mode: GameMode, config?: AxiosRequestConfig) {
	return api.get<SurvivalIndexResult>('/api/stats/survival-index', { params: { mode }, ...config });
}

export function getDefeatContribution(mode: GameMode, config?: AxiosRequestConfig) {
	return api.get<DefeatContributionResult>('/api/stats/defeat-contribution', {
		params: { mode },
		...config
	});
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
