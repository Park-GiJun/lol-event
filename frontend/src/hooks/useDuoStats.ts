import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api/api';
import type { DuoStatsResult } from '@/lib/types/stats';

export function useDuoStats(mode = 'normal', minGames = 1) {
  return useQuery({
    queryKey: ['duo-stats', mode, minGames],
    queryFn: () => api.get<DuoStatsResult>(`/stats/duo?mode=${mode}&minGames=${minGames}`),
  });
}
