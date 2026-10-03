import { describe, expect, it } from 'vitest';
import {
	MAP_MAX,
	formatClock,
	gridCellToSvg,
	heatIntensity,
	linePoints,
	mapToSvg,
	niceScale,
	timeFraction
} from '@/lib/chart';

describe('niceScale', () => {
	it('500 단위로 올려 잡는다', () => {
		expect(niceScale([100, 200])).toBe(500);
		expect(niceScale([600])).toBe(1000);
		expect(niceScale([1000])).toBe(1000);
	});

	it('음수도 크기로 본다', () => {
		expect(niceScale([-1200])).toBe(1500);
	});

	it('빈 목록이면 최소 눈금이다', () => {
		expect(niceScale([])).toBe(500);
	});

	it('단위를 바꿀 수 있다', () => {
		expect(niceScale([12], 10)).toBe(20);
	});
});

describe('linePoints', () => {
	const opts = { width: 100, height: 20, scale: 1000, pad: 0 };

	it('첫 점은 왼쪽 끝 마지막 점은 오른쪽 끝이다', () => {
		const points = linePoints([0, 0, 0], opts).split(' ');
		expect(points[0]).toBe('0,10');
		expect(points[2]).toBe('100,10');
	});

	it('양수는 기준선 위로 음수는 아래로 간다', () => {
		// SVG 는 y 가 아래로 커진다 — 양수 값의 y 가 더 작아야 위에 그려진다.
		const [up, down] = linePoints([1000, -1000], opts)
			.split(' ')
			.map((p) => Number(p.split(',')[1]));
		expect(up).toBeLessThan(10);
		expect(down).toBeGreaterThan(10);
	});

	it('값이 하나면 기준선을 가로로 긋는다', () => {
		expect(linePoints([500], opts)).toBe('0,10 100,10');
	});

	it('빈 목록이면 빈 문자열이다', () => {
		expect(linePoints([], opts)).toBe('');
	});
});

describe('mapToSvg', () => {
	it('y 를 뒤집는다', () => {
		// 협곡은 y 가 위로 갈수록 커지고 SVG 는 아래로 갈수록 커진다.
		// 뒤집지 않으면 탑 라인이 화면 아래에 그려진다.
		const blueBase = mapToSvg({ x: 0, y: 0 }, 100);
		expect(blueBase).toEqual({ x: 0, y: 100 });

		const redBase = mapToSvg({ x: MAP_MAX, y: MAP_MAX }, 100);
		expect(redBase).toEqual({ x: 100, y: 0 });
	});

	it('블루 탑 외곽 포탑은 왼쪽 위에 놓인다', () => {
		const { x, y } = mapToSvg({ x: 981, y: 10441 }, 100);
		expect(x).toBeLessThan(20);
		expect(y).toBeLessThan(40);
	});

	it('블루 봇 외곽 포탑은 오른쪽 아래에 놓인다', () => {
		const { x, y } = mapToSvg({ x: 10504, y: 1029 }, 100);
		expect(x).toBeGreaterThan(60);
		expect(y).toBeGreaterThan(80);
	});
});

describe('gridCellToSvg', () => {
	it('칸 크기는 격자로 나눈 값이다', () => {
		expect(gridCellToSvg({ x: 0, y: 0 }, 32, 320).size).toBe(10);
	});

	it('0번 칸은 왼쪽 아래다', () => {
		// 맵 좌표와 같은 이유로 y 를 뒤집는다. 돌려주는 y 는 칸의 위쪽 변이다.
		expect(gridCellToSvg({ x: 0, y: 0 }, 32, 320)).toEqual({ x: 0, y: 310, size: 10 });
	});

	it('마지막 칸은 오른쪽 위다', () => {
		expect(gridCellToSvg({ x: 31, y: 31 }, 32, 320)).toEqual({ x: 310, y: 0, size: 10 });
	});
});

describe('heatIntensity', () => {
	it('가장 많은 칸이 1 이다', () => {
		expect(heatIntensity(9, 9)).toBe(1);
	});

	it('제곱근을 써서 작은 칸도 보이게 한다', () => {
		// 선형이면 1/9 = 0.11 로 거의 투명해진다. 표본이 작을 때 대부분 칸이 그렇다.
		expect(heatIntensity(1, 9)).toBeCloseTo(1 / 3);
	});

	it('최댓값이 0이면 0 이다', () => {
		expect(heatIntensity(0, 0)).toBe(0);
	});
});

describe('formatClock', () => {
	it('분과 초로 나눈다', () => {
		expect(formatClock(0)).toBe('0:00');
		expect(formatClock(90_000)).toBe('1:30');
		expect(formatClock(900_330)).toBe('15:00');
	});

	it('음수는 0 으로 본다', () => {
		expect(formatClock(-5)).toBe('0:00');
	});
});

describe('timeFraction', () => {
	it('경기 길이에 대한 비율이다', () => {
		expect(timeFraction(600_000, 1_200_000)).toBe(0.5);
	});

	it('1 을 넘지 않는다', () => {
		// LCU 프레임 타임스탬프가 경기 길이를 조금 넘겨 오는 경우가 있다.
		expect(timeFraction(1_300_000, 1_200_000)).toBe(1);
	});

	it('길이가 0이면 0 이다', () => {
		expect(timeFraction(100, 0)).toBe(0);
	});
});
