import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api/api';
import type { StatsResponse } from '@/lib/types/stats';

export function usePlayers() {
  return useQuery({
    queryKey: ['players'],
    queryFn: () => api.get<StatsResponse>('/stats'),
  });
}
