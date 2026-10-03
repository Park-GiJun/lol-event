/** 전체 요약 — 개요 지표와 주간 어워드. */

import type { AxiosRequestConfig } from 'axios';
import { api } from '@/api/client';
import type { GameMode } from '@/types';

export function getOverviewStats(mode?: GameMode, config?: AxiosRequestConfig) {
	return api.get<OverviewStats>('/api/stats/overview', { params: { mode }, ...config });
}

export function getWeeklyAwards(mode: GameMode, config?: AxiosRequestConfig) {
	return api.get<WeeklyAwardsResult>('/api/stats/awards', { params: { mode }, ...config });
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
