/** 챔피언 축 통계 — 상세 · 티어 · 장인 인증 · 밴 분석. */

import type { AxiosRequestConfig } from 'axios';
import { api } from '@/api/client';
import type { GameMode, SampleGrade } from '@/types';

export function getChampionStats(champion: string, mode: GameMode, config?: AxiosRequestConfig) {
	return api.get<ChampionDetailStats>(`/api/stats/champion/${encodeURIComponent(champion)}`, {
		params: { mode },
		...config
	});
}

export function getChampionTier(mode: GameMode, minGames: number, config?: AxiosRequestConfig) {
	return api.get<ChampionTierResult>('/api/stats/champion-tier', {
		params: { mode, minGames },
		...config
	});
}

export function getChampionCertificate(mode: GameMode, config?: AxiosRequestConfig) {
	return api.get<ChampionCertificateResult>('/api/stats/champion-certificate', {
		params: { mode },
		...config
	});
}

export function getBanAnalysis(mode: GameMode, config?: AxiosRequestConfig) {
	return api.get<BanAnalysisResult>('/api/stats/ban-analysis', { params: { mode }, ...config });
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
