import { useState } from 'react';

export function useSortTable<T extends string>(initial: T, initialDir: 'asc' | 'desc' = 'desc') {
  const [sortKey, setSortKey] = useState<T>(initial);
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>(initialDir);

  function handleSort(key: T) {
    if (key === sortKey) setSortDir(d => d === 'desc' ? 'asc' : 'desc');
    else { setSortKey(key); setSortDir('desc'); }
  }

  function sorted<I>(items: I[], getValue: (key: T, item: I) => number): I[] {
    return [...items].sort((a, b) => {
      const d = getValue(sortKey, b) - getValue(sortKey, a);
      return sortDir === 'desc' ? d : -d;
    });
  }

  return { sortKey, sortDir, handleSort, sorted };
}
