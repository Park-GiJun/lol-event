/** 사람 사이의 관계 통계 — 같은 팀이었을 때(듀오)와 상대였을 때(라이벌). */

import type { AxiosRequestConfig } from 'axios';
import { api } from '@/api/client';
import type { GameMode, SampleGrade } from '@/types';

export function getDuoStats(mode?: GameMode, minGames?: number, config?: AxiosRequestConfig) {
	return api.get<DuoStatsResult>('/api/stats/duo', { params: { mode, minGames }, ...config });
}

export function getRivalMatchup(mode: GameMode, config?: AxiosRequestConfig) {
	return api.get<RivalMatchupResult>('/api/stats/rival-matchup', { params: { mode }, ...config });
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
