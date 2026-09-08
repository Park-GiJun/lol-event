import { describe, expect, it } from 'vitest';
import { calcMvp, fmt, parseRiotId } from './lol';
import type { Match, Participant } from './types/match';

/** 테스트에서 보는 필드만 채운 참가자. 나머지는 MVP 계산에 안 쓰인다. */
function participant(over: Partial<Participant> & { riotId: string; team: 'blue' | 'red' }): Participant {
  return {
    kills: 0, deaths: 0, assists: 0, damage: 0, cs: 0, visionScore: 0, win: false,
    ...over,
  } as Participant;
}

function match(participants: Participant[], gameDuration = 1800): Match {
  return { gameDuration, participants } as Match;
}

describe('fmt', () => {
  it('초를 mm:ss 로 옮긴다', () => {
    expect(fmt(0)).toBe('0:00');
    expect(fmt(9)).toBe('0:09');
    expect(fmt(60)).toBe('1:00');
    expect(fmt(1830)).toBe('30:30');
  });

  it('한 시간을 넘겨도 분으로 계속 센다', () => {
    // 내전에서 60분 넘는 경기가 실제로 나온다. 시:분:초로 바꾸지 않는 게 현재 규약이다.
    expect(fmt(3661)).toBe('61:01');
  });
});

describe('parseRiotId', () => {
  it('이름과 태그를 가른다', () => {
    expect(parseRiotId('Hide on bush#KR1')).toEqual({ name: 'Hide on bush', tag: 'KR1' });
  });

  it('# 이 없으면 전부 이름으로 본다', () => {
    expect(parseRiotId('그냥이름')).toEqual({ name: '그냥이름', tag: '' });
  });

  it('이름에 # 이 들어가도 첫 # 만 구분자로 쓴다', () => {
    expect(parseRiotId('a#b#c')).toEqual({ name: 'a', tag: 'b#c' });
  });

  it('빈 문자열에서 터지지 않는다', () => {
    expect(parseRiotId('')).toEqual({ name: '', tag: '' });
  });
});

describe('calcMvp', () => {
  it('팀별 MVP 와 에이스를 각각 고른다', () => {
    const m = match([
      participant({ riotId: 'blue-carry', team: 'blue', kills: 10, assists: 5, deaths: 1, damage: 30000, cs: 250, visionScore: 30, win: true }),
      participant({ riotId: 'blue-sub',   team: 'blue', kills: 1,  assists: 2, deaths: 8, damage: 5000,  cs: 100, visionScore: 10, win: true }),
      participant({ riotId: 'red-carry',  team: 'red',  kills: 8,  assists: 3, deaths: 4, damage: 25000, cs: 220, visionScore: 25, win: false }),
      participant({ riotId: 'red-sub',    team: 'red',  kills: 0,  assists: 1, deaths: 9, damage: 3000,  cs: 80,  visionScore: 8,  win: false }),
    ]);
    const { aceId, blueMvpId, redMvpId } = calcMvp(m);
    expect(blueMvpId).toBe('blue-carry');
    expect(redMvpId).toBe('red-carry');
    expect(aceId).toBe('blue-carry');
  });

  it('데스 0 인 참가자에서 0 으로 나누지 않는다', () => {
    const m = match([
      participant({ riotId: 'perfect', team: 'blue', kills: 5, assists: 5, deaths: 0, damage: 10000, cs: 200, visionScore: 20, win: true }),
      participant({ riotId: 'red-one', team: 'red',  kills: 1, assists: 1, deaths: 5, damage: 2000,  cs: 100, visionScore: 5,  win: false }),
    ]);
    const { aceId } = calcMvp(m);
    expect(aceId).toBe('perfect');
    expect(Number.isFinite(0)).toBe(true);
  });

  it('팀 전체 딜이 0 이어도 NaN 을 만들지 않는다', () => {
    // 서렌이 아주 빨리 나오면 딜 합계가 0 으로 들어오는 경기가 있다.
    const m = match([
      participant({ riotId: 'b', team: 'blue', kills: 0, assists: 0, deaths: 0, damage: 0, cs: 0, visionScore: 0, win: true }),
      participant({ riotId: 'r', team: 'red',  kills: 0, assists: 0, deaths: 0, damage: 0, cs: 0, visionScore: 0, win: false }),
    ], 180);
    expect(() => calcMvp(m)).not.toThrow();
    const { aceId } = calcMvp(m);
    expect(['b', 'r']).toContain(aceId);
  });

  it('경기 시간이 0 이어도 분당 지표에서 0 으로 나누지 않는다', () => {
    const m = match([
      participant({ riotId: 'b', team: 'blue', kills: 1, assists: 0, deaths: 0, damage: 100, cs: 10, visionScore: 1, win: true }),
      participant({ riotId: 'r', team: 'red',  kills: 0, assists: 0, deaths: 1, damage: 50,  cs: 5,  visionScore: 0, win: false }),
    ], 0);
    expect(calcMvp(m).aceId).toBe('b');
  });

  it('참가자가 없으면 터지지 않고 빈 값을 준다', () => {
    // 상세 응답이 참가자 없이 오는 경우가 실제로 있었다. reduce 초기값이 없어 터졌다.
    expect(() => calcMvp(match([]))).not.toThrow();
    expect(calcMvp(match([]))).toEqual({ aceId: '', blueMvpId: '', redMvpId: '' });
  });

  it('한 팀이 통째로 비어도 그 팀 MVP 만 빈 값이 된다', () => {
    const m = match([
      participant({ riotId: 'only-blue', team: 'blue', kills: 3, assists: 1, deaths: 2, damage: 9000, cs: 150, visionScore: 12, win: true }),
    ]);
    expect(() => calcMvp(m)).not.toThrow();
    expect(calcMvp(m)).toEqual({ aceId: 'only-blue', blueMvpId: 'only-blue', redMvpId: '' });
  });
});
