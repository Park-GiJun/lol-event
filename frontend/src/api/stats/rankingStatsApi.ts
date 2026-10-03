/** 순위표 — MVP · 라인별 랭킹 · 킬 관여. Elo 리더보드는 `@/api/rating/ratingApi` 에 있다. */

import type { AxiosRequestConfig } from 'axios';
import { api } from '@/api/client';
import type { GameMode } from '@/types';

export function getMvpStats(mode?: GameMode, config?: AxiosRequestConfig) {
	return api.get<MvpStatsResult>('/api/stats/mvp', { params: { mode }, ...config });
}

/** @param lane TOP / JUNGLE / MID / BOTTOM / SUPPORT */
export function getLaneLeaderboard(lane: string, mode?: GameMode, config?: AxiosRequestConfig) {
	return api.get<LaneLeaderboardResult>('/api/stats/lane', { params: { lane, mode }, ...config });
}

export function getKillParticipation(mode: GameMode, config?: AxiosRequestConfig) {
	return api.get<KillParticipationResult>('/api/stats/kill-participation', {
		params: { mode },
		...config
	});
}

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
