/** 세션(하루치 내전). 세션은 오전 6시에 시작하는 하루다 — 새벽 경기는 전날 세션에 들어간다. */

import type { AxiosRequestConfig } from 'axios';
import { api } from '@/api/client';
import type { GameMode } from '@/types';

export function getSessionReport(mode: GameMode, config?: AxiosRequestConfig) {
	return api.get<SessionReportResult>('/api/stats/sessions', { params: { mode }, ...config });
}

/** 없는 날짜는 백엔드가 404 를 준다. */
export function getSessionDetail(date: string, mode: GameMode, config?: AxiosRequestConfig) {
	return api.get<SessionDetailResult>(`/api/stats/sessions/${encodeURIComponent(date)}`, {
		params: { mode },
		...config
	});
}

// 세션 분석
/**
 * 세션 하나. 세션은 **오전 6시에 시작하는 하루**다 — 새벽 경기는 전날 세션에 들어간다.
 * 자정으로 자르면 23:40 에 시작한 판과 00:20 에 끝난 판이 갈려서 그렇게 뒀다.
 */
export interface SessionEntry {
	/** `yyyy-MM-dd`. 세션 상세의 식별자다. */
	date: string;
	/** 그 세션의 경기들. 시작 시각 오름차순. */
	matchIds: string[];
	games: number;
	totalDurationMin: number;
	sessionMvp: string | null;
	sessionMvpKda: number;
	team100Wins: number;
	team200Wins: number;
	totalKills: number;
	pentaKills: number;
	participants: string[];
}

export interface SessionReportResult {
	sessions: SessionEntry[];
	totalSessions: number;
}

export interface SessionMatchEntry {
	matchId: string;
	gameCreation: number;
	durationSec: number;
	winnerTeamId: number | null;
	totalKills: number;
	hasTimeline: boolean;
	/** 스파크라인용. 블루 − 레드, index = 분. 타임라인이 없으면 빈 목록. */
	teamGoldDiffByMinute: number[];
	/** 15분 팀 골드 격차(블루 − 레드). 타임라인이 없으면 null. */
	goldDiffAt15: number | null;
}

export interface SessionPlayerEntry {
	riotId: string;
	games: number;
	wins: number;
	kills: number;
	deaths: number;
	assists: number;
	kda: number;
	/** 타임라인이 있는 경기만. 없으면 null. */
	avgGoldDiff15: number | null;
	laneGames: number;
	earlyKills: number;
	earlyDeaths: number;
	soloKills: number;
}

export interface SessionStreak {
	riotId: string;
	length: number;
}

export interface SessionDetailResult {
	date: string;
	games: number;
	/** 그중 타임라인이 있는 경기 수. 아래 타임라인 수치 전부의 분모다. */
	timelineGames: number;
	firstGameAt: number;
	lastGameAt: number;
	totalDurationMin: number;
	totalKills: number;
	pentaKills: number;
	team100Wins: number;
	team200Wins: number;
	matches: SessionMatchEntry[];
	players: SessionPlayerEntry[];
	teamFights: number;
	team100FightWins: number;
	team200FightWins: number;
	/** 팀이 매 판 바뀌므로 **사람** 기준이다. 2연승부터 기록으로 본다. */
	longestWinStreak: SessionStreak | null;
	longestLossStreak: SessionStreak | null;
	/** 15분에 가장 크게 뒤지고도 이긴 경기. */
	biggestComeback: SessionMatchEntry | null;
	longestGame: SessionMatchEntry | null;
	shortestGame: SessionMatchEntry | null;
}
