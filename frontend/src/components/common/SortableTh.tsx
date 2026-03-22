interface SortableThProps<T extends string> {
  label: string;
  col: T;
  sortKey: T;
  sortDir: 'asc' | 'desc';
  onSort: (col: T) => void;
  right?: boolean;
  width?: number | string;
}

export function SortableTh<T extends string>({
  label, col, sortKey, sortDir, onSort, right, width,
}: SortableThProps<T>) {
  const active = sortKey === col;
  return (
    <th
      onClick={() => onSort(col)}
      className={right ? 'table-number' : undefined}
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
        <span style={{ fontSize: 9, opacity: active ? 1 : 0.25 }}>
          {!active ? '↕' : sortDir === 'desc' ? '▼' : '▲'}
        </span>
      </span>
    </th>
  );
}
