import type { BuildTeamsRequest, TeamCandidate } from '@/api/team/teamBuildApi';
import { POSITIONS, type Position } from '@/lib/position';

/** 한 팀의 인원. */
export const TEAM_SIZE = 5;

/** 고를 수 있는 총 인원. 5 명씩 2~6 팀. */
export const SIZES = [10, 15, 20, 25, 30] as const;

/** 묶음 이름. 30 명이면 2 명씩 묶어도 15 개가 최대지만, 실제로는 서너 개면 충분하다. */
export const GROUP_LABELS = ['A', 'B', 'C', 'D', 'E', 'F'] as const;

/** 편성에 넣은 한 사람. */
export interface Entry {
	riotId: string;
	elo: number;
	/** 이번 판에 갈 수 있는 포지션. */
	positions: Position[];
	/** 같은 묶음 이름을 가진 사람끼리 같은 팀이 된다. null 이면 묶음 없음. */
	group: string | null;
	/** 포지션별 지금까지 한 판수. 버튼에 같이 보여 준다. */
	games: Partial<Record<Position, number>>;
}

export function entryOf(candidate: TeamCandidate): Entry {
	return {
		riotId: candidate.riotId,
		elo: candidate.elo,
		positions: candidate.defaultPositions,
		group: null,
		games: Object.fromEntries(candidate.positions.map((p) => [p.position, p.games]))
	};
}

/** 포지션을 켜고 끈다. 다섯 자리 순서를 유지한다. */
export function togglePosition(entry: Entry, position: Position): Entry {
	const next = entry.positions.includes(position)
		? entry.positions.filter((p) => p !== position)
		: [...entry.positions, position];
	return { ...entry, positions: POSITIONS.filter((p) => next.includes(p)) };
}

/** 보내기 전에 화면에서 먼저 잡을 수 있는 문제. 없으면 null. */
export function problemOf(entries: Entry[], size: number): string | null {
	if (entries.length !== size) return `${size}명을 골라 주세요 (지금 ${entries.length}명).`;

	const noPosition = entries.find((e) => e.positions.length === 0);
	if (noPosition) return `${nameOf(noPosition.riotId)}의 가능 포지션을 하나 이상 켜 주세요.`;

	for (const label of GROUP_LABELS) {
		const members = entries.filter((e) => e.group === label);
		if (members.length === 1)
			return `묶음 ${label}에 한 명뿐입니다. 한 명 더 넣거나 묶음을 풀어 주세요.`;
		if (members.length > TEAM_SIZE) return `묶음 ${label}이 ${TEAM_SIZE}명을 넘습니다.`;
	}
	return null;
}

export function requestOf(entries: Entry[], seed: number, commentary: boolean): BuildTeamsRequest {
	return {
		players: entries.map((e) => ({ riotId: e.riotId, positions: e.positions })),
		togetherGroups: GROUP_LABELS.map((label) =>
			entries.filter((e) => e.group === label).map((e) => e.riotId)
		).filter((group) => group.length > 1),
		seed,
		commentary
	};
}

/** `이름#태그` 의 이름. */
export function nameOf(riotId: string): string {
	return riotId.split('#')[0];
}
