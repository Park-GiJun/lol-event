import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api/api';
import type { PlayerEloHistoryResult } from '@/lib/types/stats';

export function usePlayerEloHistory(riotId: string) {
  return useQuery({
    queryKey: ['players', riotId, 'elo-history'],
    queryFn: () => api.get<PlayerEloHistoryResult>(`/stats/player/${encodeURIComponent(riotId)}/elo-history?limit=20`),
    enabled: !!riotId,
  });
}
