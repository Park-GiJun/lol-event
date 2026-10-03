/** 레이팅 — Elo 리더보드 · 변동 내역 · 관리자용 재집계. */

import type { AxiosRequestConfig } from 'axios';
import { api } from '@/api/client';
import type { SampleGrade } from '@/types';

export function getEloLeaderboard(config?: AxiosRequestConfig) {
	return api.get<EloLeaderboardResult>('/api/stats/elo', config);
}

export function getEloHistory(riotId: string, limit: number, config?: AxiosRequestConfig) {
	return api.get<PlayerEloHistoryResult>(
		`/api/stats/player/${encodeURIComponent(riotId)}/elo-history`,
		{ params: { limit }, ...config }
	);
}

/** 두 레이팅을 초기화하고 전체 경기를 처음부터 다시 반영한다. 오래 걸린다. */
export function resetRatings() {
	return api.post<unknown>('/api/admin/elo/reset');
}

/**
 * `elo*` 는 **실력 레이팅(laneElo)** 이다. 화면이 "Elo" 라고 부르던 자리가 라인 레이팅으로 바뀌었다.
 * 전적 레이팅은 `teamElo*` 로 따로 온다.
 */
export interface EloHistoryEntry {
	matchId: string;
	eloBefore: number;
	eloAfter: number;
	delta: number;
	win: boolean;
	/** 라인 맞대결 결과. NONE 은 대결이 성립하지 않은 경기(칼바람이거나 포지션이 깨진 경기). */
	laneResult: 'WIN' | 'LOSS' | 'NONE';
	laneOpponent: string | null;
	teamEloBefore: number;
	teamEloAfter: number;
	teamDelta: number;
	gameCreation: number;
}

export interface PlayerEloHistoryResult {
	riotId: string;
	currentElo: number;
	eloRank: number | null;
	history: EloHistoryEntry[];
}

/**
 * 리더보드 한 줄. 실력 레이팅(laneElo)과 전적 레이팅(teamElo)은 끝까지 별개다 — 합쳐 쓰지 말 것.
 *
 * 표시·정렬은 `laneEloDisplay`(수축 적용)를 쓴다. 표본이 적은 사람이 과하게 튀는 것을 막는 값이다.
 * 계산에 넣을 일이 있으면 원값(`laneElo`)을 써야 한다.
 */
export interface EloRankEntry {
	/** 배치 중인 플레이어는 0. 순위를 매기지 않는다. */
	rank: number;
	riotId: string;

	/** 실력 레이팅 원값. */
	laneElo: number;
	/** 실력 레이팅 표시값(수축 적용). 화면은 이 값을 쓴다. */
	laneEloDisplay: number;
	laneDuels: number;
	laneWins: number;
	laneLosses: number;
	/** 라인 맞대결 승률(0~1). */
	laneWinRate: number;

	/** 전적 레이팅 원값. */
	teamElo: number;
	teamEloDisplay: number;
	teamGames: number;
	/** 팀 승률(0~1). */
	winRate: number;

	/** `teamElo - laneElo`. 절댓값이 크면 편성자의 평가와 라인 실적이 어긋나 있다는 뜻이다. */
	gap: number;

	/** 라인 맞대결 표본 미달. 목록에는 남기되 순위에서는 뺀다. */
	placement: boolean;
	mainPosition: string | null;

	/** `laneElo` 와 같은 값. */
	elo: number;
	/** `teamGames` 와 같은 값. */
	games: number;
	wins: number;
	losses: number;
	/** 표시 전용. 레이팅 산식에는 쓰이지 않는다. */
	winStreak: number;
	lossStreak: number;
	sampleGrade: SampleGrade;
}

export interface EloLeaderboardResult {
	/** 순위가 매겨진 플레이어가 먼저, 배치 중인 플레이어가 뒤에 온다. */
	players: EloRankEntry[];
	/** 순위에 들어가기 위한 최소 **라인 맞대결 수**. 경기 수가 아니다. */
	minDuels: number;
	rankedCount: number;
	placementCount: number;
}
