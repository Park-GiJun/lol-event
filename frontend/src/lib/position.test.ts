import { describe, expect, it } from 'vitest';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { POSITIONS, byPosition, positionLabel, positionOrder } from './position';

describe('positionLabel', () => {
  it('백엔드 포지션 다섯 개를 모두 한글로 옮긴다', () => {
    expect(POSITIONS.map(positionLabel)).toEqual(['탑', '정글', '미드', '원딜', '서포터']);
  });

  it('배정되지 않았거나 모르는 값은 - 로 떨어뜨린다', () => {
    // 백엔드는 미배정을 빈 문자열로, 판정 실패를 UNKNOWN 으로 보낸다.
    expect(positionLabel('')).toBe('-');
    expect(positionLabel('UNKNOWN')).toBe('-');
    expect(positionLabel(null)).toBe('-');
    expect(positionLabel(undefined)).toBe('-');
    // Riot 원본 표기는 백엔드가 이미 변환해서 주므로 프론트에 오면 안 된다.
    expect(positionLabel('MIDDLE')).toBe('-');
    expect(positionLabel('BOTTOM')).toBe('-');
    expect(positionLabel('UTILITY')).toBe('-');
  });
});

describe('positionOrder', () => {
  it('탑에서 서포터까지 라인 순서를 지킨다', () => {
    expect(POSITIONS.map(positionOrder)).toEqual([0, 1, 2, 3, 4]);
  });

  it('모르는 값은 전부 맨 뒤로 보낸다', () => {
    for (const v of ['', 'UNKNOWN', 'MIDDLE', null, undefined]) {
      expect(positionOrder(v)).toBe(POSITIONS.length);
    }
  });
});

describe('byPosition', () => {
  it('뒤섞인 참가자를 라인 순서대로 세운다', () => {
    const rows = [
      { assignedPosition: 'SUPPORT' },
      { assignedPosition: 'TOP' },
      { assignedPosition: 'ADC' },
      { assignedPosition: 'JUNGLE' },
      { assignedPosition: 'MID' },
    ];
    expect([...rows].sort(byPosition).map(r => r.assignedPosition))
      .toEqual(['TOP', 'JUNGLE', 'MID', 'ADC', 'SUPPORT']);
  });

  it('미배정 참가자는 배정된 참가자 뒤에 남는다', () => {
    const rows = [
      { assignedPosition: '' },
      { assignedPosition: 'MID' },
      { assignedPosition: null },
      { assignedPosition: 'TOP' },
    ];
    expect([...rows].sort(byPosition).map(r => r.assignedPosition))
      .toEqual(['TOP', 'MID', '', null]);
  });
});

/**
 * 계약 검사. 포지션 표기가 프론트와 백엔드에서 갈라지면 정렬 비교자의 indexOf 가
 * 전부 -1 이 되면서 화면이 조용히 틀어진다. 실제로 한 번 겪은 사고라
 * (lib/position.ts 주석 참고) 백엔드 enum 을 직접 읽어 대조한다.
 */
describe('백엔드 Position enum 과의 계약', () => {
  const POSITION_KT = resolve(
    __dirname,
    '../../../backend/main-service/src/main/kotlin/com/gijun/main/domain/model/match/Position.kt',
  );

  function backendPositions(): string[] {
    const src = readFileSync(POSITION_KT, 'utf8');
    const body = src.match(/enum\s+class\s+Position\s*\{([^}]*)\}/)?.[1];
    if (!body) throw new Error(`Position.kt 에서 enum 본문을 못 찾았다: ${POSITION_KT}`);
    return body.split(',').map(s => s.trim()).filter(Boolean);
  }

  it('백엔드가 보내는 라인 값을 프론트가 빠짐없이 안다', () => {
    // UNKNOWN 은 판정 실패용 센티널이라 화면 라인 목록에는 넣지 않는다.
    const lanes = backendPositions().filter(p => p !== 'UNKNOWN');
    expect(lanes).toEqual([...POSITIONS]);
  });

  it('UNKNOWN 이 사라지거나 이름이 바뀌면 알아챈다', () => {
    // 사라지면 위 필터가 조용히 통과해 버리므로 존재 자체를 못박아 둔다.
    expect(backendPositions()).toContain('UNKNOWN');
  });
});
