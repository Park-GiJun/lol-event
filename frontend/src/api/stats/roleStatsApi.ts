/** 역할 수행 통계. */

import type { AxiosRequestConfig } from 'axios';
import { api } from '@/api/client';
import type { GameMode } from '@/types';

export function getJungleDominance(mode: GameMode, config?: AxiosRequestConfig) {
	return api.get<JungleDominanceResult>('/api/stats/jungle-dominance', {
		params: { mode },
		...config
	});
}

export function getSupportImpact(mode: GameMode, config?: AxiosRequestConfig) {
	return api.get<SupportImpactResult>('/api/stats/support-impact', { params: { mode }, ...config });
}

export function getVisionDominance(mode: GameMode, config?: AxiosRequestConfig) {
	return api.get<VisionDominanceResult>('/api/stats/vision-dominance', {
		params: { mode },
		...config
	});
}

export function getGoldEfficiency(mode: GameMode, config?: AxiosRequestConfig) {
	return api.get<GoldEfficiencyResult>('/api/stats/gold-efficiency', {
		params: { mode },
		...config
	});
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
