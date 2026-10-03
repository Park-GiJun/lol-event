import { describe, expect, it } from 'vitest';
import { splitBold } from './richText';

describe('splitBold', () => {
	it('별표 두 개로 감싼 곳만 굵게 표시한다', () => {
		expect(splitBold('승률은 **71%** 입니다')).toEqual([
			{ text: '승률은 ', bold: false },
			{ text: '71%', bold: true },
			{ text: ' 입니다', bold: false }
		]);
	});

	it('굵은 글씨가 없으면 통째로 돌려준다', () => {
		expect(splitBold('그냥 글')).toEqual([{ text: '그냥 글', bold: false }]);
	});

	it('짝이 안 맞는 별표는 글자로 둔다', () => {
		expect(splitBold('**룰루** 와 **레오나')).toEqual([
			{ text: '룰루', bold: true },
			{ text: ' 와 **레오나', bold: false }
		]);
	});

	it('빈 글은 조각이 없다', () => {
		expect(splitBold('')).toEqual([]);
	});
});
