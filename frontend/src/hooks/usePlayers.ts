import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api/api';
import type { StatsResponse } from '@/lib/types/stats';

export function usePlayers(mode = 'normal') {
  return useQuery({
    queryKey: ['players', mode],
    queryFn: () => api.get<StatsResponse>(`/stats?mode=${mode}`),
  });
}
