/** 교전 통계. */

import type { AxiosRequestConfig } from 'axios';
import { api } from '@/api/client';
import type { GameMode } from '@/types';

export function getMultiKillHighlights(mode: GameMode, config?: AxiosRequestConfig) {
	return api.get<MultiKillHighlightsResult>('/api/stats/multikill-highlights', {
		params: { mode },
		...config
	});
}

export function getChaosMatch(mode: GameMode, config?: AxiosRequestConfig) {
	return api.get<ChaosMatchResult>('/api/stats/chaos-match', { params: { mode }, ...config });
}

export function getDamageAnalysis(mode: GameMode, config?: AxiosRequestConfig) {
	return api.get<DamageAnalysisResult>('/api/stats/damage-analysis', {
		params: { mode },
		...config
	});
}

export function getSurrenderAnalysis(mode: GameMode, config?: AxiosRequestConfig) {
	return api.get<SurrenderAnalysisResult>('/api/stats/surrender-analysis', {
		params: { mode },
		...config
	});
}

export function getLateGame(mode: GameMode, config?: AxiosRequestConfig) {
	return api.get<LateGameResult>('/api/stats/late-game', { params: { mode }, ...config });
}

// 멀티킬 하이라이트
export interface MultiKillEvent {
	riotId: string;
	champion: string;
	championId: number;
	multiKillType: string;
	matchId: string;
	gameCreation: number;
}

export interface PlayerMultiKillStat {
	riotId: string;
	pentaKills: number;
	quadraKills: number;
	tripleKills: number;
	doubleKills: number;
	topChampion: string | null;
	topChampionId: number | null;
}

export interface MultiKillHighlightsResult {
	pentaKillEvents: MultiKillEvent[];
	recentHighlights: MultiKillEvent[];
	playerRankings: PlayerMultiKillStat[];
}

// 경기 혼돈 지수
export interface ChaosMatchEntry {
	matchId: string;
	gameCreation: number;
	gameDurationMin: number;
	chaosIndex: number;
	totalKills: number;
	killDensity: number;
	multiKillScore: number;
	gameTypeTag: string;
	participants: string[];
}

export interface ChaosMatchResult {
	topChaosMatches: ChaosMatchEntry[];
	topBloodBathMatches: ChaosMatchEntry[];
	topStrategicMatches: ChaosMatchEntry[];
	avgChaosIndex: number;
}

// 데미지 분석
export interface DamagePlayerEntry {
	riotId: string;
	games: number;
	avgTotalDamage: number;
	avgPhysicalDamage: number;
	avgMagicDamage: number;
	avgTrueDamage: number;
	physicalRatio: number;
	magicRatio: number;
	trueRatio: number;
	damageProfile: 'AD' | 'AP' | 'Hybrid' | 'Tank' | 'Unknown';
	avgDamageTaken: number;
	avgTurretDamage: number;
}

/** 백엔드는 players 로 내려준다. 예전 타입은 rankings 라 undefined.map 으로 터졌다. */
export interface DamageAnalysisResult {
	players: DamagePlayerEntry[];
}

// 서렌더 분석
export interface SurrenderPlayerEntry {
	riotId: string;
	games: number;
	surrenderGames: number;
	earlySurrenderGames: number;
	causedEarlySurrenderGames: number;
	surrenderRate: number;
	earlySurrenderRate: number;
}

export interface SurrenderAnalysisResult {
	totalGames: number;
	surrenderGames: number;
	earlySurrenderGames: number;
	overallSurrenderRate: number;
	overallEarlySurrenderRate: number;
	players: SurrenderPlayerEntry[];
}

// 후반 지배
export interface LateGamePlayerEntry {
	riotId: string;
	games: number;
	avgInhibitorKills: number;
	avgSurvivalSeconds: number;
	avgKillingSpree: number;
	lateGameScore: number;
	longestKillingSpree: number;
	topChampion: string | null;
	topChampionId: number | null;
}

/** 백엔드는 players 만 내려준다. lateGameKing 은 목록에서 뽑는다. */
export interface LateGameResult {
	players: LateGamePlayerEntry[];
}
