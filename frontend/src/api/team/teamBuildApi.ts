/** 팀 편성. 편성은 서버 코드가 계산하고, LLM 은 해설만 붙인다. */

import type { AxiosRequestConfig } from 'axios';
import { api } from '@/api/client';
import type { Position } from '@/lib/position';

export interface TeamCandidate {
	riotId: string;
	/** 라인 레이팅 원값. 기록이 없으면 시작 점수. */
	elo: number;
	games: number;
	mainPosition: Position | null;
	/** 간 적 있는 포지션과 판수. 많이 간 순. */
	positions: { position: Position; games: number }[];
	/** 처음에 켜 둘 포지션 — 충분히 가 본 자리. 기록이 없으면 다섯 자리 전부. */
	defaultPositions: Position[];
	/** 포지션 → 그 자리에 앉았을 때의 라인 Elo. 편성은 `elo` 가 아니라 이 값으로 팀 강도를 잰다. */
	seatElo: Record<Position, number>;
}

export function getTeamCandidates(config?: AxiosRequestConfig) {
	return api.get<TeamCandidate[]>('/api/team-build/candidates', config);
}

export interface BuildTeamsRequest {
	players: { riotId: string; positions: Position[] }[];
	/** 같은 팀이어야 하는 사람들의 묶음. 한 묶음은 5 명까지. */
	togetherGroups: string[][];
	/** 같은 값이면 같은 편성. 바꾸면 비슷하게 좋은 다른 편성이 나온다. */
	seed: number;
	/** LLM 해설을 붙일지. 해설은 수십 초 걸린다. */
	commentary: boolean;
}

export interface TeamBuildMember {
	riotId: string;
	position: Position;
	elo: number;
	/** 갈 수 있다고 한 포지션이 아닌 자리에 앉았다. */
	offRole: boolean;
}

export interface TeamBuildTeam {
	name: string;
	averageElo: number;
	/** 나머지 팀들의 평균을 상대로 이길 기대 확률(0~1). */
	winProbability: number;
	/** 탑 → 정글 → 미드 → 원딜 → 서포터 순. */
	members: TeamBuildMember[];
}

export interface TeamBuildResult {
	/** 평균 Elo 가 높은 팀부터. */
	teams: TeamBuildTeam[];
	/** 가장 센 팀과 가장 약한 팀의 평균 Elo 차. */
	eloSpread: number;
	/** 가능 포지션만으로 자리를 다 채우지 못했다. */
	positionConflict: boolean;
	commentary: string | null;
	/** 해설을 못 만든 이유. 편성은 이 값과 무관하게 유효하다. */
	commentaryError: string | null;
}

export function buildTeams(request: BuildTeamsRequest) {
	return api.post<TeamBuildResult>('/api/team-build', request);
}
