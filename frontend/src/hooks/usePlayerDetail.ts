import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api/api';
import type { PlayerDetailStats } from '@/lib/types/stats';

export function usePlayerDetail(riotId: string) {
  return useQuery({
    queryKey: ['players', riotId, 'detail'],
    queryFn: () => api.get<PlayerDetailStats>(`/stats/player/${encodeURIComponent(riotId)}?mode=all`),
    enabled: !!riotId,
  });
}
