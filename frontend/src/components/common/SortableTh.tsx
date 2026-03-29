import { memo } from 'react';
import type React from 'react';

interface SortableThProps<T extends string> {
  label: string;
  col: T;
  sortKey: T;
  sortDir: 'asc' | 'desc';
  onSort: (col: T) => void;
  right?: boolean;
  width?: number | string;
}

function SortableThInner<T extends string>({
  label, col, sortKey, sortDir, onSort, right, width,
}: SortableThProps<T>) {
  const active = sortKey === col;
  const sortLabel = active
    ? (sortDir === 'desc' ? '내림차순 정렬됨' : '오름차순 정렬됨')
    : '정렬 가능';

  function handleKeyDown(e: React.KeyboardEvent) {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      onSort(col);
    }
  }

  return (
    <th
      onClick={() => onSort(col)}
      onKeyDown={handleKeyDown}
      className={right ? 'table-number' : undefined}
      tabIndex={0}
      role="columnheader"
      aria-sort={active ? (sortDir === 'desc' ? 'descending' : 'ascending') : 'none'}
      aria-label={`${label} ${sortLabel}`}
      style={{
        cursor: 'pointer',
        userSelect: 'none',
        whiteSpace: 'nowrap',
        width: width ?? undefined,
        color: active ? 'var(--color-primary)' : undefined,
      }}
    >
      <span style={{ display: 'inline-flex', alignItems: 'center', gap: 3 }}>
        {label}
        <span style={{ fontSize: 9, opacity: active ? 1 : 0.25 }} aria-hidden="true">
          {!active ? '↕' : sortDir === 'desc' ? '▼' : '▲'}
        </span>
      </span>
    </th>
  );
}

// memo는 제네릭 컴포넌트에 직접 적용이 어려워 캐스팅으로 처리
export const SortableTh = memo(SortableThInner) as typeof SortableThInner;
