import { describe, expect, it } from 'vitest';
import type { TeamCandidate } from '@/api/team/teamBuildApi';
import { entryOf, problemOf, requestOf, togglePosition, type Entry } from './teamAiState';

const arang: TeamCandidate = {
	riotId: '아랑택#아랑택',
	elo: 1403,
	games: 113,
	mainPosition: 'TOP',
	positions: [
		{ position: 'TOP', games: 53 },
		{ position: 'MID', games: 28 },
		{ position: 'JUNGLE', games: 18 },
		{ position: 'ADC', games: 14 }
	],
	defaultPositions: ['TOP', 'JUNGLE', 'MID', 'ADC']
};

function entries(count: number): Entry[] {
	return Array.from({ length: count }, (_, i) => ({
		riotId: `p${i}#KR1`,
		elo: 1500,
		positions: ['TOP', 'MID'],
		group: null,
		games: {}
	}));
}

describe('entryOf', () => {
	it('가 본 포지션을 켠 채로 시작한다', () => {
		const entry = entryOf(arang);

		expect(entry.positions).toEqual(['TOP', 'JUNGLE', 'MID', 'ADC']);
		expect(entry.games).toEqual({ TOP: 53, MID: 28, JUNGLE: 18, ADC: 14 });
		expect(entry.group).toBeNull();
	});
});

describe('togglePosition', () => {
	it('켜고 끄며, 순서는 탑 → 서포터를 지킨다', () => {
		const on = togglePosition(entryOf(arang), 'SUPPORT');
		expect(on.positions).toEqual(['TOP', 'JUNGLE', 'MID', 'ADC', 'SUPPORT']);

		const off = togglePosition(on, 'JUNGLE');
		expect(off.positions).toEqual(['TOP', 'MID', 'ADC', 'SUPPORT']);
	});
});

describe('problemOf', () => {
	it('조건이 다 맞으면 문제가 없다', () => {
		expect(problemOf(entries(10), 10)).toBeNull();
	});

	it('인원이 안 맞으면 몇 명인지 말한다', () => {
		expect(problemOf(entries(9), 10)).toContain('지금 9명');
	});

	it('포지션을 다 끈 사람이 있으면 그 사람을 말한다', () => {
		const list = entries(10);
		list[3] = { ...list[3], positions: [] };

		expect(problemOf(list, 10)).toContain('p3');
	});

	it('한 명짜리 묶음과 다섯을 넘는 묶음을 막는다', () => {
		const alone = entries(10);
		alone[0] = { ...alone[0], group: 'A' };
		expect(problemOf(alone, 10)).toContain('묶음 A에 한 명뿐');

		const tooMany = entries(10).map((e, i) => (i < 6 ? { ...e, group: 'B' } : e));
		expect(problemOf(tooMany, 10)).toContain('묶음 B');
	});
});

describe('requestOf', () => {
	it('같은 묶음 이름끼리 한 묶음으로 보낸다', () => {
		const list = entries(10).map((e, i) => ({
			...e,
			group: i < 2 ? 'A' : i < 5 ? 'B' : null
		}));

		const request = requestOf(list, 7, true);

		expect(request.togetherGroups).toEqual([
			['p0#KR1', 'p1#KR1'],
			['p2#KR1', 'p3#KR1', 'p4#KR1']
		]);
		expect(request.players[0]).toEqual({ riotId: 'p0#KR1', positions: ['TOP', 'MID'] });
		expect(request.seed).toBe(7);
	});
});
