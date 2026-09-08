import { act, renderHook } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { useSortTable } from './useSortTable';

type Key = 'winRate' | 'games';
const rows = [
  { name: 'a', winRate: 40, games: 30 },
  { name: 'b', winRate: 70, games: 10 },
  { name: 'c', winRate: 55, games: 20 },
];
const get = (key: Key, item: typeof rows[number]) => item[key];

describe('useSortTable', () => {
  it('처음에는 지정한 키를 내림차순으로 잡는다', () => {
    const { result } = renderHook(() => useSortTable<Key>('winRate'));
    expect(result.current.sortKey).toBe('winRate');
    expect(result.current.sortDir).toBe('desc');
    expect(result.current.sorted(rows, get).map(r => r.name)).toEqual(['b', 'c', 'a']);
  });

  it('같은 키를 다시 누르면 방향만 뒤집는다', () => {
    const { result } = renderHook(() => useSortTable<Key>('winRate'));
    act(() => result.current.handleSort('winRate'));
    expect(result.current.sortDir).toBe('asc');
    expect(result.current.sorted(rows, get).map(r => r.name)).toEqual(['a', 'c', 'b']);

    act(() => result.current.handleSort('winRate'));
    expect(result.current.sortDir).toBe('desc');
  });

  it('다른 키를 누르면 그 키의 내림차순으로 새로 시작한다', () => {
    const { result } = renderHook(() => useSortTable<Key>('winRate', 'asc'));
    act(() => result.current.handleSort('games'));
    expect(result.current.sortKey).toBe('games');
    // 이전 방향(asc)을 물려받지 않고 desc 로 리셋되는 게 규약이다.
    expect(result.current.sortDir).toBe('desc');
    expect(result.current.sorted(rows, get).map(r => r.name)).toEqual(['a', 'c', 'b']);
  });

  it('원본 배열을 건드리지 않는다', () => {
    const { result } = renderHook(() => useSortTable<Key>('winRate'));
    const before = rows.map(r => r.name);
    result.current.sorted(rows, get);
    expect(rows.map(r => r.name)).toEqual(before);
  });

  it('빈 목록에서도 터지지 않는다', () => {
    const { result } = renderHook(() => useSortTable<Key>('winRate'));
    expect(result.current.sorted([], get)).toEqual([]);
  });
});
